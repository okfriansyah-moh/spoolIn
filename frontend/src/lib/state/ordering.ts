/** Matches JavaScript's default string ordering, including surrogate code units. */
export function compareUtf16(left: string, right: string): number {
  if (left === right) return 0;
  return left < right ? -1 : 1;
}
