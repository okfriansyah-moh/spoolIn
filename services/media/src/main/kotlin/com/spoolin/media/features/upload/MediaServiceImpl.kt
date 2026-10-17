package com.spoolin.media.features.upload

import com.spoolin.media.operations.receipts.OwnerReceiptReads
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.ReceiptAuthority
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.UploadChunk
import java.util.UUID
import javax.sql.DataSource

/**
 * Combined MediaService implementation that dispatches Upload and receipt operations.
 *
 * Wired by Application.kt as the single registered [MediaServiceGrpc.MediaServiceImplBase] bean.
 * Each method delegates to its responsible handler, preserving independent testability.
 */
class MediaServiceImpl(
    private val upload: UploadRpc,
    private val holds: com.spoolin.media.features.hold.HoldRpc,
    private val receiptReads: OwnerReceiptReads,
) : MediaServiceGrpc.MediaServiceImplBase() {
    override fun upload(response: StreamObserver<Outcome>): StreamObserver<UploadChunk> = upload.upload(response)

    override fun reserve(
        request: spoolin.v1.Spoolin.Hold,
        response: StreamObserver<spoolin.v1.Spoolin.Attachment>,
    ) = holds.reserve(request, response)

    override fun confirm(
        request: spoolin.v1.Spoolin.Hold,
        response: StreamObserver<spoolin.v1.Spoolin.Attachment>,
    ) = holds.confirm(request, response)

    override fun release(
        request: spoolin.v1.Spoolin.Hold,
        response: StreamObserver<spoolin.v1.Spoolin.Attachment>,
    ) = holds.release(request, response)

    override fun getOperation(
        request: Id,
        response: StreamObserver<Outcome>,
    ) {
        try {
            val actor = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            val id = parseUuid(request.id)
            val receipt =
                receiptReads.find(id, "account:${actor.accountId}")
                    ?: throw Status.NOT_FOUND.asRuntimeException()
            response.onNext(operationOutcome(receipt))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    private fun operationOutcome(receipt: com.spoolin.support.receipts.ReceiptOutcome): Outcome {
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
        receipt.errorCode?.let { code ->
            builder.problem =
                spoolin.v1.Spoolin.Problem
                    .newBuilder()
                    .setCode(code)
                    .build()
        }
        return builder.build()
    }

    private fun parseUuid(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.asRuntimeException() }
}
