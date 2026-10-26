import type { DraftSource } from '../draft/types';
import { DraftSync, type DraftSnapshot } from '../editing/draft-sync';
import type { WriteContext } from '../movie/types';

export type CreditDraftValues = Readonly<{ titleId: string; personId: string; roleId: string; characters: readonly string[] }>;

export class CreditDraftSync extends DraftSync<CreditDraftValues> {
  constructor(source: DraftSource, parentTitleId: string, onChange: (snapshot: DraftSnapshot) => void = () => {}) {
    super(source, null, {
      kind: 'CREDIT',
      matches: (draft) => draft.targetKind === 'CREDIT' && (draft.values as Partial<CreditDraftValues>).titleId === parentTitleId,
      strictIdentity: true, resetTarget: true, resetEmptyDiscard: true, onChange,
    });
    this.reset();
  }

  async clearSubmission(values: CreditDraftValues, context: WriteContext, baseVersion: string | null): Promise<void> {
    this.catalogOperationId = null;
    await this.save(values, context, baseVersion);
  }

  async retryBacking(context: WriteContext): Promise<boolean> {
    await this.queue;
    const result = await this.reconcile(context);
    this.notify();
    return result && this.backed;
  }
}
