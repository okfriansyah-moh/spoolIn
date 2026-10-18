import { mcpDiagnostics, runMcpProcess } from '../operations/diagnostics.js';
import { initializeTracing, mcpTracing, shutdownTracing } from '../operations/tracing.js';
import { readFileSync } from 'node:fs';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/client';
import { StdioServerTransport } from '@modelcontextprotocol/server/stdio';

type Close = () => Promise<void>;
function configuration(): { url: URL; token: string } {
  const tokenFile = process.env.MCP_CONNECTOR_TOKEN_FILE;
  const endpoint = process.env.MCP_ENDPOINT;
  if (!tokenFile || !endpoint) throw new Error('Required bridge configuration unavailable');
  const url = new URL(endpoint);
  if (url.protocol !== 'https:' && !(url.protocol === 'http:' &&
    ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname))) throw new Error('MCP endpoint requires TLS outside loopback');
  const token = readFileSync(tokenFile, 'utf8').trim();
  if (!token || token.includes('\n')) throw new Error('Invalid connector credential file');
  return { url, token };
}
function closer(upstream: StreamableHTTPClientTransport, stdio: StdioServerTransport): Close {
  let closed = false;
  return async () => {
    if (closed) return;
    closed = true;
    await upstream.terminateSession().catch(() => undefined);
    try { await upstream.close(); }
    finally { await stdio.close(); }
  };
}
function route(upstream: StreamableHTTPClientTransport, stdio: StdioServerTransport, close: Close): void {
  const stop = () => { void close().catch(() => mcpDiagnostics.emit('transport_failed')); };
  upstream.onmessage = (message) => {
    void mcpTracing.stdio(async () => {
      if ('result' in message && typeof message.result.protocolVersion === 'string')
        upstream.setProtocolVersion(message.result.protocolVersion);
      await stdio.send(message);
    }).catch(stop);
  };
  stdio.onmessage = (message) => {
    void mcpTracing.stdio(() => upstream.send(message)).catch(() => { process.exitCode = 1; stop(); });
  };
  upstream.onerror = () => {
    void mcpTracing.stdio(async () => { mcpDiagnostics.emit('transport_failed', { error_class: 'transport' }); });
  };
  stdio.onclose = stop;
}
/** Transparent bridge: protocol messages are never diagnostic fields or stdout log records. */
async function start(register: (close: Close) => void): Promise<void> {
  initializeTracing();
  const { url, token } = configuration();
  const upstream = new StreamableHTTPClientTransport(url, {
    requestInit: { headers: { authorization: `Bearer ${token}` } },
    reconnectionOptions: { maxRetries: 2, initialReconnectionDelay: 1000,
      maxReconnectionDelay: 5000, reconnectionDelayGrowFactor: 2 },
  });
  const stdio = new StdioServerTransport();
  const close = closer(upstream, stdio);
  route(upstream, stdio, close);
  register(async () => {
    try { await close(); }
    finally { await shutdownTracing(); }
  });
  await upstream.start();
  await stdio.start();
}
await runMcpProcess(start);
