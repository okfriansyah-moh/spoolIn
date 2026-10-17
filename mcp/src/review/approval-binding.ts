export type ExactApprovalBindingInput = Readonly<{
  proposalId: string;
  revision: number;
  contentHash: string;
  challenge: string;
}>;

export function buildApprovalBody(binding: ExactApprovalBindingInput): Record<string, unknown> {
  return {
    proposalId: binding.proposalId,
    revision: binding.revision,
    contentHash: binding.contentHash,
    challenge: binding.challenge,
  };
}

/** Trusted review posts only explicit human-bound approval payloads (no auto-approve). */
export function assertManualApproval(challenge: string): void {
  if (!challenge.trim()) {
    throw new Error('Trusted approval requires a UI challenge');
  }
}
