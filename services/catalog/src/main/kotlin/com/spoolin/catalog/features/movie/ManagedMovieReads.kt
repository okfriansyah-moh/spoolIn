@file:Suppress("TooManyFunctions", "NestedBlockDepth")

package com.spoolin.catalog.features.movie

import com.spoolin.catalog.boundary.grpc.ProtoHelpers
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.ManagedReadCursors
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.operations.audit.AuditReads
import com.spoolin.support.identity.ActorRejected
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.ReadPolicy
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
import java.util.UUID
import javax.sql.DataSource

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
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
    private val cursors: ManagedReadCursors = ManagedReadCursors.ephemeral(),
) {
    fun managedTitle(
        request: Id,
        response: StreamObserver<Title>,
    ) {
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
        } catch (_: InputViolation) {
            response.onError(Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException())
        }
    }

    fun managedTitles(
        request: PageRequest,
        response: StreamObserver<TitlePage>,
    ) {
        try {
            requireMaintainer()
            val state =
                request.state.takeIf { it.isNotEmpty() }
                    ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_STATE").asRuntimeException()
            if (state !in setOf("ACTIVE", "ARCHIVED")) {
                throw Status.INVALID_ARGUMENT.withDescription("INVALID_STATE").asRuntimeException()
            }
            val first = ReadPolicy(policy()).rpcPageSize(request.first)
            val scope = cursors.scope(ReadPolicy(policy()), "managed-titles", first, mapOf("state" to state))
            val after = cursors.title(request.after.takeIf { it.isNotEmpty() }, scope)
            val page = listManagedTitles(state, first, after, scope)
            val ids = page.rows.map { it.id }
            val genres = titleSearch.findGenresForTitles(ids)
            val sources = provenance.forSubjects(ids, "TITLE")
            val builder = TitlePage.newBuilder()
            page.rows.forEach { row ->
                builder.addNodes(titleProto(row, genres[row.id].orEmpty(), sources[row.id].orEmpty()))
            }
            page.endCursor?.let { builder.endCursor = it }
            builder.hasNextPage = page.hasNextPage
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: InputViolation) {
            response.onError(Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException())
        }
    }

    fun audit(
        request: PageRequest,
        response: StreamObserver<AuditPage>,
    ) {
        try {
            requireMaintainer()
            val subjectId = request.targetId.takeIf { it.isNotEmpty() }?.let { parseId(it) }
            val first = ReadPolicy(policy()).rpcPageSize(request.first)
            val scope =
                cursors.scope(
                    ReadPolicy(policy()),
                    "managed-audit",
                    first,
                    mapOf("subject" to subjectId?.toString()),
                )
            val after = cursors.audit(request.after.takeIf { it.isNotEmpty() }, scope)
            val page = auditReads.list(subjectId, first, after)
            val builder = AuditPage.newBuilder()
            page.rows.forEach { row -> builder.addNodes(auditEvent(row)) }
            page.keyset?.let { ks ->
                builder.endCursor =
                    cursors.people.encode(
                        com.spoolin.catalog.features.search.PeopleCursor
                            .Keyset(ks.createdAt.toString(), ks.id),
                        scope,
                        cursors.now(),
                    )
            }
            builder.hasNextPage = page.hasNextPage
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: InputViolation) {
            response.onError(Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException())
        }
    }

    private fun auditEvent(row: AuditReads.AuditRow): AuditEvent =
        AuditEvent
            .newBuilder()
            .setId(row.id.toString())
            .setActorRef(actorRef(row.actorId, row.actorKind))
            .setAction(row.action)
            .setOutcome(row.outcome)
            .setSubjectKind(row.subjectKind)
            .setAt(
                com.google.protobuf.Timestamp
                    .newBuilder()
                    .setSeconds(row.createdAt.epochSecond)
                    .setNanos(row.createdAt.nano),
            ).addAllChanges(AuditReadChanges.read(row.changes, row.subjectKind))
            .also { b ->
                row.subjectId?.let { b.subjectId = it.toString() }
                row.operationId?.let { b.operationId = it.toString() }
            }.build()

    // ── private helpers ───────────────────────────────────────────────────────

    private fun requireMaintainer() {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw ActorRejected()
    }

    private data class ManagedTitleRow(
        val id: UUID,
        val kind: String,
        val status: String,
        val displayTitle: String,
        val originalTitle: String?,
        val synopsis: String,
        val synopsisLanguage: String,
        val originalLanguage: String?,
        val countries: List<String>,
        val releaseYear: Int?,
        val releaseMonth: Int?,
        val releaseDay: Int?,
        val releaseContext: String?,
        val runtimeMinutes: Int?,
        val tvStatus: String?,
        val endYear: Int?,
        val endMonth: Int?,
        val endDay: Int?,
        val assetId: UUID?,
        val version: Long,
    )

    private data class ManagedPage(
        val rows: List<ManagedTitleRow>,
        val endCursor: String?,
        val hasNextPage: Boolean,
    )

    private fun findManagedTitle(id: UUID): ManagedTitleRow? =
        dataSource.connection.use { conn ->
            conn
                .queryBound(
                    "SELECT ${TitleSearchSql.TITLE_COLUMNS} FROM title t WHERE t.id = ?",
                    listOf(id),
                ) { mapManagedRow(it) }
                .firstOrNull()
        }

    private fun listManagedTitles(
        state: String,
        first: Int,
        after: com.spoolin.catalog.features.search.CatalogCursor.Keyset?,
        scope: String,
    ): ManagedPage {
        val params = mutableListOf<Any?>(state)
        val keyset = after?.let { " AND " + titleSearch.keysetCondition(it, params) } ?: ""
        val sql =
            "SELECT ${TitleSearchSql.TITLE_COLUMNS} FROM title t WHERE t.status = ? $keyset " +
                "ORDER BY t.release_year DESC NULLS LAST, t.release_month DESC NULLS LAST, " +
                "t.release_day DESC NULLS LAST, t.id ASC LIMIT ${first + 1}"
        val rows = dataSource.connection.use { conn -> conn.queryBound(sql, params) { mapManagedRow(it) } }
        val hasNext = rows.size > first
        val page = if (hasNext) rows.dropLast(1) else rows
        val cursor =
            page.lastOrNull()?.let {
                cursors.titles.encode(
                    com.spoolin.catalog.features.search.CatalogCursor.Keyset(
                        it.releaseYear,
                        it.releaseMonth,
                        it.releaseDay,
                        it.id,
                    ),
                    scope,
                    cursors.now(),
                )
            }
        return ManagedPage(page, cursor, hasNext)
    }

    private fun mapManagedRow(rs: ResultSet): ManagedTitleRow {
        val rawCountries = rs.getArray("countries")?.array
        val countries = (rawCountries as? Array<*>)?.map { it.toString() } ?: emptyList()
        return ManagedTitleRow(
            id = rs.getObject("id", UUID::class.java),
            kind = rs.getString("kind"),
            status = rs.getString("status"),
            displayTitle = rs.getString("display_title"),
            originalTitle = rs.getString("original_title"),
            synopsis = rs.getString("synopsis"),
            synopsisLanguage = rs.getString("synopsis_language"),
            originalLanguage = rs.getString("original_language"),
            countries = countries,
            releaseYear = rs.getObject("release_year") as? Int,
            releaseMonth = rs.getObject("release_month") as? Int,
            releaseDay = rs.getObject("release_day") as? Int,
            releaseContext = rs.getString("release_context"),
            runtimeMinutes = rs.getObject("runtime_minutes") as? Int,
            tvStatus = rs.getString("tv_status"),
            endYear = rs.getObject("end_year") as? Int,
            endMonth = rs.getObject("end_month") as? Int,
            endDay = rs.getObject("end_day") as? Int,
            assetId = rs.getObject("asset_id")?.let { UUID.fromString(it.toString()) },
            version = rs.getLong("version"),
        )
    }

    private fun titleProto(
        row: ManagedTitleRow,
        genreIds: List<UUID>,
        provenanceRows: List<ProvenanceReads.ProvenanceRow>,
    ): Title {
        val values =
            spoolin.v1.Spoolin.MovieValues
                .newBuilder()
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
                }.build()
        return Title
            .newBuilder()
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
            }.build()
    }

    private fun actorRef(
        actorId: UUID?,
        actorKind: String,
    ): String =
        when (actorKind) {
            "HUMAN" -> actorId?.toString() ?: "UNKNOWN"
            else -> actorKind
        }

    private fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }
}

