@file:Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount", "MagicNumber")

package com.spoolin.media.features.upload

import com.spoolin.media.features.asset.AssetSql
import com.spoolin.media.features.asset.AssetState
import com.spoolin.media.features.processing.ImageValidator
import com.spoolin.media.features.processing.ProcessingBounds
import com.spoolin.media.operations.receipts.ReceiptWork
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import java.security.MessageDigest
import java.sql.Timestamp
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.UploadChunk
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * gRPC Upload(stream UploadChunk) → Outcome implementation (§5.2.9 / §7.6 / ATDD-254-T1-R01).
 *
 * Stream protocol:
 *  1. First message must be an [UploadHeader] with context, actor, declared size, asset UUID, SHA-256 and intent.
 *  2. Subsequent messages carry raw [bytes data] chunks.
 *
 * On completion:
 *  - Exact byte count and SHA-256 are verified.
 *  - Asset row is inserted in RECEIVING state inside a committed transaction.
 *  - Processing work is committed atomically, transitioning to PROCESSING.
 *  - Returns PENDING Outcome for async processing.
 *
 * Failure cases:
 *  - Header missing → INVALID_ARGUMENT.
 *  - Actor identity absent → PERMISSION_DENIED.
 *  - Bounds exceeded / cancellation / checksum mismatch → asset REJECTED, INVALID_ARGUMENT.
 *  - Validation failure → INVALID_ARGUMENT with safe error code in problem field.
 */
