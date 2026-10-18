package com.spoolin.catalog.seed

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.JsonNode
import java.sql.Connection
import java.util.Locale
import java.util.UUID

internal object SeedRecords {
    fun roles(
        connection: Connection,
        seed: SeedManifest,
        provenance: SeedProvenance,
    ): Map<String, UUID> =
        seed.roles.associate { role ->
            val normalized = role["label"].asText().lowercase(Locale.ROOT)
            val identity = SeedRoleIdentity.resolve(role["source"].asText(), role["externalId"].asText(), role["stableUUID"].asText())
            val id =
                connection
                    .queryBound(
                        "SELECT id,category,allows_characters FROM credit_role WHERE id=?",
                        listOf(identity),
                    ) {
                        require(
                            it.getString("category") == role["category"].asText() &&
                                it.getBoolean("allows_characters") == role["allowsCharacters"].asBoolean(),
                        ) {
                            "Existing starter role differs from approved semantics"
                        }
                        it.getObject("id", UUID::class.java)
                    }.singleOrNull() ?: identity.also { value ->
                    connection.executeBound(
                        "INSERT INTO credit_role(id,category,label,normalized_label,allows_characters) VALUES(?,?,?,?,?)",
                        value,
                        role["category"].asText(),
                        role["label"].asText(),
                        normalized,
                        role["allowsCharacters"].asBoolean(),
                    )
                }
            provenance.member(role, "ROLE", id)
            role["externalId"].asText() to id
        }

    fun person(
        connection: Connection,
        node: JsonNode,
        provenance: SeedProvenance,
    ) {
        val id = UUID.fromString(node["stableUUID"].asText())
        connection.executeBound(
            "INSERT INTO person(id,name,biography,status,version) VALUES(?,?,?,'ACTIVE',0)",
            id,
            node["name"].asText(),
            optionalText(node, "biography"),
        )
        provenance.member(node, "PERSON")
        provenance.source(node, "PERSON", id)
    }

    fun genres(
        connection: Connection,
        seed: SeedManifest,
        provenance: SeedProvenance,
    ): Map<String, UUID> =
        seed.records.flatMap { it["genres"].toList() }.distinctBy { it["externalId"].asText() }.associate { node ->
            val id = UUID.fromString(node["stableUUID"].asText())
            connection.executeBound(
                "INSERT INTO genre(id,label,normalized_label) VALUES(?,?,?)",
                id,
                node["label"].asText(),
                node["label"].asText().lowercase(Locale.ROOT),
            )
            provenance.member(node, "GENRE")
            node["stableUUID"].asText() to id
        }

    data class TitlePlan(
        val seed: SeedManifest,
        val genres: Map<String, UUID>,
        val roles: Map<String, UUID>,
    )

    fun title(
        connection: Connection,
        node: JsonNode,
        provenance: SeedProvenance,
        plan: TitlePlan,
    ) {
        val id = UUID.fromString(node["stableUUID"].asText())
        titleFields(connection, node)
        node["normalized"]["genreIds"].forEach { genre ->
            connection.executeBound(
                "INSERT INTO title_genre(title_id,genre_id) VALUES(?,?)",
                id,
                plan.genres.getValue(genre.asText()),
            )
        }
        node["credits"].forEach { credit ->
            connection.executeBound(
                "INSERT INTO credit(id,title_id,person_id,role_id,characters,version) VALUES(?,?,?,?,?::text[],0)",
                UUID.fromString(credit["stableUUID"].asText()),
                id,
                SeedIdentities.record(plan.seed.namespace, "wikidata", "PERSON", credit["personExternalId"].asText()),
                plan.roles.getValue(credit["roleExternalId"].asText()),
                connection.createArrayOf("text", credit["characters"].toList().map { it.asText() }.toTypedArray()),
            )
            provenance.member(credit, "CREDIT")
        }
        provenance.member(node, "TITLE")
        provenance.source(node, "TITLE", id)
    }

    private fun titleFields(
        connection: Connection,
        node: JsonNode,
    ) {
        val id = UUID.fromString(node["stableUUID"].asText())
        val values = node["normalized"]
        val release = values["release"]
        val end = values.path("end")
        val context = if (node["kind"].asText() == "TV") "FIRST_AIR" else "ORIGINAL_RELEASE"
        val status = values.path("tvStatus").takeUnless { it.isMissingNode || it.isNull }?.asText()
        connection.executeBound(
            "INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,release_year," +
                "release_month,release_day,release_context,tv_status,end_year,end_month,end_day," +
                "original_title,original_language,countries,runtime_minutes,version) " +
                "VALUES(?,?,'ACTIVE',?,?,?,?,?,?,?,?,?,?,?,?,?,?::text[],?,0)",
            id,
            node["kind"].asText(),
            values["title"].asText(),
            values["synopsis"].asText(),
            values["synopsisLanguage"].asText(),
            release["year"].asInt(),
            integer(release, "month"),
            integer(release, "day"),
            context,
            status,
            integer(end, "year"),
            integer(end, "month"),
            integer(end, "day"),
            optionalText(values, "originalTitle"),
            optionalText(values, "originalLanguage"),
            connection.createArrayOf("text", values["countries"].toList().map { it.asText() }.toTypedArray()),
            integer(values, "runtimeMinutes"),
        )
    }

    private fun optionalText(
        node: JsonNode,
        field: String,
    ): String? = node.path(field).takeUnless { it.isNull || it.isMissingNode }?.asText()

    private fun integer(
        node: JsonNode,
        field: String,
    ): Int? = node.path(field).takeUnless { it.isNull || it.isMissingNode }?.asInt()
}
