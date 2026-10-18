package com.spoolin.catalog.seed

import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.util.UUID

class SeedManifest private constructor(
    val root: JsonNode,
) {
    val namespace: UUID = UUID.fromString(root["namespaceUUID"].asText())
    val version: String = root["manifestVersion"].asText()
    val records: List<JsonNode> = root["records"].toList()
    val people: List<JsonNode> = root["people"].toList()
    val roles: List<JsonNode> = root["roles"].toList()
    val snapshotHash: ByteArray = SeedIdentities.hash(root)

    private fun validate() {
        validateHeader()
        validateCounts()
        validateDistribution()
        val validation = SeedRecordValidation(namespace, people, roles)
        records.forEach(validation::validateRecord)
        people.forEach(validation::validatePerson)
        roles.forEach(validation::validateRole)
        require(roles.map { it.path("label").asText() }.toSet() == setOf("Actor", "Director", "Writer", "Producer"))
        require(root.path("files").isArray && root.path("files").size() == 0) {
            "Unreviewed seed artwork is prohibited"
        }
    }

    private fun validateHeader() {
        require(root.path("schemaVersion").asInt() == SeedLimits.SCHEMA_VERSION)
        require(version.matches(Regex("[A-Za-z0-9_.-]{1,64}")))
        require(root.path("expectedMovieCount").asInt() == SeedLimits.MOVIES)
        require(root.path("expectedTVCount").asInt() == SeedLimits.SERIES)
        require(root.path("sourcePolicyVersion").asText().isNotBlank())
        require(root.path("licenseSummary").asText().isNotBlank())
        require(root.path("snapshotSHA256").asText() == SeedIdentities.hashHex(root)) {
            "Seed snapshot checksum mismatch"
        }
    }

    private fun validateCounts() {
        require(records.size == SeedLimits.TITLES)
        require(records.count { it.path("kind").asText() == "MOVIE" } == SeedLimits.MOVIES)
        require(records.count { it.path("kind").asText() == "TV" } == SeedLimits.SERIES)
        require(records.map { it.path("externalId").asText() }.distinct().size == SeedLimits.TITLES)
        require(people.map { it.path("externalId").asText() }.distinct().size == people.size)
    }

    private fun validateDistribution() {
        val years = records.groupingBy { it["normalized"]["release"]["year"].asText() }.eachCount()
        val months =
            records
                .groupingBy {
                    it["normalized"]["release"]["month"].takeUnless { month -> month.isNull }?.asText() ?: "unknown"
                }.eachCount()
        val stored = root.path("distribution")
        require(stored.path("years").propertyNames().toSet() == years.keys)
        require(stored.path("months").propertyNames().toSet() == months.keys)
        years.forEach { (year, count) -> require(stored["years"].path(year).asInt() == count) }
        months.forEach { (month, count) -> require(stored["months"].path(month).asInt() == count) }
    }

    companion object {
        fun read(path: Path): SeedManifest {
            val root = JsonMapper.builder().build().readTree(path.toFile())
            return SeedManifest(root).also { it.validate() }
        }

        fun prepare(
            path: Path,
            policy: PublicPolicy = PolicyEnvironment.resolve(System::getenv),
        ): SeedManifest = read(path).also { SeedPolicyValidation(policy).validate(it) }
    }
}
