import { matchingPage } from '../../state/search-page';
import type { GraphqlRequest } from '../../transport/graphql';
import { fetchManagedTitlesPage } from './api';
import { appendDistinct } from '../../state/pagination';
import type { ListState } from './types';

export class MovieListController {
  private generation = 0;
  private query = '';
  private current: ListState = $state.raw<ListState>({ kind: 'loading' });

  constructor(private readonly request: GraphqlRequest) {}

  get snapshot(): ListState {
    return this.current;
  }

  async load(
    csrf: string,
    signal: AbortSignal,
    query = this.query,
  ): Promise<void> {
    this.query = query.trim();
    const generation = ++this.generation;
    this.publish({ kind: 'loading' });
    try {
      const page = await matchingPage(
        (after) => fetchManagedTitlesPage(this.request, csrf, after, signal),
        null,
        this.query,
        (row) => row.title,
        () => !signal.aborted && generation === this.generation,
      );
      if (signal.aborted || generation !== this.generation) return;
      this.publish({
        kind: 'ready',
        titles: page.nodes,
        next: page.pageInfo.hasNextPage ? page.pageInfo.endCursor : null,
        loadingMore: false,
        pagingError: false,
      });
    } catch (_error) {
      if (signal.aborted || generation !== this.generation) return;
      this.publish({ kind: 'failure' });
    }
  }

  async loadMore(csrf: string, signal: AbortSignal): Promise<void> {
    const current = this.current;
    if (current.kind !== 'ready' || !current.next || current.loadingMore)
      return;
    const generation = this.generation;
    this.publish({ ...current, loadingMore: true, pagingError: false });
    try {
      const page = await matchingPage(
        (after) => fetchManagedTitlesPage(this.request, csrf, after, signal),
        current.next,
        this.query,
        (row) => row.title,
        () => !signal.aborted && generation === this.generation,
      );
      if (signal.aborted || generation !== this.generation) return;
      this.publish({
        kind: 'ready',
        titles: appendDistinct(current.titles, page.nodes),
        next: page.pageInfo.hasNextPage ? page.pageInfo.endCursor : null,
        loadingMore: false,
        pagingError: false,
      });
    } catch {
      if (!signal.aborted && generation === this.generation)
        this.publish({ ...current, loadingMore: false, pagingError: true });
    }
  }

  dispose(): void {
    this.generation += 1;
  }

  private publish(state: ListState): void {
    this.current = Object.freeze(state);
  }
}

export function createMovieListController(
  request: GraphqlRequest,
): MovieListController {
  return new MovieListController(request);
}
