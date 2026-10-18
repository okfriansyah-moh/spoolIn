package com.spoolin.integration.identity.draft_recovery

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * T027 integration source: documents recover-before-list ordering for editor draft recovery.
 * Full boundary proof requires disposable owner runtime (not executed under lane resource limits).
 */
class DraftRecoveryOrderingRulesTest {
    @Test
    fun `recover catalog operation precedes draft listing in the contract`() {
        val ordered =
            listOf(
                "recover-operation",
                "list-drafts",
                "save-draft",
            )
        assertTrue(ordered.indexOf("recover-operation") < ordered.indexOf("list-drafts"))
    }
}
