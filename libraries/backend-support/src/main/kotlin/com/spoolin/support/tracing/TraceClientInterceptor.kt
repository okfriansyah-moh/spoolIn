package com.spoolin.support.tracing

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status

/** Registered only on configured private mTLS channels; metadata never includes baggage or tracestate. */
class TraceClientInterceptor(private val tracing: OwnerTracing) : ClientInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        method: MethodDescriptor<ReqT, RespT>, options: CallOptions, channel: Channel,
    ): ClientCall<ReqT, RespT> {
        val span = tracing.client()
        var created = false
        try {
            val delegate = span.scoped { channel.newCall(method, options) }
            created = true
            return TracedClientCall(delegate, span)
        } finally { if (!created) span.finish(false) }
    }
}

private class TracedClientCall<Q, S>(delegate: ClientCall<Q, S>, private val span: OwnerSpan) :
    ForwardingClientCall.SimpleForwardingClientCall<Q, S>(delegate) {
    override fun start(listener: Listener<S>, headers: Metadata) {
        TraceContexts.inject(span.context, headers)
        val traced = TracedClientListener(listener, span)
        var started = false
        try { span.scoped { super.start(traced, headers) }; started = true }
        finally { if (!started) span.finish(false) }
    }
    override fun sendMessage(message: Q) = span.scoped { super.sendMessage(message) }
    override fun halfClose() = span.scoped { super.halfClose() }
    override fun request(messages: Int) = span.scoped { super.request(messages) }
    override fun cancel(message: String?, cause: Throwable?) {
        try { span.scoped { super.cancel(message, cause) } } finally { span.finish(false) }
    }
}

private class TracedClientListener<S>(delegate: ClientCall.Listener<S>, private val span: OwnerSpan) :
    ForwardingClientCallListener.SimpleForwardingClientCallListener<S>(delegate) {
    override fun onHeaders(headers: Metadata) = span.scoped { super.onHeaders(headers) }
    override fun onMessage(message: S) = span.scoped { super.onMessage(message) }
    override fun onReady() = span.scoped { super.onReady() }
    override fun onClose(status: Status, trailers: Metadata) {
        try { span.scoped { super.onClose(status, trailers) } } finally { span.finish(status.isOk) }
    }
}
