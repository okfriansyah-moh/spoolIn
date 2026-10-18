package com.spoolin.core.features.artworkproxy

import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID

private const val STREAM_LIMIT = 65_536
private const val SCRATCH_LIMIT = 4

class ArtworkTransferRulesTest {
    private val bytes = "processed artwork bytes".toByteArray()

    @Test
    fun `valid declared processed stream serves exact bytes and deletes private scratch on close`() {
        val asset = ArtworkTransfer.receive(listOf(header(), data(bytes)).iterator())
        val path = asset.path
        asset.use {
            assertEquals("image/png", it.contentType)
            assertEquals(bytes.size.toLong(), it.bytes)
            assertArrayEquals(bytes, Files.readAllBytes(it.path))
        }
        assertFalse(Files.exists(path))
    }

    @Test
    fun `truncation extra bytes repeated header oversized chunks and hash mismatch all release scratch`() {
        val invalid =
            listOf(
                listOf(header(), data(bytes.copyOf(bytes.size - 1))),
                listOf(header(), data(bytes + byteArrayOf(1))),
                listOf(header(), header()),
                listOf(header(), data(ByteArray(STREAM_LIMIT + 1))),
                listOf(header(), data(ByteArray(bytes.size))),
                emptyList(),
                listOf(data(bytes)),
            )
        invalid.forEach { chunks ->
            val error = assertThrows(StatusRuntimeException::class.java) { ArtworkTransfer.receive(chunks.iterator()) }
            assertEquals(Status.Code.DATA_LOSS, error.status.code)
            assertAllScratchAvailable()
        }
    }

    @Test
    fun `absent original cancellation receipt remains nullable while malformed present receipt fails`() {
        val channel =
            io.grpc.netty.NettyChannelBuilder
                .forTarget("localhost:1")
                .usePlaintext()
                .build()
        try {
            val graphql = ArtworkGraphql(CatalogServiceGrpc.newBlockingStub(channel), MediaServiceGrpc.newStub(channel), null)
            val operation = UUID.randomUUID()
            val failed =
                Spoolin.Outcome
                    .newBuilder()
                    .setOperationId(operation.toString())
                    .setState("FAILED")
                    .setProblem(Spoolin.Problem.newBuilder().setCode("NOT_FOUND"))
            val missing =
                Spoolin.ArtworkCancelOutcome
                    .newBuilder()
                    .setOperation(failed)
                    .setCancelled(false)
                    .build()
            val result = graphql.cancellationResult(missing, operation, UUID.randomUUID())
            assertNull(result["uploadOperation"])
            assertEquals(false, result["cancelled"])
            val malformed = missing.toBuilder().setUploadOperation(Spoolin.Outcome.getDefaultInstance()).build()
            assertEquals(
                Status.Code.DATA_LOSS,
                assertThrows(StatusRuntimeException::class.java) {
                    graphql.cancellationResult(malformed, operation, UUID.randomUUID())
                }.status.code,
            )
        } finally {
            channel.shutdownNow()
        }
    }

    private fun header(): Spoolin.AssetReadChunk =
        Spoolin.AssetReadChunk
            .newBuilder()
            .setHeader(
                Spoolin.AssetReadHeader
                    .newBuilder()
                    .setFormat("png")
                    .setWidth(1)
                    .setHeight(1)
                    .setOutputBytes(bytes.size.toLong())
                    .setContentHash(ByteString.copyFrom(MessageDigest.getInstance("SHA-256").digest(bytes))),
            ).build()

    private fun data(value: ByteArray): Spoolin.AssetReadChunk =
        Spoolin.AssetReadChunk
            .newBuilder()
            .setData(ByteString.copyFrom(value))
            .build()

    private fun assertAllScratchAvailable() {
        val files = mutableListOf<java.nio.file.Path>()
        try {
            repeat(SCRATCH_LIMIT) { files.add(ArtworkScratch.create()) }
            assertEquals(SCRATCH_LIMIT, files.size)
        } finally {
            files.forEach(ArtworkScratch::delete)
        }
    }
}
