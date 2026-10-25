package com.spoolin.media.features.upload

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.UUID

class QuarantineLifecycleRulesTest {
    @TempDir lateinit var root: Path

    @Test fun `private staged writes preserve offsets flush durable bytes and refuse overwrites`() {
        val store = QuarantineStore(root)
        val file = store.stagingPath(UUID.randomUUID())
        store.openForWrite(file).use { out ->
            out.write(1)
            out.write(byteArrayOf(0, 2, 3, 0), 1, 2)
            out.flush()
        }
        assertArrayEquals(byteArrayOf(1, 2, 3), Files.readAllBytes(file))
        assertThrows(java.nio.file.FileAlreadyExistsException::class.java) { store.openForWrite(file) }
        assertTrue(store.deleteQuarantine(file))
        assertFalse(store.deleteQuarantine(file))
    }

    @Test fun `sweep deletes only old private UUID attempts and preserves current outputs and unrelated directories`() {
        val store = QuarantineStore(root)
        val output = store.outputDirectory()
        val now = Instant.parse("2026-01-02T03:04:05Z")
        val old = Files.createDirectory(output.resolve(UUID.randomUUID().toString()))
        Files.writeString(old.resolve("partial"), "partial")
        Files.setLastModifiedTime(old, FileTime.from(now.minusSeconds(3601)))
        val current = Files.createDirectory(output.resolve(UUID.randomUUID().toString()))
        Files.setLastModifiedTime(current, FileTime.from(now.minusSeconds(100)))
        val unrelated = Files.createDirectory(output.resolve("other-directory"))
        Files.setLastModifiedTime(unrelated, FileTime.from(now.minusSeconds(9000)))
        val published = Files.writeString(output.resolve("${UUID.randomUUID()}.png"), "complete")
        store.sweepAbandonedScratch(now)
        assertFalse(Files.exists(old))
        assertTrue(Files.exists(current))
        assertTrue(Files.exists(unrelated))
        assertTrue(Files.exists(published))
    }
}
