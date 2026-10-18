export type ArchiveTab = 'movies' | 'people';

export type ArchiveListItem = Readonly<{
  id: string;
  label: string;
  state: string;
  version: string;
}>;

export type ArchiveListState =
  | { kind: 'loading' }
  | { kind: 'empty' }
  | { kind: 'denied' }
  | { kind: 'error'; message: string }
  | {
      kind: 'ready';
      tab: ArchiveTab;
      items: readonly ArchiveListItem[];
      pageInfo?: import('../../state/pagination').PageInfo;
      pageError?: string | null;
      loadingMore?: boolean;
    };
