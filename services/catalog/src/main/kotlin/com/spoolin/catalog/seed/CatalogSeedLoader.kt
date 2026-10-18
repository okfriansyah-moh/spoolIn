package com.spoolin.catalog.seed

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.sql.Connection
import java.util.Locale
import java.util.UUID
import javax.sql.DataSource

/** Applies one validated frozen snapshot atomically; successful reruns verify retained evidence. */
class CatalogSeedLoader(private val dataSource: DataSource) {
    private val mapper = JsonMapper.builder().build()

    fun loadFromManifest(manifest: Path, seedVersion: String): Int {
        val seed = SeedManifest.read(manifest)
        require(seedVersion == seed.version) { "Configured seed version differs from manifestVersion" }
        return dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                lock(connection)
                val provenance = SeedProvenance(connection, seed, seedVersion)
                if (existing(connection, seed, seedVersion)) {
                    provenance.verify()
                    connection.commit()
                    return@use 0
                }
                pending(connection, seed, seedVersion)
                val roleIds = roles(connection, seed, provenance)
                seed.people.forEach { person(connection, it, provenance) }
                val genreIds = genres(connection, seed, provenance)
                seed.records.forEach { title(connection, it, provenance, genreIds, roleIds, seed) }
                complete(connection, seed, seedVersion)
                provenance.verify()
                connection.commit()
                seed.records.size
            } catch (failure: Exception) {
                connection.rollback()
                throw failure
            }
        }
    }

    private fun lock(connection: Connection) {
        connection.executeBound("SET LOCAL lock_timeout = '120s'")
        connection.queryBound("SELECT pg_advisory_xact_lock(41002,1)", emptyList()) { 1 }
        connection.queryBound("SELECT pg_advisory_xact_lock(41001,1)", emptyList()) { 1 }
    }

    private fun existing(connection: Connection, seed: SeedManifest, version: String): Boolean {
        val receipt = connection.queryBound(
            "SELECT state,seed_snapshot_hash,seed_movie_count,seed_tv_count FROM operation_receipt " +
                "WHERE seed_version=? AND receipt_type='CATALOG_INITIALIZATION' FOR UPDATE", listOf(version),
        ) { listOf(it.getString("state"), it.getBytes("seed_snapshot_hash"), it.getInt("seed_movie_count"), it.getInt("seed_tv_count")) }.singleOrNull() ?: return false
        require(java.security.MessageDigest.isEqual(receipt[1] as ByteArray, seed.snapshotHash)) { "SEED_MANIFEST_CONFLICT" }
        if (receipt[0] == "SUCCEEDED") {
            require(receipt[2] == 100 && receipt[3] == 100) { "Invalid seed receipt counts" }
            return true
        }
        require(receipt[0] in setOf("PENDING", "FAILED"))
        return false
    }

    private fun pending(connection: Connection, seed: SeedManifest, version: String) {
        val id = SeedIdentities.initialization(seed.namespace, version)
        connection.executeBound(
            "INSERT INTO operational_event(id,operation_id,actor_kind,action,subject_kind,outcome,correlation_id,event_type,changes) " +
                "SELECT ?,id,'SERVICE','SEED_INITIALIZATION_RETRY','CATALOG','FAILED',correlation_id,'ACTIVITY','{}'::jsonb " +
                "FROM operation_receipt WHERE seed_version=? AND state='FAILED'",
            UUID.randomUUID(), version,
        )
        connection.executeBound(
            "INSERT INTO operation_receipt(id,actor_scope,kind,receipt_type,request_hash,state,correlation_id," +
                "seed_version,seed_snapshot_hash,seed_movie_count,seed_tv_count,seed_summary) " +
                "VALUES(?,'SERVICE_INITIALIZER','INITIALIZE_CATALOG','CATALOG_INITIALIZATION',?,'PENDING',?,?,?,0,0,'{}'::jsonb) " +
                "ON CONFLICT(seed_version) DO UPDATE SET state='PENDING',error_code=NULL WHERE operation_receipt.state IN ('PENDING','FAILED')",
            id, seed.snapshotHash, id, version, seed.snapshotHash,
        )
    }

    private fun complete(connection: Connection, seed: SeedManifest, version: String) {
        val summary = mapOf("manifestVersion" to seed.version, "validationRevision" to "TD10.1-v2", "source" to "wikidata",
            "distribution" to mapper.convertValue(seed.root["distribution"], Map::class.java), "people" to seed.people.size,
            "credits" to seed.records.sumOf { it["credits"].size() }, "artworkFiles" to 0)
        connection.executeBound(
            "UPDATE operation_receipt SET state='SUCCEEDED',seed_movie_count=100,seed_tv_count=100,seed_summary=?::jsonb " +
                "WHERE seed_version=? AND receipt_type='CATALOG_INITIALIZATION' AND state='PENDING'",
            mapper.writeValueAsString(summary), version,
        )
    }

    private fun roles(connection: Connection, seed: SeedManifest, provenance: SeedProvenance): Map<String, UUID> =
        seed.roles.associate { role ->
            val normalized = role["label"].asText().lowercase(Locale.ROOT)
            val id = connection.queryBound("SELECT id FROM credit_role WHERE normalized_label=?", listOf(normalized)) {
                it.getObject("id", UUID::class.java)
            }.singleOrNull() ?: UUID.fromString(role["stableUUID"].asText()).also { value ->
                connection.executeBound("INSERT INTO credit_role(id,category,label,normalized_label,allows_characters) VALUES(?,?,?,?,?)",
                    value, role["category"].asText(), role["label"].asText(), normalized, role["allowsCharacters"].asBoolean())
            }
            provenance.member(role, "ROLE", id)
            role["externalId"].asText() to id
        }

    private fun person(connection: Connection, node: JsonNode, provenance: SeedProvenance) {
        val id = UUID.fromString(node["stableUUID"].asText())
        connection.executeBound("INSERT INTO person(id,name,status,version) VALUES(?,?,'ACTIVE',0)", id, node["name"].asText())
        provenance.member(node, "PERSON")
        provenance.source(node, "PERSON", id)
    }

    private fun genres(connection: Connection, seed: SeedManifest, provenance: SeedProvenance): Map<String, UUID> =
        seed.records.flatMap { it["genres"].toList() }.distinctBy { it["externalId"].asText() }.associate { node ->
            val id = UUID.fromString(node["stableUUID"].asText())
            connection.executeBound("INSERT INTO genre(id,label,normalized_label) VALUES(?,?,?)",
                id, node["label"].asText(), node["label"].asText().lowercase(Locale.ROOT))
            provenance.member(node, "GENRE")
            node["stableUUID"].asText() to id
        }

    private fun title(connection: Connection, node: JsonNode, provenance: SeedProvenance, genres: Map<String, UUID>, roles: Map<String, UUID>, seed: SeedManifest) {
        val id = UUID.fromString(node["stableUUID"].asText())
        val values = node["normalized"]
        val release = values["release"]
        val end = values.path("end")
        connection.executeBound(
            "INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,release_year,release_month,release_day," +
                "release_context,tv_status,end_year,end_month,end_day,version) VALUES(?,?,'ACTIVE',?,?,?,?,?,?,?,?,?,?,?,0)",
            id, node["kind"].asText(), values["title"].asText(), values["synopsis"].asText(), values["synopsisLanguage"].asText(),
            release["year"].asInt(), integer(release, "month"), integer(release, "day"),
            if (node["kind"].asText() == "TV") "FIRST_AIR" else "ORIGINAL_RELEASE", values.path("tvStatus").takeUnless { it.isMissingNode || it.isNull }?.asText(),
            integer(end, "year"), integer(end, "month"), integer(end, "day"),
        )
        values["genreIds"].forEach { genre -> connection.executeBound("INSERT INTO title_genre(title_id,genre_id) VALUES(?,?)", id, genres.getValue(genre.asText())) }
        node["credits"].forEach { credit ->
            connection.executeBound("INSERT INTO credit(id,title_id,person_id,role_id,version) VALUES(?,?,?,?,0)",
                UUID.fromString(credit["stableUUID"].asText()), id,
                SeedIdentities.record(seed.namespace, "wikidata", "PERSON", credit["personExternalId"].asText()), roles.getValue(credit["roleExternalId"].asText()))
            provenance.member(credit, "CREDIT")
        }
        provenance.member(node, "TITLE")
        provenance.source(node, "TITLE", id)
    }

    private fun integer(node: JsonNode, field: String): Int? = node.path(field).takeUnless { it.isNull || it.isMissingNode }?.asInt()
}
