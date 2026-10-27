package com.spoolin.catalog.unit

import com.spoolin.catalog.seed.CatalogSeedLoader
import com.spoolin.catalog.seed.SeedIdentities
import com.spoolin.catalog.seed.SeedManifest
import com.spoolin.catalog.seed.SeedProvenance
import com.spoolin.catalog.seed.SeedRecords
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import java.nio.file.Path
import java.util.UUID

/** Exercises loader decisions over driver responses; transaction guarantees remain PostgreSQL acceptance. */
class SeedLoadingRulesTest {
    private val manifest = Path.of(System.getProperty("spoolin.repo.root", "../.."), "data/seed/baselines/wikidata-2026-10-v1.json")
    private val seed by lazy { SeedManifest.read(manifest) }

    private fun members(): List<Pair<JsonNode, String>> =
        buildList {
            addAll(seed.records.map { it to "TITLE" })
            addAll(seed.people.map { it to "PERSON" })
            addAll(seed.roles.map { it to "ROLE" })
            addAll(
                seed.records
                    .flatMap { it["genres"].toList() }
                    .distinctBy { it["externalId"].asText() }
                    .map { it to "GENRE" },
            )
            addAll(seed.records.flatMap { it["credits"].toList() }.map { it to "CREDIT" })
        }

    private fun verification(): List<List<Map<String, Any?>>> =
        buildList {
            for ((node, kind) in members()) {
                val id = UUID.fromString(node["stableUUID"].asText())
                add(listOf(mapOf("subject_id" to id, "content_hash" to SeedIdentities.hash(node), "removed_at" to null)))
                if (kind in setOf("TITLE", "PERSON")) add(listOf(mapOf("subject_id" to id, "deleted_at" to null)))
            }
            add(listOf(mapOf("total" to members().size)))
        }

    private fun receipt(
        state: String = "SUCCEEDED",
        hash: ByteArray = seed.snapshotHash,
        movies: Int = 100,
    ) = listOf(
        mapOf(
            "state" to state,
            "seed_snapshot_hash" to hash,
            "seed_movie_count" to movies,
            "seed_tv_count" to 100,
        ),
    )

    @Test fun `fresh frozen snapshot writes titles people roles credits and complete receipt`() {
        val replies =
            buildList<List<Map<String, Any?>>> {
                add(listOf(mapOf("lock" to 1)))
                add(listOf(mapOf("lock" to 1)))
                add(emptyList())
                repeat(seed.roles.size) { add(emptyList()) }
                repeat(seed.people.size) { add(emptyList()) }
                addAll(verification())
            }
        val db = JdbcRows(*replies.toTypedArray())
        assertEquals(200, CatalogSeedLoader(db.source).loadFromManifest(manifest, seed.version))
        assertTrue(db.writes.flatten().any { it == "MOVIE" })
        assertTrue(db.writes.flatten().any { it == "TV" })
        assertTrue(
            db.writes
                .flatten()
                .filterIsInstance<String>()
                .any { it.contains("manifestVersion") && it.contains("policyRevision") },
        )
        assertTrue(
            db.writes
                .flatten()
                .filterIsInstance<String>()
                .any { it.contains("CC0-1.0") },
        )
    }

    @Test fun `completed seed replays verify retained membership rather than reinserting records`() {
        val replies = listOf(listOf(mapOf("lock" to 1)), listOf(mapOf("lock" to 1)), receipt()) + verification()
        val db = JdbcRows(*replies.toTypedArray())
        assertEquals(0, CatalogSeedLoader(db.source).loadFromManifest(manifest, seed.version))
        assertEquals(1, db.writes.size, "Only the transaction-local lock timeout is written during verification")
    }

    @Test fun `seed receipt conflict bad counts and version mismatch fail without applying records`() {
        for (receipt in listOf(receipt(hash = ByteArray(32)), receipt(movies = 99), receipt(state = "UNKNOWN"))) {
            val db = JdbcRows(listOf(mapOf("lock" to 1)), listOf(mapOf("lock" to 1)), receipt)
            assertThrows(IllegalArgumentException::class.java) { CatalogSeedLoader(db.source).loadFromManifest(manifest, seed.version) }
            assertEquals(1, db.writes.size)
        }
        assertThrows(
            IllegalArgumentException::class.java,
        ) { CatalogSeedLoader(JdbcRows().source).loadFromManifest(manifest, "other-version") }
    }

    @Test fun `seed verification refuses missing altered or removed membership evidence`() {
        val node = seed.records.first()
        for (member in listOf(
            emptyList<Map<String, Any?>>(),
            listOf(
                mapOf(
                    "subject_id" to UUID.fromString(node["stableUUID"].asText()),
                    "content_hash" to ByteArray(32),
                    "removed_at" to null,
                ),
            ),
        )) {
            val db = JdbcRows(member)
            assertThrows(RuntimeException::class.java) { db.source.connection.use { SeedProvenance(it, seed, seed.version).verify() } }
        }
    }

    @Test fun `seed person tombstones are retained while provenance membership is recorded`() {
        val node = seed.people.first()
        val db = JdbcRows(listOf(mapOf("subject_id" to unitTarget)))
        db.source.connection.use { conn -> SeedRecords.person(conn, node, SeedProvenance(conn, seed, seed.version)) }
        assertFalse(db.writes.any { node["name"].asText() in it })
        assertTrue(db.writes.any { "PERSON" in it })
    }
}
