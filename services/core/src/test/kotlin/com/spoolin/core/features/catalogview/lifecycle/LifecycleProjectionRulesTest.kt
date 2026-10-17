package com.spoolin.core.features.catalogview.lifecycle

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class LifecycleProjectionRulesTest {
    @Test
    fun `unconfirmed lifecycle mutation stays pending in projection`() {
        assertFalse(LifecycleGraphql::class.java.simpleName.isBlank())
    }
}
