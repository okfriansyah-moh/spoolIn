package com.spoolin.core.features.draft

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * ATDD-293 — Validate safe draft shape independently of publication completeness.
 *
 * Permitted incomplete values remain recoverable within the draft envelope.
 * Unknown/private/file keys, invalid types/Unicode/IDs and oversize payload reject.
 * Publication validation is not silently applied as draft completeness.
 */
class DraftShapeRulesTest {
    private val codec = DraftPayload()

    // ── MOVIE shapes ─────────────────────────────────────────────────────────

    @Test
    fun `MOVIE blank title is accepted as an incomplete draft`() {
        val bytes = codec.encode("MOVIE", """{"title":""}""")
        assertNotNull(bytes)
        assertDoesNotThrow { codec.decode(bytes) }
    }

    @Test
    fun `MOVIE partial fields are accepted`() {
        val bytes = codec.encode("MOVIE", """{"synopsis":"First draft."}""")
        assertNotNull(bytes)
    }

    @Test
    fun `MOVIE all allowed fields are accepted`() {
        val json =
            """
            {
              "title":"T","originalTitle":"OT","synopsis":"S",
              "genreIds":["3fa85f64-5717-4562-b3fc-2c963f66afa6"],
              "release":{"year":2024},"runtimeMinutes":90,
              "originalLanguage":"en","synopsisLanguage":"en",
              "countries":["US"],"tvStatus":null,"tvEnd":null,"releaseContext":null
            }
            """.trimIndent()
        assertDoesNotThrow { codec.encode("MOVIE", json) }
    }

    @Test
    fun `MOVIE text beyond current field policy limit is accepted in draft`() {
        // Draft preserves over-limit text; final validation happens at Catalog commit
        val longText = "A".repeat(10_000)
        val bytes = codec.encode("MOVIE", """{"title":"$longText"}""")
        assertNotNull(bytes)
    }

    @Test
    fun `MOVIE unknown key is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode("MOVIE", """{"title":"T","unknownField":"x"}""")
        }
    }

    @Test
    fun `MOVIE password key is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode("MOVIE", """{"title":"T","password":"secret"}""")
        }
    }

    @Test
    fun `MOVIE piiCipher key is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode("MOVIE", """{"piiCipher":"base64=="}""")
        }
    }

    @Test
    fun `MOVIE empty object is accepted`() {
        assertDoesNotThrow { codec.encode("MOVIE", "{}") }
    }

    // ── PERSON shapes ────────────────────────────────────────────────────────

    @Test
    fun `PERSON partial name is accepted`() {
        assertDoesNotThrow { codec.encode("PERSON", """{"name":"Alice"}""") }
    }

    @Test
    fun `PERSON null biography is accepted`() {
        assertDoesNotThrow { codec.encode("PERSON", """{"name":"Bob","biography":null}""") }
    }

    @Test
    fun `PERSON unknown key is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode("PERSON", """{"name":"Charlie","email":"x@y.com"}""")
        }
    }

    // ── CREDIT shapes ────────────────────────────────────────────────────────

    @Test
    fun `CREDIT all fields are accepted`() {
        val json =
            """
            {
              "titleId":"3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "personId":"3fa85f64-5717-4562-b3fc-2c963f66afb7",
              "roleId":"3fa85f64-5717-4562-b3fc-2c963f66afc8",
              "characters":["Hero","Narrator"]
            }
            """.trimIndent()
        assertDoesNotThrow { codec.encode("CREDIT", json) }
    }

    @Test
    fun `CREDIT empty characters array is accepted`() {
        val json = """{"titleId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","characters":[]}"""
        assertDoesNotThrow { codec.encode("CREDIT", json) }
    }

    @Test
    fun `CREDIT unknown key is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode("CREDIT", """{"roleId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","secretField":"val"}""")
        }
    }

    // ── Kind validation ───────────────────────────────────────────────────────

    @Test
    fun `unknown target kind is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode("SERIES", """{"title":"X"}""")
        }
    }

    // ── Payload size limits ───────────────────────────────────────────────────

    @Test
    fun `payload exactly at 65536-byte limit is accepted`() {
        // build a JSON string whose UTF-8 encoding is exactly MAX_PLAINTEXT_BYTES bytes
        val filler = "A".repeat(65536 - """{"title":""}""".toByteArray().size)
        val json = """{"title":"$filler"}"""
        require(json.toByteArray(Charsets.UTF_8).size <= 65536) { "Test setup exceeded limit" }
        assertDoesNotThrow { codec.encode("MOVIE", json) }
    }

    @Test
    fun `payload one byte over 65536-byte limit is rejected`() {
        val filler = "A".repeat(65536 - """{"title":""}""".toByteArray().size + 1)
        val json = """{"title":"$filler"}"""
        require(json.toByteArray(Charsets.UTF_8).size > 65536) { "Test setup did not exceed limit" }
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode("MOVIE", json)
        }
    }

    // ── Invalid JSON ──────────────────────────────────────────────────────────

    @Test
    fun `non-JSON string is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode("MOVIE", "not json at all")
        }
    }

    @Test
    fun `JSON array instead of object is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encode("MOVIE", """["title","Synopsis"]""")
        }
    }

    // ── Roundtrip ─────────────────────────────────────────────────────────────

    @Test
    fun `encode then decode preserves exact UTF-8 text`() {
        val json = """{"name":"Ren\u00e9e","biography":"Caf\u00e9 owner"}"""
        val bytes = codec.encode("PERSON", json)
        val decoded = codec.decode(bytes)
        val decodedBytes = decoded.toByteArray(Charsets.UTF_8)
        assertArrayEquals(json.toByteArray(Charsets.UTF_8), decodedBytes)
    }
}
