package com.spoolin.catalog.seed

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.time.LocalDate
import java.util.UUID

class SeedManifest private constructor(val root: JsonNode) {
    val namespace: UUID = UUID.fromString(root["namespaceUUID"].asText())
    val version: String = root["manifestVersion"].asText()
    val records: List<JsonNode> = root["records"].toList()
    val people: List<JsonNode> = root["people"].toList()
    val roles: List<JsonNode> = root["roles"].toList()
    val snapshotHash: ByteArray = SeedIdentities.hash(root)

    private fun validate() {
        require(root.path("schemaVersion").asInt() == 2 && version.matches(Regex("[A-Za-z0-9_.-]{1,64}")))
        require(root.path("expectedMovieCount").asInt() == 100 && root.path("expectedTVCount").asInt() == 100)
        require(root.path("sourcePolicyVersion").asText().isNotBlank() && root.path("licenseSummary").asText().isNotBlank())
        require(root.path("snapshotSHA256").asText() == SeedIdentities.hashHex(root)) { "Seed snapshot checksum mismatch" }
        require(records.size == 200 && records.count { it.path("kind").asText() == "MOVIE" } == 100)
        require(records.count { it.path("kind").asText() == "TV" } == 100)
        require(records.map { it.path("externalId").asText() }.distinct().size == 200)
        require(people.map { it.path("externalId").asText() }.distinct().size == people.size)
        records.forEach(::validateRecord)
        people.forEach { identity(it, "PERSON"); checksum(it); sources(it, setOf("name")); text(it, "name", 255) }
        roles.forEach { identity(it, "ROLE"); sources(it, setOf("label", "category", "allowsCharacters")); require(it.path("category").asText() in setOf("CAST", "CREATOR")) }
        require(roles.map { it.path("label").asText() }.toSet() == setOf("Actor", "Director", "Writer", "Producer"))
        require(root.path("files").isArray && root.path("files").size() == 0) { "Unreviewed seed artwork is prohibited" }
    }

    private fun validateRecord(record: JsonNode) {
        val kind = record["kind"].asText()
        identity(record, kind)
        checksum(record)
        sources(record, setOf("title", "synopsis", "release", "genres"))
        val values = record["normalized"]
        text(values, "title", 255); text(values, "synopsis", 5000)
        require(values.path("synopsisLanguage").asText() == "en")
        date(values["release"])
        if (kind == "TV") {
            require(values.path("tvStatus").asText() in setOf("CONTINUING", "ENDED", "CANCELLED", "UNKNOWN"))
            if (!values.path("end").isNull && !values.path("end").isMissingNode) date(values["end"])
        }
        val genres = record["genres"].toList()
        require(genres.size in 1..20 && genres.map { it["stableUUID"].asText() }.distinct().size == genres.size)
        genres.forEach { identity(it, "GENRE"); sources(it, setOf("label")); text(it, "label", 255) }
        require(values["genreIds"].map { it.asText() } == genres.map { it["stableUUID"].asText() })
        record["credits"].forEach(::validateCredit)
        require(record.path("artwork").isNull) { "Artwork requires a separately reviewed file license" }
    }

    private fun validateCredit(credit: JsonNode) {
        identity(credit, "CREDIT")
        require(people.any { it["externalId"].asText() == credit["personExternalId"].asText() })
        require(roles.any { it["externalId"].asText() == credit["roleExternalId"].asText() })
        sources(credit, setOf("person", "role"))
        require(credit["characters"].isArray)
    }

    private fun identity(node: JsonNode, kind: String) {
        val source = node.path("source").asText()
        val external = node.path("externalId").asText()
        require(source == "wikidata" && external.isNotBlank())
        require(node.path("stableUUID").asText() == SeedIdentities.record(namespace, source, kind, external).toString())
    }

    private fun checksum(node: JsonNode) {
        require(node.path("contentSHA256").asText() == SeedIdentities.hashHex(node)) { "Seed record checksum mismatch" }
    }

    private fun sources(node: JsonNode, fields: Set<String>) {
        val sources = node.path("sources").toList()
        require(sources.map { it.path("field").asText() }.toSet().containsAll(fields))
        sources.forEach {
            require(it.path("url").asText().startsWith("https://www.wikidata.org/wiki/"))
            require(it.path("license").asText() == "CC0-1.0" && it.path("permissionBasis").asText().isNotBlank())
            java.time.Instant.parse(it.path("retrievedAt").asText())
        }
    }

    private fun text(node: JsonNode, field: String, maximum: Int) {
        val value = node.path(field).asText()
        require(value.isNotBlank() && value.codePointCount(0, value.length) <= maximum)
    }

    private fun date(node: JsonNode) {
        val year = node.path("year").asInt()
        require(year in 1..9999)
        val month = node.path("month").takeUnless { it.isNull }?.asInt()
        val day = node.path("day").takeUnless { it.isNull }?.asInt()
        require(month == null || month in 1..12)
        require(day == null || month != null)
        if (day != null) LocalDate.of(year, requireNotNull(month), day)
        require(node.path("precision").asText() == if (day != null) "DAY" else if (month != null) "MONTH" else "YEAR")
    }

    companion object {
        fun read(path: Path): SeedManifest = SeedManifest(JsonMapper.builder().build().readTree(path.toFile())).also { it.validate() }
    }
}
