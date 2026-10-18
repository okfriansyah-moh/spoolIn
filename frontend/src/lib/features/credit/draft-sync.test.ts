import { afterEach, describe, expect, it, vi } from 'vitest';
import { CreditDraftSync } from './draft-sync';
import type { DraftSource } from '../draft/types';

describe('CreditDraftSync', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('autosaves credit draft values through DraftSource', async () => {
    vi.useFakeTimers();
    const saveDraft = vi.fn<DraftSource['saveDraft']>(async (operationId, draftId) => ({
      receipt: { operationId, state: 'SUCCEEDED', resultId: draftId, version: '1', resultExpired: false },
      problemCode: null,
    }));
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
    expect(payload?.values).toMatchObject({ personId: 'person-1' });
    sync.dispose();
  });
});
