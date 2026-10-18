import type { GraphqlRequest } from '../../transport/graphql';
import { fetchManagedTitlesPage } from './api';
import { appendDistinct } from '../../state/pagination';
import type { ListState } from './types';

export class MovieListController {
  private generation = 0;
  private current: ListState = { kind: 'loading' };
  private readonly listeners = new Set<(state: ListState) => void>();

  constructor(private readonly request: GraphqlRequest) {}

  get snapshot(): ListState {
    return this.current;
  }

  subscribe(listener: (state: ListState) => void): () => void {
    this.listeners.add(listener);
    listener(this.current);
    return () => this.listeners.delete(listener);
  }

  async load(csrf: string, signal: AbortSignal): Promise<void> {
    const generation = ++this.generation;
    this.publish({ kind: 'loading' });
    try {
      const page = await fetchManagedTitlesPage(
        this.request,
        csrf,
        null,
        signal,
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
      const page = await fetchManagedTitlesPage(
        this.request,
        csrf,
        current.next,
        signal,
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
    this.listeners.clear();
  }

  private publish(state: ListState): void {
    this.current = Object.freeze(state);
    this.listeners.forEach((listener) => listener(this.current));
  }
}

export function createMovieListController(
  request: GraphqlRequest,
): MovieListController {
  return new MovieListController(request);
}
