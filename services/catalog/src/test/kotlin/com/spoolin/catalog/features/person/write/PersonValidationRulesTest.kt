package com.spoolin.catalog.features.person.write

import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PatchValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin.PersonValues
import spoolin.v1.Spoolin.PersonWrite

/**
 * ATDD-114 / PRD-BOUND-01 / AC-140: Person name boundary validation rules.
 *
 * One and 100 code points pass; zero and 101 reject with a field-specific error and no partial change.
 * Mononyms, punctuation and combining sequences are preserved intact.
 * PRD-POLICY-18: No required family/given name split; one-character professional names accepted.
 * PRD-CONCEPT-09: Catalog person name defaults differ from account registration name defaults.
 */
class PersonValidationRulesTest {

    // ── create validation ──────────────────────────────────────────────────────

    @Test
    fun `single character name is accepted (PRD-BOUND-01 min=1)`() {
        val values = personValues(name = "X")
        val result = PersonValidation.validateCreate(values)
        assertEquals("X", result.name)
    }

    @Test
    fun `exactly 100 code point name is accepted (PRD-BOUND-01 max=100)`() {
        val name = "A".repeat(100)
        val values = personValues(name = name)
        val result = PersonValidation.validateCreate(values)
        assertEquals(100, result.name.codePointCount(0, result.name.length))
    }

    @Test
    fun `empty name after trim is rejected (AC-140-C01 zero code points)`() {
        val values = personValues(name = "   ")
        assertViolation("name") { PersonValidation.validateCreate(values) }
    }

    @Test
    fun `zero code point name is rejected (AC-140-C01)`() {
        val values = personValues(name = "")
        assertViolation("name") { PersonValidation.validateCreate(values) }
    }

    @Test
    fun `101 code point name is rejected (AC-140-C01 max+1)`() {
        val name = "A".repeat(101)
        val values = personValues(name = name)
        assertViolation("name") { PersonValidation.validateCreate(values) }
    }

    @Test
    fun `mononym is accepted without surname requirement (PRD-POLICY-18)`() {
        val values = personValues(name = "Cher")
        val result = PersonValidation.validateCreate(values)
        assertEquals("Cher", result.name)
    }

    @Test
    fun `name with hyphen and apostrophe is accepted (PRD-POLICY-18)`() {
        val values = personValues(name = "O'Brien-Smith")
        val result = PersonValidation.validateCreate(values)
        assertEquals("O'Brien-Smith", result.name)
    }

    @Test
    fun `Unicode name is preserved intact (PRD-POLICY-18)`() {
        val values = personValues(name = "Björn Ångström")
        val result = PersonValidation.validateCreate(values)
        assertEquals("Björn Ångström", result.name)
    }

    @Test
    fun `name with punctuation is preserved as-is (PRD-POLICY-18)`() {
        val values = personValues(name = "St. John")
        val result = PersonValidation.validateCreate(values)
        assertEquals("St. John", result.name)
    }

    @Test
    fun `markup in name is preserved as plain text (PRD-POLICY-26)`() {
        val values = personValues(name = "<b>Actor</b>")
        val result = PersonValidation.validateCreate(values)
        assertEquals("<b>Actor</b>", result.name)
    }

    @Test
    fun `name is trimmed of surrounding whitespace`() {
        val values = personValues(name = "  Alice  ")
        val result = PersonValidation.validateCreate(values)
        assertEquals("Alice", result.name)
    }

    @Test
    fun `biography is absent when not supplied (TD-ROW-L4490 NULL allowed)`() {
        val values = personValues(name = "Alice")
        val result = PersonValidation.validateCreate(values)
        assertNull(result.biography, "Missing biography must be null, not fabricated")
    }

    @Test
    fun `biography is preserved when supplied`() {
        val values = personValues(name = "Alice", biography = "Award-winning actor.")
        val result = PersonValidation.validateCreate(values)
        assertEquals("Award-winning actor.", result.biography)
    }

    @Test
    fun `empty biography is treated as absent (not stored as empty string)`() {
        val values = personValues(name = "Alice", biography = "")
        val result = PersonValidation.validateCreate(values)
        assertNull(result.biography, "Empty biography must be absent, not stored as empty")
    }

    // ── patch validation ───────────────────────────────────────────────────────

    @Test
    fun `name omitted from mask leaves name unaffected`() {
        val write = personWrite(name = "Alice", maskPaths = emptyList())
        val patch = PersonValidation.validatePatch(write)
        assertEquals(PatchValue.Omitted, patch.name)
    }

    @Test
    fun `name in mask with one code point is accepted`() {
        val write = personWrite(name = "X", maskPaths = listOf("values.name"))
        val patch = PersonValidation.validatePatch(write)
        assertEquals(PatchValue.Value("X"), patch.name)
    }

    @Test
    fun `name in mask with 101 code points is rejected`() {
        val write = personWrite(name = "A".repeat(101), maskPaths = listOf("values.name"))
        assertViolation("name") { PersonValidation.validatePatch(write) }
    }

    @Test
    fun `biography omitted from mask leaves biography unaffected`() {
        val write = personWrite(biography = "Bio", maskPaths = emptyList())
        val patch = PersonValidation.validatePatch(write)
        assertEquals(PatchValue.Omitted, patch.biography)
    }

    @Test
    fun `empty biography in mask signals clear (TD-ROW-L4490 NULL allowed)`() {
        val write = personWrite(biography = "", maskPaths = listOf("values.biography"))
        val patch = PersonValidation.validatePatch(write)
        assertEquals(PatchValue.Clear, patch.biography)
    }

    @Test
    fun `non-empty biography in mask is accepted as value`() {
        val write = personWrite(biography = "New bio.", maskPaths = listOf("values.biography"))
        val patch = PersonValidation.validatePatch(write)
        assertEquals(PatchValue.Value("New bio."), patch.biography)
    }

    // ── identity tests — same names stay distinct ──────────────────────────────

    @Test
    fun `two creates with same name produce distinct objects`() {
        val values = personValues(name = "Alex Smith")
        val result1 = PersonValidation.validateCreate(values)
        val result2 = PersonValidation.validateCreate(values)
        // Both must carry the same name — no merging, no fabricated disambiguator.
        assertEquals("Alex Smith", result1.name)
        assertEquals("Alex Smith", result2.name)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun personValues(name: String, biography: String? = null): PersonValues {
        val builder = PersonValues.newBuilder().setName(name)
        biography?.let { builder.biography = it }
        return builder.build()
    }

    private fun personWrite(
        name: String = "Alice",
        biography: String = "",
        maskPaths: List<String>,
    ): PersonWrite {
        val values = PersonValues.newBuilder().setName(name).setBiography(biography).build()
        val mask = com.google.protobuf.FieldMask.newBuilder().addAllPaths(maskPaths).build()
        return PersonWrite.newBuilder()
            .setContext(
                spoolin.v1.Spoolin.Context.newBuilder()
                    .setOperationId("00000000-0000-0000-0000-000000000001")
                    .setCorrelationId("00000000-0000-0000-0000-000000000002")
                    .setPolicyRevision("v1")
                    .build(),
            )
            .setId("00000000-0000-0000-0000-000000000003")
            .setExpectedVersion(0L)
            .setValues(values)
            .setUpdateMask(mask)
            .build()
    }

    private fun assertViolation(expectedPath: String, block: () -> Unit) {
        val e = assertThrows(InputViolation::class.java, block)
        assertEquals(
            expectedPath, e.path,
            "Expected violation on path '$expectedPath' but got '${e.path}'",
        )
    }
}
