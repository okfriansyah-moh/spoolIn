package com.spoolin.media.features.cleanup

import com.spoolin.media.features.asset.AssetState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CleanupRulesTest {
    @Test
    fun `protective hold prevents deletion`() {
        assertTrue(CleanupRules.blocksDeletion(1))
        assertFalse(CleanupRules.mayDeleteAsset(AssetState.READY, 1))
    }

    @Test
    fun `unreferenced READY asset may delete`() {
        assertTrue(CleanupRules.mayDeleteAsset(AssetState.READY, 0))
    }
}
