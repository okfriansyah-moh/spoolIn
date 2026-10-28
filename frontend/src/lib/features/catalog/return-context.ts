export type CatalogReturn = {
  url: string;
  catalogCursor: string | null;
  catalogPages: readonly (string | null)[];
};

export function readCatalogReturn(
  state: unknown,
  current: URL,
  root: string,
): CatalogReturn | null {
  if (!state || typeof state !== 'object') return null;
  const value = Object.fromEntries(Object.entries(state)).catalogReturn;
  if (!value || typeof value !== 'object') return null;
  const entry = Object.fromEntries(Object.entries(value));
  try {
    const url = new URL(String(entry.url));
    if (url.origin !== current.origin || url.pathname !== root) return null;
    if (entry.catalogCursor !== null && typeof entry.catalogCursor !== 'string')
      return null;
    if (
      !Array.isArray(entry.catalogPages) ||
      entry.catalogPages[0] !== null ||
      !entry.catalogPages
        .slice(1)
        .every((cursor) => typeof cursor === 'string' && cursor.length > 0)
    )
      return null;
    return {
      url: url.href,
      catalogCursor: entry.catalogCursor,
      catalogPages: entry.catalogPages,
    };
  } catch {
    return null;
  }
}

export function plainNavigation(event: MouseEvent): boolean {
  return (
    event.button === 0 &&
    !event.metaKey &&
    !event.ctrlKey &&
    !event.shiftKey &&
    !event.altKey
  );
}
