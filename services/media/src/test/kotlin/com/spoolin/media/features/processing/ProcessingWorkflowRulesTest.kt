package com.spoolin.media.features.processing

import com.spoolin.media.features.upload.QuarantineStore
import com.spoolin.media.fixtures.JdbcBoundary
import com.spoolin.media.fixtures.actorId
import com.spoolin.media.fixtures.assetId
import com.spoolin.media.fixtures.assetRecord
import com.spoolin.media.fixtures.instant
import com.spoolin.media.fixtures.operationId
import com.spoolin.support.work.WorkClaim
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Clock
import java.time.ZoneOffset
import javax.imageio.ImageIO

class ProcessingWorkflowRulesTest {
    @TempDir lateinit var root: Path
    private val claim =
        WorkClaim(operationId, operationId, "MEDIA_PROCESS", "{}", 1, 4, instant.plusSeconds(120), actorId, 1, operationId, 100, 1000, 2, 0)
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)

    private fun png(): ByteArray =
        ByteArrayOutputStream()
            .also {
                ImageIO.write(BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB), "png", it)
            }.toByteArray()

    private fun record(bytes: ByteArray) =
        assetRecord("PROCESSING") +
            mapOf("input_bytes" to bytes.size.toLong(), "processing_input_hash" to MessageDigest.getInstance("SHA-256").digest(bytes))

    private fun db(row: Map<String, Any?>) = JdbcBoundary().rows(row).rows().rows(row)

    private fun processor() =
        ProcessorRunner(
            sandbox =
                CodecExecution { _, scratch, args, _ ->
                    if (args.first().endsWith("spoolin-image-header")) {
                        Files.writeString(scratch.resolve("dimensions"), "2 3 3")
                    } else {
                        Files.write(scratch.resolve("encoded.png"), png())
                    }
                    null
                },
        )

    @Test fun `successful processing commits ready output before deleting accepted private input`() {
        val bytes = png()
        val store = QuarantineStore(root)
        Files.write(store.stagingPath(assetId), bytes)
        val db = db(record(bytes))
        MediaProcessingHandler(db.source, store, processor(), clock).handle(claim)
        assertEquals(1, db.commits)
        assertFalse(Files.exists(store.stagingPath(assetId)))
        assertTrue(Files.isRegularFile(store.outputDirectory().resolve("$assetId.png")))
    }

    @Test fun `lost terminal fence keeps accepted input for the winning worker`() {
        val bytes = png()
        val store = QuarantineStore(root)
        Files.write(store.stagingPath(assetId), bytes)
        val db = JdbcBoundary().rows(record(bytes)).rows().apply { updates.add(0) }
        MediaProcessingHandler(db.source, store, processor(), clock).handle(claim)
        assertTrue(Files.exists(store.stagingPath(assetId)))
    }

    @Test fun `missing mismatched corrupt and incomplete accepted inputs are rejected without codec execution`() {
        val store = QuarantineStore(root)
        val valid = png()
        val cases =
            listOf(
                record(valid) + ("processing_staging_key" to "quarantine/other"),
                record(valid) + ("input_bytes" to 1L),
                record(valid) + ("processing_input_hash" to byteArrayOf(0)),
                record(valid) + ("input_bytes" to 0L),
            )
        val mustNotRun = ProcessorRunner(sandbox = CodecExecution { _, _, _, _ -> error("invalid input reached process") })
        for (row in cases) {
            Files.write(store.stagingPath(assetId), valid)
            val db = db(row)
            MediaProcessingHandler(db.source, store, mustNotRun, clock).handle(claim)
            assertEquals(1, db.commits)
            assertFalse(Files.exists(store.stagingPath(assetId)))
        }
        val missing = db(record(valid))
        MediaProcessingHandler(missing.source, store, mustNotRun, clock).handle(claim)
        assertEquals(1, missing.commits)
        for (bytes in listOf("not an image".toByteArray(), valid.copyOfRange(0, 24))) {
            Files.write(store.stagingPath(assetId), bytes)
            MediaProcessingHandler(db(record(bytes)).source, store, mustNotRun, clock).handle(claim)
            assertFalse(Files.exists(store.stagingPath(assetId)))
        }
    }

    @Test fun `claims cannot process another operation or a terminal asset`() {
        val store = QuarantineStore(root)
        assertThrows(IllegalArgumentException::class.java) {
            MediaProcessingHandler(JdbcBoundary().source, store).handle(claim.copy(kind = "OTHER"))
        }
        for (row in listOf(assetRecord("READY"), assetRecord("PROCESSING") + ("upload_operation_id" to actorId))) {
            assertThrows(IllegalStateException::class.java) { MediaProcessingHandler(JdbcBoundary().rows(row).source, store).handle(claim) }
        }
    }
}
