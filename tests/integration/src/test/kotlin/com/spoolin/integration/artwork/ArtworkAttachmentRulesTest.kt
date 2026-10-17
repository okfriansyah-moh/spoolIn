package com.spoolin.integration.artwork

import com.spoolin.catalog.features.artwork.ArtworkAttachmentState
import com.spoolin.media.features.cleanup.CleanupRules
import com.spoolin.media.features.hold.ReferenceState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Cross-owner artwork attachment rule checks across catalog and media owners (T023). */
class ArtworkAttachmentRulesTest {
    @Test
    fun `preparing artwork can attach or abort but not regress from attached`() {
        assertTrue(ArtworkAttachmentState.PREPARING.canTransitionTo(ArtworkAttachmentState.ATTACHED))
        assertTrue(ArtworkAttachmentState.PREPARING.canTransitionTo(ArtworkAttachmentState.ABORTED))
        assertFalse(ArtworkAttachmentState.ATTACHED.canTransitionTo(ArtworkAttachmentState.ABORTED))
    }

    @Test
    fun `media hold release and cleanup respect active references`() {
        assertTrue(ReferenceState.ACTIVE.canTransitionTo(ReferenceState.RELEASED))
        assertTrue(CleanupRules.blocksDeletion(1))
        assertFalse(CleanupRules.blocksDeletion(0))
    }
}
