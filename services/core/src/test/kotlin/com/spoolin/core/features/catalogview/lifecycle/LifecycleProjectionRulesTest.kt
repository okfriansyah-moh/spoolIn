package com.spoolin.core.features.catalogview.lifecycle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LifecycleProjectionRulesTest {
    @Test
    fun `lifecycle graphql exposes bind entry for catalog lifecycle mutations`() {
        val methods = LifecycleGraphql::class.java.declaredMethods.map { it.name }
        assertEquals(true, methods.contains("bind"))
        assertEquals(true, methods.any { it.contains("Lifecycle", ignoreCase = true) })
    }
}
