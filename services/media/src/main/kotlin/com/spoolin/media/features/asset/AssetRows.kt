package com.spoolin.media.features.asset

import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

/**
 * Immutable database row for the `asset` table (§6.2.6 / ATDD-248).
 * Processing-evidence columns are all-or-none (enforced by DDL CHECK).
 * Internal columns (processing_*, storage_key, content_hash, actor_id)
 * are never exposed via GraphQL or proto (TD-ROW-L5020 to L5035).
 */
data class AssetRow(
    val id: UUID,
    val uploadOperationId: UUID,
    val actorId: UUID,
    val state: AssetState,
    val storageKey: String?,
    val contentHash: ByteArray?,
    val format: String?,
    val inputBytes: Long,
    val outputBytes: Long?,
    val width: Int?,
    val height: Int?,
    val policyRevision: String,
    val provenance: String,
    val errorCode: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    // Processing evidence block — null when no job accepted (ATDD-250-T1-R01..R04)
    val processingWorkId: UUID?,
    val processingStagingKey: String?,
    val processingInputHash: ByteArray?,
    val processingCreatedAt: Instant?,
) {
    /** True when the all-or-none processing evidence block is present. */
    val hasProcessingEvidence: Boolean get() = processingWorkId != null

    companion object {
        fun decode(rs: ResultSet): AssetRow {
            val pWorkId = rs.getObject("processing_work_id", UUID::class.java)
            return AssetRow(
                id = rs.getObject("id", UUID::class.java),
                uploadOperationId = rs.getObject("upload_operation_id", UUID::class.java),
                actorId = rs.getObject("actor_id", UUID::class.java),
                state = AssetState.valueOf(rs.getString("state")),
                storageKey = rs.getString("storage_key"),
                contentHash = rs.getBytes("content_hash"),
                format = rs.getString("format"),
                inputBytes = rs.getLong("input_bytes"),
                outputBytes = rs.getLong("output_bytes").takeIf { !rs.wasNull() },
                width = rs.getInt("width").takeIf { !rs.wasNull() },
                height = rs.getInt("height").takeIf { !rs.wasNull() },
                policyRevision = rs.getString("policy_revision"),
                provenance = rs.getString("provenance"),
                errorCode = rs.getString("error_code"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                updatedAt = rs.getTimestamp("updated_at").toInstant(),
                processingWorkId = pWorkId,
                processingStagingKey = rs.getString("processing_staging_key"),
                processingInputHash = rs.getBytes("processing_input_hash"),
                processingCreatedAt = rs.getTimestamp("processing_created_at")?.toInstant(),
            )
        }
    }
}
