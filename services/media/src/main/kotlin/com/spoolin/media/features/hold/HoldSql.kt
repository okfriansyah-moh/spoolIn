package com.spoolin.media.features.hold

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.util.UUID

object HoldSql {
    fun insertReserved(
        connection: Connection,
        holdId: UUID,
        assetId: UUID,
        titleId: UUID,
    ): Int =
        connection.executeBound(
            "INSERT INTO asset_reference (id, asset_id, title_id, state, updated_at)" +
                " VALUES (?, ?, ?, 'RESERVED', CURRENT_TIMESTAMP)",
            holdId,
            assetId,
            titleId,
        )

    fun confirmActive(
        connection: Connection,
        holdId: UUID,
        catalogVersion: Long,
    ): Int =
        connection.executeBound(
            "UPDATE asset_reference SET state = 'ACTIVE', catalog_version = ?, updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND state = 'RESERVED'",
            catalogVersion,
            holdId,
        )

    fun release(
        connection: Connection,
        holdId: UUID,
    ): Int =
        connection.executeBound(
            "UPDATE asset_reference SET state = 'RELEASED', updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND state IN ('RESERVED','ACTIVE')",
            holdId,
        )

    fun countProtectiveHolds(
        connection: Connection,
        assetId: UUID,
    ): Int =
        connection
            .queryBound(
                "SELECT COUNT(*) FROM asset_reference WHERE asset_id = ? AND state IN ('RESERVED','ACTIVE')",
                listOf(assetId),
            ) { it.getInt(1) }
            .first()
}
