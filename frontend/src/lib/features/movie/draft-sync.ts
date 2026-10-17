import { AUTOSAVE_IDLE_MS } from '../draft/types';
import type { DraftSource } from '../draft/types';
import type { MovieDraft } from '../../generated/json-types';
import type { WriteContext } from './types';

export class MovieDraftSync {
  private draftId = '';
  private expectedVersion: string | null = null;
  private catalogOperationId: string | null = null;
  private timer: ReturnType<typeof globalThis.setTimeout> | undefined;
  private pendingOperationId: string | null = null;
  private backed = false;
  private unbacked = false;

  constructor(
    private readonly source: DraftSource,
    private readonly targetId: string | null,
  ) {}

  reset(options: {
    draftId?: string;
    catalogOperationId?: string | null;
    expectedVersion?: string | null;
  } = {}): void {
    this.draftId = options.draftId ?? globalThis.crypto.randomUUID();
    this.catalogOperationId = options.catalogOperationId ?? null;
    this.expectedVersion = options.expectedVersion ?? null;
    this.backed = false;
    this.unbacked = false;
    this.pendingOperationId = null;
    globalThis.clearTimeout(this.timer);
  }

  get snapshot(): Readonly<{ backed: boolean; unbacked: boolean; pendingOperationId: string | null }> {
    return Object.freeze({
      backed: this.backed,
      unbacked: this.unbacked,
      pendingOperationId: this.pendingOperationId,
    });
  }

  schedule(values: MovieDraft, context: WriteContext, baseVersion: string | null): void {
    globalThis.clearTimeout(this.timer);
    this.timer = globalThis.setTimeout(() => {
      void this.save(values, context, baseVersion);
    }, AUTOSAVE_IDLE_MS);
  }

  async save(
    values: MovieDraft,
    context: WriteContext,
    baseVersion: string | null,
  ): Promise<void> {
    const operationId = globalThis.crypto.randomUUID();
    this.pendingOperationId = operationId;
    try {
      const outcome = await this.source.saveDraft(
        operationId,
        this.draftId,
        this.expectedVersion,
        {
          targetKind: 'MOVIE',
          targetId: this.targetId,
          baseVersion,
          values,
          operationId: this.catalogOperationId,
          policyRevision: context.policyRevision,
        },
        context.csrf,
        new AbortController().signal,
      );
      this.pendingOperationId = null;
      if (outcome.receipt.state === 'SUCCEEDED' && outcome.receipt.resultId) {
        this.draftId = outcome.receipt.resultId;
        this.expectedVersion = outcome.receipt.version;
        this.backed = true;
        this.unbacked = false;
        return;
      }
      this.unbacked = true;
      this.backed = false;
    } catch {
      this.pendingOperationId = null;
      this.unbacked = true;
      this.backed = false;
    }
  }

  async discard(context: WriteContext): Promise<boolean> {
    if (!this.expectedVersion) return true;
    const operationId = globalThis.crypto.randomUUID();
    try {
      const outcome = await this.source.discardDraft(
        operationId,
        this.draftId,
        this.expectedVersion,
        context.csrf,
        new AbortController().signal,
      );
      return outcome.receipt.state === 'SUCCEEDED' && outcome.discarded;
    } catch {
      return false;
    }
  }

  dispose(): void {
    globalThis.clearTimeout(this.timer);
  }
}
