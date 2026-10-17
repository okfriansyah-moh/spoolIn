package com.spoolin.integration.lifecycle

import com.spoolin.catalog.features.lifecycle.LifecycleRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LifecycleRulesIntegrationTest {
    @Test
    fun `lifecycle rules match archive restore and delete obligations`() {
        assertEquals(
            setOf(LifecycleRules.Action.ARCHIVE),
            LifecycleRules.allowedActions("ACTIVE"),
        )
        assertEquals(
            setOf(LifecycleRules.Action.RESTORE, LifecycleRules.Action.DELETE),
            LifecycleRules.allowedActions("ARCHIVED"),
        )
        assertFalse(LifecycleRules.allowedActions("DELETED").isNotEmpty())
        assertTrue(LifecycleRules.requiresConfirmation(LifecycleRules.Action.DELETE))
        assertFalse(LifecycleRules.requiresConfirmation(LifecycleRules.Action.RESTORE))
    }
}
