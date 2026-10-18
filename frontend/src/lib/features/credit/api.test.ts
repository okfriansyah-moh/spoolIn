import { describe, expect, it, vi } from 'vitest';
import type { GraphqlRequest } from '../../transport/graphql';
import {
  addCredit,
  removeCredit,
  updateCredit,
  recoverCredit,
  fetchActivePeoplePage,
} from './api';

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
  const request = vi.fn<GraphqlRequest>(async () => ({
    managedPeople: {
      nodes: [{ id, name: 'One' }],
      pageInfo: { hasNextPage: true, endCursor: 'next-cursor' },
    },
  }));
  expect(await fetchActivePeoplePage(request, 'csrf', null)).toEqual({
    nodes: [{ id, name: 'One' }],
    next: 'next-cursor',
  });
  expect(request.mock.calls[0]?.[1]).toEqual({ state: 'ACTIVE', after: null });
  await expect(
    fetchActivePeoplePage(request, 'csrf', 'next-cursor'),
  ).rejects.toThrow('cursor');
  request.mockResolvedValue({
    managedPeople: {
      nodes: [],
      pageInfo: { hasNextPage: true, endCursor: null },
    },
  });
  await expect(fetchActivePeoplePage(request, 'csrf', null)).rejects.toThrow(
    'cursor',
  );
});

it('known credit domain rejection is distinct from a retained failed operation receipt', async () => {
  const request = vi.fn<GraphqlRequest>(async () => ({
    addCredit: {
      operation: null,
      problem: { code: 'ALREADY_EXISTS', retryAfterMs: null },
    },
  }));
  expect(
    await addCredit(
      request,
      id,
      id,
      '1',
      'p1',
      { personId: id, roleId: id, characters: [] },
      'csrf',
      new AbortController().signal,
    ),
  ).toMatchObject({
    state: 'REJECTED',
    operationId: id,
    problem: { code: 'ALREADY_EXISTS' },
  });
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

it('managed credit paging sends the exact cursor and refuses missing or repeated continuation', async () => {
  const { fetchTitleCreditsPage } = await import('./api');
  const request = vi.fn<GraphqlRequest>(async () => ({ managedCredits: { nodes: [{ id, personId: id,
    personName: 'One', characters: ['Maya / Echo', 'Young Maya'], version: '9007199254740993',
    role: { id, label: 'Actor', category: 'CAST', allowsCharacters: true } }],
    pageInfo: { endCursor: 'next', hasNextPage: true } } }));
  const page = await fetchTitleCreditsPage(request, id, 'csrf', 'prior');
  expect(page.nodes[0]).toMatchObject({ characters: ['Maya / Echo', 'Young Maya'], version: '9007199254740993' });
  expect(page.pageInfo).toEqual({ endCursor: 'next', hasNextPage: true });
  expect(request.mock.calls[0]?.[1]).toEqual({ titleId: id, after: 'prior' });
  expect(request.mock.calls[0]?.[0]).toContain('$after: Cursor');
  await expect(fetchTitleCreditsPage(request, id, 'csrf', 'next')).rejects.toThrow('cursor');
  request.mockResolvedValue({ managedCredits: { nodes: [], pageInfo: { endCursor: null, hasNextPage: true } } });
  await expect(fetchTitleCreditsPage(request, id, 'csrf', null)).rejects.toThrow('cursor');
});

it('a final managed credit page exposes no invented next cursor and missing nodes never become empty success', async () => {
  const { fetchTitleCreditsPage } = await import('./api');
  const request = vi.fn<GraphqlRequest>(async () => ({ managedCredits: { nodes: [], pageInfo: { endCursor: null, hasNextPage: false } } }));
  expect(await fetchTitleCreditsPage(request, id, 'csrf', 'last')).toEqual({ nodes: [], pageInfo: { endCursor: null, hasNextPage: false } });
  request.mockResolvedValue({ managedCredits: { pageInfo: { endCursor: null, hasNextPage: false } } });
  await expect(fetchTitleCreditsPage(request, id, 'csrf', null)).rejects.toThrow('data');
});

it('rejects malformed credit identities, Long versions, role flags and character arrays instead of inventing display values', async () => {
  const { fetchTitleCreditsPage } = await import('./api');
  const credit = { id, personId: id, personName: 'One', characters: ['Maya / Echo'], version: '9',
    role: { id, label: 'Actor', category: 'CAST', allowsCharacters: true } };
  for (const node of [{ ...credit, id: null }, { ...credit, version: '9223372036854775808' },
    { ...credit, characters: [null] }, { ...credit, role: { ...credit.role, allowsCharacters: 'true' } }]) {
    const request = vi.fn<GraphqlRequest>(async () => ({ managedCredits: { nodes: [node], pageInfo: { endCursor: null, hasNextPage: false } } }));
    await expect(fetchTitleCreditsPage(request, id, 'csrf', null)).rejects.toThrow();
  }
});

it('checks each unique loaded credit person through actual owner state without inferring archival from picker absence', async () => {
  const { fetchTitleCreditsPage } = await import('./api');
  const second = '22222222-2222-4222-8222-222222222222';
  const node = { id, personId: id, personName: 'One', characters: [], version: '0',
    role: { id, label: 'Director', category: 'CREATOR', allowsCharacters: false } };
  const request = vi.fn<GraphqlRequest>(async (query, variables) => query.includes('query ManagedCredits(')
    ? { managedCredits: { nodes: [node, { ...node, id: second }], pageInfo: { endCursor: null, hasNextPage: false } } }
    : { managedPerson: { id: variables.id, state: 'ARCHIVED' } });
  const page = await fetchTitleCreditsPage(request, id, 'csrf', null);
  expect(page.nodes.map((credit) => credit.personArchived)).toEqual([true, true]);
  expect(page.nodes[0]?.version).toBe('0');
  expect(request.mock.calls.filter((call) => call[0].includes('query ManagedCreditPerson'))).toHaveLength(1);
  expect(request.mock.calls[1]?.[1]).toEqual({ id });
  expect(request.mock.calls[1]?.[0]).toContain('managedPerson(id: $id) { id state }');
});

it('only an exact current ACTIVE person enables editing; missing, failed and mismatched reads stay explicitly unknown', async () => {
  const { fetchTitleCreditsPage } = await import('./api');
  const node = { id, personId: id, personName: 'One', characters: [], version: '1',
    role: { id, label: 'Director', category: 'CREATOR', allowsCharacters: false } };
  for (const person of [null, { id, state: 'ACTIVE' }, { id: '22222222-2222-4222-8222-222222222222', state: 'ACTIVE' }]) {
    const request = vi.fn<GraphqlRequest>(async (query) => query.includes('query ManagedCredits(')
      ? { managedCredits: { nodes: [node], pageInfo: { endCursor: null, hasNextPage: false } } }
      : { managedPerson: person });
    const page = await fetchTitleCreditsPage(request, id, 'csrf', null);
    expect(page.nodes[0]?.personArchived).toBe(person?.id === id ? false : undefined);
  }
  const failing = vi.fn<GraphqlRequest>(async (query) => {
    if (query.includes('query ManagedCredits(')) return { managedCredits: { nodes: [node], pageInfo: { endCursor: null, hasNextPage: false } } };
    throw new Error('Owner unavailable');
  });
  expect((await fetchTitleCreditsPage(failing, id, 'csrf', null)).nodes[0]?.personArchived).toBeUndefined();
});
