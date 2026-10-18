package com.spoolin.support.tracing

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import java.util.concurrent.atomic.AtomicBoolean

/** Local SDK context only: no collector, exporter, request payload attributes or global SDK mutation. */
class OwnerTracing private constructor(
    private val provider: SdkTracerProvider,
) : AutoCloseable {
    constructor(service: String) : this(provider(service))

    private val tracer = provider.get("com.spoolin.owner")

    fun browser(): OwnerSpan = span("browser.ingress", SpanKind.SERVER, Context.root())

    fun recovery(): OwnerSpan = span("work.recovery", SpanKind.INTERNAL, Context.root())

    internal fun server(parent: Context): OwnerSpan = span("grpc.server", SpanKind.SERVER, parent)

    internal fun client(): OwnerSpan = span("grpc.client", SpanKind.CLIENT, TraceContexts.current())

    private fun span(
        name: String,
        kind: SpanKind,
        parent: Context,
    ): OwnerSpan {
        val span =
            tracer
                .spanBuilder(name)
                .setSpanKind(kind)
                .setParent(parent)
                .startSpan()
        return OwnerSpan(parent.with(span), span)
    }

    override fun close() {
        provider.close()
    }

    companion object {
        internal fun testing(provider: SdkTracerProvider): OwnerTracing = OwnerTracing(provider)

        private fun provider(service: String): SdkTracerProvider {
            require(service in setOf("core", "catalog", "media"))
            val resource = Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), service))
            return SdkTracerProvider.builder().setResource(resource).build()
        }
    }
}

class OwnerSpan internal constructor(
    internal val context: Context,
    private val span: Span,
) {
    private val ended = AtomicBoolean()

    fun <T> scoped(action: () -> T): T =
        context.makeCurrent().use {
            var returned = false
            try {
                action().also { returned = true }
            } finally {
                if (!returned) finish(false)
            }
        }

    fun wrap(action: Runnable): Runnable = Runnable { scoped { action.run() } }

    fun finish(success: Boolean) {
        if (ended.compareAndSet(false, true)) {
            if (!success) span.setStatus(StatusCode.ERROR)
            span.end()
        }
    }
}
