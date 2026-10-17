export type ArchiveTab = 'movies' | 'people';

export type ArchiveListState =
  | { kind: 'loading' }
  | { kind: 'empty' }
  | { kind: 'denied' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; tab: ArchiveTab; items: Array<{ id: string; label: string; state: string }> };
