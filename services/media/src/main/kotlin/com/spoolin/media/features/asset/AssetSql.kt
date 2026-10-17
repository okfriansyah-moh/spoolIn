package com.spoolin.media.features.asset

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Owner-only SQL operations for the `asset` table (§6.2.6, §7.6, ATDD-248, ATDD-250).
 *
 * All writes use explicit parameter binding. No user-supplied text reaches SQL identifiers
 * or unparameterized positions (§8.3.2). State transitions are atomic; invalid transitions
 * are refused at the row predicate rather than relying solely on application logic.
 */
object AssetSql {
    /**
     * Insert a new asset in RECEIVING state with the exact received byte count.
     * Called inside the upload RPC transaction, before acknowledging byte acceptance.
     */
    fun insertReceiving(
        connection: Connection,
        id: UUID,
        uploadOperationId: UUID,
        actorId: UUID,
        inputBytes: Long,
        policyRevision: String,
    ): Int =
        connection.executeBound(
            "INSERT INTO asset (id, upload_operation_id, actor_id, state, input_bytes, policy_revision, updated_at)" +
                " VALUES (?, ?, ?, 'RECEIVING', ?, ?, CURRENT_TIMESTAMP)",
            id,
            uploadOperationId,
            actorId,
            inputBytes,
            policyRevision,
        )

    /**
     * Atomically transition RECEIVING → PROCESSING with complete processing evidence.
     * Returns 1 when committed, 0 when the predicate did not match (idempotent guard).
     */
    fun acceptProcessing(
        connection: Connection,
        id: UUID,
        stagingKey: String,
        inputHash: ByteArray,
        workId: UUID,
        processingCreatedAt: Instant,
    ): Int =
        connection.executeBound(
            "UPDATE asset SET state = 'PROCESSING', processing_work_id = ?," +
                " processing_staging_key = ?, processing_input_hash = ?," +
                " processing_created_at = ?, updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND state = 'RECEIVING'",
            workId,
            stagingKey,
            inputHash,
            Timestamp.from(processingCreatedAt),
            id,
        )

    /**
     * Transition PROCESSING → READY with immutable output identity.
     * All READY columns required by DDL CHECK must be non-null.
     */
    fun markReady(
        connection: Connection,
        id: UUID,
        storageKey: String,
        contentHash: ByteArray,
        format: String,
        outputBytes: Long,
        width: Int,
        height: Int,
    ): Int =
        connection.executeBound(
            "UPDATE asset SET state = 'READY', storage_key = ?, content_hash = ?," +
                " format = ?, output_bytes = ?, width = ?, height = ?," +
                " updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND state = 'PROCESSING'",
            storageKey,
            contentHash,
            format,
            outputBytes,
            width,
            height,
            id,
        )

    /**
     * Transition RECEIVING or PROCESSING → REJECTED with a safe rejection code.
     * The error code must be a declared enum value, never free-form user text.
     */
    fun markRejected(
        connection: Connection,
        id: UUID,
        errorCode: String,
    ): Int =
        connection.executeBound(
            "UPDATE asset SET state = 'REJECTED', error_code = ?, updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND state IN ('RECEIVING', 'PROCESSING')",
            errorCode,
            id,
        )

    /**
     * Read a single asset row by primary key, with a FOR UPDATE lock when inside a transaction.
     */
    fun findLocked(connection: Connection, id: UUID): AssetRow? =
        connection
            .queryBound("SELECT * FROM asset WHERE id = ? FOR UPDATE", listOf(id)) { AssetRow.decode(it) }
            .firstOrNull()

    /** Read-only lookup for projection and status queries. */
    fun find(dataSource: DataSource, id: UUID): AssetRow? =
        dataSource.connection.use { connection ->
            connection.queryBound("SELECT * FROM asset WHERE id = ?", listOf(id)) { AssetRow.decode(it) }
                .firstOrNull()
        }

    /** Find all RECEIVING/PROCESSING assets older than the given threshold for orphan reconciliation. */
    fun findOrphans(
        dataSource: DataSource,
        olderThan: Instant,
        limit: Int = 50,
    ): List<AssetRow> =
        dataSource.connection.use { connection ->
            connection.queryBound(
                "SELECT * FROM asset WHERE state IN ('RECEIVING','PROCESSING')" +
                    " AND updated_at < ? ORDER BY updated_at, id LIMIT ?",
                listOf(Timestamp.from(olderThan), limit),
            ) { AssetRow.decode(it) }
        }
}
