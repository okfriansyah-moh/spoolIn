package com.spoolin.integration.artwork

import com.spoolin.catalog.features.artwork.ArtworkAttachmentState
import com.spoolin.media.features.cleanup.CleanupRules
import com.spoolin.media.features.hold.ReferenceState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Cross-owner artwork attachment rule smoke checks (T023 integration selector). */
class ArtworkAttachmentRulesTest {
    @Test
    fun `hold and cleanup rules agree on protection`() {
        assertTrue(ReferenceState.ACTIVE.canTransitionTo(ReferenceState.RELEASED))
        assertTrue(CleanupRules.blocksDeletion(1))
        assertTrue(ArtworkAttachmentState.PREPARING.canTransitionTo(ArtworkAttachmentState.ATTACHED))
    }
}
