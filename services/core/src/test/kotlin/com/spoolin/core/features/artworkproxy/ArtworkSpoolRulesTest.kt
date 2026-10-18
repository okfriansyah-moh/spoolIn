package com.spoolin.core.features.artworkproxy

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.util.Base64

private const val INPUT_LIMIT = 8L
private const val SCRATCH_LIMIT = 4

class ArtworkSpoolRulesTest {
    @Test
    fun `base64 input is streamed with exact digest size private mode and cleanup`() {
        val input = "artwork".toByteArray()
        val spool = ArtworkSpool.decode(Base64.getEncoder().encodeToString(input), INPUT_LIMIT)
        val path = spool.path
        spool.use {
            assertEquals(input.size.toLong(), it.size)
            assertArrayEquals(input, Files.readAllBytes(path))
            assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(input), it.sha256)
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(path))
        }
        assertFalse(Files.exists(path))
    }

    @Test
    fun `empty malformed and decoded oversize inputs cannot leak scratch capacity`() {
        val invalid = listOf("", "abcd=", "====", "AA A", "界AAA", "AAAAAAAAAAAA")
        invalid.forEach { input ->
            assertThrows(IllegalArgumentException::class.java) { ArtworkSpool.decode(input, INPUT_LIMIT) }
            assertAllScratchAvailable()
        }
    }

    private fun assertAllScratchAvailable() {
        val files = mutableListOf<java.nio.file.Path>()
        try {
            repeat(SCRATCH_LIMIT) { files.add(ArtworkScratch.create()) }
            assertEquals(SCRATCH_LIMIT, files.size)
        } finally { files.forEach(ArtworkScratch::delete) }
    }

}
