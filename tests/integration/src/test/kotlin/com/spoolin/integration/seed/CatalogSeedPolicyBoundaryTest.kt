package com.spoolin.integration.seed

import com.spoolin.catalog.seed.CatalogSeedLoader
import com.spoolin.catalog.seed.SeedIdentities
import com.spoolin.catalog.seed.SeedManifest
import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PolicySettings
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

/** Real Catalog PostgreSQL initialization boundary; controlled post-load edits do not claim lifecycle/UI proof. */
class CatalogSeedPolicyBoundaryTest {
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `fresh policy rejection writes nothing and successful rerun preserves receipt edits and tombstones`() {
        withCatalog("t033-effective-seed-policy.json") { fixture, pool ->
            val manifest = SeedManifest.read(path(fixture))
            val reads = AtomicInteger()
            var current = configured(mapOf("CREDIT_ROLE_MAX_CODE_POINTS" to "1"))
            val loader =
                CatalogSeedLoader(pool) {
                    reads.incrementAndGet()
                    current
                }
            assertThrows(InputViolation::class.java) { loader.loadFromManifest(path(fixture), manifest.version) }
            listOf("title", "person", "seed_member", "provenance_record").forEach { table ->
                assertEquals(0, count(pool, "SELECT count(*) FROM $table"))
            }
            assertEquals(
                0,
                count(pool, "SELECT count(*) FROM operation_receipt WHERE receipt_type='CATALOG_INITIALIZATION'"),
            )
            current = configured()
            assertEquals(200, loader.loadFromManifest(path(fixture), manifest.version))
            assertEquals(100, count(pool, "SELECT count(*) FROM title WHERE kind='MOVIE'"))
            assertEquals(100, count(pool, "SELECT count(*) FROM title WHERE kind='TV'"))
            val receipt = receipt(pool)
            assertTrue(receipt.contains(current.revision))
            val id = UUID.fromString(manifest.records.first()["stableUUID"].asText())
            retainUserTombstone(pool, id)
            val calls = reads.get()
            current = configured(mapOf("TITLE_MAX_CODE_POINTS" to "1", "PERSON_NAME_MAX_CODE_POINTS" to "1"))
            assertEquals(0, loader.loadFromManifest(path(fixture), manifest.version))
            assertEquals(calls, reads.get())
            assertEquals(receipt, receipt(pool))
            assertEquals(
                1,
                count(
                    pool,
                    "SELECT count(*) FROM title WHERE display_title='Retained user edit' " +
                        "AND status='DELETED' AND version=1",
                ),
            )
            assertEquals(1, count(pool, "SELECT count(*) FROM seed_member WHERE removed_at IS NOT NULL"))
        }
    }

    @Test
    fun `retained failed receipt remains unchanged on invalid retry and records history on valid recovery`() {
        withCatalog("t033-failed-seed-policy.json") { fixture, pool ->
            val manifest = SeedManifest.read(path(fixture))
            val id = SeedIdentities.initialization(manifest.namespace, manifest.version)
            // Controlled retained-failure setup; this test exercises actual loader retry, not process-crash capture.
            pool.committed { connection ->
                connection.executeBound(
                    "INSERT INTO operation_receipt(" +
                        "id,actor_scope,kind,receipt_type,request_hash,state,correlation_id," +
                        "seed_version,seed_snapshot_hash,seed_movie_count,seed_tv_count,seed_summary,error_code) " +
                        "VALUES(?,'SERVICE_INITIALIZER','INITIALIZE_CATALOG','CATALOG_INITIALIZATION'," +
                        "?,'FAILED',?,?,?,0,0,'{}'::jsonb,'SEED_FAILED')",
                    id,
                    manifest.snapshotHash,
                    id,
                    manifest.version,
                    manifest.snapshotHash,
                )
            }
            var current = configured(mapOf("TITLE_MAX_CODE_POINTS" to "1"))
            val loader = CatalogSeedLoader(pool) { current }
            val original = receipt(pool)
            assertThrows(InputViolation::class.java) { loader.loadFromManifest(path(fixture), manifest.version) }
            assertEquals(original, receipt(pool))
            assertEquals(0, count(pool, "SELECT count(*) FROM title"))
            assertEquals(
                0,
                count(pool, "SELECT count(*) FROM operational_event WHERE action='SEED_INITIALIZATION_RETRY'"),
            )
            current = configured()
            assertEquals(200, loader.loadFromManifest(path(fixture), manifest.version))
            assertEquals(
                1,
                count(
                    pool,
                    "SELECT count(*) FROM operational_event " +
                        "WHERE action='SEED_INITIALIZATION_RETRY' AND outcome='FAILED'",
                ),
            )
            assertEquals(0, loader.loadFromManifest(path(fixture), manifest.version))
            assertEquals(
                1,
                count(pool, "SELECT count(*) FROM operational_event WHERE action='SEED_INITIALIZATION_RETRY'"),
            )
        }
    }

