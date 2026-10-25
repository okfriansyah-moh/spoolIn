package com.spoolin.catalog.unit

import com.spoolin.catalog.operations.receipts.OwnerReceiptReads
import com.spoolin.catalog.operations.receipts.ReceiptRpc
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptAuthority
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class ReceiptTransportBranchesTest {
    private fun rpc(db: JdbcRows): ReceiptRpc {
        val reads = OwnerReceiptReads(db.source, unitClock)
        return ReceiptRpc(ReceiptAuthority { ActorRpc.current()?.let { "account:${it.accountId}" } }, reads::find)
    }

    @Test
    fun `receipt transport requires actor valid identity and visible row`() {
        val denied = RpcResult<Spoolin.Outcome>()
        rpc(JdbcRows()).getOperation(Spoolin.Id.newBuilder().setId(unitOperation.toString()).build(), denied)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(denied.failure).code)
        withMaintainer {
            val invalid = RpcResult<Spoolin.Outcome>()
            rpc(JdbcRows()).getOperation(Spoolin.Id.newBuilder().setId("bad").build(), invalid)
            assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(invalid.failure).code)
            val absent = RpcResult<Spoolin.Outcome>()
            rpc(JdbcRows(emptyList())).getOperation(Spoolin.Id.newBuilder().setId(unitOperation.toString()).build(), absent)
            assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(absent.failure).code)
        }
    }

    @Test
    fun `receipt transport projects pending completed failed and expired results independently`() = withMaintainer {
        val intent = OperationIntent(unitOperation, "account:$unitActor", "WriteMovie", "policy", emptyList())
        val rows = listOf(receiptRow(intent),
            receiptRow(intent, "SUCCEEDED", """{"resultId":"$unitTarget"}"""),
            receiptRow(intent, "SUCCEEDED", """{"resultId":"$unitTarget","version":"8"}"""),
            receiptRow(intent, "FAILED", error = "CANCELLED"),
            receiptRow(intent, "SUCCEEDED", """{"resultId":"$unitTarget","version":"8"}""") +
                ("expires_at" to java.sql.Timestamp.from(unitNow.minusSeconds(1))))
        for ((index, row) in rows.withIndex()) {
            val result = RpcResult<Spoolin.Outcome>()
            rpc(JdbcRows(listOf(row))).getOperation(Spoolin.Id.newBuilder().setId(unitOperation.toString()).build(), result)
            val value = result.value()
            assertEquals(unitOperation.toString(), value.operationId)
            when (index) {
                0 -> { assertEquals("PENDING", value.state); assertFalse(value.hasResultId()) }
                1 -> { assertEquals(unitTarget.toString(), value.resultId); assertFalse(value.hasVersion()) }
                2 -> assertEquals(8L, value.version)
                3 -> assertEquals("CANCELLED", value.problem.code)
                4 -> { assertEquals(true, value.resultExpired); assertEquals("RESULT_EXPIRED", value.problem.code); assertFalse(value.hasResultId()) }
            }
        }
    }
}
