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
class PeopleCursor(private val key: ByteArray) {

    data class Keyset(val name: String, val id: UUID)

    fun encode(keyset: Keyset, scopeHash: String, now: Instant): String {
        val expiresAt = now.plusSeconds(CURSOR_TTL_SECONDS).epochSecond
        val nameSafe = keyset.name.replace("\\", "\\\\").replace("\"", "\\\"")
        val json = "{\"n\":\"$nameSafe\",\"u\":\"${keyset.id}\"," +
            "\"s\":\"$scopeHash\",\"e\":$expiresAt}"
        val payload = json.toByteArray(Charsets.UTF_8)
        val mac = mac(payload)
        val bytes = payload + mac
        require(bytes.size <= MAX_CURSOR_BYTES) { "People cursor exceeds 1024 bytes" }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun decode(encoded: String, scopeHash: String, now: Instant): Keyset? = runCatching {
        val bytes = Base64.getUrlDecoder().decode(encoded)
        if (bytes.size < HMAC_LEN + 1) return@runCatching null
        val payload = bytes.copyOfRange(0, bytes.size - HMAC_LEN)
        val expected = mac(payload)
        val actual = bytes.copyOfRange(bytes.size - HMAC_LEN, bytes.size)
        if (!constantTimeEquals(expected, actual)) return@runCatching null
        val json = String(payload, Charsets.UTF_8)
        val values = parseSimpleJson(json) ?: return@runCatching null
        val expiresAt = values["e"] as? Long ?: return@runCatching null
        if (now.epochSecond >= expiresAt) return@runCatching null
        val scope = values["s"] as? String ?: return@runCatching null
        if (scope != scopeHash) return@runCatching null
        val name = values["n"] as? String ?: return@runCatching null
        val u = values["u"] as? String ?: return@runCatching null
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
        private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
            if (a.size != b.size) return false
            var diff = 0
            for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
            return diff == 0
        }

        private fun parseSimpleJson(json: String): Map<String, Any>? = runCatching {
            val cleaned = json.trim().removePrefix("{").removeSuffix("}")
            val result = mutableMapOf<String, Any>()
            var pos = 0
            while (pos < cleaned.length) {
                // Skip whitespace
                while (pos < cleaned.length && cleaned[pos].isWhitespace()) pos++
                if (pos >= cleaned.length) break
                // Parse key
                if (cleaned[pos] != '"') return@runCatching null
                val keyEnd = findClosingQuote(cleaned, pos + 1)
                val key = unescape(cleaned.substring(pos + 1, keyEnd))
                pos = keyEnd + 1
                while (pos < cleaned.length && cleaned[pos].isWhitespace()) pos++
                if (pos >= cleaned.length || cleaned[pos] != ':') return@runCatching null
                pos++
                while (pos < cleaned.length && cleaned[pos].isWhitespace()) pos++
                // Parse value
                val value: Any
                if (cleaned[pos] == '"') {
                    val valEnd = findClosingQuote(cleaned, pos + 1)
                    value = unescape(cleaned.substring(pos + 1, valEnd))
                    pos = valEnd + 1
                } else {
                    val end = cleaned.indexOf(',', pos).let { if (it < 0) cleaned.length else it }
                    value = cleaned.substring(pos, end).trim().toLongOrNull() ?: return@runCatching null
                    pos = end
                }
                result[key] = value
                while (pos < cleaned.length && cleaned[pos].isWhitespace()) pos++
                if (pos < cleaned.length && cleaned[pos] == ',') pos++
            }
            result
        }.getOrNull()

        private fun findClosingQuote(s: String, start: Int): Int {
            var i = start
            while (i < s.length) {
                if (s[i] == '"' && (i == 0 || s[i - 1] != '\\')) return i
                i++
            }
            return s.length
        }

        private fun unescape(s: String) = s.replace("\\\"", "\"").replace("\\\\", "\\")
    }
}
