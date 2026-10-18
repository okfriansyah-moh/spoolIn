export type PageInfo = Readonly<{
  endCursor: string | null;
  hasNextPage: boolean;
}>;
export type PageSlice<T> = Readonly<{
  nodes: readonly T[];
  pageInfo: PageInfo;
}>;

function pageRecord(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new Error('Page information unavailable.');
  return Object.fromEntries(Object.entries(value));
}

export function readPageInfo(
  value: unknown,
  after: string | null = null,
): PageInfo {
  const input = pageRecord(value);
  if (typeof input.hasNextPage !== 'boolean')
    throw new Error('Page state unavailable.');
  const cursor = input.endCursor;
  if (cursor !== null && typeof cursor !== 'string')
    throw new Error('Page cursor unavailable.');
  if (input.hasNextPage && (!cursor || cursor === after))
    throw new Error('Next page cursor unavailable.');
  return Object.freeze({
    endCursor: cursor as string | null,
    hasNextPage: input.hasNextPage,
  });
}

export function appendDistinct<T extends { id: string }>(
  existing: readonly T[],
  incoming: readonly T[],
): readonly T[] {
  const seen = new Set(existing.map((row) => row.id));
  for (const row of incoming) {
    if (seen.has(row.id))
      throw new Error('Page identity changed. Reload the current list.');
    seen.add(row.id);
  }
  return Object.freeze([...existing, ...incoming]);
}

export function historyCursor(state: unknown, key: string): string | null {
  if (!state || typeof state !== 'object') return null;
  const value = Object.fromEntries(Object.entries(state))[key];
  return typeof value === 'string' && value.length ? value : null;
}
