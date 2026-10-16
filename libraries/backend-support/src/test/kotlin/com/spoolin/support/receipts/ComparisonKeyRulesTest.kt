package com.spoolin.support.receipts

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID

class ComparisonKeyRulesTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `retained version remains selected after active rotation without exposing material`() {
        val old = key("old", 1)
        val active = key("active", 2)
        val intent = OperationIntent(UUID.randomUUID(), "account:A", "DRAFT_SAVE", "p1", listOf("draft" to "Private"))
        val before = ComparisonKeys.load(mapOf("Old_A" to old), "Old_A", old)
        val digest = before.hash(intent, "Old_A")
        val after = ComparisonKeys.load(mapOf("Old_A" to old, "new" to active), "new", active)
        assertArrayEquals(digest, after.hash(intent, "Old_A"))
        assertFalse(digest.contentEquals(after.hash(intent, "new")))
        assertFalse(intent.toString().contains("Private"))
    }

    @Test
    fun `file capacity permission links active binding and purpose independence fail closed`() {
        val good = key("good", 3)
        val short = key("short", 4, 31)
        val long = key("long", 5, 33)
        val linked = directory.resolve("linked")
        Files.createSymbolicLink(linked, Path.of(good))
        val public = key("public", 6)
        Files.setPosixFilePermissions(Path.of(public), PosixFilePermissions.fromString("rw-r--r--"))
        listOf(
            short,
            long,
            linked.toString(),
            public,
            directory.resolve("missing").toString(),
            directory.toString(),
        ).forEach {
            val error =
                assertThrows(
                    IllegalArgumentException::class.java,
                ) { ComparisonKeys.load(mapOf("active" to it), "active", it) }
            assertFalse(error.message.orEmpty().contains(directory.toString()))
        }
        assertThrows(
            IllegalArgumentException::class.java,
        ) { ComparisonKeys.load(mapOf("active" to good), "missing", good) }
        assertThrows(
            IllegalArgumentException::class.java,
        ) { ComparisonKeys.load(mapOf("active" to good), "active", short) }
        assertThrows(IllegalArgumentException::class.java) {
            ComparisonKeys.load(mapOf("active" to good), "active", good, listOf(good))
        }
    }

    private fun key(
        name: String,
        byte: Int,
        size: Int = 32,
    ): String {
        val path = directory.resolve(name)
        Files.write(path, ByteArray(size) { byte.toByte() })
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        return path.toString()
    }
}
