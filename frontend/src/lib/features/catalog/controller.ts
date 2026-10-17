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

export type CatalogListener = (state: CatalogState) => void;

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
  private active: AbortController | null = null;
  private debounceTimer: ReturnType<typeof globalThis.setTimeout> | undefined;
  private readonly listeners = new Set<CatalogListener>();

  private current: CatalogState = {
    kind: 'loading',
    criteria: EMPTY_CRITERIA,
    titles: [],
    pageInfo: null,
    invalidField: null,
  };

  constructor(private readonly request: GraphqlRequest) {}

  get snapshot(): CatalogState {
    return this.current;
  }

  subscribe(listener: CatalogListener): () => void {
    this.listeners.add(listener);
    listener(this.current);
    return () => {
      this.listeners.delete(listener);
    };
  }

  /** Apply new criteria. Text changes are debounced; others fire immediately. */
  setCriteria(next: Criteria, options: { debounce?: boolean } = {}): void {
    // Enforce year→month dependency
    const safe: Criteria = next.year ? next : { ...next, month: '' };

    const textChanged = safe.text !== this.current.criteria.text;
    this.current = { ...this.current, criteria: safe };

    globalThis.clearTimeout(this.debounceTimer);

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
    void this.search(this.current.criteria);
  }

  /** Trigger initial load with the given criteria (no debounce). */
  load(criteria: Criteria): void {
    this.current = { ...this.current, criteria };
    void this.search(criteria);
  }

  dispose(): void {
    globalThis.clearTimeout(this.debounceTimer);
    this.active?.abort();
    this.listeners.clear();
  }

  private async search(criteria: Criteria): Promise<void> {
    // Validate text before issuing a query
    const textCheck = validateText(criteria.text, MAX_SEARCH_CODE_POINTS);
    if (!textCheck.valid) {
      this.publish(invalidCatalogState(criteria));
      return;
    }

    const input = buildSearchInput(criteria);
    if (input === null) {
      this.publish(invalidCatalogState(criteria));
      return;
    }

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
        { filter: input as Parameters<typeof fetchTitles>[1]['filter'] },
        signal,
      );

      if (generation !== this.generation) return; // stale

      const titles = mapTitleNodes(result.nodes);

      this.publish({
        kind: catalogResultKind(criteria, titles),
        criteria,
        titles,
        pageInfo: result.pageInfo,
        invalidField: null,
      });
    } catch (err) {
      if (generation !== this.generation) return; // stale
      if (err instanceof Error && err.name === 'AbortError') return;
      this.publish({
        kind: 'failure',
        criteria,
        titles: [],
        pageInfo: null,
        invalidField: null,
      });
    }
  }

  private begin(): number {
    this.generation += 1;
    this.active?.abort();
    this.active = new AbortController();
    return this.generation;
  }

  private publish(state: CatalogState): void {
    this.current = Object.freeze(state);
    this.listeners.forEach((l) => l(this.current));
  }
}

export function createCatalogController(
  request: GraphqlRequest,
): CatalogController {
  return new CatalogController(request);
}
