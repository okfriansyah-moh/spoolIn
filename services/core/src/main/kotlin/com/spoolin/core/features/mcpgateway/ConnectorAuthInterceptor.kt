package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorTokenDigest
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status

private const val BEARER_PREFIX = "Bearer "

private val AUTHORIZATION = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

class ConnectorAuthInterceptor : ServerInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        if (call.methodDescriptor.serviceName?.endsWith("McpGatewayService") != true) {
            return next.startCall(call, headers)
        }
        return authenticated(call, headers, next)
    }

    private fun <ReqT : Any, RespT : Any> authenticated(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val token = bearerToken(headers.get(AUTHORIZATION))
        return if (token == null) {
            call.close(Status.UNAUTHENTICATED.withDescription("CONNECTOR_TOKEN_REQUIRED"), Metadata())
            object : ServerCall.Listener<ReqT>() {}
        } else {
            val hash = ConnectorTokenDigest.sha256(token.toByteArray(Charsets.UTF_8))
            val context =
                ConnectorRpcContext.context(hash).withValue(
                    UiAssertionVerifier.assertionContext,
                    headers.get(Metadata.Key.of("mcp-ui-assertion", Metadata.ASCII_STRING_MARSHALLER)),
                )
            Contexts.interceptCall(context, call, headers, next)
        }
    }

    private fun bearerToken(value: String?): String? =
        value
            ?.trim()
            ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
            ?.ifEmpty { null }
}
