package com.spoolin.core.operations.receipts

import com.spoolin.core.operations.events.ReceiptActivity
import com.spoolin.support.receipts.Comparison
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.ReceiptHidden
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.ReceiptState
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.WorkRequest
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.util.UUID
import javax.sql.DataSource

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
        transaction { connection ->
            val inserted = reserve(connection, intent, correlationId, privateInput)
            val existing = locked(connection, intent.id)
            when (hashes.compare(intent, existing.actor, existing.kind, existing.hash, existing.keyId)) {
                Comparison.HIDDEN -> throw ReceiptHidden()
                Comparison.CONFLICT -> throw ReceiptConflict()
                Comparison.REPLAY -> Unit
            }
            if (!inserted || existing.outcome.state != ReceiptState.PENDING) return@transaction existing.outcome
            val result = mutation(connection)
            complete(connection, intent.id, result)
            ReceiptActivity.append(connection, intent.id, correlationId, result.resultId)
            work.forEach { ReceiptWork.enqueue(connection, intent.id, correlationId, it, clock.instant()) }
            locked(connection, intent.id).outcome
        }

    fun find(
        id: UUID,
        actorScope: String,
    ): ReceiptOutcome? =
        dataSource.connection.use { connection ->
            connection
                .prepareStatement(
                    "SELECT * FROM operation_receipt WHERE id=? AND actor_scope=? AND receipt_type='REQUEST'",
                ).use { statement ->
                    statement.setObject(1, id)
                    statement.setString(2, actorScope)
                    statement.executeQuery().use { rows -> if (rows.next()) decode(rows).outcome else null }
                }
        }

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
        return connection.prepareStatement(sql).use { statement ->
            statement.setObject(1, intent.id)
            statement.setString(2, intent.actorScope)
            statement.setString(3, intent.kind)
            statement.setBytes(4, hashes.hash(intent, keyId))
            statement.setString(5, keyId)
            statement.setObject(6, correlation)
            statement.setTimestamp(7, Timestamp.from(clock.instant()))
            statement.setTimestamp(8, Timestamp.from(clock.instant().plus(Duration.ofDays(7))))
            statement.executeUpdate() == 1
        }
    }

    private fun complete(
        connection: Connection,
        id: UUID,
        result: SafeResult,
    ) {
        connection
            .prepareStatement(
                "UPDATE operation_receipt SET state='SUCCEEDED',result=?::jsonb WHERE id=? AND state='PENDING'",
            ).use { statement ->
                statement.setString(1, result.json())
                statement.setObject(2, id)
                check(statement.executeUpdate() == 1) { "Operation completion failed" }
            }
    }

    private fun locked(
        connection: Connection,
        id: UUID,
    ): StoredReceipt =
        connection.prepareStatement("SELECT * FROM operation_receipt WHERE id=? FOR UPDATE").use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { rows ->
                check(rows.next()) { "Operation receipt is unavailable" }
                if (rows.getString("receipt_type") != "REQUEST") throw ReceiptHidden()
                decode(rows)
            }
        }

    private fun decode(rows: ResultSet): StoredReceipt {
        val state = ReceiptState.valueOf(rows.getString("state"))
        val expiry = rows.getTimestamp("expires_at").toInstant()
        val expired = state != ReceiptState.PENDING && !clock.instant().isBefore(expiry)
        val outcome =
            ReceiptOutcome(
                rows.getObject("id", UUID::class.java),
                state,
                if (expired) null else rows.getString("result"),
                if (expired) "RESULT_EXPIRED" else rows.getString("error_code"),
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

    private fun <T> transaction(action: (Connection) -> T): T =
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val result = action(connection)
                connection.commit()
                result
            } catch (failure: Throwable) {
                connection.rollback()
                throw failure
            }
        }
}

private data class StoredReceipt(
    val actor: String,
    val kind: String,
    val hash: ByteArray,
    val keyId: String?,
    val outcome: ReceiptOutcome,
)
