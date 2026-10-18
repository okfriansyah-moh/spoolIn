import { describe, expect, it, vi } from 'vitest';
import type { GraphqlRequest } from '../../transport/graphql';
import { createCreditRole, recoverCreditRole } from './role-api';

const id = '11111111-1111-4111-8111-111111111111';
const role = '22222222-2222-4222-8222-222222222222';
const intent = { operationId: id, category: 'CAST' as const, label: 'Stunt', policyRevision: 'r1' };
const signal = () => new AbortController().signal;
function receipt(state: string) {
  return { id, state, resultId: state === 'SUCCEEDED' ? role : null, resultExpired: false, problem: null };
}

describe('custom role command boundary', () => {
  it('binds exact category policy and operation and only projects the confirmed role identity', async () => {
    const request = vi.fn<GraphqlRequest>(async () => ({ createRole: { operation: receipt('SUCCEEDED'), problem: null } }));
    const outcome = await createCreditRole(request, intent, 'csrf', signal());
    expect(outcome).toMatchObject({ state: 'SUCCEEDED', operationId: id, resultId: role });
    expect(request.mock.calls[0]?.[1]).toEqual(intent);
    expect(request.mock.calls[0]?.[2]).toMatchObject({ mutation: true, csrf: 'csrf' });
  });

  it('retains pending and explicit rejected results without manufacturing a successful or failed receipt', async () => {
    const request = vi.fn<GraphqlRequest>(async () => ({ createRole: { operation: receipt('PENDING'), problem: null } }));
    expect(await createCreditRole(request, intent, 'csrf', signal())).toMatchObject({ state: 'PENDING', resultId: null });
    request.mockResolvedValue({ createRole: { operation: null, problem: { code: 'ALREADY_EXISTS', retryAfterMs: null } } });
    expect(await createCreditRole(request, intent, 'csrf', signal())).toMatchObject({ state: 'REJECTED', operationId: id });
  });

  it('rejects wrong original identity and successful receipts missing their role identity', async () => {
    const request = vi.fn<GraphqlRequest>(async () => ({ createRole: { operation: { ...receipt('SUCCEEDED'), id: role }, problem: null } }));
    await expect(createCreditRole(request, intent, 'csrf', signal())).rejects.toThrow();
    request.mockResolvedValue({ createRole: { operation: { ...receipt('SUCCEEDED'), resultId: null }, problem: null } });
    await expect(createCreditRole(request, intent, 'csrf', signal())).rejects.toThrow();
  });

  it('uses Catalog owner original lookup and never treats an absent operation as success', async () => {
    const request = vi.fn<GraphqlRequest>(async () => ({ operation: null }));
    expect(await recoverCreditRole(request, id, 'csrf', signal())).toBeNull();
    expect(request.mock.calls[0]?.[0]).toContain('owner: CATALOG');
    request.mockResolvedValue({ operation: receipt('SUCCEEDED') });
    expect(await recoverCreditRole(request, id, 'csrf', signal())).toMatchObject({ resultId: role });
  });
});
