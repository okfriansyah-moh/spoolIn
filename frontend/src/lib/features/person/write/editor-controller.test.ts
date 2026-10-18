import { afterEach, describe, expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';

vi.mock('./api', () => ({
  fetchManagedPerson: vi.fn(async () => null),
  createPerson: vi.fn(),
  updatePerson: vi.fn(),
  recoverCatalogOperation: vi.fn(),
}));

import type { DraftSource } from '../../draft/types';
import { TransportFailure } from '../../../transport/graphql';
import { PersonDraftSync } from './draft-sync';
import * as api from './api';
import { PersonEditorController } from './editor-controller';

const policy = specimenPolicy();
const context = Object.freeze({
  accountId: '10000000-0000-4000-8000-000000000001',
  csrf: 'A'.repeat(43),
  policyRevision: policy.revision,
});

describe('PersonEditorController', () => {
  it('rejects empty name before submit', async () => {
    const draftSource = {
      recoverOperation: vi.fn(),
      listDrafts: vi.fn(),
      saveDraft: vi.fn(),
      discardDraft: vi.fn(),
    };
    const controller = new PersonEditorController(
      async () => ({}),
      draftSource,
      'create',
      null,
    );
    let phase = 'idle';
    controller.subscribe((state) => {
      phase = state.phase;
    });
    await controller.bootstrap(policy, context, new AbortController().signal);
    await controller.submit();
    expect(phase).toBe('failed');
  });
});

afterEach(() => vi.clearAllMocks());

function sourceFixture(): DraftSource {
  return {
    listDrafts: async () => [],
    recoverOperation: async () => null,
    saveDraft: async (operationId, draftId) => ({
      receipt: {
        operationId,
        state: 'SUCCEEDED',
        resultId: draftId,
        version: '1',
        resultExpired: false,
      },
      problemCode: null,
    }),
    discardDraft: async (operationId, draftId) => ({
      receipt: {
        operationId,
        state: 'SUCCEEDED',
        resultId: draftId,
        version: '2',
        resultExpired: false,
      },
      discarded: true,
      problemCode: null,
    }),
  };
}

function personDraft() {
  return {
    id: '22222222-2222-4222-8222-222222222222',
    targetKind: 'PERSON',
    targetId: null,
    baseVersion: null,
    values: { name: 'Retained mononym', biography: 'Retained biography' },
    operationId: null,
    version: '11',
    expiresAt: new Date(Date.now() + 3600000).toISOString(),
  };
}

describe('person reload and uncertain saves', () => {
  it('recovers a unique create draft from the authenticated source', async () => {
    const source = sourceFixture();
    source.listDrafts = async () => [
      personDraft(),
      { ...personDraft(), targetKind: 'MOVIE' },
    ];
    const controller = new PersonEditorController(
      vi.fn(),
      source,
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    expect(controller.snapshot.draft).toEqual(personDraft().values);
    controller.dispose();
  });

  it('preserves the original identity after a lost create response', async () => {
    const controller = new PersonEditorController(
      vi.fn(),
      sourceFixture(),
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    controller.updateDraft({ name: 'Remote Maintainer person' });
    vi.mocked(api.createPerson).mockRejectedValueOnce(
      new TransportFailure('uncertain'),
    );
    await controller.submit();
    const original = controller.snapshot.pendingOperationId;
    expect(original).toBeTruthy();
    expect(controller.snapshot.phase).toBe('uncertain');
    const calls = vi.mocked(api.createPerson).mock.calls.length;
    await controller.submit();
    expect(controller.snapshot.pendingOperationId).toBe(original);
    expect(vi.mocked(api.createPerson).mock.calls.length).toBe(calls);
    controller.dispose();
  });

  it('does not treat PENDING as a failed operation eligible for a new create', async () => {
    const controller = new PersonEditorController(
      vi.fn(),
      sourceFixture(),
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    controller.updateDraft({ name: 'Pending person' });
    vi.mocked(api.createPerson).mockResolvedValueOnce({
      receipt: {
        operationId: 'pending',
        state: 'PENDING',
        resultExpired: false,
        problem: null,
      },
      resultPersonId: null,
      resultVersion: null,
      problemCode: null,
    });
    await controller.submit();
    expect(controller.snapshot.phase).toBe('uncertain');
    expect(controller.snapshot.pendingOperationId).toBeTruthy();
    controller.dispose();
  });
});

describe('draft mutation identity and optimistic versions', () => {
  it('replays a lost Core save with the same intent before saving changed values', async () => {
    const source = sourceFixture();
    const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
    save.mockRejectedValueOnce(new TransportFailure('uncertain'));
    source.saveDraft = save;
    const sync = new PersonDraftSync(source, null);
    sync.reset();
    await sync.save({ name: 'First' }, context, null);
    const original = save.mock.calls[0];
    expect(sync.snapshot.pendingOperationId).toBe(original?.[0]);
    await sync.save({ name: 'Second' }, context, null);
    expect(save.mock.calls[1]?.slice(0, 5)).toEqual(original?.slice(0, 5));
    expect(original?.[5]).toBeInstanceOf(AbortSignal);
    expect(save.mock.calls[1]?.[5]).toBeInstanceOf(AbortSignal);
    expect(save.mock.calls[2]?.[0]).not.toBe(original?.[0]);
    expect(save.mock.calls[2]?.[2]).toBe('1');
    expect(save.mock.calls[2]?.[3].values).toEqual({ name: 'Second' });
    sync.dispose();
  });

  it('uses the retained Core draft version independently of Catalog base version', async () => {
    const source = sourceFixture();
    const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
    source.saveDraft = save;
    const sync = new PersonDraftSync(source, 'target');
    sync.adopt({
      ...personDraft(),
      targetId: 'target',
      baseVersion: '9007199254740993',
    });
    await sync.save({ name: 'Updated' }, context, '9007199254740993');
    expect(save.mock.calls[0]?.[2]).toBe('11');
    expect(save.mock.calls[0]?.[3].baseVersion).toBe('9007199254740993');
    sync.dispose();
  });
});

describe('person draft binding after publication', () => {
  it('clears the old Catalog intent before backing subsequent edits', async () => {
    const source = sourceFixture();
    const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
    source.saveDraft = save;
    const sync = new PersonDraftSync(source, 'target');
    sync.reset();
    await sync.prepareSubmission(
      '44444444-4444-4444-8444-444444444444',
      { name: 'Published' },
      context,
      '5',
    );
    await sync.completeSubmission({ name: 'Published' }, context, '6');
    await sync.save({ name: 'Unsaved next edit' }, context, '6');
    expect(save.mock.calls[0]?.[3].operationId).toBe(
      '44444444-4444-4444-8444-444444444444',
    );
    expect(save.mock.calls[1]?.[3].operationId).toBeNull();
    expect(save.mock.calls[2]?.[3].operationId).toBeNull();
    expect(save.mock.calls[2]?.[3].values).toEqual({
      name: 'Unsaved next edit',
    });
    sync.dispose();
  });
});

describe('explicit person recovery decisions', () => {
  it('keeps multiple drafts unselected until an exact retained identity is chosen', async () => {
    const source = sourceFixture();
    const first = personDraft();
    const second = {
      ...first,
      id: '33333333-3333-4333-8333-333333333333',
      version: '22',
      values: { name: 'Second retained person', biography: null },
    };
    source.listDrafts = async () => [first, second];
    const save = vi.fn<DraftSource['saveDraft']>(source.saveDraft);
    source.saveDraft = save;
    const controller = new PersonEditorController(
      vi.fn(),
      source,
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    expect(controller.snapshot.draftChoices).toHaveLength(2);
    await controller.selectRetainedDraft(second.id);
    expect(controller.snapshot.draft.name).toBe('Second retained person');
    await controller.submit();
    expect(save.mock.calls[0]?.[1]).toBe(second.id);
    expect(save.mock.calls[0]?.[2]).toBe('22');
    controller.dispose();
  });

  it('starts a separate draft only after the explicit new-draft action', async () => {
    const source = sourceFixture();
    source.listDrafts = async () => [
      personDraft(),
      { ...personDraft(), id: '33333333-3333-4333-8333-333333333333' },
    ];
    const controller = new PersonEditorController(
      vi.fn(),
      source,
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    expect(controller.snapshot.draftChoices).toHaveLength(2);
    controller.startNewDraft();
    expect(controller.snapshot.draftChoices).toHaveLength(0);
    expect(controller.snapshot.draft.name).toBe('');
    expect(controller.snapshot.message).toMatch(/Other retained drafts remain/);
    controller.dispose();
  });

  it('discloses unavailable historical base values after restoring a stale draft', async () => {
    const person = {
      id: '190db10d-02b8-437e-8e7c-ece9c105fe2a',
      name: 'Current person',
      biography: 'Current biography',
      version: '5',
      state: 'ACTIVE' as const,
    };
    const source = sourceFixture();
    source.listDrafts = async () => [
      { ...personDraft(), targetId: person.id, baseVersion: '3' },
    ];
    vi.mocked(api.fetchManagedPerson).mockResolvedValue(person);
    const controller = new PersonEditorController(
      vi.fn(),
      source,
      'edit',
      person.id,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    expect(controller.snapshot.phase).toBe('stale');
    expect(controller.snapshot.draftBaseKnown).toBe(false);
    await controller.rebaseAfterReview(true);
    expect(controller.snapshot.phase).toBe('idle');
    expect(controller.snapshot.draft.name).toBe('Retained mononym');
    expect(controller.snapshot.saved?.name).toBe('Current person');
    controller.dispose();
  });
});

describe('published create draft consumption', () => {
  it('discards the published create draft and retries an uncertain discard with its original ID', async () => {
    const source = sourceFixture();
    const discard = vi.fn<DraftSource['discardDraft']>(source.discardDraft);
    discard.mockRejectedValueOnce(new TransportFailure('uncertain'));
    source.discardDraft = discard;
    const sync = new PersonDraftSync(source, null);
    sync.adopt(personDraft());
    await sync.completeSubmission({ name: 'Published person' }, context, '1');
    const original = discard.mock.calls[0];
    await sync.completeSubmission({ name: 'Published person' }, context, '1');
    const repeated = discard.mock.calls[1];
    expect(repeated?.slice(0, 4)).toEqual(original?.slice(0, 4));
    expect(original?.[4]).toBeInstanceOf(AbortSignal);
    expect(repeated?.[4]).toBeInstanceOf(AbortSignal);
    expect(original?.[1]).toBe(personDraft().id);
    expect(original?.[2]).toBe('11');
    sync.dispose();
  });
});

describe('person explicit discard', () => {
  it('cancel keeps values and uncertain discard retries the original operation before clearing the selected draft', async () => {
    const source = sourceFixture();
    source.listDrafts = async () => [personDraft()];
    const discard = vi.fn<DraftSource['discardDraft']>(
      async (operationId, draftId) => ({
        receipt: {
          operationId,
          resultId: draftId,
          version: '2',
          state: 'PENDING',
          resultExpired: false,
        },
        discarded: false,
        problemCode: null,
      }),
    );
    source.discardDraft = discard;
    const controller = new PersonEditorController(
      async () => ({}),
      source,
      'create',
      null,
    );
    await controller.bootstrap(policy, context, new AbortController().signal);
    controller.openDiscardDialog();
    controller.closeDiscardDialog();
    expect(controller.snapshot.draft.name).toBe('Retained mononym');
    controller.openDiscardDialog();
    await controller.confirmDiscard();
    expect(controller.snapshot.discardDialogOpen).toBe(true);
    expect(controller.snapshot.draft.name).toBe('Retained mononym');
    const original = discard.mock.calls[0];
    discard.mockImplementation(async (operationId, draftId) => ({
      receipt: {
        operationId,
        resultId: draftId,
        version: '2',
        state: 'SUCCEEDED',
        resultExpired: false,
      },
      discarded: true,
      problemCode: null,
    }));
    await controller.confirmDiscard();
    expect(discard.mock.calls[1]?.slice(0, 3)).toEqual(original?.slice(0, 3));
    expect(controller.snapshot.discardDialogOpen).toBe(false);
    expect(controller.snapshot.draft.name).toBe('');
    controller.dispose();
  });
});

it('an expired successful direct create retains its original intent and never refetches it as a current saved person', async () => {
  const controller = new PersonEditorController(vi.fn(), sourceFixture(), 'create', null);
  await controller.bootstrap(policy, context, new AbortController().signal);
  controller.updateDraft({ name: 'Retained person' });
  vi.mocked(api.createPerson).mockResolvedValueOnce({ receipt: { operationId: 'original', state: 'SUCCEEDED',
    resultExpired: true, problem: null }, resultPersonId: 'result', resultVersion: '1', problemCode: null });
  await controller.submit();
  expect(controller.snapshot.phase).toBe('uncertain');
  expect(controller.snapshot.saved).toBeNull();
  expect(controller.snapshot.pendingOperationId).toBeTruthy();
  const count = vi.mocked(api.createPerson).mock.calls.length;
  await controller.submit();
  expect(vi.mocked(api.createPerson).mock.calls.length).toBe(count);
  controller.dispose();
});
