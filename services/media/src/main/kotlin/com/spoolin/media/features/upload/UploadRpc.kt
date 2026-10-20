package com.spoolin.media.features.upload

import com.spoolin.media.features.asset.AssetSql
import com.spoolin.media.features.processing.ImageValidator
import com.spoolin.media.features.processing.ProcessingBounds
import com.spoolin.media.operations.receipts.ReceiptWork
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.UploadChunk
import spoolin.v1.Spoolin.UploadHeader
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Timestamp
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
    private val workDeadlineOffset: Duration = Duration.ofMinutes(WORK_DEADLINE_MINUTES)

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
                protectUpload({ failure ->
                    fail(response, failure.status, header?.assetId, failure.status.description ?: "UPLOAD_REJECTED")
                }, { fail(response, Status.INTERNAL, null, "UPLOAD_ERROR") }) {
                    when (chunk.partCase) {
                        UploadChunk.PartCase.HEADER -> handleHeader(chunk)
                        UploadChunk.PartCase.DATA -> handleData(chunk)
                        UploadChunk.PartCase.PART_NOT_SET ->
                            fail(response, Status.INVALID_ARGUMENT, null, "INVALID_CHUNK")
                        else -> fail(response, Status.INVALID_ARGUMENT, null, "INVALID_CHUNK")
                    }
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
                protectUpload({ failure ->
                    fail(response, failure.status, header?.assetId, failure.status.description ?: "UPLOAD_REJECTED")
                }, { fail(response, Status.INTERNAL, h.assetId, "UPLOAD_ERROR") }) { complete(h, s) }
            }

            private fun complete(
                h: HeaderState,
                sink: ChunkSink,
            ) {
                stream?.flush()
                stream?.close()
                stream = null
                when (val completed = sink.complete()) {
                    is ChunkSink.CompleteResult.Rejected -> rejectAndRespond(response, h.assetId, completed.errorCode)
                    is ChunkSink.CompleteResult.Accepted -> validateCompleted(h, completed)
                }
            }

            private fun validateCompleted(
                h: HeaderState,
                completed: ChunkSink.CompleteResult.Accepted,
            ) {
                val stagingBytes = Files.readAllBytes(stagingPath!!)
                val validation =
                    ImageValidator.validate(
                        ByteArrayInputStream(stagingBytes),
                        ProcessingBounds.MAX_INPUT_BYTES,
                    )
                if (validation is ImageValidator.ValidationResult.Invalid) {
                    rejectAndRespond(response, h.assetId, validation.errorCode)
                    cleanupStaging()
                } else {
                    val workId =
                        WorkRequest(
                            "upload-process",
                            "MEDIA_PROCESS",
                            h.assetId,
                            clock.instant().plus(workDeadlineOffset),
                        ).id(h.uploadOperationId)
                    val accepted = commitProcessing(h, completed, workId, clock.instant())
                    if (!accepted) {
                        rejectAndRespond(response, h.assetId, "ASSET_CONFLICT")
                        cleanupStaging()
                    } else {
                        acceptedJob = true
                        response.onNext(pendingOutcome(h.uploadOperationId))
                        response.onCompleted()
                    }
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
                val state = headerState(h, actor.accountId)
                verifyIntent(state.intentId, state.assetId, state.actorId)
                val newSink = ChunkSink(openStaging(state.assetId), h.inputBytes, h.inputSha256.toByteArray())
                val headerCheck = newSink.validateHeader()
                if (headerCheck is ChunkSink.WriteResult.Rejected) {
                    fail(response, Status.INVALID_ARGUMENT, null, headerCheck.errorCode)
                } else {
                    val inserted = reserveReceiving(h, state.actorId, state.assetId, state.uploadOperationId)
                    if (inserted != 1) {
                        fail(response, Status.ALREADY_EXISTS, null, "DUPLICATE_UPLOAD")
                    } else {
                        header = state
                        sink = newSink
                    }
                }
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

            private fun cleanupStaging() {
                runCatching { stream?.close() }
                if (!acceptedJob) stagingPath?.let { runCatching { quarantineStore.deleteQuarantine(it) } }
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

    private fun reserveReceiving(
        h: UploadHeader,
        actor: UUID,
        assetId: UUID,
        uploadOpId: UUID,
    ): Int =
        dataSource.committed { connection ->
            val requestHash = MessageDigest.getInstance("SHA-256").digest(h.toByteArray())
            check(
                connection.executeBound(
                    "INSERT INTO operation_receipt " +
                        "(id,actor_scope,kind,request_hash,state,correlation_id,created_at,expires_at) " +
                        "VALUES (?,?,'UPLOAD',?,'PENDING',?,?,?) ON CONFLICT (id) DO NOTHING",
                    uploadOpId,
                    "account:$actor",
                    requestHash,
                    uploadOpId,
                    Timestamp.from(clock.instant()),
                    Timestamp.from(clock.instant().plus(Duration.ofDays(UPLOAD_RECEIPT_RETENTION_DAYS))),
                ) == 1,
            ) { "Upload identity already exists; reconcile its receipt" }
            val inserted = AssetSql.insertReceiving(connection, assetId, uploadOpId, actor, h.inputBytes, policyRevision)
            val intent = catalog?.find(UUID.fromString(h.intentId))
            if (intent?.titleId == h.intentId) connection.executeBound("UPDATE asset SET provenance='{\"preparedPoster\":true}'::jsonb WHERE id=?",assetId)
            inserted
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
                    "FIELD_INVALID",
                    assetId,
                )
            }
        }
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
            val workReq =
                WorkRequest(
                    "upload-process",
                    "MEDIA_PROCESS",
                    h.assetId,
                    now.plus(workDeadlineOffset),
                )
            check(workReq.id(h.uploadOperationId) == workId)
            ReceiptWork.enqueue(connection, h.uploadOperationId, h.uploadOperationId, workReq, now)
            check(
                connection.executeBound(
                    "UPDATE durable_work SET payload=jsonb_set(payload,'{intentId}',to_jsonb(?::text)) WHERE id=?",
                    h.intentId.toString(),
                    workId,
                ) == 1,
            )
            check(
                AssetSql.acceptProcessing(
                    connection,
                    h.assetId,
                    stagingKey,
                    completed.inputHash,
                    workId,
                    now,
                ) == 1,
            ) { "Asset processing acceptance conflicted" }
            true
        }
    }

    private fun headerState(
        h: UploadHeader,
        actor: UUID,
    ): HeaderState {
        val asset = parseUuid(h.assetId)
        val operation = parseUuid(h.context.operationId)
        if (asset == null || operation == null) {
            throw Status.INVALID_ARGUMENT
                .withDescription("INVALID_IDENTITY")
                .asRuntimeException()
        }
        val intent =
            parseUuid(h.intentId) ?: throw Status.INVALID_ARGUMENT
                .withDescription("INVALID_INTENT")
                .asRuntimeException()
        return HeaderState(asset, operation, actor, intent)
    }

    private fun verifyIntent(
        intentId: UUID,
        assetId: UUID,
        actorId: UUID,
    ) {
        val authority =
            catalog ?: throw Status.UNAVAILABLE
                .withDescription("ATTACHMENT_AUTHORITY_UNAVAILABLE")
                .asRuntimeException()
        val intent = authority.find(intentId)
        val bound =
            intent.holdId == intentId.toString() && intent.assetId == assetId.toString() &&
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

private const val WORK_DEADLINE_MINUTES = 5L
private const val UPLOAD_RECEIPT_RETENTION_DAYS = 7L

private fun protectUpload(
    onStatus: (io.grpc.StatusRuntimeException) -> Unit,
    onFailure: () -> Unit,
    operation: () -> Unit,
) {
    try {
        operation()
    } catch (
        failure: io.grpc.StatusRuntimeException,
    ) {
        onStatus(failure)
    } catch (
        _: java.io.IOException,
    ) {
        onFailure()
    } catch (
        _: java.sql.SQLException,
    ) {
        onFailure()
    } catch (
        _: IllegalStateException,
    ) {
        onFailure()
    } catch (
        _: IllegalArgumentException,
    ) {
        onFailure()
    } catch (
        _: java.security.GeneralSecurityException,
    ) {
        onFailure()
    } catch (_: SecurityException) {
        onFailure()
    } catch (_: UnsupportedOperationException) {
        onFailure()
    }
}
