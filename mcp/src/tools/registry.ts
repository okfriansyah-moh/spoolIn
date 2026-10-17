/** Tool names and input keys aligned with contracts/json/mcp-inputs.schema.json (§5.8). */
export const MCP_TOOLS = Object.freeze([
  'movie_context',
  'propose_movie',
  'movie_operation',
  'approve_movie',
  'reject_movie',
] as const);

export type McpToolName = (typeof MCP_TOOLS)[number];

export type ToolDescriptor = Readonly<{
  name: McpToolName;
  description: string;
  inputSchemaRef: string;
}>;

export const toolRegistry: readonly ToolDescriptor[] = Object.freeze(
  MCP_TOOLS.map((name) =>
    Object.freeze({
      name,
      description: `SpoolIn ${name.replaceAll('_', ' ')}`,
      inputSchemaRef: `contracts/json/mcp-inputs.schema.json#/${name}`,
    }),
  ),
);
