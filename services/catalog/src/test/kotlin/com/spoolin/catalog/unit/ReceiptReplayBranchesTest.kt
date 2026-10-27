package com.spoolin.catalog.unit

import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.ReceiptHidden
import com.spoolin.support.receipts.SafeResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReceiptReplayBranchesTest {
    private val intent = OperationIntent(unitOperation, "account:$unitActor", "WriteMovie", "policy", emptyList())

    @Test
    fun `completed receipt replay never reruns mutation or deferred preparation`() {
        val row = receiptRow(intent, "SUCCEEDED", """{"resultId":"$unitTarget","version":"8"}""")
        val db = JdbcRows(listOf(mapOf("lock" to 1)), listOf(row), listOf(row))
        val owner = ownerReceipts(db)
        val result = owner.execute(intent, unitCorrelation, true) { error("Completed receipt must not mutate") }
        assertEquals(8L, result.result?.version)
        val deferred = owner.defer(intent, unitCorrelation, true) { error("Completed receipt must not prepare") }
        assertEquals(unitTarget, deferred.result?.resultId)
    }

    @Test
    fun `receipt actor hash and record type mismatches remain hidden or conflict`() {
        val pending = receiptRow(intent)
        for (deferred in listOf(false, true)) {
            for ((row, type) in listOf(
                (pending + ("actor_scope" to "other")) to ReceiptHidden::class.java,
                (pending + ("request_hash" to ByteArray(32))) to ReceiptConflict::class.java,
                (pending + ("receipt_type" to "INTERNAL")) to ReceiptHidden::class.java,
            )) {
                val replies = if (deferred) arrayOf(listOf(row)) else arrayOf(listOf(mapOf("lock" to 1)), listOf(row))
                val db = JdbcRows(*replies)
                assertThrows(type) {
                    if (deferred) {
                        ownerReceipts(db).defer(intent, unitCorrelation, true) { error("Rejected intent") }
                    } else {
                        ownerReceipts(db).execute(intent, unitCorrelation, true) { SafeResult(unitTarget) }
                    }
                }
            }
        }
    }
}
