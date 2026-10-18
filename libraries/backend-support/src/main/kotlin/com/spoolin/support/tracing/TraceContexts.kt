package com.spoolin.support.tracing

import io.grpc.Metadata
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter

/** Only validated traceparent enters transport; tracestate, baggage and arbitrary MDC are excluded. */
object TraceContexts {
    internal val grpcKey: io.grpc.Context.Key<Context> = io.grpc.Context.key("spoolin-otel-context")
    private val traceparent = Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER)
    private val tracestate = Metadata.Key.of("tracestate", Metadata.ASCII_STRING_MARSHALLER)
    private val baggage = Metadata.Key.of("baggage", Metadata.ASCII_STRING_MARSHALLER)
    private const val MIN_PARENT_BYTES = 55
    private const val MAX_PARENT_BYTES = 512
    private val shape = Regex("[0-9a-f-]+")
    private val propagator = W3CTraceContextPropagator.getInstance()

    internal fun current(): Context {
        val local = Context.current()
        return if (Span.fromContext(local).spanContext.isValid) local else grpcKey.get() ?: Context.root()
    }

    fun wrap(action: Runnable): Runnable = current().wrap(action)

    fun identifiers(): Pair<String, String> {
        val span = Span.fromContext(current()).spanContext
        return span.traceId to span.spanId
    }

    internal fun extract(headers: Metadata, trusted: Boolean): Context {
        if (!trusted) return Context.root()
        val values = headers.getAll(traceparent)?.iterator() ?: return Context.root()
        if (!values.hasNext()) return Context.root()
        val value = values.next()
        if (values.hasNext() || value.length !in MIN_PARENT_BYTES..MAX_PARENT_BYTES || !shape.matches(value)) {
            return Context.root()
        }
        val getter = object : TextMapGetter<String> {
            override fun keys(carrier: String): Iterable<String> = listOf("traceparent")
            override fun get(carrier: String?, key: String): String? = if (key == "traceparent") carrier else null
        }
        return propagator.extract(Context.root(), value, getter)
    }

    internal fun inject(context: Context, headers: Metadata) {
        headers.discardAll(traceparent)
        headers.discardAll(tracestate)
        headers.discardAll(baggage)
        val setter = TextMapSetter<Metadata> { carrier, key, value ->
            if (key == "traceparent") requireNotNull(carrier).put(traceparent, value)
        }
        propagator.inject(context, headers, setter)
    }
}
