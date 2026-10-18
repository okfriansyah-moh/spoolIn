package com.spoolin.media.features.asset

import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import spoolin.v1.Spoolin.Attachment
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

internal data class AssetHold(
    val id: UUID,
    val asset: UUID,
    val title: UUID,
    val state: String,
)

/** Catalog projection and local hold agree before any byte or service-scoped metadata is exposed. */
class AssetAccess(
    private val dataSource: DataSource,
    private val catalog: CatalogAttachments,
) {
    fun current(hold: UUID): Attachment = catalog.find(hold)

    internal fun hold(
        connection: Connection,
        id: UUID,
    ): AssetHold? =
        connection
            .queryBound(
                "SELECT id,asset_id,title_id,state FROM asset_reference WHERE id=? FOR UPDATE",
                listOf(id),
            ) {
                AssetHold(
                    it.getObject("id", UUID::class.java),
                    it.getObject("asset_id", UUID::class.java),
                    it.getObject("title_id", UUID::class.java),
                    it.getString("state"),
                )
            }.singleOrNull()

    internal fun checkRead(
        local: AssetHold?,
        remote: Attachment,
        asset: UUID,
        hold: UUID,
        version: Long,
    ) {
        val active = local?.state in setOf("RESERVED", "ACTIVE") && remote.state == "ACTIVE"
        val current = remote.hasCatalogVersion() && remote.catalogVersion == version && version > 0
        if (!sameReadTuple(local, remote, asset, hold) || !active || !current) {
            throw Status.NOT_FOUND.asRuntimeException()
        }
    }

    private fun sameReadTuple(
        local: AssetHold?,
        remote: Attachment,
        asset: UUID,
        hold: UUID,
    ): Boolean =
        local != null && local.id == hold && local.asset == asset && remote.holdId == hold.toString() &&
            remote.assetId == asset.toString() && remote.titleId == local.title.toString()

    fun catalogMetadata(asset: AssetRow): Boolean {
        val intentId =
            dataSource.connection.use { connection ->
                connection
                    .queryBound(
                        "SELECT payload->>'intentId' AS intent FROM durable_work WHERE id=? AND kind='MEDIA_PROCESS'",
                        listOf(asset.processingWorkId),
                    ) { it.getString("intent") }
                    .singleOrNull()
            }
        return if (intentId != null) intentMetadata(intentId, asset) else holdMetadata(asset)
    }

    private fun intentMetadata(
        intentId: String,
        asset: AssetRow,
    ): Boolean {
        val intent = runCatching { UUID.fromString(intentId) }.getOrNull() ?: return false
        val remote = catalog.find(intent)
        return remote.holdId == intent.toString() && remote.assetId == asset.id.toString() &&
            remote.hasActorId() && remote.actorId == asset.actorId.toString() &&
            remote.state in setOf("PREPARING", "ACTIVE", "RELEASED")
    }

    private fun holdMetadata(asset: AssetRow): Boolean {
        val holds =
            dataSource.connection.use { connection ->
                connection.queryBound(
                    "SELECT id,asset_id,title_id,state FROM asset_reference WHERE asset_id=? ORDER BY id LIMIT 10",
                    listOf(asset.id),
                ) {
                    AssetHold(
                        it.getObject("id", UUID::class.java),
                        it.getObject("asset_id", UUID::class.java),
                        it.getObject("title_id", UUID::class.java),
                        it.getString("state"),
                    )
                }
            }
        return holds.any { local ->
            val remote = catalog.find(local.id)
            remote.holdId == local.id.toString() && remote.assetId == asset.id.toString() &&
                remote.titleId == local.title.toString() && remote.state in setOf("PREPARING", "ACTIVE", "RELEASED")
        }
    }
}
