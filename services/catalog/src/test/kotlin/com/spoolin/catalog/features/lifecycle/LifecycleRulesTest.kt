package com.spoolin.catalog.features.lifecycle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LifecycleRulesTest {
    @Test
    fun `active record allows archive only`() {
        assertEquals(setOf(LifecycleRules.Action.ARCHIVE), LifecycleRules.allowedActions("ACTIVE"))
    }

    @Test
    fun `archived record allows restore or delete`() {
        assertEquals(
            setOf(LifecycleRules.Action.RESTORE, LifecycleRules.Action.DELETE),
            LifecycleRules.allowedActions("ARCHIVED"),
        )
    }

    @Test
    fun `delete requires confirmation`() {
        assertTrue(LifecycleRules.requiresConfirmation(LifecycleRules.Action.DELETE))
        assertFalse(LifecycleRules.requiresConfirmation(LifecycleRules.Action.ARCHIVE))
    }
}
