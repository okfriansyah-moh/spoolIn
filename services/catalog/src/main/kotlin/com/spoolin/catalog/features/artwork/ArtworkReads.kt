package com.spoolin.catalog.features.artwork

import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import spoolin.v1.Spoolin
import java.util.UUID
import javax.sql.DataSource

class ArtworkReads(private val dataSource: DataSource) {
    fun attachment(id: UUID): Spoolin.Attachment = dataSource.connection.use { connection ->
        connection.queryBound(
            "SELECT a.*,t.attachment_id AS selected_hold,t.version AS selected_version,t.id AS surviving_title,t.status AS title_status " +
                "FROM artwork_attachment_request a LEFT JOIN title t ON t.id=a.title_id WHERE a.id=?", listOf(id),
        ) { row ->
            val selected = row.getObject("selected_hold", UUID::class.java) == id
            val survives = row.getObject("surviving_title", UUID::class.java) != null
            if (!survives) throw Status.FAILED_PRECONDITION.withDescription("REFERENCE_VERSION_UNAVAILABLE").asRuntimeException()
            val state = when {
                row.getString("state") == "PREPARING" && row.getString("title_status") != "DELETED" -> "PREPARING"
                row.getString("state") == "ATTACHED" && selected && row.getString("title_status") != "DELETED" -> "ACTIVE"
                else -> "RELEASED"
            }
            Spoolin.Attachment.newBuilder().setHoldId(id.toString()).setState(state)
                .setAssetId(row.getObject("asset_id", UUID::class.java).toString())
                .setTitleId(row.getObject("title_id", UUID::class.java).toString())
                .setActorId(row.getObject("actor_id", UUID::class.java).toString())
                .setCatalogVersion(row.getLong("selected_version")).build()
        }.singleOrNull() ?: throw Status.NOT_FOUND.asRuntimeException()
    }

    fun access(request: Spoolin.ArtworkAccessRequest): Spoolin.ArtworkGrant {
        if (request.management && ActorRpc.current()?.let { Permissions.management(it.role) } != true) {
            throw Status.PERMISSION_DENIED.asRuntimeException()
        }
        val asset = parseId(request.assetId)
        return dataSource.connection.use { connection ->
            connection.queryBound(
                "SELECT id,attachment_id,version FROM title WHERE asset_id=? AND attachment_id IS NOT NULL " +
                    if (request.management) "ORDER BY id LIMIT 1" else "AND status='ACTIVE' ORDER BY id LIMIT 1",
                listOf(asset),
            ) { row -> Spoolin.ArtworkGrant.newBuilder().setAssetId(asset.toString())
                .setHoldId(row.getObject("attachment_id", UUID::class.java).toString())
                .setTitleId(row.getObject("id", UUID::class.java).toString()).setCatalogVersion(row.getLong("version")).build()
            }.singleOrNull() ?: throw Status.NOT_FOUND.asRuntimeException()
        }
    }

    private fun parseId(value: String): UUID = try {
        UUID.fromString(value).also { require(it.toString() == value) }
    } catch (_: IllegalArgumentException) { throw Status.INVALID_ARGUMENT.asRuntimeException() }
}
