package com.spoolin.catalog.features.movie

import com.spoolin.support.policy.InputViolation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin.MovieValues
import spoolin.v1.Spoolin.PartialDate

/**
 * ATDD-018 / PRD-BOUND-03/04: Movie input validation boundary rules.
 *
 * Verifies that invalid or missing required fields are rejected without committing any record.
 * PRD-CONCEPT-04: partial dates preserve supplied precision; no invented values.
 * PRD-POLICY-26: markup characters are preserved as plain text, not executed.
 */
class MovieValidationRulesTest {
    // ── create validation ──────────────────────────────────────────────────────

    @Test
    fun `valid minimal create input is accepted`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "Valid Movie",
                    synopsis = "A valid synopsis.",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                ),
            )
        val result = MovieValidation.validateCreate(values)
        assertEquals("Valid Movie", result.displayTitle)
        assertNull(result.originalTitle)
        assertEquals(2024, result.releaseYear)
        assertNull(result.releaseMonth)
    }

    @Test
    fun `blank title is rejected`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "   ",
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                ),
            )
        assertViolation("title") { MovieValidation.validateCreate(values) }
    }

    @Test
    fun `title exceeding 255 code points is rejected`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "A".repeat(256),
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                ),
            )
        assertViolation("title") { MovieValidation.validateCreate(values) }
    }

    @Test
    fun `exactly 255 code point title is accepted`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "A".repeat(255),
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                ),
            )
        val result = MovieValidation.validateCreate(values)
        assertEquals(255, result.displayTitle.length)
    }

    @Test
    fun `synopsis exceeding 5000 code points is rejected`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "T",
                    synopsis = "X".repeat(5001),
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                ),
            )
        assertViolation("synopsis") { MovieValidation.validateCreate(values) }
    }

    @Test
    fun `exactly 5000 code point synopsis is accepted`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "T",
                    synopsis = "X".repeat(5000),
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                ),
            )
        val result = MovieValidation.validateCreate(values)
        assertEquals(5000, result.synopsis.length)
    }

    @Test
    fun `empty genre list is rejected`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "T",
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = emptyList(),
                    year = 2024,
                ),
            )
        assertViolation("genreIds") { MovieValidation.validateCreate(values) }
    }

    @Test
    fun `release year 0 is rejected`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "T",
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 0,
                ),
            )
        assertViolation("release.year") { MovieValidation.validateCreate(values) }
    }

    @Test
    fun `year-only release preserves precision — no month or day invented`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "T",
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2020,
                ),
            )
        val result = MovieValidation.validateCreate(values)
        assertEquals(2020, result.releaseYear)
        assertNull(result.releaseMonth, "Month must not be invented")
        assertNull(result.releaseDay, "Day must not be invented")
    }

    @Test
    fun `optional originalTitle is trimmed and stored when present`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "T",
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                    originalTitle = " Orig ",
                ),
            )
        val result = MovieValidation.validateCreate(values)
        assertEquals("Orig", result.originalTitle)
    }

    @Test
    fun `runtime minutes 0 is rejected`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "T",
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                    runtimeMinutes = 0,
                ),
            )
        assertViolation("runtimeMinutes") { MovieValidation.validateCreate(values) }
    }

    @Test
    fun `runtime minutes 100001 is rejected`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "T",
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                    runtimeMinutes = 100001,
                ),
            )
        assertViolation("runtimeMinutes") { MovieValidation.validateCreate(values) }
    }

    @Test
    fun `markup characters in title are preserved as plain text`() {
        val values =
            movieValues(
                MovieValuesFixture(
                    title = "<script>alert('x')</script>",
                    synopsis = "desc",
                    synopsisLanguage = "en",
                    genres = listOf("00000000-0000-0000-0000-000000000001"),
                    year = 2024,
                ),
            )
        val result = MovieValidation.validateCreate(values)
        assertEquals("<script>alert('x')</script>", result.displayTitle)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private data class MovieValuesFixture(
        val title: String,
        val synopsis: String,
        val synopsisLanguage: String,
        val genres: List<String>,
        val year: Int,
        val originalTitle: String? = null,
        val runtimeMinutes: Int? = null,
    )

    private fun movieValues(fixture: MovieValuesFixture): MovieValues {
        val release = PartialDate.newBuilder().setYear(fixture.year).build()
        val builder =
            MovieValues
                .newBuilder()
                .setTitle(fixture.title)
                .setSynopsis(fixture.synopsis)
                .setSynopsisLanguage(fixture.synopsisLanguage)
                .addAllGenreIds(fixture.genres)
                .setRelease(release)
        fixture.originalTitle?.let { builder.originalTitle = it }
        fixture.runtimeMinutes?.let { builder.runtimeMinutes = it }
        return builder.build()
    }

    private fun assertViolation(
        expectedPath: String,
        block: () -> Unit,
    ) {
        val e = assertThrows(InputViolation::class.java, block)
        assertEquals(expectedPath, e.path, "Expected violation on path '$expectedPath' but got '${e.path}'")
    }
}
