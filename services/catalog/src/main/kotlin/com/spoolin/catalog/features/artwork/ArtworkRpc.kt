package com.spoolin.catalog.features.artwork

import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.catalog.operations.receipts.ReceiptWork
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin
import java.sql.Connection
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

private const val ARTWORK_DEADLINE_SECONDS = 300L
private const val ATTACH_BASE_DELAY_MS = 10000
private const val ATTACH_CAP_DELAY_MS = 60000

class ArtworkRpc(
    private val dataSource: DataSource,
    private val receipts: OwnerReceipts,
    private val policyRevision: () -> String = { "unavailable" },
) {
    private val reads = ArtworkReads(dataSource)

    fun beginArtwork(request: Spoolin.Hold, response: StreamObserver<Spoolin.Outcome>) = respond(response) {
        val command = beginCommand(request)
        val receipt = receipts.defer(command.intent(), command.correlation, true, beforeReserve = ::lock) { connection ->
            prepareBegin(connection, command)
        }
        outcome(receipt)
    }

    private fun prepareBegin(connection: Connection, command: BeginArtworkCommand) {
        if (command.request.context.policyRevision != policyRevision()) {
            throw Status.FAILED_PRECONDITION.withDescription("POLICY_CHANGED").asRuntimeException()
        }
        val old = beginTitle(connection, command)
        ArtworkAttachmentSql.insertPreparing(connection, command.operation, command.title, command.actor.accountId,
            command.request.expectedVersion, command.asset, command.request.context.policyRevision, old.oldHold)
        val work = WorkRequest("attach", "ARTWORK_ATTACH", command.operation,
            Instant.now().plusSeconds(ARTWORK_DEADLINE_SECONDS), baseDelayMs = ATTACH_BASE_DELAY_MS,
            capDelayMs = ATTACH_CAP_DELAY_MS)
        ReceiptWork.enqueue(connection, command.operation, command.correlation, work, Instant.now())
        connection.executeBound("UPDATE durable_work SET payload=?::jsonb WHERE id=?",
            "{\"subjectId\":\"${command.operation}\",\"credentialId\":\"${command.actor.credentialId}\"," +
                "\"authEpoch\":${command.actor.authEpoch}}", work.id(command.operation))
    }

    fun cancelArtwork(request: Spoolin.CancelArtworkRequest,
        response: StreamObserver<Spoolin.ArtworkCancelOutcome>) = respond(response) {
        val actor = managementActor()
        val command = CancelArtworkCommand(id(request.context.operationId), id(request.uploadOperationId),
            id(request.context.correlationId), actor)
        val result = receipts.defer(command.intent(), command.correlation, true, beforeReserve = ::lock) { connection ->
            prepareCancellation(connection, command)
        }
        cancellationOutcome(result, command)
    }

    private fun prepareCancellation(connection: Connection, command: CancelArtworkCommand) {
        val original = connection.queryBound(
            "SELECT id FROM operation_receipt WHERE id=? AND actor_scope=? FOR UPDATE",
            listOf(command.upload, command.scope),
        ) { it.getObject("id", UUID::class.java) }.singleOrNull()
        val intent = cancellationIntent(connection, command)
        if (original == null || intent == null) {
            receipts.completePending(connection, command.operation, SafeResult(command.upload), "NOT_FOUND")
            return
        }
        val cancelled = intent.state == "PREPARING"
        if (cancelled) cancelPreparing(connection, command, intent)
        receipts.completePending(connection, command.operation, SafeResult(command.upload, discarded = cancelled))
    }

    private fun cancelPreparing(connection: Connection, command: CancelArtworkCommand, intent: CancellationIntent) {
        check(ArtworkAttachmentSql.markAborted(connection, command.upload) == 1)
        receipts.completePending(connection, command.upload, SafeResult(command.upload), "CANCELLED")
        AuditInsert.record(connection, command.operation, command.actor.accountId, "HUMAN", "CANCEL_ARTWORK", "TITLE",
            intent.title, "SUCCEEDED", "{\"artworkIntent.state\":{\"before\":\"PREPARING\",\"after\":\"ABORTED\"}}",
            command.correlation, Instant.now())
        enqueueRelease(connection, command.upload, command.upload, command.correlation)
    }

    private fun cancellationOutcome(result: ReceiptOutcome, command: CancelArtworkCommand): Spoolin.ArtworkCancelOutcome {
        val builder = Spoolin.ArtworkCancelOutcome.newBuilder().setOperation(outcome(result))
            .setCancelled(result.result?.discarded ?: false)
        if (result.errorCode != "NOT_FOUND") {
            val original = receipts.find(command.upload, command.scope) ?: throw Status.NOT_FOUND.asRuntimeException()
            builder.uploadOperation = outcome(original)
        }
        return builder.build()
    }

    fun getAttachment(request: Spoolin.Id, response: StreamObserver<Spoolin.Attachment>) =
        respond(response) { reads.attachment(id(request.id)) }

    fun resolveArtworkAccess(request: Spoolin.ArtworkAccessRequest, response: StreamObserver<Spoolin.ArtworkGrant>) =
        respond(response) { reads.access(request) }

    companion object {
        fun lock(connection: Connection) {
            connection.queryBound("SELECT pg_advisory_xact_lock(41001,1)", emptyList()) { 1 }
        }
        fun id(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value) }

        fun enqueueRelease(connection: Connection, parent: UUID, hold: UUID, correlation: UUID) {
            val work = WorkRequest("release-$hold", "ARTWORK_RELEASE", hold,
                Instant.now().plusSeconds(ARTWORK_DEADLINE_SECONDS))
            val version = connection.queryBound(
                "SELECT t.version FROM title t JOIN artwork_attachment_request a ON a.title_id=t.id WHERE a.id=?",
                listOf(hold),
            ) { it.getLong("version") }.singleOrNull()
                ?: throw Status.FAILED_PRECONDITION.withDescription("REFERENCE_VERSION_UNAVAILABLE").asRuntimeException()
            ReceiptWork.enqueue(connection, parent, correlation, work, Instant.now())
            connection.executeBound("UPDATE durable_work SET payload=?::jsonb WHERE id=?",
                "{\"subjectId\":\"$hold\",\"catalogVersion\":$version}", work.id(parent))
        }
    }
}

