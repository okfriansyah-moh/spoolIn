import type { GraphqlRequest } from '../../transport/graphql';
import { ManagedListController } from '../editing/list-controller.svelte';
import { fetchManagedTitlesPage } from './api';
import type { ListState, ManagedTitleRow } from './types';

export class MovieListController {
  private readonly pages: ManagedListController<ManagedTitleRow>;
  constructor(request: GraphqlRequest) {
    this.pages = new ManagedListController((csrf, after, signal) => fetchManagedTitlesPage(request, csrf, after, signal), (row) => row.title);
  }
  get snapshot(): ListState {
    const page = this.pages.snapshot;
    if (page.kind !== 'ready') return page;
    return Object.freeze({ kind: 'ready', titles: page.rows, next: page.next, loadingMore: page.loadingMore, pagingError: page.pagingError });
  }
  load(csrf: string, signal: AbortSignal, query?: string): Promise<void> { return this.pages.load(csrf, signal, query); }
  loadMore(csrf: string, signal: AbortSignal): Promise<void> { return this.pages.loadMore(csrf, signal); }
  dispose(): void { this.pages.dispose(); }
}
export function createMovieListController(request: GraphqlRequest): MovieListController { return new MovieListController(request); }
