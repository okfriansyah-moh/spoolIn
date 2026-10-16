package com.spoolin.support.receipts

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

data class WorkRequest(
    val step: String,
    val kind: String,
    val subjectId: UUID,
    val deadline: Instant,
    val maxAttempts: Int = 3,
    val baseDelayMs: Int = 500,
    val capDelayMs: Int = 5000,
    val multiplier: Int = 2,
) {
    init {
        require(step.matches(Regex("[A-Za-z][A-Za-z0-9_-]{0,63}")))
        require(kind.matches(Regex("[A-Z][A-Z0-9_]{0,63}")) && kind != "NOTIFICATION")
        require(maxAttempts in 1..10 && baseDelayMs in 1..60000 && capDelayMs in 1..3600000)
        require(multiplier in 1..10 && capDelayMs >= baseDelayMs)
    }

    fun id(parent: UUID): UUID {
        val namespace =
            ByteBuffer
                .allocate(
                    16,
                ).putLong(parent.mostSignificantBits)
                .putLong(parent.leastSignificantBits)
                .array()
        val bytes = MessageDigest.getInstance("SHA-1").digest(namespace + step.toByteArray(Charsets.UTF_8)).copyOf(16)
        bytes[6] = ((bytes[6].toInt() and 15) or 80).toByte()
        bytes[8] = ((bytes[8].toInt() and 63) or 128).toByte()
        val result = ByteBuffer.wrap(bytes)
        return UUID(result.long, result.long)
    }
}
