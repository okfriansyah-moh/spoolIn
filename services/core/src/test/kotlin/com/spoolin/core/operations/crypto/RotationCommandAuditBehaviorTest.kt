package com.spoolin.core.operations.crypto

import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.session.SessionJdbcFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.nio.file.Path

class RotationCommandAuditBehaviorTest {
    @Test
    fun `rotation activity records safe progress and a stable correlation for completion and failure`() {
        val db = SessionJdbcFixture(emptyList())
        val options = RotationCliOptions("old", "new", KeyPurpose.PII, Path.of("/run/secrets/keys.json"), false, false, 100)
        val audit = RotationCommandAudit(db, options, "operator")
        audit.record("STARTED", "PENDING")
        audit.completed(RotationOutcome(true, "private diagnostic message", 3, 0))
        audit.completed(RotationOutcome(false, "private failure detail", 2, 1))
        assertEquals(3, db.commits)
        val changes = db.writes.map { ObjectMapper().readValue(it[3] as String, Map::class.java) }
        assertEquals(listOf("STARTED", "COMPLETED", "INCOMPLETE"), changes.map { it["phase"] })
        assertEquals(listOf("PENDING", "SUCCEEDED", "FAILED"), db.writes.map { it[2] })
        assertEquals(
            1,
            db.writes
                .map { it[4] }
                .distinct()
                .size,
        )
        assertEquals(3, changes[1]["backfilled"])
        assertFalse(db.writes.toString().contains("private diagnostic"))
        assertFalse(db.writes.toString().contains("private failure"))
        assertFalse(db.writes.toString().contains("keys.json"))
    }
}
