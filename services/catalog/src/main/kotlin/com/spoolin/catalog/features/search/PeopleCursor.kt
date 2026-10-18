@file:Suppress("CyclomaticComplexMethod")

package com.spoolin.catalog.features.search

import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HMAC_ALGO = "HmacSHA256"
private const val HMAC_LEN = 32
private const val MAX_CURSOR_BYTES = 1024
private const val CURSOR_TTL_SECONDS = 900L // 15 minutes

/**
 * Signed, query-scoped pagination cursor for public person searches.
 *
 * Format: Base64Url(payload_bytes || hmac_bytes)
 * Payload: {"n":"<name>","u":"<uuid>","s":"<scopeHex>","e":<epochSeconds>}
 * Shares the signing key with TitleCursor for consistent security posture.
 */
class PeopleCursor(
    private val key: ByteArray,
) {
    data class Keyset(
        val name: String,
        val id: UUID,
    )

    fun encode(
        keyset: Keyset,
        scopeHash: String,
        now: Instant,
    ): String {
        val expiresAt = now.plusSeconds(CURSOR_TTL_SECONDS).epochSecond
        val payload =
            mapper.writeValueAsBytes(
                mapOf("n" to keyset.name, "u" to keyset.id.toString(), "s" to scopeHash, "e" to expiresAt),
            )
        val mac = mac(payload)
        val bytes = payload + mac
        require(bytes.size <= MAX_CURSOR_BYTES) { "People cursor exceeds 1024 bytes" }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun decode(
        encoded: String,
        scopeHash: String,
        now: Instant,
    ): Keyset? =
        runCatching {
            require(encoded.length <= MAX_ENCODED_BYTES)
            val bytes = Base64.getUrlDecoder().decode(encoded)
            require(bytes.size <= MAX_CURSOR_BYTES)
            if (bytes.size < HMAC_LEN + 1) return@runCatching null
            val payload = bytes.copyOfRange(0, bytes.size - HMAC_LEN)
            val expected = mac(payload)
            val actual = bytes.copyOfRange(bytes.size - HMAC_LEN, bytes.size)
            if (!constantTimeEquals(expected, actual)) return@runCatching null
            val values = mapper.readTree(payload)
            require(values.isObject && values.propertyNames().toSet() == setOf("n", "u", "s", "e"))
            require(values["e"].isIntegralNumber)
            val expiresAt = values["e"].asLong()
            if (now.epochSecond >= expiresAt) return@runCatching null
            val scope = text(values, "s")
            if (scope != scopeHash) return@runCatching null
            val name = text(values, "n")
            val u = text(values, "u")
            val id = UUID.fromString(u)
            if (id.toString() != u) return@runCatching null
            Keyset(name, id)
        }.getOrNull()

    private fun mac(payload: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(key, HMAC_ALGO))
        return mac.doFinal(payload)
    }

    companion object {
        private fun constantTimeEquals(
            a: ByteArray,
            b: ByteArray,
        ): Boolean {
            if (a.size != b.size) return false
            var diff = 0
            for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
            return diff == 0
        }

        private const val MAX_ENCODED_BYTES = 1368
        private val mapper =
            tools.jackson.databind.json.JsonMapper
                .builder()
                .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build()

        private fun text(
            node: tools.jackson.databind.JsonNode,
            field: String,
        ): String {
            require(node[field].isString)
            return node[field].asString()
        }
    }
}
