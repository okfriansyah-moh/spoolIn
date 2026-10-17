package com.spoolin.catalog.operations.audit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ATDD-298: AuditDiff produces safe, allowlisted, size-bounded change records.
 *
 * Verifies:
 * - Within-bound changes preserve their values inline.
 * - Oversized changes produce summarised form with byte lengths + digests + valueOmitted=true.
 * - Forbidden private fields never appear in the output.
 * - Source domain inputs are never mutated.
 * - An empty diff produces an empty changes array.
 */
class AuditDiffRulesTest {

    @Test
    fun `unchanged fields produce no change entries`() {
        val before = mapOf("displayTitle" to "Movie A", "synopsis" to "desc")
        val after = mapOf("displayTitle" to "Movie A", "synopsis" to "desc")
        val result = AuditDiff.movie(before, after)
        assertTrue(result.contains("\"changes\":[]"), "No changes expected for identical snapshots")
        assertFalse(result.contains("valueOmitted"), "No omission for small payload")
    }

    @Test
    fun `changed fields appear inline when within size limit`() {
        val before = mapOf("displayTitle" to "Old Title")
        val after = mapOf("displayTitle" to "New Title")
        val result = AuditDiff.movie(before, after)
        assertTrue(result.contains("\"displayTitle\""), "Path must appear")
        assertTrue(result.contains("Old Title"), "Before value must be inline")
        assertTrue(result.contains("New Title"), "After value must be inline")
        assertTrue(result.contains("\"summarised\":false"))
    }

    @Test
    fun `private fields are excluded from changes output`() {
        val before = mapOf("displayTitle" to "X", "password" to "secret", "email" to "x@example.com")
        val after = mapOf("displayTitle" to "Y", "password" to "changed", "email" to "y@example.com")
        val result = AuditDiff.movie(before, after)
        assertFalse(result.contains("password"), "Passwords must not appear in audit diff")
        assertFalse(result.contains("email"), "Email must not appear in audit diff")
        assertTrue(result.contains("displayTitle"), "Allowlisted field must appear")
    }

    @Test
    fun `oversized diff triggers summarised form with digests and valueOmitted`() {
        // Build a change large enough to exceed 65536 bytes.
        val longBefore = "A".repeat(40000)
        val longAfter = "B".repeat(40000)
        val before = mapOf("synopsis" to longBefore)
        val after = mapOf("synopsis" to longAfter)
        val result = AuditDiff.movie(before, after)
        assertTrue(result.contains("\"summarised\":true"), "Must use summarised form")
        assertTrue(result.contains("valueOmitted"), "valueOmitted must be present")
        assertTrue(result.contains("byteLength"), "Byte length must appear")
        assertTrue(result.contains("beforeDigest"), "Before digest must appear")
        assertTrue(result.contains("afterDigest"), "After digest must appear")
        assertFalse(result.contains(longBefore), "Raw before value must not appear")
        assertFalse(result.contains(longAfter), "Raw after value must not appear")
        assertTrue(result.toByteArray(Charsets.UTF_8).size <= 65536, "Summary must fit 65536 bytes")
    }

    @Test
    fun `source domain inputs are not mutated`() {
        val before = mapOf("displayTitle" to "Original")
        val after = mapOf("displayTitle" to "Updated")
        val beforeCopy = before.toMap()
        val afterCopy = after.toMap()
        AuditDiff.movie(before, after)
        assertEquals(beforeCopy, before, "Before map must not be mutated")
        assertEquals(afterCopy, after, "After map must not be mutated")
    }

    @Test
    fun `new field added appears as create entry with null before`() {
        val before = emptyMap<String, Any?>()
        val after = mapOf("displayTitle" to "Brand New")
        val result = AuditDiff.movie(before, after)
        assertTrue(result.contains("displayTitle"))
        assertTrue(result.contains("\"before\":null"))
        assertTrue(result.contains("Brand New"))
    }

    @Test
    fun `field removed appears as delete entry with null after`() {
        val before = mapOf("originalTitle" to "Original Title")
        val after = emptyMap<String, Any?>()
        val result = AuditDiff.movie(before, after)
        assertTrue(result.contains("originalTitle"))
        assertTrue(result.contains("\"after\":null"))
    }

    @Test
    fun `result is valid JSON-like object containing changes key`() {
        val result = AuditDiff.movie(mapOf("displayTitle" to "A"), mapOf("displayTitle" to "B"))
        assertTrue(result.startsWith("{"), "Result must be a JSON object")
        assertTrue(result.endsWith("}"))
        assertTrue(result.contains("\"changes\":["))
    }

    @Test
    fun `encode value handles list and set types`() {
        val listVal = AuditDiff.encodeValue(listOf("a", "b"))
        assertEquals("[\"a\",\"b\"]", listVal)
        // Sets are sorted before encoding
        val setVal = AuditDiff.encodeValue(setOf("z", "a"))
        assertEquals("[\"a\",\"z\"]", setVal)
    }

    @Test
    fun `encodeValue handles null number boolean and string`() {
        assertEquals(null, AuditDiff.encodeValue(null))
        assertEquals("42", AuditDiff.encodeValue(42))
        assertEquals("true", AuditDiff.encodeValue(true))
        assertEquals("\"hello\"", AuditDiff.encodeValue("hello"))
    }
}
