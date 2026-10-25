package com.spoolin.media.features.upload

import com.google.protobuf.ByteString
import com.spoolin.media.features.asset.CatalogAttachments
import com.spoolin.media.fixtures.CatalogBoundary
import com.spoolin.media.fixtures.JdbcBoundary
import com.spoolin.media.fixtures.Response
import com.spoolin.media.fixtures.actorId
import com.spoolin.media.fixtures.assetId
import com.spoolin.media.fixtures.holdId
import com.spoolin.media.fixtures.instant
import com.spoolin.media.fixtures.operationId
import com.spoolin.media.fixtures.titleId
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorVerifier
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin.Attachment
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.UploadChunk
import spoolin.v1.Spoolin.UploadHeader
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.time.Clock
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import javax.imageio.ImageIO

class UploadWorkflowRulesTest {
    @TempDir lateinit var root: Path
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)

    private fun png(): ByteArray =
        ByteArrayOutputStream()
            .also {
                ImageIO.write(BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB), "png", it)
            }.toByteArray()

    private fun header(bytes: ByteArray = png()): UploadChunk =
        UploadChunk
            .newBuilder()
            .setHeader(
                UploadHeader
                    .newBuilder()
                    .setAssetId(assetId.toString())
                    .setActorId(actorId.toString())
                    .setIntentId(holdId.toString())
                    .setInputBytes(bytes.size.toLong())
                    .setInputSha256(ByteString.copyFrom(MessageDigest.getInstance("SHA-256").digest(bytes)))
                    .setContext(
                        spoolin.v1.Spoolin.Context
                            .newBuilder()
                            .setOperationId(
                                operationId.toString(),
                            ).setCorrelationId(operationId.toString())
                            .setPolicyRevision("p1"),
                    ),
            ).build()

    private fun attachment() =
        Attachment
            .newBuilder()
            .setHoldId(holdId.toString())
            .setAssetId(assetId.toString())
            .setTitleId(titleId.toString())
            .setActorId(actorId.toString())
            .setState("PREPARING")
            .build()

    private fun data(bytes: ByteArray) = UploadChunk.newBuilder().setData(ByteString.copyFrom(bytes)).build()

    /** Real signed actor interceptor establishes the context; transport and JDBC are the external boundaries. */
    private fun authorized(
        rpc: UploadRpc,
        response: Response<Outcome>,
    ): ServerCall.Listener<UploadChunk> {
        val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val method = MediaServiceGrpc.getUploadMethod()
        val claims =
            ActorClaims(
                "media",
                "/${method.fullMethodName}",
                actorId,
                operationId,
                null,
                "MAINTAINER",
                1,
                instant.epochSecond,
                instant.epochSecond + 20,
                Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ByteArray(16) {
                        3
                    },
                ),
                operationId,
                operationId,
            )
        val metadata = Metadata().apply { put(ActorRpc.metadata, ActorSigner("unit", keys.private).sign(claims)) }
        val call =
            object : ServerCall<UploadChunk, Outcome>() {
                override fun request(count: Int) {}

                override fun sendHeaders(headers: Metadata) {}

                override fun sendMessage(message: Outcome) {
                    response.onNext(message)
                }

                override fun close(
                    status: Status,
                    trailers: Metadata,
                ) {
                    if (!status.isOk) response.onError(status.asRuntimeException())
                }

                override fun isCancelled() = false

                override fun getMethodDescriptor() = method
            }
        return ActorInterceptor("media", setOf(method.fullMethodName), ActorVerifier(mapOf("unit" to keys.public)), {}, clock)
            .interceptCall(
                call,
                metadata,
                ServerCallHandler { _, _ ->
                    val upload = rpc.upload(response)
                    object : ServerCall.Listener<UploadChunk>() {
                        override fun onMessage(message: UploadChunk) {
                            upload.onNext(message)
                        }

                        override fun onHalfClose() {
                            upload.onCompleted()
                        }

                        override fun onCancel() {
                            upload.onError(Status.CANCELLED.asRuntimeException())
                        }
                    }
                },
            )
    }

    @Test fun `verified complete image upload returns pending receipt and retains private accepted input`() {
        val bytes = png()
        val store = QuarantineStore(root)
        val db = JdbcBoundary()
        val response = Response<Outcome>()
        val upload =
            authorized(UploadRpc(db.source, store, "p1", clock, CatalogAttachments(CatalogBoundary(attachment()).client)), response)
        upload.onMessage(header(bytes))
        upload.onMessage(data(bytes.copyOfRange(0, 10)))
        upload.onMessage(data(bytes.copyOfRange(10, bytes.size)))
        upload.onHalfClose()
        assertNull(response.failure)
        assertTrue(response.completed)
        assertEquals("PENDING", response.values.single().state)
        assertEquals(operationId.toString(), response.values.single().operationId)
        assertArrayEquals(bytes, Files.readAllBytes(store.stagingPath(assetId)))
        assertEquals(2, db.commits)
    }

    @Test fun `stream protocol rejects missing header missing actor and data before header`() {
        val rpc = UploadRpc(JdbcBoundary().source, QuarantineStore(root), "p1")
        val missing = Response<Outcome>()
        rpc.upload(missing).onCompleted()
        assertEquals(Status.Code.INVALID_ARGUMENT, missing.code)
        val actor = Response<Outcome>()
        rpc.upload(actor).onNext(header())
        assertEquals(Status.Code.PERMISSION_DENIED, actor.code)
        for (chunk in listOf(data(byteArrayOf(1)), UploadChunk.getDefaultInstance())) {
            val response = Response<Outcome>()
            rpc.upload(response).onNext(chunk)
            assertEquals(Status.Code.INVALID_ARGUMENT, response.code)
        }
    }

    @Test fun `invalid identities authority and intent bindings fail before accepting bytes`() {
        val good = header()
        val cases =
            listOf(
                good.toBuilder().setHeader(good.header.toBuilder().setAssetId("bad")).build() to Status.Code.INVALID_ARGUMENT,
                good.toBuilder().setHeader(good.header.toBuilder().setIntentId("bad")).build() to Status.Code.INVALID_ARGUMENT,
            )
        for ((chunk, expected) in cases) {
            val response = Response<Outcome>()
            authorized(UploadRpc(JdbcBoundary().source, QuarantineStore(root), "p1", clock), response).onMessage(chunk)
            assertEquals(expected, response.code)
        }
        val unavailable = Response<Outcome>()
        authorized(UploadRpc(JdbcBoundary().source, QuarantineStore(root), "p1", clock), unavailable).onMessage(good)
        assertEquals(Status.Code.UNAVAILABLE, unavailable.code)
        for (invalid in listOf(
            attachment().toBuilder().clearActorId().build(),
            attachment().toBuilder().setState("ACTIVE").build(),
            attachment().toBuilder().setAssetId(UUID.randomUUID().toString()).build(),
        )) {
            val response = Response<Outcome>()
            authorized(
                UploadRpc(JdbcBoundary().source, QuarantineStore(root), "p1", clock, CatalogAttachments(CatalogBoundary(invalid).client)),
                response,
            ).onMessage(good)
            assertEquals(Status.Code.PERMISSION_DENIED, response.code)
        }
    }

    @Test fun `size checksum invalid image and duplicate header failures delete staging bytes`() {
        val valid = png()
        val store = QuarantineStore(root)
        for ((index, payload) in listOf(
            valid.dropLast(1).toByteArray(),
            valid
                .map {
                    (it + 1).toByte()
                }.toByteArray(),
            "not an image".toByteArray(),
        ).withIndex()) {
            val response = Response<Outcome>()
            val rpc = UploadRpc(JdbcBoundary().source, store, "p1", clock, CatalogAttachments(CatalogBoundary(attachment()).client))
            val stream = authorized(rpc, response)
            stream.onMessage(if (index == 2) header(payload) else header(valid))
            stream.onMessage(data(payload))
            stream.onHalfClose()
            assertEquals(Status.Code.INVALID_ARGUMENT, response.code)
            assertFalse(Files.exists(store.stagingPath(assetId)))
        }
        val response = Response<Outcome>()
        val stream =
            authorized(
                UploadRpc(JdbcBoundary().source, store, "p1", clock, CatalogAttachments(CatalogBoundary(attachment()).client)),
                response,
            )
        stream.onMessage(header(valid))
        stream.onMessage(header(valid))
        stream.onMessage(data(valid))
        stream.onHalfClose()
        assertEquals(Status.Code.INVALID_ARGUMENT, response.code)
        assertTrue(response.values.isEmpty())
        assertFalse(Files.exists(store.stagingPath(assetId)))
    }

    @Test fun `cancelled accepted header removes staging and never acknowledges an upload`() {
        val store = QuarantineStore(root)
        val response = Response<Outcome>()
        val stream =
            authorized(
                UploadRpc(JdbcBoundary().source, store, "p1", clock, CatalogAttachments(CatalogBoundary(attachment()).client)),
                response,
            )
        stream.onMessage(header())
        stream.onCancel()
        stream.onHalfClose()
        assertTrue(response.values.isEmpty())
        assertFalse(response.completed)
        assertFalse(Files.exists(store.stagingPath(assetId)))
    }

    @Test fun `reservation conflicts and storage failures never leave staging inputs`() {
        val store = QuarantineStore(root)
        val db =
            JdbcBoundary().apply {
                updates.add(1)
                updates.add(0)
            }
        val duplicate = Response<Outcome>()
        authorized(
            UploadRpc(db.source, store, "p1", clock, CatalogAttachments(CatalogBoundary(attachment()).client)),
            duplicate,
        ).onMessage(header())
        assertEquals(Status.Code.ALREADY_EXISTS, duplicate.code)
        assertFalse(Files.exists(store.stagingPath(assetId)))
        val failure = Response<Outcome>()
        authorized(
            UploadRpc(
                JdbcBoundary(java.sql.SQLException("secret")).source,
                store,
                "p1",
                clock,
                CatalogAttachments(CatalogBoundary(attachment()).client),
            ),
            failure,
        ).onMessage(header())
        assertEquals(Status.Code.INTERNAL, failure.code)
        assertFalse(failure.failure!!.message!!.contains("secret"))
        assertFalse(Files.exists(store.stagingPath(assetId)))
    }

    @Test
    fun `invalid upload bounds and checksum headers never reserve accepted input`() {
        val valid = header()
        val variants =
            listOf(
                valid.header
                    .toBuilder()
                    .setInputBytes(0)
                    .build(),
                valid.header
                    .toBuilder()
                    .setInputBytes(com.spoolin.media.features.processing.ProcessingBounds.MAX_INPUT_BYTES + 1)
                    .build(),
                valid.header
                    .toBuilder()
                    .setInputSha256(ByteString.copyFrom(byteArrayOf(1)))
                    .build(),
            )
        val store = QuarantineStore(root)
        variants.forEach { value ->
            val db = JdbcBoundary()
            val response = Response<Outcome>()
            val stream =
                authorized(UploadRpc(db.source, store, "p1", clock, CatalogAttachments(CatalogBoundary(attachment()).client)), response)
            stream.onMessage(UploadChunk.newBuilder().setHeader(value).build())
            stream.onHalfClose()
            assertEquals(Status.Code.INVALID_ARGUMENT, response.code)
            assertEquals(0, db.commits)
            assertFalse(Files.exists(store.stagingPath(assetId)))
        }
    }

    @Test
    fun `authority changes after transferred bytes refuse processing and erase staging`() {
        val bytes = png()
        val boundary = CatalogBoundary(attachment())
        val store = QuarantineStore(root)
        val response = Response<Outcome>()
        val stream = authorized(UploadRpc(JdbcBoundary().source, store, "p1", clock, CatalogAttachments(boundary.client)), response)
        stream.onMessage(header(bytes))
        stream.onMessage(data(bytes))
        boundary.attachment = attachment().toBuilder().setHoldId(UUID.randomUUID().toString()).build()
        stream.onHalfClose()
        assertEquals(Status.Code.PERMISSION_DENIED, response.code)
        assertFalse(Files.exists(store.stagingPath(assetId)))
        assertTrue(response.values.isEmpty())
    }
}
