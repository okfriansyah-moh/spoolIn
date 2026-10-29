export function readPageStarts(
  value: unknown,
): readonly (string | null)[] | null {
  if (!Array.isArray(value) || value[0] !== null) return null;
  if (
    !value
      .slice(1)
      .every((cursor) => typeof cursor === 'string' && cursor.length > 0)
  )
    return null;
  return new Set(value).size === value.length ? value : null;
}

/** Cursor history belongs to a browser history entry, never a shareable URL. */
export function catalogPageStarts(
  state: unknown,
  after: string | null,
): readonly (string | null)[] {
  const fallback = after ? [null, after] : [null];
  if (!state || typeof state !== 'object') return fallback;
  const starts = readPageStarts(
    Object.fromEntries(Object.entries(state)).catalogPages,
  );
  if (!starts) return fallback;
  if (after !== null && !starts.includes(after)) return fallback;
  return starts;
}
