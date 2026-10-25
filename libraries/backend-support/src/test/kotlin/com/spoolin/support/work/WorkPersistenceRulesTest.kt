package com.spoolin.support.work

import com.spoolin.support.fixtures.JdbcBoundary
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class WorkPersistenceRulesTest {
    @TempDir lateinit var root: Path
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val worker = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val now = Instant.parse("2026-01-02T03:04:05Z")
    private fun row(kind: String = "MEDIA_PROCESS", state: String = "EXHAUSTED") = mapOf<String, Any?>(
        "id" to id, "operation_id" to id, "correlation_id" to id, "kind" to kind, "state" to state,
        "payload" to "{}", "attempt_count" to 2, "max_attempts" to 4, "generation" to 3,
        "deadline_at" to Timestamp.from(now.plusSeconds(60)), "lease_owner" to worker, "lease_epoch" to 5L,
        "lease_until" to Timestamp.from(now.plusSeconds(30)), "base_delay_ms" to 100,
        "cap_delay_ms" to 1000, "backoff_multiplier" to 2, "last_error" to "PROCESSING_TIMEOUT",
    )
    private fun claim() = WorkSql.selectForClaim(JdbcBoundary().rows(row()).connection, now).single()
    private fun commit() = WorkRecoveryCommit("operator", "Retry after verified outage", id)
    private fun history(type: String) = mapOf<String, Any?>("event_type" to type, "generation" to "3", "attempt_no" to "1", "lease_epoch" to "5",
        "attempt_outcome" to "TRANSIENT", "error_code" to "NOTIFICATION_CONNECT_FAILED", "created_at" to now.toString())

    @Test fun `claim selection decodes immutable retry and fence snapshot`() {
        val selected = claim()
        assertEquals(id, selected.workId)
        assertEquals("MEDIA_PROCESS", selected.kind)
        assertEquals(3, selected.generation)
        assertEquals(5L, selected.leaseEpoch)
        assertEquals(2, selected.attemptCount)
        assertEquals(now.plusSeconds(60), selected.deadlineAt)
        assertEquals(1000, selected.capDelayMs)
        assertEquals(30, WorkSql.leaseDuration)
        assertEquals(10, WorkSql.heartbeatInterval)
        assertEquals(15, WorkSql.maxEffectDuration)
        assertTrue(WorkSql.selectForClaim(JdbcBoundary().connection, now, allowedKinds = setOf("MEDIA_PROCESS")).isEmpty())
        for (batch in listOf(0, 11)) assertThrows(IllegalArgumentException::class.java) { WorkSql.selectForClaim(JdbcBoundary().connection, now, batch) }
        for (kinds in listOf(emptySet(), setOf("bad-input"), (1..11).map { "TYPE_$it" }.toSet())) {
            assertThrows(IllegalArgumentException::class.java) { WorkSql.selectForClaim(JdbcBoundary().connection, now, allowedKinds = kinds) }
        }
    }

    @Test fun `claim commit advances the attempt tuple and returns no ownership after a lost race`() {
        val before = claim()
        val claimed = requireNotNull(WorkSql.commitClaim(JdbcBoundary().connection, before, worker, now))
        assertEquals(worker, claimed.leaseOwner)
        assertEquals(6L, claimed.leaseEpoch)
        assertEquals(3, claimed.attemptCount)
        assertEquals(before.workId, claimed.workId)
        assertEquals(before.generation, claimed.generation)
        assertNull(WorkSql.commitClaim(JdbcBoundary().apply { updates.add(0) }.connection, before, worker, now))
    }

    @Test fun `renewal and terminal commits expose superseded ownership without retrying effects`() {
        val claim = claim()
        assertTrue(WorkSql.renewLease(JdbcBoundary().connection, claim, now))
        assertFalse(WorkSql.renewLease(JdbcBoundary().apply { updates.add(0) }.connection, claim, now))
        for ((outcome, state) in listOf(AttemptOutcome.SUCCEEDED to "DONE", AttemptOutcome.TRANSIENT to "READY",
            AttemptOutcome.PERMANENT to "EXHAUSTED", AttemptOutcome.CANCELLED to "CANCELLED", AttemptOutcome.UNCERTAIN to "LEASED")) {
            val completion = WorkAttemptCompletion(outcome, "SAFE_ERROR", state, now.plusSeconds(1))
            assertTrue(WorkSql.completeAttempt(JdbcBoundary().connection, claim, completion, now))
            assertFalse(WorkSql.completeAttempt(JdbcBoundary().apply { updates.add(0) }.connection, claim, completion, now))
        }
        val status = requireNotNull(WorkSql.leaseStatus(JdbcBoundary().rows(row(state = "LEASED")).connection, id))
        assertEquals(worker, status.leaseOwner)
        assertEquals(now.plusSeconds(30), status.leaseUntil)
        assertNull(WorkSql.leaseStatus(JdbcBoundary().connection, id))
        assertNull(WorkSql.leaseStatus(JdbcBoundary().rows(row() + ("lease_until" to null)).connection, id)!!.leaseUntil)
    }

    @Test fun `controlled recovery reports unavailable and lost races without claiming success`() {
        assertFalse(WorkSql.recoverWork(JdbcBoundary().connection, id, commit(), now))
        val lost = JdbcBoundary().rows(row()).apply { updates.add(0) }
        assertFalse(WorkSql.recoverWork(lost.connection, id, commit(), now))
        assertTrue(WorkSql.recoverWork(JdbcBoundary().rows(row()).connection, id, commit(), now))
        for (operator in listOf("", "x".repeat(97))) {
            assertThrows(IllegalArgumentException::class.java) { WorkSql.recoverWork(JdbcBoundary().connection, id, commit().copy(operatorId = operator), now) }
        }
        assertThrows(IllegalArgumentException::class.java) { WorkRecoveryRequest(id, "operator", " ") }
    }

    @Test fun `inspection exposes bounded phase history and safe missing work result`() {
        assertNull(WorkRecoveryOperations.inspect(JdbcBoundary().connection, id))
        val events = (1..1001).map { history("ATTEMPT_FINISHED") }
        val db = JdbcBoundary().rows(row()).apply { queries.add(events) }
        val result = requireNotNull(WorkRecoveryOperations.inspect(db.connection, id))
        assertEquals(1000, result.phaseHistory.size)
        assertTrue(result.historyTruncated)
        assertEquals("PROCESSING_TIMEOUT", result.error)
        assertEquals(2, result.attempts)
    }

    @Test fun `notification retry requires proven unaccepted complete attempts`() {
        val base = WorkRecoveryInspection(id, "NOTIFICATION", "EXHAUSTED", 3, 5, 1, 4, now, null, id, id,
            listOf(history("ATTEMPT_STARTED").mapValues { it.value?.toString() }, history("ATTEMPT_FINISHED").mapValues { it.value?.toString() }))
        fun db(state: String = "FAILED", mode: String = "slack", reference: String? = null) = JdbcBoundary().rows(mapOf(
            "notification_state" to state, "notification_mode" to mode, "provider_reference" to reference, "notification_summary" to "{}"))
        WorkRecoveryOperations.validateNotification(base, db().connection)
        WorkRecoveryOperations.validateNotification(base.copy(attempts = 0, phaseHistory = emptyList()), db(mode = "google_chat").connection)
        for (invalid in listOf(db("UNKNOWN"), db(mode = "smtp"), db(reference = "accepted"))) {
            assertThrows(IllegalArgumentException::class.java) { WorkRecoveryOperations.validateNotification(base, invalid.connection) }
        }
        for (invalid in listOf(base.copy(historyTruncated = true), base.copy(attempts = 2), base.copy(phaseHistory = base.phaseHistory.take(1)),
            base.copy(phaseHistory = base.phaseHistory.map { it + ("attempt_outcome" to "UNCERTAIN") }),
            base.copy(phaseHistory = base.phaseHistory.map { it + ("error_code" to "NOTIFICATION_UNCERTAIN") }))) {
            assertThrows(IllegalArgumentException::class.java) { WorkRecoveryOperations.validateNotification(invalid, db().connection) }
        }
    }

    @Test fun `media recovery verifies accepted local input before changing generation`() {
        val bytes = byteArrayOf(1, 4, 9)
        Files.createDirectories(root.resolve("quarantine"))
        Files.write(root.resolve("quarantine/$id"), bytes)
        val asset = mapOf("id" to id, "state" to "PROCESSING", "receipt_state" to "PENDING", "input_bytes" to 3L,
            "processing_staging_key" to "quarantine/$id", "processing_input_hash" to MessageDigest.getInstance("SHA-256").digest(bytes))
        val db = JdbcBoundary().rows(row()).rows().rows(asset).rows(mapOf("1" to Timestamp.from(now.minusSeconds(30))))
            .rows(mapOf("1" to Timestamp.from(now))).rows(mapOf("1" to Timestamp.from(now))).rows(row())
        assertTrue(WorkRecoveryOperations.recover(db.connection, WorkRecoveryAction(id, 3, 5, commit()), root))
        for (invalid in listOf(asset + ("state" to "READY"), asset + ("receipt_state" to "SUCCEEDED"),
            asset + ("input_bytes" to 4L), asset + ("processing_input_hash" to byteArrayOf(0)),
            asset + ("processing_staging_key" to "quarantine/other"))) {
            val rejected = JdbcBoundary().rows(row()).rows().rows(invalid)
            assertThrows(IllegalArgumentException::class.java) {
                WorkRecoveryOperations.recover(rejected.connection, WorkRecoveryAction(id, 3, 5, commit()), root)
            }
        }
    }

    @Test fun `recovery refuses changed fence incomplete evidence and unknown destination`() {
        for (record in listOf(row(state = "DONE"), row() + ("generation" to 4), row() + ("lease_epoch" to 6L))) {
            assertThrows(IllegalArgumentException::class.java) {
                WorkRecoveryOperations.recover(JdbcBoundary().rows(record).rows().connection, WorkRecoveryAction(id, 3, 5, commit()), root)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkRecoveryOperations.recover(JdbcBoundary().rows(row("EXTERNAL_WRITE")).rows().connection, WorkRecoveryAction(id, 3, 5, commit()), root)
        }
    }
}
