import { appendDistinct, type PageInfo, type PageSlice } from '../../state/pagination';
import { matchingPage } from '../../state/search-page';

export type ManagedListState<Row> = Readonly<
  | { kind: 'loading' }
  | { kind: 'failure' }
  | { kind: 'ready'; rows: readonly Row[]; pageInfo: PageInfo; next: string | null; loadingMore: boolean; pagingError: boolean }
>;

export class ManagedListController<Row extends { id: string }> {
  private generation = 0;
  private query = '';
  private current = $state.raw<ManagedListState<Row>>({ kind: 'loading' });

  constructor(
    private readonly fetchPage: (csrf: string, cursor: string | null, signal: AbortSignal) => Promise<PageSlice<Row>>,
    private readonly label: (row: Row) => string,
  ) {}

  get snapshot() { return this.current; }
  private live(generation: number, signal: AbortSignal) { return !signal.aborted && generation === this.generation; }
  private publish(value: ManagedListState<Row>) { this.current = Object.freeze(value); }

  private page(csrf: string, cursor: string | null, signal: AbortSignal, generation: number) {
    return matchingPage((after) => this.fetchPage(csrf, after, signal), cursor, this.query, this.label, () => this.live(generation, signal));
  }

  async load(csrf: string, signal: AbortSignal, query = this.query): Promise<void> {
    this.query = query.trim();
    const generation = ++this.generation;
    this.publish({ kind: 'loading' });
    try {
      const page = await this.page(csrf, null, signal, generation);
      if (this.live(generation, signal)) this.publish(this.ready(page));
    } catch {
      if (this.live(generation, signal)) this.publish({ kind: 'failure' });
    }
  }

  private ready(page: PageSlice<Row>): Extract<ManagedListState<Row>, { kind: 'ready' }> {
    return { kind: 'ready', rows: page.nodes, pageInfo: page.pageInfo, next: page.pageInfo.hasNextPage ? page.pageInfo.endCursor : null, loadingMore: false, pagingError: false };
  }

  async loadMore(csrf: string, signal: AbortSignal): Promise<void> {
    const current = this.current;
    if (current.kind !== 'ready' || !current.next || current.loadingMore) return;
    const generation = this.generation;
    this.publish({ ...current, loadingMore: true, pagingError: false });
    try {
      const page = await this.page(csrf, current.next, signal, generation);
      if (!this.live(generation, signal)) return;
      this.publish({ ...this.ready(page), kind: 'ready', rows: appendDistinct(current.rows, page.nodes) });
    } catch {
      if (this.live(generation, signal)) this.publish({ ...current, loadingMore: false, pagingError: true });
    }
  }

  dispose(): void { this.generation += 1; }
}
