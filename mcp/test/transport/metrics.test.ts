import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { Client, StreamableHTTPClientTransport } from '@modelcontextprotocol/client';
import { McpMetrics } from '../../src/operations/metrics.js';
import { CoreReadiness } from '../../src/operations/readiness.js';
import { StreamableHttpTransport } from '../../src/transport/streamable-http.js';
import { createSession } from '../../src/gateway/session.js';
import { ReadAdmission } from '../../src/transport/read-admission.js';
import { Server, ServerCredentials } from '@grpc/grpc-js';
import type { ServerUnaryCall, sendUnaryData } from '@grpc/grpc-js';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

// Real HTTP/SDK and gRPC/mTLS boundaries with controlled gateway/health fixtures; not chat-host acceptance.
describe('private MCP exporter and logical observations', () => {
  let server: StreamableHttpTransport;
  let metrics: McpMetrics;
  let ready = false;
  let client: Client | undefined;
  let transport: StreamableHTTPClientTransport | undefined;
  let exporter: string;
  const secret = 'synthetic-credential-do-not-export';
  const id = '00000000-0000-4000-8000-000000000001';
  const policy = { revision: 'a'.repeat(64), textBounds: [] };
  beforeEach(async () => {
    process.env.MCP_PORT = '0'; ready = false; metrics = new McpMetrics();
    server = new StreamableHttpTransport(async (token) => {
      if (token !== secret) throw new Error('credential rejected');
      return createSession(token, {
        policy: async () => policy,
        context: async () => ({ policy, titles: { nodes: [], hasNextPage: false } }),
        operation: async (operationId) => ({ operationId, state: 'FAILED', resultExpired: false,
          problem: { code: 'PERMISSION_DENIED', fields: [] } }),
        propose: async () => { throw new Error('private-diagnostic-do-not-export'); }, close: () => undefined,
      }, metrics);
    }, new ReadAdmission(), metrics, { check: async () => ready, close: () => { ready = false; } });
    await server.listen({ httpPath: '/mcp', healthPort: 0 });
    exporter = `http://127.0.0.1:${server.healthPort}`;
  });
  afterEach(async () => { await client?.close(); await server.close(); client = undefined; transport = undefined; delete process.env.MCP_PORT; });
  async function connect() {
    transport = new StreamableHTTPClientTransport(new URL(`http://127.0.0.1:${server.port}/mcp`), {
      requestInit: { headers: { authorization: `Bearer ${secret}` } },
    });
    client = new Client({ name: 'private-client-name-do-not-export', version: '1' });
    await client.connect(transport);
    return client;
  }
  it('separates liveness readiness and metrics and exposes neither public metrics nor arbitrary routes', async () => {
    expect((await fetch(`${exporter}/health`)).status).toBe(200);
    expect((await fetch(`${exporter}/health/live`)).status).toBe(200);
    expect((await fetch(`${exporter}/health/ready`)).status).toBe(503);
    expect(await (await fetch(`${exporter}/metrics`)).text()).toContain('spoolin_service_ready{service="mcp"} 0');
    ready = true;
    expect((await fetch(`${exporter}/health/ready`)).status).toBe(200);
    const response = await fetch(`${exporter}/metrics`);
    expect(response.headers.get('content-type')).toBe('text/plain; version=0.0.4; charset=utf-8');
    expect(await response.text()).toContain('spoolin_service_ready{service="mcp"} 1');
    expect((await fetch(`${exporter}/metrics?credential=${secret}`)).status).toBe(404);
    expect((await fetch(`${exporter}/metrics`, { method: 'POST' })).status).toBe(405);
    expect((await fetch(`http://127.0.0.1:${server.port}/metrics`)).status).toBe(404);
  });
  it('records typed failures under HTTP200 and untyped failures as unknown without sensitive labels', async () => {
    const connected = await connect();
    await connected.callTool({ name: 'movie_context', arguments: { query: 'private-query-do-not-export', limit: 1 } });
    await connected.callTool({ name: 'movie_operation', arguments: { operationId: id } });
    await connected.callTool({ name: 'propose_movie', arguments: { operationId: id, title: 'Private title',
      synopsis: 'Private synopsis', genreIds: ['00000000-0000-4000-8000-000000000002'], release: { year: 2024 } } });
    const text = await (await fetch(`${exporter}/metrics`)).text();
    expect(text).toContain('operation="movie_operation",boundary="owner",outcome="DOMAIN_REJECTED",code="PERMISSION_DENIED"} 1');
    expect(text).toContain('operation="movie_operation",boundary="ingress",outcome="DOMAIN_REJECTED",code="PERMISSION_DENIED"} 1');
    expect(text).toContain('operation="movie_context",boundary="owner",outcome="SUCCESS",code="OK"} 1');
    expect(text).toContain('operation="propose_movie",boundary="owner",outcome="UNCERTAIN",code="UNKNOWN"} 1');
    expect(text).toContain('operation="movie_operation",boundary="owner",le="+Inf"} 1');
    [secret, id, 'private-query', 'Private title', 'private-diagnostic', 'private-client-name'].forEach((value) => expect(text).not.toContain(value));
    expect(text).toContain('spoolin_mcp_sessions{service="mcp",state="ACTIVE"} 1');
    await transport!.terminateSession();
    const after = await (await fetch(`${exporter}/metrics`)).text();
    expect(after).toContain('spoolin_mcp_sessions{service="mcp",state="ACTIVE"} 0');
    expect(after).toContain('spoolin_mcp_sessions_opened_total{service="mcp"} 1');
    expect(after).toContain('spoolin_mcp_sessions_closed_total{service="mcp"} 1');
  });
  it('folds unknown operations and codes into finite labels and emits every approved histogram bucket', async () => {
    await metrics.logical(`private-operation-${id}`, async () => ({ structuredContent: { problem: { code: secret } } }));
    const text = metrics.render(0, 0, false);
    expect(text).toContain('operation="unknown",boundary="owner",outcome="UNCERTAIN",code="UNKNOWN"} 1');
    expect(text).not.toContain(secret); expect(text).not.toContain(id);
    [0.01, 0.05, 0.1, 0.3, 0.5, 1, 3, 5, 15, 30].forEach((value) => expect(text).toContain(`le="${value}"}`));
    expect(text.endsWith('\n')).toBe(true);
  });
});

