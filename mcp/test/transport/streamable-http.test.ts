import { policyFixture } from '../fixtures/policy.js';
import { GatewayFailure } from '../../src/operations/gateway-failure.js';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { StdioClientTransport } from '@modelcontextprotocol/client/stdio';
import {
  Client,
  StreamableHTTPClientTransport,
} from '@modelcontextprotocol/client';
import { StreamableHttpTransport } from '../../src/transport/streamable-http.js';
import { createSession } from '../../src/gateway/session.js';
import type {
  MovieContext,
  Outcome,
  PolicySnapshot,
  ProposalRevision,
} from '../../src/generated/spoolin/v1/spoolin.js';

// Unit protocol proof against a fake gateway only. This does not establish either chat host's isolation.
describe('real SDK streamable HTTP session lifecycle', () => {
  let server: StreamableHttpTransport;
  let transport: StreamableHTTPClientTransport | undefined;
  let client: Client | undefined;
  let closed = 0;
  let endpoint: string;
  let policy: PolicySnapshot = policyFixture();
  let proposals = 0;
  let proposedPolicy: string | undefined;
  let policyChangesOnWrite = false;
  beforeEach(async () => {
    process.env.MCP_PORT = '0';
    closed = 0;
    proposals = 0;
    proposedPolicy = undefined;
    policyChangesOnWrite = false;
    policy = policyFixture();
    server = new StreamableHttpTransport(async (token) => {
      if (token !== 'synthetic-connector') throw new GatewayFailure('FORBIDDEN');
      return createSession(token, {
        policy: async () => policy,
        context: async (): Promise<MovieContext> => ({
          policy,
          titles: { nodes: [], hasNextPage: false },
          genres: { nodes: [] },
          roles: { nodes: [] },
        }),
        operation: async (id): Promise<Outcome> => ({
          operationId: id,
          state: 'SUCCEEDED',
          resultExpired: false,
        }),
        propose: async (request): Promise<ProposalRevision> => {
          proposals += 1;
          proposedPolicy = request.context?.policyRevision;
          if (policyChangesOnWrite) {
            policy = nextPolicy();
            throw new GatewayFailure('POLICY_CHANGED');
          }
          throw new Error('synthetic upstream failure');
        },
        close: () => {
          closed += 1;
        },
      });
    });
    await server.listen({ httpPath: '/mcp', healthPort: 0 });
    endpoint = `http://127.0.0.1:${server.port}/mcp`;
  });
  afterEach(async () => {
    await client?.close();
    await server.close();
    transport = undefined;
    client = undefined;
    delete process.env.MCP_PORT;
  });
  async function connect() {
    transport = new StreamableHTTPClientTransport(new URL(endpoint), {
      requestInit: { headers: { authorization: 'Bearer synthetic-connector' } },
    });
    client = new Client({ name: 'spoolin-protocol-unit', version: '1.0.0' });
    await client.connect(transport);
    return client;
  }
  it('negotiates lists tools executes a gateway call and terminates its session', async () => {
    const connected = await connect();
    expect(
      (await connected.listTools()).tools.map((tool) => tool.name),
    ).toEqual(['movie_context', 'movie_operation', 'propose_movie']);
    const result = await connected.callTool({
      name: 'movie_context',
      arguments: { query: '', limit: 1 },
    });
    expect(result.isError).not.toBe(true);
    expect(result.structuredContent).toMatchObject({
      titles: [],
      genres: [],
      roles: [],
      policyRevision: policy.revision,
    });
    await transport!.terminateSession();
    expect(closed).toBe(1);
  });
  it('rejects missing credentials disallowed origins and cross-connector session reuse', async () => {
    expect((await fetch(endpoint, { method: 'POST' })).status).toBe(401);
    expect(
      (
        await fetch(endpoint, {
          method: 'POST',
          headers: { origin: 'https://untrusted.example' },
        })
      ).status,
    ).toBe(403);
    await connect();
    expect(
      (
        await fetch(endpoint, {
          method: 'GET',
          headers: {
            authorization: 'Bearer another-connector',
            'mcp-session-id': transport!.sessionId!,
          },
        })
      ).status,
    ).toBe(404);
  });
  it('retains actual owner credential refusal status before allocating a protocol session', async () => {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        authorization: 'Bearer denied-connector',
        'content-type': 'application/json',
        accept: 'application/json, text/event-stream',
      },
      body: JSON.stringify({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {
        protocolVersion: '2025-11-25', capabilities: {}, clientInfo: { name: 'unit', version: '1' },
      } }),
    });
    expect(response.status).toBe(403);
    expect(await response.json()).toEqual({ problem: { code: 'FORBIDDEN' } });
    expect(response.headers.get('mcp-session-id')).toBeNull();
    expect(closed).toBe(0);
  });
  it('bridges a real stdio process into the HTTP session without exposing its credential', async () => {
    const directory = mkdtempSync(join(tmpdir(), 'spoolin-stdio-'));
    const tokenFile = join(directory, 'connector');
    writeFileSync(tokenFile, 'synthetic-connector', { mode: 0o600 });
    const bridge = new StdioClientTransport({
      command: process.execPath,
      args: [
        fileURLToPath(
          new URL('../../dist/transport/stdio.js', import.meta.url),
        ),
      ],
      env: {
        PATH: process.env.PATH ?? '',
        MCP_CONNECTOR_TOKEN_FILE: tokenFile,
        MCP_ENDPOINT: endpoint,
      },
      stderr: 'pipe',
    });
    client = new Client({ name: 'spoolin-stdio-unit', version: '1.0.0' });
    try {
      await client.connect(bridge);
      const tools = await client.listTools();
      expect(tools.tools.map((tool) => tool.name)).toContain('movie_context');
      const response = await client.callTool({
        name: 'movie_context',
        arguments: { query: 'movie' },
      });
      expect(response.isError).not.toBe(true);
      expect(JSON.stringify(response)).not.toContain('synthetic-connector');
    } finally {
      await client.close();
      client = undefined;
      rmSync(directory, { recursive: true });
    }
  });
  it('rejects malformed tool inputs and propagates safe gateway failures', async () => {
    const connected = await connect();
    const invalid = await connected.callTool({
      name: 'movie_context',
      arguments: { query: '', limit: 100 },
    });
    expect(invalid.isError).toBe(true);
    await expect(
      connected.callTool({ name: 'approve_movie', arguments: {} }),
    ).rejects.toThrow('Tool approve_movie not found');
    const failure = await connected.callTool({
      name: 'propose_movie',
      arguments: {
        operationId: '00000000-0000-4000-8000-000000000001',
        title: 'A movie',
        synopsis: 'A complete movie proposal',
        genreIds: ['00000000-0000-4000-8000-000000000002'],
        release: { year: 2024 },
      },
    });
    expect(failure.isError).toBe(true);
    expect(JSON.stringify(failure)).not.toContain('synthetic upstream failure');
  });
  function candidate(title = 'A movie') {
    return {
      operationId: '00000000-0000-4000-8000-000000000001',
      title,
      synopsis: 'A complete movie proposal',
      genreIds: ['00000000-0000-4000-8000-000000000002'],
      release: { year: 2024 },
    };
  }
  function nextPolicy() {
    const next = policyFixture();
    next.revision = 'b'.repeat(64);
    next.textBounds.find((bound) => bound.field === 'title')!.maxCodePoints = 5;
    return next;
  }
  it('advertises effective bounds and refreshes stale schemas without retrying or forwarding a stale write', async () => {
    const connected = await connect();
    policy = nextPolicy();
    const stale = await connected.callTool({
      name: 'propose_movie',
      arguments: candidate(),
    });
    expect(stale.structuredContent).toMatchObject({
      problem: { code: 'POLICY_CHANGED' },
    });
    expect(proposals).toBe(0);
    const listed = await connected.listTools();
    expect(
      listed.tools.find((tool) => tool.name === 'propose_movie')?.inputSchema,
    ).toMatchObject({ properties: { title: { maxLength: 5 } } });
    await connected.callTool({
      name: 'propose_movie',
      arguments: candidate('Movie'),
    });
    expect(proposals).toBe(1);
    expect(proposedPolicy).toBe(policy.revision);
  });
  it('refreshes effective schemas on an empty context read and preserves required fields', async () => {
    const connected = await connect();
    policy = nextPolicy();
    await connected.callTool({
      name: 'movie_context',
      arguments: { query: '' },
    });
    expect(
      (await connected.listTools()).tools.find(
        (tool) => tool.name === 'propose_movie',
      )?.inputSchema,
    ).toMatchObject({
      required: ['operationId', 'title', 'synopsis', 'genreIds', 'release'],
      properties: { title: { maxLength: 5 } },
    });
    const invalid = await connected.callTool({
      name: 'propose_movie',
      arguments: candidate(),
    });
    expect(invalid.isError).toBe(true);
    expect(proposals).toBe(0);
  });
  it('refreshes tools listing itself before advertising current effective bounds', async () => {
    const connected = await connect();
    policy = nextPolicy();
    const tools = (await connected.listTools()).tools;
    expect(
      tools.find((tool) => tool.name === 'propose_movie')?.inputSchema,
    ).toMatchObject({ properties: { title: { maxLength: 5 } } });
    expect(tools.map((tool) => tool.name)).not.toContain('approve_movie');
    expect(tools.find((tool) => tool.name === 'propose_movie')?._meta).toEqual({
      ui: { resourceUri: 'ui://spoolin/movie-review' },
    });
    expect(proposals).toBe(0);
  });
  it('refreshes a downstream stale-policy rejection but never automatically repeats the write', async () => {
    const connected = await connect();
    policyChangesOnWrite = true;
    const rejected = await connected.callTool({
      name: 'propose_movie',
      arguments: candidate(),
    });
    expect(rejected.structuredContent).toMatchObject({
      problem: { code: 'POLICY_CHANGED' },
    });
    expect(proposals).toBe(1);
    expect(
      (await connected.listTools()).tools.find(
        (tool) => tool.name === 'propose_movie',
      )?.inputSchema,
    ).toMatchObject({ properties: { title: { maxLength: 5 } } });
  });
});
