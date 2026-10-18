package com.spoolin.core.features.artworkproxy

import io.grpc.Context
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val SPOOL_LIMIT = 1024L
private const val CANCEL_AFTER_MS = 5000L
private const val CANCEL_WAIT_SECONDS = 5L

class ArtworkUploadBoundaryTest {
    private val input = "synthetic bounded artwork input".toByteArray()

    @Test
    fun `production uploader sends exact bytes digest immutable intent and actor-bound context over mutual TLS`() {
        ArtworkUploadFixture(ArtworkUploadFixture.Mode.NORMAL).use { f ->
            spool().use { spool ->
                val result = ArtworkUpload.upload(f.request(spool))
                assertEquals(f.operation.toString(), result.operationId)
                assertEquals("PENDING", result.state)
                val header = requireNotNull(f.header.get())
                assertEquals(f.operation.toString(), header.context.operationId)
                assertEquals(f.correlation.toString(), header.context.correlationId)
                assertEquals("p1", header.context.policyRevision)
                assertEquals(f.asset.toString(), header.assetId)
                assertEquals(f.intent.toString(), header.intentId)
                assertEquals(input.size.toLong(), header.inputBytes)
                assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(input), header.inputSha256.toByteArray())
                assertArrayEquals(input, f.bytes.toByteArray())
            }
        }
    }

    @Test
    fun `duplicate unary response is rejected at the real protocol boundary instead of accepted as success`() {
        ArtworkUploadFixture(ArtworkUploadFixture.Mode.DUPLICATE).use { f ->
            spool().use { spool ->
                val error = assertThrows(StatusRuntimeException::class.java) { ArtworkUpload.upload(f.request(spool)) }
                assertTrue(error.status.code in setOf(Status.Code.INTERNAL, Status.Code.DATA_LOSS))
            }
        }
    }

    @Test
    fun `mismatched operation identity in actual Media response fails closed`() {
        ArtworkUploadFixture(ArtworkUploadFixture.Mode.MISMATCH).use { f ->
            spool().use { spool ->
                val error = assertThrows(StatusRuntimeException::class.java) { ArtworkUpload.upload(f.request(spool)) }
                assertEquals(Status.Code.DATA_LOSS, error.status.code)
            }
        }
    }

    @Test
    fun `parent request expiry cancels actual hanging downstream stream and releases scratch`() {
        ArtworkUploadFixture(ArtworkUploadFixture.Mode.HOLD).use { assertCancellation(it) }
    }

    private fun assertCancellation(fixture: ArtworkUploadFixture) {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        try {
            val saved = spool()
            val path = saved.path
            saved.use { assertDeadline(fixture, it, scheduler) }
            assertFalse(
                java.nio.file.Files
                    .exists(path),
            )
        } finally {
            scheduler.shutdownNow()
        }
    }

    private fun assertDeadline(
        fixture: ArtworkUploadFixture,
        spool: ArtworkSpool,
        scheduler: java.util.concurrent.ScheduledExecutorService,
    ) {
        val deadline = Context.current().withDeadlineAfter(CANCEL_AFTER_MS, TimeUnit.MILLISECONDS, scheduler)
        try {
            val error =
                assertThrows(StatusRuntimeException::class.java) {
                    deadline.call(Callable { ArtworkUpload.upload(fixture.request(spool)) })
                }
            assertTrue(error.status.code in setOf(Status.Code.DEADLINE_EXCEEDED, Status.Code.CANCELLED))
            assertTrue(fixture.cancelled.await(CANCEL_WAIT_SECONDS, TimeUnit.SECONDS))
        } finally {
            deadline.cancel(null)
        }
    }

    private fun spool(): ArtworkSpool = ArtworkSpool.decode(Base64.getEncoder().encodeToString(input), SPOOL_LIMIT)
}
