package com.spoolin.core.operations.receipts

import com.spoolin.support.receipts.OperationOwner
import com.spoolin.support.receipts.ReceiptAuthority
import com.spoolin.support.receipts.ReceiptOutcome
import io.grpc.Status
import io.grpc.StatusRuntimeException
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.Outcome
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val LOOKUP_BUDGET_SECONDS = 3L

class OperationRouter(
    private val authority: ReceiptAuthority,
    private val local: (UUID, String) -> ReceiptOutcome?,
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val media: MediaServiceGrpc.MediaServiceBlockingStub,
) {
    fun find(
        id: UUID,
        owner: OperationOwner?,
    ): Map<String, Any?>? {
        val actor = authority.actorScope() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LOOKUP_BUDGET_SECONDS)
        val owners = owner?.let { listOf(it) } ?: OperationOwner.entries
        val matches = owners.mapNotNull { selected -> lookup(id, actor, selected, deadline) }
        if (matches.size > 1) throw AmbiguousOperation()
        return matches.singleOrNull()?.let(::projection)
    }

    private fun lookup(
        id: UUID,
        actor: String,
        owner: OperationOwner,
        deadline: Long,
    ): Outcome? {
        val remaining = deadline - System.nanoTime()
        if (remaining <= 0) throw Status.DEADLINE_EXCEEDED.asRuntimeException()
        val request = Id.newBuilder().setId(id.toString()).build()
        return try {
            when (owner) {
                OperationOwner.CORE -> localBeforeDeadline(id, actor, remaining)?.let(::protobuf)
                OperationOwner.CATALOG ->
                    catalog.withDeadlineAfter(remaining, TimeUnit.NANOSECONDS).getOperation(request)
                OperationOwner.MEDIA -> media.withDeadlineAfter(remaining, TimeUnit.NANOSECONDS).getOperation(request)
            }
        } catch (failure: StatusRuntimeException) {
            if (failure.status.code == Status.Code.NOT_FOUND) null else throw failure
        }
    }

    private fun localBeforeDeadline(
        id: UUID,
        actor: String,
        remaining: Long,
    ): ReceiptOutcome? {
        val pending = java.util.concurrent.FutureTask { local(id, actor) }
        Thread.ofVirtual().name("owner-receipt-read").start(pending)
        return try {
            pending.get(remaining, TimeUnit.NANOSECONDS)
        } catch (_: java.util.concurrent.TimeoutException) {
            fail(Status.DEADLINE_EXCEEDED)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            fail(Status.CANCELLED)
        } catch (_: java.util.concurrent.ExecutionException) {
            fail(Status.UNAVAILABLE)
        } finally {
            if (!pending.isDone) pending.cancel(true)
        }
    }

    private fun fail(status: Status): Nothing = throw status.asRuntimeException()

    private fun protobuf(receipt: ReceiptOutcome): Outcome {
        val output =
            Outcome
                .newBuilder()
                .setOperationId(receipt.id.toString())
                .setState(receipt.state.name)
                .setResultExpired(receipt.resultExpired)
        receipt.result?.let { result ->
            output.resultId = result.resultId.toString()
            result.version?.let { output.version = it }
        }
        receipt.errorCode?.let {
            output.problem =
                spoolin.v1.Spoolin.Problem
                    .newBuilder()
                    .setCode(it)
                    .build()
        }
        return output.build()
    }

    private fun projection(outcome: Outcome): Map<String, Any?> =
        mapOf(
            "id" to outcome.operationId,
            "state" to outcome.state,
            "resultId" to if (outcome.hasResultId()) outcome.resultId else null,
            "version" to if (outcome.hasVersion()) outcome.version.toString() else null,
            "resultExpired" to outcome.resultExpired,
            "problem" to
                if (outcome.hasProblem()) {
                    mapOf(
                        "code" to outcome.problem.code,
                        "message" to "The operation could not be completed.",
                        "fields" to emptyList<Any>(),
                    )
                } else {
                    null
                },
        )
}

class AmbiguousOperation : IllegalStateException("AMBIGUOUS_OPERATION")
