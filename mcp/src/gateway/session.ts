import { randomUUID } from 'node:crypto';
import { existsSync, readFileSync } from 'node:fs';
import {
  McpServer,
  ResourceTemplate,
  fromJsonSchema,
} from '@modelcontextprotocol/server';
import type {
  JsonSchemaType,
  RegisteredTool,
  Tool,
} from '@modelcontextprotocol/server';
import { McpMetrics, mcpMetrics } from '../operations/metrics.js';
import { GatewayFailure } from '../operations/gateway-failure.js';
import { GatewayClient } from '../operations/gateway-client.js';
import { Proposal } from '../generated/spoolin/v1/spoolin.js';
import {
  effectiveInputSchemas,
  SessionPolicy,
  validateEffectiveInput,
} from '../tools/effective-policy.js';
import { reviewHtml } from '../resources/review.js';
import { buildProposeMovieCallToolResult } from '../transport/propose-tool-result.js';
import {
  contextProjection,
  outcomeProjection,
  proposalProjection,
} from './projections.js';

const schemaFile = (name: string) => {
  const built = new URL(`../${name}.schema.json`, import.meta.url);
  return existsSync(built)
    ? built
    : new URL(`../../../contracts/json/${name}.schema.json`, import.meta.url);
};
const inputs = JSON.parse(
  readFileSync(schemaFile('mcp-inputs'), 'utf8'),
) as Record<string, JsonSchemaType>;
const outputs = JSON.parse(readFileSync(schemaFile('mcp-outputs'), 'utf8')) as {
  $defs: Record<string, JsonSchemaType>;
};
const input = (state: SessionState, name: string) =>
  fromJsonSchema<Record<string, unknown>>(state.schemas[name]!);
const output = (name: string) =>
  fromJsonSchema<Record<string, unknown>>(outputs.$defs[name]!);
function result(value: Record<string, unknown>) {
  const text = JSON.stringify(value);
  if (Buffer.byteLength(text) > 128 * 1024)
    throw new GatewayFailure('RESOURCE_EXHAUSTED');
  return {
    content: [{ type: 'text' as const, text }],
    structuredContent: value,
  };
}
function safe<T extends Record<string, unknown>>(
  operation: string,
  metrics: McpMetrics,
  handler: (args: T) => Promise<ReturnType<typeof result>>,
) {
  return async (args: T) =>
    metrics.logical(operation, async () => {
      try {
        return await handler(args);
      } catch (failure) {
        const problem =
          failure instanceof GatewayFailure
            ? failure.problem
            : new GatewayFailure('UNAVAILABLE').problem;
        return {
          isError: true,
          content: [{ type: 'text' as const, text: problem.message }],
          structuredContent: { problem },
        };
      }
    });
}

function registerResources(
  server: McpServer,
  proposals: Map<string, { content: Record<string, unknown>; expires: number }>,
): void {
  server.registerResource(
    'movie-review',
    'ui://spoolin/movie-review',
    {
      mimeType: 'text/html;profile=mcp-app',
      _meta: { ui: { csp: { connectDomains: [], resourceDomains: [] } } },
    },
    async () => ({
      contents: [
        {
          uri: 'ui://spoolin/movie-review',
          mimeType: 'text/html;profile=mcp-app',
          text: reviewHtml(),
        },
      ],
    }),
  );
  server.registerResource(
    'proposal',
    new ResourceTemplate('spoolin://proposal/{proposal}/{revision}', {
      list: undefined,
    }),
    {},
    async (uri) => {
      const entry = proposals.get(uri.href);
      if (!entry || entry.expires <= Date.now())
        throw new Error('Proposal unavailable');
      return {
        contents: [
          {
            uri: uri.href,
            mimeType: 'application/json',
            text: JSON.stringify(entry.content),
          },
        ],
      };
    },
  );
}

type GatewayOperations = Pick<
  GatewayClient,
  'policy' | 'context' | 'propose' | 'operation' | 'close'
>;

type ProposalCache = Map<
  string,
  { content: Record<string, unknown>; expires: number }
>;
type SessionState = {
  gateway: GatewayOperations;
  policy: SessionPolicy;
  schemas: Record<string, JsonSchemaType>;
  proposals: ProposalCache;
  metrics: McpMetrics;
  tools: Map<string, RegisteredTool>;
};

export async function createSession(
  token: string,
  gateway: GatewayOperations = new GatewayClient(token),
  metrics: McpMetrics = mcpMetrics,
): Promise<{ server: McpServer; close: () => Promise<void> }> {
  try {
    const initial = await gateway.policy();
    const tools = new Map<string, RegisteredTool>();
    const policy = new SessionPolicy(
      initial,
      () => gateway.policy(),
      (next) => {
        state.schemas = effectiveInputSchemas(inputs, next);
        for (const name of ['movie_context', 'propose_movie']) {
          tools.get(name)?.update({ paramsSchema: input(state, name) });
        }
      },
    );
    const state: SessionState = {
      gateway,
      policy,
      schemas: effectiveInputSchemas(inputs, policy.current),
      proposals: new Map(),
      metrics,
      tools,
    };
    const server = new McpServer({ name: 'spoolin', version: '1.0.0' });
    registerResources(server, state.proposals);
    registerContext(server, state);
    registerOperation(server, state);
    registerProposal(server, state);
    registerToolListing(server, state);
    return {
      server,
      close: async () => {
        state.proposals.clear();
        gateway.close();
        await server.close();
      },
    };
  } catch (error) {
    gateway.close();
    throw error;
  }
}

