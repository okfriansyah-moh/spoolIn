package com.spoolin.core.features.artworkproxy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArtworkProgressRulesTest {
    @Test
    fun `transfer at one hundred percent is not saved`() {
        assertTrue(
            ArtworkProgressRules.transferCompleteIsNotSaved(
                transferred = 100,
                declared = 100,
                attached = false,
            ),
        )
    }

    @Test
    fun `attached bytes show saved stage`() {
        assertEquals(
            ArtworkProgressRules.Stage.SAVED,
            ArtworkProgressRules.stageFromBytes(100, 100, processing = false, attached = true),
        )
    }
}
