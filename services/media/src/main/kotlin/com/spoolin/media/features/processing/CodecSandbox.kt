package com.spoolin.media.features.processing

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/** A missing namespace sandbox is a processing failure, never an unrestricted fallback. */
fun interface CodecExecution {
    fun execute(input: Path, scratch: Path, codecArguments: List<String>, deadline: Long): String?
}

class CodecSandbox : CodecExecution {
    override fun execute(
        input: Path,
        scratch: Path,
        codecArguments: List<String>,
        deadline: Long,
    ): String? {
        val command = CodecCommand.build(input, scratch, codecArguments)
        return try {
            boundedProcess(command, deadline, codecArguments.first().endsWith("spoolin-image-header"))
        } catch (
            _: IOException,
        ) {
            "PROCESSING_SANDBOX_UNAVAILABLE"
        } catch (
            _: SecurityException,
        ) {
            "PROCESSING_SANDBOX_UNAVAILABLE"
        } catch (
            _: IllegalArgumentException,
        ) {
            "PROCESSING_SANDBOX_UNAVAILABLE"
        } catch (
            _: UnsupportedOperationException,
        ) {
            "PROCESSING_SANDBOX_UNAVAILABLE"
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            "PROCESSING_SANDBOX_UNAVAILABLE"
        }
    }

    private fun boundedProcess(
        command: List<String>,
        deadline: Long,
        headerRead: Boolean,
    ): String? {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val captured = ByteArrayOutputStream()
        val reader = drainOutput(process, captured)
        return try {
            if (!process.waitFor((deadline - System.nanoTime()).coerceAtLeast(1), TimeUnit.NANOSECONDS)) {
                "PROCESSING_TIMEOUT"
            } else {
                reader.join(READER_JOIN_MILLIS)
                exitError(process, captured, headerRead)
            }
        } finally {
            terminate(process, reader)
        }
    }

    private fun exitError(
        process: Process,
        captured: ByteArrayOutputStream,
        headerRead: Boolean,
    ): String? {
        if (process.exitValue() == 0) return null
        val diagnostic = captured.toString(Charsets.UTF_8)
        return if (diagnostic.contains("bwrap:") || diagnostic.contains("prlimit:")) {
            "PROCESSING_SANDBOX_UNAVAILABLE"
        } else if (headerRead && process.exitValue() == 3) {
            "INVALID_IMAGE_DIMENSIONS"
        } else {
            "PROCESSING_FAILED"
        }
    }

    private fun drainOutput(
        process: Process,
        captured: ByteArrayOutputStream,
    ): Thread =
        Thread {
            try {
                process.inputStream.use { stream ->
                    capture(stream, captured)
                }
            } catch (_: IOException) {
                // Closing the owned process stream ends the drain.
            }
        }.apply {
            isDaemon = true
            start()
        }

    private fun capture(
        stream: InputStream,
        captured: ByteArrayOutputStream,
    ) {
        val buffer = ByteArray(DIAGNOSTIC_BYTES)
        var count = stream.read(buffer)
        while (count >= 0) {
            val keep = minOf(count, DIAGNOSTIC_BYTES - captured.size())
            if (keep > 0) captured.write(buffer, 0, keep)
            count = stream.read(buffer)
        }
    }

    private fun terminate(
        process: Process,
        reader: Thread,
    ) {
        process.descendants().forEach { it.destroyForcibly() }
        if (process.isAlive) process.destroyForcibly()
        process.waitFor(1, TimeUnit.SECONDS)
        process.inputStream.close()
        reader.join(READER_JOIN_MILLIS)
    }
}

class CodecFailure(
    val errorCode: String,
) : RuntimeException(errorCode)

private const val DIAGNOSTIC_BYTES = 4096
private const val READER_JOIN_MILLIS = 1000L
