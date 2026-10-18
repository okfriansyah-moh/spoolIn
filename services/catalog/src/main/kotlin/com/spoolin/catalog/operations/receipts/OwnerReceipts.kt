package com.spoolin.catalog.operations.receipts

import com.spoolin.catalog.operations.events.ReceiptActivity
import com.spoolin.support.receipts.Comparison
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.ReceiptHidden
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.ReceiptState
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.util.UUID
import javax.sql.DataSource

private const val RECEIPT_DAYS = 7L

class OwnerReceipts(
    private val dataSource: DataSource,
    private val hashes: IntentHashes,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(
        intent: OperationIntent,
        correlationId: UUID,
        privateInput: Boolean,
        work: List<WorkRequest> = emptyList(),
        mutation: (Connection) -> SafeResult,
    ): ReceiptOutcome =
        dataSource.committed { connection ->
            connection.queryBound("SELECT pg_advisory_xact_lock(41001,1)", emptyList()) { 1 }
            val inserted = reserve(connection, intent, correlationId, privateInput)
            val existing = locked(connection, intent.id)
            when (hashes.compare(intent, existing.actor, existing.kind, existing.hash, existing.keyId)) {
                Comparison.HIDDEN -> throw ReceiptHidden()
                Comparison.CONFLICT -> throw ReceiptConflict()
                Comparison.REPLAY -> Unit
            }
            if (!inserted || existing.outcome.state != ReceiptState.PENDING) return@committed existing.outcome
            val result = mutation(connection)
            complete(connection, intent.id, result)
            ReceiptActivity.append(connection, intent.id, correlationId, result.resultId)
            work.forEach { ReceiptWork.enqueue(connection, intent.id, correlationId, it, clock.instant()) }
            locked(connection, intent.id).outcome
        }

    fun defer(
        intent: OperationIntent,
        correlationId: UUID,
        privateInput: Boolean,
        work: List<WorkRequest> = emptyList(),
        beforeReserve: (Connection) -> Unit = {},
        prepare: (Connection) -> Unit,
    ): ReceiptOutcome =
        dataSource.committed { connection ->
            beforeReserve(connection)
            val inserted = reserve(connection, intent, correlationId, privateInput)
            val existing = locked(connection, intent.id)
            when (hashes.compare(intent, existing.actor, existing.kind, existing.hash, existing.keyId)) {
                Comparison.HIDDEN -> throw ReceiptHidden()
                Comparison.CONFLICT -> throw ReceiptConflict()
                Comparison.REPLAY -> Unit
            }
            if (!inserted || existing.outcome.state != ReceiptState.PENDING) return@committed existing.outcome
            prepare(connection)
            work.forEach { ReceiptWork.enqueue(connection, intent.id, correlationId, it, clock.instant()) }
            locked(connection, intent.id).outcome
        }

    fun completePending(
        connection: Connection,
        id: UUID,
        result: SafeResult,
        errorCode: String? = null,
    ) {
        val updated =
            connection.executeBound(
                "UPDATE operation_receipt SET state=?,result=?::jsonb,error_code=? WHERE id=? AND state='PENDING'",
                if (errorCode == null) "SUCCEEDED" else "FAILED",
                if (errorCode == null) result.json() else null,
                errorCode,
                id,
            )
        check(updated == 1) { "Operation completion failed" }
        if (errorCode == null) {
            val correlation =
                connection
                    .queryBound(
                        "SELECT correlation_id FROM operation_receipt WHERE id=?",
                        listOf(id),
                    ) { it.getObject("correlation_id", UUID::class.java) }
                    .single()
            ReceiptActivity.append(connection, id, correlation, result.resultId)
        }
    }

    fun find(
        id: UUID,
        actorScope: String,
    ): ReceiptOutcome? = OwnerReceiptReads(dataSource, clock).find(id, actorScope)

    private fun reserve(
        connection: Connection,
        intent: OperationIntent,
        correlation: UUID,
        privateInput: Boolean,
    ): Boolean {
        val keyId = if (privateInput) hashes.active else null
        val sql =
            "INSERT INTO operation_receipt " +
                "(id,actor_scope,kind,request_hash,hash_key_id,state,correlation_id,created_at,expires_at) " +
                "VALUES (?,?,?,?,?,'PENDING',?,?,?) ON CONFLICT (id) DO NOTHING"
        return connection.executeBound(
            sql,
            intent.id,
            intent.actorScope,
            intent.kind,
            hashes.hash(intent, keyId),
            keyId,
            correlation,
            Timestamp.from(clock.instant()),
            Timestamp.from(clock.instant().plus(Duration.ofDays(RECEIPT_DAYS))),
        ) == 1
    }

    private fun complete(
        connection: Connection,
        id: UUID,
        result: SafeResult,
    ) {
        val updated =
            connection.executeBound(
                "UPDATE operation_receipt SET state='SUCCEEDED',result=?::jsonb WHERE id=? AND state='PENDING'",
                result.json(),
                id,
            )
        check(updated == 1) { "Operation completion failed" }
    }

    private fun locked(
        connection: Connection,
        id: UUID,
    ): StoredReceipt =
        connection
            .queryBound("SELECT * FROM operation_receipt WHERE id=? FOR UPDATE", listOf(id)) { rows ->
                if (rows.getString("receipt_type") != "REQUEST") throw ReceiptHidden()
                decode(rows)
            }.single()

    private fun decode(rows: ResultSet): StoredReceipt = ReceiptRows.decode(rows, clock.instant())
}

internal data class StoredReceipt(
    val actor: String,
    val kind: String,
    val hash: ByteArray,
    val keyId: String?,
    val outcome: ReceiptOutcome,
)

internal object ReceiptRows {
    fun decode(
        rows: ResultSet,
        now: java.time.Instant,
    ): StoredReceipt {
        val state = ReceiptState.valueOf(rows.getString("state"))
        val expiry = rows.getTimestamp("expires_at").toInstant()
        val expired = state != ReceiptState.PENDING && !now.isBefore(expiry)
        val outcome =
            ReceiptOutcome(
                rows.getObject("id", UUID::class.java),
                state,
                if (expired) null else SafeReceiptProjection.read(rows.getString("result")),
                if (expired) "RESULT_EXPIRED" else SafeReceiptProjection.errorCode(rows.getString("error_code")),
                rows.getObject("correlation_id", UUID::class.java),
                rows.getTimestamp("created_at").toInstant(),
                expired,
            )
        return StoredReceipt(
            rows.getString("actor_scope"),
            rows.getString("kind"),
            rows.getBytes("request_hash"),
            rows.getString("hash_key_id"),
            outcome,
        )
    }
}
