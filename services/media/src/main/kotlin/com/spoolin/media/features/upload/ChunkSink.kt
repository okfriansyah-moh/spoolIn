@file:Suppress("ReturnCount", "MagicNumber")

package com.spoolin.media.features.upload

import com.spoolin.media.features.processing.ProcessingBounds
import java.io.OutputStream
import java.security.MessageDigest

/**
 * Bounded, integrity-checked byte sink for an artwork upload stream (§7.6 / ATDD-159).
 *
 * Accepts a declared byte count and SHA-256 advertised by the caller.
 * Rejects early when the running count exceeds [maxBytes] (backpressure / decompression-bomb guard).
 * On [complete], verifies the exact received byte count and checksum match declarations.
 *
 * Invariants:
 * - All written bytes are counted before flushing to the [outputStream].
 * - The caller must close [outputStream] in a finally block; this class does not own the stream.
 * - [isCancelled] is checked before each chunk to honour stream cancellation (AC-041).
 */
class ChunkSink(
    private val outputStream: OutputStream,
    private val declaredBytes: Long,
    private val declaredSha256: ByteArray,
    private val maxBytes: Long = ProcessingBounds.MAX_INPUT_BYTES,
) {
    private var received: Long = 0L
    private val digest = MessageDigest.getInstance("SHA-256")
    var isCancelled: Boolean = false
        private set

    sealed class WriteResult {
        object Accepted : WriteResult()

        data class Rejected(
            val errorCode: String,
        ) : WriteResult()
    }

    sealed class CompleteResult {
        data class Accepted(
            val inputHash: ByteArray,
            val inputBytes: Long,
        ) : CompleteResult()

        data class Rejected(
            val errorCode: String,
        ) : CompleteResult()
    }

    /** Validate header-declared byte count and digest before accepting any data. */
    fun validateHeader(): WriteResult {
        if (declaredBytes < ProcessingBounds.MIN_INPUT_BYTES) return WriteResult.Rejected("FILE_EMPTY")
        if (declaredBytes > maxBytes) return WriteResult.Rejected("FILE_TOO_LARGE")
        if (declaredSha256.size != 32) return WriteResult.Rejected("INVALID_CHECKSUM")
        return WriteResult.Accepted
    }

    /** Write one data chunk, updating running count and digest. Checks bounds and cancellation. */
    fun writeChunk(data: ByteArray): WriteResult {
        if (isCancelled) return WriteResult.Rejected("UPLOAD_CANCELLED")
        val next = received + data.size
        if (next > maxBytes) return WriteResult.Rejected("FILE_TOO_LARGE")
        outputStream.write(data)
        digest.update(data)
        received = next
        return WriteResult.Accepted
    }

    /** Mark the stream as cancelled; subsequent [writeChunk] calls will be rejected. */
    fun cancel() {
        isCancelled = true
    }

    /**
     * Verify exact byte count and SHA-256 after the last chunk.
     * Returns the input hash for use as [AssetSql.acceptProcessing] evidence.
     */
    fun complete(): CompleteResult {
        if (isCancelled) return CompleteResult.Rejected("UPLOAD_CANCELLED")
        if (received != declaredBytes) return CompleteResult.Rejected("BYTE_COUNT_MISMATCH")
        val actual = digest.digest()
        if (!actual.contentEquals(declaredSha256)) return CompleteResult.Rejected("CHECKSUM_MISMATCH")
        return CompleteResult.Accepted(inputHash = actual, inputBytes = received)
    }
}
