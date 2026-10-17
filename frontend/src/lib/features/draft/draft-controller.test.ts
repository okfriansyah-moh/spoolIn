import { afterEach, describe, expect, it, vi } from 'vitest';
import { DraftController } from './controller';
import type {
  DraftDiscardOutcome,
  DraftMutationOutcome,
  DraftRecord,
  DraftReceipt,
  DraftSource,
} from './types';

const accountA = '10000000-0000-4000-8000-000000000001';
const accountB = '20000000-0000-4000-8000-000000000002';
const catalogOp = '30000000-0000-4000-8000-000000000003';
const draftId = '40000000-0000-4000-8000-000000000004';
const policy = 'a'.repeat(64);

function session(accountId: string | null, generation = 0) {
  return Object.freeze({
    accountId,
    csrf: 'A'.repeat(43),
    policyRevision: policy,
    generation,
  });
}

function pending<T>() {
  let resolve: (value: T) => void = () => {
    throw new Error('Uninitialized barrier');
  };
  const promise = new Promise<T>((accept) => {
    resolve = accept;
  });
  return { promise, resolve };
}

function source(
  partial: Partial<DraftSource> & Pick<DraftSource, 'saveDraft'>,
): DraftSource {
  return {
    recoverOperation: async () => null,
    listDrafts: async () => [],
    discardDraft: async () => ({
      receipt: {
        operationId: '00000000-0000-4000-8000-000000000099',
        state: 'SUCCEEDED',
        resultId: draftId,
        version: '1',
        resultExpired: false,
      },
      discarded: true,
      problemCode: null,
    }),
    ...partial,
  };
}

afterEach(() => {
  vi.useRealTimers();
});

describe('T015 draft autosave', () => {
  it('starts autosave only after 750 ms idle', async () => {
    vi.useFakeTimers();
    const saveDraft = vi.fn<DraftSource['saveDraft']>().mockResolvedValue({
      receipt: {
        operationId: '11111111-1111-4111-8111-111111111111',
        state: 'SUCCEEDED',
        resultId: draftId,
        version: '0',
        resultExpired: false,
      },
      problemCode: null,
    });
    const controller = new DraftController(source({ saveDraft }));
    controller.bindSession(session(accountA));
    controller.beginEdit(
      { kind: 'PERSON', id: null },
      { catalogOperationId: catalogOp },
    );
    controller.setName('O’Connor');
    await vi.advanceTimersByTimeAsync(749);
    expect(saveDraft).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1);
    await Promise.resolve();
    expect(saveDraft).toHaveBeenCalledTimes(1);
    expect(saveDraft.mock.calls[0]?.[3].operationId).toBe(catalogOp);
    controller.dispose();
  });

  it('T015-A3 marks unbacked state and keeps in-memory input when autosave fails', async () => {
    vi.useFakeTimers();
    const saveDraft = vi
      .fn<DraftSource['saveDraft']>()
      .mockRejectedValue(new Error('network'));
    const controller = new DraftController(source({ saveDraft }));
    controller.bindSession(session(accountA));
    controller.beginEdit(
      { kind: 'PERSON', id: null },
      { catalogOperationId: catalogOp },
    );
    controller.setName('Retained name');
    await vi.advanceTimersByTimeAsync(750);
    await Promise.resolve();
    expect(controller.snapshot.unbacked).toBe(true);
    expect(controller.snapshot.layers?.draft.name).toBe('Retained name');
    expect(controller.snapshot.message).toContain('not backed up');
    controller.dispose();
  });
});

describe('T015 account isolation', () => {
  it('T015-A2 rejects a delayed save response after switching to another account', async () => {
    const held = pending<DraftMutationOutcome>();
    const saveDraft = vi
      .fn<DraftSource['saveDraft']>()
      .mockReturnValueOnce(held.promise);
    const controller = new DraftController(source({ saveDraft }));
    controller.bindSession(session(accountA));
    controller.beginEdit({ kind: 'PERSON', id: null });
    controller.setName('Private A');
    vi.useFakeTimers();
    await vi.advanceTimersByTimeAsync(750);
    controller.bindSession(session(accountB, 1));
    held.resolve({
      receipt: {
        operationId: '11111111-1111-4111-8111-111111111111',
        state: 'SUCCEEDED',
        resultId: draftId,
        version: '0',
        resultExpired: false,
      },
      problemCode: null,
    });
    await Promise.resolve();
    expect(controller.snapshot.concealed).toBe(true);
    expect(controller.snapshot.layers).toBeNull();
    controller.dispose();
    vi.useRealTimers();
  });

  it('conceals on session loss and releases object URLs', () => {
    const controller = new DraftController(source({ saveDraft: vi.fn() }));
    controller.bindSession(session(accountA));
    controller.beginEdit({ kind: 'PERSON', id: null });
    controller.trackPreviewUrl('blob:https://localhost/preview-a');
    controller.bindSession(session(null));
    expect(controller.snapshot.concealed).toBe(true);
    expect(controller.releasedHandleCount).toBe(1);
    controller.dispose();
  });
});

