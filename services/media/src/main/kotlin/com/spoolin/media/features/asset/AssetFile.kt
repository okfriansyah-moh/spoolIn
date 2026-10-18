package com.spoolin.media.features.asset

import com.spoolin.media.features.processing.ProcessingBounds
import io.grpc.Status
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

internal class AssetFile(val row: AssetRow, val channel: FileChannel) : AutoCloseable {
    override fun close() = channel.close()

    companion object {
        fun open(output: Path, row: AssetRow): AssetFile {
            val channel = FileChannel.open(path(output, row), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)
            var verified = false
            try {
                verify(channel, row)
                channel.position(0)
                verified = true
                return AssetFile(row, channel)
            } finally {
                if (!verified) channel.close()
            }
        }

        private fun path(output: Path, row: AssetRow): Path {
            val extension = extension(row.format)
            if (row.storageKey != "output/${row.id}.$extension") throw Status.UNAVAILABLE.asRuntimeException()
            val path = output.resolve("${row.id}.$extension")
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw Status.NOT_FOUND.asRuntimeException()
            return path
        }

        private fun extension(format: String?): String = when (format) {
            "JPEG" -> "jpeg"
            "PNG" -> "png"
            "WEBP" -> "webp"
            else -> throw Status.UNAVAILABLE.asRuntimeException()
        }

        private fun verify(channel: FileChannel, row: AssetRow) {
            check(channel.size() == row.outputBytes && channel.size() in 1..ProcessingBounds.MAX_OUTPUT_BYTES)
            check(row.width != null && row.height != null && row.contentHash != null)
            check(MessageDigest.isEqual(hash(channel), row.contentHash))
        }

        private fun hash(channel: FileChannel): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteBuffer.allocate(ProcessingBounds.CHUNK_BYTES)
            while (channel.read(buffer) != -1) {
                if (io.grpc.Context.current().isCancelled) throw Status.CANCELLED.asRuntimeException()
                buffer.flip(); digest.update(buffer); buffer.clear()
            }
            return digest.digest()
        }
    }
}
