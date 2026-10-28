/**
 * T013 – Catalog search controller.
 *
 * Invariants:
 *   - Each new search increments the generation; a response from a stale generation is discarded.
 *   - Text is debounced 250 ms; other filters are applied immediately (new generation).
 *   - Invalid text (> maxCodePoints) moves to 'invalid' state without issuing a query.
 *   - Clearing year also clears month to prevent hidden date restrictions.
 *   - Retry uses the current criteria and generation.
 *   - Criteria are preserved across all states so they stay editable.
 */
import { TransportFailure } from '../../transport/graphql';
import { readPageInfo } from '../../state/pagination';
import type { GraphqlRequest } from '../../transport/graphql';
import type { Criteria, CatalogState, TitleSummary } from './types';
import { EMPTY_CRITERIA, MAX_SEARCH_CODE_POINTS } from './types';
import {
  buildSearchInput,
  validateText,
  hasActiveFilters,
  formatReleaseDate,
} from './filter';
import { fetchTitles } from './api';

const DEBOUNCE_MS = 250;

function invalidCatalogState(criteria: Criteria): CatalogState {
  return {
    kind: 'invalid',
    criteria,
    titles: [],
    pageInfo: null,
    invalidField: 'text',
  };
}

function mapTitleNodes(
  nodes: Awaited<ReturnType<typeof fetchTitles>>['nodes'],
): TitleSummary[] {
  return nodes.map((n) => ({
    id: n.id,
    kind: n.kind as 'MOVIE' | 'TV',
    title: n.title,
    releaseContext: n.releaseContext ?? formatReleaseDate(n.release),
    artworkUrl: n.artwork?.url ?? null,
    displayRelease: formatReleaseDate(n.release),
    genreLabels: n.genres.map((genre) => genre.label),
  }));
}

function catalogResultKind(
  criteria: Criteria,
  titles: TitleSummary[],
): CatalogState['kind'] {
  if (titles.length > 0) return 'catalog';
  return hasActiveFilters(criteria) ? 'no-match' : 'empty';
}

export class CatalogController {
  private generation = 0;
  private currentAfter: string | null = null;
  private active: AbortController | null = null;
  private debounceTimer: ReturnType<typeof globalThis.setTimeout> | undefined;

  private current: CatalogState = $state.raw<CatalogState>({
    kind: 'loading',
    criteria: EMPTY_CRITERIA,
    titles: [],
    pageInfo: null,
    invalidField: null,
  });

  constructor(private readonly request: GraphqlRequest) {}

  get snapshot(): CatalogState {
    return this.current;
  }

  /** Apply new criteria. Text changes are debounced; others fire immediately. */
  setCriteria(next: Criteria, options: { debounce?: boolean } = {}): void {
    // Enforce year→month dependency
    const safe: Criteria = next.year ? next : { ...next, month: '' };

    const textChanged = safe.text !== this.current.criteria.text;
    this.current = {
      ...this.current,
      criteria: safe,
      pageStarts: [null],
      pageNumber: 1,
    };

    globalThis.clearTimeout(this.debounceTimer);
    this.begin();

    if (options.debounce !== false && textChanged) {
      // Show outdated/loading transition while debouncing
      this.publish({ ...this.current, kind: 'outdated' });
      this.debounceTimer = globalThis.setTimeout(() => {
        void this.search(safe);
      }, DEBOUNCE_MS);
    } else {
      void this.search(safe);
    }
  }

  /** Retry current criteria (e.g. after failure). */
  retry(): void {
    void this.search(this.current.criteria, this.currentAfter);
  }

  /** Trigger initial load with the given criteria (no debounce). */
  load(
    criteria: Criteria,
    after: string | null = null,
    starts: readonly (string | null)[] = [null],
  ): void {
    globalThis.clearTimeout(this.debounceTimer);
    const pageStarts =
      after && !starts.includes(after) ? [null, after] : starts;
    this.current = {
      ...this.current,
      criteria,
      pageStarts,
      pageNumber: after ? pageStarts.indexOf(after) + 1 : 1,
    };
    void this.search(criteria, after);
  }

  private pageAvailable(current: CatalogState): boolean {
    return (
      current.kind === 'catalog' &&
      !current.loadingMore &&
      !!current.pageInfo?.hasNextPage
    );
  }

  async loadMore(): Promise<boolean> {
    if (!this.pageAvailable(this.current)) return false;
    return this.changePage('next');
  }

  async previousPage(): Promise<boolean> {
    if ((this.current.pageNumber ?? 1) <= 1 || this.current.loadingMore)
      return false;
    return this.changePage('previous');
  }

