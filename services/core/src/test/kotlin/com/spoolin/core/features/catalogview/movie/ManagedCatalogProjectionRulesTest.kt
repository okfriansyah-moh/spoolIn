package com.spoolin.core.features.catalogview.movie

import com.spoolin.core.features.catalogview.read.CatalogReadProjection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin.MovieValues
import spoolin.v1.Spoolin.PartialDate
import spoolin.v1.Spoolin.Title

/**
 * ATDD-251-T1-R11/R14/R23/R24: Projection mapping rules for managed catalog responses.
 *
 * Verifies that proto-to-GraphQL projection:
 * - Exposes only declared fields (no credentials, emails or internal state).
 * - Correctly maps optional / nullable fields.
 * - Translates partial date precision without inventing absent values.
 * - Represents absent optional title fields as null, not empty string.
 */
class ManagedCatalogProjectionRulesTest {
    // ── title map projection ──────────────────────────────────────────────────

    @Test
    fun `title map exposes id kind state version`() {
        val map = CatalogReadProjection.title(basicTitle(), management = true)
        assertEquals("test-id", map["id"])
        assertEquals("MOVIE", map["kind"])
        assertEquals("ACTIVE", map["state"])
        assertEquals("0", map["version"])
    }

    @Test
    fun `absent optional title fields project as null`() {
        val map = CatalogReadProjection.title(basicTitle(), management = true)
        assertNull(map["originalTitle"], "originalTitle must be null when absent")
        assertNull(map["runtimeMinutes"], "runtimeMinutes must be null when 0")
        assertNull(map["tvStatus"], "tvStatus must be null when absent")
        assertNull(map["releaseContext"], "releaseContext must be null when absent")
    }

    @Test
    fun `release partial date maps year precision without inventing month or day`() {
        val title = basicTitle(year = 2024)
        val map = CatalogReadProjection.title(title, management = true)

        @Suppress("UNCHECKED_CAST")
        val release = map["release"] as Map<String, Any?>
        assertEquals(2024, release["year"])
        assertNull(release["month"], "Month must not be invented")
        assertNull(release["day"], "Day must not be invented")
        assertEquals("YEAR", release["precision"])
    }

    @Test
    fun `full partial date maps all three components`() {
        val title = basicTitle(year = 2024, month = 10, day = 9)
        val map = CatalogReadProjection.title(title, management = true)

        @Suppress("UNCHECKED_CAST")
        val release = map["release"] as Map<String, Any?>
        assertEquals(2024, release["year"])
        assertEquals(10, release["month"])
        assertEquals(9, release["day"])
        assertEquals("DAY", release["precision"])
    }

    @Test
    fun `unknown release date maps precision to UNKNOWN`() {
        val title = basicTitle()
        val map = CatalogReadProjection.title(title, management = true)

        @Suppress("UNCHECKED_CAST")
        val release = map["release"] as Map<String, Any?>
        assertNull(release["year"])
        assertEquals("UNKNOWN", release["precision"])
    }

    @Test
    fun `genres list contains only id maps`() {
        val title =
            Title
                .newBuilder()
                .setId("t1")
                .setKind("MOVIE")
                .setState("ACTIVE")
                .setVersion(1)
                .setValues(
                    MovieValues
                        .newBuilder()
                        .setTitle("X")
                        .setSynopsis("desc")
                        .setSynopsisLanguage("en")
                        .addGenreIds("g1")
                        .addGenreIds("g2")
                        .setRelease(PartialDate.getDefaultInstance())
                        .build(),
                ).setTvEnd(PartialDate.getDefaultInstance())
                .build()
        val map = CatalogReadProjection.title(title, management = true)

        @Suppress("UNCHECKED_CAST")
        val genres = map["genres"] as List<Map<String, Any?>>
        assertEquals(2, genres.size)
        assertEquals("g1", genres[0]["id"])
        assertEquals("g2", genres[1]["id"])
    }

    @Test
    fun `no private account data appears in title projection`() {
        val map = CatalogReadProjection.title(basicTitle(), management = true)
        val keys = map.keys
        assertTrue(keys.none { it.lowercase().contains("password") })
        assertTrue(keys.none { it.lowercase().contains("email") })
        assertTrue(keys.none { it.lowercase().contains("credential") })
        assertTrue(keys.none { it.lowercase().contains("token") })
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun basicTitle(
        year: Int? = null,
        month: Int? = null,
        day: Int? = null,
    ): Title {
        val release =
            PartialDate
                .newBuilder()
                .also { b ->
                    year?.let { b.year = it }
                    month?.let { b.month = it }
                    day?.let { b.day = it }
                }.build()
        return Title
            .newBuilder()
            .setId("test-id")
            .setKind("MOVIE")
            .setState("ACTIVE")
            .setVersion(0)
            .setValues(
                MovieValues
                    .newBuilder()
                    .setTitle("Test Movie")
                    .setSynopsis("A synopsis.")
                    .setSynopsisLanguage("en")
                    .setRelease(release)
                    .build(),
            ).setTvEnd(PartialDate.getDefaultInstance())
            .build()
    }
}
