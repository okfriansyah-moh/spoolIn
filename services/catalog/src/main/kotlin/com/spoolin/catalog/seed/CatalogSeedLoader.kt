package com.spoolin.catalog.seed

import com.spoolin.support.receipts.executeBound
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant
import java.util.HexFormat
import java.util.UUID
import javax.sql.DataSource

/** Loads catalog rows from manifest.json (movies + series) with seed_member identity (T032/T033). */
class CatalogSeedLoader(
    private val dataSource: DataSource,
) {
    private val mapper = JsonMapper.builder().build()

    fun loadFromManifest(
        manifest: Path,
        seedId: String,
        genreResolver: (String) -> UUID,
    ): Int {
        val root = mapper.readTree(manifest.toFile())
        var inserted = 0
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            for (section in listOf("movies", "series")) {
                val entries = root.get(section) ?: continue
                for (node in entries) {
                    inserted += insertEntry(connection, seedId, node, genreResolver)
                }
            }
            connection.commit()
        }
        return inserted
    }

    private fun insertEntry(
        connection: java.sql.Connection,
        seedId: String,
        node: tools.jackson.databind.JsonNode,
        genreResolver: (String) -> UUID,
    ): Int {
        val sourceId = node.get("sourceId").asText()
        if (memberExists(connection, seedId, sourceId)) return 0
        val titleId = UUID.randomUUID()
        val now = Timestamp.from(Instant.now())
        connection.executeBound(
            "INSERT INTO title (id,kind,status,display_title,synopsis,release_year,version," +
                "created_at,updated_at) VALUES (?,?,?,?,?,?,0,?,?)",
            titleId,
            node.get("kind").asText(),
            "ACTIVE",
            node.get("title").asText(),
            node.get("synopsis").asText(),
            node.get("releaseYear").asInt(),
            now,
            now,
        )
        for (label in node.get("genreLabels")) {
            val genreId = genreResolver(label.asText())
            connection.executeBound(
                "INSERT INTO title_genre (title_id, genre_id) VALUES (?, ?)",
                titleId,
                genreId,
            )
        }
        val contentHash =
            HexFormat.of().parseHex(node.get("contentHash").asText())
        connection.executeBound(
            "INSERT INTO seed_member (seed_id, member_key, subject_kind, subject_id, content_hash) " +
                "VALUES (?, ?, 'TITLE', ?, ?)",
            seedId,
            sourceId,
            titleId,
            contentHash,
        )
        return 1
    }

    private fun memberExists(
        connection: java.sql.Connection,
        seedId: String,
        memberKey: String,
    ): Boolean =
        connection
            .prepareStatement(
                "SELECT 1 FROM seed_member WHERE seed_id = ? AND member_key = ? AND removed_at IS NULL LIMIT 1",
            ).use { statement ->
                statement.setString(1, seedId)
                statement.setString(2, memberKey)
                statement.executeQuery().use { it.next() }
            }
}
