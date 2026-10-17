package com.spoolin.media.features.processing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * ATDD-159 / ATDD-160 / §7.6 / OD-007 / OD-019
 *
 * Validates the processing bounds constants and format detection.
 * Each limit is applied independently; passing one does not waive the others.
 */
class ProcessingBoundsRulesTest {
    @Test
    fun `MIN_INPUT_BYTES is 1 — rejects zero-byte streams`() {
        assertEquals(1L, ProcessingBounds.MIN_INPUT_BYTES)
    }

    @Test
    fun `MAX_INPUT_BYTES is exactly 5 MiB — 5242880 bytes`() {
        assertEquals(5_242_880L, ProcessingBounds.MAX_INPUT_BYTES)
    }

    @Test
    fun `MAX_DIMENSION_PX is 8192`() {
        assertEquals(8192, ProcessingBounds.MAX_DIMENSION_PX)
    }

    @Test
    fun `MAX_PIXELS is 40 million`() {
        assertEquals(40_000_000L, ProcessingBounds.MAX_PIXELS)
    }

    @Test
    fun `MAX_DECODED_BYTES is 160 MB`() {
        assertEquals(160_000_000L, ProcessingBounds.MAX_DECODED_BYTES)
    }

    @Test
    fun `MAX_OUTPUT_BYTES is 8 MiB`() {
        assertEquals(8_388_608L, ProcessingBounds.MAX_OUTPUT_BYTES)
    }

    @Test
    fun `CHUNK_BYTES is 64 KiB`() {
        assertEquals(65_536, ProcessingBounds.CHUNK_BYTES)
    }

    @Test
    fun `MAX_WALL_SECONDS is 10 and MAX_CPU_SECONDS is 5`() {
        assertEquals(10L, ProcessingBounds.MAX_WALL_SECONDS)
        assertEquals(5L, ProcessingBounds.MAX_CPU_SECONDS)
    }

    @Test
    fun `SUPPORTED_FORMATS contains exactly JPEG PNG WEBP`() {
        assertEquals(setOf("JPEG", "PNG", "WEBP"), ProcessingBounds.SUPPORTED_FORMATS)
    }

    @Test
    fun `dimension limit 8192 and pixel limit 40M are independent — 8192x8192 exceeds pixels`() {
        val w = ProcessingBounds.MAX_DIMENSION_PX.toLong()
        val h = ProcessingBounds.MAX_DIMENSION_PX.toLong()
        // 8192 × 8192 = 67,108,864 > 40,000,000
        assert(w * h > ProcessingBounds.MAX_PIXELS) {
            "8192×8192 should exceed MAX_PIXELS to show limits are independent"
        }
    }
}
