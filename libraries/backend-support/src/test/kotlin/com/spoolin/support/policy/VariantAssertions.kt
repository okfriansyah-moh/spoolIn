package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal object VariantAssertions {
    fun refuses(
        name: String,
        action: () -> Unit,
    ) {
        val error = runCatching(action).exceptionOrNull()
        assertTrue(error is ConfigurationViolation, "Expected a typed configuration refusal.")
        if (error is ConfigurationViolation) {
            assertEquals(name, error.setting)
            assertFalse(TextRules.blank(error.correction), "A safe correction is required.")
            assertFalse(
                error.message.orEmpty().contains("SYNTHETIC_PRIVATE_VALUE"),
                "Correction must omit supplied data.",
            )
        }
    }
}
