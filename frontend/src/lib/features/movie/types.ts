import type { MovieDraft } from '../../generated/json-types';
import type { Policy } from '../../policy/types';

export type ManagedTitleRow = Readonly<{
  id: string;
  kind: 'MOVIE' | 'TV';
  state: 'ACTIVE' | 'ARCHIVED';
  title: string;
  releaseContext: string;
  version: string;
}>;

export type ManagedMovie = Readonly<{
  id: string;
  kind: 'MOVIE' | 'TV';
  state: 'ACTIVE' | 'ARCHIVED';
  title: string;
  originalTitle: string | null;
  synopsis: string;
  genreIds: readonly string[];
  releaseYear: string;
  releaseMonth?: number | null;
  releaseDay?: number | null;
  runtimeMinutes?: number | null;
  originalLanguage?: string | null;
  synopsisLanguage?: string;
  countries?: readonly string[];
  version: string;
  artworkUrl: string | null;
  artworkUnavailable: boolean;
}>;

export type GenreOption = Readonly<{ id: string; label: string }>;

export type FieldError = Readonly<{ id: string; message: string }>;

export type WriteContext = Readonly<{
  accountId: string;
  csrf: string;
  policyRevision: string;
}>;

export type ListState =
  | { kind: 'loading' }
  | { kind: 'failure' }
  | {
      kind: 'ready';
      titles: readonly ManagedTitleRow[];
    };

export type SavePhase =
  | 'idle'
  | 'submitting'
  | 'saved'
  | 'failed'
  | 'uncertain'
  | 'resolved'
  | 'stale'
  | 'target-changed';

export type RetainedDraftChoice = Readonly<{
  id: string;
  label: string;
  expiresAt: string;
}>;

export type EditorState = Readonly<{
  draftChoices: readonly RetainedDraftChoice[];
  draftBaseKnown: boolean;
  phase: SavePhase;
  mode: 'create' | 'edit';
  movieId: string | null;
  saved: ManagedMovie | null;
  current: ManagedMovie | null;
  draft: MovieDraft;
  genres: readonly GenreOption[];
  fieldErrors: readonly FieldError[];
  summaryTitle: string;
  message: string;
  pendingOperationId: string | null;
  staleReviewOpen: boolean;
  mediaOutage: boolean;
  discardDialogOpen: boolean;
}>;

export type MovieWriteSource = Readonly<{
  recoverOperation(
    operationId: string,
    csrf: string,
    signal: AbortSignal,
  ): Promise<import('../session/commands').IdentityOutcome | null>;
}>;

export const EMPTY_MOVIE_DRAFT: MovieDraft = Object.freeze({
  title: '',
  originalTitle: null,
  synopsis: '',
  genreIds: Object.freeze([]),
  release: Object.freeze({ year: null, month: null, day: null }),
  synopsisLanguage: 'und',
  countries: Object.freeze([]),
});

export function policyGuidance(
  policy: Policy,
  field: 'title' | 'synopsis',
): string {
  const bound = policy.textBounds[field];
  if (!bound) return '';
  return `${bound.minCodePoints}–${bound.maxCodePoints} Unicode code points.`;
}