function registerToolListing(server: McpServer, state: SessionState) {
  const results: Record<string, string> = {
    movie_context: 'context',
    movie_operation: 'operationReceipt',
    propose_movie: 'proposalReview',
  };
  server.server.setRequestHandler('tools/list', async () => {
    await state.policy.refresh();
    const tools: Tool[] = [...state.tools]
      .filter(([, tool]) => tool.enabled)
      .map(([name, tool]) => ({
        name,
        description: tool.description!,
        inputSchema: state.schemas[name]! as Tool['inputSchema'],
        outputSchema: outputs.$defs[results[name]!]! as NonNullable<
          Tool['outputSchema']
        >,
        ...(tool._meta ? { _meta: tool._meta } : {}),
      }));
    return { tools };
  });
}

async function refreshInput(
  state: SessionState,
  name: string,
  args: Record<string, unknown>,
) {
  const advertised = state.policy.current.revision;
  const policy = await state.policy.refresh();
  if (name === 'propose_movie' && policy.revision !== advertised)
    throw new GatewayFailure('POLICY_CHANGED');
  await validateEffectiveInput(state.schemas[name]!, args, policy);
  return policy;
}

function registerContext(server: McpServer, state: SessionState) {
  const tool = server.registerTool(
    'movie_context',
    {
      description: 'Read current movie context and effective policy',
      inputSchema: input(state, 'movie_context'),
      outputSchema: output('context'),
    },
    safe('movie_context', state.metrics, async (args) => {
      const policy = await refreshInput(state, 'movie_context', args);
      const reply = await state.gateway.context(
        String(args.query),
        Number(args.limit ?? Math.min(20, policy.maxPage)),
      );
      if (!reply.policy) throw new GatewayFailure('UNAVAILABLE');
      state.policy.accept(reply.policy);
      return result(contextProjection(reply));
    }),
  );
  state.tools.set('movie_context', tool);
}

function registerOperation(server: McpServer, state: SessionState) {
  const tool = server.registerTool(
    'movie_operation',
    {
      description: 'Read the persisted operation result',
      inputSchema: input(state, 'movie_operation'),
      outputSchema: output('operationReceipt'),
    },
    safe('movie_operation', state.metrics, async (args) =>
      result(
        outcomeProjection(
          await state.gateway.operation(String(args.operationId)),
        ),
      ),
    ),
  );
  state.tools.set('movie_operation', tool);
}

function registerProposal(server: McpServer, state: SessionState) {
  const tool = server.registerTool(
    'propose_movie',
    {
      description:
        'Prepare an immutable proposal for human review; does not publish',
      _meta: { ui: { resourceUri: 'ui://spoolin/movie-review' } },
      inputSchema: input(state, 'propose_movie'),
      outputSchema: output('proposalReview'),
    },
    safe('propose_movie', state.metrics, async (args) => propose(state, args)),
  );
  state.tools.set('propose_movie', tool);
}

async function propose(state: SessionState, args: Record<string, unknown>) {
  const policy = await refreshInput(state, 'propose_movie', args);
  for (const [uri, entry] of state.proposals) {
    if (entry.expires <= Date.now()) state.proposals.delete(uri);
  }
  if (state.proposals.size >= 20)
    throw new GatewayFailure('RESOURCE_EXHAUSTED');
  const { operationId, proposalId, expectedRevision, sources, ...values } =
    args;
  try {
    const revision = await state.gateway.propose(
      Proposal.fromJSON({
        context: {
          operationId,
          correlationId: randomUUID(),
          policyRevision: policy.revision,
        },
        values,
        sources,
        proposalId,
        expectedRevision,
      }),
    );
    const projection = proposalProjection(revision, policy.revision);
    const envelope = buildProposeMovieCallToolResult({
      ...projection,
      uiChallenge: revision.uiChallenge
        ? Buffer.from(revision.uiChallenge).toString('utf8')
        : '',
    });
    state.proposals.set(projection.reviewResourceUri, {
      content: { ...projection, sources },
      expires: Date.parse(projection.expiresAt),
    });
    return { ...envelope, content: result(envelope.structuredContent).content };
  } catch (failure) {
    if (
      failure instanceof GatewayFailure &&
      failure.problem.code === 'POLICY_CHANGED'
    )
      await state.policy.refresh();
    throw failure;
  }
}
