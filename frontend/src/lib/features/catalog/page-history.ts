/** Cursor history belongs to a browser history entry, never a shareable URL. */
export function catalogPageStarts(
  state: unknown,
  after: string | null,
): readonly (string | null)[] {
  if (!state || typeof state !== 'object')
    return after ? [null, after] : [null];
  const starts = Object.fromEntries(Object.entries(state)).catalogPages;
  if (
    !Array.isArray(starts) ||
    starts[0] !== null ||
    !starts
      .slice(1)
      .every((cursor) => typeof cursor === 'string' && cursor.length > 0) ||
    new Set(starts).size !== starts.length ||
    (after !== null && !starts.includes(after))
  )
    return after ? [null, after] : [null];
  return starts;
}
