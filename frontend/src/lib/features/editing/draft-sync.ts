import { TransportFailure } from '../../transport/graphql';
import type { DraftValues } from '../../generated/json-types';
import { AUTOSAVE_IDLE_MS } from '../draft/types';
import type { DraftSource, DraftRecord, DraftReceipt, DraftDiscardOutcome } from '../draft/types';
import type { WriteContext } from '../movie/types';

export type DraftSnapshot = Readonly<{ backed: boolean; unbacked: boolean; pendingOperationId: string | null }>;
export type DraftReset = { draftId?: string; targetId?: string | null; catalogOperationId?: string | null; expectedVersion?: string | null };
type PendingDiscard = { operationId: string; draftId: string; expectedVersion: string };
type DraftBehavior = {
  kind: string;
  matches: (draft: DraftRecord) => boolean;
  strictIdentity?: boolean;
  resetTarget?: boolean;
  resetEmptyDiscard?: boolean;
  continueAfterRejection?: boolean;
  rejectedCodes?: ReadonlySet<string>;
  onChange?: (snapshot: DraftSnapshot) => void;
};

type PendingDraft<T extends DraftValues> = Readonly<{
  operationId: string;
  draftId: string;
  expectedVersion: string | null;
  input: {
    targetKind: string;
    targetId: string | null;
    baseVersion: string | null;
    values: T;
    operationId: string | null;
    policyRevision: string;
  };
}>;

export class DraftSync<T extends DraftValues> {
  private draftId = '';
  private expectedVersion: string | null = null;
  protected catalogOperationId: string | null = null;
  private timer: ReturnType<typeof globalThis.setTimeout> | undefined;
  private pending: PendingDraft<T> | null = null;
  private pendingDiscard: {
    operationId: string;
    draftId: string;
    expectedVersion: string;
  } | null = null;
  protected backed = false;
  private unbacked = false;
  protected failureCode: string | null = null;
  protected queue: Promise<void> = Promise.resolve();

  constructor(
    private readonly source: DraftSource,
    protected targetId: string | null,
    private readonly behavior: DraftBehavior,
  ) {}

  reset(
    options: DraftReset | string = {},
  ): void {
    const selected = typeof options === 'string' ? { catalogOperationId: options } : options;
    globalThis.clearTimeout(this.timer);
    if (this.pending || this.pendingDiscard) return;
    this.draftId = selected.draftId ?? globalThis.crypto.randomUUID();
    this.catalogOperationId = selected.catalogOperationId ?? null;
    this.expectedVersion = selected.expectedVersion ?? null;
    this.backed = false;
    this.unbacked = false;
    this.failureCode = null;
    if (this.behavior.resetTarget) this.targetId = selected.targetId ?? null;
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
        this.behavior.matches(draft) &&
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
    this.notify();
  }

  schedule(
    values: T,
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
    values: T,
    context: WriteContext,
    baseVersion: string | null,
  ): Promise<boolean> {
    globalThis.clearTimeout(this.timer);
    this.catalogOperationId = operationId;
    await this.save(values, context, baseVersion);
    return this.backed && !this.pending;
  }

  save(
    values: T,
    context: WriteContext,
    baseVersion: string | null,
  ): Promise<void> {
    const next = this.queue.then(() =>
      this.saveSerial(values, context, baseVersion),
    );
    this.queue = next.catch(() => {});
    return this.behavior.onChange ? next.finally(() => this.notify()) : next;
  }

  private async saveSerial(
    values: T,
    context: WriteContext,
    baseVersion: string | null,
  ): Promise<void> {
    this.backed = false;
    if (this.pendingDiscard) {
      this.unbacked = true;
      return;
    }
    try {
      if (this.pending) {
        const confirmed = await this.reconcile(context);
        if (!confirmed && (!this.behavior.continueAfterRejection || this.pending)) {
          this.unbacked = true;
          return;
        }
      }
      this.backed = false;
      this.failureCode = null;
      this.pending = {
        operationId: globalThis.crypto.randomUUID(),
        draftId: this.draftId,
        expectedVersion: this.expectedVersion,
        input: {
          targetKind: this.behavior.kind,
          targetId: this.targetId,
          baseVersion,
          values,
          operationId: this.catalogOperationId,
          policyRevision: context.policyRevision,
        },
      };
      const outcome = await this.sendNewPending(context);
      this.failureCode = outcome.problemCode;
      this.accept(outcome.receipt);
    } catch {
      this.unbacked = true;
    }
  }

  private async sendNewPending(context: WriteContext) {
    try {
      return await this.sendPending(context);
    } catch (error) {
      if (
        error instanceof TransportFailure &&
        error.kind === 'rejected' &&
        this.behavior.rejectedCodes?.has(error.code)
      ) {
        this.pending = null;
        this.failureCode = error.code;
      }
      throw error;
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
      if (this.behavior.strictIdentity) this.unbacked = true;
      return false;
    }
    if (
      receipt.state !== 'SUCCEEDED' ||
      receipt.resultExpired ||
      !this.acceptsResult(receipt.resultId) ||
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
    this.failureCode = null;
    return true;
  }

  protected async reconcile(context: WriteContext): Promise<boolean> {
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

  private acceptsResult(id: string | null): id is string {
    return this.behavior.strictIdentity ? id === this.pending?.draftId : !!id;
  }

  protected notify(): void {
    this.behavior.onChange?.(this.snapshot);
  }

  private acceptDiscard(outcome: DraftDiscardOutcome, pending: PendingDiscard): boolean {
    const receipt = outcome.receipt;
    if (receipt.operationId !== pending.operationId || receipt.resultExpired) return false;
    if (receipt.state === 'FAILED') {
      this.pendingDiscard = null;
      return false;
    }
    if (receipt.state !== 'SUCCEEDED') return false;
    return !this.behavior.strictIdentity || (receipt.resultId === pending.draftId && outcome.discarded);
  }

  async discard(context: WriteContext): Promise<boolean> {
    globalThis.clearTimeout(this.timer);
    await this.queue;
    if (this.pending && !(await this.reconcile(context))) return false;
    if (!this.expectedVersion) {
      if (this.behavior.resetEmptyDiscard) this.reset();
      return true;
    }
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
      if (!this.acceptDiscard(outcome, pending)) return false;
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
