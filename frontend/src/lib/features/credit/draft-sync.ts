import { AUTOSAVE_IDLE_MS } from '../draft/types';
import type { DraftSource, DraftRecord, DraftReceipt } from '../draft/types';
import type { WriteContext } from '../movie/types';

export type CreditDraftValues = Readonly<{
  titleId: string;
  personId: string;
  roleId: string;
  characters: readonly string[];
}>;


type CreditDraftReset = {
  draftId?: string;
  targetId?: string | null;
  catalogOperationId?: string | null;
  expectedVersion?: string | null;
};

type PendingDraft = Readonly<{
  operationId: string;
  draftId: string;
  expectedVersion: string | null;
  input: {
    targetKind: string;
    targetId: string | null;
    baseVersion: string | null;
    values: CreditDraftValues;
    operationId: string | null;
    policyRevision: string;
  };
}>;

export class CreditDraftSync {
  private draftId = '';
  private targetId: string | null = null;
  private expectedVersion: string | null = null;
  private catalogOperationId: string | null = null;
  private timer: ReturnType<typeof globalThis.setTimeout> | undefined;
  private pending: PendingDraft | null = null;
  private pendingDiscard: {
    operationId: string;
    draftId: string;
    expectedVersion: string;
  } | null = null;
  private backed = false;
  private unbacked = false;
  private queue: Promise<void> = Promise.resolve();

  constructor(
    private readonly source: DraftSource,
    private readonly parentTitleId: string,
    private readonly onChange: (state: Readonly<{ backed: boolean; unbacked: boolean; pendingOperationId: string | null }>) => void = () => {},
  ) { this.reset(); }

  reset(
    options: CreditDraftReset | string = {},
  ): void {
    const selected: CreditDraftReset = typeof options === 'string' ? { catalogOperationId: options } : options;
    globalThis.clearTimeout(this.timer);
    if (this.pending || this.pendingDiscard) return;
    this.draftId = selected.draftId ?? globalThis.crypto.randomUUID();
    this.catalogOperationId = selected.catalogOperationId ?? null;
    this.targetId = selected.targetId ?? null;
    this.expectedVersion = selected.expectedVersion ?? null;
    this.backed = false;
    this.unbacked = false;
  }

  get snapshot() {
    return Object.freeze({
      backed: this.backed,
      unbacked: this.unbacked,
      pendingOperationId: this.pending?.operationId ?? null,
    });
  }

  async load(
    context: WriteContext,
    signal: AbortSignal,
  ): Promise<readonly DraftRecord[]> {
    const drafts = await this.source.listDrafts(context.csrf, signal);
    return drafts.filter(
      (draft) =>
        draft.targetKind === 'CREDIT' &&
        (draft.values as Partial<CreditDraftValues>).titleId === this.parentTitleId &&
        Date.parse(draft.expiresAt) > Date.now(),
    );
  }

  adopt(draft: DraftRecord): void {
    this.reset({
      draftId: draft.id,
      targetId: draft.targetId,
      expectedVersion: draft.version,
      catalogOperationId: draft.operationId,
    });
    this.backed = true;
    this.onChange(this.snapshot);
  }

  schedule(
    values: CreditDraftValues,
    context: WriteContext,
    baseVersion: string | null,
  ): void {
    globalThis.clearTimeout(this.timer);
    this.timer = globalThis.setTimeout(() => {
      void this.save(values, context, baseVersion);
    }, AUTOSAVE_IDLE_MS);
  }

  async prepareSubmission(
    operationId: string,
    values: CreditDraftValues,
    context: WriteContext,
    baseVersion: string | null,
  ): Promise<boolean> {
    globalThis.clearTimeout(this.timer);
    this.catalogOperationId = operationId;
    await this.save(values, context, baseVersion);
    return this.backed && !this.pending;
  }

  async clearSubmission(values: CreditDraftValues, context: WriteContext, baseVersion: string | null): Promise<void> {
    this.catalogOperationId = null;
    await this.save(values, context, baseVersion);
  }

