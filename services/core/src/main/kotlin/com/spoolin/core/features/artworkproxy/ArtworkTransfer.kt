package com.spoolin.core.features.artworkproxy

import io.grpc.Status
import spoolin.v1.Spoolin
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private const val MAX_CHUNK_BYTES = 65_536

internal object ArtworkTransfer {
    fun receive(chunks: Iterator<Spoolin.AssetReadChunk>): ArtworkReadProxy.VerifiedAsset {
        val header = header(chunks)
        val contentType = ArtworkReadValidation.contentType(header)
        val path = ArtworkScratch.create()
        var verified = false
        try {
            val hash = MessageDigest.getInstance("SHA-256")
            val bytes = write(chunks, path, header.outputBytes, hash)
            if (bytes != header.outputBytes ||
                !MessageDigest.isEqual(hash.digest(), header.contentHash.toByteArray())
            ) {
                throw Status.DATA_LOSS.asRuntimeException()
            }
            val result = ArtworkReadProxy.VerifiedAsset(path, contentType, bytes)
            verified = true
            return result
        } finally {
            if (!verified) ArtworkScratch.delete(path)
        }
    }

    fun header(chunks: Iterator<Spoolin.AssetReadChunk>): Spoolin.AssetReadHeader {
        if (!chunks.hasNext()) throw Status.DATA_LOSS.asRuntimeException()
        val first = chunks.next()
        if (!first.hasHeader()) throw Status.DATA_LOSS.asRuntimeException()
        ArtworkReadValidation.contentType(first.header)
        return first.header
    }

    private fun write(
        chunks: Iterator<Spoolin.AssetReadChunk>,
        path: Path,
        maximum: Long,
        hash: MessageDigest,
    ): Long =
        Files.newOutputStream(path).use { output ->
            writeChunks(chunks, output, maximum, hash)
        }

    private fun writeChunks(
        chunks: Iterator<Spoolin.AssetReadChunk>,
        output: OutputStream,
        maximum: Long,
        hash: MessageDigest,
    ): Long {
        var bytes = 0L
        while (chunks.hasNext()) {
            val data = data(chunks.next())
            if (data.size.toLong() > maximum - bytes) throw Status.DATA_LOSS.asRuntimeException()
            bytes += data.size
            hash.update(data)
            output.write(data)
        }
        return bytes
    }

    private fun data(chunk: Spoolin.AssetReadChunk): ByteArray {
        if (!chunk.hasData() || chunk.data.size() !in 1..MAX_CHUNK_BYTES) throw Status.DATA_LOSS.asRuntimeException()
        return chunk.data.toByteArray()
    }
}
