import { context as traceContext } from '@opentelemetry/api';
import { mcpDiagnostics } from './diagnostics.js';
import { AsyncLocalStorage } from 'node:async_hooks';
import type http from 'node:http';

const operations = new Set([
  'initialize',
  'notifications/initialized',
  'ping',
  'tools/list',
  'resources/list',
  'resources/read',
  'movie_context',
  'movie_operation',
  'propose_movie',
  'unknown',
]);
const codes = new Set([
  'OK',
  'INVALID_ARGUMENT',
  'PERMISSION_DENIED',
  'NOT_FOUND',
  'UNAUTHENTICATED',
  'RATE_LIMITED',
  'STALE_VERSION',
  'POLICY_CHANGED',
  'CONFLICT',
  'UNAVAILABLE',
  'UNKNOWN',
  'FIELD_INVALID',
  'FORBIDDEN',
  'IDEMPOTENCY_CONFLICT',
  'RESOURCE_EXHAUSTED',
  'DEADLINE_EXCEEDED',
]);
const buckets = [0.01, 0.05, 0.1, 0.3, 0.5, 1, 3, 5, 15, 30];
type Result = {
  outcome: 'SUCCESS' | 'DOMAIN_REJECTED' | 'SYSTEM_FAILED' | 'UNCERTAIN';
  code: string;
};
type RequestState = { operation: string; result?: Result };
type Histogram = { count: number; sum: number; buckets: number[] };

/** Finite labels only. No request headers, IDs, paths, content or errors enter the registry. */
export class McpMetrics {
  private readonly context = new AsyncLocalStorage<RequestState>();
  private readonly results = new Map<string, number>();
  private readonly durations = new Map<string, Histogram>();
  private opened = 0;
  private closed = 0;

  ingress(
    _req: http.IncomingMessage,
    res: http.ServerResponse,
    handle: () => Promise<void>,
  ): Promise<void> {
    const state: RequestState = { operation: 'unknown' };
    const started = process.hrtime.bigint();
    const tracing = traceContext.active();
    let finished = false;
    const finish = () => {
      if (finished) return;
      finished = true;
      traceContext.with(tracing, () =>
        this.record(
          state.operation,
          'ingress',
          state.result ?? httpResult(res.statusCode, res.writableFinished),
          started,
        ),
      );
    };
    res.once('finish', finish);
    res.once('close', finish);
    return this.context.run(state, handle);
  }

  protocol(body: unknown): void {
    const state = this.context.getStore();
    if (!state || !body || typeof body !== 'object') return;
    const message = body as { method?: unknown; params?: { name?: unknown } };
    const value =
      message.method === 'tools/call' ? message.params?.name : message.method;
    state.operation =
      typeof value === 'string' && operations.has(value) ? value : 'unknown';
  }

  async logical<T>(operation: string, handle: () => Promise<T>): Promise<T> {
    const started = process.hrtime.bigint();
    try {
      const value = await handle();
      this.logicalResult(operation, toolResult(value, operation), started);
      return value;
    } catch (failure) {
      this.logicalResult(
        operation,
        { outcome: 'UNCERTAIN', code: 'UNKNOWN' },
        started,
      );
      throw failure;
    }
  }

  private logicalResult(
    operation: string,
    result: Result,
    started: bigint,
  ): void {
    const state = this.context.getStore();
    if (state) state.result = result;
    this.record(operation, 'owner', result, started);
  }

  sessionOpened(): void {
    this.opened += 1;
  }
  sessionClosed(): void {
    this.closed += 1;
  }

  private record(
    operation: string,
    boundary: 'owner' | 'ingress',
    result: Result,
    started: bigint,
  ): void {
    const fixed = operations.has(operation) ? operation : 'unknown';
    const code = codes.has(result.code) ? result.code : 'UNKNOWN';
    const labels = `service="mcp",operation="${fixed}",boundary="${boundary}"`;
    const key = `${labels},outcome="${result.outcome}",code="${code}"`;
    mcpDiagnostics.emit(logEvent(result), {
      operation: fixed,
      boundary,
      outcome: result.outcome,
      code,
      duration_ms: Number(process.hrtime.bigint() - started) / 1e6,
    });
    this.results.set(key, (this.results.get(key) ?? 0) + 1);
    this.duration(labels, started);
  }
  private duration(labels: string, started: bigint): void {
    const seconds = Number(process.hrtime.bigint() - started) / 1e9;
    const histogram = this.durations.get(labels) ?? {
      count: 0,
      sum: 0,
      buckets: buckets.map(() => 0),
    };
    histogram.count += 1;
    histogram.sum += seconds;
    buckets.forEach((limit, index) => {
      if (seconds <= limit) histogram.buckets[index]! += 1;
    });
    this.durations.set(labels, histogram);
  }

