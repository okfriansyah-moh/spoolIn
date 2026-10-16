package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class KeyFilesIntegrationTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun mountedFilesRequireExactSizePermissionsAndIndependentMaterial() {
        val first = key("first", 32, 1)
        val second = key("second", 32, 2)
        MountedKeys(
            mapOf("pii" to KeyMount(KeyPurpose.PII, first), "email" to KeyMount(KeyPurpose.EMAIL_LOOKUP, second)),
        ).use { keys ->
            val bytes = requireNotNull(keys.key("pii", KeyPurpose.PII))
            bytes.fill(0)
            assertArrayEquals(ByteArray(32) { 1 }, keys.key("pii", KeyPurpose.PII))
            assertFalse(
                requireNotNull(
                    keys.key("pii", KeyPurpose.PII),
                ).contentEquals(requireNotNull(keys.key("email", KeyPurpose.EMAIL_LOOKUP))),
            )
            assertNull(keys.key("pii", KeyPurpose.DRAFT))
            assertNull(keys.key("missing", KeyPurpose.PII))
            keys.close()
            assertNull(keys.key("pii", KeyPurpose.PII))
        }
        assertThrows(IllegalArgumentException::class.java) {
            MountedKeys(
                mapOf(
                    "pii" to KeyMount(KeyPurpose.PII, first),
                    "email" to KeyMount(KeyPurpose.EMAIL_LOOKUP, first),
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            MountedKeys(
                mapOf("bad id" to KeyMount(KeyPurpose.PII, first)),
            )
        }
        listOf(31, 33).forEach { size ->
            assertThrows(IllegalArgumentException::class.java) {
                MountedKeys(
                    mapOf("pii" to KeyMount(KeyPurpose.PII, key("size$size", size, 1))),
                )
            }
        }
        Files.setPosixFilePermissions(first, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(
            IllegalArgumentException::class.java,
        ) { MountedKeys(mapOf("pii" to KeyMount(KeyPurpose.PII, first))) }
        val link = directory.resolve("link")
        Files.createSymbolicLink(link, second)
        assertThrows(
            IllegalArgumentException::class.java,
        ) { MountedKeys(mapOf("pii" to KeyMount(KeyPurpose.PII, link))) }
        assertThrows(IllegalArgumentException::class.java) {
            MountedKeys(
                mapOf("pii" to KeyMount(KeyPurpose.PII, directory.resolve("absent"))),
            )
        }
    }

    private fun key(
        name: String,
        size: Int,
        value: Byte,
    ): Path =
        directory.resolve(name).also {
            Files.write(it, ByteArray(size) { value })
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }
}
