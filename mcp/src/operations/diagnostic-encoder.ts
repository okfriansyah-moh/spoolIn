import { context, isSpanContextValid, trace } from '@opentelemetry/api';

export type Level = 'debug' | 'info' | 'warning' | 'error' | 'fatal';
export const events = {
  ready: ['info', 'MCP service ready'],
  stopped: ['info', 'MCP service stopped'],
  startup_failed: ['fatal', 'MCP service could not start'],
  process_failed: ['fatal', 'MCP process must stop safely'],
  branch_debug: ['debug', 'MCP execution branch selected'],
  request_completed: ['info', 'MCP request completed'],
  request_denied: ['warning', 'MCP request denied'],
  request_uncertain: ['warning', 'MCP request outcome uncertain'],
  request_failed: ['error', 'MCP request failed'],
  transport_failed: ['error', 'MCP transport failed'],
  session_cleanup_failed: ['error', 'MCP session cleanup failed'],
  logger_contract: ['error', 'MCP diagnostic contract rejected'],
} as const;
export type Event = keyof typeof events;
export type Resource = Readonly<{
  'service.name': 'mcp';
  'service.namespace': 'spoolin';
  'service.version': string;
  'service.env': 'local' | 'test';
}>;
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
const outcomes = new Set([
  'SUCCESS',
  'DOMAIN_REJECTED',
  'SYSTEM_FAILED',
  'UNCERTAIN',
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
const fixed = new Map<string, ReadonlySet<string>>([
  ['operation', operations],
  ['outcome', outcomes],
  ['code', codes],
  ['boundary', new Set(['owner', 'ingress', 'client'])],
  ['peer_service', new Set(['core'])],
  ['route', new Set(['/mcp'])],
  ['method', new Set(['GET', 'POST', 'DELETE', 'UNKNOWN'])],
  [
    'error_class',
    new Set(['dependency', 'transport', 'configuration', 'internal']),
  ],
]);

function safe(name: string, value: unknown): boolean {
  if (name === 'correlation_id')
    return (
      typeof value === 'string' &&
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(
        value,
      )
    );
  if (name === 'duration_ms' || name === 'count') return numeric(value);
  const allowed = fixed.get(name);
  return (
    allowed !== undefined && typeof value === 'string' && allowed.has(value)
  );
}
function numeric(value: unknown): boolean {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0;
}

function validResource(value: Resource): boolean {
  return (
    value['service.name'] === 'mcp' &&
    value['service.namespace'] === 'spoolin' &&
    ['local', 'test'].includes(value['service.env']) &&
    /^[A-Za-z0-9][A-Za-z0-9._+-]{0,127}$/.test(value['service.version']) &&
    Object.keys(value).sort().join(',') ===
      'service.env,service.name,service.namespace,service.version'
  );
}

function safeAttributes(
  event: Event,
  attributes: Readonly<Record<string, unknown>>,
) {
  const values: Record<string, unknown> = { event };
  let dropped = 0;
  for (const [name, value] of Object.entries(attributes)) {
    if (
      Object.keys(values).length < 16 &&
      name !== 'event' &&
      safe(name, value)
    )
      values[name] = value;
    else dropped += 1;
  }
  return { values, dropped };
}
function activeIds() {
  const span = trace.getSpan(context.active());
  const ids = span?.spanContext();
  if (span?.isRecording() && ids && isSpanContextValid(ids))
    return { trace_id: ids.traceId, span_id: ids.spanId };
  return { trace_id: '0'.repeat(32), span_id: '0'.repeat(16) };
}
export function encodeDiagnostic(
  event: Event,
  resource: Resource,
  attributes: Readonly<Record<string, unknown>>,
): string {
  if (!Object.hasOwn(events, event))
    throw new Error('Invalid diagnostic event');
  const selected = events[event];
  if (!validResource(resource)) throw new Error('Invalid diagnostic resource');
  if (!selected) throw new Error('Invalid diagnostic event');
  const { values, dropped } = safeAttributes(event, attributes);
  const record = {
    level: selected[0],
    timestamp: new Date().toISOString(),
    ...activeIds(),
    message: selected[1],
    resource,
    attributes: values,
    ...(dropped ? { dropped_attributes_count: dropped } : {}),
  };
  const encoded = JSON.stringify(record) + '\n';
  if (Buffer.byteLength(encoded) > 16384)
    throw new Error('Diagnostic capacity exceeded');
  return encoded;
}