  private async changePage(direction: 'next' | 'previous'): Promise<boolean> {
    const current = this.current;
    const starts = current.pageStarts ?? [null];
    const index = (current.pageNumber ?? 1) - 1;
    const after =
      direction === 'next' ? current.pageInfo?.endCursor : starts[index - 1];
    if (after === undefined) return false;
    const generation = this.generation;
    const filter = buildSearchInput(current.criteria);
    if (!filter) return false;
    this.publish({
      ...current,
      loadingMore: true,
      pageDirection: direction,
      pageError: null,
    });
    try {
      const result = await fetchTitles(
        this.request,
        { filter, after },
        this.active?.signal,
      );
      if (generation !== this.generation) return false;
      this.acceptPage(current, result, after, direction);
      return true;
    } catch (error) {
      if (generation === this.generation)
        this.failPage(current, direction, error);
      return false;
    }
  }

  private acceptPage(
    current: CatalogState,
    result: Awaited<ReturnType<typeof fetchTitles>>,
    after: string | null,
    direction: 'next' | 'previous',
  ): void {
    const starts = current.pageStarts ?? [null];
    const index = (current.pageNumber ?? 1) - 1;
    const pageInfo = readPageInfo(result.pageInfo, after);
    const titles = mapTitleNodes(result.nodes);
    const nextIndex = direction === 'next' ? index + 1 : index - 1;
    this.currentAfter = after;
    this.publish({
      ...current,
      kind: 'catalog',
      titles,
      pageInfo,
      loadingMore: false,
      pageError: null,
      cursorInvalid: false,
      pageDirection: direction,
      pageNumber: nextIndex + 1,
      pageStarts:
        direction === 'next' ? [...starts.slice(0, index + 1), after] : starts,
    });
  }

  private failPage(
    current: CatalogState,
    direction: 'next' | 'previous',
    error: unknown,
  ): void {
    this.publish({
      ...current,
      loadingMore: false,
      pageDirection: direction,
      cursorInvalid:
        error instanceof TransportFailure && error.code === 'FIELD_INVALID',
      pageError: `${direction === 'next' ? 'Next' : 'Previous'} titles could not be loaded. Retry this page or restart from page one.`,
    });
  }

  dispose(): void {
    globalThis.clearTimeout(this.debounceTimer);
    this.active?.abort();
  }

  private validSearchInput(criteria: Criteria) {
    if (!validateText(criteria.text, MAX_SEARCH_CODE_POINTS).valid) return null;
    return buildSearchInput(criteria);
  }

  private async search(
    criteria: Criteria,
    after: string | null = null,
  ): Promise<void> {
    const input = this.validSearchInput(criteria);
    if (input === null) {
      this.invalidate(criteria);
      return;
    }

    this.currentAfter = after;
    const generation = this.begin();
    const signal = this.active?.signal;
    if (!signal) return;

    this.publish({
      kind: 'loading',
      criteria,
      titles: [],
      pageInfo: null,
      invalidField: null,
      pageStarts: this.current.pageStarts ?? [null],
      pageNumber: this.current.pageNumber ?? 1,
    });

    try {
      const result = await fetchTitles(
        this.request,
        {
          filter: input,
          ...(after ? { after } : {}),
        },
        signal,
      );

      if (generation !== this.generation) return; // stale

      this.publishSearchResult(criteria, result, after);
    } catch (err) {
      if (this.obsoleteSearch(generation, err)) return;
      this.publishSearchFailure(criteria, after);
    }
  }

  private publishSearchResult(
    criteria: Criteria,
    result: Awaited<ReturnType<typeof fetchTitles>>,
    after: string | null,
  ): void {
    const titles = mapTitleNodes(result.nodes);
    this.publish({
      kind: after ? 'catalog' : catalogResultKind(criteria, titles),
      pageStarts: this.current.pageStarts,
      pageNumber: this.current.pageNumber,
      criteria,
      titles,
      pageInfo: readPageInfo(result.pageInfo, after),
      invalidField: null,
    });
  }

  private publishSearchFailure(criteria: Criteria, after: string | null): void {
    this.publish({
      kind: 'failure',
      cursorInvalid: after !== null,
      pageStarts: this.current.pageStarts,
      pageNumber: this.current.pageNumber,
      criteria,
      titles: [],
      pageInfo: null,
      invalidField: null,
    });
  }

  private obsoleteSearch(generation: number, error: unknown): boolean {
    return (
      generation !== this.generation ||
      (error instanceof Error && error.name === 'AbortError')
    );
  }

  private invalidate(criteria: Criteria): void {
    this.begin();
    this.publish(invalidCatalogState(criteria));
  }

  private begin(): number {
    this.generation += 1;
    this.active?.abort();
    this.active = new AbortController();
    return this.generation;
  }

  private publish(state: CatalogState): void {
    this.current = Object.freeze(state);
  }
}
