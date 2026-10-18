package com.spoolin.core.features.artworkproxy

import com.google.protobuf.ByteString
import io.grpc.Metadata
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private const val UPLOAD_CHUNK_BYTES = 64 * 1024
private const val UPLOAD_TIMEOUT_SECONDS = 30L

internal object ArtworkUpload {
    fun upload(request: ArtworkUploadRequest): Spoolin.Outcome {
        val digest = request.spool.sha256
        val header =
            Spoolin.UploadHeader
                .newBuilder()
                .setContext(
                    Spoolin.Context
                        .newBuilder()
                        .setOperationId(request.uploadOperationId.toString())
                        .setCorrelationId(request.correlationId.toString())
                        .setPolicyRevision(request.policyRevision)
                        .build(),
                ).setActorId(request.principal.accountId.toString())
                .setInputBytes(request.spool.size)
                .setAssetId(request.assetId.toString())
                .setInputSha256(ByteString.copyFrom(digest))
                .setIntentId(request.intentId.toString())
                .build()
        val latch = CountDownLatch(1)
        val outcome = AtomicReference<Spoolin.Outcome>()
        val error = AtomicReference<Throwable>()
        val response =
            object : StreamObserver<Spoolin.Outcome> {
                override fun onNext(value: Spoolin.Outcome) {
                    outcome.set(value)
                }

                override fun onError(t: Throwable) {
                    error.set(t)
                    latch.countDown()
                }

                override fun onCompleted() {
                    latch.countDown()
                }
            }
        val meta = Metadata().apply { put(com.spoolin.support.identity.ActorRpc.metadata, request.actorToken) }
        val stub = request.media.withDeadlineAfter(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(meta))
        val stream = stub.upload(response)
        stream.onNext(
            Spoolin.UploadChunk
                .newBuilder()
                .setHeader(header)
                .build(),
        )
        java.nio.file.Files.newInputStream(request.spool.path).use { input ->
            val buffer = ByteArray(UPLOAD_CHUNK_BYTES)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (error.get() != null) break
                stream.onNext(Spoolin.UploadChunk.newBuilder().setData(ByteString.copyFrom(buffer, 0, count)).build())
            }
        }
        stream.onCompleted()
        return awaitUploadOutcome(latch, error, outcome)
    }

    @Suppress("ThrowsCount")
    private fun awaitUploadOutcome(
        latch: CountDownLatch,
        error: AtomicReference<Throwable>,
        outcome: AtomicReference<Spoolin.Outcome>,
    ): Spoolin.Outcome {
        if (!latch.await(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw StatusRuntimeException(
                io.grpc.Status.DEADLINE_EXCEEDED
                    .withDescription("UPLOAD_TIMEOUT"),
            )
        }
        error.get()?.let { throw it }
        return outcome.get()
            ?: throw StatusRuntimeException(
                io.grpc.Status.INTERNAL
                    .withDescription("UPLOAD_EMPTY"),
            )
    }
}
