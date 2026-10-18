import { context, SpanKind, SpanStatusCode, trace } from '@opentelemetry/api';
import {
  InMemorySpanExporter,
  SimpleSpanProcessor,
} from '@opentelemetry/sdk-trace-node';
import http from 'node:http';
import { encodeDiagnostic } from '../../src/operations/diagnostic-encoder.js';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import {
  activeCancellation,
  initializeTracing,
  McpTracing,
  traceparent,
} from '../../src/operations/tracing.js';

// Real SDK and owned loopback HTTP; collector delivery and supported chat hosts remain separate obligations.
describe('MCP SDK tracing boundaries', () => {
  const exporter = new InMemorySpanExporter();
  const provider = initializeTracing([new SimpleSpanProcessor(exporter)]);
  const tracing = new McpTracing(provider.getTracer('mcp-boundary-test'));
  let server: http.Server;
  let origin: string;
  let cancelled = false;
  let cancellationReady = () => {};
  beforeAll(async () => {
    server = http.createServer((req, res) => {
      void tracing
        .ingress(req, res, async () => {
          const ingress = trace.getSpan(context.active())!.spanContext();
          if (req.url === '/cancel') {
            await tracing.gateway(
              'GetPolicy',
              () =>
                new Promise<void>((_resolve, reject) => {
                  activeCancellation()!.addEventListener(
                    'abort',
                    () => {
                      cancelled = true;
                      reject(new Error('synthetic cancellation'));
                    },
                    { once: true },
                  );
                  cancellationReady();
                }),
            );
            return;
          }
          const child = await tracing.gateway('GetPolicy', async (span) => {
            await new Promise((resolve) => setTimeout(resolve, 5));
            expect(trace.getSpan(context.active())?.spanContext().spanId).toBe(
              span.spanContext().spanId,
            );
            return { id: span.spanContext().spanId, parent: traceparent(span) };
          });
          expect(trace.getSpan(context.active())?.spanContext().spanId).toBe(
            ingress.spanId,
          );
          const diagnostic = JSON.parse(
            encodeDiagnostic(
              'request_completed',
              {
                'service.name': 'mcp',
                'service.namespace': 'spoolin',
                'service.version': 'test',
                'service.env': 'test',
              },
              {},
            ),
          );
          res.end(
            JSON.stringify({
              trace: ingress.traceId,
              root: ingress.spanId,
              child,
              diagnostic,
            }),
          );
        })
        .catch(() => res.destroy());
    });
    await new Promise<void>((resolve) =>
      server.listen(0, '127.0.0.1', resolve),
    );
    const address = server.address();
    if (!address || typeof address === 'string')
      throw new Error('Owned listener unavailable');
    origin = `http://127.0.0.1:${address.port}`;
  });
  afterAll(async () => {
    server.closeAllConnections();
    await new Promise<void>((resolve) => server.close(() => resolve()));
    await provider.shutdown();
  });
  it('creates independent roots and real children across concurrent requests without trusting incoming headers', async () => {
    const forged = '00-' + '1'.repeat(32) + '-' + '2'.repeat(16) + '-01';
    const requests = await Promise.all(
      [1, 2].map(async () => {
        const response = await fetch(origin, {
          headers: {
            traceparent: forged,
            tracestate: 'secret=value',
            baggage: 'private=value',
          },
        });
        return response.json() as Promise<{
          trace: string;
          root: string;
          child: { id: string; parent: string };
          diagnostic: { trace_id: string; span_id: string };
        }>;
      }),
    );
    await provider.forceFlush();
    expect(new Set(requests.map((value) => value.trace)).size).toBe(2);
    for (const result of requests) {
      expect(result.trace).toMatch(/^[0-9a-f]{32}$/);
      expect(result.trace).not.toBe('1'.repeat(32));
      expect(result.diagnostic.trace_id).toBe(result.trace);
      expect(result.diagnostic.span_id).toBe(result.root);
      const spans = exporter
        .getFinishedSpans()
        .filter((span) => span.spanContext().traceId === result.trace);
      const root = spans.find((span) => span.kind === SpanKind.SERVER)!;
      const child = spans.find((span) => span.kind === SpanKind.CLIENT)!;
      expect(root.parentSpanContext).toBeUndefined();
      expect(child.parentSpanContext?.spanId).toBe(result.root);
      expect(child.spanContext().spanId).toBe(result.child.id);
      expect(result.child.parent).toBe(
        `00-${result.trace}-${result.child.id}-01`,
      );
      expect(
        JSON.stringify(spans.map((span) => span.attributes)),
      ).not.toContain('private');
    }
    expect(trace.getSpan(context.active())).toBeUndefined();
  });
  it('ends cancelled root and child spans and clears async context', async () => {
    const ready = new Promise<void>((resolve) => {
      cancellationReady = resolve;
    });
    const request = http.get(`${origin}/cancel`);
    request.on('error', () => undefined);
    await ready;
    request.destroy();
    for (let attempt = 0; attempt < 20 && !cancelled; attempt += 1)
      await new Promise((resolve) => setTimeout(resolve, 5));
    expect(cancelled).toBe(true);
    await provider.forceFlush();
    expect(
      exporter
        .getFinishedSpans()
        .filter((span) => span.status.code === SpanStatusCode.ERROR).length,
    ).toBeGreaterThanOrEqual(2);
    expect(trace.getSpan(context.active())).toBeUndefined();
  });
});
