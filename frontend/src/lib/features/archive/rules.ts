import type { ArchiveTab } from './types';

/** Permanent person deletion requires a fresh impact token from deletionImpact. */
export function permanentDeleteReady(tab: ArchiveTab, impactToken: string | null): boolean {
  if (tab === 'people') return impactToken !== null && impactToken.trim() !== '';
  return true;
}
