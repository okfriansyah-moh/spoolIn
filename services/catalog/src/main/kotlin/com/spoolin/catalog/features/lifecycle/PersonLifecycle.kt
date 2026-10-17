package com.spoolin.catalog.features.lifecycle

import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Lifecycle
import spoolin.v1.Spoolin.Outcome
import java.util.UUID
import javax.sql.DataSource

class PersonLifecycle(
    private val dataSource: DataSource,
    private val receipts: OwnerReceipts,
) {
    fun apply(request: Lifecycle, response: StreamObserver<Outcome>) {
        try {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!Permissions.management(claims.role)) throw Status.PERMISSION_DENIED.asRuntimeException()
            val target = parseUuid(request.target.id)
            val version = request.target.expectedVersion
            val action = LifecycleRules.Action.valueOf(request.action)
            if (action == LifecycleRules.Action.DELETE && request.impactToken.isNullOrBlank()) {
                throw Status.FAILED_PRECONDITION.withDescription("IMPACT_TOKEN_REQUIRED").asRuntimeException()
            }
            if (!request.confirmed && LifecycleRules.requiresConfirmation(action)) {
                throw Status.FAILED_PRECONDITION.withDescription("CONFIRMATION_REQUIRED").asRuntimeException()
            }
            val operationId = parseUuid(request.target.context.operationId)
            val intent =
                OperationIntent(
                    operationId,
                    "account:${claims.accountId}",
                    "PersonLifecycle",
                    request.target.context.policyRevision,
                    listOf("action" to action.name, "target" to target.toString()),
                )
            val outcome =
                receipts.execute(intent, parseUuid(request.target.context.correlationId), true) { conn ->
                    val updated =
                        when (action) {
                            LifecycleRules.Action.ARCHIVE ->
                                conn.executeBound(
                                    "UPDATE person SET status = 'ARCHIVED', version = version + 1, updated_at = CURRENT_TIMESTAMP" +
                                        " WHERE id = ? AND version = ? AND status = 'ACTIVE'",
                                    target,
                                    version,
                                )
                            LifecycleRules.Action.RESTORE ->
                                conn.executeBound(
                                    "UPDATE person SET status = 'ACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP" +
                                        " WHERE id = ? AND version = ? AND status = 'ARCHIVED'",
                                    target,
                                    version,
                                )
                            LifecycleRules.Action.DELETE ->
                                conn.executeBound(
                                    "UPDATE person SET status = 'DELETED', version = version + 1, updated_at = CURRENT_TIMESTAMP" +
                                        " WHERE id = ? AND version = ? AND status = 'ARCHIVED'",
                                    target,
                                    version,
                                )
                        }
                    if (updated != 1) throw Status.FAILED_PRECONDITION.withDescription("STALE_TARGET").asRuntimeException()
                    SafeResult(resultId = target, version = version + 1)
                }
            response.onNext(outcomeProto(outcome))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    private fun outcomeProto(receipt: com.spoolin.support.receipts.ReceiptOutcome): Outcome =
        Outcome.newBuilder()
            .setOperationId(receipt.id.toString())
            .setState(receipt.state.name)
            .setResultExpired(receipt.resultExpired)
            .also { b ->
                receipt.result?.let { r ->
                    b.resultId = r.resultId.toString()
                    r.version?.let { b.version = it }
                }
            }
            .build()

    private fun parseUuid(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.asRuntimeException() }
}
