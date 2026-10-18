package com.spoolin.catalog.seed

import com.spoolin.catalog.features.credit.CreditValidation
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.StructuralRules
import com.spoolin.support.policy.TextBound
import com.spoolin.support.policy.TextRules
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI

/** The reviewed snapshot must already be normalized; validation never edits or abbreviates frozen facts. */
class SeedPolicyValidation(
    private val policy: PublicPolicy,
) {
    fun validate(seed: SeedManifest) {
        seed.people.forEach { person ->
            text(person, "name", "personName")
            optional(person, "biography", "biography")
            if (known(person, "biography")) coverage(person, setOf("biography"))
            sources(person)
        }
        seed.roles.forEach { role ->
            text(role, "label", "creditRole")
            sources(role)
        }
        val roles = seed.roles.associateBy { it["externalId"].asText() }
        seed.records.forEach { record ->
            val values = record["normalized"]
            title(values)
            val extra = setOf("originalTitle", "originalLanguage", "runtimeMinutes").filter { known(values, it) }.toSet()
            coverage(record, extra + if (values["countries"].size() == 0) emptySet() else setOf("countries"))
            sources(record)
            record["genres"].forEach(::sources)
            record["credits"].forEach { credit -> credit(credit, roles.getValue(credit["roleExternalId"].asText())) }
        }
    }

    private fun title(values: JsonNode) {
        text(values, "title", "title")
        text(values, "synopsis", "synopsis")
        optional(values, "originalTitle", "originalTitle")
        language(values, "synopsisLanguage", required = true)
        language(values, "originalLanguage", required = false)
        val countries = values.path("countries")
        require(countries.isArray) { "Seed countries must be an array" }
        countries.forEach {
            require(it.isString)
            StructuralRules.country(it.asText())
        }
        val runtime = values.path("runtimeMinutes")
        require(runtime.isNull || runtime.isMissingNode || runtime.isIntegralNumber && runtime.asLong() in 1..MAX_RUNTIME) {
            "Seed runtime must be a positive supported integer"
        }
    }

    private fun credit(
        node: JsonNode,
        role: JsonNode,
    ) {
        val raw =
            node["characters"].toList().map {
                require(it.isString)
                it.asText()
            }
        val names = TextRules.characters(raw, policy.textBounds.getValue("characterName"))
        require(names == raw) { "Seed characters must already be normalized" }
        if (names.isNotEmpty()) coverage(node, setOf("characters"))
        CreditValidation.validateCharactersForRole(role["allowsCharacters"].asBoolean(), role["category"].asText(), names)
        sources(node)
    }

    private fun language(
        node: JsonNode,
        field: String,
        required: Boolean,
    ) {
        val value = node.path(field)
        if (!required && (value.isNull || value.isMissingNode)) return
        require(value.isString) { "Seed language must be text" }
        StructuralRules.language(value.asText())
    }

    private fun optional(
        node: JsonNode,
        field: String,
        bound: String,
    ) {
        if (!node.path(field).isNull && !node.path(field).isMissingNode) text(node, field, bound)
    }

    private fun text(
        node: JsonNode,
        field: String,
        bound: String,
    ) {
        exact(node.path(field), policy.textBounds.getValue(bound), field)
    }

    private fun exact(
        node: JsonNode,
        bound: TextBound,
        field: String,
    ): String {
        require(node.isString) { "Seed metadata must be text" }
        val raw = node.asText()
        val checked = TextRules.ordinary(raw, bound, field)
        if (checked != raw) throw InputViolation(field, "The frozen seed must already contain normalized text.")
        return checked
    }

    private fun known(
        node: JsonNode,
        field: String,
    ): Boolean = !node.path(field).isNull && !node.path(field).isMissingNode

    private fun coverage(
        node: JsonNode,
        required: Set<String>,
    ) {
        require(
            node["sources"]
                .toList()
                .map { it["field"].asText() }
                .toSet()
                .containsAll(required),
        ) { "Missing seed field provenance" }
    }

    private fun sources(node: JsonNode) {
        val entries = node["sources"].toList()
        entries.forEach { source ->
            source.propertyNames().forEach { field ->
                require(source[field].isString) { "Seed source metadata must contain text values" }
                TextRules.scalars(source[field].asText(), "sourceMetadata")
            }
        }
        require(entries.map { it["field"].asText() }.distinct().size == entries.size) { "Duplicate seed source field" }
        val fields = entries.associate { it["field"].asText() to it }
        val compact = mapper.writeValueAsBytes(fields).size
        val spaces =
            (fields.size - 1).coerceAtLeast(0) + fields.size +
                entries.sumOf { source ->
                    source.size() + (source.size() - 1).coerceAtLeast(0)
                }
        require(compact + spaces <= SOURCE_ENVELOPE_BYTES) { "Seed provenance exceeds its storage envelope" }
        node["sources"].forEach { source ->
            val url = exact(source["url"], policy.textBounds.getValue("sourceUrl"), "sourceUrl")
            exact(source["permissionBasis"], policy.textBounds.getValue("reuseBasis"), "reuseBasis")
            val uri = URI(url)
            require(
                uri.scheme == "https" && uri.host == "www.wikidata.org" && uri.userInfo == null && uri.port == -1 &&
                    uri.path.startsWith("/wiki/") && uri.path.length > WIKI_PREFIX_LENGTH,
            ) { "Unsupported seed source URL" }
        }
    }

    companion object {
        private const val MAX_RUNTIME = 100000L
        private const val WIKI_PREFIX_LENGTH = 6
        private const val SOURCE_ENVELOPE_BYTES = 16384
        private val mapper = JsonMapper.builder().build()
    }
}
