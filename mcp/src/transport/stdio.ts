import { readFileSync } from 'node:fs';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/client';
import { StdioServerTransport } from '@modelcontextprotocol/server/stdio';

/** Transparent protocol bridge: the host negotiates directly with the same HTTP session. */
const tokenFile = process.env.MCP_CONNECTOR_TOKEN_FILE;
const endpoint = process.env.MCP_ENDPOINT;
if (!tokenFile || !endpoint)
  throw new Error('MCP_CONNECTOR_TOKEN_FILE and MCP_ENDPOINT are required');
const url = new URL(endpoint);
if (
  url.protocol !== 'https:' &&
  !(
    url.protocol === 'http:' &&
    ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)
  )
) {
  throw new Error('MCP endpoint requires TLS outside loopback');
}
const token = readFileSync(tokenFile, 'utf8').trim();
if (!token || token.includes('\n'))
  throw new Error('Invalid connector credential file');
const upstream = new StreamableHTTPClientTransport(url, {
  requestInit: { headers: { authorization: `Bearer ${token}` } },
  reconnectionOptions: {
    maxRetries: 2,
    initialReconnectionDelay: 1000,
    maxReconnectionDelay: 5000,
    reconnectionDelayGrowFactor: 2,
  },
});
const stdio = new StdioServerTransport();
let closed = false;
const close = async () => {
  if (closed) return;
  closed = true;
  await upstream.terminateSession().catch(() => undefined);
  await upstream.close();
  await stdio.close();
};
upstream.onmessage = (message) => {
  if (
    'result' in message &&
    typeof message.result.protocolVersion === 'string'
  ) {
    upstream.setProtocolVersion(message.result.protocolVersion);
  }
  void stdio.send(message).catch(close);
};
stdio.onmessage = (message) => {
  void upstream.send(message).catch(() => {
    process.stderr.write('MCP upstream unavailable\n');
    void close();
  });
};
upstream.onerror = () => {
  process.stderr.write('MCP transport failure\n');
};
stdio.onclose = () => {
  void close();
};
process.once('SIGTERM', () => {
  void close();
});
process.once('SIGINT', () => {
  void close();
});
await upstream.start();
await stdio.start();
