package com.spoolin.core.features.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class MountedKeyRefreshBehaviorTest {
    @TempDir lateinit var root: Path
    private val oldBytes = ByteArray(32) { 103 }
    private val newBytes = ByteArray(32) { 107 }

    @Test
    fun `new protected key versions load atomically without changing existing material`() {
        val old = key("old", oldBytes)
        val next = key("new", newBytes)
        val reload = config(old, next)
        MountedKeys(mapOf("old" to KeyMount(KeyPurpose.PII, old)), reload).use { keys ->
            assertArrayEquals(newBytes, keys.key("new", KeyPurpose.PII))
            assertArrayEquals(oldBytes, keys.key("old", KeyPurpose.PII))
            assertNull(keys.key("absent", KeyPurpose.PII))
        }
    }

    @Test
    fun `reload rejects changed existing material and leaves previously mounted version intact`() {
        val old = key("old", oldBytes)
        val changed = key("changed", newBytes)
        val next = key("new", ByteArray(32) { 109 })
        val reload = config(changed, next)
        MountedKeys(mapOf("old" to KeyMount(KeyPurpose.PII, old)), reload).use { keys ->
            assertThrows(ProtectionUnavailable::class.java) { keys.key("new", KeyPurpose.PII) }
            assertArrayEquals(oldBytes, keys.key("old", KeyPurpose.PII))
        }
    }

    @Test
    fun `reload configuration rejects malformed shape duplicate fields relative paths and public permissions`() {
        val old = key("old", oldBytes)
        val next = key("new", newBytes)
        val file = config(old, next)
        for (invalid in listOf(
            "{}",
            "[]",
            "{\"old\":{\"purpose\":\"PII\",\"file\":\"relative\"},\"new\":{\"purpose\":\"PII\",\"file\":\"$next\"}}",
            "{\"old\":{},\"old\":{}}",
        )) {
            Files.writeString(file, invalid)
            assertThrows(Exception::class.java) { MountedKeyRefresh.read(file) }
        }
        config(old, next)
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { MountedKeyRefresh.read(file) }
    }

    private fun key(
        name: String,
        bytes: ByteArray,
    ): Path =
        root.resolve(name).also {
            Files.write(it, bytes)
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }

    private fun config(
        old: Path,
        next: Path,
    ): Path =
        root.resolve("keys.json").also {
            Files.writeString(it, "{\"old\":{\"purpose\":\"PII\",\"file\":\"$old\"},\"new\":{\"purpose\":\"PII\",\"file\":\"$next\"}}")
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }
}
