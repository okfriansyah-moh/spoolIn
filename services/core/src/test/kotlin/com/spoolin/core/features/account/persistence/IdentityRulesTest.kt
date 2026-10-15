package com.spoolin.core.features.account.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class IdentityRulesTest {
    private val rules = IdentityRules()

    @Test
    fun caseFoldingAndIdna2008PreserveIdentityDistinctions() {
        assertEquals("a@example.org", rules.normalizedEmail(" A@EXAMPLE.ORG "))
        assertEquals("a@xn--fa-hia.de", rules.normalizedEmail("A@faß.de"))
        assertNotEquals(rules.normalizedEmail("a@faß.de"), rules.normalizedEmail("a@fass.de"))
        assertEquals("a@xn--bcher-kva.example", rules.normalizedEmail("A@bücher.example"))
        assertNotEquals(rules.normalizedEmail("a+tag@example.org"), rules.normalizedEmail("a@example.org"))
        assertNotEquals(rules.normalizedEmail("a.b@example.org"), rules.normalizedEmail("ab@example.org"))
        val identity = rules.identity("  É O'Name  ", " A@EXAMPLE.ORG ")
        assertEquals("É O'Name", identity.name)
        assertEquals("A@EXAMPLE.ORG", identity.emailDisplay)
        assertEquals("a@example.org", identity.emailNormalized)
    }

    @Test
    fun unsupportedEmailSyntaxAndOctetOverflowReject() {
        listOf(
            "a\r\n@example.org",
            "a@example.org\n",
            "A <a@example.org>",
            "é@example.org",
            "a..b@example.org",
            ".a@example.org",
            "a.@example.org",
            "a@@example.org",
            "a@",
            "a@-bad.example",
            "a@example.org.",
            "a@x..org",
            "a@a_b.org",
            "a".repeat(65) + "@example.org",
            "a@" + "x".repeat(64) + ".org",
            "a@😀.org",
            "a@\u200d.example",
            "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(62),
        ).forEachIndexed {
            index,
            value,
            ->
            assertThrows(
                IllegalArgumentException::class.java,
                { rules.normalizedEmail(value) },
                "Invalid email row $index was accepted",
            )
        }
    }

    @Test
    fun idna2008LabelAndContextRulesRejectCompatibilityAndMalformedAlabels() {
        val accepted =
            listOf(
                "faß.de",
                "βόλος.example",
                "l·l.example",
                "例え.テスト",
                "\u0cf1.example",
                "\u0cf2.example",
                "a\uD804\uDDC9.example",
                "क्\u200dष.example",
            )
        accepted.forEach { rules.normalizedEmail("a@$it") }
    }

    @Test
    fun idna2008RejectsDisallowedLabelsAndWholeDomainBidiViolations() {
        val rejected =
            listOf(
                "xn--e28h.example",
                "xn--.example",
                "xn--a.example",
                "xn--not-valid-.example",
                "ab--cd.example",
                "-a.example",
                "a-.example",
                "\u0301a.example",
                "a·l.example",
                "a\u0375.example",
                "a\u05f3.example",
                "a\u30fb.example",
                "a\u200c.example",
                "a\u200d.example",
                "a\u0640.example",
                "a\u07fa.example",
                "a\u303b.example",
                "a\u19da.example",
                "a\u0378.example",
                "a\uffff.example",
                "a\u00ad.example",
                "ａ.example",
                "a\u20dd.example",
                "a\u1100.example",
                "\u05d0\u05d1.example.123",
                "אa.example",
            )
        rejected.forEachIndexed { index, domain ->
            assertThrows(
                IllegalArgumentException::class.java,
                { rules.normalizedEmail("a@$domain") },
                "Invalid IDNA row $index was accepted",
            )
        }
        assertEquals(rules.normalizedEmail("a@é.example"), rules.normalizedEmail("a@e\u0301.example"))
    }

    @Test
    fun nameBoundsCountCodePointsWithoutChangingInternalText() {
        assertEquals("😀😀", rules.identity("😀😀", "a@b.org").name)
        assertEquals("x".repeat(100), rules.identity("x".repeat(100), "a@b.org").name)
        listOf("x", "x".repeat(101), "ab\u0000", "ab\uD800").forEach {
            assertThrows(IllegalArgumentException::class.java) { rules.identity(it, "a@b.org") }
        }
    }
}
