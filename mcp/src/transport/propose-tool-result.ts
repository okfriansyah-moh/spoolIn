import { composeProposeMovieFromGatewayPayload } from '../tools/propose-movie-from-gateway.js';

export type CallToolResultEnvelope = Readonly<{
  structuredContent: Record<string, unknown>;
  _meta: Record<string, unknown>;
}>;

/** MCP CallToolResult for propose_movie after Core/Catalog gateway revision. */
export function buildProposeMovieCallToolResult(
  gatewayRevision: Record<string, unknown>,
): CallToolResultEnvelope {
  return composeProposeMovieFromGatewayPayload(gatewayRevision);
}
