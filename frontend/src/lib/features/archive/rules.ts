import type { ArchiveTab } from './types';

/** Permanent person deletion requires a fresh impact token from deletionImpact. */
export function permanentDeleteReady(tab: ArchiveTab, impactToken: string | null): boolean {
  if (tab === 'people') return impactToken !== null && impactToken.trim() !== '';
  return true;
}

/** Maps deletionImpact fields to the maintainer confirmation summary line. */
export function formatDeletionImpactSummary(
  affectedCreditCount: number,
  affectedTitles: readonly string[],
): string {
  if (affectedCreditCount === 0) return 'No active credits reference this person.';
  return `Affects ${affectedCreditCount} credit(s): ${affectedTitles.join(', ')}`;
}

/** Person permanent delete stays disabled until impact preview returns a token. */
export function personDeleteSubmitDisabled(
  tab: ArchiveTab,
  impactToken: string | null,
  busy: boolean,
): boolean {
  return busy || (tab === 'people' && !permanentDeleteReady(tab, impactToken));
}
