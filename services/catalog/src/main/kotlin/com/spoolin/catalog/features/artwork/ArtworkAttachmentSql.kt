@file:Suppress("LongParameterList", "MaxLineLength")

package com.spoolin.catalog.features.artwork

import com.spoolin.support.receipts.executeBound
import java.sql.Connection
import java.util.UUID

object ArtworkAttachmentSql {
    fun insertPreparing(
        connection: Connection,
        id: UUID,
        titleId: UUID,
        actorId: UUID,
        expectedVersion: Long,
        assetId: UUID,
        policyRevision: String,
        oldHoldId: UUID?,
    ): Int =
        connection.executeBound(
            "INSERT INTO artwork_attachment_request" +
                " (id, title_id, actor_id, expected_version, asset_id, old_hold_id, state, policy_revision, updated_at)" +
                " VALUES (?, ?, ?, ?, ?, ?, 'PREPARING', ?, CURRENT_TIMESTAMP)",
            id,
            titleId,
            actorId,
            expectedVersion,
            assetId,
            oldHoldId,
            policyRevision,
        )

    fun markAttached(
        connection: Connection,
        id: UUID,
    ): Int =
        connection.executeBound(
            "UPDATE artwork_attachment_request SET state = 'ATTACHED', updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND state = 'PREPARING'",
            id,
        )

    fun markAborted(
        connection: Connection,
        id: UUID,
    ): Int =
        connection.executeBound(
            "UPDATE artwork_attachment_request SET state = 'ABORTED', updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND state = 'PREPARING'",
            id,
        )
}
