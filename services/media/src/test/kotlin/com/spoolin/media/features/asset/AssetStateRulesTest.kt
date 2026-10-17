package com.spoolin.media.features.asset

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * ATDD-248 / TD-ROW-L5019 / §6.2.6
 *
 * Validates the declared asset lifecycle state machine.
 * State transitions are: RECEIVING→PROCESSING|REJECTED, PROCESSING→READY|REJECTED,
 * READY→DELETING, DELETING→DELETED. Terminal states have no valid successors.
 */
class AssetStateRulesTest {
    private val allowedTransitions: Set<Pair<AssetState, AssetState>> =
        setOf(
            AssetState.RECEIVING to AssetState.PROCESSING,
            AssetState.RECEIVING to AssetState.REJECTED,
            AssetState.PROCESSING to AssetState.READY,
            AssetState.PROCESSING to AssetState.REJECTED,
            AssetState.READY to AssetState.DELETING,
            AssetState.DELETING to AssetState.DELETED,
        )

    @TestFactory
    fun `declared transitions are allowed and all other pairs are blocked`(): List<DynamicTest> {
        val tests = mutableListOf<DynamicTest>()
        for (from in AssetState.entries) {
            for (to in AssetState.entries) {
                val pair = from to to
                val expected = pair in allowedTransitions
                tests +=
                    DynamicTest.dynamicTest("$from → $to (expected=$expected)") {
                        if (expected) {
                            assertTrue(from.canTransitionTo(to), "$from should allow → $to")
                        } else {
                            assertFalse(from.canTransitionTo(to), "$from should not allow → $to")
                        }
                    }
            }
        }
        return tests
    }

    @TestFactory
    fun `terminal states have no valid successors`(): List<DynamicTest> =
        listOf(AssetState.REJECTED, AssetState.DELETED).map { terminal ->
            DynamicTest.dynamicTest("$terminal has no successors") {
                for (next in AssetState.entries) {
                    assertFalse(
                        terminal.canTransitionTo(next),
                        "$terminal should have no valid successor, but canTransitionTo($next) returned true",
                    )
                }
            }
        }

    @TestFactory
    fun `READY only transitions to DELETING`(): List<DynamicTest> =
        AssetState.entries.map { candidate ->
            DynamicTest.dynamicTest("READY → $candidate") {
                if (candidate == AssetState.DELETING) {
                    assertTrue(AssetState.READY.canTransitionTo(candidate))
                } else {
                    assertFalse(AssetState.READY.canTransitionTo(candidate))
                }
            }
        }

    @TestFactory
    fun `RECEIVING can transition to PROCESSING or REJECTED only`(): List<DynamicTest> =
        AssetState.entries.map { candidate ->
            DynamicTest.dynamicTest("RECEIVING → $candidate") {
                val valid = candidate == AssetState.PROCESSING || candidate == AssetState.REJECTED
                if (valid) {
                    assertTrue(AssetState.RECEIVING.canTransitionTo(candidate))
                } else {
                    assertFalse(AssetState.RECEIVING.canTransitionTo(candidate))
                }
            }
        }
}
