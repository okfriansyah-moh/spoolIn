export const POLL_START_MS = 500;
export const POLL_MAX_MS = 5000;
export const FOREGROUND_POLL_CAP_MS = 5 * 60 * 1000;

/** Catalog operation poll interval: 500ms doubling up to 5s (§3.5). */
export function pollDelayMs(attempt: number): number {
  const scaled = POLL_START_MS * 2 ** Math.max(0, attempt);
  return Math.min(scaled, POLL_MAX_MS);
}

export function transferPercent(loaded: number, total: number): number {
  if (total <= 0) return 0;
  return Math.min(100, Math.floor((loaded * 100) / total));
}
