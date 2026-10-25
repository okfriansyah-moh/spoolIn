package com.spoolin.core.coveragehttp

import com.google.protobuf.Empty
import com.spoolin.core.features.connector.ConnectorTokenDigest
import com.spoolin.core.features.mcpgateway.ConnectorAuthInterceptor
import com.spoolin.core.features.mcpgateway.ConnectorRpcContext
import com.spoolin.core.features.mcpgateway.UiAssertionVerifier
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.McpGatewayServiceGrpc
import spoolin.v1.Spoolin

class ConnectorInterceptorRulesTest {
    @Test
    fun `gateway interceptor rejects missing unsupported and blank bearer credentials`() {
        for (authorization in listOf(null, "", "Basic token", "Bearer", "Bearer ", "Bearer   ")) {
            val call = GatewayCall(McpGatewayServiceGrpc.getGetPolicyMethod())
            val headers = headers(authorization)
            ConnectorAuthInterceptor().interceptCall(call, headers, ServerCallHandler { _, _ -> error("Unauthenticated gateway dispatched") })
            assertEquals(Status.Code.UNAUTHENTICATED, call.failure!!.code)
        }
    }

    @Test
    fun `gateway context carries only digest and private assertion and is detached after dispatch`() {
        for (authorization in listOf("Bearer token", "  bEaReR  token  ")) {
            val call = GatewayCall(McpGatewayServiceGrpc.getGetPolicyMethod())
            val headers = headers(authorization)
            headers.put(Metadata.Key.of("mcp-ui-assertion", Metadata.ASCII_STRING_MARSHALLER), "private-assertion")
            var dispatched = false
            ConnectorAuthInterceptor().interceptCall(call, headers, ServerCallHandler { _, _ ->
                dispatched = true
                assertArrayEquals(ConnectorTokenDigest.sha256("token".toByteArray()), ConnectorRpcContext.current())
                assertEquals("private-assertion", UiAssertionVerifier.assertionContext.get())
                object : ServerCall.Listener<Empty>() {}
            })
            assertTrue(dispatched)
            assertNull(call.failure)
            assertNull(ConnectorRpcContext.current())
            assertNull(UiAssertionVerifier.assertionContext.get())
        }
    }

    @Test
    fun `non gateway RPC passes without connector authentication context`() {
        val descriptor = McpGatewayServiceGrpc.getGetPolicyMethod().toBuilder().setFullMethodName("spoolin.OtherService/GetPolicy").build()
        val call = GatewayCall(descriptor)
        var dispatched = false
        ConnectorAuthInterceptor().interceptCall(call, Metadata(), ServerCallHandler { _, _ ->
            dispatched = true
            assertNull(ConnectorRpcContext.current())
            object : ServerCall.Listener<Empty>() {}
        })
        assertTrue(dispatched)
        assertNull(call.failure)
    }

    private fun headers(authorization: String?) = Metadata().also { if (authorization != null) it.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), authorization) }
    private class GatewayCall(private val descriptor: MethodDescriptor<Empty, Spoolin.PolicySnapshot>) : ServerCall<Empty, Spoolin.PolicySnapshot>() {
        var failure: Status? = null
        override fun getMethodDescriptor() = descriptor
        override fun request(count: Int) = Unit
        override fun sendHeaders(headers: Metadata) = Unit
        override fun sendMessage(message: Spoolin.PolicySnapshot) = Unit
        override fun close(status: Status, trailers: Metadata) { failure = status }
        override fun isCancelled() = false
    }
}
