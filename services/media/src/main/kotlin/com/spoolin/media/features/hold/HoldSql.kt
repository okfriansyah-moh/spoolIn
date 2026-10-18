package com.spoolin.media.features.hold

import com.spoolin.media.operations.events.ReceiptActivity
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import java.security.MessageDigest
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

internal data class HoldRecord(val id: UUID, val asset: UUID, val title: UUID, val state: String, val version: Long?)

object HoldSql {
    internal fun lock(connection: Connection, id: UUID): HoldRecord? = connection.queryBound(
        "SELECT * FROM asset_reference WHERE id=? FOR UPDATE", listOf(id),
    ) { HoldRecord(it.getObject("id", UUID::class.java), it.getObject("asset_id", UUID::class.java),
        it.getObject("title_id", UUID::class.java), it.getString("state"),
        it.getLong("catalog_version").takeUnless { _ -> it.wasNull() }) }.singleOrNull()

    internal fun reserveReceipt(connection: Connection, command: HoldCommand, now: Instant): Boolean {
        val inserted = connection.executeBound(
            "INSERT INTO operation_receipt " +
                "(id,actor_scope,kind,request_hash,state,correlation_id,created_at,expires_at) " +
                "VALUES (?,?,?,?,'PENDING',?,?,?) ON CONFLICT(id) DO NOTHING",
            command.operation, command.scope, command.action.kind, command.hash(), command.correlation,
            Timestamp.from(now), Timestamp.from(now.plusSeconds(604800)),
        ) == 1
        val matches = connection.queryBound(
            "SELECT actor_scope,kind,request_hash,state,result,expires_at FROM operation_receipt WHERE id=? FOR UPDATE",
            listOf(command.operation),
        ) { row ->
            if (row.getString("actor_scope") != command.scope) throw Status.NOT_FOUND.asRuntimeException()
            val compatible = row.getString("kind") == command.action.kind &&
                MessageDigest.isEqual(row.getBytes("request_hash"), command.hash())
            if (!compatible) throw Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException()
            if (!inserted && (row.getString("state") != "SUCCEEDED" || row.getString("result") == null ||
                !row.getTimestamp("expires_at").toInstant().isAfter(now))) {
                throw Status.FAILED_PRECONDITION.withDescription("HOLD_RECEIPT_UNAVAILABLE_OR_EXPIRED").asRuntimeException()
            }
            true
        }.single()
        if (!matches) throw Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException()
        return inserted
    }

    internal fun completeReceipt(connection: Connection, command: HoldCommand, hold: HoldRecord) {
        check(connection.executeBound(
            "UPDATE operation_receipt SET state='SUCCEEDED',result=?::jsonb WHERE id=? AND state='PENDING'",
            SafeResult(hold.id, hold.version).json(), command.operation,
        ) == 1)
        ReceiptActivity.append(connection, command.operation, command.correlation, hold.id)
    }

    fun insertReserved(connection: Connection, holdId: UUID, assetId: UUID, titleId: UUID): Int = connection.executeBound(
        "INSERT INTO asset_reference (id,asset_id,title_id,state,updated_at) VALUES (?,?,?,'RESERVED',CURRENT_TIMESTAMP)",
        holdId, assetId, titleId,
    )

    fun confirmActive(connection: Connection, holdId: UUID, catalogVersion: Long): Int = connection.executeBound(
        "UPDATE asset_reference SET state='ACTIVE',catalog_version=?,updated_at=CURRENT_TIMESTAMP " +
            "WHERE id=? AND state='RESERVED' AND (catalog_version IS NULL OR catalog_version<=?)",
        catalogVersion, holdId, catalogVersion,
    )

    fun release(connection: Connection, holdId: UUID, catalogVersion: Long): Int = connection.executeBound(
        "UPDATE asset_reference SET state='RELEASED',catalog_version=?,updated_at=CURRENT_TIMESTAMP " +
            "WHERE id=? AND state IN ('RESERVED','ACTIVE') AND (catalog_version IS NULL OR catalog_version<=?)",
        catalogVersion, holdId, catalogVersion,
    )

    fun countProtectiveHolds(connection: Connection, assetId: UUID): Int = connection.queryBound(
        "SELECT COUNT(*) FROM asset_reference WHERE asset_id=? AND state IN ('RESERVED','ACTIVE')", listOf(assetId),
    ) { it.getInt(1) }.single()
}
