/** T039 client acceptance harness source (ChatGPT tunnel). Not executed in lane. */
export type ChatGptHarness = Readonly<{
  tunnelScript: string;
  reviewResource: string;
}>;

export const defaultHarness: ChatGptHarness = Object.freeze({
  tunnelScript: 'ops/bin/tunnel.sh',
  reviewResource: 'mcp/dist/review/index.html',
});
