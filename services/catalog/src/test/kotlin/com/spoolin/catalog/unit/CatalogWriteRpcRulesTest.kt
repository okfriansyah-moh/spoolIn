package com.spoolin.catalog.unit

import com.spoolin.catalog.boundary.grpc.CatalogWriteContext
import com.spoolin.catalog.boundary.grpc.CatalogWriteRpc
import com.spoolin.catalog.operations.audit.auditPatchedValue
import com.spoolin.support.identity.ActorRejected
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PatchValue
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.ReceiptHidden
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.ReceiptState
import com.spoolin.support.receipts.SafeResult
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class CatalogWriteRpcRulesTest {
    private fun receipt(
        state: ReceiptState,
        result: SafeResult? = null,
        error: String? = null,
        expired: Boolean = false,
    ) = ReceiptOutcome(unitOperation, state, result, error, unitCorrelation, unitNow, expired)

    @Test
    fun `receipt responses retain optional version and expired failure without inventing result fields`() {
        val pending = CatalogWriteRpc.outcome(receipt(ReceiptState.PENDING))
        assertEquals("PENDING", pending.state)
        assertFalse(pending.hasResultId())
        assertFalse(pending.hasVersion())
        assertFalse(pending.hasProblem())
        val completed = RpcResult<Spoolin.Outcome>()
        CatalogWriteRpc.receipt(completed) { receipt(ReceiptState.SUCCEEDED, SafeResult(unitTarget)) }
        assertEquals(unitTarget.toString(), completed.value().resultId)
        assertFalse(completed.value().hasVersion())
        assertEquals(9L, CatalogWriteRpc.outcome(receipt(ReceiptState.SUCCEEDED, SafeResult(unitTarget, 9))).version)
        val expired = CatalogWriteRpc.outcome(receipt(ReceiptState.EXPIRED, error = "RESULT_EXPIRED", expired = true))
        assertTrue(expired.resultExpired)
        assertEquals("RESULT_EXPIRED", expired.problem.code)
        assertFalse(expired.hasResultId())
    }

    @Test
    fun `receipt failures retain transport distinctions and never complete an errored stream`() {
        val original = Status.ABORTED.withDescription("VERSION_CONFLICT").asRuntimeException()
        val failures =
            listOf(
                Triple(original, Status.Code.ABORTED, "VERSION_CONFLICT"),
                Triple(ReceiptConflict(), Status.Code.ALREADY_EXISTS, "IDEMPOTENCY_CONFLICT"),
                Triple(ReceiptHidden(), Status.Code.NOT_FOUND, null),
                Triple(ActorRejected(), Status.Code.PERMISSION_DENIED, "UNAUTHORISED"),
            )
        for ((failure, code, description) in failures) {
            val response = RpcResult<Spoolin.Outcome>()
            CatalogWriteRpc.receipt(response) { throw failure }
            assertEquals(code, Status.fromThrowable(response.failure).code)
            assertEquals(description, Status.fromThrowable(response.failure).description)
            assertTrue(response.values.isEmpty())
            assertFalse(response.completed)
            if (failure === original) assertSame(original, response.failure)
        }
    }

    @Test
    fun `field validation returns correctable failed outcome while raw receipt path leaves validation to caller`() {
        val violation = InputViolation("originalTitle", "Supply a shorter title.")
        val response = RpcResult<Spoolin.Outcome>()
        CatalogWriteRpc.fields(unitOperation.toString(), response) { throw violation }
        assertEquals(unitOperation.toString(), response.value().operationId)
        assertEquals("FAILED", response.value().state)
        assertEquals("FIELD_INVALID", response.value().problem.code)
        assertEquals(
            "originalTitle",
            response
                .value()
                .problem.fieldsList
                .single()
                .path,
        )
        assertEquals(
            "Supply a shorter title.",
            response
                .value()
                .problem.fieldsList
                .single()
                .message,
        )
        assertNull(response.failure)
        assertSame(
            violation,
            assertThrows(InputViolation::class.java) {
                CatalogWriteRpc.receipt(RpcResult()) { throw violation }
            },
        )
    }

    @Test
    fun `patch snapshots preserve omitted values and distinguish explicit clear and replacement`() {
        assertEquals("Existing", auditPatchedValue(PatchValue.Omitted, "Existing"))
        assertNull(auditPatchedValue(PatchValue.Clear, "Existing"))
        assertEquals("New", auditPatchedValue(PatchValue.Value("New"), "Existing"))
        assertNull(auditPatchedValue(PatchValue.Value<String?>(null), "Existing"))
        assertNull(auditPatchedValue(PatchValue.Omitted, null))
    }

    @Test
    fun `movie and person creates omit version while updates require a nonnegative supplied version`() {
        CatalogWriteRpc.requireVersion(true, false, -1)
        CatalogWriteRpc.requireVersion(false, true, 0)
        for ((supplied, version) in listOf(false to 0L, true to -1L)) {
            val violation =
                assertThrows(InputViolation::class.java) { CatalogWriteRpc.requireVersion(false, supplied, version) }
            assertEquals("expectedVersion", violation.path)
        }
    }

    @Test
    fun `management context preserves actor scope operation correlation policy and intent fields`() =
        withMaintainer {
            val command =
                CatalogWriteContext.management(
                    Spoolin.Context
                        .newBuilder()
                        .setOperationId(unitOperation.toString())
                        .setCorrelationId(unitCorrelation.toString())
                        .setPolicyRevision("p7")
                        .build(),
                )
            assertEquals(unitActor, command.actor)
            assertEquals(unitCorrelation, command.correlation)
            val fields = listOf("title" to "River", "originalTitle" to null)
            val intent = command.intent("WriteMovie", fields)
            assertEquals(unitOperation, intent.id)
            assertEquals("account:$unitActor", intent.actorScope)
            assertEquals("p7", command.policy)
            assertEquals(fields, intent.fields)
            val invalid = assertThrows(io.grpc.StatusRuntimeException::class.java) { CatalogWriteRpc.id("bad") }
            assertEquals(Status.Code.INVALID_ARGUMENT, invalid.status.code)
            assertEquals("INVALID_ID", invalid.status.description)
        }
}
