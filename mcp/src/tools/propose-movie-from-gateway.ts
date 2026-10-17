import { composeProposeMovieToolResult } from './propose-movie-result.js';

type GatewayRevisionWire = Readonly<{
  proposalId?: string;
  proposal_id?: string;
  revision?: number;
  contentHash?: string;
  content_hash?: string;
  uiBindingToken?: string;
  ui_binding_token?: string;
  bindingToken?: string;
}>;

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

/** Strip gateway-only secrets before model structured content; binding token lives in _meta only. */
export function composeProposeMovieFromGatewayPayload(
  gatewayPayload: Record<string, unknown>,
): Readonly<{ structuredContent: Record<string, unknown>; _meta: Record<string, unknown> }> {
  const wire = gatewayPayload as GatewayRevisionWire & Record<string, unknown>;
  const proposalId = wire.proposalId ?? wire.proposal_id ?? readString(gatewayPayload, 'proposalId', 'proposal_id');
  const revision = wire.revision ?? readRevision(gatewayPayload);
  const contentHash = wire.contentHash ?? wire.content_hash ?? readString(gatewayPayload, 'contentHash', 'content_hash');
  const bindingToken =
    wire.uiBindingToken ??
    wire.ui_binding_token ??
    wire.bindingToken ??
    readString(gatewayPayload, 'uiBindingToken', 'ui_binding_token', 'bindingToken');
  return composeProposeMovieToolResult(gatewayPayload, {
    proposalId,
    revision,
    contentHash,
    bindingToken,
  });
}
