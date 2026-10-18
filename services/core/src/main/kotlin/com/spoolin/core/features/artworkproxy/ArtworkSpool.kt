package com.spoolin.core.features.artworkproxy

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64

internal class ArtworkSpool private constructor(val path: Path, val size: Long, val sha256: ByteArray) : AutoCloseable {
    override fun close() { ArtworkScratch.delete(path) }

    companion object {
        fun decode(encoded: String, maximum: Long): ArtworkSpool {
            require(encoded.length.toLong() <= ((maximum + 2) / 3) * 4) { "FILE_INVALID" }
            require(encoded.length % 4 == 0) { "FILE_INVALID" }
            val padding = encoded.takeLast(2).count { it == '=' }
            require((0 until encoded.length - padding).all { index ->
                val value = encoded[index]
                value in 'A'..'Z' || value in 'a'..'z' || value in '0'..'9' || value == '+' || value == '/'
            }) { "FILE_INVALID" }
            require(encoded.takeLast(padding).all { it == '=' }) { "FILE_INVALID" }
            val path = ArtworkScratch.create()
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                var size = 0L
                val ascii = object : InputStream() {
                    var offset = 0
                    override fun read(): Int {
                        if (offset == encoded.length) return -1
                        val value = encoded[offset++].code
                        require(value < 128) { "FILE_INVALID" }
                        return value
                    }
                }
                Base64.getDecoder().wrap(ascii).use { input ->
                    Files.newOutputStream(path).use { output ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            size += count
                            require(size <= maximum) { "FILE_INVALID" }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                    }
                }
                require(size > 0) { "FILE_INVALID" }
                return ArtworkSpool(path, size, digest.digest())
            } catch (failure: Exception) {
                ArtworkScratch.delete(path)
                throw failure
            }
        }
    }
}