/** Read only known domain paths before encoding; unsupported/private properties never enter the wire projection. */
private object AuditReadChanges {
    private const val MAX_BYTES = 65536
    private const val MAX_DEPTH = 8
    private val mapper =
        tools.jackson.databind.json.JsonMapper
            .builder(
                tools.jackson.core.json.JsonFactory
                    .builder()
                    .streamReadConstraints(
                        tools.jackson.core.StreamReadConstraints
                            .builder()
                            .maxNestingDepth(MAX_DEPTH)
                            .maxStringLength(MAX_BYTES)
                            .build(),
                    ).build(),
            ).enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build()

    fun read(
        json: String,
        kind: String,
    ): List<spoolin.v1.Spoolin.AuditChange> =
        try {
            require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
            val root = mapper.readTree(json)
            require(root.isObject)
            val allowed = paths(kind)
            if (root.has("changes")) structured(root, allowed) else legacy(root, allowed)
        } catch (_: Exception) {
            throw Status.DATA_LOSS.withDescription("Audit projection unavailable").asRuntimeException()
        }

    private fun paths(kind: String): Set<String> =
        when (kind) {
            "TITLE" ->
                com.spoolin.catalog.operations.audit.AuditDiff.MOVIE_PATHS +
                    setOf("assetId", "attachmentId", "artworkIntent.state")
            "PERSON" -> com.spoolin.catalog.operations.audit.AuditDiff.PERSON_PATHS
            "CREDIT" -> setOf("credit")
            "ROLE" -> setOf("label", "category", "allowsCharacters")
            else -> emptySet()
        }

