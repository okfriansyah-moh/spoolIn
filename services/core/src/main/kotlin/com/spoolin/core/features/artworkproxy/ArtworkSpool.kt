package com.spoolin.core.features.artworkproxy

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64

private const val BASE64_INPUT_GROUP = 3
private const val BASE64_OUTPUT_GROUP = 4
private const val BASE64_PADDING_MAX = 2
private const val ASCII_LIMIT = 128
private const val DECODE_BUFFER_BYTES = 65_536

internal class ArtworkSpool private constructor(val path: Path, val size: Long, val sha256: ByteArray) : AutoCloseable {
    override fun close() { ArtworkScratch.delete(path) }

    companion object {
        fun decode(encoded: String, maximum: Long): ArtworkSpool {
            validateEncoding(encoded, maximum)
            val path = ArtworkScratch.create()
            var decoded = false
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val size = decodeToFile(encoded, path, maximum, digest)
                require(size > 0) { "FILE_INVALID" }
                val result = ArtworkSpool(path, size, digest.digest())
                decoded = true
                return result
            } finally {
                if (!decoded) ArtworkScratch.delete(path)
            }
        }

        private fun validateEncoding(encoded: String, maximum: Long) {
            val groups = (maximum + BASE64_PADDING_MAX) / BASE64_INPUT_GROUP
            require(encoded.length.toLong() <= groups * BASE64_OUTPUT_GROUP) { "FILE_INVALID" }
            require(encoded.length % BASE64_OUTPUT_GROUP == 0) { "FILE_INVALID" }
            val padding = encoded.takeLast(BASE64_PADDING_MAX).count { it == '=' }
            require((0 until encoded.length - padding).all { isBase64(encoded[it]) }) { "FILE_INVALID" }
            require(encoded.takeLast(padding).all { it == '=' }) { "FILE_INVALID" }
        }

        private fun isBase64(value: Char): Boolean =
            value in 'A'..'Z' || value in 'a'..'z' || value in '0'..'9' || value == '+' || value == '/'

        private fun decodeToFile(encoded: String, path: Path, maximum: Long, digest: MessageDigest): Long =
            Base64.getDecoder().wrap(EncodedInput(encoded)).use { input ->
                Files.newOutputStream(path).use { output -> copy(input, output, maximum, digest) }
            }

        private fun copy(input: InputStream, output: java.io.OutputStream, maximum: Long, digest: MessageDigest): Long {
            var size = 0L
            val buffer = ByteArray(DECODE_BUFFER_BYTES)
            var count = input.read(buffer)
            while (count >= 0) {
                size += count
                require(size <= maximum) { "FILE_INVALID" }
                digest.update(buffer, 0, count)
                output.write(buffer, 0, count)
                count = input.read(buffer)
            }
            return size
        }
    }
}

private class EncodedInput(private val encoded: String) : InputStream() {
    private var offset = 0
    override fun read(): Int {
        if (offset == encoded.length) return -1
        val value = encoded[offset++].code
        require(value < ASCII_LIMIT) { "FILE_INVALID" }
        return value
    }
}
