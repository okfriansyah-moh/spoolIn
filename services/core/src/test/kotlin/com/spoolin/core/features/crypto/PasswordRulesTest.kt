package com.spoolin.core.features.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordRulesTest {
    @Test
    fun exactUtf8PreparationDoesNotTrimNormalizeOrTruncate() {
        val password = "a".repeat(14) + "é"
        assertArrayEquals(
            java.util.HexFormat
                .of()
                .parseHex("6161616161616161616161616161c3a9"),
            PasswordInput.exactBytes(password),
        )
        assertFalse(PasswordInput.exactBytes(password).contentEquals(PasswordInput.exactBytes(" $password")))
        assertFalse(
            PasswordInput.exactBytes(password).contentEquals(PasswordInput.exactBytes("a".repeat(14) + "e\u0301")),
        )
        assertArrayEquals(byteArrayOf(32, 9, 10), PasswordInput.exactBytes(" \t\n"))
        assertThrows(IllegalArgumentException::class.java) { PasswordInput.exactBytes("a\uD800") }
        assertThrows(IllegalArgumentException::class.java) { PasswordInput.exactBytes("\uDC00") }
    }

    @Test
    fun inclusiveNewPasswordBoundsUseCodePointsAndUtf8Octets() {
        val policy = NewPasswordPolicy()
        assertFalse(policy.accepts("a".repeat(14)))
        assertTrue(policy.accepts("a".repeat(15)))
        assertTrue(policy.accepts("a".repeat(128)))
        assertFalse(policy.accepts("a".repeat(129)))
        assertTrue(policy.accepts("😀".repeat(128)))
        assertFalse(policy.accepts("😀".repeat(128) + "a"))
        assertFalse(policy.accepts("\uD800" + "a".repeat(15)))
        assertThrows(IllegalArgumentException::class.java) { NewPasswordPolicy(0, 128) }
        assertThrows(IllegalArgumentException::class.java) { NewPasswordPolicy(129, 128) }
    }

    @Test
    fun existingCredentialEligibilityIgnoresNewPasswordPolicyTightening() {
        val old = "a".repeat(15)
        assertFalse(NewPasswordPolicy(20, 128).accepts(old))
        assertTrue(PasswordInput.verificationEligible(old))
        assertTrue(PasswordInput.verificationEligible("short old"))
        assertFalse(PasswordInput.verificationEligible("\uD800"))
    }

    @Test
    fun phcParsingBoundsResourceCostBeforeHashing() {
        val salt = "MDEyMzQ1Njc4OWFiY2RlZg"
        val hash = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY"
        val valid = "\$argon2id\$v=19\$m=65536,t=3,p=1\$$salt\$$hash"
        assertFalse(requireNotNull(PhcParameters.parse(valid)).needsRehash())
        assertTrue(requireNotNull(PhcParameters.parse(valid.replace("t=3", "t=2"))).needsRehash())
        listOf(
            valid.replace("m=65536", "m=999999"),
            valid.replace("t=3", "t=99"),
            valid.replace("p=1", "p=9"),
            valid.replace("v=19", "v=16"),
            valid.replace("argon2id", "argon2i"),
            valid.replace("m=65536", "m=1"),
            valid + "=",
            "a".repeat(513),
            "",
            valid.replace(salt, "a"),
        ).forEach { assertNull(PhcParameters.parse(it)) }
    }
}
