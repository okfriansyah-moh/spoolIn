package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorRecord
import io.grpc.Context
import io.grpc.Status
import spoolin.v1.Spoolin
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val MAX_ASSERTION_CHARACTERS = 4096
private const val ASSERTION_SEGMENTS = 3
private const val SHA256_BYTES = 32
private const val ASSERTION_LIFETIME_SECONDS = 30L
private const val MAX_REPLAY_ENTRIES = 10000

/** Private adapter assertions only; the authenticated host must bind its live session before signing. */
class UiAssertionVerifier(
    private val enabled: Boolean,
    private val keys: UiAssertionKeys?,
    @Suppress("UNUSED_PARAMETER") mapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC(),
) : AutoCloseable {
    constructor(
        enabled: Boolean,
        keyId: String?,
        secret: ByteArray?,
        mapper: ObjectMapper,
        clock: Clock = Clock.systemUTC(),
    ) : this(
        enabled,
        if (enabled) UiAssertionKeys.single(requireNotNull(keyId), requireNotNull(secret)) else null,
        mapper,
        clock,
    )

    private val strictMapper =
        JsonMapper
            .builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build()
    private val consumed = mutableMapOf<String, Long>()
    private val bindings = UiAssertionClaims(clock)

    init {
        require(!enabled || keys != null) { "Invalid private UI assertion configuration" }
    }

    fun requireTrustedUi(
        connector: ConnectorRecord,
        input: Spoolin.ApprovalInput,
        method: String,
    ) {
        val assertion = assertionContext.get() ?: deny()
        if (!enabled || assertion.length !in 1..MAX_ASSERTION_CHARACTERS) deny()
        try {
            verify(assertion, connector, input, method)
        } catch (
            _: tools.jackson.core.JacksonException,
        ) {
            deny()
        } catch (_: IllegalArgumentException) {
            deny()
        }
    }

    private fun verify(
        assertion: String,
        connector: ConnectorRecord,
        input: Spoolin.ApprovalInput,
        method: String,
    ) {
        val parts = assertion.split('.')
        if (parts.size != ASSERTION_SEGMENTS || !parts[0].matches(Regex("[A-Za-z0-9_-]{1,64}"))) deny()
        val now = clock.instant().epochSecond
        verifySignature(parts, now)
        val bytes = decode(parts[1])
        val claims = strictMapper.readTree(bytes) ?: deny()
        bindings.requireBinding(claims, connector, input, method)
        val expiry = expiry(claims, now)
        val ordered = claims.properties().associate { it.key to it.value }.toSortedMap()
        if (!bytes.contentEquals(strictMapper.writeValueAsBytes(ordered))) deny()
        consume(bindings.text(claims, "invocation"), expiry, now)
    }

    private fun verifySignature(
        parts: List<String>,
        now: Long,
    ) {
        val key = keys?.keyFor(parts[0], now) ?: deny()
        try {
            val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
            val signature = decode(parts[2])
            if (signature.size != SHA256_BYTES ||
                !MessageDigest.isEqual(mac.doFinal("${parts[0]}.${parts[1]}".toByteArray(Charsets.UTF_8)), signature)
            ) {
                deny()
            }
        } finally {
            key.fill(0)
        }
    }

    private fun expiry(
        claims: JsonNode,
        now: Long,
    ): Long {
        val value = claims.path("expiry")
        if (!value.isIntegralNumber || !value.canConvertToLong()) deny()
        val expiry = value.longValue()
        if (expiry <= now || expiry > now + ASSERTION_LIFETIME_SECONDS) deny()
        return expiry
    }

    private fun decode(value: String): ByteArray {
        if (!value.matches(Regex("[A-Za-z0-9_-]+"))) deny()
        return Base64.getUrlDecoder().decode(value).also {
            if (Base64.getUrlEncoder().withoutPadding().encodeToString(it) != value) deny()
        }
    }

    @Synchronized
    private fun consume(
        invocation: String,
        expiry: Long,
        now: Long,
    ) {
        consumed.entries.removeIf { it.value <= now }
        if (consumed.size >= MAX_REPLAY_ENTRIES || consumed.putIfAbsent(invocation, expiry) != null) deny()
    }

    @Synchronized
    override fun close() {
        keys?.close()
        consumed.clear()
    }

    private fun deny(): Nothing {
        val denied = Status.PERMISSION_DENIED.withDescription("TRUSTED_UI_REQUIRED")
        throw denied.asRuntimeException()
    }

    companion object {
        val assertionContext: Context.Key<String> = Context.key("mcp-private-ui-assertion")
    }
}

private class UiAssertionClaims(
    private val clock: Clock,
) {
    fun requireBinding(
        claims: JsonNode,
        connector: ConnectorRecord,
        input: Spoolin.ApprovalInput,
        method: String,
    ) {
        if (!claims.isObject || claims.propertyNames().toSet() != FIELDS) deny()
        if (method !in METHODS || text(claims, "audience") != "core" || text(claims, "method") != method) deny()
        requireIdentifiers(claims)
        requireActor(claims, connector)
        requireInput(claims, input)
    }

    private fun requireIdentifiers(claims: JsonNode) {
        for (name in listOf("connector", "session", "proposal", "invocation")) {
            if (!text(claims, name).matches(UUID_PATTERN)) deny()
        }
    }

    private fun requireActor(
        claims: JsonNode,
        connector: ConnectorRecord,
    ) {
        if (text(claims, "connector") != connector.id.toString() || connector.revokedAt != null ||
            !connector.expiresAt.isAfter(clock.instant())
        ) {
            deny()
        }
        val client = text(claims, "client")
        if (client !in setOf("CLAUDE", "CHATGPT") || client != connector.clientKind) deny()
    }

    private fun requireInput(
        claims: JsonNode,
        input: Spoolin.ApprovalInput,
    ) {
        val revision = claims.path("revision")
        if (!revision.isIntegralNumber || !revision.canConvertToInt()) deny()
        if (revision.intValue() < 1 || revision.intValue() != input.revision) deny()
        if (text(claims, "proposal") != input.proposalId || text(claims, "hash") != input.contentHash) deny()
        requireHash(text(claims, "hash"))
    }

    private fun requireHash(hash: String) {
        val decoded = Base64.getDecoder().decode(hash)
        if (decoded.size != SHA256_BYTES || Base64.getEncoder().encodeToString(decoded) != hash) deny()
    }

    fun text(
        claims: JsonNode,
        name: String,
    ): String {
        val value = claims.path(name)
        if (!value.isString) deny()
        return value.stringValue()
    }

    private fun deny(): Nothing {
        val denied = Status.PERMISSION_DENIED.withDescription("TRUSTED_UI_REQUIRED")
        throw denied.asRuntimeException()
    }

    companion object {
        private val FIELDS =
            setOf(
                "audience",
                "connector",
                "client",
                "session",
                "proposal",
                "revision",
                "hash",
                "method",
                "invocation",
                "expiry",
            )
        private val METHODS =
            setOf(
                "/spoolin.v1.McpGatewayService/ApproveMovie",
                "/spoolin.v1.McpGatewayService/RejectMovie",
            )
        private val UUID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }
}
