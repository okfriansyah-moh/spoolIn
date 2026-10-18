package com.spoolin.core.features.artworkproxy

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.ActorTarget
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.support.identity.ActorRpc
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.stub.MetadataUtils
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class ArtworkReadProxy(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val media: MediaServiceGrpc.MediaServiceBlockingStub,
    private val issuer: ActorIssuer?,
) {
    data class VerifiedAsset(val path: Path, val contentType: String, val bytes: Long) : AutoCloseable {
        override fun close() { ArtworkScratch.delete(path) }
    }

    fun read(assetId: UUID, management: Boolean, context: BrowserContext): VerifiedAsset {
        val request = Spoolin.ArtworkAccessRequest.newBuilder().setAssetId(assetId.toString()).setManagement(management).build()
        val principal = context.principal
        val metadata = Metadata()
        if (management) {
            if (principal == null || principal.role.name != "MAINTAINER") throw Status.PERMISSION_DENIED.asRuntimeException()
            val token = issuer?.issue(principal, ActorTarget("catalog", "/spoolin.v1.CatalogService/ResolveArtworkAccess", null, context.correlation))
                ?: throw Status.UNAVAILABLE.asRuntimeException()
            metadata.put(ActorRpc.metadata, token)
        }
        val grant = catalog.withDeadlineAfter(3, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).resolveArtworkAccess(request)
        if (grant.assetId != assetId.toString() || grant.catalogVersion < 0 ||
            runCatching { UUID.fromString(grant.holdId).toString() == grant.holdId &&
                UUID.fromString(grant.titleId).toString() == grant.titleId }.getOrDefault(false).not()) {
            throw Status.DATA_LOSS.asRuntimeException()
        }
        val read = Spoolin.AssetReadRequest.newBuilder().setAssetId(grant.assetId).setHoldId(grant.holdId)
            .setCatalogVersion(grant.catalogVersion).build()
        val cancellation = io.grpc.Context.current().withCancellation()
        return try { cancellation.call(java.util.concurrent.Callable { receive(media.withDeadlineAfter(10, TimeUnit.SECONDS).readAsset(read)) }) }
        finally { cancellation.cancel(null) }
    }

    private fun receive(chunks: Iterator<Spoolin.AssetReadChunk>): VerifiedAsset {
        if (!chunks.hasNext()) throw Status.DATA_LOSS.asRuntimeException()
        val first = chunks.next()
        if (!first.hasHeader()) throw Status.DATA_LOSS.asRuntimeException()
        val header = first.header
        val contentType = contentType(header)
        val path = ArtworkScratch.create()
        try {
            val hash = MessageDigest.getInstance("SHA-256")
            var bytes = 0L
            Files.newOutputStream(path).use { output ->
                while (chunks.hasNext()) {
                    val chunk = chunks.next()
                    if (!chunk.hasData() || chunk.data.size() !in 1..65536) throw Status.DATA_LOSS.asRuntimeException()
                    bytes += chunk.data.size()
                    if (bytes > header.outputBytes) throw Status.DATA_LOSS.asRuntimeException()
                    val data = chunk.data.toByteArray()
                    hash.update(data)
                    output.write(data)
                }
            }
            if (bytes != header.outputBytes || !MessageDigest.isEqual(hash.digest(), header.contentHash.toByteArray())) throw Status.DATA_LOSS.asRuntimeException()
            return VerifiedAsset(path, contentType, bytes)
        } catch (failure: Exception) { ArtworkScratch.delete(path); throw failure }
    }

    private fun contentType(header: Spoolin.AssetReadHeader): String {
        if (header.outputBytes !in 1L..8388608L || header.width !in 1..8192 || header.height !in 1..8192 ||
            header.width.toLong() * header.height > 40000000 || header.contentHash.size() != 32) throw Status.DATA_LOSS.asRuntimeException()
        return when (header.format.lowercase(java.util.Locale.ROOT)) {
            "jpeg", "image/jpeg" -> "image/jpeg"
            "png", "image/png" -> "image/png"
            "webp", "image/webp" -> "image/webp"
            else -> throw Status.DATA_LOSS.asRuntimeException()
        }
    }
}
