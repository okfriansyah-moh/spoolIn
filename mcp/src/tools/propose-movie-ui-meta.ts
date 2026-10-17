export type ProposeMovieUiBinding = Readonly<{
  proposalId: string;
  revision: number;
  contentHash: string;
  bindingToken: string;
}>;

/** MCP Apps UI-only metadata; must not appear in model structured content. */
export function buildProposeMovieToolMeta(binding: ProposeMovieUiBinding): Record<string, unknown> {
  return Object.freeze({
    ui: Object.freeze({
      resourceUri: `spoolin://proposal/${binding.proposalId}/${binding.revision}`,
      visibility: ['app'],
      bindingToken: binding.bindingToken,
      contentHash: binding.contentHash,
      revision: binding.revision,
      proposalId: binding.proposalId,
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
