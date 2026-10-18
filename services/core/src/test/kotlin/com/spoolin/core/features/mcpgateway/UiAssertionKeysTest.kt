package com.spoolin.core.features.mcpgateway

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.env.MockEnvironment
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class UiAssertionKeysTest {
    @TempDir lateinit var directory: Path
    private val active = ByteArray(32) { 2 }
    private val previous = ByteArray(32) { 1 }

    private fun clock(now: Long) = Clock.fixed(Instant.ofEpochSecond(now), ZoneOffset.UTC)

    private fun key(
        name: String,
        value: ByteArray,
    ): Path =
        directory.resolve(name).also {
            Files.write(it, value)
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }

    private fun environment(): MockEnvironment =
        MockEnvironment()
            .withProperty("MCP_UI_ASSERTION_KEY_ID", "v2")
            .withProperty("MCP_UI_ASSERTION_KEY_FILE", key("active", active).toString())
            .withProperty("MCP_UI_ASSERTION_PREVIOUS_KEY_ID", "v1")
            .withProperty("MCP_UI_ASSERTION_PREVIOUS_KEY_FILE", key("previous", previous).toString())
            .withProperty("MCP_UI_ASSERTION_ROTATED_AT", "1000")

    @Test
    fun `previous read window is absolute survives restart and ends at equality`() {
        val environment = environment()
        UiAssertionKeys.load(environment, clock(1059)).use {
            assertArrayEquals(previous, it.keyFor("v1", 1059))
            assertArrayEquals(active, it.keyFor("v2", 1060))
            assertNull(it.keyFor("v1", 1060))
            assertNull(it.keyFor("v1", 1059))
        }
        UiAssertionKeys.load(environment, clock(1060)).use {
            assertNull(it.keyFor("v1", 1060))
            assertArrayEquals(active, it.keyFor("v2", 1060))
        }
    }

    @Test
    fun `unknown keys future or partial rotation duplicate IDs and reused material fail closed`() {
        UiAssertionKeys.single("v1", previous).use { assertNull(it.keyFor("unknown", 1000)) }
        assertThrows(IllegalArgumentException::class.java) { UiAssertionKeys.load(environment(), clock(999)) }
        assertThrows(IllegalArgumentException::class.java) {
            UiAssertionKeys.load(environment().withProperty("MCP_UI_ASSERTION_ROTATED_AT", ""), clock(1000))
        }
        assertThrows(IllegalArgumentException::class.java) {
            UiAssertionKeys.load(environment().withProperty("MCP_UI_ASSERTION_PREVIOUS_KEY_ID", "v2"), clock(1000))
        }
        assertThrows(IllegalArgumentException::class.java) {
            UiAssertionKeys.load(
                environment().withProperty("MCP_UI_ASSERTION_PREVIOUS_KEY_FILE", key("same", active).toString()),
                clock(1000),
            )
        }
    }

    @Test
    fun `protected files reject weak permissions symlinks wrong size and cross purpose material`() {
        val environment = environment()
        val file = Path.of(environment.getRequiredProperty("MCP_UI_ASSERTION_KEY_FILE"))
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { UiAssertionKeys.load(environment, clock(1000)) }
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        val link = directory.resolve("link")
        Files.createSymbolicLink(link, file)
        assertThrows(IllegalArgumentException::class.java) {
            UiAssertionKeys.load(environment.withProperty("MCP_UI_ASSERTION_KEY_FILE", link.toString()), clock(1000))
        }
        assertThrows(IllegalArgumentException::class.java) {
            UiAssertionKeys.load(
                environment().withProperty("MCP_UI_ASSERTION_KEY_FILE", key("short", ByteArray(31)).toString()),
                clock(1000),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            UiAssertionKeys.load(
                environment().withProperty("RATE_LIMIT_KEY_FILE", key("reused", active).toString()),
                clock(1000),
            )
        }
    }

    @Test
    fun `closed keys cannot verify and publication disabled does not require private key mounts`() {
        val keys = UiAssertionKeys.single("v1", previous)
        keys.close()
        assertNull(keys.keyFor("v1", 1000))
        McpGatewayConfiguration().uiAssertionVerifier(MockEnvironment(), ObjectMapper()).close()
        assertThrows(IllegalStateException::class.java) {
            McpGatewayConfiguration().uiAssertionVerifier(
                MockEnvironment().withProperty("MCP_PUBLICATION_ENABLED", "true"),
                ObjectMapper(),
            )
        }
    }
}
