package com.spoolin.catalog.features.artwork

import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.SafeResult
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.ArtworkCancelOutcome
import spoolin.v1.Spoolin.CancelArtworkRequest
import spoolin.v1.Spoolin.Hold
import spoolin.v1.Spoolin.Outcome
import java.util.UUID
import javax.sql.DataSource

class ArtworkRpc(
    private val dataSource: DataSource,
    private val receipts: OwnerReceipts,
) {
    fun beginArtwork(request: Hold, response: StreamObserver<Outcome>) {
        try {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!Permissions.management(claims.role)) throw Status.PERMISSION_DENIED.asRuntimeException()
            val operationId = parseUuid(request.context.operationId)
            val titleId = parseUuid(request.titleId)
            val assetId = parseUuid(request.assetId)
            val expectedVersion = request.expectedVersion
            val intent =
                OperationIntent(
                    operationId,
                    "account:${claims.accountId}",
                    "BeginArtwork",
                    request.context.policyRevision,
                    listOf("titleId" to titleId.toString(), "assetId" to assetId.toString()),
                )
            val correlationId = parseUuid(request.context.correlationId)
            val outcome =
                receipts.execute(intent, correlationId, true) { conn ->
                    ArtworkAttachmentSql.insertPreparing(
                        conn,
                        operationId,
                        titleId,
                        claims.accountId,
                        expectedVersion,
                        assetId,
                        request.context.policyRevision,
                        null,
                    )
                    SafeResult(resultId = operationId, version = expectedVersion + 1)
                }
            response.onNext(outcomeProto(outcome))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun cancelArtwork(request: CancelArtworkRequest, response: StreamObserver<ArtworkCancelOutcome>) {
        try {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            val uploadOp = parseUuid(request.uploadOperationId)
            dataSource.connection.use { conn ->
                conn.autoCommit = false
                try {
                    ArtworkAttachmentSql.markAborted(conn, uploadOp)
                    conn.commit()
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                }
            }
            response.onNext(
                ArtworkCancelOutcome.newBuilder()
                    .setCancelled(true)
                    .setOperation(
                        Outcome.newBuilder()
                            .setOperationId(request.context.operationId)
                            .setState("SUCCEEDED")
                            .build(),
                    )
                    .setUploadOperation(
                        Outcome.newBuilder()
                            .setOperationId(uploadOp.toString())
                            .setState("CANCELLED")
                            .build(),
                    )
                    .build(),
            )
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    private fun outcomeProto(receipt: com.spoolin.support.receipts.ReceiptOutcome): Outcome {
        val builder =
            Outcome.newBuilder()
                .setOperationId(receipt.id.toString())
                .setState(receipt.state.name)
                .setResultExpired(receipt.resultExpired)
        receipt.result?.let { result ->
            builder.resultId = result.resultId.toString()
            result.version?.let { builder.version = it }
        }
        return builder.build()
    }

    private fun parseUuid(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.asRuntimeException() }
}
