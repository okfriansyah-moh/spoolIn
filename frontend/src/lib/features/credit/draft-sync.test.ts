import { afterEach, describe, expect, it, vi } from 'vitest';
import { CreditDraftSync } from './draft-sync';
import type { DraftSource } from '../draft/types';

describe('CreditDraftSync', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('autosaves credit draft values through DraftSource', async () => {
    vi.useFakeTimers();
    const saveDraft = vi.fn<DraftSource['saveDraft']>(
      async (operationId, draftId) => ({
        receipt: {
          operationId,
          state: 'SUCCEEDED',
          resultId: draftId,
          version: '1',
          resultExpired: false,
        },
        problemCode: null,
      }),
    );
    const source = {
      recoverOperation: vi.fn(),
      listDrafts: vi.fn(),
      saveDraft,
      discardDraft: vi.fn(),
    };
    const sync = new CreditDraftSync(source, 'title-1');
    sync.reset('op-1');
    sync.schedule(
      {
        titleId: 'title-1',
        personId: 'person-1',
        roleId: 'role-1',
        characters: ['Ada'],
      },
      {
        accountId: '10000000-0000-4000-8000-000000000001',
        csrf: 'A'.repeat(43),
        policyRevision: 'policy-1',
      },
      'v1',
    );
    await vi.runAllTimersAsync();
    expect(saveDraft).toHaveBeenCalledTimes(1);
    const payload = saveDraft.mock.calls[0]?.[3];
    expect(payload?.targetKind).toBe('CREDIT');
    expect(payload?.targetId).toBeNull();
    expect(payload?.values).toMatchObject({ personId: 'person-1' });
    sync.dispose();
  });
});


const draftContext = { accountId: '10000000-0000-4000-8000-000000000001', csrf: 'csrf', policyRevision: 'r1' };
const values = { titleId: 'title', personId: 'person', roleId: 'role', characters: ['Maya / Echo', 'Young Maya'] };
function creditSource(): DraftSource {
  return { listDrafts: async () => [], recoverOperation: async () => null,
    saveDraft: async (operationId, draftId) => ({ receipt: { operationId, state: 'SUCCEEDED',
      resultId: draftId, version: '1', resultExpired: false }, problemCode: null }),
    discardDraft: async (operationId, draftId) => ({ receipt: { operationId, state: 'SUCCEEDED',
      resultId: draftId, version: '2', resultExpired: false }, discarded: true, problemCode: null }) };
}

it('retains uncertain original Core intent and reconciles before a new draft version', async () => {
  const source = creditSource();
  const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
  save.mockRejectedValueOnce(new Error('Lost committed response'));
  source.saveDraft = save;
  const recover = vi.fn<DraftSource['recoverOperation']>(async (operationId) => ({ operationId,
    state: 'SUCCEEDED', resultId: String(save.mock.calls[0]?.[1]), version: '1', resultExpired: false }));
  source.recoverOperation = recover;
  const sync = new CreditDraftSync(source, 'title');
  await sync.save(values, draftContext, '1');
  expect(sync.snapshot.unbacked).toBe(true);
  const original = save.mock.calls[0];
  await sync.prepareSubmission('catalog-original', values, draftContext, '1');
  expect(recover).toHaveBeenCalledWith(original?.[0], 'csrf', expect.any(AbortSignal));
  expect(save.mock.calls[1]?.[1]).toBe(original?.[1]);
  expect(save.mock.calls[1]?.[2]).toBe('1');
  expect(save.mock.calls[1]?.[3].operationId).toBe('catalog-original');
  expect(save.mock.calls[1]?.[0]).not.toBe('catalog-original');
  sync.dispose();
});

it('adopts only exact target current drafts without replacing the retained draft identity', async () => {
  const source = creditSource();
  const own = { id: 'retained-draft', targetKind: 'CREDIT', targetId: null, baseVersion: '1', values,
    operationId: null, version: '7', expiresAt: new Date(Date.now() + 60000).toISOString() };
  source.listDrafts = async () => [own, { ...own, targetKind: 'PERSON' }, { ...own, values: { ...values, titleId: 'different' } },
    { ...own, expiresAt: '2000-01-01T00:00:00Z' }];
  const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
  source.saveDraft = save;
  const sync = new CreditDraftSync(source, 'title');
  expect(await sync.load(draftContext, new AbortController().signal)).toEqual([own]);
  sync.adopt(own);
  await sync.save(values, draftContext, '1');
  expect(save.mock.calls[0]?.[1]).toBe('retained-draft');
  expect(save.mock.calls[0]?.[2]).toBe('7');
  sync.dispose();
});

