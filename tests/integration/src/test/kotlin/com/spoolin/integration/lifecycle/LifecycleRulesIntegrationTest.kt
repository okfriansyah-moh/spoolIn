package com.spoolin.integration.lifecycle

import com.spoolin.catalog.features.lifecycle.LifecycleRules
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class LifecycleRulesIntegrationTest {
    @Test
    fun `deleted status exposes no further actions`() {
        assertFalse(LifecycleRules.allowedActions("DELETED").isNotEmpty())
    }
}
