package com.spoolin.catalog.seed

import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

/** Applies one validated frozen snapshot atomically; successful reruns verify retained evidence. */
class CatalogSeedLoader(
    private val dataSource: DataSource,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
) {
    private val mapper = JsonMapper.builder().build()

    fun loadFromManifest(
        manifest: Path,
        seedVersion: String,
    ): Int {
        val seed = SeedManifest.read(manifest)
        require(seedVersion == seed.version) { "Configured seed version differs from manifestVersion" }
        return dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val inserted = applySeed(connection, seed, seedVersion)
                connection.commit()
                inserted
            } finally {
                connection.rollback()
            }
        }
    }

    private fun applySeed(
        connection: Connection,
        seed: SeedManifest,
        version: String,
    ): Int {
        lock(connection)
        val provenance = SeedProvenance(connection, seed, version)
        val inserted =
            if (existing(connection, seed, version)) {
                0
            } else {
                val effective = policy()
                SeedPolicyValidation(effective).validate(seed)
                pending(connection, seed, version)
                val roleIds = SeedRecords.roles(connection, seed, provenance)
                seed.people.forEach { SeedRecords.person(connection, it, provenance) }
                val genreIds = SeedRecords.genres(connection, seed, provenance)
                val plan = SeedRecords.TitlePlan(seed, genreIds, roleIds)
                seed.records.forEach { SeedRecords.title(connection, it, provenance, plan) }
                complete(connection, seed, version, effective.revision)
                seed.records.size
            }
        provenance.verify()
        return inserted
    }

    private fun lock(connection: Connection) {
        connection.executeBound("SET LOCAL lock_timeout = '120s'")
        connection.queryBound("SELECT pg_advisory_xact_lock(41002,1)", emptyList()) { 1 }
        connection.queryBound("SELECT pg_advisory_xact_lock(41001,1)", emptyList()) { 1 }
    }

    private data class SeedReceipt(
        val state: String,
        val hash: ByteArray,
        val movies: Int,
        val series: Int,
    )

    private fun existing(
        connection: Connection,
        seed: SeedManifest,
        version: String,
    ): Boolean {
        val receipt =
            connection
                .queryBound(
                    "SELECT state,seed_snapshot_hash,seed_movie_count,seed_tv_count FROM operation_receipt " +
                        "WHERE seed_version=? AND receipt_type='CATALOG_INITIALIZATION' FOR UPDATE",
                    listOf(version),
                ) {
                    SeedReceipt(
                        it.getString("state"),
                        it.getBytes("seed_snapshot_hash"),
                        it.getInt("seed_movie_count"),
                        it.getInt("seed_tv_count"),
                    )
                }.singleOrNull()
        return receipt?.let { validateReceipt(it, seed) } ?: false
    }

    private fun validateReceipt(
        receipt: SeedReceipt,
        seed: SeedManifest,
    ): Boolean {
        require(java.security.MessageDigest.isEqual(receipt.hash, seed.snapshotHash)) { "SEED_MANIFEST_CONFLICT" }
        require(receipt.state in setOf("SUCCEEDED", "PENDING", "FAILED"))
        val succeeded = receipt.state == "SUCCEEDED"
        if (succeeded) {
            require(receipt.movies == SeedLimits.MOVIES && receipt.series == SeedLimits.SERIES) {
                "Invalid seed receipt counts"
            }
        }
        return succeeded
    }

    private fun pending(
        connection: Connection,
        seed: SeedManifest,
        version: String,
    ) {
        val id = SeedIdentities.initialization(seed.namespace, version)
        connection.executeBound(
            "INSERT INTO operational_event(id,operation_id,actor_kind,action,subject_kind,outcome," +
                "correlation_id,event_type,changes) " +
                "SELECT ?,id,'SERVICE','SEED_INITIALIZATION_RETRY','CATALOG','FAILED',correlation_id," +
                "'ACTIVITY','{}'::jsonb FROM operation_receipt WHERE seed_version=? AND state='FAILED'",
            UUID.randomUUID(),
            version,
        )
        connection.executeBound(
            "INSERT INTO operation_receipt(id,actor_scope,kind,receipt_type,request_hash,state,correlation_id," +
                "seed_version,seed_snapshot_hash,seed_movie_count,seed_tv_count,seed_summary) " +
                "VALUES(?,'SERVICE_INITIALIZER','INITIALIZE_CATALOG','CATALOG_INITIALIZATION'," +
                "?,'PENDING',?,?,?,0,0,'{}'::jsonb) " +
                "ON CONFLICT(seed_version) DO UPDATE SET state='PENDING',error_code=NULL " +
                "WHERE operation_receipt.state IN ('PENDING','FAILED')",
            id,
            seed.snapshotHash,
            id,
            version,
            seed.snapshotHash,
        )
    }

    private fun complete(
        connection: Connection,
        seed: SeedManifest,
        version: String,
        policyRevision: String,
    ) {
        val summary =
            mapOf(
                "manifestVersion" to seed.version,
                "validationRevision" to "TD10.1-v2",
                "policyRevision" to policyRevision,
                "source" to "wikidata",
                "distribution" to mapper.convertValue(seed.root["distribution"], Map::class.java),
                "people" to seed.people.size,
                "credits" to seed.records.sumOf { it["credits"].size() },
                "artworkFiles" to 0,
            )
        connection.executeBound(
            "UPDATE operation_receipt SET state='SUCCEEDED',seed_movie_count=100,seed_tv_count=100," +
                "seed_summary=?::jsonb WHERE seed_version=? " +
                "AND receipt_type='CATALOG_INITIALIZATION' AND state='PENDING'",
            mapper.writeValueAsString(summary),
            version,
        )
    }

}
