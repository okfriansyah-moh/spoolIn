/** T038 client acceptance harness source (Claude Desktop). Not executed in lane. */
export type ClaudeHarness = Readonly<{
  connectorPath: string;
  reviewResource: string;
  approvalChecklist: readonly string[];
}>;

export const defaultHarness: ClaudeHarness = Object.freeze({
  connectorPath: 'mcp/dist/stdio-bridge.js',
  reviewResource: 'mcp/dist/review/index.html',
  approvalChecklist: Object.freeze([
    'human-visible exact revision values',
    'challenge never exposed to model tools',
    'approve_movie includes contentHash + challenge',
    'reject publishes no catalog row',
    'cross-actor approval denied',
  ]),
});

export function assertApprovalChecklist(items: readonly string[]): void {
  for (const item of defaultHarness.approvalChecklist) {
    if (!items.includes(item)) {
      throw new Error(`missing checklist evidence: ${item}`);
    }
  }
}