describe('T015 recovery ordering', () => {
  it('T015-A1 reconciles the known Core operation before listing current drafts', async () => {
    const coreReceipt: DraftReceipt = {
      operationId: '11111111-1111-4111-8111-111111111111',
      state: 'SUCCEEDED',
      resultId: draftId,
      version: '0',
      resultExpired: false,
    };
    const currentDraft: DraftRecord = {
      id: draftId,
      targetKind: 'PERSON',
      targetId: null,
      baseVersion: null,
      values: { name: 'Current server name', biography: null },
      operationId: catalogOp,
      version: '8',
      expiresAt: '2099-01-01T00:00:00Z',
    };
    const recoverOperation = vi
      .fn<DraftSource['recoverOperation']>()
      .mockResolvedValue(coreReceipt);
    const listDrafts = vi
      .fn<DraftSource['listDrafts']>()
      .mockResolvedValue([currentDraft]);
    const held = pending<DraftMutationOutcome>();
    const saveDraft = vi
      .fn<DraftSource['saveDraft']>()
      .mockReturnValueOnce(held.promise);
    const controller = new DraftController(
      source({
        saveDraft,
        recoverOperation,
        listDrafts,
      }),
    );
    controller.bindSession(session(accountA));
    controller.beginEdit(
      { kind: 'PERSON', id: null },
      { catalogOperationId: catalogOp },
    );
    vi.useFakeTimers();
    controller.setName('Pending save');
    await vi.advanceTimersByTimeAsync(750);
    for (let step = 0; step < 5; step += 1) await Promise.resolve();
    expect(saveDraft).toHaveBeenCalled();
    await controller.resumeAfterEligibility({
      permitted: true,
      message: '',
    });
    const recoverIndex =
      controller.requestSequence.indexOf('recover-operation');
    const draftsIndex = controller.requestSequence.indexOf('list-drafts');
    expect(recoverIndex).toBeGreaterThanOrEqual(0);
    expect(draftsIndex).toBeGreaterThan(recoverIndex);
    expect(controller.snapshot.layers?.draft.name).toBe('Current server name');
    expect(controller.snapshot.catalogOperationId).toBe(catalogOp);
    held.resolve({
      receipt: coreReceipt,
      problemCode: null,
    });
    controller.dispose();
    vi.useRealTimers();
  });

  it('does not claim discard success before a confirmed outcome', async () => {
    vi.useFakeTimers();
    const discardDraft = vi
      .fn<DraftSource['discardDraft']>()
      .mockResolvedValue({
        receipt: {
          operationId: '55555555-5555-4555-8555-555555555555',
          state: 'PENDING',
          resultId: null,
          version: null,
          resultExpired: false,
        },
        discarded: false,
        problemCode: null,
      } satisfies DraftDiscardOutcome);
    const saveDraft = vi.fn<DraftSource['saveDraft']>().mockResolvedValue({
      receipt: {
        operationId: '11111111-1111-4111-8111-111111111111',
        state: 'SUCCEEDED',
        resultId: draftId,
        version: '2',
        resultExpired: false,
      },
      problemCode: null,
    });
    const controller = new DraftController(source({ saveDraft, discardDraft }));
    controller.bindSession(session(accountA));
    controller.beginEdit({ kind: 'PERSON', id: null });
    controller.setName('Discard me');
    await vi.advanceTimersByTimeAsync(750);
    await Promise.resolve();
    await controller.discard();
    expect(controller.snapshot.notice).toBe('discard-failed');
    expect(controller.snapshot.message).toBe('Discard did not complete.');
    controller.dispose();
    vi.useRealTimers();
  });
});
