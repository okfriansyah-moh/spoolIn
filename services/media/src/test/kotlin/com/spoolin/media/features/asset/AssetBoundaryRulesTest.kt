package com.spoolin.media.features.asset

import com.spoolin.media.boundary.grpc.MediaPeer
import com.spoolin.media.features.upload.QuarantineStore
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
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import spoolin.v1.Spoolin.Asset
import spoolin.v1.Spoolin.AssetReadChunk
import spoolin.v1.Spoolin.AssetReadRequest
import spoolin.v1.Spoolin.Attachment
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.MoviePosterImage
import spoolin.v1.Spoolin.MoviePosterReadRequest
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.SQLException
import java.util.UUID

class AssetBoundaryRulesTest {
    @TempDir lateinit var root: Path

    private fun row(state: String = "READY"): AssetRow =
        requireNotNull(AssetSql.find(JdbcBoundary().rows(assetRecord(state)).source, assetId))

    private fun remote(state: String = "ACTIVE") =
        Attachment
            .newBuilder()
            .setHoldId(holdId.toString())
            .setAssetId(assetId.toString())
            .setTitleId(titleId.toString())
            .setActorId(actorId.toString())
            .setState(state)
            .setCatalogVersion(4)
            .build()

    private fun local(state: String = "ACTIVE") = AssetHold(holdId, assetId, titleId, state)

    private fun status(
        code: Status.Code,
        action: () -> Unit,
    ) {
        assertEquals(code, Status.fromThrowable(assertThrows(StatusRuntimeException::class.java, action)).code)
    }

    @Test fun `asset rows retain full processing evidence and nullable output metadata`() {
        val full = row()
        assertTrue(full.hasProcessingEvidence)
        assertEquals(instant, full.processingCreatedAt)
        assertEquals(100L, full.outputBytes)
        assertEquals(10, full.width)
        val nullable =
            assetRecord("RECEIVING").toMutableMap().apply {
                listOf("processing_work_id", "processing_created_at", "width", "height", "output_bytes").forEach { put(it, null) }
            }
        val decoded = requireNotNull(AssetSql.find(JdbcBoundary().rows(nullable).source, assetId))
        assertFalse(decoded.hasProcessingEvidence)
        assertNull(decoded.outputBytes)
        assertNull(decoded.width)
        assertNull(decoded.height)
        assertNull(AssetSql.find(JdbcBoundary().source, assetId))
    }

    @Test fun `catalog failures are reduced to bounded service errors`() {
        for ((remote, expected) in listOf(
            Status.CANCELLED to Status.Code.CANCELLED,
            Status.NOT_FOUND to Status.Code.NOT_FOUND,
            Status.INTERNAL to Status.Code.UNAVAILABLE,
            Status.DEADLINE_EXCEEDED to Status.Code.UNAVAILABLE,
        )) {
            status(expected) { CatalogAttachments(CatalogBoundary(status = remote).client).find(holdId) }
        }
        assertEquals(remote(), CatalogAttachments(CatalogBoundary(remote()).client).find(holdId))
    }

    @Test fun `read access requires the same active tuple and a positive current version`() {
        val access = AssetAccess(JdbcBoundary().source, CatalogAttachments(CatalogBoundary().client))
        for (state in listOf("RESERVED", "ACTIVE")) access.checkRead(local(state), remote(), assetId, holdId, 4)
        val wrong = UUID.randomUUID()
        val cases =
            listOf(
                null to remote(),
                local("RELEASED") to remote(),
                local().copy(asset = wrong) to remote(),
                local().copy(title = wrong) to remote(),
                local() to remote("RELEASED"),
                local() to remote().toBuilder().setHoldId(wrong.toString()).build(),
                local() to remote().toBuilder().setAssetId(wrong.toString()).build(),
                local() to remote().toBuilder().clearCatalogVersion().build(),
            )
        for ((local, remote) in cases) status(Status.Code.NOT_FOUND) { access.checkRead(local, remote, assetId, holdId, 4) }
        for (version in listOf(0L, 3L, 5L)) status(Status.Code.NOT_FOUND) { access.checkRead(local(), remote(), assetId, holdId, version) }
    }

