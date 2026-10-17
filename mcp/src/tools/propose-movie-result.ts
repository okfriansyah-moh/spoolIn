import {
  buildProposeMovieToolMeta,
  modelVisibleProposeMovieContent,
} from './propose-movie-ui-meta.js';

export type ProposeMovieGatewayRevision = Readonly<{
  proposalId: string;
  revision: number;
  contentHash: string;
  bindingToken: string;
}>;

/** Server-side CallToolResult assembly: binding token only in _meta, never structured content. */
export function composeProposeMovieToolResult(
  structured: Record<string, unknown>,
  gateway: ProposeMovieGatewayRevision,
): Readonly<{ structuredContent: Record<string, unknown>; _meta: Record<string, unknown> }> {
  return Object.freeze({
    structuredContent: modelVisibleProposeMovieContent(structured),
    _meta: buildProposeMovieToolMeta({
      proposalId: gateway.proposalId,
      revision: gateway.revision,
      contentHash: gateway.contentHash,
      bindingToken: gateway.bindingToken,
    }),
  });
}