class UploadRpc(
    private val dataSource: DataSource,
    private val quarantineStore: QuarantineStore,
    private val policyRevision: String,
    private val clock: Clock = Clock.systemUTC(),
    private val catalog: com.spoolin.media.features.asset.CatalogAttachments? = null,
) : MediaServiceGrpc.MediaServiceImplBase() {
    private val workDeadlineOffset: Duration = Duration.ofMinutes(5)

    override fun upload(response: StreamObserver<Outcome>): StreamObserver<UploadChunk> =
        object : StreamObserver<UploadChunk> {
            private var header: HeaderState? = null
            private var sink: ChunkSink? = null
            private var stagingPath: Path? = null
            private var stream: OutputStream? = null
            private var failed = false
            private var acceptedJob = false

            override fun onNext(chunk: UploadChunk) {
                if (failed) return
                try {
                    when (chunk.partCase) {
                        UploadChunk.PartCase.HEADER -> handleHeader(chunk)
                        UploadChunk.PartCase.DATA -> handleData(chunk)
                        UploadChunk.PartCase.PART_NOT_SET ->
                            fail(response, Status.INVALID_ARGUMENT, null, "INVALID_CHUNK")
                        else -> fail(response, Status.INVALID_ARGUMENT, null, "INVALID_CHUNK")
                    }
                } catch (e: io.grpc.StatusRuntimeException) {
                    fail(response, e.status, header?.assetId, e.status.description ?: "UPLOAD_REJECTED")
                } catch (e: Exception) {
                    fail(response, Status.INTERNAL, null, "UPLOAD_ERROR")
                }
            }

            override fun onError(t: Throwable) {
                failed = true
                sink?.cancel()
                cleanupStaging()
                if (header != null && !acceptedJob) {
                    rejectAsset(header!!.assetId, "UPLOAD_CANCELLED")
                }
            }

            override fun onCompleted() {
                if (failed) return
                val h = header
                val s = sink
                if (h == null || s == null) {
                    fail(response, Status.INVALID_ARGUMENT, null, "MISSING_HEADER")
                    return
                }
                try {
                    stream?.flush()
                    stream?.close()
                    stream = null
                    val completedResult = s.complete()
                    if (completedResult is ChunkSink.CompleteResult.Rejected) {
                        rejectAndRespond(response, h.assetId, completedResult.errorCode)
                        return
                    }
                    val completed = completedResult as ChunkSink.CompleteResult.Accepted

                    // Validate format from staged bytes (content-based, not MIME) (ATDD-159)
                    val stagingBytes = Files.readAllBytes(stagingPath!!)
                    val validationResult =
                        ImageValidator.validate(
                            ByteArrayInputStream(stagingBytes),
                            ProcessingBounds.MAX_INPUT_BYTES,
                        )
                    if (validationResult is ImageValidator.ValidationResult.Invalid) {
                        rejectAndRespond(response, h.assetId, validationResult.errorCode)
                        cleanupStaging()
                        return
                    }

                    // Commit asset PROCESSING + durable work atomically (§7.6 / ATDD-250)
                    val workId =
                        WorkRequest(
                            "upload-process",
                            "MEDIA_PROCESS",
                            h.assetId,
                            clock.instant().plus(workDeadlineOffset),
                        ).id(h.uploadOperationId)
                    val now = clock.instant()
                    val accepted = commitProcessing(h, completed, workId, now)
                    if (!accepted) {
                        rejectAndRespond(response, h.assetId, "ASSET_CONFLICT")
                        cleanupStaging()
                        return
                    }

                    acceptedJob = true
                    response.onNext(pendingOutcome(h.uploadOperationId))
                    response.onCompleted()
                } catch (e: io.grpc.StatusRuntimeException) {
                    fail(response, e.status, h.assetId, e.status.description ?: "UPLOAD_REJECTED")
                } catch (e: Exception) {
                    fail(response, Status.INTERNAL, h.assetId, "UPLOAD_ERROR")
                }
            }

            private fun handleHeader(chunk: UploadChunk) {
                if (header != null) {
                    fail(response, Status.INVALID_ARGUMENT, null, "DUPLICATE_HEADER")
                    return
                }
                val actor = ActorRpc.current()
                if (actor == null) {
                    fail(response, Status.PERMISSION_DENIED, null, "MISSING_ACTOR")
                    return
                }
                val h = chunk.header
                val assetId = parseUuid(h.assetId)
                val uploadOpId = parseUuid(h.context.operationId)
                if (assetId == null || uploadOpId == null) {
                    fail(response, Status.INVALID_ARGUMENT, null, "INVALID_IDENTITY")
                    return
                }
                val intentId = parseUuid(h.intentId)
                if (intentId == null) {
                    fail(response, Status.INVALID_ARGUMENT, null, "INVALID_INTENT")
                    return
                }
                verifyIntent(intentId, assetId, actor.accountId)
                val checksum = h.inputSha256.toByteArray()
                val newSink =
                    ChunkSink(
                        outputStream = openStaging(assetId),
                        declaredBytes = h.inputBytes,
                        declaredSha256 = checksum,
                    )
                val headerCheck = newSink.validateHeader()
                if (headerCheck is ChunkSink.WriteResult.Rejected) {
                    fail(response, Status.INVALID_ARGUMENT, null, headerCheck.errorCode)
                    return
                }
                // Reserve the parent receipt before its immediate asset foreign key.
                val inserted =
                    dataSource.committed { connection ->
                        val requestHash = MessageDigest.getInstance("SHA-256").digest(h.toByteArray())
                        check(connection.executeBound(
                            "INSERT INTO operation_receipt " +
                                "(id,actor_scope,kind,request_hash,state,correlation_id,created_at,expires_at) " +
                                "VALUES (?,?,'UPLOAD',?,'PENDING',?,?,?) ON CONFLICT (id) DO NOTHING",
                            uploadOpId, "account:${actor.accountId}", requestHash, uploadOpId,
                            Timestamp.from(clock.instant()),
                            Timestamp.from(clock.instant().plus(Duration.ofDays(7))),
                        ) == 1) { "Upload identity already exists; reconcile its receipt" }
                        AssetSql.insertReceiving(
                            connection,
                            assetId,
                            uploadOpId,
                            actor.accountId,
                            h.inputBytes,
                            policyRevision,
                        )
                    }
                if (inserted != 1) {
                    fail(response, Status.ALREADY_EXISTS, null, "DUPLICATE_UPLOAD")
                    return
                }
                header = HeaderState(assetId, uploadOpId, actor.accountId, intentId)
                sink = newSink
            }

            private fun handleData(chunk: UploadChunk) {
                val s =
                    sink ?: run {
                        fail(response, Status.INVALID_ARGUMENT, null, "DATA_BEFORE_HEADER")
                        return
                    }
                val data = chunk.data.toByteArray()
                val result = s.writeChunk(data)
                if (result is ChunkSink.WriteResult.Rejected) {
                    rejectAndRespond(response, header!!.assetId, result.errorCode)
                }
            }

            private fun openStaging(assetId: UUID): OutputStream {
                val path = quarantineStore.stagingPath(assetId)
                stagingPath = path
                val out = quarantineStore.openForWrite(path)
                stream = out
                return out
            }

            private fun rejectAndRespond(
                responseObserver: StreamObserver<Outcome>,
                assetId: UUID,
                code: String,
            ) {
                failed = true
                rejectAsset(assetId, code)
                cleanupStaging()
                fail(responseObserver, Status.INVALID_ARGUMENT, null, code)
            }

            private fun rejectAsset(
                assetId: UUID,
                code: String,
            ) {
                runCatching {
                    dataSource.committed { connection ->
                        AssetSql.markRejected(connection, assetId, code)
                        connection.executeBound(
                            "UPDATE operation_receipt SET state='FAILED',error_code=? " +
                                "WHERE id=(SELECT upload_operation_id FROM asset WHERE id=?) AND state='PENDING'",
                            "FIELD_INVALID", assetId,
                        )
                    }
                }
            }

            private fun cleanupStaging() {
                runCatching { stream?.close() }
                if (!acceptedJob) stagingPath?.let { runCatching { quarantineStore.deleteQuarantine(it) } }
            }

            private fun commitProcessing(
                h: HeaderState,
                completed: ChunkSink.CompleteResult.Accepted,
                workId: UUID,
                now: Instant,
            ): Boolean {
                verifyIntent(h.intentId, h.assetId, h.actorId)
                val stagingKey = "${ProcessingBounds.STAGING_SUBDIR}/${h.assetId}"
                return dataSource.committed { connection ->
                    val workReq = WorkRequest(
                        "upload-process", "MEDIA_PROCESS", h.assetId, now.plus(workDeadlineOffset),
                    )
                    check(workReq.id(h.uploadOperationId) == workId)
                    ReceiptWork.enqueue(connection, h.uploadOperationId, h.uploadOperationId, workReq, now)
                    check(connection.executeBound(
                        "UPDATE durable_work SET payload=jsonb_set(payload,'{intentId}',to_jsonb(?::text)) WHERE id=?",
                        h.intentId.toString(), workId,
                    ) == 1)
                    check(AssetSql.acceptProcessing(
                        connection, h.assetId, stagingKey, completed.inputHash, workId, now,
                    ) == 1) { "Asset processing acceptance conflicted" }
                    true
                }
            }

            private fun fail(
                responseObserver: StreamObserver<Outcome>,
                status: Status,
                assetId: UUID?,
                code: String,
            ) {
                failed = true
                if (assetId != null && !acceptedJob) rejectAsset(assetId, code)
                cleanupStaging()
                responseObserver.onError(status.withDescription(code).asRuntimeException())
            }
        }

    private fun verifyIntent(intentId: UUID, assetId: UUID, actorId: UUID) {
        val authority = catalog ?: throw Status.UNAVAILABLE.withDescription("ATTACHMENT_AUTHORITY_UNAVAILABLE")
            .asRuntimeException()
        val intent = authority.find(intentId)
        val bound = intent.holdId == intentId.toString() && intent.assetId == assetId.toString() &&
            intent.hasActorId() && intent.actorId == actorId.toString() && intent.state == "PREPARING"
        if (!bound) throw Status.PERMISSION_DENIED.withDescription("UPLOAD_INTENT_INVALID").asRuntimeException()
    }

    private fun pendingOutcome(operationId: UUID): Outcome =
        Outcome
            .newBuilder()
            .setOperationId(operationId.toString())
            .setState("PENDING")
            .setResultExpired(false)
            .build()

    private fun parseUuid(value: String): UUID? =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }.getOrNull()
}

private data class HeaderState(
    val assetId: UUID,
    val uploadOperationId: UUID,
    val actorId: UUID,
    val intentId: UUID,
)
