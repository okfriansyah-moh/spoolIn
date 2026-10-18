import http from 'node:http';
import { mcpTracing } from '../operations/tracing.js';
import { mcpDiagnostics } from '../operations/diagnostics.js';
import { GatewayFailure } from '../operations/gateway-failure.js';
import { randomUUID, timingSafeEqual } from 'node:crypto';
import { NodeStreamableHTTPServerTransport } from '@modelcontextprotocol/node';
import { createSession } from '../gateway/session.js';
import { connectorTokenDigest } from '../identity/connector.js';
import { McpMetrics, mcpMetrics } from '../operations/metrics.js';
import { CoreReadiness } from '../operations/readiness.js';
import type { ReadinessProbe } from '../operations/readiness.js';
import { ReadAdmission, ReadLimited } from './read-admission.js';
export { composeProposeMovieToolResult } from '../tools/propose-movie-result.js';
export { buildProposeMovieCallToolResult } from './propose-tool-result.js';

type Session = {
  transport: NodeStreamableHTTPServerTransport;
  digest: Buffer;
  touched: number;
  close: () => Promise<void>;
};
const MAX_SESSIONS = 100;
const SESSION_IDLE_MS = 15 * 60_000;

function refusalBody(
  established: boolean,
  id: unknown,
  problem: { code: string; retryAfterMs?: number },
  limited: boolean,
) {
  if (!established) return { problem };
  return {
    jsonrpc: '2.0',
    id: typeof id === 'string' || typeof id === 'number' ? id : null,
    error: {
      code: -32000,
      message: limited ? 'Request limit reached.' : 'Service unavailable.',
      data: problem,
    },
  };
}

export class StreamableHttpTransport {
  constructor(
    private readonly sessionFactory = createSession,
    private readonly admission = new ReadAdmission(),
    private readonly metrics: McpMetrics = mcpMetrics,
    private readonly readiness: ReadinessProbe = new CoreReadiness(),
  ) {}
  get port(): number {
    const address = this.listener?.address();
    if (!address || typeof address === 'string')
      throw new Error('MCP listener unavailable');
    return address.port;
  }
  get healthPort(): number {
    const address = this.health?.address();
    if (!address || typeof address === 'string')
      throw new Error('MCP exporter unavailable');
    return address.port;
  }
  private closing = false;
  private initializing = 0;
  private readonly sessions = new Map<string, Session>();
  private listener?: http.Server;
  private health?: http.Server;
  private cleanup?: ReturnType<typeof setInterval>;
  async listen(options: {
    httpPath: string;
    healthPort: number;
  }): Promise<void> {
    const origins = new Set(
      (process.env.MCP_ALLOWED_ORIGINS ?? '').split(',').filter(Boolean),
    );
    const hosts = new Set(
      (process.env.MCP_ALLOWED_HOSTS ?? 'localhost,127.0.0.1,mcp').split(','),
    );
    this.listener = this.connector(options.httpPath, origins, hosts);
    this.health = this.exporter();
    await this.bind(options.healthPort);
    mcpDiagnostics.emit('ready');
    this.cleanup = setInterval(() => this.expireSessions(), 60_000).unref();
  }
  private connector(
    path: string,
    origins: Set<string>,
    hosts: Set<string>,
  ): http.Server {
    const listener = http.createServer((req, res) => {
      void mcpTracing
        .ingress(req, res, () =>
          this.metrics.ingress(req, res, () =>
            this.handle(req, res, path, origins, hosts),
          ),
        )
        .catch((failure: unknown) => this.refuseFailure(res, failure));
    });
    listener.maxConnections = 128;
    listener.maxRequestsPerSocket = 1000;
    listener.requestTimeout = 30_000;
    listener.headersTimeout = 10_000;
    return listener;
  }
  private refuseFailure(res: http.ServerResponse, failure: unknown): void {
    const code = failure instanceof GatewayFailure ? failure.problem.code : 'UNAVAILABLE';
    const status = code === 'UNAUTHENTICATED' ? 401 : code === 'FORBIDDEN' ? 403 :
      code === 'RESOURCE_EXHAUSTED' ? 429 : 503;
    mcpDiagnostics.emit(status === 503 ? 'request_failed' : 'request_denied', {
      code, error_class: 'dependency', boundary: 'ingress',
    });
    if (res.writableEnded) return;
    if (!res.headersSent) {
      res.writeHead(status, { 'content-type': 'application/json', 'cache-control': 'no-store' });
      res.end(JSON.stringify({ problem: { code } }));
    } else {
      res.end();
    }
  }
  private exporter(): http.Server {
    const health = http.createServer((req, res) => {
      void this.operational(req, res).catch(() => {
        res.writeHead(503).end();
      });
    });
    health.maxConnections = 16;
    health.maxRequestsPerSocket = 100;
    health.requestTimeout = 2000;
    health.headersTimeout = 2000;
    return health;
  }
  private async bind(healthPort: number): Promise<void> {
    await Promise.all([
      new Promise<void>((resolve, reject) =>
        this.listener!.once('error', reject).listen(
          Number(process.env.MCP_PORT ?? '8787'),
          '0.0.0.0',
          resolve,
        ),
      ),
      new Promise<void>((resolve, reject) =>
        this.health!.once('error', reject).listen(
          healthPort,
          '0.0.0.0',
          resolve,
        ),
      ),
    ]);
  }
  private expireSessions(): void {
    for (const [id, session] of this.sessions) {
      if (Date.now() - session.touched > SESSION_IDLE_MS) {
        if (this.sessions.delete(id)) this.metrics.sessionClosed();
        void session
          .close()
          .catch(() => mcpDiagnostics.emit('session_cleanup_failed'));
      }
    }
  }

