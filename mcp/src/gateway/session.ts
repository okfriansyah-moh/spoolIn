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
import { posterInput } from '../tools/poster-import.js';
import { contextPosters, posterProjection, publicOrigin } from '../tools/poster-output.js';
import type { MovieContent } from '../tools/poster-output.js';
import { PrepareMoviePosterRequest, MoviePosterReadRequest, MovieProposalQueueRequest, Proposal } from '../generated/spoolin/v1/spoolin.js';
import {
  effectiveInputSchemas,
  SessionPolicy,
  validateEffectiveInput,
} from '../tools/effective-policy.js';
import { UiAssertionSigner } from '../identity/ui-assertion.js';
import { registerReviewAction } from './review-action.js';
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
function result(value: Record<string, unknown>): { content: MovieContent[]; structuredContent: Record<string, unknown> } {
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
  'policy' | 'context' | 'propose' | 'operation' | 'preparePoster' | 'readPoster' | 'reviewIdentity' | 'reviewQueue' | 'approve' | 'reject' | 'close'
>;

export type ProposalCache = Map<
  string,
  { content: Record<string, unknown>; expires: number; challenge: string }
>;
export type SessionState = {
  sessionId: string;
  signer: UiAssertionSigner | undefined;
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
        for (const name of ['movie_context', 'propose_movie', 'prepare_movie_poster', 'movie_review_queue']) {
          tools.get(name)?.update({ paramsSchema: input(state, name) });
        }
      },
    );
    const state: SessionState = {
      sessionId: randomUUID(),
      signer: UiAssertionSigner.fromEnvironment(),
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
    registerReviewQueue(server, state);
    registerPoster(server, state);
    registerReviewAction(server, state);
    registerToolListing(server, state);
    return {
      server,
      close: async () => {
        state.signer?.close();
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
    prepare_movie_poster: 'preparedPoster',
    review_movie_proposal: 'operationReceipt',
    movie_review_queue: 'proposalQueue',
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
      description: 'Read current movies and stored poster thumbnails. Includes image content and poster URLs when available.',
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
      const projection = contextProjection(reply);
      const images = await contextPosters(reply, projection.titles, (request) => state.gateway.readPoster(request));
      const envelope = result(projection);
      envelope.content.push(...images);
      return envelope;
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
  publicOrigin();
  for (const [uri, entry] of state.proposals) {
    if (entry.expires <= Date.now()) state.proposals.delete(uri);
  }
  if (state.proposals.size >= 20)
    throw new GatewayFailure('RESOURCE_EXHAUSTED');
  const { operationId, proposalId, expectedRevision, posterRef, sources, credits, ...values } =
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
        credits,
        proposalId,
        expectedRevision,
        posterRef,
      }),
    );
    const projection = proposalProjection(revision, policy.revision);
    const envelope = buildProposeMovieCallToolResult({
      ...projection,
      uiChallenge: revision.uiChallenge
        ? Buffer.from(revision.uiChallenge).toString('utf8')
        : '',
    });
    if (revision.poster) {
      try {
        const image = await state.gateway.readPoster(MoviePosterReadRequest.fromJSON({ refId: revision.poster.refId }));
        if (image.imageBytes.length <= 4 * 1024 * 1024 && image.poster &&
            ['image/jpeg', 'image/png', 'image/webp'].includes(image.poster.mediaType)) {
          const ui = envelope._meta.ui as Record<string, unknown>;
          // Metadata objects are frozen; preserve the private challenge while adding the stored image.
          const meta = { ...envelope._meta, ui: { ...ui, posterPreview: {
            data: Buffer.from(image.imageBytes).toString('base64'), mimeType: image.poster.mediaType,
          } } };
          state.proposals.set(projection.reviewResourceUri, { content: { ...projection, sources },
            expires: Date.parse(projection.expiresAt), challenge: revision.uiChallenge ? Buffer.from(revision.uiChallenge).toString('utf8') : '' });
          return { ...envelope, _meta: meta, content: result(envelope.structuredContent).content };
        }
      } catch { /* Browser review remains available when the stored preview cannot be read. */ }
    }
    state.proposals.set(projection.reviewResourceUri, {
      content: { ...projection, sources },
      expires: Date.parse(projection.expiresAt),
      challenge: revision.uiChallenge ? Buffer.from(revision.uiChallenge).toString('utf8') : '',
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

function registerReviewQueue(server: McpServer, state: SessionState) {
  const tool = server.registerTool('movie_review_queue', {
    description: 'Open one review queue for pending movie proposals with poster previews and credits. Optionally select proposal IDs. Refreshing replaces private decision controls in earlier cards; does not publish movies.',
    _meta: { ui: { resourceUri: 'ui://spoolin/movie-review' } },
    inputSchema: input(state, 'movie_review_queue'),
    outputSchema: output('proposalQueue'),
  }, safe('movie_review_queue', state.metrics, async (args) => {
    const policy = await refreshInput(state, 'movie_review_queue', args);
    publicOrigin();
    const queue = await state.gateway.reviewQueue(MovieProposalQueueRequest.fromJSON({
      proposalIds: args.proposalIds ?? [], limit: args.limit ?? 20,
    }));
    const proposals = queue.proposals.map((revision) => proposalProjection(revision, policy.revision));
    const envelope = result({ proposals });
    const items: Record<string, Record<string, unknown>> = {};
    let remaining = 4 * 1024 * 1024;
    for (let offset = 0; offset < queue.proposals.length; offset += 4) {
      const batch = queue.proposals.slice(offset, offset + 4);
      const previews = await Promise.allSettled(batch.map((revision) =>
        revision.poster && remaining > 0
          ? state.gateway.readPoster(MoviePosterReadRequest.fromJSON({ refId: revision.poster.refId }))
          : Promise.resolve(undefined)));
      for (const [index, revision] of batch.entries()) {
        const projection = proposals[offset + index]!;
        const challenge = revision.uiChallenge ? Buffer.from(revision.uiChallenge).toString('utf8') : '';
        const item: Record<string, unknown> = { challenge };
        const preview = previews[index];
        if (preview?.status === 'fulfilled' && preview.value?.poster &&
            ['image/jpeg', 'image/png', 'image/webp'].includes(preview.value.poster.mediaType)) {
          const data = Buffer.from(preview.value.imageBytes).toString('base64');
          if (data.length <= remaining) {
            item.posterPreview = { data, mimeType: preview.value.poster.mediaType };
            remaining -= data.length;
          }
        }
        items[revision.proposalId] = item;
        for (const [uri, entry] of state.proposals) {
          if (entry.expires <= Date.now()) state.proposals.delete(uri);
        }
        if (!state.proposals.has(projection.reviewResourceUri) && state.proposals.size >= 20) {
          const oldest = state.proposals.keys().next().value;
          if (oldest) state.proposals.delete(oldest);
        }
        state.proposals.set(projection.reviewResourceUri, {
          content: projection, expires: Date.parse(projection.expiresAt), challenge,
        });
      }
    }
    return { ...envelope, _meta: { ui: { resourceUri: 'ui://spoolin/movie-review', items } } };
  }));
  state.tools.set('movie_review_queue', tool);
}

function registerPoster(server: McpServer, state: SessionState) {
  const tool = server.registerTool(
    'prepare_movie_poster',
    {
      description: 'Import a direct HTTPS poster image URL or upload base64 image bytes. Returns posterRef for propose_movie when READY. If PREPARING or PROCESSING, repeat the identical request with the same operationId until READY. Files are limited by effective artwork policy and a 5 MiB processing ceiling.',
      inputSchema: input(state, 'prepare_movie_poster'),
      outputSchema: output('preparedPoster'),
    },
    safe('prepare_movie_poster', state.metrics, async (args) => {
      const policy = await refreshInput(state, 'prepare_movie_poster', args);
      publicOrigin();
      const image = await posterInput(args, Number(policy.artwork.maxInputBytes));
      const poster = await state.gateway.preparePoster(PrepareMoviePosterRequest.fromJSON({
        context: { operationId: args.operationId, correlationId: randomUUID(), policyRevision: policy.revision },
        imageBytes: image.bytes.toString('base64'), mediaType: image.mediaType,
        sourceUrl: image.sourceUrl, reuseBasis: args.reuseBasis,
      }));
      return result(posterProjection(poster));
    }),
  );
  state.tools.set('prepare_movie_poster', tool);
}
