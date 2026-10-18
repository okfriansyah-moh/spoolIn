import { composeProposeMovieToolResult } from './propose-movie-result.js';

function readString(record: Record<string, unknown>, ...keys: readonly string[]): string {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === 'string' && value.trim()) return value;
  }
  return '';
}

function readRevision(record: Record<string, unknown>): number {
  const value = record.revision;
  return typeof value === 'number' && Number.isFinite(value) ? value : 0;
}

/** Place the raw challenge only in UI _meta. Never return a redeemable binding token. */
export function composeProposeMovieFromGatewayPayload(
  gatewayPayload: Record<string, unknown>,
): Readonly<{ structuredContent: Record<string, unknown>; _meta: Record<string, unknown> }> {
  const proposalId = readString(gatewayPayload, 'proposalId', 'proposal_id');
  const revision = readRevision(gatewayPayload);
  const contentHash = readString(gatewayPayload, 'contentHash', 'content_hash');
  const challenge = readString(gatewayPayload, 'ui_challenge', 'uiChallenge', 'challenge');
  return composeProposeMovieToolResult(gatewayPayload, {
    proposalId,
    revision,
    contentHash,
    challenge,
  });
}
