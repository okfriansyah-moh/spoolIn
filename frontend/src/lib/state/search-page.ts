import type { PageSlice } from './pagination';

/** Skip nonmatching pages while keeping the managed list's paging boundary. */
export async function matchingPage<T>(
  fetchPage: (after: string | null) => Promise<PageSlice<T>>,
  after: string | null,
  query: string,
  label: (row: T) => string,
  isCurrent: () => boolean,
): Promise<PageSlice<T>> {
  const term = query.trim().toLocaleLowerCase();
  let cursor = after;
  while (true) {
    if (!isCurrent()) throw new Error('Search superseded.');
    const page = await fetchPage(cursor);
    const nodes = term
      ? page.nodes.filter((row) =>
          label(row).toLocaleLowerCase().includes(term),
        )
      : page.nodes;
    if (nodes.length || !page.pageInfo.hasNextPage || !term)
      return { ...page, nodes };
    if (!page.pageInfo.endCursor || page.pageInfo.endCursor === cursor)
      throw new Error('Search pagination did not advance.');
    cursor = page.pageInfo.endCursor;
  }
}
