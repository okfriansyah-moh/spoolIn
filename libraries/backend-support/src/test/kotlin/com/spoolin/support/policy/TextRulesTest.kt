package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextRulesTest {
    @Test
    fun `ATDD-276 accepts one hundred supplementary characters without UTF16 counting`() {
        val value = "😀".repeat(100)
        assertEquals(value, TextRules.ordinary(value, TextBound(1, 100)))
    }
}
