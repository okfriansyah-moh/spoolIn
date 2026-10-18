package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorRecord
import io.grpc.Context
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class UiAssertionVerifierTest {
    private val mapper = ObjectMapper()
    private val key = ByteArray(32) { 7 }
    private val now = Instant.ofEpochSecond(1000)
    private val connector =
        ConnectorRecord(UUID.randomUUID(), UUID.randomUUID(), "CLAUDE", 1, "MOVIE", now.plusSeconds(600), null)
    private val hash = Base64.getEncoder().encodeToString(ByteArray(32) { 9 })
    private val input =
        Spoolin.ApprovalInput
            .newBuilder()
            .setProposalId(UUID.randomUUID().toString())
            .setRevision(2)
            .setContentHash(hash)
            .setChallenge("private-challenge")
            .build()
    private val method = "/spoolin.v1.McpGatewayService/ApproveMovie"

    private fun verifier(enabled: Boolean = true) =
        UiAssertionVerifier(enabled, "test-v1", key, mapper, Clock.fixed(now, ZoneOffset.UTC))

    private fun claims(): MutableMap<String, Any> =
        sortedMapOf(
            "audience" to "core",
            "connector" to connector.id.toString(),
            "client" to "CLAUDE",
            "session" to UUID.randomUUID().toString(),
            "proposal" to input.proposalId,
            "revision" to 2,
            "hash" to hash,
            "method" to method,
            "invocation" to UUID.randomUUID().toString(),
            "expiry" to 1030,
        )

    private fun assertion(
        values: Map<String, Any> = claims(),
        id: String = "test-v1",
        secret: ByteArray = key,
    ): String = raw(mapper.writeValueAsString(values.toSortedMap()), id, secret)

    private fun raw(
        json: String,
        id: String = "test-v1",
        secret: ByteArray = key,
    ): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
        val body = "$id.$payload"
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret, "HmacSHA256")) }
        return "$body.${Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(body.toByteArray()))}"
    }

    private fun trusted(
        token: String,
        block: () -> Unit,
    ) {
        Context.current().withValue(UiAssertionVerifier.assertionContext, token).run(block)
    }

    private fun denied(
        token: String,
        verifier: UiAssertionVerifier = verifier(),
        actor: ConnectorRecord = connector,
        request: Spoolin.ApprovalInput = input,
        rpc: String = method,
    ) {
        assertThrows(StatusRuntimeException::class.java) {
            trusted(token) { verifier.requireTrustedUi(actor, request, rpc) }
        }
    }

    @Test
    fun `accepts exact bound invocation once and permits independently signed next invocation`() {
        val verifier = verifier()
        val token = assertion()
        trusted(token) { verifier.requireTrustedUi(connector, input, method) }
        denied(token, verifier)
        trusted(assertion()) { verifier.requireTrustedUi(connector, input, method) }
    }

    @Test
    fun `denies disabled absent expired future mismatched forged and nonallowed method assertions`() {
        assertThrows(StatusRuntimeException::class.java) { verifier().requireTrustedUi(connector, input, method) }
        for ((field, value) in listOf(
            "expiry" to 1000,
            "expiry" to 1031,
            "audience" to "catalog",
            "connector" to UUID.randomUUID().toString(),
            "client" to "CHATGPT",
            "proposal" to UUID.randomUUID().toString(),
            "revision" to 3,
            "hash" to Base64.getEncoder().encodeToString(ByteArray(32)),
            "method" to "/spoolin.v1.McpGatewayService/RejectMovie",
        )) {
            denied(assertion(claims().also { it[field] = value }))
        }
        denied(assertion() + "altered")
        denied(assertion(), verifier(false))
        denied(assertion(), actor = connector.copy(revokedAt = now))
        denied(assertion(), actor = connector.copy(expiresAt = now))
        val invalidMethod = "/spoolin.v1.McpGatewayService/GetMovieContext"
        denied(assertion(claims().also { it["method"] = invalidMethod }), rpc = invalidMethod)
    }

    @Test
    fun `rejects signed JSON coercion duplicates extra fields malformed UUIDs hashes and wire encodings`() {
        for ((field, value) in listOf(
            "expiry" to "1030",
            "expiry" to 1030.0,
            "revision" to "2",
            "revision" to 2.0,
            "revision" to Long.MAX_VALUE,
            "session" to "unbound-session",
            "invocation" to "1-1-1-1-1",
            "session" to UUID.randomUUID().toString().uppercase(),
            "extra" to "claim",
        )) {
            denied(assertion(claims().also { it[field] = value }))
        }
        val json = mapper.writeValueAsString(claims())
        denied(raw(json.dropLast(1) + ",\"revision\":2}"))
        denied(raw(json + "{}"))
        denied(raw(" " + json))
        denied(assertion() + "=")
        val badHash = input.toBuilder().setContentHash("content").build()
        denied(assertion(claims().also { it["hash"] = "content" }), request = badHash)
        denied(assertion(id = "unknown-key"))
        denied(assertion(secret = ByteArray(32)))
    }

    @Test
    fun `new write key and old read key overlap stops at exact absolute sixty second boundary`() {
        var epoch = 1059L
        val clock =
            object : Clock() {
                override fun instant(): Instant = Instant.ofEpochSecond(epoch)

                override fun getZone() = ZoneOffset.UTC

                override fun withZone(zone: java.time.ZoneId): Clock = this
            }
        val fresh = ByteArray(32) { 8 }
        val keys = UiAssertionKeys.rotated("test-v2", fresh, PreviousUiAssertionKey("test-v1", key, 1000), epoch)
        val verifier = UiAssertionVerifier(true, keys, mapper, clock)
        val prior = assertion(claims().also { it["expiry"] = 1089 })
        trusted(prior) { verifier.requireTrustedUi(connector, input, method) }
        epoch = 1060
        denied(assertion(claims().also { it["expiry"] = 1090 }), verifier)
        trusted(assertion(claims().also { it["expiry"] = 1090 }, "test-v2", fresh)) {
            verifier.requireTrustedUi(connector, input, method)
        }
        verifier.close()
        denied(assertion(claims().also { it["expiry"] = 1090 }, "test-v2", fresh), verifier)
    }

    @Test
    fun `concurrent duplicate invocation admits exactly one request`() {
        val verifier = verifier()
        val token = assertion()
        val accepted = AtomicInteger()
        val workers = Executors.newFixedThreadPool(4)
        try {
            val pending =
                (1..12).map {
                    workers.submit {
                        try {
                            trusted(token) { verifier.requireTrustedUi(connector, input, method) }
                            accepted.incrementAndGet()
                        } catch (
                            _: StatusRuntimeException,
                        ) {
                        }
                    }
                }
            pending.forEach { it.get(5, TimeUnit.SECONDS) }
            assertEquals(1, accepted.get())
        } finally {
            workers.shutdownNow()
        }
    }
}
