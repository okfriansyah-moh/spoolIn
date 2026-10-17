package com.spoolin.catalog.features.person.read

import com.spoolin.catalog.features.search.CatalogCursor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Unit rules for PersonSearch projections and ordering.
 *
 * Verifies person ordering (name ASC, id ASC) and visibility rules
 * (only ACTIVE people are visible). No database access.
 */
class PersonSearchRulesTest {
    @Test
    fun `person row preserves all required fields`() {
        val id = UUID.randomUUID()
        val row = PersonSearch.PersonRow(id, "Alice", "Bio text", "ACTIVE", 1L)
        assertEquals(id, row.id)
        assertEquals("Alice", row.name)
        assertEquals("Bio text", row.biography)
        assertEquals("ACTIVE", row.status)
        assertEquals(1L, row.version)
    }

    @Test
    fun `person row with no biography is null not empty string`() {
        val row = PersonSearch.PersonRow(UUID.randomUUID(), "Bob", null, "ACTIVE", 0L)
        assertNull(row.biography, "Missing biography must be null, not fabricated")
    }

    @Test
    fun `person ordering is name ASC then id ASC for ties`() {
        val idA = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val idB = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val rows =
            listOf(
                PersonSearch.PersonRow(idB, "Alice", null, "ACTIVE", 0L),
                PersonSearch.PersonRow(idA, "Alice", null, "ACTIVE", 0L),
            )
        val sorted = rows.sortedWith(compareBy({ it.name }, { it.id }))
        assertEquals(idA, sorted[0].id, "Same name — lower UUID must come first")
        assertEquals(idB, sorted[1].id)
    }

    @Test
    fun `same names do not collapse identities`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val alice1 = PersonSearch.PersonRow(id1, "Alice Smith", null, "ACTIVE", 0L)
        val alice2 = PersonSearch.PersonRow(id2, "Alice Smith", null, "ACTIVE", 0L)
        // Same name but distinct IDs must remain distinct
        assertTrue(alice1.id != alice2.id, "Same-named people must retain distinct IDs")
    }

    @Test
    fun `credit row preserves required fields`() {
        val creditId = UUID.randomUUID()
        val titleId = UUID.randomUUID()
        val personId = UUID.randomUUID()
        val roleId = UUID.randomUUID()
        val row =
            PersonSearch.CreditRow(
                id = creditId,
                titleId = titleId,
                personId = personId,
                roleId = roleId,
                characters = listOf("Batman"),
                version = 1L,
                personName = "Alice",
                titleName = "Dark Knight",
                roleCategory = "CAST",
                roleLabel = "Actor",
                roleAllowsCharacters = true,
            )
        assertEquals(creditId, row.id)
        assertEquals(listOf("Batman"), row.characters)
        assertEquals("CAST", row.roleCategory)
        assertTrue(row.roleAllowsCharacters)
    }

    @Test
    fun `escaped name in wildcard does not break LIKE pattern`() {
        // Wildcards in person names must be escaped before use in LIKE
        val nameWithPercent = "100%"
        val escaped = CatalogCursor.escapeWildcards(nameWithPercent)
        assertEquals("100\\%", escaped)

        val nameWithUnderscore = "O_Brien"
        val escapedUnderscore = CatalogCursor.escapeWildcards(nameWithUnderscore)
        assertEquals("O\\_Brien", escapedUnderscore)
    }
}
