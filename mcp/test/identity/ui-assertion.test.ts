import { createHmac } from 'node:crypto';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { UiAssertionSigner } from '../../src/identity/ui-assertion.js';

describe('private UI assertion signer', () => {
  it('binds the session, exact proposal and RPC method without signing a model-visible response', () => {
    const directory = mkdtempSync(join(tmpdir(), 'spoolin-ui-'));
    const key = Buffer.alloc(32, 7);
    const path = join(directory, 'key');
    writeFileSync(path, key, { mode: 0o600 });
    const signer = new UiAssertionSigner('synthetic-v1', path);
    try {
      const token = signer.sign({ connector: 'connector', client: 'CLAUDE', session: 'session',
        proposal: 'proposal', revision: 3, hash: 'hash', method: '/spoolin.v1.McpGatewayService/ApproveMovie' }, 1000);
      const [id, payload, mac] = token.split('.');
      const claims = JSON.parse(Buffer.from(payload!, 'base64url').toString('utf8'));
      expect(claims).toMatchObject({ audience: 'core', session: 'session', proposal: 'proposal', revision: 3, expiry: 1030 });
      expect(mac).toBe(createHmac('sha256', key).update(`${id}.${payload}`).digest('base64url'));
      expect(claims).not.toHaveProperty('challenge');
    } finally { signer.close(); rmSync(directory, { recursive: true }); }
  });
});