    @Test fun `metadata intent authorization requires matching asset actor and permitted state`() {
        val boundary = CatalogBoundary(remote())
        for (state in listOf("PREPARING", "ACTIVE", "RELEASED")) {
            boundary.attachment = remote(state)
            assertTrue(
                AssetAccess(
                    JdbcBoundary().rows(mapOf("intent" to holdId.toString())).source,
                    CatalogAttachments(boundary.client),
                ).catalogMetadata(row()),
            )
        }
        val denied =
            listOf(
                remote("UNKNOWN"),
                remote().toBuilder().clearActorId().build(),
                remote().toBuilder().setActorId(UUID.randomUUID().toString()).build(),
                remote().toBuilder().setAssetId(UUID.randomUUID().toString()).build(),
            )
        for (attachment in denied) {
            boundary.attachment = attachment
            assertFalse(
                AssetAccess(
                    JdbcBoundary().rows(mapOf("intent" to holdId.toString())).source,
                    CatalogAttachments(boundary.client),
                ).catalogMetadata(row()),
            )
        }
        assertFalse(
            AssetAccess(
                JdbcBoundary().rows(mapOf("intent" to "invalid")).source,
                CatalogAttachments(boundary.client),
            ).catalogMetadata(row()),
        )
    }

    @Test fun `metadata can reconcile historical holds without an intent`() {
        val db = JdbcBoundary().rows().rows(mapOf("id" to holdId, "asset_id" to assetId, "title_id" to titleId, "state" to "RELEASED"))
        assertTrue(AssetAccess(db.source, CatalogAttachments(CatalogBoundary(remote("RELEASED")).client)).catalogMetadata(row()))
        assertFalse(AssetAccess(JdbcBoundary().rows().rows().source, CatalogAttachments(CatalogBoundary().client)).catalogMetadata(row()))
    }

    @Test fun `asset projection is available to catalog only after tuple authorization`() {
        val db = JdbcBoundary().rows(assetRecord()).rows(mapOf("intent" to holdId.toString()))
        val rpc = AssetRpc(db.source, QuarantineStore(root), CatalogAttachments(CatalogBoundary(remote()).client))
        val response = Response<Asset>()
        MediaPeer.context("catalog").run { rpc.getAsset(Id.newBuilder().setId(assetId.toString()).build(), response) }
        assertNull(response.failure)
        assertTrue(response.completed)
        val result = response.values.single()
        assertEquals(assetId.toString(), result.id)
        assertEquals("READY", result.state)
        assertEquals("PNG", result.format)
        assertEquals("AQID", result.contentHash)
        assertEquals(10, result.width)
        assertEquals(20, result.height)
        assertEquals(100, result.outputBytes)
    }

    @Test fun `rejected asset projects a safe problem without leaking stored rejection text`() {
        val record = assetRecord("REJECTED") + mapOf("error_code" to "sensitive storage detail")
        val db = JdbcBoundary().rows(record).rows(mapOf("intent" to holdId.toString()))
        val rpc = AssetRpc(db.source, QuarantineStore(root), CatalogAttachments(CatalogBoundary(remote()).client))
        val response = Response<Asset>()
        MediaPeer.context("catalog").run { rpc.getAsset(Id.newBuilder().setId(assetId.toString()).build(), response) }
        assertEquals(
            "FIELD_INVALID",
            response.values
                .single()
                .problem.code,
        )
        assertFalse(
            response.values
                .single()
                .toString()
                .contains("sensitive"),
        )
    }

    @Test fun `unknown callers and unknown assets are indistinguishable`() {
        for (peer in listOf("core", "unknown", "catalog")) {
            val response = Response<Asset>()
            val rpc =
                AssetRpc(
                    JdbcBoundary()
                        .rows(assetRecord())
                        .rows()
                        .rows()
                        .source,
                    QuarantineStore(root),
                    CatalogAttachments(CatalogBoundary().client),
                )
            MediaPeer.context(peer).run { rpc.getAsset(Id.newBuilder().setId(assetId.toString()).build(), response) }
            assertEquals(Status.Code.NOT_FOUND, response.code)
            assertTrue(response.values.isEmpty())
        }
    }

    @Test fun `malformed identities and infrastructure failures produce safe errors`() {
        val catalog = CatalogAttachments(CatalogBoundary().client)
        for (identity in listOf("", "wrong", assetId.toString().uppercase().replace("00000000", "AAAAAAAA"))) {
            val response = Response<Asset>()
            AssetRpc(JdbcBoundary().source, QuarantineStore(root), catalog).getAsset(Id.newBuilder().setId(identity).build(), response)
            assertEquals(Status.Code.INVALID_ARGUMENT, response.code)
        }
        for (failure in listOf(SQLException("password secret"), IllegalStateException("secret"), SecurityException("secret"))) {
            val response = Response<Asset>()
            AssetRpc(
                JdbcBoundary(failure).source,
                QuarantineStore(root),
                catalog,
            ).getAsset(Id.newBuilder().setId(assetId.toString()).build(), response)
            assertEquals(Status.Code.UNAVAILABLE, response.code)
            assertFalse(response.failure!!.message!!.contains("secret"))
        }
    }

