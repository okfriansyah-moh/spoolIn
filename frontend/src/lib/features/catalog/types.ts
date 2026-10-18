/**
 * T013 – Catalog discovery state types.
 * Criteria maps to URL params: q, kind, genre (repeated), year, month.
 * Generation tracks each request so stale replies are suppressed.
 */
export type Criteria = Readonly<{
  text: string;
  kind: string;
  genres: readonly string[];
  year: string;
  month: string;
}>;

export const EMPTY_CRITERIA: Criteria = Object.freeze({
  text: '',
  kind: '',
  genres: Object.freeze([]) as readonly string[],
  year: '',
  month: '',
});

export type TitleSummary = Readonly<{
  id: string;
  kind: 'MOVIE' | 'TV';
  title: string;
  releaseContext: string | null;
  artworkUrl: string | null;
  displayRelease?: string | null;
  genreLabels?: readonly string[];
}>;

/** Text validation result. */
export type TextValidity =
  { valid: true } | { valid: false; reason: 'too-long'; codePoints: number };

/**
 * Catalog display states corresponding to ATDD-256 state rows.
 * - loading    : request in flight for current criteria
 * - catalog    : ≥1 result nodes present
 * - empty      : successful 0-node reply with no active filters (catalog has no content)
 * - no-match   : successful 0-node reply while at least one filter is active
 * - failure    : network / server error
 * - outdated   : criteria changed; prior response has not arrived yet (stale suppressed)
 * - invalid    : text exceeds maxCodePoints or invalid filter parameters
 */
export type CatalogKind =
  | 'loading'
  | 'catalog'
  | 'empty'
  | 'no-match'
  | 'failure'
  | 'outdated'
  | 'invalid';

export type CatalogState = Readonly<{
  kind: CatalogKind;
  criteria: Criteria;
  titles: readonly TitleSummary[];
  pageInfo: Readonly<{ endCursor: string | null; hasNextPage: boolean }> | null;
  /** For invalid state: which field has correction */
  invalidField: 'text' | 'month' | null;
  loadingMore?: boolean;
  pageError?: string | null;
}>;

export const MAX_SEARCH_CODE_POINTS = 200;
