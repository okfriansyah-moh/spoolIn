package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorTokenDigest
import io.grpc.Contexts
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
        val hash = ConnectorTokenDigest.sha256(token.toByteArray(Charsets.UTF_8))
        val context = ConnectorRpcContext.context(hash)
        return Contexts.interceptCall(context, call, headers, next)
    }

    private fun bearerToken(value: String?): String? {
        if (value == null) return null
        val trimmed = value.trim()
        if (!trimmed.startsWith("Bearer ", ignoreCase = true)) return null
        val raw = trimmed.substring(7).trim()
        return raw.ifEmpty { null }
    }
}
