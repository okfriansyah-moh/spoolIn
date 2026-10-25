package com.spoolin.media.features.processing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class CodecBoundaryRulesTest {
    @TempDir lateinit var root: Path
    private fun args(name: String = "encoded.png") = listOf("/usr/local/bin/vips", "autorot", "/input", "/output/$name")
    private fun deadline() = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
    private class ProcessBoundary(private val exit: Int, private val output: String = "", private val finishes: Boolean = true) : Process() {
        private var alive = true
        var destroyed = false
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun getInputStream() = ByteArrayInputStream(output.toByteArray())
        override fun getErrorStream() = ByteArrayInputStream(byteArrayOf())
        override fun waitFor(): Int { alive = false; return exit }
        override fun waitFor(timeout: Long, unit: TimeUnit): Boolean { if (finishes) alive = false; return finishes }
        override fun exitValue() = exit
        override fun destroy() { alive = false; destroyed = true }
        override fun destroyForcibly(): Process { destroy(); return this }
        override fun isAlive() = alive
        override fun descendants(): java.util.stream.Stream<ProcessHandle> = java.util.stream.Stream.empty()
    }

    @Test fun `sandbox command constrains filesystem environment namespaces and process resources`() {
        val input = Files.write(root.resolve("input"), byteArrayOf(1))
        for (name in listOf("encoded.jpeg", "encoded.png", "encoded.webp", "dimensions")) {
            val scratch = Files.createDirectory(root.resolve(name.replace('.', '-')))
            val arguments = if (name == "dimensions") listOf("/usr/local/bin/spoolin-image-header", "/input", "/output/dimensions") else args(name)
            val command = CodecCommand.build(input, scratch, arguments)
            assertEquals("/usr/bin/bwrap", command.first())
            for (restriction in listOf("--unshare-all", "--die-with-parent", "--new-session", "--clearenv", "--remount-ro", "--as=268435456", "--cpu=5", "--nofile=64")) {
                assertTrue(restriction in command, restriction)
            }
            assertTrue("--fsize=${if (name == "dimensions") 64 else 8388608}" in command)
            assertTrue(command.windowed(3).any { it == listOf("--ro-bind", input.toAbsolutePath().toString(), "/input") })
            assertTrue(command.windowed(3).any { it == listOf("--bind", scratch.resolve(name).toAbsolutePath().toString(), "/output/$name") })
            assertTrue(Files.isRegularFile(scratch.resolve(name)))
            assertEquals(arguments, command.takeLast(arguments.size))
        }
        assertThrows(IllegalArgumentException::class.java) { CodecCommand.build(input, root, args("secret.txt")) }
    }

    @Test fun `sandbox maps actual process outcomes to safe bounded failures and terminates timed out children`() {
        for ((exit, diagnostic, expected) in listOf(Triple(0, "", null), Triple(1, "vips: invalid input", "PROCESSING_FAILED"),
            Triple(1, "bwrap: namespace unavailable", "PROCESSING_SANDBOX_UNAVAILABLE"),
            Triple(1, "prlimit: refused", "PROCESSING_SANDBOX_UNAVAILABLE"))) {
            val process = ProcessBoundary(exit, diagnostic)
            assertEquals(expected, CodecSandbox { process }.execute(root.resolve("input"), root, args(), deadline()))
        }
        val header = listOf("/usr/local/bin/spoolin-image-header", "/input", "/output/dimensions")
        assertEquals("INVALID_IMAGE_DIMENSIONS", CodecSandbox { ProcessBoundary(3) }.execute(root.resolve("input"), root, header, deadline()))
        val timedOut = ProcessBoundary(1, finishes = false)
        assertEquals("PROCESSING_TIMEOUT", CodecSandbox { timedOut }.execute(root.resolve("input"), root, args(), deadline()))
        assertTrue(timedOut.destroyed)
        val bounded = ProcessBoundary(1, "x".repeat(10000))
        assertEquals("PROCESSING_FAILED", CodecSandbox { bounded }.execute(root.resolve("input"), root, args(), deadline()))
    }

    @Test fun `process start failures never fall back to unrestricted execution`() {
        for (failure in listOf(java.io.IOException("private executable path"), SecurityException("private path"),
            IllegalArgumentException("private argv"), UnsupportedOperationException("private platform"))) {
            val sandbox = CodecSandbox { throw failure }
            assertEquals("PROCESSING_SANDBOX_UNAVAILABLE", sandbox.execute(root.resolve("input"), root, args(), deadline()))
        }
    }
}
