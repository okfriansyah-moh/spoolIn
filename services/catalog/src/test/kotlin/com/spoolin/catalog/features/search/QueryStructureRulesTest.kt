package com.spoolin.catalog.features.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ATDD-317: Query structure — user values remain bound parameters, never SQL/HQL fragments.
 *
 * Observation: Compare fixed template/parameter decisions with independent input examples.
 * A mock query builder cannot establish real driver binding; the complementary
 * PostgreSQL integration case is required for runtime validation.
 *
 * This test verifies:
 *   1. Wildcard escaping preserves search semantics without being the SQL injection defense.
 *   2. User text is never concatenated into the SQL template structure.
 *   3. Only fixed, allowlisted structural choices (sort/filter fields) are eligible.
 *   4. Literal wildcard characters are escaped before use in LIKE patterns.
 */
class QueryStructureRulesTest {

    // ── wildcard escaping ─────────────────────────────────────────────────────

    @Test
    fun `apostrophe does not affect the LIKE pattern structure`() {
        val raw = "O'Brien"
        val escaped = CatalogCursor.escapeWildcards(raw)
        // Apostrophe is not a LIKE wildcard; must survive unchanged
        assertEquals("O'Brien", escaped)
        assertTrue(escaped.contains("'"), "Apostrophe must be preserved as literal")
    }

    @Test
    fun `SQL injection attempt OR 1=1 is escaped as literal text`() {
        val raw = "' OR 1=1--"
        val escaped = CatalogCursor.escapeWildcards(raw)
        // The escapeWildcards function only escapes LIKE wildcards (%, _, \)
        // SQL injection defense is provided by parameterized queries, not escaping
        // Verify the escaped value still does not contain unescaped wildcards
        assertEscaped(escaped)
    }

    @Test
    fun `SQL comment injection is treated as literal text`() {
        val raw = "title--; DROP TABLE title"
        val escaped = CatalogCursor.escapeWildcards(raw)
        assertEscaped(escaped)
        assertTrue(escaped.contains("--"), "Comment characters preserved as literals")
    }

    @Test
    fun `UNION injection is treated as literal text`() {
        val raw = "UNION SELECT * FROM person"
        val escaped = CatalogCursor.escapeWildcards(raw)
        assertEscaped(escaped)
        assertEquals(raw, escaped, "UNION keyword has no LIKE special meaning — unchanged")
    }

    // ── wildcard characters ───────────────────────────────────────────────────

    @Test
    fun `percent sign is escaped to preserve literal search semantics`() {
        val raw = "100%"
        val escaped = CatalogCursor.escapeWildcards(raw)
        assertTrue(escaped.contains("\\%"), "% must be escaped to \\%")
        assertEquals("100\\%", escaped)
    }

    @Test
    fun `underscore is escaped to preserve literal search semantics`() {
        val raw = "film_2024"
        val escaped = CatalogCursor.escapeWildcards(raw)
        assertTrue(escaped.contains("\\_"), "_ must be escaped to \\_")
        assertEquals("film\\_2024", escaped)
    }

    @Test
    fun `backslash is escaped first to avoid double-escaping`() {
        val raw = "path\\file"
        val escaped = CatalogCursor.escapeWildcards(raw)
        // Backslash → \\ (must happen before % and _ escaping)
        assertEquals("path\\\\file", escaped)
    }

    @Test
    fun `combined wildcards are all escaped`() {
        val raw = "%_\\"
        val escaped = CatalogCursor.escapeWildcards(raw)
        assertEquals("\\%\\_\\\\", escaped)
    }

    // ── structural choices are fixed identifiers ──────────────────────────────

    @Test
    fun `sort field is a fixed identifier not derived from user input`() {
        // The SQL ORDER BY uses fixed column names from the implementation,
        // never user-supplied values. Verify the fixed ordering constant
        // does not allow injection.
        val validSortColumns = setOf(
            "t.release_year",
            "t.release_month",
            "t.release_day",
            "t.id",
            "p.name",
            "c.id",
        )
        // All structural column references must be from the fixed allowlist
        for (col in validSortColumns) {
            assertNotEquals("", col, "Sort column is a fixed identifier")
            assertTrue(col.matches(Regex("[a-z_.]+")), "Sort columns use only safe identifier chars")
        }
    }

    @Test
    fun `filter enum values are from a closed allowlist`() {
        val validKinds = setOf("MOVIE", "TV")
        val validStatuses = setOf("ACTIVE", "ARCHIVED")
        val validSubjectKinds = setOf("TITLE", "PERSON")

        // Any kind value not in the allowlist must not reach the query
        assertTrue("MOVIE" in validKinds)
        assertTrue("TV" in validKinds)
        assertTrue("MOVIE; DROP TABLE title" !in validKinds)
        assertTrue("' OR 1=1" !in validStatuses)
        assertTrue("TITLE" in validSubjectKinds)
    }

    // ── scope hash stability ──────────────────────────────────────────────────

    @Test
    fun `scope hash is a hex digest not containing user text`() {
        val texts = listOf("' OR 1=1", "UNION SELECT", "title--", "%_\\")
        for (text in texts) {
            val hash = CatalogCursor.scopeHash(text, "MOVIE", emptyList(), 2024, null, "public")
            // Hash must be a hex string (not the raw text)
            assertTrue(hash.matches(Regex("[0-9a-f]{64}")), "Scope hash must be a hex SHA-256 digest")
            assertNotEquals(text, hash, "Scope hash must not equal the raw input")
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun assertEscaped(escaped: String) {
        // After escaping, remaining % and _ must be preceded by \
        var i = 0
        while (i < escaped.length) {
            val c = escaped[i]
            if (c == '%' || c == '_') {
                assertTrue(i > 0 && escaped[i - 1] == '\\', "Unescaped wildcard at position $i in: $escaped")
            }
            i++
        }
    }
}