it('retries uncertain discard under its original operation', async () => {
  const source = creditSource();
  const sync = new CreditDraftSync(source, 'title');
  await sync.save(values, draftContext, '1');
  const discard = vi.fn<DraftSource['discardDraft']>(source.discardDraft);
  discard.mockImplementationOnce(async (operationId, draftId) => ({ receipt: { operationId, state: 'PENDING',
    resultId: draftId, version: '2', resultExpired: false }, discarded: false, problemCode: null }));
  source.discardDraft = discard;
  expect(await sync.discard(draftContext)).toBe(false);
  expect(await sync.discard(draftContext)).toBe(true);
  expect(discard.mock.calls[1]?.slice(0, 3)).toEqual(discard.mock.calls[0]?.slice(0, 3));
  sync.dispose();
});


it('a wrong original draft identity stays unbacked and cannot authorize a fresh Core save', async () => {
  const source = creditSource();
  const save = vi.fn<DraftSource['saveDraft']>(async (operationId) => ({ receipt: { operationId, state: 'SUCCEEDED',
    resultId: 'wrong-draft', version: '1', resultExpired: false }, problemCode: null }));
  source.saveDraft = save;
  const sync = new CreditDraftSync(source, 'title');
  await sync.save(values, draftContext, '1');
  expect(sync.snapshot.unbacked).toBe(true);
  await sync.save(values, draftContext, '1');
  expect(save.mock.calls[1]?.slice(0, 3)).toEqual(save.mock.calls[0]?.slice(0, 3));
  expect(sync.snapshot.backed).toBe(false);
  sync.dispose();
});


it('backs exact existing credit identity and version rather than its parent movie', async () => {
  const source = creditSource();
  const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
  source.saveDraft = save;
  const sync = new CreditDraftSync(source, 'title');
  sync.adopt({ id: 'edit-draft', targetKind: 'CREDIT', targetId: 'exact-credit', baseVersion: '9007199254740993',
    values, operationId: null, version: '7', expiresAt: new Date(Date.now() + 60000).toISOString() });
  await sync.prepareSubmission('catalog-original', values, draftContext, '9007199254740993');
  expect(save.mock.calls[0]?.slice(1, 3)).toEqual(['edit-draft', '7']);
  expect(save.mock.calls[0]?.[3]).toMatchObject({ targetId: 'exact-credit', baseVersion: '9007199254740993',
    operationId: 'catalog-original', values });
  sync.dispose();
});

it('requires the actual discarded effect before resetting retained identity', async () => {
  const source = creditSource();
  const sync = new CreditDraftSync(source, 'title');
  await sync.save(values, draftContext, '1');
  source.discardDraft = async (operationId, draftId) => ({ receipt: { operationId, state: 'SUCCEEDED',
    resultId: draftId, version: '2', resultExpired: false }, discarded: false, problemCode: null });
  expect(await sync.discard(draftContext)).toBe(false);
  expect(sync.snapshot.backed).toBe(true);
  sync.dispose();
});


it('loads a retained Catalog binding without inventing or resending its original mutation', async () => {
  const source = creditSource();
  const retained = { id: 'retained-edit', targetKind: 'CREDIT', targetId: 'exact-credit', baseVersion: '9', values,
    operationId: 'catalog-original', version: '4', expiresAt: new Date(Date.now() + 60000).toISOString() };
  source.listDrafts = async () => [retained];
  const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
  source.saveDraft = save;
  const sync = new CreditDraftSync(source, 'title');
  const drafts = await sync.load(draftContext, new AbortController().signal);
  sync.adopt(drafts[0]!);
  expect(drafts[0]?.operationId).toBe('catalog-original');
  expect(save).not.toHaveBeenCalled();
  expect(sync.snapshot).toMatchObject({ backed: true, unbacked: false });
  sync.dispose();
});
