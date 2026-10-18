package com.spoolin.core.features.mcpgateway

import tools.jackson.databind.ObjectMapper
import com.spoolin.core.features.connector.ConnectorRecord
import io.grpc.Context
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class UiAssertionVerifierTest {
    private val mapper = ObjectMapper()
    private val key = ByteArray(32) { 7 }
    private val now = Instant.ofEpochSecond(1000)
    private val connector = ConnectorRecord(UUID.randomUUID(), UUID.randomUUID(), "CLAUDE", 1, "MOVIE", now.plusSeconds(600), null)
    private val input = Spoolin.ApprovalInput.newBuilder().setProposalId(UUID.randomUUID().toString())
        .setRevision(2).setContentHash("content").setChallenge("private-challenge").build()
    private val method = "/spoolin.v1.McpGatewayService/ApproveMovie"
    private fun verifier(enabled: Boolean = true) = UiAssertionVerifier(enabled, "test-v1", key, mapper, Clock.fixed(now, ZoneOffset.UTC))

    private fun assertion(expiry: Long = 1030, proposal: String = input.proposalId, rpc: String = method): String {
        val claims = sortedMapOf<String, Any>("audience" to "core", "connector" to connector.id.toString(),
            "client" to "CLAUDE", "session" to "private-session", "proposal" to proposal,
            "revision" to 2, "hash" to "content", "method" to rpc, "invocation" to UUID.randomUUID().toString(), "expiry" to expiry)
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(claims))
        val body = "test-v1.$payload"
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
        return "$body.${Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(body.toByteArray()))}"
    }
    private fun trusted(token: String, block: () -> Unit) {
        Context.current().withValue(UiAssertionVerifier.assertionContext, token).run(block)
    }

    @Test
    fun `accepts exact bound invocation once`() {
        val verifier = verifier()
        val token = assertion()
        trusted(token) { verifier.requireTrustedUi(connector, input, method) }
        assertThrows(StatusRuntimeException::class.java) { trusted(token) { verifier.requireTrustedUi(connector, input, method) } }
    }

    @Test
    fun `denies disabled missing expired mismatched and forged assertions`() {
        assertThrows(StatusRuntimeException::class.java) { verifier().requireTrustedUi(connector, input, method) }
        val tokens = listOf(assertion(1000), assertion(1031), assertion(proposal = UUID.randomUUID().toString()),
            assertion(rpc = "/spoolin.v1.McpGatewayService/RejectMovie"), assertion() + "altered")
        tokens.forEach { token ->
            assertThrows(StatusRuntimeException::class.java) { trusted(token) { verifier().requireTrustedUi(connector, input, method) } }
        }
        assertThrows(StatusRuntimeException::class.java) { trusted(assertion()) { verifier(false).requireTrustedUi(connector, input, method) } }
    }
}
