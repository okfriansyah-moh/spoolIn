import type { GraphqlRequest } from '../../../transport/graphql';
import { ManagedListController } from '../../editing/list-controller.svelte';
import { fetchManagedPeoplePage } from './api';
import type { ListState, PersonListRow } from './types';

export class PersonListController {
  private readonly pages: ManagedListController<PersonListRow>;
  constructor(request: GraphqlRequest) {
    this.pages = new ManagedListController((csrf, after, signal) => fetchManagedPeoplePage(request, csrf, after, signal), (row) => row.name);
  }
  get snapshot(): ListState {
    const page = this.pages.snapshot;
    if (page.kind !== 'ready') return page;
    return Object.freeze({ kind: 'ready', people: page.rows, pageInfo: page.pageInfo,
      loadingMore: page.loadingMore, pageError: page.pagingError ? 'More people could not be loaded. Retry this page.' : null });
  }
  load(csrf: string, signal: AbortSignal, query?: string): Promise<void> { return this.pages.load(csrf, signal, query); }
  loadMore(csrf: string, signal: AbortSignal): Promise<void> { return this.pages.loadMore(csrf, signal); }
  dispose(): void { this.pages.dispose(); }
}
export function createPersonListController(request: GraphqlRequest): PersonListController { return new PersonListController(request); }
