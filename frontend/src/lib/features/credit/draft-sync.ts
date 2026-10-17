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

  constructor(
    private readonly source: DraftSource,
    private readonly titleId: string,
  ) {}

  reset(catalogOperationId: string | null = null): void {
    this.draftId = globalThis.crypto.randomUUID();
    this.catalogOperationId = catalogOperationId;
    this.expectedVersion = null;
    globalThis.clearTimeout(this.timer);
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
      await this.source.saveDraft(
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
    } catch {
      /* autosave is best-effort until explicit submit */
    }
  }

  dispose(): void {
    globalThis.clearTimeout(this.timer);
  }
}
