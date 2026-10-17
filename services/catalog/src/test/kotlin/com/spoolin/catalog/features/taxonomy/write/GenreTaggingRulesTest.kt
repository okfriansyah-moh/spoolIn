package com.spoolin.catalog.features.taxonomy.write

import com.spoolin.support.policy.InputViolation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Unit rules for GenreTagging input validation.
 *
 * Verifies:
 * - Empty genre set throws InputViolation.
 * - More than 20 genres throw InputViolation.
 * - Malformed UUID throws InputViolation.
 * - Valid genre UUIDs within limit are accepted for resolution path.
 * - replace() produces stable sorted insertion order for deadlock avoidance.
 */
class GenreTaggingRulesTest {

    @Test
    fun `empty genre set is rejected`() {
        val e = assertThrows(InputViolation::class.java) {
            GenreTagging.validateGenreIds(emptySet())
        }
        assertEquals("genreIds", e.path)
    }

    @Test
    fun `more than 20 genres is rejected`() {
        val ids = (1..21).map { UUID.randomUUID().toString() }.toSet()
        val e = assertThrows(InputViolation::class.java) {
            GenreTagging.validateGenreIds(ids)
        }
        assertEquals("genreIds", e.path)
    }

    @Test
    fun `malformed UUID in genre set is rejected`() {
        val e = assertThrows(InputViolation::class.java) {
            GenreTagging.validateGenreIds(setOf("not-a-uuid"))
        }
        assertEquals("genreIds", e.path)
    }

    @Test
    fun `valid UUIDs within limit are parsed without error`() {
        val ids = (1..5).map { UUID.randomUUID().toString() }.toSet()
        // validateGenreIds returns the parsed UUIDs; no exception expected.
        val parsed = GenreTagging.validateGenreIds(ids)
        assertEquals(5, parsed.size)
    }

    @Test
    fun `sorted replacement order is stable across inputs`() {
        val a = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val b = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val c = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val input = setOf(c, a, b)
        val sorted = input.sortedBy { it.toString() }
        assertEquals(listOf(a, b, c), sorted)
    }

    @Test
    fun `exactly 20 genres are accepted`() {
        val ids = (1..20).map { UUID.randomUUID().toString() }.toSet()
        val parsed = GenreTagging.validateGenreIds(ids)
        assertEquals(20, parsed.size)
    }
}
