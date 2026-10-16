package com.spoolin.core.features.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NonceRulesTest {
    @Test
    fun allocatedCountersHaveTheSpecifiedTwelveByteEncoding() {
        mapOf(
            "1" to "000000000000000000000001",
            "2" to "000000000000000000000002",
            "9223372036854775807" to "000000007fffffffffffffff",
        ).forEach { (counter, hex) ->
            assertArrayEquals(
                java.util.HexFormat
                    .of()
                    .parseHex(hex),
                NonceCounter.encode(counter),
            )
        }
    }

    @Test
    fun invalidCountersCannotWrap() {
        listOf("0", "-1", "9223372036854775808", "18446744073709551617", "invalid").forEach {
            assertThrows(IllegalArgumentException::class.java) { NonceCounter.encode(it) }
        }
    }
}
