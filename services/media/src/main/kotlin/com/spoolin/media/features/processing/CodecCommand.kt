package com.spoolin.media.features.processing

import java.nio.file.Files
import java.nio.file.Path

private const val CODEC_OUTPUT_ARGUMENT = 3

/** Fixed production sandbox argv; no environment, namespace, mount or resource restrictions are relaxed. */
internal object CodecCommand {
    private val rootMounts =
        listOf(
            "/usr/bin/bwrap",
            "--unshare-all",
            "--die-with-parent",
            "--new-session",
            "--ro-bind",
            "/usr",
            "/usr",
            "--ro-bind",
            "/lib",
            "/lib",
            "--ro-bind",
            "/lib64",
            "/lib64",
            "--proc",
            "/proc",
            "--dev",
            "/dev",
            "--size",
            "25161728",
            "--tmpfs",
            "/tmp",
        )
    private val environment =
        listOf(
            "--remount-ro",
            "/",
            "--chdir",
            "/output",
            "--clearenv",
            "--setenv",
            "PATH",
            "/usr/local/bin:/usr/bin",
            "--setenv",
            "LD_LIBRARY_PATH",
            "/usr/local/lib/x86_64-linux-gnu:/usr/local/lib/aarch64-linux-gnu",
            "--setenv",
            "TMPDIR",
            "/tmp",
            "--setenv",
            "VIPS_CONCURRENCY",
            "1",
        )

    fun build(
        input: Path,
        scratch: Path,
        codecArguments: List<String>,
    ): List<String> {
        val name = outputName(codecArguments)
        require(name.matches(Regex("dimensions|encoded\\.(jpeg|png|webp)")))
        val output = scratch.resolve(name)
        Files.deleteIfExists(output)
        Files.createFile(output)
        val limit =
            if (name == "dimensions") {
                ProcessingBounds.MAX_HEADER_BYTES.toLong()
            } else {
                ProcessingBounds.MAX_OUTPUT_BYTES
            }
        return rootMounts + pathBindings(input, output, name) + environment + resourceLimits(limit) + codecArguments
    }

    private fun outputName(arguments: List<String>): String =
        if (arguments.first().endsWith("spoolin-image-header")) {
            "dimensions"
        } else {
            arguments[CODEC_OUTPUT_ARGUMENT].substringAfterLast('/').substringBefore('[')
        }

    private fun pathBindings(
        input: Path,
        output: Path,
        name: String,
    ): List<String> =
        listOf(
            "--ro-bind",
            input.toAbsolutePath().toString(),
            "/input",
            "--dir",
            "/output",
            "--bind",
            output.toAbsolutePath().toString(),
            "/output/$name",
        )

    private fun resourceLimits(outputLimit: Long): List<String> =
        listOf(
            "/usr/bin/prlimit",
            "--as=268435456",
            "--cpu=5",
            "--fsize=$outputLimit",
            "--nofile=64",
            "--",
        )
}
