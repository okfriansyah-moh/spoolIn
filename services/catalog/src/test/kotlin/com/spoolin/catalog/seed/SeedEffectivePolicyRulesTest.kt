package com.spoolin.catalog.seed

import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PolicySettings
import com.spoolin.support.policy.PublicPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.nio.file.Files
import java.nio.file.Path

/** Pure preparation proof; database atomicity is exercised by CatalogSeedPolicyBoundaryTest. */
class SeedEffectivePolicyRulesTest {
    @TempDir lateinit var temporary: Path
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `preparation validates every applicable configured text bound before returning a snapshot`() {
        val file = repository().resolve("data/seed/manifest.json")
        SeedManifest.prepare(file, configured())
        listOf("PERSON_NAME", "TITLE", "SYNOPSIS", "CREDIT_ROLE", "SOURCE_URL", "REUSE_BASIS").forEach { key ->
            val policy = configured(mapOf("${key}_MAX_CODE_POINTS" to "1"))
            assertThrows(InputViolation::class.java, { SeedManifest.prepare(file, policy) }, key)
        }
    }

    @Test
    fun `structural checks allow wider configured bounds without changing reviewed identities`() {
        val root = snapshot()
        val person = root["people"][0] as ObjectNode
        val id = person["stableUUID"].asText()
        person.put("name", "N".repeat(300))
        val path = write(root)
        assertEquals(
            id,
            SeedManifest
                .read(path)
                .people
                .first()["stableUUID"]
                .asText(),
        )
        assertThrows(InputViolation::class.java) { SeedManifest.prepare(path, configured()) }
        val prepared = SeedManifest.prepare(path, configured(mapOf("PERSON_NAME_MAX_CODE_POINTS" to "300")))
        assertEquals("N".repeat(300), prepared.people.first()["name"].asText())
    }

    @Test
    fun `character bounds and explicit optional metadata use the effective policy and field provenance`() {
        val root = snapshot()
        val record = root["records"].first { row -> row["credits"].any { it["roleExternalId"].asText() == "P161" } }
        val credit = record["credits"].first { it["roleExternalId"].asText() == "P161" } as ObjectNode
        credit.putArray("characters").add("Name")
        val source = (credit["sources"].first() as ObjectNode).deepCopy()
        source.put("field", "characters")
        (credit["sources"] as tools.jackson.databind.node.ArrayNode).add(source)
        val path = write(root)
        assertThrows(InputViolation::class.java) {
            SeedManifest.prepare(path, configured(mapOf("CHARACTER_NAME_MAX_CODE_POINTS" to "3")))
        }
        assertEquals(
            "Name",
            SeedManifest
                .prepare(path, configured())
                .records
                .flatMap { it["credits"].toList() }
                .first { it["stableUUID"] == credit["stableUUID"] }["characters"][0]
                .asText(),
        )
    }

    @Test
    fun `optional biography and original title enforce configured minima and maxima when supplied`() {
        val root = snapshot()
        val person = root["people"][0] as ObjectNode
        person.put("biography", "Biography")
        val personSource = (person["sources"].first() as ObjectNode).deepCopy()
        personSource.put("field", "biography")
        (person["sources"] as tools.jackson.databind.node.ArrayNode).add(personSource)
        val record = root["records"][0] as ObjectNode
        (record["normalized"] as ObjectNode).put("originalTitle", "Title")
        val titleSource = (record["sources"].first() as ObjectNode).deepCopy()
        titleSource.put("field", "originalTitle")
        (record["sources"] as tools.jackson.databind.node.ArrayNode).add(titleSource)
        val path = write(root)
        SeedManifest.prepare(path, configured())
        listOf("BIOGRAPHY", "ORIGINAL_TITLE").forEach { key ->
            assertThrows(InputViolation::class.java) {
                SeedManifest.prepare(path, configured(mapOf("${key}_MAX_CODE_POINTS" to "1")))
            }
            assertThrows(InputViolation::class.java) {
                SeedManifest.prepare(path, configured(mapOf("${key}_MIN_CODE_POINTS" to "10")))
            }
        }
    }

    @Test
    fun `preparation never silently trims frozen metadata or accepts malformed source Unicode`() {
        val root = snapshot()
        (root["people"][0] as ObjectNode).put("name", " Name ")
        assertThrows(InputViolation::class.java) { SeedManifest.prepare(write(root), configured()) }
        (root["people"][0] as ObjectNode).put("name", "Name")
        (root["people"][0]["sources"][0] as ObjectNode).put("permissionBasis", "\u0000")
        assertThrows(InputViolation::class.java) { SeedManifest.prepare(write(root), configured()) }
    }

    @Test
    fun `CLI environment imports protected JSON and preserves higher priority explicit settings`() {
        val config = temporary.resolve("catalog.json")
        Files.writeString(config, """{"TITLE_MAX_CODE_POINTS":321}""")
        val environment = StandardEnvironment()
        environment.propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)
        environment.propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
        environment.propertySources.addFirst(
            MapPropertySource(
                "seed-test",
                mapOf(
                    "spring.config.import" to "file:$config[.yaml]",
                    "spring.config.location" to "optional:file:$temporary/missing.yaml",
                ),
            ),
        )
        seedEnvironment(environment)
        assertEquals("321", environment.getProperty("TITLE_MAX_CODE_POINTS"))
        environment.propertySources.addFirst(MapPropertySource("explicit", mapOf("TITLE_MAX_CODE_POINTS" to "333")))
        assertEquals("333", environment.getProperty("TITLE_MAX_CODE_POINTS"))
        val policy = PolicyEnvironment.resolve { environment.getProperty(it) }
        assertTrue(policy.revision.isNotBlank())
    }

    private fun configured(values: Map<String, String> = emptyMap()): PublicPolicy =
        PolicySettings.resolve(ConfigurationSources(values, emptyMap()))

    private fun snapshot() = mapper.readTree(repository().resolve("data/seed/manifest.json").toFile()) as ObjectNode

    private fun write(root: ObjectNode): Path {
        root["people"].forEach { (it as ObjectNode).put("contentSHA256", SeedIdentities.hashHex(it)) }
        root["records"].forEach { (it as ObjectNode).put("contentSHA256", SeedIdentities.hashHex(it)) }
        root.put("snapshotSHA256", SeedIdentities.hashHex(root))
        return temporary.resolve("controlled-manifest.json").also { mapper.writeValue(it.toFile(), root) }
    }

    private fun repository(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isRegularFile(it.resolve("data/seed/manifest.json")) }
}