    @Test fun `only trusted peers may request bytes or movie thumbnails`() {
        val rpc = AssetRpc(JdbcBoundary().source, QuarantineStore(root), CatalogAttachments(CatalogBoundary().client))
        val bytes = Response<AssetReadChunk>()
        rpc.readAsset(AssetReadRequest.getDefaultInstance(), bytes)
        assertEquals(Status.Code.PERMISSION_DENIED, bytes.code)
        val poster = Response<MoviePosterImage>()
        rpc.readMoviePoster(MoviePosterReadRequest.getDefaultInstance(), poster)
        assertEquals(Status.Code.PERMISSION_DENIED, poster.code)
    }

    @Test fun `immutable asset files verify identity bytes hash and dimensions`() {
        val bytes = byteArrayOf(4, 3, 2, 1)
        for ((format, extension) in listOf("PNG" to "png", "JPEG" to "jpeg", "WEBP" to "webp")) {
            val record =
                row().copy(
                    format = format,
                    storageKey = "output/$assetId.$extension",
                    outputBytes = bytes.size.toLong(),
                    contentHash = MessageDigest.getInstance("SHA-256").digest(bytes),
                )
            Files.write(root.resolve("$assetId.$extension"), bytes)
            AssetFile.open(root, record).use { file ->
                assertEquals(0L, file.channel.position())
                assertEquals(bytes.size.toLong(), file.channel.size())
            }
            for (bad in listOf(record.copy(outputBytes = 9), record.copy(width = null), record.copy(contentHash = byteArrayOf(9)))) {
                assertThrows(IllegalStateException::class.java) { AssetFile.open(root, bad) }
            }
        }
        status(Status.Code.UNAVAILABLE) { AssetFile.open(root, row().copy(storageKey = "../elsewhere")) }
        status(Status.Code.UNAVAILABLE) { AssetFile.open(root, row().copy(format = "GIF")) }
        Files.delete(root.resolve("$assetId.png"))
        status(Status.Code.NOT_FOUND) { AssetFile.open(root, row()) }
    }

    @Test fun `catalog hold receipts require matching persisted and current authority tuples`() {
        val stored = assetRecord() + mapOf("receipt_hold" to holdId, "receipt_title" to titleId)
        val db = JdbcBoundary().rows().rows(stored)
        val rpc = AssetRpc(db.source, QuarantineStore(root), CatalogAttachments(CatalogBoundary(remote()).client))
        MediaPeer.context("catalog").run { assertEquals("account:$actorId", rpc.receiptScope(operationId)) }
        val unknown = AssetRpc(JdbcBoundary().rows().rows().source, QuarantineStore(root), CatalogAttachments(CatalogBoundary().client))
        status(Status.Code.NOT_FOUND) { unknown.receiptScope(operationId) }
        val denied =
            AssetRpc(
                JdbcBoundary().rows().rows(stored).source,
                QuarantineStore(root),
                CatalogAttachments(CatalogBoundary(remote("UNKNOWN")).client),
            )
        MediaPeer.context("catalog").run { status(Status.Code.NOT_FOUND) { denied.receiptScope(operationId) } }
    }

    @Test fun `byte reads verify live hold grant and close files when streaming transport is unavailable`() {
        val store = QuarantineStore(root)
        val bytes = byteArrayOf(1, 4, 9)
        Files.write(store.outputDirectory().resolve("$assetId.png"), bytes)
        val record =
            assetRecord() +
                mapOf("output_bytes" to bytes.size.toLong(), "content_hash" to MessageDigest.getInstance("SHA-256").digest(bytes))
        val db = JdbcBoundary().rows(record).rows(mapOf("id" to holdId, "asset_id" to assetId, "title_id" to titleId, "state" to "ACTIVE"))
        val rpc = AssetRpc(db.source, store, CatalogAttachments(CatalogBoundary(remote()).client))
        val response = Response<AssetReadChunk>()
        val request =
            AssetReadRequest
                .newBuilder()
                .setAssetId(assetId.toString())
                .setHoldId(holdId.toString())
                .setCatalogVersion(4)
                .build()
        MediaPeer.context("core").run { rpc.readAsset(request, response) }
        assertEquals(Status.Code.INTERNAL, response.code)
        assertTrue(response.values.isEmpty())
        assertEquals(1, db.commits)
        val poster = Response<MoviePosterImage>()
        val absent = AssetRpc(JdbcBoundary().source, store, CatalogAttachments(CatalogBoundary(remote("RELEASED")).client))
        MediaPeer
            .context(
                "catalog",
            ).run { absent.readMoviePoster(MoviePosterReadRequest.newBuilder().setRefId(holdId.toString()).build(), poster) }
        assertEquals(Status.Code.NOT_FOUND, poster.code)
    }

