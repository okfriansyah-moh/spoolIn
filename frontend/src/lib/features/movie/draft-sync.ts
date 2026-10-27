import type { MovieDraft } from '../../generated/json-types';
import type { DraftSource } from '../draft/types';
import { DraftSync } from '../editing/draft-sync';

export class MovieDraftSync extends DraftSync<MovieDraft> {
  constructor(source: DraftSource, targetId: string | null) {
    super(source, targetId, {
      kind: 'MOVIE',
      matches: (draft) => draft.targetKind === 'MOVIE' && draft.targetId === targetId,
      continueAfterRejection: true,
      rejectedCodes: new Set(['FIELD_INVALID', 'VALIDATION_FAILED', 'FORBIDDEN', 'UNAUTHENTICATED', 'RATE_LIMITED']),
    });
  }

  override get snapshot() { return Object.freeze({ ...super.snapshot, failureCode: this.failureCode }); }
}