  async retryBacking(context: WriteContext): Promise<boolean> {
    await this.queue;
    const result = await this.reconcile(context);
    this.onChange(this.snapshot);
    return result && this.backed;
  }

  save(
    values: CreditDraftValues,
    context: WriteContext,
    baseVersion: string | null,
  ): Promise<void> {
    const next = this.queue.then(() =>
      this.saveSerial(values, context, baseVersion),
    );
    this.queue = next.catch(() => {});
    return next.finally(() => this.onChange(this.snapshot));
  }

  private async saveSerial(
    values: CreditDraftValues,
    context: WriteContext,
    baseVersion: string | null,
  ): Promise<void> {
    this.backed = false;
    if (this.pendingDiscard) {
      this.unbacked = true;
      return;
    }
    try {
      if (this.pending && !(await this.reconcile(context))) {
        this.unbacked = true;
        return;
      }
      this.backed = false;
      this.pending = {
        operationId: globalThis.crypto.randomUUID(),
        draftId: this.draftId,
        expectedVersion: this.expectedVersion,
        input: {
          targetKind: 'CREDIT',
          targetId: this.targetId,
          baseVersion,
          values,
          operationId: this.catalogOperationId,
          policyRevision: context.policyRevision,
        },
      };
      const outcome = await this.sendPending(context);
      this.accept(outcome.receipt);
    } catch {
      this.unbacked = true;
    }
  }

  private async sendPending(context: WriteContext) {
    const pending = this.pending;
    if (!pending) throw new Error('No original draft intent.');
    return this.source.saveDraft(
      pending.operationId,
      pending.draftId,
      pending.expectedVersion,
      pending.input,
      context.csrf,
      new AbortController().signal,
    );
  }

  private accept(receipt: DraftReceipt): boolean {
    if (!this.pending || receipt.operationId !== this.pending.operationId) {
      this.unbacked = true;
      return false;
    }
    if (
      receipt.state !== 'SUCCEEDED' ||
      receipt.resultExpired ||
      receipt.resultId !== this.pending.draftId ||
      !receipt.version
    ) {
      this.unbacked = true;
      if (receipt.state === 'FAILED') this.pending = null;
      return false;
    }
    this.draftId = receipt.resultId;
    this.expectedVersion = receipt.version;
    this.pending = null;
    this.backed = true;
    this.unbacked = false;
    return true;
  }

  private async reconcile(context: WriteContext): Promise<boolean> {
    const pending = this.pending;
    if (!pending) return true;
    const receipt = await this.source.recoverOperation(
      pending.operationId,
      context.csrf,
      new AbortController().signal,
    );
    if (receipt) return this.accept(receipt);
    const outcome = await this.sendPending(context);
    return this.accept(outcome.receipt);
  }

  async discard(context: WriteContext): Promise<boolean> {
    globalThis.clearTimeout(this.timer);
    await this.queue;
    if (this.pending && !(await this.reconcile(context))) return false;
    if (!this.expectedVersion) { this.reset(); return true; }
    this.pendingDiscard ??= {
      operationId: globalThis.crypto.randomUUID(),
      draftId: this.draftId,
      expectedVersion: this.expectedVersion,
    };
    const pending = this.pendingDiscard;
    try {
      const outcome = await this.source.discardDraft(
        pending.operationId,
        pending.draftId,
        pending.expectedVersion,
        context.csrf,
        new AbortController().signal,
      );
      if (
        outcome.receipt.operationId !== pending.operationId ||
        outcome.receipt.resultExpired
      )
        return false;
      if (outcome.receipt.state === 'FAILED') {
        this.pendingDiscard = null;
        return false;
      }
      if (outcome.receipt.state !== 'SUCCEEDED' || outcome.receipt.resultId !== pending.draftId || !outcome.discarded) return false;
      this.pendingDiscard = null;
      this.reset();
      return true;
    } catch {
      return false;
    }
  }

  dispose(): void {
    globalThis.clearTimeout(this.timer);
  }
}
