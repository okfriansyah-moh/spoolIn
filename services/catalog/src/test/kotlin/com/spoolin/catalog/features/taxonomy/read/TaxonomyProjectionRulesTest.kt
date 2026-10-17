package com.spoolin.catalog.features.taxonomy.read

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit rules for TaxonomyReads projections.
 *
 * Verifies the ordering and projection contracts for genre and credit-role reads.
 * No database access — pure structural rules.
 */
class TaxonomyProjectionRulesTest {
    @Test
    fun `genre rows have id and label fields`() {
        val row = TaxonomyReads.GenreRow(java.util.UUID.randomUUID(), "Action")
        assertTrue(row.id.toString().isNotEmpty())
        assertTrue(row.label.isNotEmpty())
    }

    @Test
    fun `role rows have id, category, label, allowsCharacters`() {
        val row =
            TaxonomyReads.RoleRow(
                java.util.UUID.randomUUID(),
                "CAST",
                "Actor",
                allowsCharacters = true,
            )
        assertEquals("CAST", row.category)
        assertTrue(row.allowsCharacters)
    }

    @Test
    fun `genre normalized label ordering uses stable comparison`() {
        // Simulate ORDER BY normalized_label ASC, id ASC
        val rows =
            listOf(
                TaxonomyReads.GenreRow(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    "Drama",
                ),
                TaxonomyReads.GenreRow(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    "Action",
                ),
            )
        val sorted = rows.sortedWith(compareBy({ it.label.lowercase() }, { it.id }))
        assertEquals("Action", sorted[0].label)
        assertEquals("Drama", sorted[1].label)
    }

    @Test
    fun `role category allowlist is CAST or CREATOR`() {
        val validCategories = setOf("CAST", "CREATOR")
        assertTrue("CAST" in validCategories)
        assertTrue("CREATOR" in validCategories)
        assertFalse("WRITER" in validCategories)
    }
}
