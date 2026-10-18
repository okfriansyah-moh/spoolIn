package com.spoolin.media.features.processing

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID

class ProcessorRunner(
    private val libvipsExecutable: String = "/usr/local/bin/vips",
    private val sandbox: CodecSandbox = CodecSandbox(),
) {
    sealed class Outcome {
        data class Success(
            val outputPath: Path, val format: ImageFormat, val outputBytes: Long,
            val width: Int, val height: Int, val contentHash: ByteArray,
        ) : Outcome()
        data class Rejected(val errorCode: String) : Outcome()
    }

    fun process(inputPath: Path, outputDir: Path, format: ImageFormat): Outcome =
        process(inputPath, outputDir, format, UUID.randomUUID())

    fun process(inputPath: Path, outputDir: Path, format: ImageFormat, assetId: UUID): Outcome {
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10)
        val target = outputDir.resolve("$assetId.${format.label.lowercase()}")
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return inspect(target, format, outputDir, deadline)
        val scratch = Files.createTempDirectory(outputDir, "attempt-")
        try {
            val dimensions = dimensions(inputPath, scratch, deadline) ?: return Outcome.Rejected("INVALID_IMAGE_DIMENSIONS")
            val options = when (format) {
                ImageFormat.JPEG -> "[Q=85,optimize_coding=true,strip=true]"
                ImageFormat.PNG -> "[compression=6,strip=true]"
                ImageFormat.WEBP -> "[Q=85,strip=true]"
            }
            val fileName = "encoded.${format.label.lowercase()}"
            val error = sandbox.execute(inputPath, scratch, listOf(
                libvipsExecutable, "autorot", "/input", "/output/$fileName$options",
                "--vips-concurrency", "1", "--vips-cache-max", "0",
            ), deadline)
            if (error != null) return Outcome.Rejected(error)
            val result = scratch.resolve(fileName)
            val inspected = inspect(result, format, outputDir, deadline)
            if (inspected !is Outcome.Success) return inspected
            require(dimensions.first > 0)
            FileChannel.open(result, StandardOpenOption.WRITE).use { it.force(true) }
            // A hard link publishes complete bytes atomically without replacing a concurrent winner.
            try { Files.createLink(target, result) } catch (_: java.nio.file.FileAlreadyExistsException) {
                return inspect(target, format, outputDir, deadline)
            }
            FileChannel.open(outputDir, StandardOpenOption.READ).use { it.force(true) }
            return inspected.copy(outputPath = target)
        } finally {
            Files.list(scratch).use { paths -> paths.forEach { Files.deleteIfExists(it) } }
            Files.deleteIfExists(scratch)
        }
    }

    private fun inspect(path: Path, format: ImageFormat, outputDir: Path, deadline: Long): Outcome {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return Outcome.Rejected("INVALID_OUTPUT")
        val size = Files.size(path)
        if (size !in 1..ProcessingBounds.MAX_OUTPUT_BYTES) return Outcome.Rejected("OUTPUT_TOO_LARGE")
        val scratch = Files.createTempDirectory(outputDir, "inspect-")
        try {
            val dims = dimensions(path, scratch, deadline) ?: return Outcome.Rejected("PROCESSING_OUTPUT_UNREADABLE")
            val hash = Files.newInputStream(path).use { stream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(65536)
                var count = stream.read(buffer)
                while (count >= 0) { digest.update(buffer, 0, count); count = stream.read(buffer) }
                digest.digest()
            }
            return Outcome.Success(path, format, size, dims.first, dims.second, hash)
        } finally {
            Files.list(scratch).use { paths -> paths.forEach { Files.deleteIfExists(it) } }
            Files.deleteIfExists(scratch)
        }
    }

    private fun dimensions(input: Path, scratch: Path, deadline: Long): Pair<Int, Int>? {
        val error = sandbox.execute(input, scratch, listOf(
            "/usr/local/bin/spoolin-image-header", "/input", "/output/dimensions",
        ), deadline)
        if (error != null) throw CodecFailure(error)
        val header = scratch.resolve("dimensions")
        if (!Files.isRegularFile(header) || Files.size(header) > 64) return null
        val fields = Files.readString(header).trim().split(' ')
        val width = fields.getOrNull(0)?.toIntOrNull() ?: return null
        val height = fields.getOrNull(1)?.toIntOrNull() ?: return null
        val bands = fields.getOrNull(2)?.toIntOrNull() ?: return null
        if (width !in 1..8192 || height !in 1..8192 || bands !in 1..4) return null
        if (width.toLong() * height > ProcessingBounds.MAX_PIXELS) return null
        if (width.toLong() * height * bands > ProcessingBounds.MAX_DECODED_BYTES) return null
        return width to height
    }
}
