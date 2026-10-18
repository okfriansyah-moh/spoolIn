package com.spoolin.catalog.seed

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/** ATDD-301: pure identity/serialization rules; transaction and tombstones need database proof. */
class SeedManifestRulesTest {
    private val namespace = UUID.fromString("9da61864-80f5-5c70-bc79-3914da94079e")
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `UUID matches independently assembled fixed length-prefixed source tuple`() {
        // 8-byte source, 5-byte kind and 3-byte external ID, each with a four-byte length.
        val name =
            byteArrayOf(0, 0, 0, 8) + "wikidata".toByteArray() + byteArrayOf(0, 0, 0, 5) +
                "MOVIE".toByteArray() + byteArrayOf(0, 0, 0, 3) + "Q42".toByteArray()
        val bytes =
            ByteBuffer
                .allocate(16)
                .putLong(namespace.mostSignificantBits)
                .putLong(namespace.leastSignificantBits)
                .array()
        val digest = MessageDigest.getInstance("SHA-1").digest(bytes + name)
        digest[6] = ((digest[6].toInt() and 15) or 80).toByte()
        digest[8] = ((digest[8].toInt() and 63) or 128).toByte()
        val expected = ByteBuffer.wrap(digest).let { UUID(it.long, it.long) }
        val actual = SeedIdentities.record(namespace, "wikidata", "MOVIE", "Q42")
        assertEquals(expected, actual)
        assertEquals(5, actual.version())
        assertEquals(2, actual.variant())
    }

    @Test
    fun `ambiguous concatenations and kind or source changes keep distinct identities`() {
        assertNotEquals(
            SeedIdentities.record(namespace, "ab", "c", "id"),
            SeedIdentities.record(namespace, "a", "bc", "id"),
        )
        assertNotEquals(
            SeedIdentities.record(namespace, "wikidata", "MOVIE", "Q42"),
            SeedIdentities.record(namespace, "wikidata", "TV", "Q42"),
        )
        assertNotEquals(
            SeedIdentities.record(namespace, "wikidata", "MOVIE", "Q42"),
            SeedIdentities.record(namespace, "other", "MOVIE", "Q42"),
        )
    }

    @Test
    fun `retrieval timestamps and property order do not change checksums but title edits do`() {
        val original =
            mapper.readTree(
                """{"title":"Example","sources":[{"retrievedAt":"2026-01-01T00:00:00Z","license":"CC0-1.0"}]}""",
            )
        val later =
            mapper.readTree(
                """{"sources":[{"license":"CC0-1.0","retrievedAt":"2026-10-18T00:00:00Z"}],"title":"Example"}""",
            )
        assertEquals(SeedIdentities.hashHex(original), SeedIdentities.hashHex(later))
        assertNotEquals(
            SeedIdentities.hashHex(original),
            SeedIdentities.hashHex(mapper.readTree("""{"title":"Changed"}""")),
        )
        assertEquals(
            SeedIdentities.record(namespace, "wikidata", "MOVIE", "Q42"),
            SeedIdentities.record(namespace, "wikidata", "MOVIE", "Q42"),
        )
    }

    @Test
    fun `initialization receipt uses the separate documented name`() {
        assertNotEquals(
            SeedIdentities.initialization(namespace, "seed-v1"),
            SeedIdentities.record(namespace, "wikidata", "MOVIE", "seed-v1"),
        )
        assertNotEquals(
            SeedIdentities.initialization(namespace, "seed-v1"),
            SeedIdentities.initialization(namespace, "seed-v2"),
        )
    }
}
