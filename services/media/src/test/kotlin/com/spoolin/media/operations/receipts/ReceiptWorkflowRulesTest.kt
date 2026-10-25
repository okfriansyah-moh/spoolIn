package com.spoolin.media.operations.receipts

import com.spoolin.media.fixtures.*
import com.spoolin.support.receipts.*
import io.grpc.Status
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.Outcome
import java.sql.Timestamp
import java.time.Clock
import java.time.ZoneOffset

class ReceiptWorkflowRulesTest {
    private val hashes = IntentHashes(mapOf("key" to ByteArray(32) { 7 }), "key")
    private val intent = OperationIntent(operationId, "account:$actorId", "UPLOAD", "p1", listOf("asset" to assetId.toString()))
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private fun record(state: String = "PENDING", expires: java.time.Instant = instant.plusSeconds(60)): Map<String, Any?> = mapOf(
        "id" to operationId, "receipt_type" to "REQUEST", "actor_scope" to intent.actorScope, "kind" to intent.kind,
        "request_hash" to hashes.hash(intent, null), "state" to state, "correlation_id" to operationId,
        "created_at" to Timestamp.from(instant.minusSeconds(10)), "expires_at" to Timestamp.from(expires),
        "result" to if (state == "SUCCEEDED") "{\"resultId\":\"$assetId\",\"version\":\"4\"}" else null,
    )

    @Test fun `committed owner result replays without repeating a mutation`() {
        val db = JdbcBoundary().rows(record()).rows(record("SUCCEEDED"))
        val result = OwnerReceipts(db.source, hashes, clock).execute(intent, operationId, false,
            listOf(WorkRequest("process", "MEDIA_PROCESS", assetId, instant.plusSeconds(60)))) { SafeResult(assetId, 4) }
        assertEquals(ReceiptState.SUCCEEDED, result.state)
        assertEquals(assetId, result.result!!.resultId)
        assertEquals(4L, result.result!!.version)
        assertEquals(1, db.commits)
        val retry = JdbcBoundary().rows(record("SUCCEEDED")).apply { updates.add(0) }
        assertEquals(result, OwnerReceipts(retry.source, hashes, clock).execute(intent, operationId, false) { error("must not mutate") })
    }

    @Test fun `actor and request conflicts roll back before domain mutation`() {
        for ((row, type) in listOf(record() + ("actor_scope" to "account:other") to ReceiptHidden::class.java,
            record() + ("request_hash" to byteArrayOf(0)) to ReceiptConflict::class.java,
            record() + ("receipt_type" to "WORK") to ReceiptHidden::class.java)) {
            val db = JdbcBoundary().rows(row)
            assertThrows(type) { OwnerReceipts(db.source, hashes, clock).execute(intent, operationId, false) { error("must not mutate") } }
            assertEquals(0, db.commits)
            assertEquals(1, db.rollbacks)
        }
    }

    @Test fun `stored public projections reject undeclared fields malformed values and private problems`() {
        assertNull(SafeReceiptProjection.read(null))
        assertNull(SafeReceiptProjection.errorCode(null))
        assertEquals("FIELD_INVALID", SafeReceiptProjection.errorCode("FIELD_INVALID"))
        assertEquals(SafeResult(assetId, null, true), SafeReceiptProjection.read("{\"resultId\":\"$assetId\",\"discarded\":true}"))
        for (json in listOf("[]", "{}", "{\"resultId\":\"$assetId\",\"private\":true}",
            "{\"resultId\":\"$assetId\",\"version\":4}", "{\"resultId\":\"invalid\"}")) {
            assertThrows(IllegalStateException::class.java) { SafeReceiptProjection.read(json) }
        }
        assertThrows(IllegalStateException::class.java) { SafeReceiptProjection.errorCode("private rejection text") }
    }

    @Test fun `expiry removes terminal results but preserves pending receipt visibility`() {
        val expired = requireNotNull(OwnerReceiptReads(JdbcBoundary().rows(record("SUCCEEDED", instant)).source, clock).find(operationId, intent.actorScope))
        assertTrue(expired.resultExpired)
        assertNull(expired.result)
        assertEquals("RESULT_EXPIRED", expired.errorCode)
        val pending = requireNotNull(OwnerReceiptReads(JdbcBoundary().rows(record("PENDING", instant)).source, clock).find(operationId, intent.actorScope))
        assertFalse(pending.resultExpired)
        assertNull(OwnerReceipts(JdbcBoundary().source, hashes, clock).find(operationId, intent.actorScope))
    }

    @Test fun `receipt RPC exposes result identity version and declared problems`() {
        val outcome = ReceiptOutcome(operationId, ReceiptState.SUCCEEDED, SafeResult(assetId, 4), "FIELD_INVALID", operationId, instant)
        val response = Response<Outcome>()
        ReceiptRpc(ReceiptAuthority { intent.actorScope }) { _, _ -> outcome }.getOperation(Id.newBuilder().setId(operationId.toString()).build(), response)
        assertTrue(response.completed)
        assertEquals(assetId.toString(), response.values.single().resultId)
        assertEquals(4L, response.values.single().version)
        assertEquals("FIELD_INVALID", response.values.single().problem.code)
        val anonymous = Response<Outcome>()
        ReceiptRpc(ReceiptAuthority { null }) { _, _ -> outcome }.getOperation(Id.getDefaultInstance(), anonymous)
        assertEquals(Status.Code.PERMISSION_DENIED, anonymous.code)
        val invalid = Response<Outcome>()
        ReceiptRpc(ReceiptAuthority { intent.actorScope }) { _, _ -> outcome }.getOperation(Id.getDefaultInstance(), invalid)
        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.code)
        val missing = Response<Outcome>()
        ReceiptRpc(ReceiptAuthority { intent.actorScope }) { _, _ -> null }.getOperation(Id.newBuilder().setId(operationId.toString()).build(), missing)
        assertEquals(Status.Code.NOT_FOUND, missing.code)
    }
}
