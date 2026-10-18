package com.spoolin.media.features.processing

/** Reject trailing payloads before any decoder receives accepted input. Full decode follows in the sandbox. */
object ImageEnvelope {
    private const val JPEG_MIN_BYTES = 4
    private const val JPEG_END_PREFIX = 0xff
    private const val JPEG_END_SUFFIX = 0xd9
    private const val WEBP_MIN_BYTES = 12
    private const val RIFF_HEADER_BYTES = 8
    private const val RIFF_LENGTH_START = 4
    private const val RIFF_LENGTH_END = 7
    private const val UNSIGNED_BYTE_MASK = 255L
    private const val BITS_PER_BYTE = 8
    private val pngEnd = java.util.HexFormat.of().parseHex("0000000049454e44ae426082")

    fun complete(bytes: ByteArray, format: ImageFormat): Boolean = when (format) {
        ImageFormat.JPEG -> bytes.size >= JPEG_MIN_BYTES && bytes[bytes.size - 2] == JPEG_END_PREFIX.toByte() &&
            bytes.last() == JPEG_END_SUFFIX.toByte()
        ImageFormat.PNG -> {
            bytes.size >= pngEnd.size && bytes.takeLast(pngEnd.size).toByteArray().contentEquals(pngEnd)
        }
        ImageFormat.WEBP -> bytes.size >= WEBP_MIN_BYTES && littleEndianLength(bytes) == bytes.size.toLong() - RIFF_HEADER_BYTES
    }

    private fun littleEndianLength(bytes: ByteArray): Long =
        (RIFF_LENGTH_START..RIFF_LENGTH_END).fold(0L) { size, index -> size or ((bytes[index].toLong() and UNSIGNED_BYTE_MASK) shl ((index - RIFF_LENGTH_START) * BITS_PER_BYTE)) }
}
