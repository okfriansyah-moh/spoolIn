/** Stable UTF-16 ordering for protocol keys and machine-readable output. */
export function compareText(left: string, right: string): number {
  if (left < right) return -1;
  return left > right ? 1 : 0;
}
