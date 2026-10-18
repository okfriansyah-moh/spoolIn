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

private const val CONFIRM_DEADLINE_SECONDS = 300L

data class ArtworkIntent(
    val id: UUID,
    val title: UUID,
    val actor: UUID,
    val asset: UUID,
    val expectedVersion: Long,
    val policy: String,
    val state: String,
    val oldHold: UUID?,
)

class ArtworkWorkStore(
    private val dataSource: DataSource,
    private val receipts: OwnerReceipts,
    private val policy: () -> String,
) {
    fun read(id: UUID): ArtworkIntent = dataSource.connection.use { connection -> readIntent(connection, id, false) }

    fun attach(
        claim: WorkClaim,
        intent: ArtworkIntent,
    ) = dataSource.committed { connection ->
        ArtworkRpc.lock(connection)
        lockReceipt(connection, intent.id)
        fence(connection, claim)
        val current = readIntent(connection, intent.id, true)
        if (current.state != "PREPARING") {
            finish(connection, claim)
            return@committed
        }
        val title =
            connection
                .queryBound(
                    "SELECT version,status,attachment_id FROM title WHERE id=? FOR UPDATE",
                    listOf(current.title),
                ) { it.getLong("version") to it.getString("status") }
                .singleOrNull()
        val versionMatches = title != null && title.first == current.expectedVersion && title.second == "ACTIVE"
        if (!versionMatches || current.policy != policy()) {
            abort(connection, current, claim, "STALE_VERSION")
            return@committed
        }
        attachCurrent(connection, current, claim)
    }

    private fun attachCurrent(
        connection: Connection,
        current: ArtworkIntent,
        claim: WorkClaim,
    ) {
        val version = current.expectedVersion + 1
        check(
            connection.executeBound(
                "UPDATE title SET asset_id=?,attachment_id=?,version=?,updated_at=CURRENT_TIMESTAMP " +
                    "WHERE id=? AND version=?",
                current.asset,
                current.id,
                version,
                current.title,
                current.expectedVersion,
            ) == 1,
        )
        check(ArtworkAttachmentSql.markAttached(connection, current.id) == 1)
        AuditInsert.record(
            connection,
            current.id,
            current.actor,
            "HUMAN",
            "ATTACH_ARTWORK",
            "TITLE",
            current.title,
            "SUCCEEDED",
            "{\"version\":{\"before\":${current.expectedVersion},\"after\":$version}}",
            claim.correlationId,
            Instant.now(),
        )
        receipts.completePending(connection, current.id, SafeResult(current.title, version))
        val confirm =
            WorkRequest(
                "confirm",
                "ARTWORK_CONFIRM",
                current.id,
                Instant.now().plusSeconds(CONFIRM_DEADLINE_SECONDS),
            )
        ReceiptWork.enqueue(connection, current.id, claim.correlationId, confirm, Instant.now())
        current.oldHold?.let { ArtworkRpc.enqueueRelease(connection, current.id, it, claim.correlationId) }
        finish(connection, claim)
    }

    fun abort(
        claim: WorkClaim,
        intent: ArtworkIntent,
        code: String,
    ) = dataSource.committed { connection ->
        ArtworkRpc.lock(connection)
        lockReceipt(connection, intent.id)
        fence(connection, claim)
        val current = readIntent(connection, intent.id, true)
        if (current.state == "PREPARING") abort(connection, current, claim, code) else finish(connection, claim)
    }

    fun reconcileExhausted() {
        val ids =
            dataSource.connection.use { connection ->
                connection.queryBound(
                    "SELECT a.id FROM artwork_attachment_request a JOIN durable_work w ON w.operation_id=a.id " +
                        "WHERE a.state='PREPARING' AND w.kind='ARTWORK_ATTACH' AND w.state='EXHAUSTED' " +
                        "ORDER BY a.id LIMIT 10",
                    emptyList(),
                ) { it.getObject("id", UUID::class.java) }
            }
        ids.forEach(::reconcileExhausted)
    }

    private fun reconcileExhausted(id: UUID) =
        dataSource.committed { connection ->
            ArtworkRpc.lock(connection)
            lockReceipt(connection, id)
            val exhausted =
                connection
                    .queryBound(
                        "SELECT correlation_id FROM durable_work WHERE operation_id=? AND kind='ARTWORK_ATTACH' " +
                            "AND state='EXHAUSTED' FOR UPDATE",
                        listOf(id),
                    ) { it.getObject("correlation_id", UUID::class.java) }
                    .singleOrNull()
            val current = readIntent(connection, id, true)
            if (current.state == "PREPARING" && exhausted != null) {
                ArtworkAttachmentSql.markAborted(connection, id)
                receipts.completePending(connection, id, SafeResult(id), "ARTWORK_RETRY_EXHAUSTED")
                ArtworkRpc.enqueueRelease(connection, id, id, exhausted)
            }
        }

    private fun abort(
        connection: Connection,
        intent: ArtworkIntent,
        claim: WorkClaim,
        code: String,
    ) {
        ArtworkAttachmentSql.markAborted(connection, intent.id)
        receipts.completePending(connection, intent.id, SafeResult(intent.id), code)
        ArtworkRpc.enqueueRelease(connection, intent.id, intent.id, claim.correlationId)
        finish(connection, claim)
    }
}

private fun readIntent(
    connection: Connection,
    id: UUID,
    locked: Boolean,
): ArtworkIntent =
    connection
        .queryBound(
            "SELECT * FROM artwork_attachment_request WHERE id=?" + if (locked) " FOR UPDATE" else "",
            listOf(id),
        ) { row ->
            ArtworkIntent(
                id,
                row.getObject("title_id", UUID::class.java),
                row.getObject("actor_id", UUID::class.java),
                row.getObject("asset_id", UUID::class.java),
                row.getLong("expected_version"),
                row.getString("policy_revision"),
                row.getString("state"),
                row.getObject("old_hold_id", UUID::class.java),
            )
        }.single()

private fun lockReceipt(
    connection: Connection,
    id: UUID,
) {
    connection.queryBound("SELECT id FROM operation_receipt WHERE id=? FOR UPDATE", listOf(id)) { 1 }.single()
}

private fun fence(
    connection: Connection,
    claim: WorkClaim,
) {
    val owned =
        connection.queryBound(
            "SELECT id FROM durable_work WHERE id=? AND state='LEASED' AND lease_owner=? AND lease_epoch=? " +
                "AND lease_until>clock_timestamp() AND deadline_at>clock_timestamp() FOR UPDATE",
            listOf(claim.workId, claim.leaseOwner, claim.leaseEpoch),
        ) { 1 }
    check(owned.size == 1) { "WORK_LEASE_LOST" }
}

private fun finish(
    connection: Connection,
    claim: WorkClaim,
) {
    val completion = WorkAttemptCompletion(AttemptOutcome.SUCCEEDED, nextState = "DONE")
    check(WorkSql.completeAttempt(connection, claim, completion, Instant.now())) { "WORK_LEASE_LOST" }
}
