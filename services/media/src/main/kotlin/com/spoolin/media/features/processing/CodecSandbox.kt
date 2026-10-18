package com.spoolin.media.features.processing

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/** A missing namespace sandbox is a processing failure, never an unrestricted fallback. */
class CodecSandbox {
    fun execute(input: Path, scratch: Path, codecArguments: List<String>, deadline: Long): String? {
        val log = scratch.resolve("diagnostic")
        val command = listOf(
            "/usr/bin/bwrap", "--unshare-all", "--die-with-parent", "--new-session",
            "--ro-bind", "/usr", "/usr", "--ro-bind", "/lib", "/lib",
            "--ro-bind", "/lib64", "/lib64", "--proc", "/proc", "--dev", "/dev",
            "--size", "16777216", "--tmpfs", "/tmp",
            "--ro-bind", input.toAbsolutePath().toString(), "/input",
            "--bind", scratch.toAbsolutePath().toString(), "/output", "--chdir", "/output",
            "--clearenv", "--setenv", "PATH", "/usr/local/bin:/usr/bin",
            "--setenv", "LD_LIBRARY_PATH", "/usr/local/lib/x86_64-linux-gnu:/usr/local/lib/aarch64-linux-gnu",
            "--setenv", "TMPDIR", "/tmp", "--setenv", "VIPS_CONCURRENCY", "1",
            "/usr/bin/prlimit", "--as=268435456", "--cpu=5", "--fsize=8388608", "--nofile=64", "--",
        ) + codecArguments
        return runCatching { boundedProcess(command, log, deadline) }.getOrDefault("PROCESSING_SANDBOX_UNAVAILABLE")
    }

    private fun boundedProcess(command: List<String>, log: Path, deadline: Long): String? {
        val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start()
        try {
            if (!process.waitFor((deadline - System.nanoTime()).coerceAtLeast(1), TimeUnit.NANOSECONDS)) {
                return "PROCESSING_TIMEOUT"
            }
            if (process.exitValue() == 0) return null
            val diagnostic = Files.newInputStream(log).use { String(it.readNBytes(4096), Charsets.UTF_8) }
            return if (diagnostic.contains("bwrap:") || diagnostic.contains("prlimit:")) {
                "PROCESSING_SANDBOX_UNAVAILABLE"
            } else {
                "PROCESSING_FAILED"
            }
        } finally {
            process.descendants().forEach { it.destroyForcibly() }
            if (process.isAlive) process.destroyForcibly()
            process.waitFor(1, TimeUnit.SECONDS)
            Files.deleteIfExists(log)
        }
    }
}

class CodecFailure(val errorCode: String) : RuntimeException(errorCode)
