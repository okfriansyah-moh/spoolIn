import { AUTOSAVE_IDLE_MS } from '../draft/types';
import type { DraftSource } from '../draft/types';
import type { WriteContext } from '../movie/types';

export type CreditDraftValues = Readonly<{
  titleId: string;
  personId: string;
  roleId: string;
  characters: readonly string[];
}>;

export class CreditDraftSync {
  private draftId = '';
  private expectedVersion: string | null = null;
  private catalogOperationId: string | null = null;
  private timer: ReturnType<typeof globalThis.setTimeout> | undefined;
  private backed = false;
  private unbacked = false;

  constructor(
    private readonly source: DraftSource,
    private readonly titleId: string,
  ) {}

  reset(catalogOperationId: string | null = null): void {
    this.draftId = globalThis.crypto.randomUUID();
    this.catalogOperationId = catalogOperationId;
    this.expectedVersion = null;
    this.backed = false;
    this.unbacked = false;
    globalThis.clearTimeout(this.timer);
  }

  get snapshot(): Readonly<{ backed: boolean; unbacked: boolean }> {
    return Object.freeze({ backed: this.backed, unbacked: this.unbacked });
  }

  schedule(values: CreditDraftValues, context: WriteContext, baseVersion: string | null): void {
    globalThis.clearTimeout(this.timer);
    this.timer = globalThis.setTimeout(() => {
      void this.save(values, context, baseVersion);
    }, AUTOSAVE_IDLE_MS);
  }

  async save(
    values: CreditDraftValues,
    context: WriteContext,
    baseVersion: string | null,
  ): Promise<void> {
    const operationId = globalThis.crypto.randomUUID();
    try {
      const outcome = await this.source.saveDraft(
        operationId,
        this.draftId,
        this.expectedVersion,
        {
          targetKind: 'CREDIT',
          targetId: this.titleId,
          baseVersion,
          values,
          operationId: this.catalogOperationId,
          policyRevision: context.policyRevision,
        },
        context.csrf,
        new AbortController().signal,
      );
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
      this.unbacked = true;
      this.backed = false;
    }
  }

  dispose(): void {
    globalThis.clearTimeout(this.timer);
  }
}
