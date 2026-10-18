import http from 'node:http';
import { connectorTokenDigest } from '../identity/connector.js';
import { composeProposeMovieToolResult } from '../tools/propose-movie-result.js';
import { buildProposeMovieCallToolResult } from './propose-tool-result.js';
import { toolRegistry } from '../tools/registry.js';

export { composeProposeMovieToolResult, buildProposeMovieCallToolResult };

type JsonRpcRequest = Readonly<{
  method?: string;
  params?: Readonly<{
    name?: string;
    gatewayRevision?: Record<string, unknown>;
  }>;
}>;

async function readJsonBody(req: http.IncomingMessage): Promise<unknown> {
  const chunks: Buffer[] = [];
  for await (const chunk of req) {
    chunks.push(typeof chunk === 'string' ? Buffer.from(chunk) : chunk);
  }
  const raw = Buffer.concat(chunks).toString('utf8').trim();
  if (!raw) return null;
  return JSON.parse(raw) as unknown;
}

function unauthorized(res: http.ServerResponse): void {
  res.writeHead(401, { 'content-type': 'application/json' });
  res.end(JSON.stringify({ error: 'connector bearer required' }));
}

function requireBearer(req: http.IncomingMessage): string | null {
  const auth = req.headers.authorization ?? '';
  if (!auth.startsWith('Bearer ')) return null;
  const token = auth.slice('Bearer '.length).trim();
  return token || null;
}

export class StreamableHttpTransport {
  async listen(options: { httpPath: string; healthPort: number }): Promise<void> {
    const server = http.createServer(async (req, res) => {
      if (req.url === options.httpPath && req.method === 'GET') {
        const token = requireBearer(req);
        if (!token) return unauthorized(res);
        const digest = connectorTokenDigest(token).toString('hex');
        res.writeHead(200, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ tools: toolRegistry, tokenDigestPreview: digest.slice(0, 8) }));
        return;
      }
      if (req.url === options.httpPath && req.method === 'POST') {
        const token = requireBearer(req);
        if (!token) return unauthorized(res);
        try {
          const payload = (await readJsonBody(req)) as JsonRpcRequest | null;
          if (payload?.method === 'tools/call' && payload.params?.name === 'propose_movie') {
            const revision = payload.params.gatewayRevision;
            if (!revision || typeof revision !== 'object') {
              res.writeHead(400, { 'content-type': 'application/json' });
              res.end(JSON.stringify({ error: 'gatewayRevision required for propose_movie' }));
              return;
            }
            const result = buildProposeMovieCallToolResult(revision as Record<string, unknown>);
            res.writeHead(200, { 'content-type': 'application/json' });
            res.end(JSON.stringify({ result }));
            return;
          }
          res.writeHead(400, { 'content-type': 'application/json' });
          res.end(JSON.stringify({ error: 'unsupported MCP request' }));
        } catch {
          res.writeHead(400, { 'content-type': 'application/json' });
          res.end(JSON.stringify({ error: 'invalid JSON body' }));
        }
        return;
      }
      if (req.url === '/health') {
        res.writeHead(200, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ status: 'starting' }));
        return;
      }
      res.writeHead(404).end();
    });
    await new Promise<void>((resolve) => server.listen(options.healthPort, resolve));
  }
}
