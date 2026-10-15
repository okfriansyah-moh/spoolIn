package com.spoolin.core.features.crypto

import java.math.BigInteger
import java.nio.ByteBuffer

object NonceCounter {
    fun encode(counter: String): ByteArray {
        val value = counter.toBigInteger()
        require(value.signum() > 0 && value <= BigInteger.valueOf(Long.MAX_VALUE)) { "Invalid nonce counter" }
        return ByteBuffer
            .allocate(NONCE_BYTES)
            .putInt(0)
            .putLong(value.longValueExact())
            .array()
    }
}
