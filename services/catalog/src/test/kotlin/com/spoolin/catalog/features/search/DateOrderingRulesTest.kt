package com.spoolin.catalog.features.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * ATDD-281: Ordering of partial-date tuples with stable UUID ties.
 *
 * Verifies that date ordering (year DESC NULLS LAST, month DESC NULLS LAST,
 * day DESC NULLS LAST, id ASC) produces the canonical sequence.
 *
 * Given rows:
 *   a: 2024-10-09 UUID-a
 *   b: 2024-10    UUID-b (unknown day)
 *   c: 2024       UUID-c (unknown month)
 *   d: 2023       UUID-d
 *   e: unknown    UUID-e
 *   f: 2024-10-09 UUID-f (equal date to a, UUID tie)
 *   g: 2024-10-09 UUID-g (equal date, UUID after f)
 *
 * Expected order: a, b, c, d, e (for first 5), then f before g for equal-date rows.
 * Ordering must NOT manufacture chronological precision.
 */
class DateOrderingRulesTest {

    private data class DateRow(
        val year: Int?,
        val month: Int?,
        val day: Int?,
        val id: UUID,
    )

    /** Comparator matching the SQL ORDER BY clause:
     *  release_year DESC NULLS LAST, release_month DESC NULLS LAST,
     *  release_day DESC NULLS LAST, id ASC.
     */
    private val dateOrder = Comparator<DateRow> { a, b ->
        val yCmp = descNullsLast(a.year, b.year)
        if (yCmp != 0) return@Comparator yCmp
        val mCmp = descNullsLast(a.month, b.month)
        if (mCmp != 0) return@Comparator mCmp
        val dCmp = descNullsLast(a.day, b.day)
        if (dCmp != 0) return@Comparator dCmp
        // UUID ASC tie-break
        a.id.compareTo(b.id)
    }

    /** DESC ordering with NULLS LAST:
     *  null comes after non-null; among non-nulls, larger value comes first. */
    private fun descNullsLast(a: Int?, b: Int?): Int = when {
        a == null && b == null -> 0
        a == null -> 1   // null a comes after non-null b
        b == null -> -1  // non-null a comes before null b
        else -> b.compareTo(a)  // DESC: larger value first
    }

    @Test
    fun `first five rows order a,b,c,d,e regardless of insertion order`() {
        // Fixed UUIDs so relative ordering is deterministic
        val uuidA = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val uuidB = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val uuidC = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val uuidD = UUID.fromString("00000000-0000-0000-0000-000000000004")
        val uuidE = UUID.fromString("00000000-0000-0000-0000-000000000005")

        val rows = listOf(
            DateRow(null, null, null, uuidE), // e: unknown year — last
            DateRow(2023, null, null, uuidD), // d: 2023 — before 2024
            DateRow(2024, null, null, uuidC), // c: year-only — after 2024-10
            DateRow(2024, 10, null, uuidB),   // b: 2024-10 — after 2024-10-09
            DateRow(2024, 10, 9, uuidA),      // a: 2024-10-09 — newest
        )

        val sorted = rows.sortedWith(dateOrder)

        assertEquals(listOf(uuidA, uuidB, uuidC, uuidD, uuidE), sorted.map { it.id })
    }

    @Test
    fun `equal date rows are ordered by UUID ascending`() {
        val uuidF = UUID.fromString("00000000-0000-0000-0000-000000000006")
        val uuidG = UUID.fromString("00000000-0000-0000-0000-000000000007")

        val rows = listOf(
            DateRow(2024, 10, 9, uuidG), // g: same date as f, UUID after f
            DateRow(2024, 10, 9, uuidF), // f: same date as g, UUID before g
        )

        val sorted = rows.sortedWith(dateOrder)
        assertEquals(listOf(uuidF, uuidG), sorted.map { it.id })
    }

    @Test
    fun `ordering does not manufacture chronological precision`() {
        // A year-only row (c: 2024, no month) must sort after a year+month row (b: 2024-10)
        // even though 2024 "contains" October — no fabrication of missing month
        val uuidB = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val uuidC = UUID.fromString("00000000-0000-0000-0000-000000000003")

        val rows = listOf(
            DateRow(2024, null, null, uuidC), // year-only
            DateRow(2024, 10, null, uuidB),   // year+month
        )
        val sorted = rows.sortedWith(dateOrder)
        // year+month (b) comes before year-only (c) in DESC ordering
        assertEquals(uuidB, sorted[0].id, "year+month must precede year-only (DESC)")
        assertEquals(uuidC, sorted[1].id, "year-only must follow year+month (DESC NULLS LAST)")
    }

    @Test
    fun `unknown year rows appear after all known-year rows`() {
        val knownId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val unknownId = UUID.fromString("00000000-0000-0000-0000-000000000002")

        val rows = listOf(
            DateRow(null, null, null, unknownId),
            DateRow(1, null, null, knownId),
        )
        val sorted = rows.sortedWith(dateOrder)
        assertEquals(knownId, sorted[0].id, "Known year must precede unknown year")
        assertEquals(unknownId, sorted[1].id, "Unknown year must be last (NULLS LAST)")
    }

    @Test
    fun `all five rows together produce canonical sequence`() {
        val uuidA = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val uuidB = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val uuidC = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val uuidD = UUID.fromString("00000000-0000-0000-0000-000000000004")
        val uuidE = UUID.fromString("00000000-0000-0000-0000-000000000005")
        val uuidF = UUID.fromString("00000000-0000-0000-0000-000000000006")
        val uuidG = UUID.fromString("00000000-0000-0000-0000-000000000007")

        val rows = listOf(
            DateRow(null, null, null, uuidE),
            DateRow(2024, 10, 9, uuidG),
            DateRow(2023, null, null, uuidD),
            DateRow(2024, null, null, uuidC),
            DateRow(2024, 10, null, uuidB),
            DateRow(2024, 10, 9, uuidF),
            DateRow(2024, 10, 9, uuidA),
        )

        val sorted = rows.sortedWith(dateOrder)
        // Expected: a, f, g (equal date, ascending UUID), b, c, d, e
        assertEquals(uuidA, sorted[0].id)
        assertEquals(uuidF, sorted[1].id)
        assertEquals(uuidG, sorted[2].id)
        assertEquals(uuidB, sorted[3].id)
        assertEquals(uuidC, sorted[4].id)
        assertEquals(uuidD, sorted[5].id)
        assertEquals(uuidE, sorted[6].id)
    }
}
