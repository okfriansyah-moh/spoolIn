export type ProposeMovieUiBinding = Readonly<{
  proposalId: string;
  revision: number;
  contentHash: string;
  challenge: string;
}>;

/** MCP Apps UI-only metadata. Challenge stays in _meta.ui, never structured content. */
export function buildProposeMovieToolMeta(binding: ProposeMovieUiBinding): Record<string, unknown> {
  return Object.freeze({
    ui: Object.freeze({
      resourceUri: `ui://spoolin/movie-review`,
      proposalUri: `spoolin://proposal/${binding.proposalId}/${binding.revision}`,
      visibility: ['app'],
      challenge: binding.challenge,
      contentHash: binding.contentHash,
      revision: binding.revision,
    }),
  });
}

export function modelVisibleProposeMovieContent(
  structured: Record<string, unknown>,
): Record<string, unknown> {
  const copy = { ...structured };
  delete copy.challenge;
  delete copy.ui_challenge;
  delete copy.uiChallenge;
  delete copy.bindingToken;
  delete copy.ui_binding_token;
  delete copy.uiBindingToken;
  return Object.freeze(copy);
}
