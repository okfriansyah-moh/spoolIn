package com.spoolin.core.features.artworkproxy

import com.google.protobuf.ByteString
import com.spoolin.core.features.session.SessionPrincipal
import io.grpc.Metadata
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal object ArtworkUpload {
    fun upload(
        media: MediaServiceGrpc.MediaServiceStub,
        actorToken: String,
        principal: SessionPrincipal,
        uploadOperationId: UUID,
        correlationId: UUID,
        policyRevision: String,
        assetId: UUID,
        content: ByteArray,
    ): Spoolin.Outcome {
        val digest = MessageDigest.getInstance("SHA-256").digest(content)
        val header =
            Spoolin.UploadHeader.newBuilder()
                .setContext(
                    Spoolin.Context.newBuilder()
                        .setOperationId(uploadOperationId.toString())
                        .setCorrelationId(correlationId.toString())
                        .setPolicyRevision(policyRevision)
                        .build(),
                )
                .setActorId(principal.accountId.toString())
                .setInputBytes(content.size.toLong())
                .setAssetId(assetId.toString())
                .setInputSha256(ByteString.copyFrom(digest))
                .setIntentId("artwork")
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
        val meta = Metadata().apply { put(com.spoolin.support.identity.ActorRpc.metadata, actorToken) }
        val stub = media.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(meta))
        val request = stub.upload(response)
        request.onNext(Spoolin.UploadChunk.newBuilder().setHeader(header).build())
        val chunkSize = 64 * 1024
        var offset = 0
        while (offset < content.size) {
            val end = minOf(offset + chunkSize, content.size)
            request.onNext(
                Spoolin.UploadChunk.newBuilder()
                    .setData(ByteString.copyFrom(content, offset, end - offset))
                    .build(),
            )
            offset = end
        }
        request.onCompleted()
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw StatusRuntimeException(io.grpc.Status.DEADLINE_EXCEEDED.withDescription("UPLOAD_TIMEOUT"))
        }
        error.get()?.let { throw it }
        return outcome.get() ?: throw StatusRuntimeException(io.grpc.Status.INTERNAL.withDescription("UPLOAD_EMPTY"))
    }
}
