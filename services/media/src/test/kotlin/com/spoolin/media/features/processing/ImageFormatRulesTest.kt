package com.spoolin.media.features.processing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * ATDD-151 / ATDD-159 / AC-150 / AC-151 / §7.6
 *
 * Validates magic-byte format detection independently of filename or declared MIME type.
 * A forged extension cannot bypass format detection (AC-151-C02 / PRD-POLICY-29).
 */
class ImageFormatRulesTest {
    // Minimum header bytes for detection
    private val jpegMagic = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + ByteArray(9)
    private val pngMagic =
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(8)
    private val webpMagic =
        "RIFF".toByteArray() + ByteArray(4) + "WEBP".toByteArray() + ByteArray(4)

    @Test
    fun `JPEG magic FF D8 FF is detected correctly`() {
        assertEquals(ImageFormat.JPEG, ImageFormat.detect(jpegMagic))
    }

    @Test
    fun `PNG magic 89504E47 is detected correctly`() {
        assertEquals(ImageFormat.PNG, ImageFormat.detect(pngMagic))
    }

    @Test
    fun `WebP magic RIFF_WEBP is detected correctly`() {
        assertEquals(ImageFormat.WEBP, ImageFormat.detect(webpMagic))
    }

    @Test
    fun `GIF magic is not a supported format`() {
        val gifMagic = "GIF89a".toByteArray() + ByteArray(10)
        assertNull(ImageFormat.detect(gifMagic), "GIF must not be accepted")
    }

    @Test
    fun `SVG-like text magic is not supported`() {
        val svgHeader = "<svg xmlns".toByteArray() + ByteArray(6)
        assertNull(ImageFormat.detect(svgHeader), "SVG must not be accepted")
    }

    @Test
    fun `random one-byte file is not supported`() {
        assertNull(ImageFormat.detect(byteArrayOf(0x00)), "Single-byte file must not be accepted")
    }

    @Test
    fun `empty header is not supported`() {
        assertNull(ImageFormat.detect(byteArrayOf()), "Empty header must not be accepted")
    }

    @Test
    fun `JPEG magic with altered first byte is rejected`() {
        val malformed = jpegMagic.copyOf()
        malformed[0] = 0xFE.toByte()
        assertNull(ImageFormat.detect(malformed))
    }

    @Test
    fun `format label matches declared name`() {
        assertEquals("JPEG", ImageFormat.JPEG.label)
        assertEquals("PNG", ImageFormat.PNG.label)
        assertEquals("WEBP", ImageFormat.WEBP.label)
    }
}
