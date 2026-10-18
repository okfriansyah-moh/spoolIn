package com.spoolin.catalog.operations.receipts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SafeReceiptProjectionRulesTest {
    @Test
    fun `approved terminal decisions and retained artwork codes remain readable`() {
        for (code in listOf(
            "REJECTED",
            "CANCELLED",
            "FIELD_INVALID",
            "UNAVAILABLE",
            "FILE_INVALID",
            "ARTWORK_UNAVAILABLE",
            "ARTWORK_RETRY_EXHAUSTED",
        )) {
            assertEquals(code, SafeReceiptProjection.errorCode(code))
        }
    }

    @Test
    fun `an unrecognized stored problem cannot expose arbitrary text`() {
        val failure =
            assertThrows(IllegalStateException::class.java) {
                SafeReceiptProjection.errorCode("untrusted stored provider response")
            }
        assertEquals("Stored operation problem is unavailable", failure.message)
    }
}
