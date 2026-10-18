import http from 'node:http';
import { randomUUID, timingSafeEqual } from 'node:crypto';
import { NodeStreamableHTTPServerTransport } from '@modelcontextprotocol/node';
import { createSession } from '../gateway/session.js';
import { connectorTokenDigest } from '../identity/connector.js';
export { composeProposeMovieToolResult } from '../tools/propose-movie-result.js';
export { buildProposeMovieCallToolResult } from './propose-tool-result.js';

type Session = { transport: NodeStreamableHTTPServerTransport; digest: Buffer; touched: number; close: () => Promise<void> };
const MAX_SESSIONS = 100;
const SESSION_IDLE_MS = 15 * 60_000;

export class StreamableHttpTransport {
  constructor(private readonly sessionFactory = createSession) {}
  get port(): number {
    const address = this.listener?.address();
    if (!address || typeof address === 'string') throw new Error('MCP listener unavailable');
    return address.port;
  }
  private initializing = 0;
  private readonly sessions = new Map<string, Session>();
  private listener?: http.Server;
  private health?: http.Server;
  private cleanup?: ReturnType<typeof setInterval>;
  async listen(options: { httpPath: string; healthPort: number }): Promise<void> {
    const origins = new Set((process.env.MCP_ALLOWED_ORIGINS ?? '').split(',').filter(Boolean));
    const hosts = new Set((process.env.MCP_ALLOWED_HOSTS ?? 'localhost,127.0.0.1,mcp').split(','));
    this.listener = http.createServer((req, res) => {
      void this.handle(req, res, options.httpPath, origins, hosts).catch(() => {
        if (!res.headersSent) res.writeHead(502);
        res.end();
      });
    });
    this.listener.requestTimeout = 30_000;
    this.listener.headersTimeout = 10_000;
    this.health = http.createServer((req, res) => {
      res.writeHead(req.url === '/health' ? 200 : 404, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ status: 'UP' }));
    });
    await Promise.all([
      new Promise<void>((resolve, reject) => this.listener!.once('error', reject).listen(Number(process.env.MCP_PORT ?? '8787'), '0.0.0.0', resolve)),
      new Promise<void>((resolve, reject) => this.health!.once('error', reject).listen(options.healthPort, '0.0.0.0', resolve)),
    ]);
    this.cleanup = setInterval(() => {
      for (const [id, session] of this.sessions) {
        if (Date.now() - session.touched > SESSION_IDLE_MS) { this.sessions.delete(id); void session.close(); }
      }
    }, 60_000).unref();
  }
  private async handle(req: http.IncomingMessage, res: http.ServerResponse, path: string, origins: Set<string>, hosts: Set<string>): Promise<void> {
    if (req.url !== path) { res.writeHead(404).end(); return; }
    const host = new URL(`http://${req.headers.host ?? ''}`).hostname;
    if (!hosts.has(host) || (req.headers.origin && !origins.has(req.headers.origin))) { res.writeHead(403).end(); return; }
    const authorization = req.headers.authorization;
    if (!authorization?.startsWith('Bearer ') || authorization.length > 4096) { res.writeHead(401).end(); return; }
    const token = authorization.slice(7).trim();
    if (!token) { res.writeHead(401).end(); return; }
    const digest = connectorTokenDigest(token);
    const id = req.headers['mcp-session-id'];
    if (typeof id === 'string') {
      const session = this.sessions.get(id);
      if (!session || !timingSafeEqual(session.digest, digest)) { res.writeHead(404).end(); return; }
      session.touched = Date.now();
      await session.transport.handleRequest(req, res);
      return;
    }
    if (req.method !== 'POST' || this.sessions.size + this.initializing >= MAX_SESSIONS) { res.writeHead(req.method === 'POST' ? 429 : 400).end(); return; }
    // Core validates the live credential before an MCP session or any tool is allocated.
    this.initializing += 1;
    try { await this.initialize(req, res, token, digest); } finally { this.initializing -= 1; }
  }
  private async initialize(req: http.IncomingMessage, res: http.ServerResponse, token: string, digest: Buffer): Promise<void> {
    const service = await this.sessionFactory(token);
    let sessionId: string | undefined;
    const transport = new NodeStreamableHTTPServerTransport({
      sessionIdGenerator: randomUUID,
      maxRequestBodySize: 128 * 1024,
      onsessioninitialized: (value) => {
        sessionId = value;
        this.sessions.set(value, { transport, digest, touched: Date.now(), close: service.close });
      },
      onsessionclosed: async (value) => { this.sessions.delete(value); await service.close(); },
    });
    try {
      await service.server.connect(transport);
      await transport.handleRequest(req, res);
    } finally {
      if (!sessionId) await service.close();
    }
  }
  async close(): Promise<void> {
    if (this.cleanup) clearInterval(this.cleanup);
    await Promise.all([...this.sessions.values()].map((session) => session.close()));
    this.sessions.clear();
    this.listener?.closeAllConnections(); this.health?.closeAllConnections();
    await Promise.all([this.listener, this.health].filter((server) => server !== undefined).map((server) =>
      new Promise<void>((resolve) => server.close(() => resolve()))));
  }
}
