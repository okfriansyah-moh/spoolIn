package com.spoolin.media.features.processing

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/** A missing namespace sandbox is a processing failure, never an unrestricted fallback. */
class CodecSandbox {
    fun execute(input: Path, scratch: Path, codecArguments: List<String>, deadline: Long): String? {
        val outputName = if (codecArguments.first().endsWith("spoolin-image-header")) {
            "dimensions"
        } else {
            codecArguments[CODEC_OUTPUT_ARGUMENT].substringAfterLast('/').substringBefore('[')
        }
        require(outputName.matches(Regex("dimensions|encoded\\.(jpeg|png|webp)")))
        val output = scratch.resolve(outputName)
        Files.deleteIfExists(output)
        Files.createFile(output)
        val outputLimit = if (outputName == "dimensions") ProcessingBounds.MAX_HEADER_BYTES
            else ProcessingBounds.MAX_OUTPUT_BYTES
        val command = listOf(
            "/usr/bin/bwrap", "--unshare-all", "--die-with-parent", "--new-session",
            "--ro-bind", "/usr", "/usr", "--ro-bind", "/lib", "/lib",
            "--ro-bind", "/lib64", "/lib64", "--proc", "/proc", "--dev", "/dev",
            "--size", "25161728", "--tmpfs", "/tmp",
            "--ro-bind", input.toAbsolutePath().toString(), "/input",
            "--dir", "/output", "--bind", output.toAbsolutePath().toString(), "/output/$outputName",
            "--remount-ro", "/", "--chdir", "/output",
            "--clearenv", "--setenv", "PATH", "/usr/local/bin:/usr/bin",
            "--setenv", "LD_LIBRARY_PATH", "/usr/local/lib/x86_64-linux-gnu:/usr/local/lib/aarch64-linux-gnu",
            "--setenv", "TMPDIR", "/tmp", "--setenv", "VIPS_CONCURRENCY", "1",
            "/usr/bin/prlimit", "--as=268435456", "--cpu=5",
            "--fsize=$outputLimit",
            "--nofile=64", "--",
        ) + codecArguments
        return try { boundedProcess(command, deadline) }
        catch (_: IOException) { "PROCESSING_SANDBOX_UNAVAILABLE" }
        catch (_: SecurityException) { "PROCESSING_SANDBOX_UNAVAILABLE" }
        catch (_: IllegalArgumentException) { "PROCESSING_SANDBOX_UNAVAILABLE" }
        catch (_: UnsupportedOperationException) { "PROCESSING_SANDBOX_UNAVAILABLE" }
        catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            "PROCESSING_SANDBOX_UNAVAILABLE"
        }
    }

    private fun boundedProcess(command: List<String>, deadline: Long): String? {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val captured = ByteArrayOutputStream()
        val reader = drainOutput(process, captured)
        return try {
            if (!process.waitFor((deadline - System.nanoTime()).coerceAtLeast(1), TimeUnit.NANOSECONDS)) {
                "PROCESSING_TIMEOUT"
            } else {
                reader.join(READER_JOIN_MILLIS)
                exitError(process, captured)
            }
        } finally {
            terminate(process, reader)
        }
    }

    private fun exitError(process: Process, captured: ByteArrayOutputStream): String? {
        if (process.exitValue() == 0) return null
        val diagnostic = captured.toString(Charsets.UTF_8)
        return if (diagnostic.contains("bwrap:") || diagnostic.contains("prlimit:")) {
            "PROCESSING_SANDBOX_UNAVAILABLE"
        } else {
            "PROCESSING_FAILED"
        }
    }

    private fun drainOutput(process: Process, captured: ByteArrayOutputStream): Thread = Thread {
        try { process.inputStream.use { stream -> capture(stream, captured) } }
        catch (_: IOException) { /* Closing the owned process stream ends the drain. */ }
    }.apply { isDaemon = true; start() }

    private fun capture(stream: InputStream, captured: ByteArrayOutputStream) {
        val buffer = ByteArray(DIAGNOSTIC_BYTES)
        var count = stream.read(buffer)
        while (count >= 0) {
            val keep = minOf(count, DIAGNOSTIC_BYTES - captured.size())
            if (keep > 0) captured.write(buffer, 0, keep)
            count = stream.read(buffer)
        }
    }

    private fun terminate(process: Process, reader: Thread) {
        process.descendants().forEach { it.destroyForcibly() }
        if (process.isAlive) process.destroyForcibly()
        process.waitFor(1, TimeUnit.SECONDS)
        process.inputStream.close()
        reader.join(READER_JOIN_MILLIS)
    }
}

class CodecFailure(val errorCode: String) : RuntimeException(errorCode)

private const val CODEC_OUTPUT_ARGUMENT = 3
private const val DIAGNOSTIC_BYTES = 4096
private const val READER_JOIN_MILLIS = 1000L
