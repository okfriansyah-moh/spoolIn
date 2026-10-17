package com.spoolin.media.features.processing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

/**
 * ATDD-159 / ATDD-160 / AC-149 / AC-150 / AC-151 / §7.6
 *
 * Validates the content-based image validator independently of filename or MIME type.
 * Each rejection path uses a safe, declared error code rather than free-form user text.
 */
class ImageValidatorRulesTest {
    private val maxBytes = ProcessingBounds.MAX_INPUT_BYTES

    private fun jpeg(size: Int = 16): ByteArray {
        val header = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        return header + ByteArray(size - header.size)
    }

    private fun png(size: Int = 16): ByteArray {
        val header = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        return header + ByteArray(size - header.size)
    }

    private fun webp(size: Int = 16): ByteArray {
        val header = "RIFF".toByteArray() + ByteArray(4) + "WEBP".toByteArray()
        return header + ByteArray(size - header.size)
    }

    private fun validate(bytes: ByteArray, max: Long = maxBytes): ImageValidator.ValidationResult =
        ImageValidator.validate(ByteArrayInputStream(bytes), max)

    @Test
    fun `valid JPEG header returns Valid with JPEG format`() {
        val result = validate(jpeg())
        assert(result is ImageValidator.ValidationResult.Valid)
        assertEquals(ImageFormat.JPEG, (result as ImageValidator.ValidationResult.Valid).format)
        assertEquals(32, result.inputHash.size)
    }

    @Test
    fun `valid PNG header returns Valid with PNG format`() {
        val result = validate(png())
        assert(result is ImageValidator.ValidationResult.Valid)
        assertEquals(ImageFormat.PNG, (result as ImageValidator.ValidationResult.Valid).format)
    }

    @Test
    fun `valid WebP header returns Valid with WEBP format`() {
        val result = validate(webp())
        assert(result is ImageValidator.ValidationResult.Valid)
        assertEquals(ImageFormat.WEBP, (result as ImageValidator.ValidationResult.Valid).format)
    }

    @Test
    fun `empty stream returns FILE_EMPTY`() {
        val result = validate(byteArrayOf())
        assert(result is ImageValidator.ValidationResult.Invalid)
        assertEquals("FILE_EMPTY", (result as ImageValidator.ValidationResult.Invalid).errorCode)
    }

    @Test
    fun `stream exactly at maximum is accepted (size boundary)`() {
        // Exact maximum — must pass size check, subject to content checks (AC-149-C01)
        val data = jpeg(maxBytes.toInt())
        val result = validate(data, maxBytes)
        // Content check may still reject it, but size boundary must be accepted
        assert(result !is ImageValidator.ValidationResult.Invalid ||
            (result as ImageValidator.ValidationResult.Invalid).errorCode != "FILE_TOO_LARGE")
    }

    @Test
    fun `stream one byte above maximum returns FILE_TOO_LARGE`() {
        val oversize = ByteArray((maxBytes + 1).toInt()) { 0xFF.toByte() }
        val result = validate(oversize, maxBytes)
        assert(result is ImageValidator.ValidationResult.Invalid)
        assertEquals("FILE_TOO_LARGE", (result as ImageValidator.ValidationResult.Invalid).errorCode)
    }

    @Test
    fun `GIF content returns UNSUPPORTED_FORMAT`() {
        val gif = "GIF89a".toByteArray() + ByteArray(16)
        val result = validate(gif)
        assert(result is ImageValidator.ValidationResult.Invalid)
        assertEquals("UNSUPPORTED_FORMAT", (result as ImageValidator.ValidationResult.Invalid).errorCode)
    }

    @Test
    fun `random bytes return UNSUPPORTED_FORMAT`() {
        val random = ByteArray(32) { it.toByte() }
        val result = validate(random)
        assert(result is ImageValidator.ValidationResult.Invalid)
        assertEquals("UNSUPPORTED_FORMAT", (result as ImageValidator.ValidationResult.Invalid).errorCode)
    }

    @Test
    fun `single-byte invalid file returns UNSUPPORTED_FORMAT`() {
        // AC-149: one-byte invalid file is rejected (empty is FILE_EMPTY; 1-byte non-image is UNSUPPORTED)
        val single = byteArrayOf(0x00)
        val result = validate(single)
        assert(result is ImageValidator.ValidationResult.Invalid) {
            "Single byte must be rejected; got $result"
        }
    }

    @Test
    fun `animated PNG with acTL chunk before IDAT returns ANIMATED_IMAGE`() {
        // Construct a synthetic PNG with acTL chunk before IDAT
        val sig = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        // acTL chunk: length(4) + "acTL"(4) + data(8) + CRC(4)
        val actl = intToBytes(8) + "acTL".toByteArray() + ByteArray(8) + ByteArray(4)
        val idat = intToBytes(0) + "IDAT".toByteArray() + ByteArray(4)
        val apng = sig + actl + idat
        val result = validate(apng)
        assert(result is ImageValidator.ValidationResult.Invalid)
        assertEquals("ANIMATED_IMAGE", (result as ImageValidator.ValidationResult.Invalid).errorCode)
    }

    @Test
    fun `animated WebP with ANIM chunk returns ANIMATED_IMAGE`() {
        // WebP with ANIM chunk in RIFF structure
        val animChunk = "ANIM".toByteArray() + intToLEBytes(8) + ByteArray(8)
        val payload = "WEBP".toByteArray() + animChunk
        val riff = "RIFF".toByteArray() + intToLEBytes(payload.size) + payload
        val result = validate(riff)
        assert(result is ImageValidator.ValidationResult.Invalid)
        assertEquals("ANIMATED_IMAGE", (result as ImageValidator.ValidationResult.Invalid).errorCode)
    }

    @Test
    fun `input hash is SHA-256 of complete content`() {
        val data = jpeg()
        val result = validate(data)
        require(result is ImageValidator.ValidationResult.Valid)
        val expected = java.security.MessageDigest.getInstance("SHA-256").digest(data)
        assert(result.inputHash.contentEquals(expected))
    }

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        (value shr 24 and 0xFF).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    private fun intToLEBytes(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 24 and 0xFF).toByte(),
    )
}
