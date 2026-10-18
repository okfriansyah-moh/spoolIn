package com.spoolin.core.features.bootstrap

import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class InitializationReadKeysRulesTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `additional read mounts preserve immutable setup key identities`() {
        val old = key("old", 1)
        val fresh = key("fresh", 2)
        val original = mapOf("old" to KeyMount(KeyPurpose.PII, old))
        val merged = InitializationReadKeys.merge(original, config("fresh", "PII", fresh))
        assertEquals(setOf("old"), original.keys)
        assertEquals(setOf("old", "fresh"), merged.keys)
        MountedKeys(merged, null).use { keys -> assertEquals(2, keys.key("fresh", KeyPurpose.PII)?.first()?.toInt()) }
    }

    @Test
    fun `a repeated identity cannot change key purpose or material`() {
        val original = mapOf("old" to KeyMount(KeyPurpose.PII, key("old", 1)))
        assertThrows(IllegalArgumentException::class.java) {
            InitializationReadKeys.merge(original, config("old", "DRAFT", key("wrong-purpose", 2)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            InitializationReadKeys.merge(original, config("old", "PII", key("wrong-material", 3)))
        }
    }

    private fun key(
        name: String,
        value: Byte,
    ): Path =
        directory.resolve(name).also {
            Files.write(it, ByteArray(32) { value })
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }

    private fun config(
        id: String,
        purpose: String,
        key: Path,
    ): Path =
        directory.resolve("read-keys.json").also {
            Files.write(
                it,
                JsonMapper.builder().build().writeValueAsBytes(mapOf(id to mapOf("purpose" to purpose, "file" to key.toString()))),
            )
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }
}