    @Test fun `symbolic links are not accepted as immutable asset files`() {
        val target = Files.write(root.resolve("target"), byteArrayOf(1))
        Files.createSymbolicLink(root.resolve("$assetId.png"), target)
        status(Status.Code.NOT_FOUND) { AssetFile.open(root, row()) }
    }

    @Test
    fun `read grants reject every independently mismatching tuple member and version`() {
        val access = AssetAccess(JdbcBoundary().source, CatalogAttachments(CatalogBoundary().client))
        val other = UUID.randomUUID()
        val local = local()
        val variants =
            listOf(
                remote().toBuilder().setTitleId(other.toString()).build(),
                remote().toBuilder().setCatalogVersion(0).build(),
                remote().toBuilder().setCatalogVersion(5).build(),
            )
        variants.forEach { grant -> status(Status.Code.NOT_FOUND) { access.checkRead(local, grant, assetId, holdId, 4) } }
        status(Status.Code.NOT_FOUND) { access.checkRead(local.copy(id = other), remote(), assetId, holdId, 4) }
        val holds =
            listOf(
                mapOf("id" to holdId, "asset_id" to assetId, "title_id" to other, "state" to "ACTIVE"),
                mapOf("id" to holdId, "asset_id" to assetId, "title_id" to titleId, "state" to "ACTIVE"),
            )
        for (grant in listOf(
            remote().toBuilder().setHoldId(other.toString()).build(),
            remote().toBuilder().setAssetId(other.toString()).build(),
            remote("UNKNOWN"),
        )) {
            val db = JdbcBoundary().rows().rows(*holds.toTypedArray())
            assertFalse(AssetAccess(db.source, CatalogAttachments(CatalogBoundary(grant).client)).catalogMetadata(row()))
        }
    }

    @Test
    fun `asset integrity rejects absent required fields zero output and cancelled readers`() {
        val bytes = byteArrayOf(1, 2)
        Files.write(root.resolve("$assetId.png"), bytes)
        val valid = row().copy(outputBytes = 2, contentHash = MessageDigest.getInstance("SHA-256").digest(bytes))
        for (bad in listOf(valid.copy(height = null), valid.copy(contentHash = null), valid.copy(outputBytes = null))) {
            assertThrows(IllegalStateException::class.java) { AssetFile.open(root, bad) }
        }
        val context =
            io.grpc.Context
                .current()
                .withCancellation()
        context.cancel(null)
        context.run { status(Status.Code.CANCELLED) { AssetFile.open(root, valid) } }
        Files.write(root.resolve("$assetId.png"), byteArrayOf())
        assertThrows(IllegalStateException::class.java) { AssetFile.open(root, valid.copy(outputBytes = 0)) }
    }

    @Test
    fun `movie poster reads verify ready asset actor request binding and active hold grant`() {
        val request = MoviePosterReadRequest.newBuilder().setRefId(holdId.toString()).build()

        data class Case(
            val grant: Attachment,
            val asset: Map<String, Any?>?,
            val requested: String?,
            val expected: Status.Code,
        )
        val cases =
            listOf(
                Case(remote("PREPARING"), null, null, Status.Code.NOT_FOUND),
                Case(remote("PREPARING"), assetRecord("PROCESSING"), null, Status.Code.NOT_FOUND),
                Case(remote("PREPARING").toBuilder().clearActorId().build(), assetRecord(), null, Status.Code.PERMISSION_DENIED),
                Case(
                    remote("PREPARING").toBuilder().setActorId(UUID.randomUUID().toString()).build(),
                    assetRecord(),
                    null,
                    Status.Code.PERMISSION_DENIED,
                ),
                Case(remote("PREPARING"), assetRecord(), UUID.randomUUID().toString(), Status.Code.NOT_FOUND),
                Case(remote("PREPARING"), assetRecord(), assetId.toString(), Status.Code.NOT_FOUND),
                Case(remote("ACTIVE"), assetRecord(), null, Status.Code.NOT_FOUND),
            )
        cases.forEach { case ->
            val db = JdbcBoundary()
            if (case.asset == null) db.rows() else db.rows(case.asset)
            db.rows()
            val rpc = AssetRpc(db.source, QuarantineStore(root), CatalogAttachments(CatalogBoundary(case.grant).client))
            val response = Response<MoviePosterImage>()
            val query = request.toBuilder().also { builder -> case.requested?.let { builder.assetId = it } }.build()
            MediaPeer.context("catalog").run { rpc.readMoviePoster(query, response) }
            assertEquals(case.expected, response.code, case.toString())
            assertTrue(response.values.isEmpty())
        }
    }
}
