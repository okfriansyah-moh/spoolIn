package com.spoolin.catalog.seed

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.time.LocalDate
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
        records.forEach(::validateRecord)
        people.forEach(::validatePerson)
        roles.forEach(::validateRole)
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

    private fun validatePerson(person: JsonNode) {
        identity(person, "PERSON")
        checksum(person)
        sources(person, setOf("name"))
        text(person, "name", SeedLimits.LABEL_CODE_POINTS)
    }

    private fun validateRole(role: JsonNode) {
        identity(role, "ROLE")
        sources(role, setOf("label", "category", "allowsCharacters"))
        require(role.path("category").asText() in setOf("CAST", "CREATOR"))
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

    private fun validateRecord(record: JsonNode) {
        val kind = record["kind"].asText()
        identity(record, kind)
        checksum(record)
        sources(record, setOf("title", "synopsis", "release", "genres"))
        val values = record["normalized"]
        text(values, "title", SeedLimits.LABEL_CODE_POINTS)
        text(values, "synopsis", SeedLimits.SYNOPSIS_CODE_POINTS)
        require(values.path("synopsisLanguage").asText() == "en")
        date(values["release"])
        if (kind == "TV") validateSeries(values)
        validateGenres(record)
        record["credits"].forEach(::validateCredit)
        require(record.path("artwork").isNull) { "Artwork requires a separately reviewed file license" }
    }

    private fun validateSeries(values: JsonNode) {
        require(values.path("tvStatus").asText() in setOf("CONTINUING", "ENDED", "CANCELLED", "UNKNOWN"))
        val end = values.path("end")
        if (!end.isNull && !end.isMissingNode) date(end)
    }

    private fun validateGenres(record: JsonNode) {
        val genres = record["genres"].toList()
        require(genres.size in 1..SeedLimits.GENRES_PER_TITLE)
        require(genres.map { it["stableUUID"].asText() }.distinct().size == genres.size)
        genres.forEach(::validateGenre)
        require(
            record["normalized"]["genreIds"].toList().map { it.asText() } ==
                genres.map { it["stableUUID"].asText() },
        )
    }

    private fun validateGenre(genre: JsonNode) {
        identity(genre, "GENRE")
        sources(genre, setOf("label"))
        text(genre, "label", SeedLimits.LABEL_CODE_POINTS)
    }

    private fun validateCredit(credit: JsonNode) {
        identity(credit, "CREDIT")
        require(people.any { it["externalId"].asText() == credit["personExternalId"].asText() })
        require(roles.any { it["externalId"].asText() == credit["roleExternalId"].asText() })
        sources(credit, setOf("person", "role"))
        require(credit["characters"].isArray)
    }

    private fun identity(
        node: JsonNode,
        kind: String,
    ) {
        val source = node.path("source").asText()
        val external = node.path("externalId").asText()
        require(source == "wikidata" && external.isNotBlank())
        require(node.path("stableUUID").asText() == SeedIdentities.record(namespace, source, kind, external).toString())
    }

    private fun checksum(node: JsonNode) {
        require(node.path("contentSHA256").asText() == SeedIdentities.hashHex(node)) { "Seed record checksum mismatch" }
    }

    private fun sources(
        node: JsonNode,
        fields: Set<String>,
    ) {
        val sources = node.path("sources").toList()
        require(sources.map { it.path("field").asText() }.toSet().containsAll(fields))
        sources.forEach(::validateSource)
    }

    private fun validateSource(source: JsonNode) {
        require(source.path("url").asText().startsWith("https://www.wikidata.org/wiki/"))
        require(source.path("license").asText() == "CC0-1.0")
        require(source.path("permissionBasis").asText().isNotBlank())
        java.time.Instant.parse(source.path("retrievedAt").asText())
    }

    private fun text(
        node: JsonNode,
        field: String,
        maximum: Int,
    ) {
        val value = node.path(field).asText()
        require(value.isNotBlank() && value.codePointCount(0, value.length) <= maximum)
    }

    private fun date(node: JsonNode) {
        val year = node.path("year").asInt()
        require(year in 1..SeedLimits.MAXIMUM_YEAR)
        val month = node.path("month").takeUnless { it.isNull }?.asInt()
        val day = node.path("day").takeUnless { it.isNull }?.asInt()
        require(month == null || month in 1..SeedLimits.MONTHS_PER_YEAR)
        require(day == null || month != null)
        if (day != null) LocalDate.of(year, requireNotNull(month), day)
        val precision =
            when {
                day != null -> "DAY"
                month != null -> "MONTH"
                else -> "YEAR"
            }
        require(node.path("precision").asText() == precision)
    }

    companion object {
        fun read(path: Path): SeedManifest = SeedManifest(JsonMapper.builder().build().readTree(path.toFile())).also { it.validate() }
    }
}
