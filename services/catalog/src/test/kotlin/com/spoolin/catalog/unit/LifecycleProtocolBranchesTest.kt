package com.spoolin.catalog.unit

import com.spoolin.catalog.features.lifecycle.LifecycleCommand
import com.spoolin.catalog.features.lifecycle.lifecycleOutcome
import com.spoolin.catalog.features.lifecycle.lifecycleRejection
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.ReceiptState
import com.spoolin.support.receipts.SafeResult
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class LifecycleProtocolBranchesTest {
    private fun claims() =
        ActorClaims(
            "catalog",
            "/spoolin.v1.CatalogService/MovieLifecycle",
            unitActor,
            unitCredential,
            null,
            "MAINTAINER",
            1,
            1,
            31,
            "nonce",
            unitCorrelation,
            unitOperation,
        )

    private fun request() =
        Spoolin.Lifecycle
            .newBuilder()
            .setAction("ARCHIVE")
            .setTarget(
                Spoolin.VersionedId
                    .newBuilder()
                    .setId(unitTarget.toString())
                    .setExpectedVersion(7)
                    .setContext(
                        Spoolin.Context
                            .newBuilder()
                            .setOperationId(unitOperation.toString())
                            .setCorrelationId(unitCorrelation.toString())
                            .setPolicyRevision("policy"),
                    ),
            ).build()

    @Test
    fun `lifecycle command binds full target intent and optional impact token`() {
        for (token in listOf(null, "signed-impact")) {
            val request = request().toBuilder().also { if (token != null) it.impactToken = token }.build()
            val command = LifecycleCommand.parse(request, claims())
            assertEquals(unitTarget, command.target)
            assertEquals(token, command.impactToken)
            assertEquals("MovieLifecycle", command.intent("MovieLifecycle").kind)
            assertEquals("account:$unitActor", command.intent("MovieLifecycle").actorScope)
        }
        for (claims in listOf(claims().copy(operationId = unitAsset), claims().copy(correlationId = unitAsset))) {
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                assertThrows(io.grpc.StatusRuntimeException::class.java) {
                    LifecycleCommand.parse(request(), claims)
                }.status.code,
            )
        }
        for (invalid in listOf(
            request().toBuilder().setAction("UNKNOWN").build(),
            request().toBuilder().setTarget(request().target.toBuilder().clearExpectedVersion()).build(),
            request().toBuilder().setTarget(request().target.toBuilder().setExpectedVersion(-1)).build(),
            request().toBuilder().setTarget(request().target.toBuilder().setId("bad")).build(),
        )) {
            assertEquals(
                Status.Code.INVALID_ARGUMENT,
                assertThrows(io.grpc.StatusRuntimeException::class.java) {
                    LifecycleCommand.parse(invalid, claims())
                }.status.code,
            )
        }
    }

    @Test
    fun `lifecycle receipts project optional result version problem and expiry explicitly`() {
        val base = ReceiptOutcome(unitOperation, ReceiptState.PENDING, null, null, unitCorrelation, unitNow, false)
        assertFalse(lifecycleOutcome(base).hasResultId())
        assertFalse(lifecycleOutcome(base).hasVersion())
        assertFalse(lifecycleOutcome(base).hasProblem())
        val withoutVersion = lifecycleOutcome(base.copy(state = ReceiptState.SUCCEEDED, result = SafeResult(unitTarget)))
        assertEquals(unitTarget.toString(), withoutVersion.resultId)
        assertFalse(withoutVersion.hasVersion())
        val versioned = lifecycleOutcome(base.copy(state = ReceiptState.SUCCEEDED, result = SafeResult(unitTarget, 8)))
        assertEquals(8L, versioned.version)
        val failed = lifecycleOutcome(base.copy(state = ReceiptState.EXPIRED, resultExpired = true, errorCode = "RESULT_EXPIRED"))
        assertEquals("RESULT_EXPIRED", failed.problem.code)
        assertEquals(true, failed.resultExpired)
    }

    @Test
    fun `lifecycle conflict responses preserve actionable failures and let unrelated transport failures propagate`() {
        for ((description, code) in listOf(
            "IMPACT_CHANGED" to "IMPACT_CHANGED",
            "STALE_VERSION" to "STALE_VERSION",
            "POLICY_CHANGED" to "POLICY_CHANGED",
            "LIFECYCLE_STATE_CHANGED" to "FIELD_INVALID",
            "CONFIRMATION_REQUIRED" to "FIELD_INVALID",
        )) {
            val result = lifecycleRejection(request(), Status.FAILED_PRECONDITION.withDescription(description).asRuntimeException())
            assertEquals("FAILED", result?.state)
            assertEquals(code, result?.problem?.code)
            assertEquals(unitOperation.toString(), result?.operationId)
        }
        assertNull(lifecycleRejection(request(), Status.UNAVAILABLE.asRuntimeException()))
    }
}
