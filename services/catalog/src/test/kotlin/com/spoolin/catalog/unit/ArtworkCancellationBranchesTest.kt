package com.spoolin.catalog.unit

import com.spoolin.catalog.features.artwork.ArtworkRpc
import com.spoolin.support.receipts.OperationIntent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class ArtworkCancellationBranchesTest {
    @Test
    fun `cancelling preparation releases hold while settled attachment is retained`() = withMaintainer {
        for (state in listOf("PREPARING", "ATTACHED")) {
            val intent = OperationIntent(unitOperation, "account:$unitActor", "CancelArtwork", "", listOf("uploadOperationId" to unitTarget.toString()))
            val upload = OperationIntent(unitTarget, "account:$unitActor", "BeginArtwork", "policy", emptyList())
            val cancelled = state == "PREPARING"
            val final = receiptRow(intent, "SUCCEEDED", """{"resultId":"$unitTarget","discarded":$cancelled}""")
            val original = if (cancelled) receiptRow(upload, "FAILED", error = "CANCELLED")
                else receiptRow(upload, "SUCCEEDED", """{"resultId":"$unitAsset","version":"8"}""")
            val replies = mutableListOf(
                listOf(mapOf("lock" to 1)), listOf(receiptRow(intent)), listOf(mapOf("id" to unitTarget)),
                listOf(mapOf("state" to state, "title_id" to unitAsset)),
            )
            if (cancelled) replies.add(listOf(mapOf("version" to 8L)))
            replies.add(listOf(mapOf("correlation_id" to unitCorrelation)))
            replies.add(listOf(final))
            replies.add(listOf(original))
            val db = JdbcRows(*replies.toTypedArray())
            val result = RpcResult<Spoolin.ArtworkCancelOutcome>()
            ArtworkRpc(db.source, ownerReceipts(db)).cancelArtwork(
                Spoolin.CancelArtworkRequest.newBuilder().setContext(Spoolin.Context.newBuilder()
                    .setOperationId(unitOperation.toString()).setCorrelationId(unitCorrelation.toString()))
                    .setUploadOperationId(unitTarget.toString()).build(), result,
            )
            assertEquals(cancelled, result.value().cancelled)
            assertEquals(unitTarget.toString(), result.value().uploadOperation.operationId)
            if (cancelled) {
                assertEquals("CANCELLED", result.value().uploadOperation.problem.code)
                assertTrue(db.writes.any { "ARTWORK_RELEASE" in it })
            } else {
                assertEquals("SUCCEEDED", result.value().uploadOperation.state)
                assertEquals(8L, result.value().uploadOperation.version)
            }
        }
    }
}
