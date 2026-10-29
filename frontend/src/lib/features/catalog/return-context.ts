import { readPageStarts } from './page-history';

export type CatalogReturn = {
  url: string;
  catalogCursor: string | null;
  catalogPages: readonly (string | null)[];
};

export function readCatalogReturn(
  state: unknown,
  current: Pick<URL, 'origin'>,
  root: string,
): CatalogReturn | null {
  const entry = returnEntry(state);
  if (!entry) return null;
  try {
    const url = new URL(String(entry.url));
    if (url.origin !== current.origin || url.pathname !== root) return null;
    if (entry.catalogCursor !== null && typeof entry.catalogCursor !== 'string')
      return null;
    const catalogPages = readPageStarts(entry.catalogPages);
    if (!catalogPages) return null;
    return {
      url: url.href,
      catalogCursor: entry.catalogCursor,
      catalogPages,
    };
  } catch {
    return null;
  }
}

function returnEntry(state: unknown): Record<string, unknown> | null {
  if (!state || typeof state !== 'object') return null;
  const value = Object.fromEntries(Object.entries(state)).catalogReturn;
  if (!value || typeof value !== 'object') return null;
  return Object.fromEntries(Object.entries(value));
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
