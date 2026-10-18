import { describe, expect, it } from 'vitest';
import { GatewayFailure } from '../../src/operations/gateway-failure.js';

describe('safe downstream failure classifications', () => {
  it('retains fixed RPC domain and system codes without downstream diagnostics', () => {
    for (const [code, expected] of [
      [3, 'FIELD_INVALID'],
      [4, 'DEADLINE_EXCEEDED'],
      [5, 'NOT_FOUND'],
      [6, 'IDEMPOTENCY_CONFLICT'],
      [7, 'FORBIDDEN'],
      [8, 'RESOURCE_EXHAUSTED'],
      [9, 'FIELD_INVALID'],
      [10, 'STALE_VERSION'],
      [11, 'FIELD_INVALID'],
      [14, 'UNAVAILABLE'],
      [16, 'UNAUTHENTICATED'],
    ]) {
      const failure = GatewayFailure.fromGrpc(
        Object.assign(new Error('private downstream body'), {
          code,
          details: 'private details',
        }),
      );
      expect(failure.problem.code).toBe(expected);
      expect(JSON.stringify(failure)).not.toMatch(
        /private downstream|private details/,
      );
    }
  });
  it('accepts only an exact classified policy change and safely folds unknown errors', () => {
    expect(
      GatewayFailure.fromGrpc(
        Object.assign(new Error(), { code: 9, details: 'POLICY_CHANGED' }),
      ).problem.code,
    ).toBe('POLICY_CHANGED');
    expect(
      GatewayFailure.fromGrpc(new Error('private downstream body')).problem
        .code,
    ).toBe('UNAVAILABLE');
  });
});
