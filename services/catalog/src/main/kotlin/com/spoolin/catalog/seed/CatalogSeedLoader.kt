package com.spoolin.catalog.seed

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Instant
import java.util.HexFormat
import java.util.UUID
import javax.sql.DataSource
import kotlin.io.path.readBytes

/** Loads catalog rows from manifest.json (movies + series) with seed_member identity (T032/T033). */
class CatalogSeedLoader(
    private val dataSource: DataSource,
) {
    private val mapper = JsonMapper.builder().build()

    fun loadFromManifest(
        manifest: Path,
        seedVersion: String,
    ): Int {
        if (initializationSucceeded(seedVersion)) return 0
        val root = mapper.readTree(manifest.toFile())
        val snapshotHash = snapshotDigest(manifest)
        var inserted = 0
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            ensurePendingInitializationReceipt(connection, seedVersion, snapshotHash)
            for (section in listOf("movies", "series")) {
                val entries = root.get(section) ?: continue
                for (node in entries) {
                    inserted += insertEntry(connection, seedVersion, node)
                }
            }
            finalizeInitializationReceipt(connection, seedVersion)
            connection.commit()
        }
        return inserted
    }

    private fun initializationSucceeded(seedVersion: String): Boolean =
        dataSource.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT 1 FROM operation_receipt WHERE seed_version = ? AND receipt_type = 'CATALOG_INITIALIZATION' " +
                        "AND state = 'SUCCEEDED' LIMIT 1",
                    listOf(seedVersion),
                ) { 1 }
                .isNotEmpty()
        }

    private fun ensurePendingInitializationReceipt(
        connection: java.sql.Connection,
        seedVersion: String,
        snapshotHash: ByteArray,
    ) {
        val exists =
            connection
                .queryBound(
                    "SELECT 1 FROM operation_receipt WHERE seed_version = ? AND receipt_type = 'CATALOG_INITIALIZATION' LIMIT 1",
                    listOf(seedVersion),
                ) { 1 }
                .isNotEmpty()
        if (exists) return
        val receiptId = UUID.nameUUIDFromBytes("catalog-init:$seedVersion".toByteArray(Charsets.UTF_8))
        connection.executeBound(
            "INSERT INTO operation_receipt " +
                "(id, actor_scope, kind, receipt_type, request_hash, state, correlation_id, " +
                "seed_version, seed_snapshot_hash, seed_movie_count, seed_tv_count, seed_summary) " +
                "VALUES (?, 'SERVICE_INITIALIZER', 'INITIALIZE_CATALOG', 'CATALOG_INITIALIZATION', ?, 'PENDING', ?, ?, ?, 0, 0, '{}'::jsonb)",
            receiptId,
            snapshotHash,
            UUID.nameUUIDFromBytes("catalog-init-correlation:$seedVersion".toByteArray()),
            seedVersion,
            snapshotHash,
        )
    }

    private fun finalizeInitializationReceipt(
        connection: java.sql.Connection,
        seedVersion: String,
    ) {
        connection.executeBound(
            "UPDATE operation_receipt SET state = 'SUCCEEDED', seed_movie_count = 100, seed_tv_count = 100 " +
                "WHERE seed_version = ? AND receipt_type = 'CATALOG_INITIALIZATION' AND state = 'PENDING'",
            seedVersion,
        )
    }

    private fun insertEntry(
        connection: java.sql.Connection,
        seedId: String,
        node: tools.jackson.databind.JsonNode,
    ): Int {
        val sourceId = node.get("sourceId").asText()
        if (memberExists(connection, seedId, sourceId)) return 0
        val titleKind = titleKindFromManifest(node.get("kind").asText())
        val titleId = UUID.randomUUID()
        val now = Timestamp.from(Instant.now())
        val synopsisLanguage =
            node.get("synopsisLanguage")?.asText()?.takeIf { it.isNotBlank() } ?: DEFAULT_SYNOPSIS_LANGUAGE
        connection.executeBound(
            "INSERT INTO title (id,kind,status,display_title,synopsis,synopsis_language,release_year,version," +
                "created_at,updated_at) VALUES (?,?,?,?,?,?,?,0,?,?)",
            titleId,
            titleKind,
            "ACTIVE",
            node.get("title").asText(),
            node.get("synopsis").asText(),
            synopsisLanguage,
            node.get("releaseYear").asInt(),
            now,
            now,
        )
        for (label in node.get("genreLabels")) {
            val genreId = ensureGenre(connection, label.asText())
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

    private fun titleKindFromManifest(kind: String): String =
        when (kind.uppercase()) {
            "SERIES", "TV" -> "TV"
            "MOVIE" -> "MOVIE"
            else -> error("unsupported manifest title kind: $kind")
        }

    private fun ensureGenre(
        connection: java.sql.Connection,
        label: String,
    ): UUID {
        connection
            .queryBound("SELECT id FROM genre WHERE label = ?", listOf(label)) {
                it.getObject("id", UUID::class.java)
            }.singleOrNull()
            ?.let { return it }
        val id = UUID.nameUUIDFromBytes("genre:${label.lowercase()}".toByteArray(Charsets.UTF_8))
        val normalized = label.lowercase()
        connection.executeBound(
            "INSERT INTO genre (id, label, normalized_label) VALUES (?, ?, ?) " +
                "ON CONFLICT (normalized_label) DO NOTHING",
            id,
            label,
            normalized,
        )
        return connection
            .queryBound("SELECT id FROM genre WHERE normalized_label = ?", listOf(normalized)) {
                it.getObject("id", UUID::class.java)
            }.single()
    }

    private fun memberExists(
        connection: java.sql.Connection,
        seedId: String,
        memberKey: String,
    ): Boolean =
        connection
            .queryBound(
                "SELECT 1 FROM seed_member WHERE seed_id = ? AND member_key = ? AND removed_at IS NULL LIMIT 1",
                listOf(seedId, memberKey),
            ) { 1 }
            .isNotEmpty()

    private fun snapshotDigest(manifest: Path): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(manifest.readBytes())

    companion object {
        private const val DEFAULT_SYNOPSIS_LANGUAGE = "en"
    }
}
