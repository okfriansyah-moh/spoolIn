/** T039 client acceptance harness source (ChatGPT). Not executed in lane. */
export type ChatGptHarness = Readonly<{
  tunnelProfile: string;
  reviewResource: string;
  approvalChecklist: readonly string[];
}>;

export const defaultHarness: ChatGptHarness = Object.freeze({
  tunnelProfile: 'spoolin-local',
  reviewResource: 'mcp/dist/review/index.html',
  approvalChecklist: Object.freeze([
    'private tunnel registration evidence',
    'trusted review resource origin',
    'exact contentHash on approve',
    'corrected synopsis requires new revision',
    'model-direct gateway approval denied',
  ]),
});

export function assertApprovalChecklist(items: readonly string[]): void {
  for (const item of defaultHarness.approvalChecklist) {
    if (!items.includes(item)) {
      throw new Error(`missing checklist evidence: ${item}`);
    }
  }
}