private data class BeginArtworkCommand(val request: Spoolin.Hold, val actor: ActorClaims, val operation: UUID,
    val title: UUID, val asset: UUID, val correlation: UUID) {
    fun intent(): OperationIntent = OperationIntent(operation, "account:${actor.accountId}", "BeginArtwork",
        request.context.policyRevision, listOf("titleId" to title.toString(), "assetId" to asset.toString(),
            "expectedVersion" to request.expectedVersion.toString()))
}

private fun beginCommand(request: Spoolin.Hold): BeginArtworkCommand {
    val claims = managementActor()
    val operation = ArtworkRpc.id(request.context.operationId)
    if (request.holdId != operation.toString() || !request.hasExpectedVersion() || request.expectedVersion < 0) {
        throw Status.INVALID_ARGUMENT.asRuntimeException()
    }
    return BeginArtworkCommand(request, claims, operation, ArtworkRpc.id(request.titleId),
        ArtworkRpc.id(request.assetId), ArtworkRpc.id(request.context.correlationId))
}

private data class BeginTitle(val version: Long, val status: String, val oldHold: UUID?)

private fun beginTitle(connection: Connection, command: BeginArtworkCommand): BeginTitle {
    val old = connection.queryBound(
        "SELECT version,status,attachment_id FROM title WHERE id=? AND kind='MOVIE' FOR UPDATE", listOf(command.title),
    ) { BeginTitle(it.getLong("version"), it.getString("status"), it.getObject("attachment_id", UUID::class.java)) }
        .singleOrNull() ?: throw Status.NOT_FOUND.asRuntimeException()
    if (old.version != command.request.expectedVersion || old.status != "ACTIVE") {
        throw Status.FAILED_PRECONDITION.withDescription("STALE_VERSION").asRuntimeException()
    }
    return old
}

private data class CancelArtworkCommand(val operation: UUID, val upload: UUID, val correlation: UUID,
    val actor: ActorClaims) {
    val scope: String get() = "account:${actor.accountId}"
    fun intent(): OperationIntent = OperationIntent(operation, scope, "CancelArtwork", "",
        listOf("uploadOperationId" to upload.toString()))
}

private data class CancellationIntent(val state: String, val title: UUID)

private fun cancellationIntent(connection: Connection, command: CancelArtworkCommand): CancellationIntent? =
    connection.queryBound("SELECT state,title_id FROM artwork_attachment_request WHERE id=? AND actor_id=? FOR UPDATE",
        listOf(command.upload, command.actor.accountId),
    ) { CancellationIntent(it.getString("state"), it.getObject("title_id", UUID::class.java)) }.singleOrNull()

private fun managementActor(): ActorClaims {
    val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
    if (!Permissions.management(claims.role)) throw Status.PERMISSION_DENIED.asRuntimeException()
    return claims
}

private fun outcome(receipt: ReceiptOutcome): Spoolin.Outcome = Spoolin.Outcome.newBuilder()
    .setOperationId(receipt.id.toString()).setState(receipt.state.name).setResultExpired(receipt.resultExpired)
    .also { builder ->
        receipt.result?.let { result -> builder.resultId = result.resultId.toString()
            result.version?.let { builder.version = it } }
        receipt.errorCode?.let { builder.problem = Spoolin.Problem.newBuilder().setCode(it).build() }
    }.build()

private fun <T> respond(response: StreamObserver<T>, operation: () -> T) {
    try { response.onNext(operation()); response.onCompleted() }
    catch (failure: io.grpc.StatusRuntimeException) { response.onError(failure) }
    catch (_: IllegalArgumentException) { response.onError(Status.INVALID_ARGUMENT.asRuntimeException()) }
    catch (_: com.spoolin.support.receipts.ReceiptConflict) {
        response.onError(Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException())
    }
    catch (_: com.spoolin.support.receipts.ReceiptHidden) { response.onError(Status.NOT_FOUND.asRuntimeException()) }
}
