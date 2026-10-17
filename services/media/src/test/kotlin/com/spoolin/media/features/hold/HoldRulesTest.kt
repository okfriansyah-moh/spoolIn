package com.spoolin.media.features.hold

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HoldRulesTest {
    @Test
    fun `RESERVED may become ACTIVE or RELEASED only`() {
        assertTrue(ReferenceState.RESERVED.canTransitionTo(ReferenceState.ACTIVE))
        assertTrue(ReferenceState.RESERVED.canTransitionTo(ReferenceState.RELEASED))
        assertFalse(ReferenceState.RESERVED.canTransitionTo(ReferenceState.RESERVED))
    }

    @Test
    fun `RELEASED is terminal`() {
        assertFalse(ReferenceState.RELEASED.canTransitionTo(ReferenceState.ACTIVE))
    }
}
