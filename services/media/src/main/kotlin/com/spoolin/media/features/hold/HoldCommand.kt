package com.spoolin.media.features.hold

import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.WorkRequest
import io.grpc.Status
import spoolin.v1.Spoolin.Hold
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

internal enum class HoldAction(val step: String, val kind: String) {
    RESERVE("media-reserve", "HOLD_RESERVE"),
    CONFIRM("media-confirm", "HOLD_CONFIRM"),
    RELEASE("media-release", "HOLD_RELEASE"),
}

internal data class HoldCommand(
    val hold: UUID, val asset: UUID, val title: UUID, val actor: UUID,
    val operation: UUID, val correlation: UUID, val version: Long?,
    val expectedVersion: Long?, val policy: String, val action: HoldAction,
) {
    val scope: String get() = "account:$actor"
    fun child(step: String): UUID = WorkRequest(step, "ARTWORK", hold, Instant.MAX).id(hold)
    fun hash(): ByteArray = MessageDigest.getInstance("SHA-256").digest(OperationIntent(
        operation, scope, action.kind, policy,
        listOf("hold" to hold.toString(), "asset" to asset.toString(), "title" to title.toString(),
            "actor" to actor.toString(), "version" to version?.toString(), "expectedVersion" to expectedVersion?.toString()),
    ).serialized())

    companion object {
        fun parse(request: Hold, action: HoldAction): HoldCommand {
            val command = HoldCommand(
                id(request.holdId), id(request.assetId), id(request.titleId), id(request.actorId),
                id(request.context.operationId), id(request.context.correlationId),
                if (request.hasCatalogVersion()) request.catalogVersion else null,
                if (request.hasExpectedVersion()) request.expectedVersion else null,
                request.context.policyRevision, action,
            )
            if (command.policy.length > 64 || command.version?.let { it < 0 } == true ||
                command.expectedVersion?.let { it < 0 } == true) throw Status.INVALID_ARGUMENT.asRuntimeException()
            if (command.operation != command.child(action.step)) throw Status.INVALID_ARGUMENT
                .withDescription("HOLD_OPERATION_BINDING_INVALID").asRuntimeException()
            return command
        }

        private fun id(value: String): UUID = runCatching {
            UUID.fromString(value).also { require(it.toString() == value) }
        }.getOrElse { throw Status.INVALID_ARGUMENT.asRuntimeException() }
    }
}
