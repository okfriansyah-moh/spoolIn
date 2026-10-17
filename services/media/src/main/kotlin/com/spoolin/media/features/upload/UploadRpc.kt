package com.spoolin.media.features.upload

import com.spoolin.media.features.asset.AssetSql
import com.spoolin.media.features.asset.AssetState
import com.spoolin.media.features.processing.ImageValidator
import com.spoolin.media.features.processing.ProcessingBounds
import com.spoolin.media.operations.receipts.ReceiptWork
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
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
) : MediaServiceGrpc.MediaServiceImplBase() {
    private val workDeadlineOffset: Duration = Duration.ofMinutes(5)

    override fun upload(response: StreamObserver<Outcome>): StreamObserver<UploadChunk> =
        object : StreamObserver<UploadChunk> {
            private var header: HeaderState? = null
            private var sink: ChunkSink? = null
            private var stagingPath: Path? = null
            private var stream: OutputStream? = null
            private var failed = false

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
                } catch (e: Exception) {
                    fail(response, Status.INTERNAL, null, "UPLOAD_ERROR")
                }
            }

            override fun onError(t: Throwable) {
                failed = true
                sink?.cancel()
                cleanupStaging()
                if (header != null) {
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
                    val workId = WorkRequest("upload-process", "MEDIA_PROCESS", h.assetId,
                        clock.instant().plus(workDeadlineOffset)).id(h.uploadOperationId)
                    val now = clock.instant()
                    val accepted = commitProcessing(h, completed, workId, now)
                    if (!accepted) {
                        rejectAndRespond(response, h.assetId, "ASSET_CONFLICT")
                        cleanupStaging()
                        return
                    }

                    response.onNext(pendingOutcome(h.uploadOperationId))
                    response.onCompleted()
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
                val checksum = h.inputSha256.toByteArray()
                val newSink = ChunkSink(
                    outputStream = openStaging(assetId),
                    declaredBytes = h.inputBytes,
                    declaredSha256 = checksum,
                )
                val headerCheck = newSink.validateHeader()
                if (headerCheck is ChunkSink.WriteResult.Rejected) {
                    fail(response, Status.INVALID_ARGUMENT, null, headerCheck.errorCode)
                    return
                }
                // Insert asset in RECEIVING (idempotent via upload_operation_id UNIQUE)
                val inserted = dataSource.committed { connection ->
                    AssetSql.insertReceiving(
                        connection, assetId, uploadOpId,
                        actor.accountId, h.inputBytes, policyRevision,
                    )
                }
                if (inserted != 1) {
                    fail(response, Status.ALREADY_EXISTS, null, "DUPLICATE_UPLOAD")
                    return
                }
                header = HeaderState(assetId, uploadOpId, actor.accountId)
                sink = newSink
            }

            private fun handleData(chunk: UploadChunk) {
                val s = sink ?: run {
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

            private fun rejectAndRespond(responseObserver: StreamObserver<Outcome>, assetId: UUID, code: String) {
                failed = true
                rejectAsset(assetId, code)
                cleanupStaging()
                fail(responseObserver, Status.INVALID_ARGUMENT, null, code)
            }

            private fun rejectAsset(assetId: UUID, code: String) {
                runCatching {
                    dataSource.committed { connection ->
                        AssetSql.markRejected(connection, assetId, code)
                    }
                }
            }

            private fun cleanupStaging() {
                runCatching { stream?.close() }
                stagingPath?.let { runCatching { quarantineStore.deleteQuarantine(it) } }
            }

            private fun commitProcessing(
                h: HeaderState,
                completed: ChunkSink.CompleteResult.Accepted,
                workId: UUID,
                now: Instant,
            ): Boolean {
                val stagingKey = "${ProcessingBounds.STAGING_SUBDIR}/${h.assetId}"
                return dataSource.committed { connection ->
                    val updated = AssetSql.acceptProcessing(
                        connection, h.assetId, stagingKey,
                        completed.inputHash, workId, now,
                    )
                    if (updated == 1) {
                        val workReq = WorkRequest(
                            "upload-process", "MEDIA_PROCESS", h.assetId,
                            now.plus(workDeadlineOffset),
                        )
                        ReceiptWork.enqueue(connection, h.uploadOperationId, h.uploadOperationId, workReq, now)
                    }
                    updated == 1
                }
            }

            private fun fail(
                responseObserver: StreamObserver<Outcome>,
                status: Status,
                assetId: UUID?,
                code: String,
            ) {
                failed = true
                if (assetId != null) rejectAsset(assetId, code)
                cleanupStaging()
                responseObserver.onError(status.withDescription(code).asRuntimeException())
            }
        }

    private fun pendingOutcome(operationId: UUID): Outcome =
        Outcome.newBuilder()
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
)
