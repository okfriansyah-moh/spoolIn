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
    this.current = { ...this.current, criteria: safe };

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
  load(criteria: Criteria, after: string | null = null): void {
    globalThis.clearTimeout(this.debounceTimer);
    this.current = { ...this.current, criteria };
    void this.search(criteria, after);
  }

  private pageAvailable(current: CatalogState): boolean {
    return (
      current.kind === 'catalog' &&
      !current.loadingMore &&
      !!current.pageInfo?.hasNextPage
    );
  }

  async loadMore(): Promise<void> {
    const current = this.current;
    if (!this.pageAvailable(current)) return;
    const after = current.pageInfo?.endCursor;
    if (!after) return;
    const generation = this.generation;
    const filter = buildSearchInput(current.criteria);
    if (!filter) return;
    this.publish({ ...current, loadingMore: true, pageError: null });
    try {
      const result = await fetchTitles(
        this.request,
        {
          filter: filter as Parameters<typeof fetchTitles>[1]['filter'],
          after,
        },
        this.active?.signal,
      );
      if (generation !== this.generation) return;
      this.currentAfter = after;
      this.publish({
        ...current,
        titles: mapTitleNodes(result.nodes),
        pageInfo: readPageInfo(result.pageInfo, after),
        loadingMore: false,
        pageError: null,
      });
    } catch {
      if (generation === this.generation)
        this.publish({
          ...current,
          loadingMore: false,
          pageError: 'More titles could not be loaded. Retry this page.',
        });
    }
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
    });

    try {
      const result = await fetchTitles(
        this.request,
        {
          filter: input as Parameters<typeof fetchTitles>[1]['filter'],
          ...(after ? { after } : {}),
        },
        signal,
      );

      if (generation !== this.generation) return; // stale

      const titles = mapTitleNodes(result.nodes);

      this.publish({
        kind: catalogResultKind(criteria, titles),
        criteria,
        titles,
        pageInfo: readPageInfo(result.pageInfo, after),
        invalidField: null,
      });
    } catch (err) {
      if (this.obsoleteSearch(generation, err)) return;
      this.publish({
        kind: 'failure',
        criteria,
        titles: [],
        pageInfo: null,
        invalidField: null,
      });
    }
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

export function createCatalogController(
  request: GraphqlRequest,
): CatalogController {
  return new CatalogController(request);
}
