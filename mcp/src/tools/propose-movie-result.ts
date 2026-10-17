import {
  buildProposeMovieToolMeta,
  modelVisibleProposeMovieContent,
} from './propose-movie-ui-meta.js';

export type ProposeMovieGatewayRevision = Readonly<{
  proposalId: string;
  revision: number;
  contentHash: string;
  challenge: string;
}>;

/** Challenge is UI-only _meta. Model structured content omits it and any binding token. */
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
      challenge: gateway.challenge,
    }),
  });
}
