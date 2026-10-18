import { describe, expect, it, vi } from 'vitest';
import type { GraphqlRequest } from '../../transport/graphql';
import { addCredit, removeCredit, updateCredit, recoverCredit, fetchActivePeoplePage } from './api';

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


it('credit people paging preserves actual cursors and refuses a looping or incomplete page', async () => {
  const request = vi.fn<GraphqlRequest>(async () => ({ managedPeople: {
    nodes: [{ id, name: 'One' }], pageInfo: { hasNextPage: true, endCursor: 'next-cursor' },
  } }));
  expect(await fetchActivePeoplePage(request, 'csrf', null)).toEqual({ nodes: [{ id, name: 'One' }], next: 'next-cursor' });
  expect(request.mock.calls[0]?.[1]).toEqual({ state: 'ACTIVE', after: null });
  await expect(fetchActivePeoplePage(request, 'csrf', 'next-cursor')).rejects.toThrow('cursor');
  request.mockResolvedValue({ managedPeople: { nodes: [], pageInfo: { hasNextPage: true, endCursor: null } } });
  await expect(fetchActivePeoplePage(request, 'csrf', null)).rejects.toThrow('cursor');
});

it('known credit domain rejection is distinct from a retained failed operation receipt', async () => {
  const request = vi.fn<GraphqlRequest>(async () => ({ addCredit: { operation: null,
    problem: { code: 'ALREADY_EXISTS', retryAfterMs: null } } }));
  expect(await addCredit(request, id, id, '1', 'p1', { personId: id, roleId: id, characters: [] }, 'csrf', new AbortController().signal))
    .toMatchObject({ state: 'REJECTED', operationId: id, problem: { code: 'ALREADY_EXISTS' } });
});


it('checks the exact Catalog operation until its actual terminal result without issuing another write', async () => {
  const request = vi.fn<GraphqlRequest>();
  request.mockResolvedValueOnce({ operation: { id, state: 'PENDING', resultExpired: false, problem: null } });
  request.mockResolvedValueOnce({ operation: { id, state: 'SUCCEEDED', resultId: id, version: '9', resultExpired: false, problem: null } });
  expect(await recoverCredit(request, id, 'csrf', new AbortController().signal)).toMatchObject({ operationId: id, state: 'PENDING' });
  expect(await recoverCredit(request, id, 'csrf', new AbortController().signal)).toMatchObject({ operationId: id, state: 'SUCCEEDED', version: '9' });
  for (const call of request.mock.calls) {
    expect(call[0]).toContain('query CreditOperation');
    expect(call[1]).toEqual({ id });
  }
});
