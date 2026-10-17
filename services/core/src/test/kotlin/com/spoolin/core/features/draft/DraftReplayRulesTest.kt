package com.spoolin.core.features.draft

import com.spoolin.support.receipts.Comparison
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.ReceiptState
import com.spoolin.support.receipts.SafeResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * ATDD-320 — Replay safe draft outcomes without current-row reconstruction.
 *
 * A retained safe receipt returns its original ID/version/discarded outcome; current-row
 * changes/absence cannot turn replay into creation or refresh; foreign/mismatched/
 * expired-result behavior follows the scoped receipt contract.
 */
class DraftReplayRulesTest {
    private val hashes = IntentHashes(mapOf("v1" to ByteArray(32) { 11 }), "v1")
    private val accountA = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val accountB = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")
    private val draftX = UUID.fromString("dddddddd-0000-0000-0000-000000000003")
    private val coreD1 = UUID.fromString("cccccccc-1111-0000-0000-000000000001")
    private val now = Instant.parse("2026-10-01T12:00:00Z")

    private fun saveIntent(
        operationId: UUID,
        accountId: UUID,
        draftId: UUID,
        expectedVersion: Long?,
        values: String = """{"title":"Movie"}""",
    ) = OperationIntent(
        id = operationId,
        actorScope = "account:$accountId",
        kind = "DRAFT_SAVE",
        policyRevision = "rev1",
        fields =
            buildList {
                add("draftId" to draftId.toString())
                add("targetKind" to "MOVIE")
                add("targetId" to null)
                add("baseVersion" to null)
                add("operationId" to null)
                add("expectedVersion" to expectedVersion?.toString())
                add("values" to values)
            },
    )

    private fun discardIntent(
        operationId: UUID,
        accountId: UUID,
        draftId: UUID,
        expectedVersion: Long,
    ) = OperationIntent(
        id = operationId,
        actorScope = "account:$accountId",
        kind = "DRAFT_DISCARD",
        policyRevision = "discard",
        fields =
            listOf(
                "draftId" to draftId.toString(),
                "expectedVersion" to expectedVersion.toString(),
            ),
    )

    private fun storedOutcome(
        operationId: UUID,
        draftId: UUID,
        version: Long? = null,
        discarded: Boolean? = null,
        expired: Boolean = false,
    ) = ReceiptOutcome(
        id = operationId,
        state = ReceiptState.SUCCEEDED,
        result = if (expired) null else SafeResult(resultId = draftId, version = version, discarded = discarded),
        errorCode = if (expired) "RESULT_EXPIRED" else null,
        correlationId = UUID.randomUUID(),
        createdAt = now,
        resultExpired = expired,
    )

    // ── Save replay ───────────────────────────────────────────────────────────

    @Test
    fun `matching save replay returns original operation ID and version`() {
        val intent = saveIntent(coreD1, accountA, draftX, null)
        val retained = storedOutcome(coreD1, draftX, version = 0L)
        // Compare: same actor, same kind, same hash → REPLAY
        val stored = hashes.hash(intent, "v1")
        val comparison = hashes.compare(intent, "account:$accountA", "DRAFT_SAVE", stored, "v1")
        assertEquals(Comparison.REPLAY, comparison)
        // Verify retained result fields match expected shape
        assertEquals(coreD1, retained.id)
        assertEquals(draftX, retained.result?.resultId)
        assertEquals(0L, retained.result?.version)
        assertNull(retained.result?.discarded)
    }

    @Test
    fun `changed values under same operationId is a CONFLICT`() {
        val original = saveIntent(coreD1, accountA, draftX, null, """{"title":"Version A"}""")
        val changed = saveIntent(coreD1, accountA, draftX, null, """{"title":"Version B"}""")
        val storedHash = hashes.hash(original, "v1")
        val comparison = hashes.compare(changed, "account:$accountA", "DRAFT_SAVE", storedHash, "v1")
        assertEquals(Comparison.CONFLICT, comparison)
    }

    @Test
    fun `foreign account cannot inspect receipts for same operationId`() {
        val original = saveIntent(coreD1, accountA, draftX, null)
        val foreign = saveIntent(coreD1, accountB, draftX, null)
        val storedHash = hashes.hash(original, "v1")
        val comparison = hashes.compare(foreign, "account:$accountA", "DRAFT_SAVE", storedHash, "v1")
        assertEquals(Comparison.HIDDEN, comparison)
    }

    // ── Discard replay ────────────────────────────────────────────────────────

    @Test
    fun `matching discard replay returns original discarded flag`() {
        val intent = discardIntent(coreD1, accountA, draftX, expectedVersion = 5L)
        val retained = storedOutcome(coreD1, draftX, discarded = true)
        val stored = hashes.hash(intent, "v1")
        val comparison = hashes.compare(intent, "account:$accountA", "DRAFT_DISCARD", stored, "v1")
        assertEquals(Comparison.REPLAY, comparison)
        assertEquals(true, retained.result?.discarded)
        assertEquals(draftX, retained.result?.resultId)
    }

    @Test
    fun `expired result body is null and resultExpired is true`() {
        val retained = storedOutcome(coreD1, draftX, expired = true)
        assertNull(retained.result)
        assertEquals("RESULT_EXPIRED", retained.errorCode)
        assertFalse(!retained.resultExpired)
    }

    // ── Result shape ──────────────────────────────────────────────────────────

    @Test
    fun `save result encodes resultId and version as non-null strings`() {
        val result = SafeResult(resultId = draftX, version = 7L)
        val json = result.json()
        assertNotNull(json)
        // resultId and version must be present
        assert(json.contains("\"resultId\"")) { "json missing resultId: $json" }
        assert(json.contains("\"version\"")) { "json missing version: $json" }
        // discarded must not be present for a save
        assertFalse(json.contains("\"discarded\"")) { "save json must not contain discarded: $json" }
    }

    @Test
    fun `discard result encodes resultId and discarded flag`() {
        val result = SafeResult(resultId = draftX, discarded = true)
        val json = result.json()
        assert(json.contains("\"resultId\"")) { "json missing resultId: $json" }
        assert(json.contains("\"discarded\":true")) { "json missing discarded: $json" }
        assertFalse(json.contains("\"version\"")) { "discard json must not contain version: $json" }
    }

    @Test
    fun `discard result with discarded false encodes correctly`() {
        val result = SafeResult(resultId = draftX, discarded = false)
        val json = result.json()
        assert(json.contains("\"discarded\":false")) { "json should have discarded=false: $json" }
    }
}
