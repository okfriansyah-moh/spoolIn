package com.spoolin.core.features.artworkproxy

import com.google.protobuf.ByteString
import com.spoolin.support.identity.ActorRpc
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private const val UPLOAD_CHUNK_BYTES = 64 * 1024
private const val UPLOAD_TIMEOUT_SECONDS = 30L

internal object ArtworkUpload {
    fun upload(request: ArtworkUploadRequest): Spoolin.Outcome {
        val cancellation = Context.current().withCancellation()
        return try {
            cancellation.call(Callable { transmit(request) })
        } finally {
            cancellation.cancel(null)
        }
    }

    private fun transmit(request: ArtworkUploadRequest): Spoolin.Outcome {
        val response = UploadResponse()
        val meta = Metadata().apply { put(ActorRpc.metadata, request.actorToken) }
        val stub =
            request.media
                .withDeadlineAfter(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(meta))
        val stream = stub.upload(response)
        stream.onNext(
            Spoolin.UploadChunk
                .newBuilder()
                .setHeader(header(request))
                .build(),
        )
        sendBytes(request, stream, response)
        if (response.error.get() == null) stream.onCompleted()
        val outcome = response.await()
        if (outcome.operationId != request.uploadOperationId.toString()) throw Status.DATA_LOSS.asRuntimeException()
        return outcome
    }

    private fun sendBytes(
        request: ArtworkUploadRequest,
        stream: StreamObserver<Spoolin.UploadChunk>,
        response: UploadResponse,
    ) {
        Files.newInputStream(request.spool.path).use { input ->
            sendChunks(input, stream, response)
        }
    }

    private fun sendChunks(
        input: InputStream,
        stream: StreamObserver<Spoolin.UploadChunk>,
        response: UploadResponse,
    ) {
        val buffer = ByteArray(UPLOAD_CHUNK_BYTES)
        while (response.error.get() == null && !Context.current().isCancelled) {
            val count = input.read(buffer)
            if (count < 0) break
            stream.onNext(
                Spoolin.UploadChunk
                    .newBuilder()
                    .setData(ByteString.copyFrom(buffer, 0, count))
                    .build(),
            )
        }
    }

    private fun header(request: ArtworkUploadRequest): Spoolin.UploadHeader =
        Spoolin.UploadHeader
            .newBuilder()
            .setContext(
                Spoolin.Context
                    .newBuilder()
                    .setOperationId(request.uploadOperationId.toString())
                    .setCorrelationId(request.correlationId.toString())
                    .setPolicyRevision(request.policyRevision),
            ).setActorId(request.actorId.toString())
            .setInputBytes(request.spool.size)
            .setAssetId(request.assetId.toString())
            .setInputSha256(ByteString.copyFrom(request.spool.sha256))
            .setIntentId(request.intentId.toString())
            .build()
}

private class UploadResponse : StreamObserver<Spoolin.Outcome> {
    private val latch = CountDownLatch(1)
    private val outcome = AtomicReference<Spoolin.Outcome>()
    val error = AtomicReference<Throwable>()

    override fun onNext(value: Spoolin.Outcome) {
        if (!outcome.compareAndSet(null, value)) onError(Status.DATA_LOSS.asRuntimeException())
    }

    override fun onError(t: Throwable) {
        error.set(t)
        latch.countDown()
    }

    override fun onCompleted() {
        latch.countDown()
    }

    fun await(): Spoolin.Outcome {
        if (!latch.await(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw Status.DEADLINE_EXCEEDED.withDescription("UPLOAD_TIMEOUT").asRuntimeException()
        }
        propagateFailure()
        return outcome.get() ?: throw Status.INTERNAL.withDescription("UPLOAD_EMPTY").asRuntimeException()
    }

    private fun propagateFailure() {
        error.get()?.let { throw it }
    }
}
