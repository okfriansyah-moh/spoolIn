package com.spoolin.support.receipts

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

private const val MAX_ATTEMPTS = 10
private const val MAX_BASE_DELAY_MS = 60000
private const val MAX_CAP_DELAY_MS = 3600000
private const val MAX_MULTIPLIER = 10
private const val UUID_BYTES = 16
private const val VERSION_OFFSET = 6
private const val VARIANT_OFFSET = 8
private const val VERSION_MASK = 15
private const val VERSION_FIVE = 80
private const val VARIANT_MASK = 63
private const val RFC_VARIANT = 128

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
        require(
            maxAttempts in 1..MAX_ATTEMPTS && baseDelayMs in 1..MAX_BASE_DELAY_MS && capDelayMs in 1..MAX_CAP_DELAY_MS,
        )
        require(multiplier in 1..MAX_MULTIPLIER && capDelayMs >= baseDelayMs)
    }

    fun id(parent: UUID): UUID {
        val namespace =
            ByteBuffer
                .allocate(
                    UUID_BYTES,
                ).putLong(parent.mostSignificantBits)
                .putLong(parent.leastSignificantBits)
                .array()
        val bytes =
            MessageDigest
                .getInstance(
                    "SHA-1",
                ).digest(namespace + step.toByteArray(Charsets.UTF_8))
                .copyOf(UUID_BYTES)
        bytes[VERSION_OFFSET] = ((bytes[VERSION_OFFSET].toInt() and VERSION_MASK) or VERSION_FIVE).toByte()
        bytes[VARIANT_OFFSET] = ((bytes[VARIANT_OFFSET].toInt() and VARIANT_MASK) or RFC_VARIANT).toByte()
        val result = ByteBuffer.wrap(bytes)
        return UUID(result.long, result.long)
    }
}
