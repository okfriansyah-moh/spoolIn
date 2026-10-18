package com.spoolin.media.features.processing

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/** A missing namespace sandbox is a processing failure, never an unrestricted fallback. */
class CodecSandbox {
    fun execute(input: Path, scratch: Path, codecArguments: List<String>, deadline: Long): String? {
        val outputName = if (codecArguments.first().endsWith("spoolin-image-header")) {
            "dimensions"
        } else {
            codecArguments[3].substringAfterLast('/').substringBefore('[')
        }
        require(outputName.matches(Regex("dimensions|encoded\\.(jpeg|png|webp)")))
        val output = scratch.resolve(outputName)
        Files.deleteIfExists(output)
        Files.createFile(output)
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
            "/usr/bin/prlimit", "--as=268435456", "--cpu=5", "--fsize=${if (outputName == "dimensions") 64 else 8388608}", "--nofile=64", "--",
        ) + codecArguments
        return runCatching { boundedProcess(command, deadline) }.getOrDefault("PROCESSING_SANDBOX_UNAVAILABLE")
    }

    private fun boundedProcess(command: List<String>, deadline: Long): String? {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val captured = java.io.ByteArrayOutputStream()
        val reader = Thread {
            runCatching { process.inputStream.use { stream ->
                val buffer = ByteArray(4096)
                var count = stream.read(buffer)
                while (count >= 0) {
                    val keep = minOf(count, 4096 - captured.size())
                    if (keep > 0) captured.write(buffer, 0, keep)
                    count = stream.read(buffer)
                }
            } }
        }.apply { isDaemon = true; start() }
        try {
            if (!process.waitFor((deadline - System.nanoTime()).coerceAtLeast(1), TimeUnit.NANOSECONDS)) {
                return "PROCESSING_TIMEOUT"
            }
            reader.join(1000)
            if (process.exitValue() == 0) return null
            val diagnostic = captured.toString(Charsets.UTF_8)
            return if (diagnostic.contains("bwrap:") || diagnostic.contains("prlimit:")) {
                "PROCESSING_SANDBOX_UNAVAILABLE"
            } else {
                "PROCESSING_FAILED"
            }
        } finally {
            process.descendants().forEach { it.destroyForcibly() }
            if (process.isAlive) process.destroyForcibly()
            process.waitFor(1, TimeUnit.SECONDS)
            process.inputStream.close()
            reader.join(1000)
        }
    }
}

class CodecFailure(val errorCode: String) : RuntimeException(errorCode)
