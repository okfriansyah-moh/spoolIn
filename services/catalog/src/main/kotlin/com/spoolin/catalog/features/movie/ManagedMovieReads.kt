package com.spoolin.catalog.features.movie

import com.spoolin.catalog.boundary.grpc.ProtoHelpers
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.operations.audit.AuditReads
import com.spoolin.support.identity.ActorRejected
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.AuditEvent
import spoolin.v1.Spoolin.AuditPage
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.PageRequest
import spoolin.v1.Spoolin.Title
import spoolin.v1.Spoolin.TitlePage
import java.sql.ResultSet
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

private const val DEFAULT_FIRST = 24
private const val MAX_PAGE = 100

/**
 * Managed (Maintainer-only) catalog read operations.
 *
 * ManagedTitle: returns active OR archived MOVIE/TV titles.
 * ManagedTitles: returns titles matching the explicit ACTIVE|ARCHIVED state filter.
 * Audit: returns catalog business audit events (read-only; AC-056).
 *
 * F-PERMISSIONS: MAINTAINER role required; enforced before every call.
 * AC-127: actorRef exposes the account UUID only, never email or credentials.
 */
class ManagedMovieReads(
    private val dataSource: DataSource,
    private val provenance: ProvenanceReads,
    private val titleSearch: TitleSearchSql,
    private val auditReads: AuditReads,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun managedTitle(request: Id, response: StreamObserver<Title>) {
        try {
            requireMaintainer()
            val id = parseId(request.id)
            val row = findManagedTitle(id)
            if (row == null) {
                response.onNext(Title.getDefaultInstance())
                response.onCompleted()
                return
            }
            val genreIds = titleSearch.findGenreIds(id)
            val provenanceRows = provenance.forSubject(id, "TITLE")
            response.onNext(titleProto(row, genreIds, provenanceRows))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun managedTitles(request: PageRequest, response: StreamObserver<TitlePage>) {
        try {
            requireMaintainer()
            val state = request.state.takeIf { it.isNotEmpty() }
                ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_STATE").asRuntimeException()
            if (state !in setOf("ACTIVE", "ARCHIVED")) {
                throw Status.INVALID_ARGUMENT.withDescription("INVALID_STATE").asRuntimeException()
            }
            val first = if (request.first > 0) request.first.coerceAtMost(MAX_PAGE) else DEFAULT_FIRST
            val after = request.after.takeIf { it.isNotEmpty() }
            val page = listManagedTitles(state, first, after)
            val builder = TitlePage.newBuilder()
            page.rows.forEach { row ->
                val genreIds = titleSearch.findGenreIds(row.id)
                val provenanceRows = provenance.forSubject(row.id, "TITLE")
                builder.addNodes(titleProto(row, genreIds, provenanceRows))
            }
            page.endCursor?.let { builder.endCursor = it }
            builder.hasNextPage = page.hasNextPage
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun audit(request: PageRequest, response: StreamObserver<AuditPage>) {
        try {
            requireMaintainer()
            val subjectId = request.targetId.takeIf { it.isNotEmpty() }?.let { parseId(it) }
            val first = if (request.first > 0) request.first.coerceAtMost(MAX_PAGE) else DEFAULT_FIRST
            val after = parseAuditCursor(request.after)
            val page = auditReads.list(subjectId, first, after)
            val builder = AuditPage.newBuilder()
            page.rows.forEach { row ->
                val event = AuditEvent.newBuilder()
                    .setId(row.id.toString())
                    .setActorRef(actorRef(row.actorId, row.actorKind))
                    .setAction(row.action)
                    .setOutcome(row.outcome)
                    .setSubjectKind(row.subjectKind)
                    .setAt(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(row.createdAt.epochSecond).setNanos(row.createdAt.nano).build())
                    .also { b ->
                        row.subjectId?.let { b.subjectId = it.toString() }
                        row.operationId?.let { b.operationId = it.toString() }
                    }
                    .build()
                builder.addNodes(event)
            }
            page.keyset?.let { ks ->
                builder.endCursor = "${ks.createdAt.toEpochMilli()}:${ks.id}"
            }
            builder.hasNextPage = page.hasNextPage
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private fun requireMaintainer() {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw ActorRejected()
    }

    private data class ManagedTitleRow(
        val id: UUID, val kind: String, val status: String, val displayTitle: String,
        val originalTitle: String?, val synopsis: String, val synopsisLanguage: String,
        val originalLanguage: String?, val countries: List<String>,
        val releaseYear: Int?, val releaseMonth: Int?, val releaseDay: Int?,
        val releaseContext: String?, val runtimeMinutes: Int?, val tvStatus: String?,
        val endYear: Int?, val endMonth: Int?, val endDay: Int?, val assetId: UUID?, val version: Long,
    )

    private data class ManagedPage(val rows: List<ManagedTitleRow>, val endCursor: String?, val hasNextPage: Boolean)

    private fun findManagedTitle(id: UUID): ManagedTitleRow? =
        dataSource.connection.use { conn ->
            conn.queryBound(
                "SELECT ${TitleSearchSql.TITLE_COLUMNS} FROM title t WHERE t.id = ?",
                listOf(id),
            ) { mapManagedRow(it) }.firstOrNull()
        }

    private fun listManagedTitles(state: String, first: Int, after: String?): ManagedPage {
        val params = mutableListOf<Any?>(state)
        var keyset = ""
        if (after != null) {
            val parts = after.split(":")
            if (parts.size >= 2) {
                val millis = parts[0].toLongOrNull()
                val uuid = runCatching { UUID.fromString(parts.drop(1).joinToString(":")) }.getOrNull()
                if (millis != null && uuid != null) {
                    keyset = " AND (t.release_year < ? OR t.release_year IS NULL OR (t.release_year = ? AND t.id > ?))"
                }
            }
        }
        val sql =
            "SELECT ${TitleSearchSql.TITLE_COLUMNS} FROM title t WHERE t.status = ? $keyset " +
                "ORDER BY t.release_year DESC NULLS LAST, t.release_month DESC NULLS LAST, " +
                "t.release_day DESC NULLS LAST, t.id ASC LIMIT ${first + 1}"
        val rows = dataSource.connection.use { conn -> conn.queryBound(sql, params) { mapManagedRow(it) } }
        val hasNext = rows.size > first
        val page = if (hasNext) rows.dropLast(1) else rows
        val cursor = page.lastOrNull()?.let { "${it.releaseYear ?: "null"}:${it.id}" }
        return ManagedPage(page, cursor, hasNext)
    }

    private fun mapManagedRow(rs: ResultSet): ManagedTitleRow {
        val rawCountries = rs.getArray("countries")?.array
        val countries = (rawCountries as? Array<*>)?.map { it.toString() } ?: emptyList()
        return ManagedTitleRow(
            id = rs.getObject("id", UUID::class.java), kind = rs.getString("kind"),
            status = rs.getString("status"), displayTitle = rs.getString("display_title"),
            originalTitle = rs.getString("original_title"), synopsis = rs.getString("synopsis"),
            synopsisLanguage = rs.getString("synopsis_language"), originalLanguage = rs.getString("original_language"),
            countries = countries, releaseYear = rs.getObject("release_year") as? Int,
            releaseMonth = rs.getObject("release_month") as? Int, releaseDay = rs.getObject("release_day") as? Int,
            releaseContext = rs.getString("release_context"), runtimeMinutes = rs.getObject("runtime_minutes") as? Int,
            tvStatus = rs.getString("tv_status"), endYear = rs.getObject("end_year") as? Int,
            endMonth = rs.getObject("end_month") as? Int, endDay = rs.getObject("end_day") as? Int,
            assetId = rs.getObject("asset_id")?.let { UUID.fromString(it.toString()) },
            version = rs.getLong("version"),
        )
    }

    private fun titleProto(
        row: ManagedTitleRow,
        genreIds: List<UUID>,
        provenanceRows: List<ProvenanceReads.ProvenanceRow>,
    ): Title {
        val values = spoolin.v1.Spoolin.MovieValues.newBuilder()
            .setTitle(row.displayTitle)
            .setSynopsis(row.synopsis)
            .setSynopsisLanguage(row.synopsisLanguage)
            .addAllCountries(row.countries)
            .addAllGenreIds(genreIds.map { it.toString() })
            .setRelease(ProtoHelpers.partialDate(row.releaseYear, row.releaseMonth, row.releaseDay))
            .also { b ->
                row.originalTitle?.let { b.originalTitle = it }
                row.originalLanguage?.let { b.originalLanguage = it }
                row.runtimeMinutes?.let { b.runtimeMinutes = it }
            }
            .build()
        return Title.newBuilder()
            .setId(row.id.toString())
            .setKind(row.kind)
            .setState(row.status)
            .setValues(values)
            .setVersion(row.version)
            .setTvEnd(ProtoHelpers.partialDate(row.endYear, row.endMonth, row.endDay))
            .addAllProvenance(provenanceRows.map { ProtoHelpers.provenance(it) })
            .also { b ->
                row.assetId?.let { b.assetId = it.toString() }
                row.tvStatus?.let { b.tvStatus = it }
                row.releaseContext?.let { b.releaseContext = it }
            }
            .build()
    }

    private fun actorRef(actorId: UUID?, actorKind: String): String =
        when (actorKind) {
            "HUMAN" -> actorId?.toString() ?: "UNKNOWN"
            else -> actorKind
        }

    private fun parseAuditCursor(raw: String): AuditReads.Keyset? {
        if (raw.isEmpty()) return null
        return runCatching {
            val parts = raw.split(":")
            val millis = parts[0].toLong()
            val uuid = UUID.fromString(parts.drop(1).joinToString(":"))
            AuditReads.Keyset(java.time.Instant.ofEpochMilli(millis), uuid)
        }.getOrNull()
    }

    private fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }
}