  render(sessions: number, initializing: number, ready: boolean): string {
    const lines = ['# TYPE spoolin_operation_results_total counter'];
    [...this.results]
      .sort()
      .forEach(([labels, value]) =>
        lines.push(`spoolin_operation_results_total{${labels}} ${value}`),
      );
    lines.push('# TYPE spoolin_operation_duration_seconds histogram');
    [...this.durations]
      .sort()
      .forEach(([labels, histogram]) =>
        lines.push(...renderHistogram(labels, histogram)),
      );
    lines.push(
      '# TYPE spoolin_service_ready gauge',
      `spoolin_service_ready{service="mcp"} ${ready ? 1 : 0}`,
      '# TYPE spoolin_mcp_sessions gauge',
      `spoolin_mcp_sessions{service="mcp",state="ACTIVE"} ${sessions}`,
      `spoolin_mcp_sessions{service="mcp",state="INITIALIZING"} ${initializing}`,
      '# TYPE spoolin_mcp_sessions_opened_total counter',
      `spoolin_mcp_sessions_opened_total{service="mcp"} ${this.opened}`,
      '# TYPE spoolin_mcp_sessions_closed_total counter',
      `spoolin_mcp_sessions_closed_total{service="mcp"} ${this.closed}`,
    );
    lines.push(...mcpDiagnostics.sink.render());
    return lines.join('\n') + '\n';
  }
}

function renderHistogram(labels: string, histogram: Histogram): string[] {
  const lines: string[] = [];
  buckets.forEach((limit, index) =>
    lines.push(
      `spoolin_operation_duration_seconds_bucket{${labels},le="${limit}"} ${histogram.buckets[index]}`,
    ),
  );
  lines.push(
    `spoolin_operation_duration_seconds_bucket{${labels},le="+Inf"} ${histogram.count}`,
    `spoolin_operation_duration_seconds_sum{${labels}} ${histogram.sum}`,
    `spoolin_operation_duration_seconds_count{${labels}} ${histogram.count}`,
  );
  return lines;
}

function logEvent(result: Result) {
  if (result.outcome === 'SYSTEM_FAILED') return 'request_failed';
  if (result.outcome === 'UNCERTAIN') return 'request_uncertain';
  if (
    [
      'PERMISSION_DENIED',
      'UNAUTHENTICATED',
      'FORBIDDEN',
      'RATE_LIMITED',
      'RESOURCE_EXHAUSTED',
    ].includes(result.code)
  )
    return 'request_denied';
  return 'request_completed';
}

function httpResult(status: number, finished: boolean): Result {
  if (!finished) return { outcome: 'UNCERTAIN', code: 'UNKNOWN' };
  // HTTP 200 alone cannot establish the SDK's logical JSON-RPC outcome.
  if (status < 400) return { outcome: 'UNCERTAIN', code: 'UNKNOWN' };
  const code = new Map([
    [400, 'INVALID_ARGUMENT'],
    [401, 'UNAUTHENTICATED'],
    [403, 'PERMISSION_DENIED'],
    [404, 'NOT_FOUND'],
    [413, 'INVALID_ARGUMENT'],
    [429, 'RATE_LIMITED'],
  ]).get(status);
  return code
    ? { outcome: 'DOMAIN_REJECTED', code }
    : { outcome: 'SYSTEM_FAILED', code: 'UNAVAILABLE' };
}

type ToolContent = {
  state?: string;
  operationId?: string;
  problem?: { code?: string };
};

function operationStatus(
  content: ToolContent,
  operation: string,
  isError?: boolean,
): boolean {
  return (
    operation === 'movie_operation' &&
    !isError &&
    typeof content.operationId === 'string' &&
    ['PENDING', 'SUCCEEDED', 'FAILED', 'EXPIRED'].includes(content.state ?? '')
  );
}

function problemResult(problem: { code?: string }): Result {
  if (!problem.code || problem.code === 'OK' || !codes.has(problem.code))
    return { outcome: 'UNCERTAIN', code: 'UNKNOWN' };
  return {
    outcome: ['UNAVAILABLE', 'DEADLINE_EXCEEDED'].includes(problem.code)
      ? 'SYSTEM_FAILED'
      : 'DOMAIN_REJECTED',
    code: problem.code,
  };
}

function toolResult(value: unknown, operation: string): Result {
  if (!value || typeof value !== 'object')
    return { outcome: 'UNCERTAIN', code: 'UNKNOWN' };
  const result = value as {
    isError?: boolean;
    structuredContent?: ToolContent;
  };
  const content = result.structuredContent;
  if (!content) return { outcome: 'UNCERTAIN', code: 'UNKNOWN' };
  if (operationStatus(content, operation, result.isError))
    return { outcome: 'SUCCESS', code: 'OK' };
  if (content.problem) return problemResult(content.problem);
  if (
    result.isError ||
    ['PENDING', 'FAILED', 'EXPIRED'].includes(content.state ?? '')
  )
    return { outcome: 'UNCERTAIN', code: 'UNKNOWN' };
  return { outcome: 'SUCCESS', code: 'OK' };
}

export const mcpMetrics = new McpMetrics();
