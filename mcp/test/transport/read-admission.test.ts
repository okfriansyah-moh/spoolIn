import { describe, expect, it } from 'vitest';
import {
  Client,
  StreamableHTTPClientTransport,
} from '@modelcontextprotocol/client';
import { McpServer } from '@modelcontextprotocol/server';
import { connectorTokenDigest } from '../../src/identity/connector.js';
import {
  ReadAdmission,
  ReadLimited,
  ReadUnavailable,
} from '../../src/transport/read-admission.js';
import { StreamableHttpTransport } from '../../src/transport/streamable-http.js';

const digest = connectorTokenDigest('synthetic-verified-connector');
const other = connectorTokenDigest('different-synthetic-connector');
const settings = {
  RATE_LIMIT_READ_MAX_REQUESTS: '2',
  RATE_LIMIT_READ_WINDOW_SECONDS: '10',
  MCP_RATE_LIMIT_MAX_BINDINGS: '1',
};

describe('adapter READ admission', () => {
  it('saturates without overflow and uses aligned fixed windows', () => {
    let now = 12000;
    const admission = new ReadAdmission(settings, () => now);
    admission.admit(digest);
    admission.admit(digest);
    expect(() => admission.admit(digest)).toThrow(ReadLimited);
    try {
      admission.admit(digest);
    } catch (failure) {
      expect((failure as ReadLimited).retryAfterMs).toBe(8000);
    }
    now = 20000;
    expect(() => admission.admit(digest)).not.toThrow();
  });
  it('retains bindings for two windows and never evicts a live connector', () => {
    let now = 12000;
    const admission = new ReadAdmission(settings, () => now);
    admission.admit(digest);
    expect(() => admission.admit(other)).toThrow(ReadLimited);
    now = 29999;
    expect(() => admission.admit(other)).toThrow(ReadLimited);
    now = 30000;
    expect(() => admission.admit(other)).not.toThrow();
  });
  it('fails closed on unavailable time or invalid identity state', () => {
    let now = 12000;
    const admission = new ReadAdmission(settings, () => now);
    admission.admit(digest);
    now = 11999;
    expect(() => admission.admit(digest)).toThrow(ReadUnavailable);
    now = 13000;
    expect(() => admission.admit(Buffer.alloc(1))).toThrow(ReadUnavailable);
    now = Number.NaN;
    expect(() => admission.admit(digest)).toThrow(ReadUnavailable);
  });
  it.each(['', '0', '-1', '1.1', 'NaN', '2147483648'])(
    'rejects invalid maximum %s',
    (value) => {
      expect(
        () =>
          new ReadAdmission({
            ...settings,
            RATE_LIMIT_READ_MAX_REQUESTS: value,
          }),
      ).toThrow();
    },
  );
  it.each(['', '0', '86401', '1.5'])('rejects invalid window %s', (value) => {
    expect(
      () =>
        new ReadAdmission({
          ...settings,
          RATE_LIMIT_READ_WINDOW_SECONDS: value,
        }),
    ).toThrow();
  });
  it.each(['', '0', '100001', '1.5'])(
    'rejects invalid binding capacity %s',
    (value) => {
      expect(
        () =>
          new ReadAdmission({
            ...settings,
            MCP_RATE_LIMIT_MAX_BINDINGS: value,
          }),
      ).toThrow();
    },
  );
});

// Real SDK HTTP transport with a synthetic credential verifier, not chat-host compatibility proof.
it('retains reconnect quota, returns protocol resource errors and leaves tools to Core', async () => {
  const admission = new ReadAdmission(
    { ...settings, RATE_LIMIT_READ_MAX_REQUESTS: '50' },
    () => 12000,
  );
  let tools = 0;
  const server = new StreamableHttpTransport(async (token) => {
    if (token !== 'synthetic-verified-connector') throw new Error('Rejected');
    const sdk = new McpServer({ name: 'read-admission-fixture', version: '1' });
    sdk.registerResource('fixture', 'fixture://read', {}, async () => ({
      contents: [{ uri: 'fixture://read', text: 'safe fixture' }],
    }));
    sdk.registerTool('fixture_tool', {}, async () => {
      tools += 1;
      return { content: [{ type: 'text', text: 'safe tool result' }] };
    });
    return { server: sdk, close: () => sdk.close() };
  }, admission);
  const previousPort = process.env.MCP_PORT;
  process.env.MCP_PORT = '0';
  let client: Client | undefined;
  try {
    await server.listen({ httpPath: '/mcp', healthPort: 0 });
    const endpoint = new URL(`http://127.0.0.1:${server.port}/mcp`);
    const connect = async () => {
      const transport = new StreamableHTTPClientTransport(endpoint, {
        requestInit: {
          headers: { authorization: 'Bearer synthetic-verified-connector' },
        },
      });
      client = new Client({ name: 'admission-client', version: '1' });
      await client.connect(transport);
      return transport;
    };
    const first = await connect();
    await first.terminateSession();
    await client!.close();
    const second = await connect();
    // Fill the same private counter used before and after session deletion; no reset API exists.
    let additional = 0;
    while (additional < 50) {
      try {
        admission.admit(digest);
        additional += 1;
      } catch (failure) {
        expect(failure).toBeInstanceOf(ReadLimited);
        break;
      }
    }
    expect(additional).toBeLessThan(47);
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        authorization: 'Bearer synthetic-verified-connector',
        'mcp-session-id': second.sessionId!,
        'mcp-protocol-version': '2025-11-25',
        accept: 'application/json, text/event-stream',
        'content-type': 'application/json',
      },
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: 90,
        method: 'resources/list',
        params: {},
      }),
    });
    expect(response.status).toBe(429);
    expect(Number(response.headers.get('retry-after'))).toBeGreaterThanOrEqual(
      1,
    );
    expect(await response.json()).toMatchObject({
      jsonrpc: '2.0',
      id: 90,
      error: { code: -32000, data: { code: 'RATE_LIMITED' } },
    });
    const result = await client!.callTool({
      name: 'fixture_tool',
      arguments: {},
    });
    expect(result.isError).not.toBe(true);
    expect(tools).toBe(1);
    const reconnect = await fetch(endpoint, {
      method: 'POST',
      headers: {
        authorization: 'Bearer synthetic-verified-connector',
        'content-type': 'application/json',
      },
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: 91,
        method: 'initialize',
        params: {},
      }),
    });
    expect(reconnect.status).toBe(429);
    expect(JSON.stringify(await reconnect.json())).not.toContain(
      'synthetic-verified-connector',
    );
  } finally {
    await client?.close();
    await server.close();
    if (previousPort === undefined) delete process.env.MCP_PORT;
    else process.env.MCP_PORT = previousPort;
  }
});
