package com.spoolin.catalog.features.provenance

import com.spoolin.support.receipts.queryBound
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

private const val PROVENANCE_BATCH_SIZE = 100

/**
 * Provenance record reads for active title/person subjects.
 *
 * Returns all non-deleted provenance records for a given subject.
 * internal fields (subject_kind, subject_id, deleted_at) are not exposed.
 */
class ProvenanceReads(
    private val dataSource: DataSource,
) {
    data class ProvenanceRow(
        val source: String,
        val externalId: String,
        val recordUrl: String?,
        val retrievedAt: java.time.Instant,
        val reuseNote: String,
        val fieldSources: String, // raw JSON string
        val locallyChanged: List<String>,
    )

    fun forSubject(
        subjectId: UUID,
        subjectKind: String,
    ): List<ProvenanceRow> =
        dataSource.connection.use { conn ->
            conn.queryBound(
                """SELECT source, external_id, record_url, retrieved_at, reuse_note,
                          field_sources, locally_changed
                   FROM provenance_record
                   WHERE subject_id = ? AND subject_kind = ? AND deleted_at IS NULL
                   ORDER BY source ASC""",
                listOf(subjectId, subjectKind),
            ) { rs -> mapRow(rs) }
        }

    fun forSubjects(
        subjectIds: List<UUID>,
        subjectKind: String,
    ): Map<UUID, List<ProvenanceRow>> {
        if (subjectIds.isEmpty()) return emptyMap()
        return dataSource.connection.use { connection ->
            subjectIds
                .distinct()
                .chunked(PROVENANCE_BATCH_SIZE)
                .flatMap { batch ->
                    val placeholders = batch.joinToString(",") { "?" }
                    connection.queryBound(
                        "SELECT subject_id,source,external_id,record_url,retrieved_at,reuse_note," +
                            "field_sources,locally_changed FROM provenance_record " +
                            "WHERE subject_id IN ($placeholders) AND subject_kind=? AND deleted_at IS NULL " +
                            "ORDER BY subject_id,source ASC",
                        batch + subjectKind,
                    ) { row -> row.getObject("subject_id", UUID::class.java) to mapRow(row) }
                }.groupBy({ it.first }, { it.second })
        }
    }

    private fun mapRow(rs: ResultSet): ProvenanceRow {
        val rawChanged = rs.getArray("locally_changed")?.array
        val locallyChanged = (rawChanged as? Array<*>)?.map { it.toString() } ?: emptyList()
        return ProvenanceRow(
            source = rs.getString("source"),
            externalId = rs.getString("external_id"),
            recordUrl = rs.getString("record_url"),
            retrievedAt = rs.getTimestamp("retrieved_at").toInstant(),
            reuseNote = rs.getString("reuse_note"),
            fieldSources = rs.getString("field_sources") ?: "{}",
            locallyChanged = locallyChanged,
        )
    }
}
