package com.spoolin.catalog.boundary.grpc

import com.spoolin.support.identity.ActorRejected
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.ReceiptHidden
import com.spoolin.support.receipts.ReceiptOutcome
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Context
import spoolin.v1.Spoolin.FieldIssue
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.Problem
import java.util.UUID

/** Shared wire projection and receipt failures; domain validation stays in its owning mutation. */
internal object CatalogWriteRpc {
    fun receipt(
        response: StreamObserver<Outcome>,
        execute: () -> ReceiptOutcome,
    ) {
        try {
            response.onNext(outcome(execute()))
            response.onCompleted()
        } catch (failure: StatusRuntimeException) {
            response.onError(failure)
        } catch (_: ReceiptConflict) {
            response.onError(Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException())
        } catch (_: ReceiptHidden) {
            response.onError(Status.NOT_FOUND.asRuntimeException())
        } catch (_: ActorRejected) {
            response.onError(Status.PERMISSION_DENIED.withDescription("UNAUTHORISED").asRuntimeException())
        }
    }

    fun fields(
        operation: String,
        response: StreamObserver<Outcome>,
        execute: () -> ReceiptOutcome,
    ) {
        try {
            receipt(response, execute)
        } catch (violation: InputViolation) {
            response.onNext(
                Outcome
                    .newBuilder()
                    .setOperationId(operation)
                    .setState("FAILED")
                    .setProblem(
                        Problem.newBuilder().setCode("FIELD_INVALID").addFields(
                            FieldIssue
                                .newBuilder()
                                .setPath(
                                    violation.path,
                                ).setCode("INVALID")
                                .setMessage(violation.correction),
                        ),
                    ).build(),
            )
            response.onCompleted()
        }
    }

    fun outcome(receipt: ReceiptOutcome): Outcome {
        val builder =
            Outcome
                .newBuilder()
                .setOperationId(receipt.id.toString())
                .setState(receipt.state.name)
                .setResultExpired(receipt.resultExpired)
        receipt.result?.let { result ->
            builder.resultId = result.resultId.toString()
            result.version?.let { builder.version = it }
        }
        receipt.errorCode?.let { builder.problem = Problem.newBuilder().setCode(it).build() }
        return builder.build()
    }

    fun id(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }

    fun requireVersion(
        create: Boolean,
        supplied: Boolean,
        version: Long,
    ) {
        if (!create && (!supplied || version < 0)) {
            throw InputViolation("expectedVersion", "Supply the current version.")
        }
    }
}

internal data class CatalogWriteContext(
    val actor: UUID,
    val operation: UUID,
    val correlation: UUID,
    val policy: String,
) {
    fun intent(
        kind: String,
        fields: List<Pair<String, String?>>,
    ): OperationIntent = OperationIntent(operation, "account:$actor", kind, policy, fields)

    companion object {
        fun management(context: Context): CatalogWriteContext {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!Permissions.management(claims.role)) throw ActorRejected()
            return CatalogWriteContext(
                claims.accountId,
                CatalogWriteRpc.id(context.operationId),
                CatalogWriteRpc.id(context.correlationId),
                context.policyRevision,
            )
        }
    }
}
