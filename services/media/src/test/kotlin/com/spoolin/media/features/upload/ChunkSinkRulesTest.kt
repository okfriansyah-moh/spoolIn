package com.spoolin.media.features.upload

import com.spoolin.media.features.processing.ProcessingBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * ATDD-159 / ATDD-163 / AC-041 / AC-149 / §7.6
 *
 * Validates bounded stream acceptance, exact byte-count/checksum verification, and
 * cancellation guard for the ChunkSink component (isolated unit — no DB access).
 */
class ChunkSinkRulesTest {
    private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun sink(
        declared: Long = 10L,
        hash: ByteArray? = null,
        max: Long = ProcessingBounds.MAX_INPUT_BYTES,
    ): Pair<ChunkSink, ByteArrayOutputStream> {
        val out = ByteArrayOutputStream()
        val data = ByteArray(declared.toInt()) { it.toByte() }
        val checksum = hash ?: sha256(data)
        return ChunkSink(out, declared, checksum, max) to out
    }

    @Test
    fun `valid header with correct count and hash is accepted`() {
        val (s, _) = sink(10L)
        assertEquals(ChunkSink.WriteResult.Accepted, s.validateHeader())
    }

    @Test
    fun `header with declared zero bytes returns FILE_EMPTY`() {
        val (s, _) = sink(0L)
        val result = s.validateHeader()
        assert(result is ChunkSink.WriteResult.Rejected)
        assertEquals("FILE_EMPTY", (result as ChunkSink.WriteResult.Rejected).errorCode)
    }

    @Test
    fun `header with declared bytes exceeding maximum returns FILE_TOO_LARGE`() {
        val (s, _) = sink(ProcessingBounds.MAX_INPUT_BYTES + 1)
        val result = s.validateHeader()
        assert(result is ChunkSink.WriteResult.Rejected)
        assertEquals("FILE_TOO_LARGE", (result as ChunkSink.WriteResult.Rejected).errorCode)
    }

    @Test
    fun `header with 31-byte checksum returns INVALID_CHECKSUM`() {
        val out = ByteArrayOutputStream()
        val s = ChunkSink(out, 10L, ByteArray(31), ProcessingBounds.MAX_INPUT_BYTES)
        val result = s.validateHeader()
        assert(result is ChunkSink.WriteResult.Rejected)
        assertEquals("INVALID_CHECKSUM", (result as ChunkSink.WriteResult.Rejected).errorCode)
    }

    @Test
    fun `valid chunk stream completes with correct inputHash and inputBytes`() {
        val data = ByteArray(10) { it.toByte() }
        val hash = sha256(data)
        val out = ByteArrayOutputStream()
        val s = ChunkSink(out, 10L, hash)

        assertEquals(ChunkSink.WriteResult.Accepted, s.validateHeader())
        assertEquals(ChunkSink.WriteResult.Accepted, s.writeChunk(data))

        val result = s.complete()
        assert(result is ChunkSink.CompleteResult.Accepted)
        val accepted = result as ChunkSink.CompleteResult.Accepted
        assertEquals(10L, accepted.inputBytes)
        assertTrue(accepted.inputHash.contentEquals(hash))
    }

    @Test
    fun `chunk pushing running total above max is rejected with FILE_TOO_LARGE`() {
        val (s, _) = sink(5L, max = 5L)
        s.validateHeader()
        assertEquals(ChunkSink.WriteResult.Accepted, s.writeChunk(ByteArray(5)))
        val result = s.writeChunk(ByteArray(1))
        assert(result is ChunkSink.WriteResult.Rejected)
        assertEquals("FILE_TOO_LARGE", (result as ChunkSink.WriteResult.Rejected).errorCode)
    }

    @Test
    fun `received count mismatch returns BYTE_COUNT_MISMATCH`() {
        val data = ByteArray(10) { it.toByte() }
        val out = ByteArrayOutputStream()
        val s = ChunkSink(out, 20L, sha256(data)) // declared 20 but will send only 10
        s.validateHeader()
        s.writeChunk(data)
        val result = s.complete()
        assert(result is ChunkSink.CompleteResult.Rejected)
        assertEquals("BYTE_COUNT_MISMATCH", (result as ChunkSink.CompleteResult.Rejected).errorCode)
    }

    @Test
    fun `checksum mismatch returns CHECKSUM_MISMATCH`() {
        val data = ByteArray(10) { it.toByte() }
        val wrongHash = ByteArray(32) { 0xFF.toByte() }
        val out = ByteArrayOutputStream()
        val s = ChunkSink(out, 10L, wrongHash)
        s.validateHeader()
        s.writeChunk(data)
        val result = s.complete()
        assert(result is ChunkSink.CompleteResult.Rejected)
        assertEquals("CHECKSUM_MISMATCH", (result as ChunkSink.CompleteResult.Rejected).errorCode)
    }

    @Test
    fun `cancelled sink returns UPLOAD_CANCELLED from complete`() {
        val (s, _) = sink(10L)
        s.validateHeader()
        s.cancel()
        val result = s.complete()
        assert(result is ChunkSink.CompleteResult.Rejected)
        assertEquals("UPLOAD_CANCELLED", (result as ChunkSink.CompleteResult.Rejected).errorCode)
    }

    @Test
    fun `writeChunk on cancelled sink returns UPLOAD_CANCELLED`() {
        val (s, _) = sink(10L)
        s.validateHeader()
        s.cancel()
        val result = s.writeChunk(ByteArray(1))
        assert(result is ChunkSink.WriteResult.Rejected)
        assertEquals("UPLOAD_CANCELLED", (result as ChunkSink.WriteResult.Rejected).errorCode)
    }

    @Test
    fun `exact declared maximum input bytes passes header check`() {
        val (s, _) = sink(ProcessingBounds.MAX_INPUT_BYTES)
        assertEquals(ChunkSink.WriteResult.Accepted, s.validateHeader())
    }
}
