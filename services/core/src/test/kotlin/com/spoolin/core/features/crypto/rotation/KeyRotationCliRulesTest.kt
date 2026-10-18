package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class KeyRotationCliRulesTest {
    @TempDir lateinit var directory: Path
    private val valid = arrayOf("--from", "old", "--to", "new", "--purpose", "PII")
    private val map = "/run/spoolin-config/rotation-keys.json"

    @Test
    fun `native options preserve real dry-run registration and bounded batch contracts`() {
        val options = RotationCliOptions.parse(
            valid + arrayOf("--dry-run", "--register-target", "--max-batches", "1"), map,
        )
        assertEquals(KeyPurpose.PII, options.purpose)
        assertTrue(options.dryRun)
        assertTrue(options.register)
        assertEquals(1, options.maxBatches)
        assertEquals(Path.of(map), options.keys)
        val execute = RotationCliOptions.parse(valid, map)
        assertFalse(execute.dryRun)
        assertFalse(execute.register)
        assertEquals(100, execute.maxBatches)
    }

    @Test
    fun `missing duplicate unexpected and unsafe options fail before key or database access`() {
        val invalid =
            listOf(
                arrayOf("--from"),
                arrayOf("--from", "--to", "new"),
                valid + arrayOf("--from", "another"),
                valid + arrayOf("--dry-run", "--dry-run"),
                valid + arrayOf("--max-batches", "0"),
                valid + arrayOf("--max-batches", "10001"),
                valid + arrayOf("--max-batches", "01"),
                valid + arrayOf("--max-batches", "99999999999999"),
                valid + arrayOf("--keys", "relative.json"),
                valid + arrayOf("--keys", "/run/../tmp/key.json"),
                valid + arrayOf("--unknown", "private"),
                arrayOf("--from", "old;private", "--to", "new"),
                arrayOf("--from", "same", "--to", "same"),
                arrayOf("--from", "old", "--to", "new", "--purpose", "pii"),
            )
        invalid.forEach { args ->
            assertThrows(IllegalArgumentException::class.java) { RotationCliOptions.parse(args, map) }
        }
        assertThrows(IllegalArgumentException::class.java) { RotationCliOptions.parse(valid, null) }
    }

    @Test
    fun `activation input requires identical purpose-bound source and target in application mounts`() {
        val old = material("old", 11)
        val target = material("new", 12)
        val options = RotationCliOptions.parse(valid, map)
        val full = mapOf("old" to KeyMount(KeyPurpose.PII, old), "new" to KeyMount(KeyPurpose.PII, target))
        MountedKeys(full, null).use { rotation ->
            MountedKeys(full, null).use { application -> validateApplicationMaterial(application, rotation, options) }
            MountedKeys(mapOf("old" to KeyMount(KeyPurpose.PII, old)), null).use { application ->
                assertThrows(IllegalStateException::class.java) {
                    validateApplicationMaterial(application, rotation, options)
                }
            }
            val different = full + ("new" to KeyMount(KeyPurpose.PII, material("different", 13)))
            MountedKeys(different, null).use { application ->
                assertThrows(IllegalStateException::class.java) {
                    validateApplicationMaterial(application, rotation, options)
                }
            }
            val wrongPurpose = full + ("new" to KeyMount(KeyPurpose.DRAFT, target))
            MountedKeys(wrongPurpose, null).use { application ->
                assertThrows(IllegalStateException::class.java) {
                    validateApplicationMaterial(application, rotation, options)
                }
            }
        }
    }

    private fun material(
        name: String,
        value: Int,
    ): Path =
        directory.resolve(name).also { file ->
            Files.write(file, ByteArray(32) { value.toByte() })
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        }
}
