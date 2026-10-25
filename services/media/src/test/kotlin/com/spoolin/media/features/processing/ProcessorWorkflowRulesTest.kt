package com.spoolin.media.features.processing

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID

class ProcessorWorkflowRulesTest {
    @TempDir lateinit var root: Path
    private val asset = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val bytes = byteArrayOf(1, 3, 5, 7)

    private fun codec(
        header: String = "20 30 3",
        error: String? = null,
    ) = CodecExecution { _, scratch, args, _ ->
        if (args.first().endsWith("spoolin-image-header")) {
            Files.writeString(scratch.resolve("dimensions"), header)
            null
        } else {
            if (error == null) Files.write(scratch.resolve(args[3].substringAfterLast('/').substringBefore('[')), bytes)
            error
        }
    }

    @Test fun `publishes complete immutable outputs in each supported format`() {
        for (format in ImageFormat.entries) {
            val output = Files.createDirectory(root.resolve(format.name))
            val outcome = ProcessorRunner(sandbox = codec()).process(root.resolve("input"), output, format, asset)
            val result = assertInstanceOf(ProcessorRunner.Outcome.Success::class.java, outcome)
            assertEquals(20, result.width)
            assertEquals(30, result.height)
            assertEquals(4, result.outputBytes)
            assertEquals(format, result.format)
            assertArrayEquals(bytes, Files.readAllBytes(result.outputPath))
            assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(bytes), result.contentHash)
            Files.list(output).use { assertEquals(1L, it.count()) }
        }
    }

    @Test fun `retries inspect an existing winner without replacing its bytes`() {
        val target = root.resolve("$asset.png")
        Files.write(target, byteArrayOf(9, 8))
        val result =
            assertInstanceOf(
                ProcessorRunner.Outcome.Success::class.java,
                ProcessorRunner(sandbox = codec()).process(root.resolve("absent"), root, ImageFormat.PNG, asset),
            )
        assertEquals(target, result.outputPath)
        assertArrayEquals(byteArrayOf(9, 8), Files.readAllBytes(target))
        assertEquals(2L, result.outputBytes)
    }

    @Test fun `rejects malformed or unsafe decoded dimensions and removes scratch`() {
        val headers = listOf("", "12 20", "x 2 3", "0 20 3", "20 -1 3", "20 20 0", "999999 20 3", "8192 8192 4")
        for ((index, header) in headers.withIndex()) {
            val output = Files.createDirectory(root.resolve("invalid$index"))
            val result = ProcessorRunner(sandbox = codec(header)).process(root.resolve("input"), output, ImageFormat.PNG, asset)
            assertEquals(ProcessorRunner.Outcome.Rejected("INVALID_IMAGE_DIMENSIONS"), result, header)
            Files.list(output).use { assertEquals(0L, it.count()) }
        }
    }

    @Test fun `propagates declared codec failures and cleans attempt files`() {
        for (error in listOf("PROCESSING_TIMEOUT", "PROCESSING_FAILED", "PROCESSING_SANDBOX_UNAVAILABLE")) {
            val result = ProcessorRunner(sandbox = codec(error = error)).process(root.resolve("input"), root, ImageFormat.JPEG, asset)
            assertEquals(ProcessorRunner.Outcome.Rejected(error), result)
            Files.list(root).use { assertEquals(0L, it.count()) }
        }
    }

    @Test fun `header process failures retain their safe failure identity`() {
        val sandbox = CodecExecution { _, _, _, _ -> "PROCESSING_TIMEOUT" }
        val error =
            assertThrows(CodecFailure::class.java) {
                ProcessorRunner(sandbox = sandbox).process(root.resolve("input"), root, ImageFormat.PNG, asset)
            }
        assertEquals("PROCESSING_TIMEOUT", error.errorCode)
        Files.list(root).use { assertEquals(0L, it.count()) }
    }

    @Test fun `invalid image header is a rejection rather than an infrastructure failure`() {
        val runner = ProcessorRunner(sandbox = CodecExecution { _, _, _, _ -> "INVALID_IMAGE_DIMENSIONS" })
        assertEquals(
            ProcessorRunner.Outcome.Rejected("INVALID_IMAGE_DIMENSIONS"),
            runner.process(root.resolve("input"), root, ImageFormat.PNG),
        )
    }

    @Test fun `rejects missing oversized and unreadable codec output`() {
        val empty =
            CodecExecution { _, scratch, args, _ ->
                if (args.first().endsWith("spoolin-image-header")) Files.writeString(scratch.resolve("dimensions"), "20 30 3")
                null
            }
        assertEquals(
            ProcessorRunner.Outcome.Rejected("INVALID_OUTPUT"),
            ProcessorRunner(sandbox = empty).process(root.resolve("input"), root, ImageFormat.PNG, asset),
        )
        val target = root.resolve("$asset.png")
        Files.write(target, byteArrayOf())
        assertEquals(
            ProcessorRunner.Outcome.Rejected("OUTPUT_TOO_LARGE"),
            ProcessorRunner(sandbox = empty).process(root.resolve("input"), root, ImageFormat.PNG, asset),
        )
        Files.write(target, bytes)
        assertEquals(
            ProcessorRunner.Outcome.Rejected("PROCESSING_OUTPUT_UNREADABLE"),
            ProcessorRunner(sandbox = codec("bad")).process(root.resolve("input"), root, ImageFormat.PNG, asset),
        )
    }

    @Test fun `never follows a published symbolic link`() {
        val elsewhere = root.resolve("elsewhere")
        Files.write(elsewhere, bytes)
        Files.createSymbolicLink(root.resolve("$asset.png"), elsewhere)
        assertEquals(
            ProcessorRunner.Outcome.Rejected("INVALID_OUTPUT"),
            ProcessorRunner(sandbox = codec()).process(root.resolve("input"), root, ImageFormat.PNG, asset),
        )
    }

    @Test fun `missing and oversized header files are rejected`() {
        for (header in listOf<String?>(null, "1".repeat(ProcessingBounds.MAX_HEADER_BYTES + 1))) {
            val sandbox =
                CodecExecution { _, scratch, _, _ ->
                    header?.let { Files.writeString(scratch.resolve("dimensions"), it) }
                    null
                }
            assertEquals(
                ProcessorRunner.Outcome.Rejected("INVALID_IMAGE_DIMENSIONS"),
                ProcessorRunner(sandbox = sandbox).process(root.resolve("input"), root, ImageFormat.PNG, asset),
            )
        }
    }
}
