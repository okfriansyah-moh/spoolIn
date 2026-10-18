package com.spoolin.catalog.seed

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class SeedProvenance(
    private val connection: Connection,
    private val seed: SeedManifest,
    private val version: String,
) {
    private val mapper = JsonMapper.builder().build()

    fun member(
        node: JsonNode,
        kind: String,
        id: UUID = UUID.fromString(node["stableUUID"].asText()),
    ) {
        connection.executeBound(
            "INSERT INTO seed_member(seed_id,member_key,subject_kind,subject_id,content_hash) " +
                "VALUES(?,?,?,?,?) ON CONFLICT(seed_id,member_key) DO NOTHING",
            version,
            key(node, kind),
            kind,
            id,
            SeedIdentities.hash(node),
        )
        connection.executeBound(
            "UPDATE seed_member SET removed_at=p.deleted_at FROM provenance_record p " +
                "WHERE seed_member.seed_id=? AND seed_member.member_key=? AND p.subject_id=? AND " +
                "p.deleted_at IS NOT NULL",
            version,
            key(node, kind),
            id,
        )
    }

    fun source(
        node: JsonNode,
        kind: String,
        id: UUID,
    ) {
        val fields = node["sources"].associate { it["field"].asText() to mapper.convertValue(it, Map::class.java) }
        val first = node["sources"].first()
        connection.executeBound(
            "INSERT INTO provenance_record(source,external_id,subject_kind,subject_id,record_url," +
                "retrieved_at,reuse_note,field_sources) VALUES(?,?,?,?,?,?,?,?::jsonb) " +
                "ON CONFLICT(source,external_id,subject_kind) DO UPDATE SET " +
                "record_url=EXCLUDED.record_url,retrieved_at=EXCLUDED.retrieved_at," +
                "reuse_note=EXCLUDED.reuse_note,field_sources=EXCLUDED.field_sources " +
                "WHERE provenance_record.deleted_at IS NULL",
            node["source"].asText(),
            node["externalId"].asText(),
            kind,
            id,
            first["url"].asText(),
            Timestamp.from(Instant.parse(first["retrievedAt"].asText())),
            if (seed.publicPages) {
                "Public webpage source recorded; no open-license claim."
            } else {
                "CC0-1.0 Wikidata facts; original factual synopsis where applicable"
            },
            mapper.writeValueAsString(fields),
        )
    }

    fun verify() {
        val expected = mutableListOf<Pair<JsonNode, String>>()
        expected += seed.records.map { it to "TITLE" }
        expected += seed.people.map { it to "PERSON" }
        expected += seed.roles.map { it to "ROLE" }
        expected +=
            seed.records
                .flatMap { it["genres"].toList() }
                .distinctBy { it["externalId"].asText() }
                .map { it to "GENRE" }
        expected += seed.records.flatMap { it["credits"].toList() }.map { it to "CREDIT" }
        expected.forEach { (node, kind) -> verifyMember(node, kind) }
        val count =
            connection
                .queryBound(
                    "SELECT count(*) AS total FROM seed_member WHERE seed_id=?",
                    listOf(version),
                ) { it.getInt("total") }
                .single()
        require(count == expected.size) { "Seed receipt membership differs from the frozen snapshot" }
    }

    private fun verifyMember(
        node: JsonNode,
        kind: String,
    ) {
        val rows =
            connection.queryBound(
                "SELECT subject_id,content_hash,removed_at FROM seed_member " +
                    "WHERE seed_id=? AND member_key=? AND subject_kind=?",
                listOf(version, key(node, kind), kind),
            ) {
                Triple(
                    it.getObject("subject_id", UUID::class.java),
                    it.getBytes("content_hash"),
                    it.getTimestamp("removed_at"),
                )
            }
        val row = rows.singleOrNull() ?: error("Seed receipt member missing; automatic recreation prohibited")
        require(java.security.MessageDigest.isEqual(row.second, SeedIdentities.hash(node))) {
            "Seed member checksum mismatch"
        }
        if (kind != "ROLE") require(row.first.toString() == node["stableUUID"].asText())
        if (kind in setOf("TITLE", "PERSON")) verifySource(node, kind, row.first, row.third != null)
    }

    private fun verifySource(
        node: JsonNode,
        kind: String,
        id: UUID,
        removed: Boolean,
    ) {
        val rows =
            connection.queryBound(
                "SELECT subject_id,deleted_at FROM provenance_record " +
                    "WHERE source=? AND external_id=? AND subject_kind=?",
                listOf(node["source"].asText(), node["externalId"].asText(), kind),
            ) { it.getObject("subject_id", UUID::class.java) to (it.getTimestamp("deleted_at") != null) }
        require(rows.singleOrNull() == (id to removed)) { "Seed provenance/tombstone evidence mismatch" }
    }

    companion object {
        fun key(
            node: JsonNode,
            kind: String,
        ): String = "$kind/${node["source"].asText()}/${node["externalId"].asText()}"
    }
}
