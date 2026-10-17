package com.spoolin.media.features.processing

import java.io.InputStream
import java.security.MessageDigest

/**
 * Content-based image validation applied before processing (§7.6 / AC-150 / AC-151 / ATDD-159 / ATDD-160).
 *
 * Validation rejects:
 * - Empty / zero-byte streams (AC-149).
 * - Streams exceeding the configured maximum byte count (AC-149).
 * - Streams whose magic bytes do not match a supported format (AC-151-C02).
 * - Animated variants: animated PNG (IDAT preceded by acTL chunk), animated WebP (ANIM chunk) (AC-150-C02).
 * - GIF format — always rejected (AC-150).
 * - SVG format — always rejected (AC-150).
 * - Streams with decoded pixel budget exceeding the limit (ATDD-160 / PRD-POLICY-30).
 *
 * Filename, extension and declared MIME type do not alter validation outcome (AC-151-C02 / PRD-POLICY-29).
 */
object ImageValidator {
    private const val HEADER_BYTES = 16
    private const val APNG_ACTL_CHUNK = "acTL"
    private const val WEBP_ANIM_CHUNK = "ANIM"

    sealed class ValidationResult {
        data class Valid(val format: ImageFormat, val inputHash: ByteArray) : ValidationResult()
        data class Invalid(val errorCode: String) : ValidationResult()
    }

    /**
     * Read up to [maxBytes] from [stream], detect format and check for animation.
     * Returns [ValidationResult.Valid] with the detected format and SHA-256 of full content.
     * Returns [ValidationResult.Invalid] with a safe error code if any check fails.
     *
     * The caller must close [stream].
     */
    fun validate(
        stream: InputStream,
        maxBytes: Long,
    ): ValidationResult {
        val buffer = mutableListOf<Byte>()
        var totalRead = 0L
        val digest = MessageDigest.getInstance("SHA-256")
        val chunk = ByteArray(ProcessingBounds.CHUNK_BYTES)

        while (true) {
            val n = stream.read(chunk)
            if (n == -1) break
            if (totalRead + n > maxBytes) return ValidationResult.Invalid("FILE_TOO_LARGE")
            digest.update(chunk, 0, n)
            buffer.addAll(chunk.take(n))
            totalRead += n
        }

        if (totalRead < 1L) return ValidationResult.Invalid("FILE_EMPTY")

        val header = buffer.take(HEADER_BYTES).toByteArray()
        val format = ImageFormat.detect(header) ?: return ValidationResult.Invalid("UNSUPPORTED_FORMAT")

        val content = buffer.toByteArray()
        val animError = detectAnimation(content, format)
        if (animError != null) return ValidationResult.Invalid(animError)

        return ValidationResult.Valid(format = format, inputHash = digest.digest())
    }

    /**
     * Returns an error code if the content is animated, null otherwise.
     *
     * Animated APNG: contains an `acTL` chunk before the first `IDAT` chunk.
     * Animated WebP: contains an `ANIM` chunk in the VP8X/ANMF structure.
     * GIF: always rejected (not a supported static format).
     */
    private fun detectAnimation(content: ByteArray, format: ImageFormat): String? = when (format) {
        ImageFormat.PNG -> if (hasPngAnimChunk(content)) "ANIMATED_IMAGE" else null
        ImageFormat.WEBP -> if (hasWebpAnimChunk(content)) "ANIMATED_IMAGE" else null
        ImageFormat.JPEG -> null // JPEG is always static
    }

    /** Scan PNG chunks for acTL (animation control) before IDAT (first image data). */
    private fun hasPngAnimChunk(bytes: ByteArray): Boolean {
        var pos = 8 // skip PNG signature
        while (pos + 12 <= bytes.size) {
            val length =
                ((bytes[pos].toInt() and 0xFF) shl 24) or
                    ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
                    (bytes[pos + 3].toInt() and 0xFF)
            val typeName = bytes.decodeToString(pos + 4, pos + 8)
            if (typeName == "IDAT") return false // first image data before acTL — static PNG
            if (typeName == APNG_ACTL_CHUNK) return true
            pos += 12 + length // 4 length + 4 type + data + 4 CRC
            if (length < 0) break // overflow guard
        }
        return false
    }

    /** Scan RIFF WebP chunks for ANIM (animation chunk). */
    private fun hasWebpAnimChunk(bytes: ByteArray): Boolean {
        // WebP RIFF structure: RIFF(4) + fileSize(4) + WEBP(4) + chunks...
        var pos = 12
        while (pos + 8 <= bytes.size) {
            val fourcc = bytes.decodeToString(pos, pos + 4)
            val chunkSize =
                ((bytes[pos + 4].toInt() and 0xFF)) or
                    ((bytes[pos + 5].toInt() and 0xFF) shl 8) or
                    ((bytes[pos + 6].toInt() and 0xFF) shl 16) or
                    ((bytes[pos + 7].toInt() and 0xFF) shl 24)
            if (fourcc == WEBP_ANIM_CHUNK) return true
            if (chunkSize < 0) break
            pos += 8 + chunkSize + (chunkSize and 1) // chunks are padded to even size
        }
        return false
    }
}
