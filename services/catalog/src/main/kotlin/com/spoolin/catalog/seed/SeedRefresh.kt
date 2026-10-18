package com.spoolin.catalog.seed

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.sql.Connection
import java.util.UUID

/** Existing relationships are retained; metadata advances only from the frozen predecessor value. */
internal object SeedRefresh {
    private val columns =
        mapOf(
            "title" to "display_title",
            "synopsis" to "synopsis",
            "synopsisLanguage" to "synopsis_language",
            "originalTitle" to "original_title",
            "originalLanguage" to "original_language",
            "runtimeMinutes" to "runtime_minutes",
            "tvStatus" to "tv_status",
        )

    fun existing(
        connection: Connection,
        node: JsonNode,
        provenance: SeedProvenance,
        plan: SeedRecords.TitlePlan,
    ): Boolean {
        val id = UUID.fromString(node["stableUUID"].asText())
        val baseline =
            plan.seed.baseline
                ?.records
                ?.find { it["stableUUID"].asText() == id.toString() } ?: return false
        val status =
            connection
                .queryBound("SELECT status FROM title WHERE id=? FOR UPDATE", listOf(id)) {
                    it.getString("status")
                }.singleOrNull()
        val deleted =
            connection
                .queryBound(
                    "SELECT deleted_at IS NOT NULL AS deleted FROM provenance_record WHERE subject_id=?",
                    listOf(id),
                ) { it.getBoolean("deleted") }
                .any { it }
        val retained = status != null || deleted
        if (retained) {
            if (status == "ACTIVE") refreshFields(connection, id, baseline["normalized"], node["normalized"])
            node["credits"].forEach { provenance.member(it, "CREDIT") }
            provenance.member(node, "TITLE")
            provenance.source(node, "TITLE", id)
        }
        return retained
    }

    private fun refreshFields(
        connection: Connection,
        id: UUID,
        old: JsonNode,
        updated: JsonNode,
    ) {
        dateFields(connection, id, old, updated)
        columns.forEach { (field, column) ->
            val before = value(old.path(field))
            val after = value(updated.path(field))
            if (before != after) {
                connection.executeBound(
                    "UPDATE title SET $column=?,version=version+1,updated_at=CURRENT_TIMESTAMP " +
                        "WHERE id=? AND $column IS NOT DISTINCT FROM ?",
                    after,
                    id,
                    before,
                )
            }
        }
    }

    private fun dateFields(
        connection: Connection,
        id: UUID,
        old: JsonNode,
        updated: JsonNode,
    ) {
        listOf("release" to "release", "end" to "end").forEach { (field, prefix) ->
            val parts = listOf("year", "month", "day")
            val before = parts.map { value(old.path(field).path(it)) }
            val after = parts.map { value(updated.path(field).path(it)) }
            if (before != after) {
                connection.executeBound(
                    "UPDATE title SET ${prefix}_year=?,${prefix}_month=?,${prefix}_day=?,version=version+1 " +
                        "WHERE id=? AND ${prefix}_year IS NOT DISTINCT FROM ? AND ${prefix}_month " +
                        "IS NOT DISTINCT FROM ? " +
                        "AND ${prefix}_day IS NOT DISTINCT FROM ?",
                    after[0],
                    after[1],
                    after[2],
                    id,
                    before[0],
                    before[1],
                    before[2],
                )
            }
        }
        val before =
            old
                .path("countries")
                .toList()
                .map { it.asText() }
                .toTypedArray()
        val after =
            updated
                .path("countries")
                .toList()
                .map { it.asText() }
                .toTypedArray()
        if (!before.contentEquals(after)) {
            connection.executeBound(
                "UPDATE title SET countries=?,version=version+1 WHERE id=? AND countries=?",
                connection.createArrayOf("text", after),
                id,
                connection.createArrayOf("text", before),
            )
        }
    }

    private fun value(node: JsonNode): Any? =
        when {
            node.isMissingNode || node.isNull -> null
            node.isIntegralNumber -> node.asInt()
            else -> node.asText()
        }
}
