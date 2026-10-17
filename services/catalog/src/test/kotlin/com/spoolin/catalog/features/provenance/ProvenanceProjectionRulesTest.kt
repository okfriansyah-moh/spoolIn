package com.spoolin.catalog.features.provenance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit rules for ProvenanceReads projections.
 *
 * Verifies provenance row field contracts and internal field exclusion.
 * No database access.
 */
class ProvenanceProjectionRulesTest {
    @Test
    fun `provenance row preserves all required fields`() {
        val now = Instant.parse("2026-10-17T06:00:00Z")
        val row =
            ProvenanceReads.ProvenanceRow(
                source = "tmdb",
                externalId = "12345",
                recordUrl = "https://www.themoviedb.org/movie/12345",
                retrievedAt = now,
                reuseNote = "CC0 metadata",
                fieldSources = """{"title": {"url": "https://example.com"}}""",
                locallyChanged = listOf("synopsis"),
            )
        assertEquals("tmdb", row.source)
        assertEquals("12345", row.externalId)
        assertEquals("https://www.themoviedb.org/movie/12345", row.recordUrl)
        assertEquals(now, row.retrievedAt)
        assertEquals("CC0 metadata", row.reuseNote)
        assertEquals(listOf("synopsis"), row.locallyChanged)
    }

    @Test
    fun `optional recordUrl can be null without fabrication`() {
        val row =
            ProvenanceReads.ProvenanceRow(
                source = "manual",
                externalId = "m1",
                recordUrl = null,
                retrievedAt = Instant.now(),
                reuseNote = "Manual entry",
                fieldSources = "{}",
                locallyChanged = emptyList(),
            )
        assertNull(row.recordUrl, "Missing record URL must be null, not invented")
    }

    @Test
    fun `empty locally_changed is valid`() {
        val row =
            ProvenanceReads.ProvenanceRow(
                source = "tmdb",
                externalId = "1",
                recordUrl = null,
                retrievedAt = Instant.now(),
                reuseNote = "note",
                fieldSources = "{}",
                locallyChanged = emptyList(),
            )
        assertTrue(row.locallyChanged.isEmpty(), "Empty locally_changed is valid")
    }

    @Test
    fun `subject_kind and subject_id are internal fields not in the row projection`() {
        // ProvenanceRow does not expose subject_kind or subject_id
        // These are internal fields used for routing only
        val row =
            ProvenanceReads.ProvenanceRow(
                source = "tmdb",
                externalId = "1",
                recordUrl = null,
                retrievedAt = Instant.now(),
                reuseNote = "note",
                fieldSources = "{}",
                locallyChanged = emptyList(),
            )
        // Row has no subject_kind or subject_id fields (internal, not projected)
        val fields = row.javaClass.declaredFields.map { it.name }
        assertTrue("subject_kind" !in fields, "subject_kind must not be in public projection")
        assertTrue("subject_id" !in fields, "subject_id must not be in public projection")
    }

    @Test
    fun `deleted_at is internal and not in the row projection`() {
        val fields = ProvenanceReads.ProvenanceRow::class.java.declaredFields.map { it.name }
        assertTrue("deleted_at" !in fields, "deleted_at is internal — must not appear in projection")
        assertTrue("deletedAt" !in fields, "deleted_at is internal — must not appear in projection")
    }
}
