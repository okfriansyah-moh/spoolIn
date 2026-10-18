package com.spoolin.support.notifications

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.nio.file.attribute.PosixFilePermissions

internal object ProtectedNotificationSecret {
    private const val MAX_BYTES = 4096

    fun read(value: String): String =
        try {
            val path = Path.of(value)
            require(Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.size(path) in 1..MAX_BYTES)
            require(Files.getPosixFilePermissions(path, NOFOLLOW_LINKS) == PosixFilePermissions.fromString("rw-------"))
            decode(readBounded(path))
        } catch (_: Exception) {
            throw IllegalArgumentException("Selected notification configuration is invalid")
        }

    private fun readBounded(path: Path): ByteBuffer =
        Files.newByteChannel(path, setOf<java.nio.file.OpenOption>(READ, NOFOLLOW_LINKS)).use { channel ->
            val buffer = ByteBuffer.allocate(MAX_BYTES + 1)
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) { /* bounded protected file */ }
            require(buffer.position() in 1..MAX_BYTES)
            buffer.flip()
            buffer
        }

    private fun decode(buffer: ByteBuffer): String {
        val value =
            Charsets.UTF_8
                .newDecoder()
                .decode(buffer)
                .toString()
                .trimEnd('\n', '\r')
        require(value.isNotEmpty() && value.none(Char::isISOControl))
        return value
    }
}
