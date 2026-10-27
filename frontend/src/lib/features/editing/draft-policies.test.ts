import { expect, it, vi } from 'vitest';
import { CreditDraftSync } from '../credit/draft-sync';
import { EMPTY_MOVIE_DRAFT } from '../movie/types';
import { MovieDraftSync } from '../movie/draft-sync';
import { PersonDraftSync } from '../person/write/draft-sync';
import type { DraftSource } from '../draft/types';
import { TransportFailure } from '../../transport/graphql';

const context = { accountId: 'account', csrf: 'csrf', policyRevision: 'p1' };
function source(): DraftSource {
  return {
    listDrafts: async () => [], recoverOperation: async () => null,
    saveDraft: async (operationId, draftId) => ({ receipt: { operationId, state: 'SUCCEEDED', resultId: draftId, version: '1', resultExpired: false }, problemCode: null }),
    discardDraft: async (operationId, draftId) => ({ receipt: { operationId, state: 'SUCCEEDED', resultId: draftId, version: '2', resultExpired: false }, discarded: true, problemCode: null }),
  };
}

it('keeps strict credit draft identity separate from ordinary owner draft receipts', async () => {
  const transport = source();
  transport.saveDraft = async (operationId) => ({ receipt: { operationId, state: 'SUCCEEDED', resultId: 'wrong-draft', version: '1', resultExpired: false }, problemCode: null });
  const changed = vi.fn();
  const credit = new CreditDraftSync(transport, 'title', changed);
  await credit.save({ titleId: 'title', personId: 'person', roleId: 'role', characters: [] }, context, null);
  expect(credit.snapshot.backed).toBe(false);
  expect(changed).toHaveBeenCalled();
  const person = new PersonDraftSync(transport, 'person'); person.reset();
  await person.save({ name: 'Alex' }, context, '1');
  expect(person.snapshot.backed).toBe(true);
  credit.dispose(); person.dispose();
});

it('records a confirmed fresh movie rejection without losing the ability to correct and back up', async () => {
  const transport = source();
  const success = transport.saveDraft;
  transport.saveDraft = vi.fn().mockRejectedValueOnce(new TransportFailure('rejected', 'RATE_LIMITED')).mockImplementation(success);
  const movie = new MovieDraftSync(transport, null); movie.reset();
  await movie.save({ ...EMPTY_MOVIE_DRAFT, title: 'Voyage' }, context, null);
  expect(movie.snapshot).toMatchObject({ backed: false, pendingOperationId: null, failureCode: 'RATE_LIMITED' });
  await movie.save({ ...EMPTY_MOVIE_DRAFT, title: 'Corrected voyage' }, context, null);
  expect(movie.snapshot).toMatchObject({ backed: true, failureCode: null });
  movie.dispose();
});

it('retains the exact credit discard identity until deletion is explicitly confirmed', async () => {
  const transport = source();
  const calls: string[] = [];
  transport.discardDraft = async (operationId, draftId) => {
    calls.push(operationId);
    return { receipt: { operationId, state: 'SUCCEEDED', resultId: draftId, version: '2', resultExpired: false }, discarded: calls.length > 1, problemCode: null };
  };
  const sync = new CreditDraftSync(transport, 'title');
  await sync.save({ titleId: 'title', personId: 'person', roleId: 'role', characters: [] }, context, null);
  expect(await sync.discard(context)).toBe(false);
  expect(await sync.discard(context)).toBe(true);
  expect(calls[0]).toBe(calls[1]);
  expect(sync.snapshot.backed).toBe(false);
  sync.dispose();
});
