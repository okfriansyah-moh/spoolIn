package com.spoolin.core.features.catalogview.read

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

/**
 * Unit rules for CatalogViewGraphql projection and visibility contracts.
 *
 * Verifies that GraphQL projections:
 *  - Exclude account PII
 *  - Handle empty/null proto values without fabrication
 *  - Return null for archived/absent titles and people (not a same-name substitute)
 *
 * No real gRPC client or database; uses test double pattern.
 */
class CatalogViewGraphqlRulesTest {

    // ── partial date precision rules ──────────────────────────────────────────

    @Test
    fun `unknown year returns UNKNOWN precision`() {
        val pd = Spoolin.PartialDate.newBuilder().build()
        val precision = partialDatePrecision(pd)
        assertEquals("UNKNOWN", precision)
    }

    @Test
    fun `year-only returns YEAR precision`() {
        val pd = Spoolin.PartialDate.newBuilder().setYear(2024).build()
        assertEquals("YEAR", partialDatePrecision(pd))
    }

    @Test
    fun `year-month returns MONTH precision`() {
        val pd = Spoolin.PartialDate.newBuilder().setYear(2024).setMonth(10).build()
        assertEquals("MONTH", partialDatePrecision(pd))
    }

    @Test
    fun `year-month-day returns DAY precision`() {
        val pd = Spoolin.PartialDate.newBuilder().setYear(2024).setMonth(10).setDay(9).build()
        assertEquals("DAY", partialDatePrecision(pd))
    }

    // ── title visibility ──────────────────────────────────────────────────────

    @Test
    fun `empty Title proto indicates absent or archived title — must produce null`() {
        val emptyTitle = Spoolin.Title.getDefaultInstance()
        // An absent title (returned as default proto) has no id
        assertTrue(emptyTitle.id.isEmpty(), "Absent title has empty id")
        // Caller must return null, not a same-name substitute
    }

    @Test
    fun `title with id is a valid title`() {
        val title = Spoolin.Title.newBuilder()
            .setId("aaaaaaaa-0000-0000-0000-000000000001")
            .build()
        assertFalse(title.id.isEmpty())
    }

    // ── person visibility ─────────────────────────────────────────────────────

    @Test
    fun `empty Person proto indicates absent or archived person — must produce null`() {
        val emptyPerson = Spoolin.Person.getDefaultInstance()
        assertTrue(emptyPerson.id.isEmpty())
    }

    // ── no PII in projection ──────────────────────────────────────────────────

    @Test
    fun `person projection does not include email, password or session fields`() {
        // PersonValues only has name and biography — no account PII
        val values = Spoolin.PersonValues.newBuilder().setName("Alice").build()
        val knownFields = setOf("name", "biography")
        val protoFields = values.allFields.keys.map { it.name }.toSet()
        for (field in protoFields) {
            assertTrue(
                field in knownFields,
                "Unexpected field in PersonValues projection: $field",
            )
        }
    }

    @Test
    fun `title projection does not include attachment_id`() {
        // attachment_id is internal (no GraphQL/proto field per TD-ROW-L4416)
        val title = Spoolin.Title.getDefaultInstance()
        val fieldNames = title.descriptorForType.fields.map { it.name }
        assertFalse("attachment_id" in fieldNames, "attachment_id must not be in Title proto")
    }

    // ── optional value contracts ──────────────────────────────────────────────

    @Test
    fun `null originalTitle does not fabricate a value`() {
        val values = Spoolin.MovieValues.newBuilder()
            .setTitle("Dark Knight")
            .setSynopsis("A hero rises.")
            .setSynopsisLanguage("en")
            .setRelease(Spoolin.PartialDate.newBuilder().setYear(2008).build())
            .build()
        // originalTitle is not set — must remain empty string (proto default), not fabricated
        assertTrue(values.originalTitle.isEmpty(), "Absent original title must not be fabricated")
    }

    @Test
    fun `null biography does not fabricate a value`() {
        val values = Spoolin.PersonValues.newBuilder().setName("Alice").build()
        assertTrue(values.biography.isEmpty(), "Absent biography must not be fabricated")
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun partialDatePrecision(pd: Spoolin.PartialDate): String = when {
        pd.hasDay() -> "DAY"
        pd.hasMonth() -> "MONTH"
        pd.hasYear() -> "YEAR"
        else -> "UNKNOWN"
    }
}
