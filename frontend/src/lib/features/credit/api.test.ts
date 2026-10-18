import { describe, expect, it, vi } from 'vitest';
import type { GraphqlRequest } from '../../transport/graphql';
import { addCredit, removeCredit, updateCredit, recoverCredit } from './api';

const id = '11111111-1111-4111-8111-111111111111';
function response(field: string, state: string) {
  return {
    [field]: {
      operation: { id, state, resultExpired: false, problem: null },
      problem: null,
    },
  };
}

describe('credit writes and original receipts', () => {
  it('preserves long versions beyond JavaScript integer precision and pending state', async () => {
    const request = vi.fn<GraphqlRequest>(async () =>
      response('addCredit', 'PENDING'),
    );
    const outcome = await addCredit(
      request,
      id,
      id,
      '9007199254740993',
      'r1',
      { personId: id, roleId: id, characters: ['A/B', 'Second'] },
      'csrf',
      new AbortController().signal,
    );
    expect(outcome.state).toBe('PENDING');
    expect(request.mock.calls[0]?.[1]).toEqual({
      operationId: id,
      titleId: id,
      expectedVersion: '9007199254740993',
      policyRevision: 'r1',
      input: { personId: id, roleId: id, characters: ['A/B', 'Second'] },
    });
  });

  it('updates only supplied role and complete character list using credit version', async () => {
    const request = vi.fn<GraphqlRequest>(async () =>
      response('updateCredit', 'SUCCEEDED'),
    );
    await updateCredit(
      request,
      id,
      id,
      '12',
      'r1',
      { characters: ['A/B', 'C'] },
      'csrf',
      new AbortController().signal,
    );
    expect(request.mock.calls[0]?.[1]).toMatchObject({
      id,
      expectedVersion: '12',
      patch: { characters: ['A/B', 'C'] },
    });
  });

  it('removes the selected credit using its version without unrelated mutations', async () => {
    const request = vi.fn<GraphqlRequest>(async () =>
      response('removeCredit', 'SUCCEEDED'),
    );
    await removeCredit(
      request,
      id,
      id,
      '14',
      'csrf',
      new AbortController().signal,
    );
    expect(request.mock.calls[0]?.[1]).toEqual({
      operationId: id,
      id,
      expectedVersion: '14',
    });
  });

  it('rejects mismatched receipt identities instead of declaring success', async () => {
    const wrong = {
      addCredit: {
        operation: {
          id: '22222222-2222-4222-8222-222222222222',
          state: 'SUCCEEDED',
          resultExpired: false,
          problem: null,
        },
        problem: null,
      },
    };
    const request = vi.fn<GraphqlRequest>(async () => wrong);
    await expect(
      addCredit(
        request,
        id,
        id,
        '1',
        'r1',
        { personId: id, roleId: id, characters: [] },
        'csrf',
        new AbortController().signal,
      ),
    ).rejects.toThrow();
  });

  it('keeps expired original receipts terminal', async () => {
    const request = vi.fn<GraphqlRequest>(async () => ({
      operation: { id, state: 'EXPIRED', resultExpired: true, problem: null },
    }));
    expect(
      await recoverCredit(request, id, 'csrf', new AbortController().signal),
    ).toMatchObject({ state: 'EXPIRED', resultExpired: true });
  });
});
