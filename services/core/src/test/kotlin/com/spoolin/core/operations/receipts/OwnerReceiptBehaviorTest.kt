package com.spoolin.core.operations.receipts

import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.support.receipts.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class OwnerReceiptBehaviorTest {
    private val now = Instant.parse("2026-10-25T10:00:00Z")
    private val intent = OperationIntent(UUID.randomUUID(), "account:test", "SaveDraft", "revision", listOf("title" to "Film"))
    private val hashes = IntentHashes(mapOf("compare" to ByteArray(32) { 1 }), "compare")
    private val result = SafeResult(UUID.randomUUID(), 3)
    private val correlation = UUID.randomUUID()

    @Test
    fun `new operation commits mutation and returns durable safe result`() {
        val db = SessionJdbcFixture(listOf(listOf(row("PENDING")), listOf(row("SUCCEEDED"))))
        var mutations = 0
        val receipt = owner(db).execute(intent, correlation, false) { mutations++; result }
        assertEquals(1, mutations)
        assertEquals(result, receipt.result)
        assertEquals(ReceiptState.SUCCEEDED, receipt.state)
        assertEquals(1, db.commits)
        assertEquals(3, db.updates)
    }

    @Test
    fun `existing receipt replay never executes mutation again`() {
        val db = SessionJdbcFixture(listOf(listOf(row("SUCCEEDED"))))
        val receipt = owner(db).execute(intent, correlation, false) { fail<SafeResult>("Replay invoked mutation") }
        assertEquals(result, receipt.result)
        assertEquals(ReceiptState.SUCCEEDED, receipt.state)
        assertEquals(1, db.updates)
    }

    @Test
    fun `hidden actor conflicting payload and nonrequest receipt reject mutation`() {
        val invalid = listOf(
            row("PENDING") + ("actor_scope" to "account:other"),
            row("PENDING") + ("request_hash" to ByteArray(32)),
            row("PENDING") + ("receipt_type" to "ACCOUNT_INITIALIZATION"),
        )
        for ((index, record) in invalid.withIndex()) {
            val db = SessionJdbcFixture(listOf(listOf(record)))
            val expected = if (index == 1) ReceiptConflict::class.java else ReceiptHidden::class.java
            assertThrows(expected) { owner(db).execute(intent, correlation, false) { fail<SafeResult>("Rejected operation invoked mutation") } }
            assertEquals(0, db.commits)
        }
    }

    @Test
    fun `resolve distinguishes missing replay conflict hidden and expired results`() {
        val db = SessionJdbcFixture(listOf(emptyList(), listOf(row("SUCCEEDED")), listOf(row("SUCCEEDED") + ("expires_at" to Timestamp.from(now)))))
        val owner = owner(db)
        assertNull(owner.resolve(intent))
        assertEquals(result, owner.resolve(intent)!!.result)
        val expired = owner.resolve(intent)!!
        assertTrue(expired.resultExpired)
        assertNull(expired.result)
        assertEquals("RESULT_EXPIRED", expired.errorCode)
        db.enqueue(row("SUCCEEDED") + ("actor_scope" to "other"))
        assertThrows(ReceiptHidden::class.java) { owner.resolve(intent) }
        db.enqueue(row("SUCCEEDED") + ("kind" to "Other"))
        assertThrows(ReceiptConflict::class.java) { owner.resolve(intent) }
    }

    @Test
    fun `private input replay uses retained comparison key and read predicates preserve absence`() {
        val db = SessionJdbcFixture(listOf(listOf(row("PENDING", "compare")), listOf(row("SUCCEEDED", "compare"))))
        assertEquals(result, owner(db).execute(intent, correlation, true) { result }.result)
        db.enqueue(row("SUCCEEDED"))
        assertEquals(result, owner(db).find(intent.id, intent.actorScope)!!.result)
        val reads = OwnerReceiptReads(db)
        db.enqueue()
        assertFalse(reads.anonymous(intent.id, intent.actorScope))
        db.enqueue(emptyMap())
        assertTrue(reads.anonymous(intent.id, intent.actorScope))
        db.enqueue(emptyMap())
        assertTrue(reads.isSignOut(intent.id, intent.actorScope))
        db.enqueue()
        assertFalse(reads.isSignOut(intent.id, intent.actorScope))
    }

    private fun owner(db: SessionJdbcFixture) = OwnerReceipts(db, hashes, Clock.fixed(now, ZoneOffset.UTC))
    private fun row(state: String, key: String? = null): Map<String, Any?> = mapOf(
        "id" to intent.id, "state" to state, "expires_at" to Timestamp.from(now.plusSeconds(3600)),
        "created_at" to Timestamp.from(now.minusSeconds(10)), "result" to if (state == "SUCCEEDED") result.json() else null,
        "error_code" to null, "correlation_id" to correlation, "actor_scope" to intent.actorScope,
        "kind" to intent.kind, "request_hash" to hashes.hash(intent, key), "hash_key_id" to key, "receipt_type" to "REQUEST",
    )
}
