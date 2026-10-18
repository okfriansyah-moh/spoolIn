package com.spoolin.media.features.processing

/** Reject trailing payloads before any decoder receives accepted input. Full decode follows in the sandbox. */
object ImageEnvelope {
    fun complete(bytes: ByteArray, format: ImageFormat): Boolean = when (format) {
        ImageFormat.JPEG -> bytes.size >= 4 && bytes[bytes.size - 2] == 0xff.toByte() &&
            bytes.last() == 0xd9.toByte()
        ImageFormat.PNG -> {
            val end = byteArrayOf(0, 0, 0, 0, 0x49, 0x45, 0x4e, 0x44, 0xae.toByte(), 0x42, 0x60, 0x82.toByte())
            bytes.size >= end.size && bytes.takeLast(end.size).toByteArray().contentEquals(end)
        }
        ImageFormat.WEBP -> bytes.size >= 12 && littleEndianLength(bytes) == bytes.size.toLong() - 8
    }

    private fun littleEndianLength(bytes: ByteArray): Long =
        (4..7).fold(0L) { size, index -> size or ((bytes[index].toLong() and 255) shl ((index - 4) * 8)) }
}
