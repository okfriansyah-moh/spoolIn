package com.spoolin.core.features.mcpgateway

import com.fasterxml.jackson.databind.ObjectMapper
import com.spoolin.core.features.connector.ConnectorRecord
import io.grpc.Context
import io.grpc.Status
import spoolin.v1.Spoolin
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Approval assertions arrive exclusively through authenticated adapter RPC metadata. */
class UiAssertionVerifier(
    private val enabled: Boolean,
    private val keyId: String?,
    private val secret: ByteArray?,
    private val mapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val consumed = ConcurrentHashMap<String, Long>()

    fun requireTrustedUi(connector: ConnectorRecord, input: Spoolin.ApprovalInput, method: String) {
        val assertion = assertionContext.get() ?: deny()
        if (!enabled || secret?.size != 32 || assertion.length > 4096) deny()
        try {
            verify(assertion, connector, input, method)
        } catch (_: com.fasterxml.jackson.core.JsonProcessingException) {
            deny()
        } catch (_: IllegalArgumentException) {
            deny()
        }
    }

    private fun verify(assertion: String, connector: ConnectorRecord, input: Spoolin.ApprovalInput, method: String) {
        val parts = assertion.split('.')
        if (parts.size != 3 || parts[0] != keyId) deny()
        val decoder = Base64.getUrlDecoder()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(requireNotNull(secret), "HmacSHA256"))
        val expected = mac.doFinal("${parts[0]}.${parts[1]}".toByteArray(Charsets.UTF_8))
        if (!MessageDigest.isEqual(expected, decoder.decode(parts[2]))) deny()
        val claims = mapper.readTree(decoder.decode(parts[1]))
        val now = clock.instant().epochSecond
        val expiry = claims.path("expiry").asLong(0)
        if (expiry <= now || expiry > now + 30) deny()
        requireBinding(claims, connector, input, method)
        val invocation = UUID.fromString(claims.path("invocation").asText()).toString()
        consumed.entries.removeIf { it.value <= now }
        if (consumed.size >= 10000 || consumed.putIfAbsent(invocation, expiry) != null) deny()
    }

    private fun requireBinding(
        claims: com.fasterxml.jackson.databind.JsonNode,
        connector: ConnectorRecord,
        input: Spoolin.ApprovalInput,
        method: String,
    ) {
        if (claims.path("audience").asText() != "core" || claims.path("method").asText() != method) deny()
        if (claims.path("connector").asText() != connector.id.toString()) deny()
        if (claims.path("client").asText() != connector.clientKind) deny()
        if (claims.path("proposal").asText() != input.proposalId || claims.path("revision").asInt() != input.revision) deny()
        if (claims.path("hash").asText() != input.contentHash || claims.path("session").asText().isBlank()) deny()
    }

    private fun deny(): Nothing = throw Status.PERMISSION_DENIED.withDescription("TRUSTED_UI_REQUIRED").asRuntimeException()

    companion object {
        val assertionContext: Context.Key<String> = Context.key("mcp-private-ui-assertion")
    }
}
