package com.spoolin.core.features.artworkproxy

import io.grpc.Status
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.util.Base64
import java.util.UUID

/** Shares the bounded Media ingestion path with ordinary artwork uploads. */
class PreparedPosterUpload(private val media: MediaServiceGrpc.MediaServiceStub, private val maximumBytes: Long) {
    fun prepare(
        request: Spoolin.PrepareMoviePosterRequest,
        actorId: UUID,
        token: (UUID, UUID) -> String,
        register: () -> Spoolin.PreparedPoster,
    ): Spoolin.PreparedPoster {
        if (request.imageBytes.isEmpty || request.imageBytes.size().toLong() > minOf(maximumBytes, 5_242_880L)) {
            throw Status.INVALID_ARGUMENT.withDescription("FILE_INVALID").asRuntimeException()
        }
        val prepared = register()
        if (prepared.state != "PREPARING") return prepared
        val operation = UUID.fromString(request.context.operationId)
        val correlation = UUID.fromString(request.context.correlationId)
        ArtworkSpool.decode(Base64.getEncoder().encodeToString(request.imageBytes.toByteArray()), maximumBytes).use { spool ->
            val outcome = ArtworkUpload.upload(
                ArtworkUploadRequest(media, token(operation, correlation), actorId, operation, correlation,
                    request.context.policyRevision, UUID.fromString(prepared.assetId), UUID.fromString(prepared.refId), spool),
            )
            if (outcome.state == "FAILED" || outcome.state == "EXPIRED") {
                throw Status.INVALID_ARGUMENT.withDescription(if (outcome.hasProblem()) outcome.problem.code else "FILE_INVALID")
                    .asRuntimeException()
            }
        }
        return register()
    }
}
