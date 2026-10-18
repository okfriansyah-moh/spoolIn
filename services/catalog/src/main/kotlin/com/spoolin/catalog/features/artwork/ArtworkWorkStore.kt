package com.spoolin.catalog.features.artwork

import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.catalog.operations.receipts.ReceiptWork
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkAttemptCompletion
import com.spoolin.support.work.WorkClaim
import com.spoolin.support.work.WorkSql
import java.sql.Connection
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

data class ArtworkIntent(val id: UUID, val title: UUID, val actor: UUID, val asset: UUID, val expectedVersion: Long,
    val policy: String, val state: String, val oldHold: UUID?)

class ArtworkWorkStore(private val dataSource: DataSource, private val receipts: OwnerReceipts, private val policy: () -> String) {
    fun read(id: UUID): ArtworkIntent = dataSource.connection.use { connection -> read(connection, id, false) }

    fun attach(claim: WorkClaim, intent: ArtworkIntent) = dataSource.committed { connection ->
        ArtworkRpc.lock(connection)
        lockReceipt(connection, intent.id)
        fence(connection, claim)
        val current = read(connection, intent.id, true)
        if (current.state != "PREPARING") { finish(connection, claim); return@committed }
        val title = connection.queryBound("SELECT version,status,attachment_id FROM title WHERE id=? FOR UPDATE", listOf(current.title)) {
            Triple(it.getLong("version"), it.getString("status"), it.getObject("attachment_id", UUID::class.java))
        }.singleOrNull()
        if (title == null || title.first != current.expectedVersion || title.second != "ACTIVE" || current.policy != policy()) {
            abort(connection, current, claim, "STALE_VERSION")
            return@committed
        }
        val version = current.expectedVersion + 1
        check(connection.executeBound("UPDATE title SET asset_id=?,attachment_id=?,version=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=?",
            current.asset, current.id, version, current.title, current.expectedVersion) == 1)
        check(ArtworkAttachmentSql.markAttached(connection, current.id) == 1)
        AuditInsert.record(connection, current.id, current.actor, "HUMAN", "ATTACH_ARTWORK", "TITLE", current.title,
            "SUCCEEDED", "{\"version\":{\"before\":${current.expectedVersion},\"after\":$version}}", claim.correlationId, Instant.now())
        receipts.completePending(connection, current.id, SafeResult(current.title, version))
        val confirm = WorkRequest("confirm", "ARTWORK_CONFIRM", current.id, Instant.now().plusSeconds(300))
        ReceiptWork.enqueue(connection, current.id, claim.correlationId, confirm, Instant.now())
        current.oldHold?.let { ArtworkRpc.enqueueRelease(connection, current.id, it, claim.correlationId) }
        finish(connection, claim)
    }

    fun abort(claim: WorkClaim, intent: ArtworkIntent, code: String) = dataSource.committed { connection ->
        ArtworkRpc.lock(connection)
        lockReceipt(connection, intent.id)
        fence(connection, claim)
        val current = read(connection, intent.id, true)
        if (current.state == "PREPARING") abort(connection, current, claim, code) else finish(connection, claim)
    }

    private fun abort(connection: Connection, intent: ArtworkIntent, claim: WorkClaim, code: String) {
        ArtworkAttachmentSql.markAborted(connection, intent.id)
        receipts.completePending(connection, intent.id, SafeResult(intent.id), code)
        ArtworkRpc.enqueueRelease(connection, intent.id, intent.id, claim.correlationId)
        finish(connection, claim)
    }

    private fun read(connection: Connection, id: UUID, locked: Boolean): ArtworkIntent = connection.queryBound(
        "SELECT * FROM artwork_attachment_request WHERE id=?" + if (locked) " FOR UPDATE" else "", listOf(id),
    ) { row -> ArtworkIntent(id, row.getObject("title_id", UUID::class.java), row.getObject("actor_id", UUID::class.java),
        row.getObject("asset_id", UUID::class.java), row.getLong("expected_version"), row.getString("policy_revision"),
        row.getString("state"), row.getObject("old_hold_id", UUID::class.java)) }.single()

    private fun lockReceipt(connection: Connection, id: UUID) {
        connection.queryBound("SELECT id FROM operation_receipt WHERE id=? FOR UPDATE", listOf(id)) { 1 }.single()
    }

    private fun fence(connection: Connection, claim: WorkClaim) {
        check(connection.queryBound("SELECT id FROM durable_work WHERE id=? AND state='LEASED' AND lease_owner=? AND lease_epoch=? " +
            "AND lease_until>clock_timestamp() AND deadline_at>clock_timestamp() FOR UPDATE", listOf(claim.workId, claim.leaseOwner, claim.leaseEpoch)) { 1 }.size == 1) { "WORK_LEASE_LOST" }
    }

    private fun finish(connection: Connection, claim: WorkClaim) {
        check(WorkSql.completeAttempt(connection, claim, WorkAttemptCompletion(AttemptOutcome.SUCCEEDED, nextState = "DONE"), Instant.now())) { "WORK_LEASE_LOST" }
    }
}
