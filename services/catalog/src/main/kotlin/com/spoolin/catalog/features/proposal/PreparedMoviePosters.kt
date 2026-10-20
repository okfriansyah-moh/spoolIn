package com.spoolin.catalog.features.proposal

import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.security.MessageDigest
import java.sql.Connection
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class PreparedMoviePosters(private val dataSource: DataSource, private val media: MediaServiceGrpc.MediaServiceBlockingStub, private val policy: () -> com.spoolin.support.policy.PublicPolicy) {
    fun prepare(request: Spoolin.PrepareMoviePosterRequest): Spoolin.PreparedPoster {
        val actor = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        require(Permissions.management(actor.role))
        require(request.imageBytes.size().toLong() in 1..minOf(policy().artwork.maxInputBytes,5_242_880L))
        require(request.sourceUrl.codePointCount(0,request.sourceUrl.length) <= policy().textBounds.getValue("sourceUrl").maxCodePoints && request.reuseBasis.codePointCount(0,request.reuseBasis.length) <= policy().textBounds.getValue("reuseBasis").maxCodePoints)
        val currentPolicy = policy()
        require(request.reuseBasis.codePointCount(0, request.reuseBasis.length) >= currentPolicy.textBounds.getValue("reuseBasis").minCodePoints)
        require(request.mediaType in currentPolicy.artwork.formats.map { "image/" + it.lowercase() })
        val id = UUID.fromString(request.context.operationId)
        val asset = UUID.nameUUIDFromBytes("poster:$id".toByteArray())
        val hash = MessageDigest.getInstance("SHA-256").digest(request.toBuilder().setContext(request.context.toBuilder().clearCorrelationId()).build().toByteArray())
        val initial = Spoolin.PreparedPoster.newBuilder().setRefId(id.toString()).setAssetId(asset.toString())
            .setMediaType(request.mediaType).setSourceUrl(request.sourceUrl).setReuseBasis(request.reuseBasis)
            .setState("PREPARING").build()
        dataSource.committed { conn ->
            conn.executeBound("INSERT INTO prepared_movie_poster (id,asset_id,actor_id,connector_id,request_hash,metadata,policy_revision,expires_at) " +
                "VALUES (?,?,?,?,?,?::jsonb,?,CURRENT_TIMESTAMP+interval '1 hour') ON CONFLICT (id) DO NOTHING",
                id, asset, actor.accountId, actor.credentialId, hash, posterJson(initial), request.context.policyRevision)
            val existing = conn.queryBound("SELECT actor_id,connector_id,request_hash FROM prepared_movie_poster WHERE id=? FOR UPDATE", listOf(id)) {
                Triple(it.getObject("actor_id", UUID::class.java),it.getObject("connector_id", UUID::class.java),it.getBytes("request_hash"))
            }.single()
            if (existing.first != actor.accountId || existing.second != actor.credentialId) throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!MessageDigest.isEqual(existing.third,hash)) throw Status.ALREADY_EXISTS.withDescription("OPERATION_CONFLICT").asRuntimeException()
        }
        val ready = try { media.withDeadlineAfter(5,TimeUnit.SECONDS).getAsset(Spoolin.Id.newBuilder().setId(asset.toString()).build()) }
        catch (e: io.grpc.StatusRuntimeException) { if (e.status.code == Status.Code.NOT_FOUND) return initial else throw e }
        if (ready.state == "READY") {
            require(ready.width > 0 && ready.height > 0)
            require(ready.width <= currentPolicy.artwork.maxDimensionPx && ready.height <= currentPolicy.artwork.maxDimensionPx)
            require(ready.width.toLong() * ready.height <= currentPolicy.artwork.maxPixels)
            require(ready.format in currentPolicy.artwork.formats)
        }
        val result = initial.toBuilder().setState(ready.state)
        if (ready.state == "READY") result.setContentHash(ready.contentHash).setWidth(ready.width).setHeight(ready.height)
            .setMediaType("image/" + ready.format.lowercase().replace("jpeg","jpeg"))
        return result.build().also { poster -> dataSource.committed { conn ->
            conn.executeBound("UPDATE prepared_movie_poster SET metadata=?::jsonb WHERE id=?",posterJson(poster),id)
        } }
    }

    fun resolve(conn: Connection, ref: String, browser: Boolean = false): Spoolin.PreparedPoster {
        val actor = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        val row = conn.queryBound("SELECT actor_id,connector_id,metadata::text,expires_at FROM prepared_movie_poster WHERE id=? FOR UPDATE",listOf(UUID.fromString(ref))) {
            listOf(it.getObject("actor_id",UUID::class.java),it.getObject("connector_id",UUID::class.java),it.getString("metadata"),it.getTimestamp("expires_at").toInstant())
        }.singleOrNull() ?: throw Status.NOT_FOUND.asRuntimeException()
        if (row[0] != actor.accountId || (!browser && row[1] != actor.credentialId)) throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Instant.now().isBefore(row[3] as Instant)) {
            val held = conn.queryBound(
                "SELECT 1 FROM movie_proposal WHERE state='REVIEW' AND expires_at>CURRENT_TIMESTAMP AND content->'poster'->>'ref_id'=? LIMIT 1",
                listOf(ref),
            ) { 1 }.isNotEmpty()
            if (!held) throw Status.FAILED_PRECONDITION.withDescription("POSTER_EXPIRED").asRuntimeException()
        }
        return posterFromMap(ProposalContent.parseJson(row[2] as String))
    }

    fun read(request: Spoolin.MoviePosterReadRequest): Spoolin.MoviePosterImage {
        if (request.hasAssetId()) {
            val grant = com.spoolin.catalog.features.artwork.ArtworkReads(dataSource).access(Spoolin.ArtworkAccessRequest.newBuilder().setAssetId(request.assetId).build())
            return media.withDeadlineAfter(10,TimeUnit.SECONDS).readMoviePoster(Spoolin.MoviePosterReadRequest.newBuilder().setRefId(grant.holdId).setAssetId(grant.assetId).build())
        }
        val poster = dataSource.connection.use { resolve(it,request.refId,ActorRpc.current()?.clientKind == null) }
        val image = media.withDeadlineAfter(10,TimeUnit.SECONDS).readMoviePoster(request)
        return image.toBuilder().setPoster(poster.toBuilder().setMediaType(image.poster.mediaType).setWidth(image.poster.width).setHeight(image.poster.height)).build()
    }
}

internal fun posterJson(poster: Spoolin.PreparedPoster): String = DeterministicContentSerializer.json(posterMap(poster))
internal fun posterMap(p: Spoolin.PreparedPoster): Map<String,Any?> = mapOf("ref_id" to p.refId,"asset_id" to p.assetId,"content_hash" to p.contentHash,"media_type" to p.mediaType,"width" to p.width,"height" to p.height,"source_url" to p.sourceUrl,"reuse_basis" to p.reuseBasis,"state" to p.state)
internal fun posterFromMap(p: Map<String,Any?>): Spoolin.PreparedPoster = Spoolin.PreparedPoster.newBuilder()
    .setRefId(p["ref_id"] as String).setAssetId(p["asset_id"] as String).setContentHash(p["content_hash"] as String)
    .setMediaType(p["media_type"] as String).setWidth((p["width"] as Number).toInt()).setHeight((p["height"] as Number).toInt())
    .setSourceUrl(p["source_url"] as String).setReuseBasis(p["reuse_basis"] as String).setState(p["state"] as String).build()
