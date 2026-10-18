import { readFileSync } from 'node:fs';
import { describe, expect, it, vi } from 'vitest';
import type { JsonSchemaType } from '@modelcontextprotocol/server';
import { GatewayFailure } from '../../src/operations/gateway-failure.js';
import { effectiveInputSchemas, readEffectivePolicy, SessionPolicy, validateEffectiveInput } from '../../src/tools/effective-policy.js';
import { createSession } from '../../src/gateway/session.js';
import { policyFixture } from '../fixtures/policy.js';

const base = JSON.parse(readFileSync(new URL('../../../contracts/json/mcp-inputs.schema.json', import.meta.url), 'utf8')) as Record<string, JsonSchemaType>;

describe('effective protobuf policy and advertised tool schemas', () => {
  it('uses inclusive current title original title synopsis source and search bounds without editing source schemas', () => {
    const snapshot = policyFixture();
    snapshot.textBounds.find((b) => b.field === 'title')!.minCodePoints = 2;
    snapshot.textBounds.find((b) => b.field === 'title')!.maxCodePoints = 8;
    snapshot.textBounds.find((b) => b.field === 'originalTitle')!.maxCodePoints = 7;
    snapshot.textBounds.find((b) => b.field === 'synopsis')!.maxCodePoints = 12;
    snapshot.textBounds.find((b) => b.field === 'sourceUrl')!.maxCodePoints = 20;
    snapshot.textBounds.find((b) => b.field === 'reuseBasis')!.maxCodePoints = 10;
    snapshot.requestLimits!.maxPage = 3;
    const schemas = effectiveInputSchemas(base, readEffectivePolicy(snapshot));
    expect(schemas.propose_movie).toMatchObject({ properties: {
      title: { minLength: 2, maxLength: 8 }, originalTitle: { minLength: 1, maxLength: 7 },
      synopsis: { maxLength: 12 }, sources: { items: { properties: {
        url: { minLength: 1, maxLength: 20 }, reuseBasis: { minLength: 1, maxLength: 10 },
      } } },
    } });
    expect(schemas.movie_context).toMatchObject({ properties: { limit: { maximum: 3 } } });
    expect(base.propose_movie).toMatchObject({ properties: { title: { maxLength: 255 } } });
  });
  it('counts Unicode code points and always permits an empty clearing query', async () => {
    const snapshot = policyFixture();
    const search = snapshot.textBounds.find((b) => b.field === 'search')!;
    search.minCodePoints = 3; search.maxCodePoints = 3;
    snapshot.requestLimits!.maxSearchCodePoints = 3;
    const policy = readEffectivePolicy(snapshot);
    const schema = effectiveInputSchemas(base, policy).movie_context!;
    await expect(validateEffectiveInput(schema, { query: '' }, policy)).resolves.toBeUndefined();
    await expect(validateEffectiveInput(schema, { query: '😀😀😀' }, policy)).resolves.toBeUndefined();
    await expect(validateEffectiveInput(schema, { query: '😀😀😀😀' }, policy)).rejects.toMatchObject({ problem: { code: 'FIELD_INVALID' } });
    await expect(validateEffectiveInput(schema, { query: 'ab' }, policy)).rejects.toBeInstanceOf(GatewayFailure);
  });
  it('rejects incomplete duplicate inconsistent overflowing and unsupported policy projections', () => {
    const initial = policyFixture();
    const cases = [
      { ...initial, revision: 'private-invalid-revision' },
      { ...initial, textBounds: [] },
      { ...initial, textBounds: [...initial.textBounds, initial.textBounds[0]!] },
      { ...initial, requestLimits: undefined },
      { ...initial, requestLimits: { ...initial.requestLimits!, maxSearchCodePoints: 1 } },
      { ...initial, artwork: { ...initial.artwork!, formats: ['GIF'] } },
      { ...initial, artwork: { ...initial.artwork!, maxPixels: '9223372036854775808' } },
      { ...initial, textBounds: initial.textBounds.map((b) => ({ ...b, maxCodePoints: 0 })) },
    ];
    for (const value of cases) expect(() => readEffectivePolicy(value)).toThrow(GatewayFailure);
  });
  it('enforces the actual metadata request bound before sending candidate values', async () => {
    const snapshot = policyFixture();
    snapshot.requestLimits!.maxMetadataBytes = 8;
    const policy = readEffectivePolicy(snapshot);
    await expect(validateEffectiveInput(base.movie_context!, { query: 'value' }, policy)).rejects.toMatchObject({ problem: { code: 'RESOURCE_EXHAUSTED' } });
  });
  it('coalesces concurrent refresh and rejects changed contents under the same revision', async () => {
    const initial = policyFixture();
    const changed = structuredClone(initial);
    changed.textBounds.find((b) => b.field === 'title')!.maxCodePoints = 8;
    const notify = vi.fn();
    const load = vi.fn(async () => changed);
    const policy = new SessionPolicy(initial, load, notify);
    await expect(policy.refresh()).rejects.toMatchObject({ problem: { code: 'UNAVAILABLE' } });
    changed.revision = 'b'.repeat(64);
    const [first, second] = await Promise.all([policy.refresh(), policy.refresh()]);
    expect(first).toBe(second);
    expect(load).toHaveBeenCalledTimes(2);
    expect(notify).toHaveBeenCalledTimes(1);
    expect(policy.current.revision).toBe(changed.revision);
  });
  it('closes the gateway when initial policy cannot safely register tools', async () => {
    const close = vi.fn();
    await expect(createSession('synthetic-policy-token', {
      policy: async () => ({ revision: 'a'.repeat(64), textBounds: [] }),
      context: async () => ({}),
      propose: async () => { throw new Error('must not send'); },
      operation: async () => { throw new Error('must not send'); },
      close,
    })).rejects.toBeInstanceOf(GatewayFailure);
    expect(close).toHaveBeenCalledTimes(1);
  });
});
