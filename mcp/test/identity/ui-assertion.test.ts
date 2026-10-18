import { createHmac } from 'node:crypto';
import { chmodSync, mkdtempSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { inspect } from 'node:util';
import { describe, expect, it } from 'vitest';
import { UiAssertionSigner } from '../../src/identity/ui-assertion.js';
import type { UiBinding } from '../../src/identity/ui-assertion.js';

const binding: UiBinding = { connector: '00000000-0000-4000-8000-000000000001', client: 'CLAUDE',
  session: '00000000-0000-4000-8000-000000000002', proposal: '00000000-0000-4000-8000-000000000003',
  revision: 3, hash: Buffer.alloc(32, 9).toString('base64'), method: '/spoolin.v1.McpGatewayService/ApproveMovie' };

function fixture(action: (signer: UiAssertionSigner, directory: string, key: Buffer) => void): void {
  const directory = mkdtempSync(join(tmpdir(), 'spoolin-ui-'));
  const key = Buffer.alloc(32, 7);
  const path = join(directory, 'key');
  writeFileSync(path, key, { mode: 0o600 });
  const signer = new UiAssertionSigner('synthetic-v1', path);
  try { action(signer, directory, key); } finally { signer.close(); rmSync(directory, { recursive: true }); }
}

function payload(token: string): Record<string, unknown> {
  return JSON.parse(Buffer.from(token.split('.')[1]!, 'base64url').toString('utf8')) as Record<string, unknown>;
}

describe('private UI assertion signer', () => {
  it('binds the exact session proposal and RPC with deterministic JSON and bounded expiry', () => fixture((signer, _directory, key) => {
    const token = signer.sign(binding, 1000);
    const [id, body, mac] = token.split('.');
    const claims = payload(token);
    expect(claims).toMatchObject({ audience: 'core', ...binding, expiry: 1030 });
    expect(Object.keys(claims)).toEqual(Object.keys(claims).sort());
    expect(mac).toBe(createHmac('sha256', key).update(`${id}.${body}`).digest('base64url'));
    expect(claims).not.toHaveProperty('challenge');
    expect(Buffer.byteLength(token)).toBeLessThanOrEqual(4096);
    expect(claims.invocation).not.toBe(payload(signer.sign(binding, 1000)).invocation);
  }));

  it('signs both exact allowed methods and rejects all other methods', () => fixture((signer) => {
    const rejection = { ...binding, method: '/spoolin.v1.McpGatewayService/RejectMovie' as const };
    expect(payload(signer.sign(rejection, 1000)).method).toBe(rejection.method);
    expect(() => Reflect.apply(signer.sign, signer, [{ ...binding, method: '/spoolin.v1.McpGatewayService/ProposeMovie' }, 1000])).toThrow();
  }));

  it('rejects runtime coercion malformed identifiers unexpected fields and hash encodings', () => fixture((signer) => {
    const invalid = [{ ...binding, revision: '3' }, { ...binding, revision: 3.5 }, { ...binding, revision: 0 },
      { ...binding, revision: 2147483648 }, { ...binding, client: 'OTHER' }, { ...binding, session: 'session' },
      { ...binding, connector: '1-1-1-1-1' }, { ...binding, proposal: 'ffffffff-FFFF-4000-8000-000000000001' },
      { ...binding, hash: 'hash' }, { ...binding, hash: 'a'.repeat(64) }, { ...binding, hash: binding.hash.trimEnd() + '=' },
      { ...binding, audience: 'catalog' }, { ...binding, expiry: 100000 }, { ...binding, challenge: 'private' }];
    for (const value of invalid) expect(() => Reflect.apply(signer.sign, signer, [value, 1000])).toThrow();
    for (const now of [NaN, Infinity, -1, 1000.1, Number.MAX_SAFE_INTEGER]) expect(() => signer.sign(binding, now)).toThrow();
  }));

  it('uses only the new write key after rotation and zeroes retired in-memory material', () => fixture((signer, directory, old) => {
    const next = Buffer.alloc(32, 8);
    const file = join(directory, 'next');
    writeFileSync(file, next, { mode: 0o600 });
    const retired: Buffer = Reflect.get(signer, 'key');
    signer.rotate('synthetic-v2', file);
    expect(retired.every(byte => byte === 0)).toBe(true);
    const token = signer.sign(binding, 1000);
    const [id, body, mac] = token.split('.');
    expect(id).toBe('synthetic-v2');
    expect(mac).toBe(createHmac('sha256', next).update(`${id}.${body}`).digest('base64url'));
    expect(mac).not.toBe(createHmac('sha256', old).update(`${id}.${body}`).digest('base64url'));
    expect(() => signer.rotate('synthetic-v1', join(directory, 'key'))).toThrow();
  }));

  it('failed rotation preserves the current signer and rejects same material under a new ID', () => fixture((signer, directory) => {
    expect(() => signer.rotate('synthetic-v2', join(directory, 'key'))).toThrow();
    expect(() => signer.rotate('synthetic-v1', join(directory, 'key'))).toThrow();
    expect(signer.sign(binding, 1000).startsWith('synthetic-v1.')).toBe(true);
  }));

  it('rejects weakly protected nonregular relative symlink and wrong size key files', () => fixture((_signer, directory) => {
    const file = join(directory, 'key');
    chmodSync(file, 0o644);
    expect(() => new UiAssertionSigner('v1', file)).toThrow();
    chmodSync(file, 0o600);
    symlinkSync(file, join(directory, 'link'));
    expect(() => new UiAssertionSigner('v1', join(directory, 'link'))).toThrow();
    expect(() => new UiAssertionSigner('v1', directory)).toThrow();
    expect(() => new UiAssertionSigner('v1', 'relative')).toThrow();
    writeFileSync(join(directory, 'short'), Buffer.alloc(31), { mode: 0o600 });
    expect(() => new UiAssertionSigner('v1', join(directory, 'short'))).toThrow();
  }));

  it('close is irreversible and diagnostic serialization never discloses material', () => fixture((signer) => {
    expect(JSON.stringify(signer)).toBe('"UiAssertionSigner(redacted)"');
    expect(inspect(signer)).toBe('UiAssertionSigner(redacted)');
    signer.close();
    expect(() => signer.sign(binding, 1000)).toThrow();
    expect(() => signer.rotate('v2', '/unused')).toThrow();
  }));

  it('default publication remains disabled and enabled configuration requires protected active keys', () => fixture((_signer, directory) => {
    expect(UiAssertionSigner.fromEnvironment({})).toBeUndefined();
    expect(UiAssertionSigner.fromEnvironment({ MCP_PUBLICATION_ENABLED: 'false' })).toBeUndefined();
    expect(() => UiAssertionSigner.fromEnvironment({ MCP_PUBLICATION_ENABLED: 'yes' })).toThrow();
    expect(() => UiAssertionSigner.fromEnvironment({ MCP_PUBLICATION_ENABLED: 'true' })).toThrow();
    const configured = UiAssertionSigner.fromEnvironment({ MCP_PUBLICATION_ENABLED: 'true',
      MCP_UI_ASSERTION_KEY_ID: 'active', MCP_UI_ASSERTION_KEY_FILE: join(directory, 'key') });
    try { expect(configured?.sign(binding, 1000).startsWith('active.')).toBe(true); } finally { configured?.close(); }
  }));
});