describe('real Core standard health over verified mutual TLS', () => {
  let directory: string;
  let server: Server;
  let probe: CoreReadiness | undefined;
  let health = 1;
  let identity = 1;
  let calls = 0;
  let config: Record<string, string>;
  beforeEach(async () => {
    health = 1; identity = 1; calls = 0;
    directory = mkdtempSync(join(tmpdir(), 'spoolin-readiness-'));
    certificateAuthority(directory); certificate(directory, 'server', true); certificate(directory, 'mcp', false);
    server = new Server();
    server.addService({ check: { path: '/grpc.health.v1.Health/Check', requestStream: false, responseStream: false,
      requestSerialize: (value: Buffer) => value, requestDeserialize: (value: Buffer) => value,
      responseSerialize: (value: Buffer) => value, responseDeserialize: (value: Buffer) => value } }, {
      check: (call: ServerUnaryCall<Buffer, Buffer>, callback: sendUnaryData<Buffer>) => {
        calls += 1; expect(call.metadata.get('authorization')).toEqual([]);
        const service = call.request.subarray(2).toString('utf8');
        callback(null, Buffer.from([8, service.includes('IdentityService') ? identity : health]));
      },
    });
    const tls = ServerCredentials.createSsl(readFileSync(join(directory, 'ca.crt')),
      [{ private_key: readFileSync(join(directory, 'server.key')), cert_chain: readFileSync(join(directory, 'server.crt')) }], true);
    const port = await new Promise<number>((resolve, reject) => server.bindAsync('localhost:0', tls, (error, value) => error ? reject(error) : resolve(value)));
    config = { CORE_GRPC_TARGET: `localhost:${port}`, GRPC_CA_FILE: join(directory, 'ca.crt'),
      GRPC_CLIENT_KEY_FILE: join(directory, 'mcp.key'), GRPC_CLIENT_CERT_FILE: join(directory, 'mcp.crt') };
  });
  afterEach(async () => { probe?.close(); probe = undefined; await new Promise<void>((resolve) => server.tryShutdown(() => resolve())); rmSync(directory, { recursive: true }); });
  it('checks both registered services without bearer and coalesces concurrent probes', async () => {
    probe = new CoreReadiness((name) => config[name]);
    expect(await Promise.all([probe.check(), probe.check(), probe.check()])).toEqual([true, true, true]);
    expect(calls).toBe(2); expect(await probe.check()).toBe(true); expect(calls).toBe(2);
    probe.close(); expect(await probe.check()).toBe(false);
  });
  it('fails readiness for either unavailable dependency and missing configuration', async () => {
    identity = 2;
    probe = new CoreReadiness((name) => config[name]); expect(await probe.check()).toBe(false);
    probe.close(); health = 2; identity = 1;
    probe = new CoreReadiness((name) => config[name]); expect(await probe.check()).toBe(false);
    const absent = new CoreReadiness(() => undefined); expect(await absent.check()).toBe(false); absent.close();
  });
  it('rejects a trusted certificate with mismatched server hostname', async () => {
    probe = new CoreReadiness((name) => name === 'CORE_GRPC_TARGET' ? config[name]!.replace('localhost', '127.0.0.1') : config[name]);
    expect(await probe.check()).toBe(false);
  });
});

function certificateAuthority(directory: string) {
  execFileSync('openssl', ['req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-keyout', join(directory, 'ca.key'),
    '-out', join(directory, 'ca.crt'), '-subj', '/CN=Synthetic readiness CA', '-days', '1'], { stdio: 'ignore' });
}
function certificate(directory: string, name: string, isServer: boolean) {
  const path = (suffix: string) => join(directory, `${name}.${suffix}`);
  execFileSync('openssl', ['req', '-newkey', 'rsa:2048', '-nodes', '-keyout', path('key'), '-out', path('csr'), '-subj', `/CN=${name}`], { stdio: 'ignore' });
  writeFileSync(path('ext'), isServer ? 'subjectAltName=DNS:localhost\nextendedKeyUsage=serverAuth\n' : 'subjectAltName=DNS:mcp\nextendedKeyUsage=clientAuth\n');
  execFileSync('openssl', ['x509', '-req', '-in', path('csr'), '-CA', join(directory, 'ca.crt'), '-CAkey', join(directory, 'ca.key'),
    '-CAcreateserial', '-out', path('crt'), '-days', '1', '-extfile', path('ext')], { stdio: 'ignore' });
}
