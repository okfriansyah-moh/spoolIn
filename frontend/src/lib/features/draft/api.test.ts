import { expect, it, vi } from 'vitest';
import { createDraftSource } from './api';
import { discardOutcome, receiptState, saveOutcome } from './outcome';
import { movieMutationOutcome, recoverMovieOperation } from '../movie/outcome';
import type { GraphqlRequest } from '../../transport/graphql';

const operationId = '00000000-0000-4000-8000-000000000001';
const draftId = '00000000-0000-4000-8000-000000000002';
const otherId = '00000000-0000-4000-8000-000000000003';
const signal = new AbortController().signal;
function operation(overrides: Record<string, unknown> = {}) {
  return {
    id: operationId,
    state: 'SUCCEEDED',
    resultId: draftId,
    version: '9007199254740993',
    resultExpired: false,
    problem: null,
    ...overrides,
  };
}
function requestReturning(value: unknown) {
  return vi.fn<GraphqlRequest>(async () => value);
}

it('lists immutable retained drafts with identity, version and optional recovery information', async () => {
  const request = requestReturning({
    drafts: [
      {
        id: draftId,
        targetKind: 'MOVIE',
        targetId: otherId,
        baseVersion: '3',
        values: { title: 'Film' },
        operationId,
        version: '4',
        expiresAt: '2099-01-01T00:00:00Z',
      },
    ],
  });
  const drafts = await createDraftSource(request).listDrafts('csrf', signal);
  expect(drafts).toEqual([
    {
      id: draftId,
      targetKind: 'MOVIE',
      targetId: otherId,
      baseVersion: '3',
      values: { title: 'Film' },
      operationId,
      version: '4',
      expiresAt: '2099-01-01T00:00:00Z',
    },
  ]);
  expect(Object.isFrozen(drafts)).toBe(true);
  expect(Object.isFrozen(drafts[0])).toBe(true);
  expect(request).toHaveBeenCalledWith(
    expect.stringContaining('query'),
    {},
    { csrf: 'csrf', signal },
  );
});

it.each([
  null,
  {},
  { drafts: {} },
  {
    drafts: [
      {
        id: draftId,
        targetKind: '',
        version: '1',
        expiresAt: 'tomorrow',
        values: {},
      },
    ],
  },
  {
    drafts: [
      {
        id: 'bad',
        targetKind: 'MOVIE',
        version: '1',
        expiresAt: 'tomorrow',
        values: {},
      },
    ],
  },
])('rejects malformed retained-draft lists: %j', async (response) => {
  await expect(
    createDraftSource(requestReturning(response)).listDrafts('csrf', signal),
  ).rejects.toThrow();
});

it('saves a new draft with explicit null optional fields and no invented expected version', async () => {
  const request = requestReturning({
    saveDraft: { operation: operation(), problem: null },
  });
  const result = await createDraftSource(request).saveDraft(
    operationId,
    draftId,
    null,
    {
      targetKind: 'PERSON',
      values: { name: 'Cher' },
      policyRevision: 'revision',
    },
    'csrf',
    signal,
  );
  expect(result.receipt.version).toBe('9007199254740993');
  expect(request).toHaveBeenCalledWith(
    expect.stringContaining('mutation'),
    {
      operationId,
      id: draftId,
      input: {
        targetKind: 'PERSON',
        targetId: null,
        baseVersion: null,
        values: { name: 'Cher' },
        operationId: null,
        policyRevision: 'revision',
      },
    },
    { mutation: true, csrf: 'csrf', signal },
  );
});

it('updates a draft against its expected version and preserves target and original operation', async () => {
  const request = requestReturning({
    saveDraft: {
      operation: operation({ state: 'FAILED' }),
      problem: { code: 'STALE' },
    },
  });
  const result = await createDraftSource(request).saveDraft(
    operationId,
    draftId,
    '4',
    {
      targetKind: 'MOVIE',
      targetId: otherId,
      baseVersion: '2',
      values: { title: 'Retained' },
      operationId: otherId,
      policyRevision: 'revision',
    },
    'csrf',
    signal,
  );
  expect(result.problemCode).toBe('STALE');
  expect(request.mock.calls[0]?.[1]).toEqual({
    operationId,
    id: draftId,
    expectedVersion: '4',
    input: {
      targetKind: 'MOVIE',
      targetId: otherId,
      baseVersion: '2',
      values: { title: 'Retained' },
      operationId: otherId,
      policyRevision: 'revision',
    },
  });
});

