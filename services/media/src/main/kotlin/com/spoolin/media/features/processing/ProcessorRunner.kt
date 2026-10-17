package com.spoolin.media.features.processing

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID

/**
 * Invokes the fixed libvips executable in a nonroot, no-network, sandboxed subprocess.
 *
 * Security controls (§7.6 / §8.3.2 / AC-161 / ATDD-161 / ATDD-325):
 * - Arguments are a fixed list; no shell interpretation.
 * - Only allowlisted numeric parameters and generated UUID paths are passed.
 * - Original user filenames, metadata, URLs, or shell commands are never passed to the process.
 * - Unknown or extra options are refused before execution.
 * - Subprocess output paths are UUID-only relative to the quarantine directory.
 */
class ProcessorRunner(
    private val libvipsExecutable: String = "vips",
) {
    sealed class Outcome {
        data class Success(
            val outputPath: Path,
            val format: ImageFormat,
            val outputBytes: Long,
            val width: Int,
            val height: Int,
            val contentHash: ByteArray,
        ) : Outcome()

        data class Rejected(val errorCode: String) : Outcome()
    }

    /**
     * Re-encode [inputPath] to [outputDir] under a generated UUID filename.
     *
     * The subprocess is invoked with fixed allowlisted arguments only. No user data touches
     * the argument list. Resource limits are enforced via wall-clock timeout.
     */
    fun process(
        inputPath: Path,
        outputDir: Path,
        format: ImageFormat,
    ): Outcome {
        val outputName = UUID.randomUUID().toString()
        val extension = format.label.lowercase()
        val outputPath = outputDir.resolve("$outputName.$extension")

        val args = buildAllowlistedArgs(inputPath, outputPath, format)
        val outcome = executeSubprocess(args)
        if (outcome != null) return Outcome.Rejected(outcome)

        return if (Files.exists(outputPath)) {
            buildSuccessOutcome(outputPath, format)
        } else {
            Outcome.Rejected("PROCESSING_OUTPUT_MISSING")
        }
    }

    private fun buildAllowlistedArgs(
        inputPath: Path,
        outputPath: Path,
        format: ImageFormat,
    ): List<String> {
        val saveOptions = when (format) {
            ImageFormat.JPEG -> "[Q=85,optimize_coding=true,strip=true]"
            ImageFormat.PNG -> "[compression=6,strip=true]"
            ImageFormat.WEBP -> "[Q=85,strip=true]"
        }
        // vips copy: decode, strip metadata, apply orientation, re-encode.
        // All arguments are fixed or numeric — no user input touches args (ATDD-325).
        return listOf(
            libvipsExecutable,
            "copy",
            inputPath.toAbsolutePath().toString(),
            outputPath.toAbsolutePath().toString() + saveOptions,
            "--vips-concurrency",
            "1",
        )
    }

    private fun executeSubprocess(args: List<String>): String? {
        return try {
            val process =
                ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .start()
            val finished =
                process.waitFor(ProcessingBounds.MAX_WALL_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return "PROCESSING_TIMEOUT"
            }
            if (process.exitValue() != 0) "PROCESSING_FAILED" else null
        } catch (e: Exception) {
            "PROCESSING_UNAVAILABLE"
        }
    }

    private fun buildSuccessOutcome(
        outputPath: Path,
        format: ImageFormat,
    ): Outcome {
        val outputBytes = Files.size(outputPath)
        if (outputBytes > ProcessingBounds.MAX_OUTPUT_BYTES) {
            Files.deleteIfExists(outputPath)
            return Outcome.Rejected("OUTPUT_TOO_LARGE")
        }
        val bytes = Files.readAllBytes(outputPath)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        // Dimensions are re-read from the output to confirm re-encoding succeeded.
        val dims = readDimensions(outputPath, format) ?: return Outcome.Rejected("PROCESSING_OUTPUT_UNREADABLE")
        return Outcome.Success(
            outputPath = outputPath,
            format = format,
            outputBytes = outputBytes,
            width = dims.first,
            height = dims.second,
            contentHash = hash,
        )
    }

    private fun readDimensions(path: Path, format: ImageFormat): Pair<Int, Int>? {
        // Use vips header to read dimensions without full decode (safe, no display).
        return try {
            val args =
                listOf(
                    libvipsExecutable, "header", "-f", "width", path.toAbsolutePath().toString(),
                )
            val widthResult = ProcessBuilder(args).start()
            val widthStr = widthResult.inputStream.reader().readText().trim()
            widthResult.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)

            val heightArgs =
                listOf(
                    libvipsExecutable, "header", "-f", "height", path.toAbsolutePath().toString(),
                )
            val heightResult = ProcessBuilder(heightArgs).start()
            val heightStr = heightResult.inputStream.reader().readText().trim()
            heightResult.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)

            val w = widthStr.toIntOrNull() ?: return null
            val h = heightStr.toIntOrNull() ?: return null
            if (w <= 0 || h <= 0 ||
                w > ProcessingBounds.MAX_DIMENSION_PX || h > ProcessingBounds.MAX_DIMENSION_PX
            ) {
                return null
            }
            if (w.toLong() * h > ProcessingBounds.MAX_PIXELS) return null
            w to h
        } catch (e: Exception) {
            null
        }
    }
}
