package com.spoolin.catalog.seed

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.sql.Connection
import java.util.UUID

internal object SeedArtworkImport {
    private const val VERSION_INDEX = 3
    private val mapper = JsonMapper.builder().build()

    fun apply(
        connection: Connection,
        seed: SeedManifest,
        input: Path,
    ): Map<String, Any> {
        require(seed.publicPages)
        val media = mapper.readTree(input.toFile())
        require(media.path("manifestVersion").asText() == seed.version)
        require(media.path("snapshotSHA256").asText() == seed.root["snapshotSHA256"].asText())
        val assets = media.path("assets").toList().associateBy { it.path("titleId").asText() }
        require(assets.size == seed.records.size)
        val attachments =
            seed.records.map { record ->
                val asset = assets.getValue(record["stableUUID"].asText())
                val art = record["artwork"]
                listOf("assetId", "holdId", "width", "height").forEach { require(asset[it] == art[it]) }
                require(asset.path("inputHash").asText() == art["sha256"].asText())
                attach(connection, seed, record, asset)
            }
        return mapOf(
            "manifestVersion" to seed.version,
            "snapshotSHA256" to seed.root["snapshotSHA256"].asText(),
            "attachments" to attachments,
            "summary" to attachments.groupingBy { it.getValue("state") }.eachCount(),
        )
    }

    private fun attach(
        connection: Connection,
        seed: SeedManifest,
        record: JsonNode,
        media: JsonNode,
    ): Map<String, Any> {
        val title = UUID.fromString(record["stableUUID"].asText())
        val art = record["artwork"]
        val asset = UUID.fromString(art["assetId"].asText())
        val hold = UUID.fromString(art["holdId"].asText())
        val rows =
            connection.queryBound(
                "SELECT status,asset_id,attachment_id,version FROM title WHERE " +
                    "id=? FOR UPDATE",
                listOf(title),
            ) {
                listOf(
                    it.getString("status"),
                    it.getObject("asset_id"),
                    it.getObject("attachment_id"),
                    it.getLong("version"),
                )
            }
        val row = rows.singleOrNull()
        val state =
            when {
                row == null || row[0] != "ACTIVE" -> "SKIPPED"
                unavailable(media) -> "PRESERVED"
                row[1] == asset && row[2] == hold -> "ATTACHED"
                eligible(connection, title, row) -> {
                    insert(connection, seed, title, art, row[VERSION_INDEX] as Long)
                    "ATTACHED"
                }
                else -> "PRESERVED"
            }
        val version =
            connection
                .queryBound("SELECT version FROM title WHERE id=?", listOf(title)) { it.getLong("version") }
                .singleOrNull() ?: 0L
        return mapOf(
            "titleId" to title.toString(),
            "assetId" to asset.toString(),
            "holdId" to hold.toString(),
            "catalogVersion" to version,
            "state" to state,
        )
    }

    private fun unavailable(media: JsonNode): Boolean =
        !media.path("available").asBoolean(true) || media.path("referenceState").asText() == "RELEASED"

    private fun eligible(
        connection: Connection,
        title: UUID,
        row: List<Any?>,
    ): Boolean {
        if (row[1] != null || row[2] != null) return false
        val history =
            connection.queryBound(
                "SELECT id FROM artwork_attachment_request WHERE " +
                    "title_id=? LIMIT 1",
                listOf(title),
            ) { 1 }
        return history.isEmpty()
    }

    private fun insert(
        connection: Connection,
        seed: SeedManifest,
        title: UUID,
        art: JsonNode,
        version: Long,
    ) {
        val hold = UUID.fromString(art["holdId"].asText())
        val asset = UUID.fromString(art["assetId"].asText())
        connection.executeBound(
            "INSERT INTO operation_receipt(id,actor_scope,kind,request_hash,state,correlation_id,expires_at) " +
                "VALUES(?,'SERVICE_INITIALIZER','ARTWORK_ATTACH',?,'SUCCEEDED',?," +
                "CURRENT_TIMESTAMP+INTERVAL '100 years')",
            hold,
            seed.snapshotHash,
            hold,
        )
        connection.executeBound(
            "INSERT INTO artwork_attachment_request(id,title_id,actor_id,expected_version,asset_id," +
                "state,policy_revision) " +
                "VALUES(?,?,?,?,?,'ATTACHED','seed-public-pages-v1')",
            hold,
            title,
            seed.namespace,
            version,
            asset,
        )
        connection.executeBound(
            "UPDATE title SET asset_id=?,attachment_id=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
            asset,
            hold,
            title,
        )
        connection.executeBound(
            "INSERT INTO seed_artwork_attribution(title_id,asset_id,attribution) VALUES(?,?,?::jsonb)",
            title,
            asset,
            mapper.writeValueAsString(art),
        )
    }
}