it('discard requires the selected draft version and reports whether it was discarded', async () => {
  const request = requestReturning({
    discardDraft: { operation: operation(), discarded: true, problem: null },
  });
  const result = await createDraftSource(request).discardDraft(
    operationId,
    draftId,
    '5',
    'csrf',
    signal,
  );
  expect(result.discarded).toBe(true);
  expect(request).toHaveBeenCalledWith(
    expect.stringContaining('mutation'),
    { operationId, id: draftId, expectedVersion: '5' },
    { mutation: true, csrf: 'csrf', signal },
  );
});

it('recovers the original draft operation and distinguishes an absent receipt', async () => {
  const request = requestReturning({
    operation: operation({ state: 'PENDING', resultId: null, version: null }),
  });
  const source = createDraftSource(request);
  expect(await source.recoverOperation(operationId, 'csrf', signal)).toEqual({
    operationId,
    state: 'PENDING',
    resultId: null,
    version: null,
    resultExpired: false,
  });
  request.mockResolvedValueOnce({ operation: null });
  expect(await source.recoverOperation(operationId, 'csrf', signal)).toBeNull();
  request.mockResolvedValueOnce({ operation: operation({ id: otherId }) });
  await expect(
    source.recoverOperation(operationId, 'csrf', signal),
  ).rejects.toThrow('Draft recovery unavailable.');
});

it.each(['PENDING', 'SUCCEEDED', 'FAILED', 'EXPIRED'] as const)(
  'accepts the documented %s receipt state',
  (state) => {
    expect(receiptState(state)).toBe(state);
  },
);
it.each([undefined, null, 'UNKNOWN', 'succeeded', 1])(
  'rejects unknown receipt states: %j',
  (state) => {
    expect(() => receiptState(state)).toThrow();
  },
);

it('rejects crossed operation identities and invalid expiry flags instead of confirming a different save', () => {
  for (const invalid of [
    operation({ id: otherId }),
    operation({ resultExpired: 'false' }),
    operation({ state: 'UNKNOWN' }),
  ]) {
    expect(() =>
      saveOutcome({ operation: invalid, problem: null }, operationId),
    ).toThrow();
  }
  expect(
    discardOutcome(
      {
        operation: operation({ resultId: 4, version: 3 }),
        discarded: 'yes',
        problem: { code: 4 },
      },
      operationId,
    ),
  ).toEqual({
    receipt: {
      operationId,
      state: 'SUCCEEDED',
      resultId: null,
      version: null,
      resultExpired: false,
    },
    discarded: false,
    problemCode: null,
  });
});

it('movie outcomes retain exact string identifiers and map server field paths to visible controls', () => {
  const result = movieMutationOutcome(
    {
      operation: operation({ state: 'FAILED' }),
      problem: {
        code: 'VALIDATION',
        fields: [
          { path: 'values.genre_ids[0]', message: 'Choose an available genre' },
          { path: 'input.release.month', message: 'Review month' },
          { path: 'other', message: 'Review changes' },
          { path: 1, message: null },
        ],
      },
    },
    operationId,
  );
  expect(result.resultMovieId).toBe(draftId);
  expect(result.resultVersion).toBe('9007199254740993');
  expect(result.fieldErrors).toEqual([
    { id: 'movie-genres', message: 'Choose an available genre' },
    { id: 'movie-releaseMonth', message: 'Review month' },
    { id: 'movie-editor-form', message: 'Review changes' },
  ]);
  expect(result.problemCode).toBe('VALIDATION');
});

it('movie recovery distinguishes missing operations and rejects a mismatched original operation', () => {
  expect(recoverMovieOperation(null, operationId)).toBeNull();
  expect(recoverMovieOperation(operation(), operationId)?.state).toBe(
    'SUCCEEDED',
  );
  expect(() =>
    recoverMovieOperation(operation({ id: otherId }), operationId),
  ).toThrow();
  expect(
    movieMutationOutcome(
      {
        operation: operation({ resultId: null, version: null }),
        problem: null,
      },
      operationId,
    ),
  ).toMatchObject({
    resultMovieId: null,
    resultVersion: null,
    problemCode: null,
    fieldErrors: [],
  });
});
