package com.spoolin.catalog.features.provenance

import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.json.JsonMapper
import java.sql.Connection
import java.time.Instant
import java.util.UUID

internal object SeedArtworkSources {
    private val mapper = JsonMapper.builder().build()

    fun read(
        connection: Connection,
        ids: List<UUID>,
    ): List<Pair<UUID, ProvenanceReads.ProvenanceRow>> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        return connection.queryBound(
            "SELECT a.title_id,a.asset_id,a.attribution FROM seed_artwork_attribution a " +
                "JOIN title t ON t.id=a.title_id AND t.asset_id=a.asset_id " +
                "WHERE a.title_id IN ($placeholders) AND t.attachment_id IS NOT NULL AND t.status<>'DELETED'",
            ids,
        ) { row ->
            val art = mapper.readTree(row.getString("attribution"))
            row.getObject("title_id", UUID::class.java) to
                ProvenanceReads.ProvenanceRow(
                    "poster",
                    row.getObject("asset_id", UUID::class.java).toString(),
                    art["sourceUrl"].asText(),
                    Instant.parse(art["retrievedAt"].asText()),
                    "Poster source: ${art["sourceName"].asText()}",
                    mapper.writeValueAsString(
                        mapOf(
                            "artwork" to
                                mapOf(
                                    "url" to art["sourceUrl"].asText(),
                                    "retrievedAt" to art["retrievedAt"].asText(),
                                    "reuseBasis" to "Public poster source recorded; no open-license claim.",
                                ),
                        ),
                    ),
                    emptyList(),
                )
        }
    }
}
