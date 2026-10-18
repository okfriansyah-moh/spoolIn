import { matchingPage } from '../../../state/search-page';
import type { GraphqlRequest } from '../../../transport/graphql';
import { appendDistinct, type PageInfo } from '../../../state/pagination';
import { fetchManagedPeoplePage } from './api';
import type { ListState } from './types';

export class PersonListController {
  private generation = 0;
  private query = '';
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
        (after) => fetchManagedPeoplePage(this.request, csrf, after, signal),
        null,
        this.query,
        (row) => row.name,
        () => !signal.aborted && generation === this.generation,
      );
      if (signal.aborted || generation !== this.generation) return;
      this.publish({
        kind: 'ready',
        people: page.nodes,
        pageInfo: page.pageInfo,
      });
    } catch {
      if (signal.aborted || generation !== this.generation) return;
      this.publish({ kind: 'failure' });
    }
  }

  private pageReady(
    state: ListState,
  ): state is Extract<ListState, { kind: 'ready' }> & { pageInfo: PageInfo } {
    return (
      state.kind === 'ready' &&
      !state.loadingMore &&
      !!state.pageInfo?.hasNextPage
    );
  }

  async loadMore(csrf: string, signal: AbortSignal): Promise<void> {
    const current = this.current;
    if (!this.pageReady(current)) return;
    const generation = this.generation;
    this.publish({ ...current, loadingMore: true, pageError: null });
    try {
      const page = await matchingPage(
        (after) => fetchManagedPeoplePage(this.request, csrf, after, signal),
        current.pageInfo.endCursor,
        this.query,
        (row) => row.name,
        () => !signal.aborted && generation === this.generation,
      );
      if (signal.aborted || generation !== this.generation) return;
      this.publish({
        ...current,
        people: appendDistinct(current.people, page.nodes),
        pageInfo: page.pageInfo,
        loadingMore: false,
      });
    } catch {
      if (!signal.aborted && generation === this.generation)
        this.publish({
          ...current,
          loadingMore: false,
          pageError: 'More people could not be loaded. Retry this page.',
        });
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

export function createPersonListController(
  request: GraphqlRequest,
): PersonListController {
  return new PersonListController(request);
}
