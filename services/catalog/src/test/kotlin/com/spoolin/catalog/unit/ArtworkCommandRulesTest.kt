package com.spoolin.catalog.unit

import com.spoolin.catalog.features.artwork.ArtworkRpc
import com.spoolin.support.receipts.OperationIntent
import io.grpc.Status
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class ArtworkCommandRulesTest {
    private val context = Spoolin.Context.newBuilder().setOperationId(unitOperation.toString())
        .setCorrelationId(unitCorrelation.toString()).setPolicyRevision("policy").build()
    private fun request() = Spoolin.Hold.newBuilder().setContext(context).setHoldId(unitOperation.toString())
        .setTitleId(unitTarget.toString()).setAssetId(unitAsset.toString()).setExpectedVersion(7)
    private fun intent() = OperationIntent(unitOperation, "account:$unitActor", "BeginArtwork", "policy", listOf(
        "titleId" to unitTarget.toString(), "assetId" to unitAsset.toString(), "expectedVersion" to "7",
    ))
    private fun db(vararg domain: List<Map<String, Any?>>, final: Map<String, Any?> = receiptRow(intent())) =
        JdbcRows(listOf(mapOf("lock" to 1)), listOf(receiptRow(intent())), *domain, listOf(final))

    @Test fun `begin artwork reserves pending intent and queues bounded attachment work`() = withMaintainer {
        val db = db(listOf(mapOf("version" to 7L, "status" to "ACTIVE", "attachment_id" to unitAsset)))
        val result = RpcResult<Spoolin.Outcome>()
        ArtworkRpc(db.source, ownerReceipts(db)) { "policy" }.beginArtwork(request().build(), result)
        assertEquals("PENDING", result.value().state)
        assertEquals(unitOperation.toString(), result.value().operationId)
        assertTrue(db.writes.any { "ARTWORK_ATTACH" in it })
        assertTrue(db.writes.flatten().filterIsInstance<String>().any { it.contains("credentialId") && it.contains(unitCredential.toString()) })
    }

    @Test fun `begin artwork rejects missing stale archived title and policy drift`() = withMaintainer {
        val cases = listOf(
            emptyList<Map<String, Any?>>() to null,
            listOf(mapOf("version" to 6L, "status" to "ACTIVE", "attachment_id" to null)) to "STALE_VERSION",
            listOf(mapOf("version" to 7L, "status" to "ARCHIVED", "attachment_id" to null)) to "STALE_VERSION",
        )
        for ((rows, code) in cases) {
            val db = db(rows)
            val result = RpcResult<Spoolin.Outcome>()
            ArtworkRpc(db.source, ownerReceipts(db)) { "policy" }.beginArtwork(request().build(), result)
            assertEquals(code, Status.fromThrowable(result.failure).description)
            assertEquals(1, db.writes.size)
        }
        val changed = db()
        val result = RpcResult<Spoolin.Outcome>()
        ArtworkRpc(changed.source, ownerReceipts(changed)) { "new-policy" }.beginArtwork(request().build(), result)
        assertEquals("POLICY_CHANGED", Status.fromThrowable(result.failure).description)
    }

    @Test fun `begin artwork rejects missing actor mismatched hold malformed ids and negative version`() {
        val source = JdbcRows()
        val denied = RpcResult<Spoolin.Outcome>()
        ArtworkRpc(source.source, ownerReceipts(source)).beginArtwork(request().build(), denied)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(denied.failure).code)
        withMaintainer {
            for (invalid in listOf(request().setHoldId(unitAsset.toString()).build(), request().clearExpectedVersion().build(),
                request().setExpectedVersion(-1).build(), request().setAssetId("bad").build())) {
                val result = RpcResult<Spoolin.Outcome>()
                ArtworkRpc(source.source, ownerReceipts(source)).beginArtwork(invalid, result)
                assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(result.failure).code)
            }
        }
    }

    @Test fun `artwork cancellation missing original returns safe not found outcome`() = withMaintainer {
        val request = Spoolin.CancelArtworkRequest.newBuilder().setContext(context).setUploadOperationId(unitTarget.toString()).build()
        val intent = OperationIntent(unitOperation, "account:$unitActor", "CancelArtwork", "", listOf("uploadOperationId" to unitTarget.toString()))
        val finished = receiptRow(intent, "FAILED", error = "NOT_FOUND")
        val db = JdbcRows(listOf(mapOf("lock" to 1)), listOf(receiptRow(intent)),
            emptyList(), emptyList(), listOf(finished))
        val result = RpcResult<Spoolin.ArtworkCancelOutcome>()
        ArtworkRpc(db.source, ownerReceipts(db)).cancelArtwork(request, result)
        assertEquals("NOT_FOUND", result.value().operation.problem.code)
        assertFalse(result.value().cancelled)
        assertFalse(result.value().hasUploadOperation())
    }

    @Test fun `artwork read RPCs convert invalid identities and missing attachments to transport errors`() {
        val db = JdbcRows(emptyList(), emptyList())
        val rpc = ArtworkRpc(db.source, ownerReceipts(db))
        val invalid = RpcResult<Spoolin.Attachment>()
        rpc.getAttachment(Spoolin.Id.newBuilder().setId("bad").build(), invalid)
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(invalid.failure).code)
        val absent = RpcResult<Spoolin.Attachment>()
        rpc.getAttachment(Spoolin.Id.newBuilder().setId(unitTarget.toString()).build(), absent)
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(absent.failure).code)
        val access = RpcResult<Spoolin.ArtworkGrant>()
        rpc.resolveArtworkAccess(Spoolin.ArtworkAccessRequest.newBuilder().setManagement(true).build(), access)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(access.failure).code)
    }

    @Test fun `release queue requires a surviving catalog version and embeds it in work`() {
        val absent = JdbcRows(emptyList())
        assertEquals("REFERENCE_VERSION_UNAVAILABLE", assertThrows(io.grpc.StatusRuntimeException::class.java) {
            absent.source.connection.use { ArtworkRpc.enqueueRelease(it, unitOperation, unitTarget, unitCorrelation) }
        }.status.description)
        val db = JdbcRows(listOf(mapOf("version" to 9L)))
        db.source.connection.use { ArtworkRpc.enqueueRelease(it, unitOperation, unitTarget, unitCorrelation) }
        assertTrue(db.writes.any { "ARTWORK_RELEASE" in it })
        assertTrue(db.writes.flatten().filterIsInstance<String>().any { it.contains("catalogVersion\":9") })
    }
}
