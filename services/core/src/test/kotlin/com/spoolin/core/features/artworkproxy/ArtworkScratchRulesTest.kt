package com.spoolin.core.features.artworkproxy

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

class ArtworkScratchRulesTest {
    @Test
    fun `scratch capacity is bounded and cleanup releases each allocation once`() {
        val allocated = (1..4).map { ArtworkScratch.create() }.toMutableList()
        try {
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(allocated.first()))
            val directoryPermissions = Files.getPosixFilePermissions(allocated.first().parent)
            assertEquals(PosixFilePermissions.fromString("rwx------"), directoryPermissions)
            val denied = assertThrows(StatusRuntimeException::class.java) { ArtworkScratch.create() }
            assertEquals(Status.Code.RESOURCE_EXHAUSTED, denied.status.code)
            val released = allocated.removeAt(0)
            ArtworkScratch.delete(released)
            ArtworkScratch.delete(released)
            assertFalse(Files.exists(released))
            allocated.add(ArtworkScratch.create())
            assertThrows(StatusRuntimeException::class.java) { ArtworkScratch.create() }
        } finally {
            allocated.forEach { ArtworkScratch.delete(it) }
        }
    }
}
