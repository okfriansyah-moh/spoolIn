package com.spoolin.catalog.seed

import com.spoolin.support.policy.PartialDate
import tools.jackson.databind.JsonNode
import java.util.UUID

internal class SeedRecordValidation(
    namespace: UUID,
    private val people: List<JsonNode>,
    private val roles: List<JsonNode>,
) {
    private val structure = SeedStructure(namespace)

    fun validatePerson(person: JsonNode) {
        structure.identity(person, "PERSON")
        structure.checksum(person)
        structure.sources(person, setOf("name"))
        structure.text(person, "name")
    }

    fun validateRole(role: JsonNode) {
        structure.identity(role, "ROLE")
        structure.sources(role, setOf("label", "category", "allowsCharacters"))
        require(role.path("category").asText() in setOf("CAST", "CREATOR"))
    }

    fun validateRecord(record: JsonNode) {
        val kind = record["kind"].asText()
        structure.identity(record, kind)
        structure.checksum(record)
        structure.sources(record, setOf("title", "synopsis", "release", "genres"))
        val values = record["normalized"]
        structure.text(values, "title")
        structure.text(values, "synopsis")
        require(values.path("synopsisLanguage").asText() == "en")
        structure.date(values["release"])
        if (kind == "TV") validateSeries(values)
        validateGenres(record)
        record["credits"].forEach(::validateCredit)
        require(record.path("artwork").isNull) { "Artwork requires a separately reviewed file license" }
    }

    private fun validateSeries(values: JsonNode) {
        require(values.path("tvStatus").asText() in setOf("CONTINUING", "ENDED", "CANCELLED", "UNKNOWN"))
        val end = values.path("end")
        if (!end.isNull && !end.isMissingNode) {
            structure.date(end)
            require(
                PartialDate.ordered(structure.partial(values["release"]), structure.partial(end)) != false,
            ) { "Series ends before its known start" }
        }
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
        structure.identity(genre, "GENRE")
        structure.sources(genre, setOf("label"))
        structure.text(genre, "label", SeedLimits.LABEL_CODE_POINTS)
    }

    private fun validateCredit(credit: JsonNode) {
        structure.identity(credit, "CREDIT")
        require(people.any { it["externalId"].asText() == credit["personExternalId"].asText() })
        require(roles.any { it["externalId"].asText() == credit["roleExternalId"].asText() })
        structure.sources(credit, setOf("person", "role"))
        require(credit["characters"].isArray)
    }
}
