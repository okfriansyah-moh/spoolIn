@file:Suppress("MagicNumber", "ReturnCount")

package com.spoolin.media.features.processing

/**
 * Statically-detected image formats based on magic bytes (§7.6 / AC-151).
 *
 * Format detection is based on actual byte content — not filename, extension or declared
 * Content-Type. A claimed MIME type that disagrees with the actual magic is a disguised file
 * and is rejected (AC-151-C02 / PRD-POLICY-29).
 */
enum class ImageFormat(
    val label: String,
) {
    JPEG("JPEG"),
    PNG("PNG"),
    WEBP("WEBP"),
    ;

    companion object {
        // JPEG: FF D8 FF
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        private val PNG_MAGIC =
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

        // WebP: RIFF....WEBP (bytes 0-3 = "RIFF", bytes 8-11 = "WEBP")
        private val WEBP_RIFF = "RIFF".toByteArray(Charsets.US_ASCII)
        private val WEBP_WEBP = "WEBP".toByteArray(Charsets.US_ASCII)

        /**
         * Detect format from the first bytes of the file.
         * Returns null if the header does not match any supported format.
         * At least 12 bytes are required for WebP detection.
         */
        fun detect(header: ByteArray): ImageFormat? =
            when {
                header.size >= 3 && header.startsWith(JPEG_MAGIC) -> JPEG
                header.size >= 8 && header.startsWith(PNG_MAGIC) -> PNG
                header.size >= 12 &&
                    header.regionMatches(0, WEBP_RIFF, 0, 4) &&
                    header.regionMatches(8, WEBP_WEBP, 0, 4) -> WEBP
                else -> null
            }

        private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
            val sufficientBytes = size >= prefix.size
            return sufficientBytes && regionMatches(0, prefix, 0, prefix.size)
        }

        private fun ByteArray.regionMatches(
            thisOffset: Int,
            other: ByteArray,
            otherOffset: Int,
            length: Int,
        ): Boolean {
            if (thisOffset + length > size || otherOffset + length > other.size) return false
            for (i in 0 until length) {
                if (this[thisOffset + i] != other[otherOffset + i]) return false
            }
            return true
        }
    }
}
