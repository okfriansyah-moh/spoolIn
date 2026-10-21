import {
  context,
  createContextKey,
  ROOT_CONTEXT,
  SpanKind,
  SpanStatusCode,
  trace,
} from '@opentelemetry/api';
import type { Context, Span, Tracer } from '@opentelemetry/api';
import {
  AlwaysOnSampler,
  NodeTracerProvider,
} from '@opentelemetry/sdk-trace-node';
import type { SpanProcessor } from '@opentelemetry/sdk-trace-node';
import type http from 'node:http';
import { mcpDiagnostics } from './diagnostics.js';

let provider: NodeTracerProvider | undefined;
const cancellation = createContextKey('mcp.ingress.cancellation');
export function initializeTracing(
  processors: SpanProcessor[] = [],
): NodeTracerProvider {
  if (!provider) {
    provider = new NodeTracerProvider({
      sampler: new AlwaysOnSampler(),
      spanProcessors: processors,
    });
    provider.register({ propagator: null });
  }
  return provider;
}
export async function shutdownTracing(): Promise<void> {
  await provider?.shutdown();
}
export function activeCancellation(): AbortSignal | undefined {
  const value = context.active().getValue(cancellation);
  return value instanceof AbortSignal ? value : undefined;
}
export function traceparent(span: Span): string {
  const value = span.spanContext();
  return `00-${value.traceId}-${value.spanId}-${value.traceFlags.toString(16).padStart(2, '0')}`;
}
export type GatewayOperation =
  | 'ListMovieProposals'
  | 'GetReviewIdentity'
  | 'GetPolicy'
  | 'PrepareMoviePoster'
  | 'ReadMoviePoster'
  | 'GetMovieContext'
  | 'ProposeMovie'
  | 'GetMovieOperation'
  | 'ApproveMovie'
  | 'RejectMovie';

function ingressLifecycle(
  span: Span,
  req: http.IncomingMessage,
  res: http.ServerResponse,
): { scope: Context; fail: () => void } {
  const controller = new AbortController();
  const scope = trace
    .setSpan(ROOT_CONTEXT, span)
    .setValue(cancellation, controller.signal);
  let ended = false;
  const finish = (failed = false) => {
    if (ended) return;
    ended = true;
    req.off('aborted', aborted);
    res.off('finish', completed);
    res.off('close', aborted);
    context.with(scope, () => {
      span.setStatus({
        code: failed ? SpanStatusCode.ERROR : SpanStatusCode.OK,
      });
      queueMicrotask(() => span.end());
    });
    controller.abort();
  };
  const completed = () => finish(res.statusCode >= 500);
  const aborted = () => finish(!res.writableFinished);
  req.once('aborted', aborted);
  res.once('finish', completed);
  res.once('close', aborted);
  return { scope, fail: () => finish(true) };
}

export class McpTracing {
  constructor(private readonly selected?: Tracer) {}
  private tracer(): Tracer {
    return this.selected ?? initializeTracing().getTracer('spoolin.mcp');
  }
  ingress(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    handle: () => Promise<void>,
  ): Promise<void> {
    // The connector/model cannot choose trusted trace context; no inbound headers are extracted.
    const span = this.tracer().startSpan(
      'mcp.connector',
      { kind: SpanKind.SERVER },
      ROOT_CONTEXT,
    );
    const lifecycle = ingressLifecycle(span, req, res);
    return context.with(lifecycle.scope, async () => {
      try {
        await handle();
      } catch (failure) {
        mcpDiagnostics.emit('request_failed', { code: 'UNAVAILABLE' });
        lifecycle.fail();
        throw failure;
      }
    });
  }
  stdio<T>(action: () => Promise<T>): Promise<T> {
    const span = this.tracer().startSpan(
      'mcp.stdio',
      { kind: SpanKind.INTERNAL },
      ROOT_CONTEXT,
    );
    return context.with(trace.setSpan(ROOT_CONTEXT, span), async () => {
      try {
        return await action();
      } catch (failure) {
        span.setStatus({ code: SpanStatusCode.ERROR });
        mcpDiagnostics.emit('transport_failed', { error_class: 'transport' });
        throw failure;
      } finally {
        span.end();
      }
    });
  }
  gateway<T>(
    operation: GatewayOperation,
    action: (span: Span) => Promise<T>,
  ): Promise<T> {
    const span = this.tracer().startSpan(`mcp.gateway.${operation}`, {
      kind: SpanKind.CLIENT,
      attributes: { 'peer.service': 'core', 'rpc.method': operation },
    });
    return context.with(trace.setSpan(context.active(), span), async () => {
      mcpDiagnostics.emit('branch_debug', {
        boundary: 'client',
        peer_service: 'core',
      });
      try {
        const result = await action(span);
        span.setStatus({ code: SpanStatusCode.OK });
        return result;
      } catch (failure) {
        span.setStatus({ code: SpanStatusCode.ERROR });
        throw failure;
      } finally {
        span.end();
      }
    });
  }
}
export const mcpTracing = new McpTracing();
