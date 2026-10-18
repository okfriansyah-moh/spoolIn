package com.spoolin.core.features.artworkproxy

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.ActorTarget
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.support.identity.ActorRpc
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.stub.MetadataUtils
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

private const val ACCESS_SECONDS = 3L
private const val READ_SECONDS = 10L
private const val RESOLVE_ARTWORK = "/spoolin.v1.CatalogService/ResolveArtworkAccess"

class ArtworkReadProxy(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val media: MediaServiceGrpc.MediaServiceBlockingStub,
    private val issuer: ActorIssuer?,
) {
    data class VerifiedAsset(
        val path: Path,
        val contentType: String,
        val bytes: Long,
    ) : AutoCloseable {
        override fun close() {
            ArtworkScratch.delete(path)
        }
    }

    private fun request(
        assetId: UUID,
        management: Boolean,
        context: BrowserContext,
    ): Spoolin.AssetReadRequest {
        val request =
            Spoolin.ArtworkAccessRequest
                .newBuilder()
                .setAssetId(assetId.toString())
                .setManagement(management)
                .build()
        val metadata = metadata(management, context)
        val grant =
            catalog
                .withDeadlineAfter(ACCESS_SECONDS, TimeUnit.SECONDS)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .resolveArtworkAccess(request)
        ArtworkReadValidation.grant(grant, assetId)
        return Spoolin.AssetReadRequest
            .newBuilder()
            .setAssetId(grant.assetId)
            .setHoldId(grant.holdId)
            .setCatalogVersion(grant.catalogVersion)
            .build()
    }

    private fun metadata(
        management: Boolean,
        context: BrowserContext,
    ): Metadata {
        val metadata = Metadata()
        if (management) {
            val principal = context.principal
            if (principal == null || principal.role.name != "MAINTAINER") {
                throw Status.PERMISSION_DENIED.asRuntimeException()
            }
            val target = ActorTarget("catalog", RESOLVE_ARTWORK, null, context.correlation)
            val token = issuer?.issue(principal, target) ?: throw Status.UNAVAILABLE.asRuntimeException()
            metadata.put(ActorRpc.metadata, token)
        }
        return metadata
    }

    fun metadata(
        assetId: UUID,
        management: Boolean,
        context: BrowserContext,
    ): ArtworkMetadata {
        val read = request(assetId, management, context)
        val cancellation = Context.current().withCancellation()
        return try {
            cancellation.call(
                Callable {
                    val chunks = media.withDeadlineAfter(ACCESS_SECONDS, TimeUnit.SECONDS).readAsset(read)
                    val header = ArtworkTransfer.header(chunks)
                    ArtworkMetadata(assetId, management, header.width, header.height)
                },
            )
        } finally {
            cancellation.cancel(null)
        }
    }

    fun read(
        assetId: UUID,
        management: Boolean,
        context: BrowserContext,
    ): VerifiedAsset {
        val read = request(assetId, management, context)
        val cancellation = Context.current().withCancellation()
        return try {
            cancellation.call(
                Callable {
                    ArtworkTransfer.receive(media.withDeadlineAfter(READ_SECONDS, TimeUnit.SECONDS).readAsset(read))
                },
            )
        } finally {
            cancellation.cancel(null)
        }
    }
}
