package com.spoolin.media.operations.receipts

import com.spoolin.support.receipts.ReceiptAuthority
import com.spoolin.support.receipts.ReceiptOutcome
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.Outcome
import java.util.UUID

class ReceiptRpc(
    private val authority: ReceiptAuthority,
    private val lookup: (UUID, String) -> ReceiptOutcome?,
) : MediaServiceGrpc.MediaServiceImplBase() {
    override fun getOperation(
        request: Id,
        response: StreamObserver<Outcome>,
    ) {
        try {
            val actor = authority.actorScope() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            val id = identity(request.id)
            val receipt = lookup(id, actor) ?: throw Status.NOT_FOUND.asRuntimeException()
            response.onNext(protobuf(receipt))
            response.onCompleted()
        } catch (failure: io.grpc.StatusRuntimeException) {
            response.onError(failure)
        }
    }

    private fun identity(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.asRuntimeException() }

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
}
