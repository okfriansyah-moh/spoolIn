import http from 'node:http';
import { randomUUID, timingSafeEqual } from 'node:crypto';
import { NodeStreamableHTTPServerTransport } from '@modelcontextprotocol/node';
import { createSession } from '../gateway/session.js';
import { connectorTokenDigest } from '../identity/connector.js';
import { ReadAdmission, ReadLimited } from './read-admission.js';
export { composeProposeMovieToolResult } from '../tools/propose-movie-result.js';
export { buildProposeMovieCallToolResult } from './propose-tool-result.js';

type Session = { transport: NodeStreamableHTTPServerTransport; digest: Buffer; touched: number; close: () => Promise<void> };
const MAX_SESSIONS = 100;
const SESSION_IDLE_MS = 15 * 60_000;

export class StreamableHttpTransport {
  constructor(private readonly sessionFactory = createSession, private readonly admission = new ReadAdmission()) {}
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
        if (!res.headersSent) res.writeHead(503);
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
      const body = await this.body(req, res);
      if (res.writableEnded || !this.admit(req, res, digest, body, true)) return;
      session.touched = Date.now();
      await session.transport.handleRequest(req, res, body);
      return;
    }
    if (req.method !== 'POST' || this.sessions.size + this.initializing >= MAX_SESSIONS) { res.writeHead(req.method === 'POST' ? 429 : 400, req.method === 'POST' ? { 'Retry-After': '1' } : {}).end(); return; }
    const body = await this.body(req, res);
    if (res.writableEnded) return;
    // Core validates the live credential before an MCP session or any tool is allocated.
    this.initializing += 1;
    try { await this.initialize(req, res, token, digest, body); } finally { this.initializing -= 1; }
  }
  private async initialize(req: http.IncomingMessage, res: http.ServerResponse, token: string, digest: Buffer, body: unknown): Promise<void> {
    const service = await this.sessionFactory(token);
    if (!this.admit(req, res, digest, body, false)) { await service.close(); return; }
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
      await transport.handleRequest(req, res, body);
    } finally {
      if (!sessionId) await service.close();
    }
  }
  private async body(req: http.IncomingMessage, res: http.ServerResponse): Promise<unknown> {
    if (req.method !== 'POST') return undefined;
    const chunks: Buffer[] = [];
    let bytes = 0;
    for await (const chunk of req) {
      const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      bytes += buffer.length;
      if (bytes > 128 * 1024) { res.writeHead(413).end(); return undefined; }
      chunks.push(buffer);
    }
    try {
      const parsed: unknown = JSON.parse(Buffer.concat(chunks).toString('utf8'));
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error('Invalid request');
      return parsed;
    } catch { res.writeHead(400).end(); return undefined; }
  }
  private admit(req: http.IncomingMessage, res: http.ServerResponse, digest: Buffer, body: unknown, established: boolean): boolean {
    const message = body as { method?: unknown; id?: unknown } | undefined;
    if (established && req.method === 'POST' && message?.method === 'tools/call') return true;
    try { this.admission.admit(digest); return true; }
    catch (failure) {
      const limited = failure instanceof ReadLimited;
      const retryAfterMs = limited ? failure.retryAfterMs : undefined;
      const status = limited ? 429 : 503;
      const problem = limited ? { code: 'RATE_LIMITED', retryAfterMs } : { code: 'UNAVAILABLE' };
      res.writeHead(status, { 'content-type': 'application/json',
        ...(limited ? { 'Retry-After': String(Math.max(1, Math.ceil(failure.retryAfterMs / 1000))) } : {}) });
      res.end(JSON.stringify(established && req.method === 'POST' ? {
        jsonrpc: '2.0', id: typeof message?.id === 'string' || typeof message?.id === 'number' ? message.id : null,
        error: { code: -32000, message: limited ? 'Request limit reached.' : 'Service unavailable.', data: problem },
      } : { problem }));
      return false;
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
