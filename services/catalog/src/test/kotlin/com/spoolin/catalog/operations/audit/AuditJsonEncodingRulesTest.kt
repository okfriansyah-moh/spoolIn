package com.spoolin.catalog.operations.audit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest

class AuditJsonEncodingRulesTest {
    private val json = JsonMapper.builder().build()

    @Test
    fun `JSON string encoding round trips every control character and Unicode`() {
        val text = (0..31).map(Int::toChar).joinToString("") + " quote\" slash\\ café 🎬"
        assertEquals(text, json.readTree(AuditDiff.jsonStr(text)).stringValue())
    }

    @Test
    fun `movie and person diffs retain multiline text and exclude private fields`() {
        val text = "First line\nSecond\tline\r\n\"quoted\" \\ café 🎬"
        val movie = json.readTree(AuditDiff.movie(emptyMap(), mapOf("synopsis" to text, "password" to "private")))
        val person = json.readTree(AuditDiff.person(emptyMap(), mapOf("biography" to text)))
        assertEquals(text, movie.path("changes").get(0).path("after").stringValue())
        assertEquals(text, person.path("changes").get(0).path("after").stringValue())
        assertEquals(1, movie.path("changes").size())
        assertFalse(movie.toString().contains("private"))
    }

    @Test
    fun `summary lengths and digests use the escaped JSON UTF8 bytes`() {
        val text = "\n".repeat(40000)
        val encoded = "\"" + "\\n".repeat(40000) + "\""
        val expectedDigest =
            MessageDigest.getInstance("SHA-256").digest(encoded.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        val result = json.readTree(AuditDiff.movie(mapOf("synopsis" to text), mapOf("synopsis" to "changed")))
        val change = result.path("changes").get(0)
        assertTrue(result.path("summarised").booleanValue())
        val expectedBytes = encoded.toByteArray(Charsets.UTF_8).size + "\"changed\"".length
        assertEquals(expectedBytes, change.path("byteLength").intValue())
        assertEquals(expectedDigest, change.path("beforeDigest").stringValue())
        assertTrue(change.path("valueOmitted").booleanValue())
    }

    @Test
    fun `all approved owner paths fit the complete summary envelope`() {
        // Fourteen movie paths and four person paths contain fixed small metadata after omission.
        // Even simultaneous oversized changes therefore retain every path without the fallback truncation.
        val oversized = "x".repeat(70000)
        for (paths in listOf(AuditDiff.MOVIE_PATHS, AuditDiff.PERSON_PATHS)) {
            val result = AuditDiff.compute(emptyMap(), paths.associateWith { oversized }, paths)
            val parsed = json.readTree(result)
            assertEquals(paths.size, parsed.path("changes").size())
            assertTrue(parsed.path("summarised").booleanValue())
            assertFalse(parsed.has("truncated"))
            assertTrue(result.toByteArray(Charsets.UTF_8).size < 65536)
            assertEquals(paths.sorted(), parsed.path("changes").map { it.path("path").stringValue() })
        }
    }
}