    private fun structured(
        root: tools.jackson.databind.JsonNode,
        allowed: Set<String>,
    ): List<spoolin.v1.Spoolin.AuditChange> {
        val changes = root["changes"]
        require(changes.isArray)
        require(!root.path("truncated").asBoolean(false))
        return changes
            .filter { it.path("path").isString && it.path("path").asString() in allowed }
            .map { change -> project(change["path"].asString(), change) }
    }

    private fun legacy(
        root: tools.jackson.databind.JsonNode,
        allowed: Set<String>,
    ): List<spoolin.v1.Spoolin.AuditChange> =
        root
            .propertyNames()
            .filter { it in allowed }
            .sorted()
            .map { path -> project(path, root[path]) }

    private fun project(
        path: String,
        value: tools.jackson.databind.JsonNode,
    ): spoolin.v1.Spoolin.AuditChange {
        require(value.isObject)
        val result =
            spoolin.v1.Spoolin.AuditChange
                .newBuilder()
                .setPath(path)
        if (value.path("valueOmitted").asBoolean(false)) {
            result.afterJson = summary(value)
        } else {
            if (value.has("before")) result.beforeJson = valueJson(path, value["before"])
            if (value.has("after")) result.afterJson = valueJson(path, value["after"])
        }
        return result.build()
    }

    private fun valueJson(
        path: String,
        value: tools.jackson.databind.JsonNode,
    ): String {
        val valid =
            when (path) {
                "countries", "genreIds" -> value.isArray && value.all { it.isString }
                "releaseYear", "releaseMonth", "releaseDay", "runtimeMinutes", "version" -> value.isIntegralNumber
                "allowsCharacters" -> value.isBoolean
                else -> value.isString
            }
        require(value.isNull || valid)
        return mapper.writeValueAsString(value)
    }

    private fun summary(value: tools.jackson.databind.JsonNode): String {
        val length = value["byteLength"]
        require(length.isIntegralNumber)
        val bytes = requireNotNull(length.asText().toLongOrNull())
        require(bytes >= 0)
        return mapper.writeValueAsString(
            mapOf(
                "valueOmitted" to true,
                "byteLength" to bytes,
                "beforeDigest" to digest(value.path("beforeDigest")),
                "afterDigest" to digest(value.path("afterDigest")),
            ),
        )
    }

    private fun digest(value: tools.jackson.databind.JsonNode): String? {
        if (value.isNull) return null
        require(value.isString && value.asString().matches(Regex("[0-9a-f]{64}")))
        return value.asString()
    }
}
