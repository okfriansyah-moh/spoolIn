package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorTokenDigest
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status

private val AUTHORIZATION = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

class ConnectorAuthInterceptor : ServerInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        if (!call.methodDescriptor.serviceName.endsWith("McpGatewayService")) {
            return next.startCall(call, headers)
        }
        val token = bearerToken(headers.get(AUTHORIZATION))
        if (token == null) {
            call.close(Status.UNAUTHENTICATED.withDescription("CONNECTOR_TOKEN_REQUIRED"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }
        ConnectorRpcContext.tokenHash.set(ConnectorTokenDigest.sha256(token.toByteArray(Charsets.UTF_8)))
        return object : ForwardingListener<ReqT>(next.startCall(call, headers)) {
            override fun onCancel() {
                ConnectorRpcContext.tokenHash.remove()
                super.onCancel()
            }

            override fun onComplete() {
                ConnectorRpcContext.tokenHash.remove()
                super.onComplete()
            }
        }
    }

    private fun bearerToken(value: String?): String? {
        if (value == null) return null
        val trimmed = value.trim()
        if (!trimmed.startsWith("Bearer ", ignoreCase = true)) return null
        val raw = trimmed.substring(7).trim()
        return raw.ifEmpty { null }
    }

    private open class ForwardingListener<ReqT>(
        private val delegate: ServerCall.Listener<ReqT>,
    ) : ServerCall.Listener<ReqT>() {
        override fun onMessage(message: ReqT) = delegate.onMessage(message)

        override fun onHalfClose() = delegate.onHalfClose()

        override fun onCancel() = delegate.onCancel()

        override fun onComplete() = delegate.onComplete()

        override fun onReady() = delegate.onReady()
    }
}
