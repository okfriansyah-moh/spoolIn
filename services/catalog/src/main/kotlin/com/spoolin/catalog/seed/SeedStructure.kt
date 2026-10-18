package com.spoolin.catalog.seed

import com.spoolin.support.policy.PartialDate
import com.spoolin.support.policy.TextRules
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID

internal class SeedStructure(private val namespace: UUID) {
    fun identity(
        node: JsonNode,
        kind: String,
    ) {
        val source = node.path("source").asText()
        val external = node.path("externalId").asText()
        require(source == "wikidata" && external.isNotBlank())
        require(node.path("stableUUID").asText() == SeedIdentities.record(namespace, source, kind, external).toString())
    }

    fun checksum(node: JsonNode) {
        require(node.path("contentSHA256").asText() == SeedIdentities.hashHex(node)) { "Seed record checksum mismatch" }
    }

    fun sources(
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

    fun text(
        node: JsonNode,
        field: String,
        maximum: Int? = null,
    ) {
        val value = node.path(field).asText()
        require(node.path(field).isString && !TextRules.blank(value))
        TextRules.scalars(value, field)
        require(maximum == null || value.codePointCount(0, value.length) <= maximum)
    }

    fun partial(node: JsonNode): PartialDate =
        PartialDate.of(
            node["year"].asInt(),
            node.path("month").takeUnless { it.isNull || it.isMissingNode }?.asInt(),
            node.path("day").takeUnless { it.isNull || it.isMissingNode }?.asInt(),
            requireYear = true,
        )

    fun date(node: JsonNode) {
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

}
