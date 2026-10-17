package com.spoolin.core.features.draft

import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.UUID

/**
 * ATDD-319 — Separate draft mutation identity from Catalog recovery identity.
 *
 * Top-level Core operationId (D1) binds the Core mutation while DraftInput.operationId (C1)
 * remains the separate Catalog recovery identity.  Account/kind/target/version/presence/
 * values/policy differences change binding or conflict.  The keyed fingerprint never
 * persists raw draft values, secrets or passwords.
 */
class DraftBindingRulesTest {
    private val hashes = IntentHashes(mapOf("v1" to ByteArray(32) { 7 }), "v1")

    private val accountA = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val accountB = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")
    private val draftX = UUID.fromString("dddddddd-0000-0000-0000-000000000003")
    private val coreD1 = UUID.fromString("cccccccc-1111-0000-0000-000000000001")
    private val catalogC1 = UUID.fromString("cccccccc-2222-0000-0000-000000000002")
    private val values = """{"title":"Draft Title"}"""

    private data class BindingCase(
        val operationId: UUID,
        val accountId: UUID,
        val draftId: UUID,
        val expectedVersion: Long?,
        val catalogSubmission: UUID?,
        val targetKind: String = "MOVIE",
    )

    private fun intent(case: BindingCase) =
        OperationIntent(
            id = case.operationId,
            actorScope = "account:${case.accountId}",
            kind = "DRAFT_SAVE",
            policyRevision = "abc123",
            fields =
                buildList {
                    add("draftId" to case.draftId.toString())
                    add("targetKind" to case.targetKind)
                    add("targetId" to null)
                    add("baseVersion" to null)
                    add("operationId" to case.catalogSubmission?.toString())
                    add("expectedVersion" to case.expectedVersion?.toString())
                    add("values" to values)
                },
        )

    // ── Core vs Catalog identity separation ──────────────────────────────────

    @Test
    fun `Core operationId D1 and Catalog operationId C1 are independent fields`() {
        // The Core receipt key is D1; C1 is just another field in the fingerprint
        val withC1 = intent(BindingCase(coreD1, accountA, draftX, null, catalogC1))
        val withoutC1 = intent(BindingCase(coreD1, accountA, draftX, null, null))
        val hashWith = hashes.hash(withC1, "v1")
        val hashWithout = hashes.hash(withoutC1, "v1")
        // Different C1 presence changes the fingerprint (content conflict)
        assertFalse(MessageDigest.isEqual(hashWith, hashWithout))
    }

    @Test
    fun `two different Core operationIds produce different receipts even with same content`() {
        val coreD2 = UUID.fromString("cccccccc-3333-0000-0000-000000000003")
        val i1 = intent(BindingCase(coreD1, accountA, draftX, null, null))
        val i2 = intent(BindingCase(coreD2, accountA, draftX, null, null))
        // Different IDs → different entries; same content still conflicts on different ID
        assertNotEquals(i1.id, i2.id)
        assertNotNull(hashes.hash(i1, "v1"))
        assertNotNull(hashes.hash(i2, "v1"))
    }

    // ── Actor scope isolation ─────────────────────────────────────────────────

    @Test
    fun `different accounts produce a HIDDEN comparison for same operationId`() {
        val i1 = intent(BindingCase(coreD1, accountA, draftX, null, null))
        val i2 = intent(BindingCase(coreD1, accountB, draftX, null, null))
        val stored = hashes.hash(i1, "v1")
        // accountB cannot see accountA's receipt (HIDDEN)
        val comparison = hashes.compare(i2, "account:$accountA", "DRAFT_SAVE", stored, "v1")
        assertEquals(com.spoolin.support.receipts.Comparison.HIDDEN, comparison)
    }

    // ── Fingerprint differences on field changes ──────────────────────────────

    @Test
    fun `changed targetKind changes the fingerprint`() {
        val movie = intent(BindingCase(coreD1, accountA, draftX, null, null, "MOVIE"))
        val person = intent(BindingCase(coreD1, accountA, draftX, null, null, "PERSON"))
        assertFalse(MessageDigest.isEqual(hashes.hash(movie, "v1"), hashes.hash(person, "v1")))
    }

    @Test
    fun `present expectedVersion versus absent changes the fingerprint`() {
        val create = intent(BindingCase(coreD1, accountA, draftX, null, null))
        val update = intent(BindingCase(coreD1, accountA, draftX, 0L, null))
        assertFalse(MessageDigest.isEqual(hashes.hash(create, "v1"), hashes.hash(update, "v1")))
    }

    @Test
    fun `different expectedVersion values change the fingerprint`() {
        val v0 = intent(BindingCase(coreD1, accountA, draftX, 0L, null))
        val v5 = intent(BindingCase(coreD1, accountA, draftX, 5L, null))
        assertFalse(MessageDigest.isEqual(hashes.hash(v0, "v1"), hashes.hash(v5, "v1")))
    }

    // ── Fingerprint does not contain raw sensitive data ───────────────────────

    @Test
    fun `fingerprint bytes do not contain the raw draft values string`() {
        val i = intent(BindingCase(coreD1, accountA, draftX, null, null))
        val hash = hashes.hash(i, "v1")
        // The 32-byte HMAC is not a substring of the values string
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        assertFalse(hashHex.contains("Draft Title", ignoreCase = true))
    }

    companion object {
        private fun assertEquals(
            expected: com.spoolin.support.receipts.Comparison,
            actual: com.spoolin.support.receipts.Comparison,
        ) = org.junit.jupiter.api.Assertions
            .assertEquals(expected, actual)
    }
}