  private async operational(
    req: http.IncomingMessage,
    res: http.ServerResponse,
  ): Promise<void> {
    if (req.method !== 'GET') {
      res.writeHead(405, { Allow: 'GET' }).end();
      return;
    }
    if (req.url === '/health' || req.url === '/health/live') {
      res
        .writeHead(200, {
          'content-type': 'application/json',
          'cache-control': 'no-store',
        })
        .end(JSON.stringify({ status: 'UP' }));
      return;
    }
    if (req.url !== '/health/ready' && req.url !== '/metrics') {
      res.writeHead(404).end();
      return;
    }
    const ready = !this.closing && (await this.readiness.check());
    if (req.url === '/health/ready') {
      res
        .writeHead(ready ? 200 : 503, {
          'content-type': 'application/json',
          'cache-control': 'no-store',
        })
        .end(JSON.stringify({ status: ready ? 'UP' : 'DOWN' }));
      return;
    }
    res
      .writeHead(200, {
        'content-type': 'text/plain; version=0.0.4; charset=utf-8',
        'cache-control': 'no-store',
      })
      .end(this.metrics.render(this.sessions.size, this.initializing, ready));
  }
  private async handle(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    path: string,
    origins: Set<string>,
    hosts: Set<string>,
  ): Promise<void> {
    const token = this.authorize(req, res, path, origins, hosts);
    if (!token) return;
    const digest = connectorTokenDigest(token);
    const id = req.headers['mcp-session-id'];
    if (typeof id === 'string') {
      await this.existing(req, res, id, digest);
      return;
    }
    if (req.method !== 'POST') {
      res.writeHead(400).end();
      return;
    }
    const body = await this.body(req, res);
    if (res.writableEnded) return;
    if (this.sessions.size + this.initializing >= MAX_SESSIONS) {
      res.writeHead(429, { 'Retry-After': '1' }).end();
      return;
    }
    // Core validates the live credential before an MCP session or any tool is allocated.
    this.initializing += 1;
    try {
      await this.initialize(req, res, token, digest, body);
    } finally {
      this.initializing -= 1;
    }
  }
  private authorize(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    path: string,
    origins: Set<string>,
    hosts: Set<string>,
  ): string | undefined {
    if (req.url !== path) {
      res.writeHead(404).end();
      return undefined;
    }
    if (!this.allowed(req, origins, hosts)) {
      res.writeHead(403).end();
      return undefined;
    }
    const authorization = req.headers.authorization;
    if (!authorization?.startsWith('Bearer ') || authorization.length > 4096) {
      res.writeHead(401).end();
      return undefined;
    }
    const token = authorization.slice(7).trim();
    if (!token) {
      res.writeHead(401).end();
      return undefined;
    }
    return token;
  }
  private allowed(
    req: http.IncomingMessage,
    origins: Set<string>,
    hosts: Set<string>,
  ): boolean {
    const host = new URL(`http://${req.headers.host ?? ''}`).hostname;
    return (
      hosts.has(host) &&
      (!req.headers.origin || origins.has(req.headers.origin))
    );
  }
  private async existing(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    id: string,
    digest: Buffer,
  ): Promise<void> {
    const session = this.sessions.get(id);
    if (!session || !timingSafeEqual(session.digest, digest)) {
      res.writeHead(404).end();
      return;
    }
    const body = await this.body(req, res);
    if (res.writableEnded || !this.admit(req, res, digest, body, true)) return;
    session.touched = Date.now();
    await session.transport.handleRequest(req, res, body);
  }
  private async initialize(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    token: string,
    digest: Buffer,
    body: unknown,
  ): Promise<void> {
    const service = await this.sessionFactory(token);
    if (!this.admit(req, res, digest, body, false)) {
      await service.close();
      return;
    }
    let sessionId: string | undefined;
    const transport = new NodeStreamableHTTPServerTransport({
      sessionIdGenerator: randomUUID,
      maxRequestBodySize: 128 * 1024,
      onsessioninitialized: (value) => {
        sessionId = value;
        this.sessions.set(value, {
          transport,
          digest,
          touched: Date.now(),
          close: service.close,
        });
        this.metrics.sessionOpened();
      },
      onsessionclosed: async (value) => {
        if (this.sessions.delete(value)) this.metrics.sessionClosed();
        await service.close();
      },
    });
    try {
      await service.server.connect(transport);
      await transport.handleRequest(req, res, body);
    } finally {
      if (!sessionId) await service.close();
    }
  }
  private async body(
    req: http.IncomingMessage,
    res: http.ServerResponse,
  ): Promise<unknown> {
    if (req.method !== 'POST') return undefined;
    const chunks: Buffer[] = [];
    let bytes = 0;
    for await (const chunk of req) {
      const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      bytes += buffer.length;
      if (bytes > 128 * 1024) {
        res.writeHead(413).end();
        return undefined;
      }
      chunks.push(buffer);
    }
    try {
      const parsed: unknown = JSON.parse(
        Buffer.concat(chunks).toString('utf8'),
      );
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
        throw new Error('Invalid request');
      this.metrics.protocol(parsed);
      return parsed;
    } catch {
      res.writeHead(400).end();
      return undefined;
    }
  }
  private admit(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    digest: Buffer,
    body: unknown,
    established: boolean,
  ): boolean {
    const message = body as { method?: unknown; id?: unknown } | undefined;
    if (
      established &&
      req.method === 'POST' &&
      message?.method === 'tools/call'
    )
      return true;
    try {
      this.admission.admit(digest);
      return true;
    } catch (failure) {
      this.denied(req, res, message, established, failure);
      return false;
    }
  }
  private denied(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    message: { id?: unknown } | undefined,
    established: boolean,
    failure: unknown,
  ): void {
    const limited = failure instanceof ReadLimited;
    const problem = limited
      ? { code: 'RATE_LIMITED', retryAfterMs: failure.retryAfterMs }
      : { code: 'UNAVAILABLE' };
    res.writeHead(limited ? 429 : 503, {
      'content-type': 'application/json',
      ...(limited
        ? {
            'Retry-After': String(
              Math.max(1, Math.ceil(failure.retryAfterMs / 1000)),
            ),
          }
        : {}),
    });
    res.end(
      JSON.stringify(
        refusalBody(
          established && req.method === 'POST',
          message?.id,
          problem,
          limited,
        ),
      ),
    );
  }

  private stopping?: Promise<void>;
  close(): Promise<void> {
    this.stopping ??= this.shutdown();
    return this.stopping;
  }
  private async shutdown(): Promise<void> {
    this.closing = true;
    let readinessFailed = false;
    try {
      this.readiness.close();
    } catch {
      readinessFailed = true;
      mcpDiagnostics.emit('transport_failed');
    }
    if (this.cleanup) clearInterval(this.cleanup);
    const sessionResults = await Promise.allSettled(
      [...this.sessions.values()].map((session) => session.close()),
    );
    this.sessions.forEach(() => this.metrics.sessionClosed());
    this.sessions.clear();
    this.listener?.closeAllConnections();
    this.health?.closeAllConnections();
    await Promise.all(
      [this.listener, this.health]
        .filter((server) => server !== undefined)
        .map(
          (server) =>
            new Promise<void>((resolve) => server.close(() => resolve())),
        ),
    );
    if (
      readinessFailed ||
      sessionResults.some((result) => result.status === 'rejected')
    ) {
      mcpDiagnostics.emit('session_cleanup_failed');
      throw new Error('MCP shutdown failed');
    }
  }
}
