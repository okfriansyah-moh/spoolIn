package com.spoolin.catalog.features.artwork

import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.catalog.operations.receipts.ReceiptWork
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.OperationIntent
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

class ArtworkRpc(
    private val dataSource: DataSource,
    private val receipts: OwnerReceipts,
    private val policyRevision: () -> String = { "unavailable" },
) {
    private val reads = ArtworkReads(dataSource)

    fun beginArtwork(request: Spoolin.Hold, response: StreamObserver<Spoolin.Outcome>) = respond(response) {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw Status.PERMISSION_DENIED.asRuntimeException()
        val operation = id(request.context.operationId)
        if (request.holdId != operation.toString() || !request.hasExpectedVersion() || request.expectedVersion < 0) throw Status.INVALID_ARGUMENT.asRuntimeException()
        val title = id(request.titleId)
        val asset = id(request.assetId)
        val correlation = id(request.context.correlationId)
        val intent = OperationIntent(operation, "account:${claims.accountId}", "BeginArtwork", request.context.policyRevision,
            listOf("titleId" to title.toString(), "assetId" to asset.toString(), "expectedVersion" to request.expectedVersion.toString()))
        val receipt = receipts.defer(intent, correlation, true, beforeReserve = ::lock) { connection ->
            if (request.context.policyRevision != policyRevision()) throw Status.FAILED_PRECONDITION.withDescription("POLICY_CHANGED").asRuntimeException()
            val old = connection.queryBound("SELECT version,status,attachment_id FROM title WHERE id=? AND kind='MOVIE' FOR UPDATE", listOf(title)) {
                Triple(it.getLong("version"), it.getString("status"), it.getObject("attachment_id", UUID::class.java))
            }.singleOrNull() ?: throw Status.NOT_FOUND.asRuntimeException()
            if (old.first != request.expectedVersion || old.second != "ACTIVE") throw Status.FAILED_PRECONDITION.withDescription("STALE_VERSION").asRuntimeException()
            ArtworkAttachmentSql.insertPreparing(connection, operation, title, claims.accountId, request.expectedVersion, asset, request.context.policyRevision, old.third)
            val work = WorkRequest("attach", "ARTWORK_ATTACH", operation, Instant.now().plusSeconds(300), baseDelayMs = 10000, capDelayMs = 60000)
            ReceiptWork.enqueue(connection, operation, correlation, work, Instant.now())
            connection.executeBound("UPDATE durable_work SET payload=?::jsonb WHERE id=?",
                "{\"subjectId\":\"$operation\",\"credentialId\":\"${claims.credentialId}\",\"authEpoch\":${claims.authEpoch}}", work.id(operation))
        }
        outcome(receipt)
    }

    fun cancelArtwork(request: Spoolin.CancelArtworkRequest, response: StreamObserver<Spoolin.ArtworkCancelOutcome>) = respond(response) {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw Status.PERMISSION_DENIED.asRuntimeException()
        val operation = id(request.context.operationId)
        val upload = id(request.uploadOperationId)
        var cancelled = false
        val result = receipts.defer(OperationIntent(operation, "account:${claims.accountId}", "CancelArtwork", "",
            listOf("uploadOperationId" to upload.toString())), id(request.context.correlationId), true, beforeReserve = ::lock) { connection ->
            val row = connection.queryBound("SELECT actor_id,state FROM artwork_attachment_request WHERE id=? FOR UPDATE", listOf(upload)) {
                it.getObject("actor_id", UUID::class.java) to it.getString("state")
            }.singleOrNull() ?: throw Status.NOT_FOUND.asRuntimeException()
            if (row.first != claims.accountId) throw Status.NOT_FOUND.asRuntimeException()
            if (row.second == "PREPARING") {
                ArtworkAttachmentSql.markAborted(connection, upload)
                receipts.completePending(connection, upload, SafeResult(upload), "CANCELLED")
                enqueueRelease(connection, upload, upload, id(request.context.correlationId))
                cancelled = true
            }
            receipts.completePending(connection, operation, SafeResult(upload, discarded = cancelled))
        }
        val persisted = result.result?.discarded ?: false
        val original = receipts.find(upload, "account:${claims.accountId}") ?: throw Status.NOT_FOUND.asRuntimeException()
        Spoolin.ArtworkCancelOutcome.newBuilder().setOperation(outcome(result)).setUploadOperation(outcome(original)).setCancelled(persisted).build()
    }

    fun getAttachment(request: Spoolin.Id, response: StreamObserver<Spoolin.Attachment>) = respond(response) { reads.attachment(id(request.id)) }
    fun resolveArtworkAccess(request: Spoolin.ArtworkAccessRequest, response: StreamObserver<Spoolin.ArtworkGrant>) = respond(response) { reads.access(request) }

    private fun outcome(receipt: com.spoolin.support.receipts.ReceiptOutcome): Spoolin.Outcome =
        Spoolin.Outcome.newBuilder().setOperationId(receipt.id.toString()).setState(receipt.state.name).setResultExpired(receipt.resultExpired)
            .also { builder -> receipt.result?.let { builder.resultId = it.resultId.toString(); it.version?.let { version -> builder.version = version } }
                receipt.errorCode?.let { builder.problem = Spoolin.Problem.newBuilder().setCode(it).build() } }.build()

    private fun <T> respond(response: StreamObserver<T>, operation: () -> T) {
        try { response.onNext(operation()); response.onCompleted() }
        catch (failure: io.grpc.StatusRuntimeException) { response.onError(failure) }
        catch (_: IllegalArgumentException) { response.onError(Status.INVALID_ARGUMENT.asRuntimeException()) }
        catch (_: com.spoolin.support.receipts.ReceiptConflict) { response.onError(Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException()) }
        catch (_: com.spoolin.support.receipts.ReceiptHidden) { response.onError(Status.NOT_FOUND.asRuntimeException()) }
    }

    companion object {
        fun lock(connection: Connection) { connection.queryBound("SELECT pg_advisory_xact_lock(41001,1)", emptyList()) { 1 } }
        fun id(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value) }
        fun enqueueRelease(connection: Connection, parent: UUID, hold: UUID, correlation: UUID) {
            val work = WorkRequest("release-${hold}", "ARTWORK_RELEASE", hold, Instant.now().plusSeconds(300))
            val version = connection.queryBound("SELECT t.version FROM title t JOIN artwork_attachment_request a ON a.title_id=t.id WHERE a.id=?", listOf(hold)) { it.getLong("version") }.singleOrNull()
                ?: throw Status.FAILED_PRECONDITION.withDescription("REFERENCE_VERSION_UNAVAILABLE").asRuntimeException()
            ReceiptWork.enqueue(connection, parent, correlation, work, Instant.now())
            connection.executeBound("UPDATE durable_work SET payload=?::jsonb WHERE id=?", "{\"subjectId\":\"$hold\",\"catalogVersion\":$version}", work.id(parent))
        }
    }
}
