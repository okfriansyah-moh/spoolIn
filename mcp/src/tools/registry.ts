/** Model-visible MCP tools (approve/reject stay UI-only per TD-C-053). */
export const MODEL_MCP_TOOLS = Object.freeze([
  'movie_context',
  'propose_movie',
  'movie_operation',
] as const);

export type McpToolName = (typeof MODEL_MCP_TOOLS)[number];

export type ToolDescriptor = Readonly<{
  name: McpToolName;
  description: string;
  inputSchemaRef: string;
}>;

export const toolRegistry: readonly ToolDescriptor[] = Object.freeze(
  MODEL_MCP_TOOLS.map((name) =>
    Object.freeze({
      name,
      description: `SpoolIn ${name.replaceAll('_', ' ')}`,
      inputSchemaRef: `contracts/json/mcp-inputs.schema.json#/${name}`,
    }),
  ),
);
