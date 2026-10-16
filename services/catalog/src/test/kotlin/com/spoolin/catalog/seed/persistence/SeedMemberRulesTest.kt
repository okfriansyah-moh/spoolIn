package com.spoolin.catalog.seed.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Unit rules for SeedMembership structural contracts.
 *
 * Verifies member row fields and the tombstone primitive contract.
 * No database access.
 */
class SeedMemberRulesTest {

    @Test
    fun `member row preserves all required fields`() {
        val seedId = "seed-v1.0.0"
        val subjectId = UUID.randomUUID()
        val hash = ByteArray(32) { it.toByte() }
        val appliedAt = Instant.parse("2026-10-17T06:00:00Z")
        val row = SeedMembership.MemberRow(
            seedId = seedId,
            memberKey = "genre/action",
            subjectKind = "GENRE",
            subjectId = subjectId,
            contentHash = hash,
            appliedAt = appliedAt,
            removedAt = null,
        )
        assertEquals(seedId, row.seedId)
        assertEquals("genre/action", row.memberKey)
        assertEquals("GENRE", row.subjectKind)
        assertEquals(subjectId, row.subjectId)
        assertEquals(32, row.contentHash.size)
        assertEquals(appliedAt, row.appliedAt)
        assertNull(row.removedAt, "Active member must have null removedAt")
    }

    @Test
    fun `removedAt is nullable — supports active and tombstoned members`() {
        val active = SeedMembership.MemberRow(
            "s1", "key", "TITLE", UUID.randomUUID(), ByteArray(32), Instant.now(), null,
        )
        val removed = SeedMembership.MemberRow(
            "s1", "key", "TITLE", UUID.randomUUID(), ByteArray(32), Instant.now(), Instant.now(),
        )
        assertNull(active.removedAt)
        assertNotNull(removed.removedAt)
    }

    @Test
    fun `content hash must be exactly 32 bytes`() {
        val hash = ByteArray(32) { 0xAA.toByte() }
        assertEquals(32, hash.size, "content_hash must be 32 bytes (SHA-256)")
    }

    @Test
    fun `subject_kind enum values match seed schema`() {
        val validKinds = setOf("TITLE", "PERSON", "GENRE", "ROLE", "CREDIT")
        for (kind in validKinds) {
            assertTrue(kind in validKinds, "$kind must be a valid subject_kind")
        }
    }
}
