package com.spoolin.support.tracing

import io.grpc.Contexts
import io.grpc.ForwardingServerCall
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status

/** Caller supplies verified mTLS peer policy; trace context is never identity or authorization. */
class TraceServerInterceptor(
    private val tracing: OwnerTracing,
    private val trustedPeer: (io.grpc.Attributes) -> Boolean,
) : ServerInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>, headers: Metadata, next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val span = tracing.server(TraceContexts.extract(headers, trustedPeer(call.attributes)))
        val context = io.grpc.Context.current().withValue(TraceContexts.grpcKey, span.context)
        val wrapped = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun sendMessage(message: RespT) = span.scoped { super.sendMessage(message) }
            override fun close(status: Status, trailers: Metadata) {
                try { span.scoped { super.close(status, trailers) } } finally { span.finish(status.isOk) }
            }
        }
        var started = false
        try {
            val delegate = span.scoped { Contexts.interceptCall(context, wrapped, headers, next) }
            started = true
            return TracedServerListener(delegate, span)
        } finally { if (!started) span.finish(false) }
    }
}

private class TracedServerListener<T>(delegate: ServerCall.Listener<T>, private val span: OwnerSpan) :
    ForwardingServerCallListener.SimpleForwardingServerCallListener<T>(delegate) {
    override fun onMessage(message: T) = span.scoped { super.onMessage(message) }
    override fun onHalfClose() = span.scoped { super.onHalfClose() }
    override fun onReady() = span.scoped { super.onReady() }
    override fun onCancel() {
        try { span.scoped { super.onCancel() } } finally { span.finish(false) }
    }
    override fun onComplete() {
        try { span.scoped { super.onComplete() } } finally { span.finish(false) }
    }
}
