package com.spoolin.media.features.upload

import com.spoolin.media.boundary.grpc.MediaPeer
import com.spoolin.media.features.asset.AssetRpc
import com.spoolin.media.features.asset.CatalogAttachments
import com.spoolin.media.features.hold.HoldRpc
import com.spoolin.media.fixtures.CatalogBoundary
import com.spoolin.media.fixtures.JdbcBoundary
import com.spoolin.media.fixtures.Response
import com.spoolin.media.fixtures.actorId
import com.spoolin.media.fixtures.assetId
import com.spoolin.media.fixtures.assetRecord
import com.spoolin.media.fixtures.holdId
import com.spoolin.media.fixtures.instant
import com.spoolin.media.fixtures.operationId
import com.spoolin.media.fixtures.titleId
import com.spoolin.media.operations.receipts.OwnerReceiptReads
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import spoolin.v1.Spoolin.Asset
import spoolin.v1.Spoolin.AssetReadChunk
import spoolin.v1.Spoolin.AssetReadRequest
import spoolin.v1.Spoolin.Attachment
import spoolin.v1.Spoolin.Hold
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.MoviePosterImage
import spoolin.v1.Spoolin.MoviePosterReadRequest
import spoolin.v1.Spoolin.Outcome
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Clock
import java.time.ZoneOffset

class MediaServiceRulesTest {
    @TempDir lateinit var root: Path

    private fun service(db: JdbcBoundary): MediaServiceImpl {
        val store = QuarantineStore(root)
        val remote =
            Attachment
                .newBuilder()
                .setHoldId(holdId.toString())
                .setAssetId(assetId.toString())
                .setTitleId(titleId.toString())
                .setActorId(actorId.toString())
                .setState("ACTIVE")
                .build()
        val catalog = CatalogAttachments(CatalogBoundary(remote).client)
        return MediaServiceImpl(
            UploadRpc(db.source, store, "p1"),
            HoldRpc(db.source, catalog),
            OwnerReceiptReads(db.source, Clock.fixed(instant, ZoneOffset.UTC)),
            AssetRpc(db.source, store, catalog),
        )
    }

    @Test fun `combined service exposes a receipt only after the persisted asset tuple authorizes catalog`() {
        val receipt =
            mapOf(
                "id" to operationId,
                "state" to "SUCCEEDED",
                "actor_scope" to "account:$actorId",
                "kind" to "UPLOAD",
                "request_hash" to byteArrayOf(1),
                "correlation_id" to operationId,
                "created_at" to Timestamp.from(instant),
                "expires_at" to Timestamp.from(instant.plusSeconds(60)),
                "result" to "{\"resultId\":\"$assetId\",\"version\":\"3\"}",
                "error_code" to "FIELD_INVALID",
            )
        val db = JdbcBoundary().rows(assetRecord()).rows(mapOf("intent" to holdId.toString())).rows(receipt)
        val response = Response<Outcome>()
        MediaPeer.context("catalog").run { service(db).getOperation(Id.newBuilder().setId(operationId.toString()).build(), response) }
        assertNull(response.failure)
        assertTrue(response.completed)
        assertEquals(assetId.toString(), response.values.single().resultId)
        assertEquals(3L, response.values.single().version)
        assertEquals(
            "FIELD_INVALID",
            response.values
                .single()
                .problem.code,
        )
    }

    @Test fun `combined service delegates public operations through each existing authorization boundary`() {
        val service = service(JdbcBoundary())
        val upload = Response<Outcome>()
        service.upload(upload).onCompleted()
        assertEquals(Status.Code.INVALID_ARGUMENT, upload.code)
        val asset = Response<Asset>()
        service.getAsset(Id.getDefaultInstance(), asset)
        assertEquals(Status.Code.INVALID_ARGUMENT, asset.code)
        val bytes = Response<AssetReadChunk>()
        service.readAsset(AssetReadRequest.getDefaultInstance(), bytes)
        assertEquals(Status.Code.PERMISSION_DENIED, bytes.code)
        val poster = Response<MoviePosterImage>()
        service.readMoviePoster(MoviePosterReadRequest.getDefaultInstance(), poster)
        assertEquals(Status.Code.PERMISSION_DENIED, poster.code)
        for (action in listOf<(Response<Attachment>) -> Unit>({
            service.reserve(Hold.getDefaultInstance(), it)
        }, { service.confirm(Hold.getDefaultInstance(), it) }, { service.release(Hold.getDefaultInstance(), it) })) {
            val response = Response<Attachment>()
            action(response)
            assertEquals(Status.Code.PERMISSION_DENIED, response.code)
        }
        val operation = Response<Outcome>()
        service.getOperation(Id.getDefaultInstance(), operation)
        assertEquals(Status.Code.INVALID_ARGUMENT, operation.code)
    }
}
