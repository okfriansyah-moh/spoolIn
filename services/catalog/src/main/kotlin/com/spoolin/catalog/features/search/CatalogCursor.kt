@file:Suppress("LongParameterList", "CyclomaticComplexMethod")

package com.spoolin.catalog.features.search

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HMAC_ALGO = "HmacSHA256"
private const val HMAC_LEN = 32
private const val KEY_BYTES = 32
private const val MAX_CURSOR_BYTES = 1024
private const val CURSOR_TTL_SECONDS = 900L // 15 minutes

/**
 * Signed, query-scoped pagination cursor for public title / person searches.
 *
 * Format: Base64Url(payload_bytes || hmac_bytes)
 * payload_bytes = varint-length-prefixed UTF-8 JSON:
 *   {"y":<year>,"m":<month>,"d":<day>,"u":"<uuid>","s":"<scopeHex>","e":<epochSeconds>}
 * hmac_bytes = HMAC-SHA256(payload_bytes, key) — 32 bytes
 *
 * The key is injected; tests use a fixed key for determinism.
 * Scope hash = SHA-256 of the typed query parameters.
 */
class CatalogCursor(
    private val key: ByteArray = newRandomKey(),
) {
    data class Keyset(
        val releaseYear: Int?,
        val releaseMonth: Int?,
        val releaseDay: Int?,
        val id: UUID,
    )

    /**
     * Encode a cursor from the last-page keyset plus a scope hash and current clock instant.
     */
    fun encode(
        keyset: Keyset,
        scopeHash: String,
        now: Instant,
    ): String {
        val expiresAt = now.plusSeconds(CURSOR_TTL_SECONDS).epochSecond
        val payload = buildPayload(keyset, scopeHash, expiresAt)
        val mac = mac(payload)
        val bytes = payload + mac
        require(bytes.size <= MAX_CURSOR_BYTES) { "Cursor exceeds 1024 bytes" }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Decode and verify a cursor. Returns null if the cursor is invalid, expired, or
     * does not match the supplied scope hash. Never throws for bad input.
     *
     * The return value is the keyset to use as the SQL continuation point.
     */
    fun decode(
        encoded: String,
        scopeHash: String,
        now: Instant,
    ): Keyset? =
        runCatching {
            val bytes = Base64.getUrlDecoder().decode(encoded)
            if (bytes.size < HMAC_LEN + 1) return@runCatching null
            val payload = bytes.copyOfRange(0, bytes.size - HMAC_LEN)
            val expected = mac(payload)
            val actual = bytes.copyOfRange(bytes.size - HMAC_LEN, bytes.size)
            if (!constantTimeEquals(expected, actual)) return@runCatching null
            val json = String(payload, Charsets.UTF_8)
            val values = parseJson(json) ?: return@runCatching null
            val expiresAt = values["e"] as? Long ?: return@runCatching null
            if (now.epochSecond >= expiresAt) return@runCatching null
            val scope = values["s"] as? String ?: return@runCatching null
            if (scope != scopeHash) return@runCatching null
            val u = values["u"] as? String ?: return@runCatching null
            val id = UUID.fromString(u)
            if (id.toString() != u) return@runCatching null
            Keyset(
                releaseYear = (values["y"] as? Long)?.toInt(),
                releaseMonth = (values["m"] as? Long)?.toInt(),
                releaseDay = (values["d"] as? Long)?.toInt(),
                id = id,
            )
        }.getOrNull()

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun buildPayload(
        keyset: Keyset,
        scopeHash: String,
        expiresAt: Long,
    ): ByteArray {
        val sb = StringBuilder("{")
        keyset.releaseYear?.let { sb.append("\"y\":$it,") }
        keyset.releaseMonth?.let { sb.append("\"m\":$it,") }
        keyset.releaseDay?.let { sb.append("\"d\":$it,") }
        sb.append("\"u\":\"${keyset.id}\",")
        sb.append("\"s\":\"$scopeHash\",")
        sb.append("\"e\":$expiresAt}")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun mac(payload: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(key, HMAC_ALGO))
        return mac.doFinal(payload)
    }

    /** Minimal JSON deserializer sufficient for cursor payloads (no external deps). */
    private fun parseJson(json: String): Map<String, Any>? =
        runCatching {
            val cleaned = json.trim().removePrefix("{").removeSuffix("}")
            val result = mutableMapOf<String, Any>()
            val entries = splitEntries(cleaned)
            for (entry in entries) {
                val colon = entry.indexOf(':')
                if (colon < 0) return@runCatching null
                val rawKey = entry.substring(0, colon).trim().trim('"')
                val rawValue = entry.substring(colon + 1).trim()
                val value: Any =
                    when {
                        rawValue.startsWith("\"") -> rawValue.trim('"')
                        rawValue.toLongOrNull() != null -> rawValue.toLong()
                        else -> return@runCatching null
                    }
                result[rawKey] = value
            }
            result
        }.getOrNull()

    private fun splitEntries(s: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var inString = false
        var start = 0
        for (i in s.indices) {
            when {
                s[i] == '"' && (i == 0 || s[i - 1] != '\\') -> inString = !inString
                !inString && s[i] == '{' -> depth++
                !inString && s[i] == '}' -> depth--
                !inString && depth == 0 && s[i] == ',' -> {
                    parts.add(s.substring(start, i))
                    start = i + 1
                }
            }
        }
        if (start < s.length) parts.add(s.substring(start))
        return parts
    }

    companion object {
        private val random = SecureRandom()

        fun newRandomKey(): ByteArray = ByteArray(KEY_BYTES).also { random.nextBytes(it) }

        /** Constant-time comparison to prevent timing attacks. */
        fun constantTimeEquals(
            a: ByteArray,
            b: ByteArray,
        ): Boolean {
            if (a.size != b.size) return false
            var diff = 0
            for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
            return diff == 0
        }

        /**
         * Compute a scope hash from the search parameters.
         * Order-stable, deterministic hex digest used in cursor binding.
         */
        fun scopeHash(
            text: String?,
            kind: String?,
            genreIds: List<String>,
            year: Int?,
            month: Int?,
            visibility: String,
        ): String {
            val serialized = com.spoolin.support.policy.PolicyJson.encode(
                listOf("catalog-scope-v2", visibility, text, kind, genreIds.distinct().sorted(), year, month),
            )
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(serialized.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        /**
         * Escape user text for pg_unicode_fast LIKE patterns.
         * Escapes \, %, _ so they are treated as literals.
         */
        fun escapeWildcards(text: String): String = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}

/** Shared signing material; scope requires the freshly admitted managed reader on every request. */
class ManagedReadCursors(
    key: ByteArray,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
) {
    val titles = CatalogCursor(key)
    val people = PeopleCursor(key)
    fun now(): Instant = clock.instant()

    fun scope(reads: com.spoolin.support.policy.ReadPolicy, surface: String, first: Int,
              criteria: Map<String, Any?>): String {
        val actor = com.spoolin.support.identity.ActorRpc.current()
            ?: throw io.grpc.Status.PERMISSION_DENIED.asRuntimeException()
        return reads.cursorScope(surface, first, mapOf(
            "criteria" to criteria,
            "reader" to listOf(actor.accountId.toString(), actor.credentialId.toString(), actor.authEpoch,
                actor.role, actor.clientKind, actor.audience),
        ))
    }

    fun title(raw: String?, scope: String): CatalogCursor.Keyset? = raw?.let {
        titles.decode(it, scope, now()) ?: invalid()
    }

    fun person(raw: String?, scope: String): PeopleCursor.Keyset? = raw?.let {
        people.decode(it, scope, now()) ?: invalid()
    }

    fun audit(raw: String?, scope: String): com.spoolin.catalog.operations.audit.AuditReads.Keyset? =
        person(raw, scope)?.let { key ->
            val at = try { Instant.parse(key.name) } catch (_: java.time.format.DateTimeParseException) { invalid() }
            com.spoolin.catalog.operations.audit.AuditReads.Keyset(at, key.id)
        }

    private fun invalid(): Nothing = throw io.grpc.Status.INVALID_ARGUMENT
        .withDescription("INVALID_CURSOR").asRuntimeException()

    companion object {
        private const val SIGNING_KEY_BYTES = 32
        fun ephemeral(): ManagedReadCursors = ManagedReadCursors(
            ByteArray(SIGNING_KEY_BYTES).also { SecureRandom().nextBytes(it) },
        )
    }
}
