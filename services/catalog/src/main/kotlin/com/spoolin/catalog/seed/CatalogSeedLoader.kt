package com.spoolin.catalog.seed

import com.spoolin.support.receipts.executeBound
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/** Loads catalog rows from manifest.json (source-complete; runtime load optional). */
class CatalogSeedLoader(
    private val dataSource: DataSource,
) {
    private val mapper = JsonMapper.builder().build()

    fun loadFromManifest(manifest: Path, genreResolver: (String) -> UUID): Int {
        val root = mapper.readTree(manifest.toFile())
        val movies = root.get("movies") ?: return 0
        var inserted = 0
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            for (node in movies) {
                val sourceId = node.get("sourceId").asText()
                if (existsSource(connection, sourceId)) continue
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
                inserted += 1
            }
            connection.commit()
        }
        return inserted
    }

    private fun existsSource(
        connection: java.sql.Connection,
        sourceId: String,
    ): Boolean =
        connection
            .prepareStatement("SELECT 1 FROM title WHERE display_title = ? LIMIT 1")
            .use { statement ->
                statement.setString(1, sourceId)
                statement.executeQuery().use { it.next() }
            }
}
