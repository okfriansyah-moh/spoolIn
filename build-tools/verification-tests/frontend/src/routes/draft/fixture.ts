import { specimenPolicy } from '../policy/fixture';

export const accountA = '10000000-0000-4000-8000-000000000001';
export const accountB = '20000000-0000-4000-8000-000000000002';
export const catalogSubmission = '30000000-0000-4000-8000-000000000003';

export function draftSession(accountId: string | null) {
  return {
    accountId,
    csrf: 'A'.repeat(43),
    policyRevision: specimenPolicy().revision,
    generation: accountId === accountB ? 1 : 0,
  };
}
