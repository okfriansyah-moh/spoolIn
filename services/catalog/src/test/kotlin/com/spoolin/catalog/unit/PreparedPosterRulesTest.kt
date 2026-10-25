package com.spoolin.catalog.unit

import com.google.protobuf.ByteString
import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.features.proposal.*
import io.grpc.Status
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Instant

class PreparedPosterRulesTest {
    private fun poster() = Spoolin.PreparedPoster.newBuilder().setRefId(unitTarget.toString()).setAssetId(unitAsset.toString())
        .setContentHash("abc").setMediaType("image/png").setWidth(10).setHeight(20).setSourceUrl("https://example.com/poster")
        .setReuseBasis("CC0").setState("READY").build()
    private fun request() = Spoolin.PrepareMoviePosterRequest.newBuilder()
        .setContext(Spoolin.Context.newBuilder().setOperationId(unitOperation.toString()).setCorrelationId(unitCorrelation.toString()).setPolicyRevision("policy"))
        .setImageBytes(ByteString.copyFromUtf8("png-bytes")).setMediaType("image/png").setSourceUrl("https://example.com/poster").setReuseBasis("CC0").build()
    private fun service(db: JdbcRows, channel: GrpcReplies) = PreparedMoviePosters(db.source, MediaServiceGrpc.newBlockingStub(channel)) { CatalogWritePolicy.environment() }
    private fun stored(expiry: Instant = Instant.parse("2100-01-01T00:00:00Z")) = listOf(mapOf(
        "actor_id" to unitActor, "connector_id" to unitCredential, "metadata" to posterJson(poster()), "expires_at" to Timestamp.from(expiry),
    ))

    @Test fun `poster preparation projects ready dimensions and stores metadata`() = withMaintainer {
        val request = request()
        val hash = MessageDigest.getInstance("SHA-256").digest(request.toBuilder().setContext(request.context.toBuilder().clearCorrelationId()).build().toByteArray())
        val db = JdbcRows(listOf(mapOf("actor_id" to unitActor, "connector_id" to unitCredential, "request_hash" to hash)))
        val channel = GrpcReplies(Spoolin.Asset.newBuilder().setState("READY").setWidth(10).setHeight(20).setFormat("PNG").setContentHash("abc").build())
        val result = service(db, channel).prepare(request)
        assertEquals("READY", result.state)
        assertEquals(10, result.width)
        assertEquals("abc", result.contentHash)
        assertEquals("image/png", result.mediaType)
        assertTrue(db.writes.flatten().filterIsInstance<String>().any { it.contains("READY") && it.contains("abc") })
    }

    @Test fun `not yet created media asset leaves preparation pending`() = withMaintainer {
        val request = request()
        val hash = MessageDigest.getInstance("SHA-256").digest(request.toBuilder().setContext(request.context.toBuilder().clearCorrelationId()).build().toByteArray())
        val db = JdbcRows(listOf(mapOf("actor_id" to unitActor, "connector_id" to unitCredential, "request_hash" to hash)))
        assertEquals("PREPARING", service(db, GrpcReplies(Status.NOT_FOUND)).prepare(request).state)
    }

    @Test fun `poster preparation rejects changed bytes ownership invalid sizes and formats`() = withMaintainer {
        for ((actor, hash, code) in listOf(Triple(unitAsset, ByteArray(32), Status.Code.PERMISSION_DENIED),
            Triple(unitActor, ByteArray(32), Status.Code.ALREADY_EXISTS))) {
            val db = JdbcRows(listOf(mapOf("actor_id" to actor, "connector_id" to unitCredential, "request_hash" to hash)))
            assertEquals(code, assertThrows(io.grpc.StatusRuntimeException::class.java) { service(db, GrpcReplies()).prepare(request()) }.status.code)
        }
        for (invalid in listOf(request().toBuilder().clearImageBytes().build(), request().toBuilder().setMediaType("text/html").build(),
            request().toBuilder().setReuseBasis("").build())) {
            assertThrows(IllegalArgumentException::class.java) { service(JdbcRows(), GrpcReplies()).prepare(invalid) }
        }
    }

    @Test fun `poster resolution requires ownership active media and live or retained reference`() = withMaintainer {
        val ready = Spoolin.Asset.newBuilder().setState("READY").build()
        val db = JdbcRows(stored())
        assertEquals(poster(), db.source.connection.use { service(db, GrpcReplies(ready)).resolve(it, unitTarget.toString()) })
        val expired = JdbcRows(stored(Instant.parse("2000-01-01T00:00:00Z")), emptyList())
        assertEquals("POSTER_EXPIRED", assertThrows(io.grpc.StatusRuntimeException::class.java) {
            expired.source.connection.use { service(expired, GrpcReplies()).resolve(it, unitTarget.toString()) }
        }.status.description)
        val held = JdbcRows(stored(Instant.parse("2000-01-01T00:00:00Z")), listOf(mapOf("one" to 1)))
        assertEquals("READY", held.source.connection.use { service(held, GrpcReplies(ready)).resolve(it, unitTarget.toString()) }.state)
        val gone = JdbcRows(stored())
        assertEquals("POSTER_UNAVAILABLE_REIMPORT_REQUIRED", assertThrows(io.grpc.StatusRuntimeException::class.java) {
            gone.source.connection.use { service(gone, GrpcReplies(Spoolin.Asset.newBuilder().setState("DELETED").build())).resolve(it, unitTarget.toString()) }
        }.status.description)
        val absent = JdbcRows(emptyList())
        assertEquals(Status.Code.NOT_FOUND, assertThrows(io.grpc.StatusRuntimeException::class.java) {
            absent.source.connection.use { service(absent, GrpcReplies()).resolve(it, unitTarget.toString()) }
        }.status.code)
    }

    @Test fun `poster read maps public grant and keeps trusted stored metadata for private preview`() = withMaintainer {
        val image = Spoolin.MoviePosterImage.newBuilder().setImageBytes(ByteString.copyFromUtf8("rendered"))
            .setPoster(poster().toBuilder().setWidth(5).setHeight(6).setMediaType("image/jpeg")).build()
        val publicDb = JdbcRows(listOf(mapOf("id" to unitTarget, "attachment_id" to unitOperation, "version" to 4L)))
        val publicChannel = GrpcReplies(image)
        assertEquals(image, service(publicDb, publicChannel).read(Spoolin.MoviePosterReadRequest.newBuilder().setAssetId(unitAsset.toString()).build()))
        val sent = publicChannel.requests.single() as Spoolin.MoviePosterReadRequest
        assertEquals(unitOperation.toString(), sent.refId)
        val db = JdbcRows(stored())
        val privateImage = service(db, GrpcReplies(Spoolin.Asset.newBuilder().setState("READY").build(), image))
            .read(Spoolin.MoviePosterReadRequest.newBuilder().setRefId(unitTarget.toString()).build())
        assertEquals("rendered", privateImage.imageBytes.toStringUtf8())
        assertEquals(5, privateImage.poster.width)
        assertEquals("https://example.com/poster", privateImage.poster.sourceUrl)
    }
}
