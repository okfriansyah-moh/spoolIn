package com.spoolin.media.features.cleanup

import com.spoolin.media.features.asset.AssetState

/** Reference-safe cleanup predicates (AC-044-C02 / ATDD-167). */
object CleanupRules {
    fun mayDeleteAsset(state: AssetState, protectiveHoldCount: Int): Boolean =
        protectiveHoldCount == 0 &&
            state in setOf(AssetState.READY, AssetState.REJECTED, AssetState.DELETING)

    fun blocksDeletion(protectiveHoldCount: Int): Boolean = protectiveHoldCount > 0
}