    @Test
    fun `wider effective bounds and optional fields persist while changed same-version input fails before policy`() {
        withCatalog("t033-seed-optional-fields.json") { fixture, pool ->
            val root = mapper.readTree(path(fixture).toFile()) as ObjectNode
            optionalFacts(root)
            val person = root["people"][0] as ObjectNode
            val input = write(root, fixture.root.resolve("controlled-seed.json"))
            val policy = configured(mapOf("PERSON_NAME_MAX_CODE_POINTS" to "300"))
            assertEquals(
                200,
                CatalogSeedLoader(pool) { policy }.loadFromManifest(input, root["manifestVersion"].asText()),
            )
            assertEquals(
                1,
                count(
                    pool,
                    "SELECT count(*) FROM person WHERE length(name)=300 AND biography='Reviewed fixture biography'",
                ),
            )
            assertEquals(
                1,
                count(
                    pool,
                    "SELECT count(*) FROM title WHERE original_title='Reviewed fixture title' " +
                        "AND original_language='en' " +
                        "AND runtime_minutes=120 AND countries=ARRAY['US']::text[]",
                ),
            )
            assertEquals(
                1,
                count(pool, "SELECT count(*) FROM credit WHERE characters=ARRAY['Reviewed Character']::text[]"),
            )
            val retained = receipt(pool)
            val loader = CatalogSeedLoader(pool) { error("Historical success must not consult new policy") }
            assertEquals(0, loader.loadFromManifest(input, root["manifestVersion"].asText()))
            person.put("name", "Changed fixture fact")
            write(root, input)
            val conflict =
                assertThrows(IllegalArgumentException::class.java) {
                    loader.loadFromManifest(input, root["manifestVersion"].asText())
                }
            assertEquals("SEED_MANIFEST_CONFLICT", conflict.message)
            assertEquals(retained, receipt(pool))
            assertEquals(1, count(pool, "SELECT count(*) FROM person WHERE length(name)=300"))
        }
    }

    private fun retainUserTombstone(
        pool: DataSource,
        id: UUID,
    ) {
        pool.committed { connection ->
            connection.executeBound(
                "UPDATE title SET display_title='Retained user edit',status='DELETED',version=1 WHERE id=?",
                id,
            )
            connection.executeBound(
                "UPDATE seed_member SET removed_at=CURRENT_TIMESTAMP WHERE subject_kind='TITLE' AND subject_id=?",
                id,
            )
            connection.executeBound(
                "UPDATE provenance_record SET deleted_at=CURRENT_TIMESTAMP " +
                    "WHERE subject_kind='TITLE' AND subject_id=?",
                id,
            )
        }
    }

    private fun optionalFacts(root: ObjectNode) {
        val person = root["people"][0] as ObjectNode
        person.put("name", "N".repeat(300)).put("biography", "Reviewed fixture biography")
        addSource(person, "biography")
        val record =
            root["records"].first { row ->
                row["credits"].any { it["roleExternalId"].asText() == "P161" }
            } as ObjectNode
        val values = record["normalized"] as ObjectNode
        values
            .put("originalTitle", "Reviewed fixture title")
            .put("originalLanguage", "en")
            .put("runtimeMinutes", 120)
        values.putArray("countries").add("US")
        listOf("originalTitle", "originalLanguage", "runtimeMinutes", "countries").forEach { addSource(record, it) }
        val credit = record["credits"].first { it["roleExternalId"].asText() == "P161" } as ObjectNode
        credit.putArray("characters").add("Reviewed Character")
        addSource(credit, "characters")
    }

    private fun withCatalog(
        name: String,
        block: (SignedReceiptFixture, DataSource) -> Unit,
    ) {
        SignedReceiptFixture(name).use { fixture ->
            fixture.fixture.owner
                .pool("catalog")
                .use { pool -> block(fixture, pool) }
        }
    }

    private fun addSource(
        node: ObjectNode,
        field: String,
    ) {
        val source = (node["sources"].first() as ObjectNode).deepCopy()
        source.put("field", field)
        (node["sources"] as ArrayNode).add(source)
    }

    private fun write(
        root: ObjectNode,
        target: Path,
    ): Path {
        root["people"].forEach { (it as ObjectNode).put("contentSHA256", SeedIdentities.hashHex(it)) }
        root["records"].forEach { (it as ObjectNode).put("contentSHA256", SeedIdentities.hashHex(it)) }
        root.put("snapshotSHA256", SeedIdentities.hashHex(root))
        mapper.writeValue(target.toFile(), root)
        return target
    }

    private fun path(fixture: SignedReceiptFixture): Path =
        fixture.fixture.owner.repository
            .resolve("data/seed/manifest.json")

    private fun configured(values: Map<String, String> = emptyMap()): PublicPolicy =
        PolicySettings.resolve(ConfigurationSources(values, emptyMap()))

    private fun count(
        pool: DataSource,
        sql: String,
    ): Int =
        pool.connection.use { connection ->
            connection.queryBound(sql, emptyList()) { it.getInt(1) }.single()
        }

    private fun receipt(pool: DataSource): String =
        pool.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT row_to_json(r)::text FROM operation_receipt r WHERE receipt_type='CATALOG_INITIALIZATION'",
                    emptyList(),
                ) {
                    it.getString(1)
                }.single()
        }
}
