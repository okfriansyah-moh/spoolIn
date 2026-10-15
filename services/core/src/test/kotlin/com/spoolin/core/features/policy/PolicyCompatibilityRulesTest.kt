package com.spoolin.core.features.policy

import com.spoolin.core.features.crypto.PasswordInput
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicySettings
import com.spoolin.support.policy.TextRules
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PolicyCompatibilityRulesTest {
    @Test
    fun `T009-A2 selected current minimum does not alter existing credential eligibility or bytes`() {
        val old = " a\u00a0 ".repeat(2)
        val current = PolicySettings.resolve(ConfigurationSources(mapOf("PASSWORD_MIN_CODE_POINTS" to "30")))
        assertTrue(PasswordInput.verificationEligible(old))
        assertArrayEquals(old.toByteArray(Charsets.UTF_8), PasswordInput.exactBytes(old))
        assertFalse(runCatching { TextRules.ordinary(old, current.textBounds.getValue("password")) }.isSuccess)
        assertTrue(PasswordInput.verificationEligible(old))
    }
}
