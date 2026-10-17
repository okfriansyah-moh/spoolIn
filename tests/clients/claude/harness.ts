/** T038 client acceptance harness source (Claude Desktop). Not executed in lane. */
export type ClaudeHarness = Readonly<{
  connectorPath: string;
  reviewResource: string;
}>;

export const defaultHarness: ClaudeHarness = Object.freeze({
  connectorPath: 'mcp/dist/stdio-bridge.js',
  reviewResource: 'mcp/dist/review/index.html',
});
