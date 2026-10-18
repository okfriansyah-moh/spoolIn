import { randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { McpServer, ResourceTemplate, fromJsonSchema } from '@modelcontextprotocol/server';
import type { JsonSchemaType } from '@modelcontextprotocol/server';
import { GatewayClient } from '../operations/gateway-client.js';
import { Proposal } from '../generated/spoolin/v1/spoolin.js';
import { reviewHtml } from '../resources/review.js';
import { buildProposeMovieCallToolResult } from '../transport/propose-tool-result.js';
import { contextProjection, outcomeProjection, proposalProjection } from './projections.js';

const inputs = JSON.parse(readFileSync(new URL('../mcp-inputs.schema.json', import.meta.url), 'utf8')) as Record<string, JsonSchemaType>;
const outputs = JSON.parse(readFileSync(new URL('../mcp-outputs.schema.json', import.meta.url), 'utf8')) as { $defs: Record<string, JsonSchemaType> };
const input = (name: string) => fromJsonSchema<Record<string, unknown>>(inputs[name]!);
const output = (name: string) => fromJsonSchema<Record<string, unknown>>(outputs.$defs[name]!);
function result(value: Record<string, unknown>) {
  const text = JSON.stringify(value);
  if (Buffer.byteLength(text) > 128 * 1024) throw new Error('RESOURCE_EXHAUSTED');
  return { content: [{ type: 'text' as const, text }], structuredContent: value };
}
function safe<T extends Record<string, unknown>>(handler: (args: T) => Promise<ReturnType<typeof result>>) {
  return async (args: T) => {
    try { return await handler(args); } catch {
      return { isError: true, content: [{ type: 'text' as const, text: 'The gateway operation could not be completed.' }] };
    }
  };
}

function registerResources(server: McpServer, proposals: Map<string, { content: Record<string, unknown>; expires: number }>): void {
  server.registerResource('movie-review', 'ui://spoolin/movie-review', {
    mimeType: 'text/html;profile=mcp-app', _meta: { ui: { csp: { connectDomains: [], resourceDomains: [] } } },
  }, async () => ({ contents: [{ uri: 'ui://spoolin/movie-review', mimeType: 'text/html;profile=mcp-app', text: reviewHtml() }] }));
  server.registerResource('proposal', new ResourceTemplate('spoolin://proposal/{proposal}/{revision}', { list: undefined }), {}, async (uri) => {
    const entry = proposals.get(uri.href);
    if (!entry || entry.expires <= Date.now()) throw new Error('Proposal unavailable');
    return { contents: [{ uri: uri.href, mimeType: 'application/json', text: JSON.stringify(entry.content) }] };
  });
}

export async function createSession(token: string): Promise<{ server: McpServer; close: () => Promise<void> }> {
  const gateway = new GatewayClient(token);
  let policy;
  try { policy = await gateway.policy(); } catch (error) { gateway.close(); throw error; }
  const server = new McpServer({ name: 'spoolin', version: '1.0.0' });
  const proposals = new Map<string, { content: Record<string, unknown>; expires: number }>();
  registerResources(server, proposals);
  server.registerTool('movie_context', { description: 'Read current movie context and effective policy',
    inputSchema: input('movie_context'), outputSchema: output('context'),
  }, safe(async (args) => result(contextProjection(await gateway.context(String(args.query), Number(args.limit ?? 20))))));
  server.registerTool('movie_operation', { description: 'Read the persisted operation result',
    inputSchema: input('movie_operation'), outputSchema: output('operationReceipt'),
  }, safe(async (args) => result(outcomeProjection(await gateway.operation(String(args.operationId))))));
  server.registerTool('propose_movie', { description: 'Prepare an immutable proposal for human review; does not publish',
    _meta: { ui: { resourceUri: 'ui://spoolin/movie-review' } },
    inputSchema: input('propose_movie'), outputSchema: output('proposalReview'),
  }, safe(async (args) => {
    for (const [uri, entry] of proposals) if (entry.expires <= Date.now()) proposals.delete(uri);
    if (proposals.size >= 20) throw new Error('RESOURCE_EXHAUSTED');
    const { operationId, proposalId, expectedRevision, sources, ...values } = args;
    const revision = await gateway.propose(Proposal.fromJSON({
      context: { operationId, correlationId: randomUUID(), policyRevision: policy.revision }, values, sources, proposalId, expectedRevision,
    }));
    const projection = proposalProjection(revision, policy.revision);
    const envelope = buildProposeMovieCallToolResult({ ...projection,
      uiChallenge: revision.uiChallenge ? Buffer.from(revision.uiChallenge).toString('utf8') : '',
    });
    proposals.set(projection.reviewResourceUri, { content: { ...projection, sources }, expires: Date.parse(projection.expiresAt) });
    return { ...envelope, content: result(envelope.structuredContent).content };
  }));
  return { server, close: async () => { proposals.clear(); gateway.close(); await server.close(); } };
}
