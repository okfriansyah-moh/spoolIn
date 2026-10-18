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
        val deadline = System.nanoTime() +
            java.util.concurrent.TimeUnit.SECONDS.toNanos(ProcessingBounds.MAX_WALL_SECONDS)
        val target = outputDir.resolve("$assetId.${format.label.lowercase()}")
        return if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) inspect(target, format, outputDir, deadline)
        else encode(inputPath, outputDir, format, target, deadline)
    }

    private fun encode(input: Path, output: Path, format: ImageFormat, target: Path, deadline: Long): Outcome {
        val scratch = Files.createDirectory(output.resolve(UUID.randomUUID().toString()))
        try {
            val dimensions = dimensions(input, scratch, deadline) ?: return Outcome.Rejected("INVALID_IMAGE_DIMENSIONS")
            val fileName = "encoded.${format.label.lowercase()}"
            val error = sandbox.execute(input, scratch, codecArguments(format, fileName), deadline)
            return if (error != null) Outcome.Rejected(error)
            else publish(scratch.resolve(fileName), target, format, output, deadline, dimensions)
        } finally {
            clearScratch(scratch)
        }
    }

    private fun codecArguments(format: ImageFormat, fileName: String): List<String> {
        val options = when (format) {
            ImageFormat.JPEG -> "[Q=85,optimize_coding=true,strip=true]"
            ImageFormat.PNG -> "[compression=6,strip=true]"
            ImageFormat.WEBP -> "[Q=85,strip=true]"
        }
        return listOf(libvipsExecutable, "autorot", "/input", "/output/$fileName$options",
            "--vips-concurrency", "1", "--vips-cache-max", "0")
    }

    private fun publish(result: Path, target: Path, format: ImageFormat, output: Path, deadline: Long,
        dimensions: Pair<Int, Int>): Outcome {
        val inspected = inspect(result, format, output, deadline)
        if (inspected !is Outcome.Success) return inspected
        require(dimensions.first > 0)
        FileChannel.open(result, StandardOpenOption.WRITE).use { it.force(true) }
        // A hard link publishes complete bytes atomically without replacing a concurrent winner.
        return try {
            Files.createLink(target, result)
            FileChannel.open(output, StandardOpenOption.READ).use { it.force(true) }
            inspected.copy(outputPath = target)
        } catch (_: java.nio.file.FileAlreadyExistsException) {
            inspect(target, format, output, deadline)
        }
    }

    private fun inspect(path: Path, format: ImageFormat, outputDir: Path, deadline: Long): Outcome =
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) Outcome.Rejected("INVALID_OUTPUT")
        else inspectRegular(path, format, outputDir, deadline)

    private fun inspectRegular(path: Path, format: ImageFormat, outputDir: Path, deadline: Long): Outcome {
        val size = Files.size(path)
        if (size !in 1..ProcessingBounds.MAX_OUTPUT_BYTES) return Outcome.Rejected("OUTPUT_TOO_LARGE")
        val scratch = Files.createDirectory(outputDir.resolve(UUID.randomUUID().toString()))
        return try {
            val dims = dimensions(path, scratch, deadline)
            if (dims == null) Outcome.Rejected("PROCESSING_OUTPUT_UNREADABLE")
            else Outcome.Success(path, format, size, dims.first, dims.second, hash(path))
        } finally {
            clearScratch(scratch)
        }
    }

    private fun hash(path: Path): ByteArray = Files.newInputStream(path).use { stream ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(ProcessingBounds.CHUNK_BYTES)
        var count = stream.read(buffer)
        while (count >= 0) { digest.update(buffer, 0, count); count = stream.read(buffer) }
        digest.digest()
    }

    private fun clearScratch(scratch: Path) {
        Files.list(scratch).use { paths -> paths.forEach { Files.deleteIfExists(it) } }
        Files.deleteIfExists(scratch)
    }

    private fun dimensions(input: Path, scratch: Path, deadline: Long): Pair<Int, Int>? {
        val error = sandbox.execute(input, scratch, listOf(
            "/usr/local/bin/spoolin-image-header", "/input", "/output/dimensions",
        ), deadline)
        if (error != null) throw CodecFailure(error)
        val header = scratch.resolve("dimensions")
        return if (!Files.isRegularFile(header) || Files.size(header) > ProcessingBounds.MAX_HEADER_BYTES) null
        else parseDimensions(Files.readString(header))
    }

}

private const val DIMENSION_FIELD_COUNT = 3

private fun parseDimensions(header: String): Pair<Int, Int>? {
    val fields = header.trim().split(' ').take(DIMENSION_FIELD_COUNT).map { it.toIntOrNull() }
    if (fields.size != DIMENSION_FIELD_COUNT || fields.any { it == null }) return null
    val width = requireNotNull(fields[0])
    val height = requireNotNull(fields[1])
    val bands = requireNotNull(fields[2])
    return if (validDimensions(width, height, bands)) width to height else null
}

private fun validDimensions(width: Int, height: Int, bands: Int): Boolean =
    width in 1..ProcessingBounds.MAX_DIMENSION_PX && height in 1..ProcessingBounds.MAX_DIMENSION_PX &&
        bands in 1..ProcessingBounds.MAX_COLOR_BANDS && width.toLong() * height <= ProcessingBounds.MAX_PIXELS &&
        width.toLong() * height * bands <= ProcessingBounds.MAX_DECODED_BYTES
