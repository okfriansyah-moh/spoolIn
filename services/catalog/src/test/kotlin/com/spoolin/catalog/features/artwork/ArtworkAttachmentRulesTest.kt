package com.spoolin.catalog.features.artwork

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArtworkAttachmentRulesTest {
    @Test
    fun `PREPARING may attach or abort`() {
        assertTrue(ArtworkAttachmentState.PREPARING.canTransitionTo(ArtworkAttachmentState.ATTACHED))
        assertTrue(ArtworkAttachmentState.PREPARING.canTransitionTo(ArtworkAttachmentState.ABORTED))
    }

    @Test
    fun `terminal states cannot transition`() {
        assertFalse(ArtworkAttachmentState.ATTACHED.canTransitionTo(ArtworkAttachmentState.ABORTED))
    }
}
