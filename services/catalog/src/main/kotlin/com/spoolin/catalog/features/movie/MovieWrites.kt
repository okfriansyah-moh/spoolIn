package com.spoolin.catalog.features.movie

import com.spoolin.catalog.features.taxonomy.write.GenreTagging
import com.spoolin.catalog.operations.audit.AuditDiff
import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.ActorRejected
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PatchValue
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.MovieWrite
import spoolin.v1.Spoolin.Outcome
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

private const val KIND_MOVIE = "MOVIE"
private const val STATUS_ACTIVE = "ACTIVE"
private const val WRITE_KIND = "WriteMovie"

/**
 * Authoritative movie create and update handler for the Catalog gRPC service.
 *
 * T016: create + update with receipt-guarded idempotency, audit and genre writes.
 * AC-016: A valid creation commits one active movie with matching audit.
 * AC-017: An update preserves unaffected fields and advances the version exactly once.
 * AC-018: Invalid or unauthorised input commits no partial record.
 * D20: domain change, audit and required work commit atomically inside [OwnerReceipts.execute].
 */
class MovieWrites(
    private val dataSource: DataSource,
    private val receipts: OwnerReceipts,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun writeMovie(request: MovieWrite, response: StreamObserver<Outcome>) {
        try {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!Permissions.management(claims.role)) throw ActorRejected()
            val operationId = parseId(request.context.operationId)
            val correlationId = parseId(request.context.correlationId)
            val actorScope = "account:${claims.accountId}"
            val isCreate = !request.hasId()
            val kind = if (isCreate) "CreateMovie" else "UpdateMovie"
            val intentFields = intentFields(request, isCreate)
            val intent = OperationIntent(operationId, actorScope, WRITE_KIND, request.context.policyRevision, intentFields)
            val outcome = receipts.execute(intent, correlationId, true) { conn ->
                if (isCreate) create(conn, request, claims.accountId, operationId, correlationId)
                else update(conn, request, claims.accountId, operationId, correlationId)
            }
            response.onNext(outcomeProto(outcome))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (e: ActorRejected) {
            response.onError(Status.PERMISSION_DENIED.withDescription("UNAUTHORISED").asRuntimeException())
        } catch (e: InputViolation) {
            val problem = spoolin.v1.Spoolin.Problem.newBuilder()
                .setCode("FIELD_INVALID")
                .addFields(spoolin.v1.Spoolin.FieldIssue.newBuilder().setPath(e.path).setCode("INVALID").setMessage(e.correction).build())
                .build()
            response.onNext(Outcome.newBuilder().setOperationId(request.context.operationId).setState("FAILED").setProblem(problem).build())
            response.onCompleted()
        }
    }

    private fun create(
        conn: Connection,
        request: MovieWrite,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val validated = MovieValidation.validateCreate(request.values)
        val genreUuids = GenreTagging.resolve(conn, validated.genreIds)
        val titleId = UUID.randomUUID()
        val now = Timestamp.from(clock.instant())
        conn.executeBound(
            "INSERT INTO title (id,kind,status,display_title,original_title,synopsis,synopsis_language," +
                "original_language,countries,release_year,release_month,release_day," +
                "runtime_minutes,version,created_at,updated_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?::text[],?,?,?,?,0,?,?)",
            titleId, KIND_MOVIE, STATUS_ACTIVE, validated.displayTitle, validated.originalTitle,
            validated.synopsis, validated.synopsisLanguage, validated.originalLanguage,
            conn.createArrayOf("text", validated.countries.toTypedArray()),
            validated.releaseYear, validated.releaseMonth, validated.releaseDay,
            validated.runtimeMinutes, now, now,
        )
        GenreTagging.replace(conn, titleId, genreUuids)
        val afterSnapshot = createSnapshot(validated, genreUuids)
        AuditInsert.record(
            conn, operationId, actorId, "HUMAN", "CREATE_MOVIE", "TITLE", titleId,
            "SUCCEEDED", AuditDiff.movie(emptyMap(), afterSnapshot), correlationId, clock.instant(),
        )
        return SafeResult(titleId, version = 0)
    }

    private fun update(
        conn: Connection,
        request: MovieWrite,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val titleId = parseId(request.id)
        val expectedVersion = request.expectedVersion
        val current = lockTitle(conn, titleId) ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        if (current.kind != KIND_MOVIE) throw Status.PERMISSION_DENIED.withDescription("TV_WRITE_DENIED").asRuntimeException()
        if (current.status != STATUS_ACTIVE) throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        if (current.version != expectedVersion) throw Status.ABORTED.withDescription("VERSION_CONFLICT").asRuntimeException()
        val patch = MovieValidation.validatePatch(request)
        val currentGenres = GenreTagging.current(conn, titleId)
        val newVersion = expectedVersion + 1
        val now = Timestamp.from(clock.instant())
        applyPatch(conn, titleId, patch, currentGenres, conn, newVersion, now)
        val afterGenres = resolveNewGenres(conn, patch, currentGenres)
        val beforeSnapshot = currentSnapshot(current, currentGenres)
        val afterSnapshot = patchedSnapshot(current, patch, afterGenres)
        AuditInsert.record(
            conn, operationId, actorId, "HUMAN", "UPDATE_MOVIE", "TITLE", titleId,
            "SUCCEEDED", AuditDiff.movie(beforeSnapshot, afterSnapshot), correlationId, clock.instant(),
        )
        return SafeResult(titleId, version = newVersion)
    }

    private fun applyPatch(
        conn: Connection,
        titleId: UUID,
        patch: ValidatedMoviePatch,
        currentGenres: Set<UUID>,
        connection: Connection,
        newVersion: Long,
        now: Timestamp,
    ) {
        val sets = mutableListOf<String>()
        val params = mutableListOf<Any?>()
        applyTextField(sets, params, patch.displayTitle, "display_title")
        applyOptTextField(sets, params, patch.originalTitle, "original_title")
        applyTextField(sets, params, patch.synopsis, "synopsis")
        applyTextField(sets, params, patch.synopsisLanguage, "synopsis_language")
        applyOptTextField(sets, params, patch.originalLanguage, "original_language")
        applyCountries(sets, params, patch.countries, connection)
        applyNullableInt(sets, params, patch.releaseYear, "release_year")
        applyNullableInt(sets, params, patch.releaseMonth, "release_month")
        applyNullableInt(sets, params, patch.releaseDay, "release_day")
        applyNullableInt(sets, params, patch.runtimeMinutes, "runtime_minutes")
        sets.add("version = ?")
        params.add(newVersion)
        sets.add("updated_at = ?")
        params.add(now)
        params.add(titleId)
        params.add(newVersion - 1)
        val updated = conn.executeBound(
            "UPDATE title SET ${sets.joinToString(",")} WHERE id = ? AND version = ?",
            *params.toTypedArray(),
        )
        check(updated == 1) { "Stale write — title was modified concurrently" }
        if (patch.genreIds is PatchValue.Value) {
            val newGenres = GenreTagging.resolve(conn, patch.genreIds.value)
            GenreTagging.replace(conn, titleId, newGenres)
        }
    }

    private fun applyTextField(sets: MutableList<String>, params: MutableList<Any?>, v: PatchValue<String>, col: String) {
        if (v is PatchValue.Value) { sets.add("$col = ?"); params.add(v.value) }
    }

    private fun applyOptTextField(sets: MutableList<String>, params: MutableList<Any?>, v: PatchValue<String>, col: String) {
        if (v is PatchValue.Value) { sets.add("$col = ?"); params.add(v.value) }
        else if (v is PatchValue.Clear) { sets.add("$col = NULL"); }
    }

    private fun applyNullableInt(sets: MutableList<String>, params: MutableList<Any?>, v: PatchValue<Int?>, col: String) {
        if (v is PatchValue.Value) { sets.add("$col = ?"); params.add(v.value) }
        else if (v is PatchValue.Clear) { sets.add("$col = NULL") }
    }

    private fun applyCountries(sets: MutableList<String>, params: MutableList<Any?>, v: PatchValue<List<String>>, conn: Connection) {
        if (v is PatchValue.Value) {
            sets.add("countries = ?::text[]")
            params.add(conn.createArrayOf("text", v.value.toTypedArray()))
        }
    }

    private fun resolveNewGenres(conn: Connection, patch: ValidatedMoviePatch, current: Set<UUID>): Set<UUID> =
        if (patch.genreIds is PatchValue.Value) GenreTagging.resolve(conn, patch.genreIds.value) else current

    // ── snapshot helpers (ATDD-298) ─────────────────────────────────────────

    private data class TitleRow(
        val kind: String, val status: String, val displayTitle: String, val originalTitle: String?,
        val synopsis: String, val synopsisLanguage: String, val originalLanguage: String?,
        val countries: List<String>, val releaseYear: Int?, val releaseMonth: Int?,
        val releaseDay: Int?, val runtimeMinutes: Int?, val version: Long,
    )

    private fun lockTitle(conn: Connection, titleId: UUID): TitleRow? =
        conn.queryBound(
            "SELECT kind,status,display_title,original_title,synopsis,synopsis_language," +
                "original_language,countries,release_year,release_month,release_day," +
                "runtime_minutes,version FROM title WHERE id = ? FOR UPDATE",
            listOf(titleId),
        ) { mapTitle(it) }.firstOrNull()

    private fun mapTitle(rs: ResultSet): TitleRow {
        val rawCountries = rs.getArray("countries")?.array
        val countries = (rawCountries as? Array<*>)?.map { it.toString() } ?: emptyList()
        return TitleRow(
            kind = rs.getString("kind"), status = rs.getString("status"),
            displayTitle = rs.getString("display_title"), originalTitle = rs.getString("original_title"),
            synopsis = rs.getString("synopsis"), synopsisLanguage = rs.getString("synopsis_language"),
            originalLanguage = rs.getString("original_language"), countries = countries,
            releaseYear = rs.getObject("release_year") as? Int, releaseMonth = rs.getObject("release_month") as? Int,
            releaseDay = rs.getObject("release_day") as? Int,
            runtimeMinutes = rs.getObject("runtime_minutes") as? Int, version = rs.getLong("version"),
        )
    }

    private fun currentSnapshot(row: TitleRow, genres: Set<UUID>): Map<String, Any?> = mapOf(
        "displayTitle" to row.displayTitle, "originalTitle" to row.originalTitle,
        "synopsis" to row.synopsis, "synopsisLanguage" to row.synopsisLanguage,
        "originalLanguage" to row.originalLanguage, "countries" to row.countries,
        "releaseYear" to row.releaseYear, "releaseMonth" to row.releaseMonth,
        "releaseDay" to row.releaseDay,
        "runtimeMinutes" to row.runtimeMinutes, "genreIds" to genres.sortedBy { it.toString() },
        "version" to row.version,
    )

    private fun createSnapshot(validated: ValidatedMovieCreate, genres: Set<UUID>): Map<String, Any?> = mapOf(
        "displayTitle" to validated.displayTitle, "originalTitle" to validated.originalTitle,
        "synopsis" to validated.synopsis, "synopsisLanguage" to validated.synopsisLanguage,
        "originalLanguage" to validated.originalLanguage, "countries" to validated.countries,
        "releaseYear" to validated.releaseYear, "releaseMonth" to validated.releaseMonth,
        "releaseDay" to validated.releaseDay,
        "runtimeMinutes" to validated.runtimeMinutes, "genreIds" to genres.sortedBy { it.toString() },
        "version" to 0L,
    )

    private fun patchedSnapshot(row: TitleRow, patch: ValidatedMoviePatch, genres: Set<UUID>): Map<String, Any?> = mapOf(
        "displayTitle" to patchValue(patch.displayTitle, row.displayTitle),
        "originalTitle" to patchValue(patch.originalTitle, row.originalTitle),
        "synopsis" to patchValue(patch.synopsis, row.synopsis),
        "synopsisLanguage" to patchValue(patch.synopsisLanguage, row.synopsisLanguage),
        "originalLanguage" to patchValue(patch.originalLanguage, row.originalLanguage),
        "countries" to patchValue(patch.countries, row.countries),
        "releaseYear" to patchValue(patch.releaseYear, row.releaseYear),
        "releaseMonth" to patchValue(patch.releaseMonth, row.releaseMonth),
        "releaseDay" to patchValue(patch.releaseDay, row.releaseDay),
        "runtimeMinutes" to patchValue(patch.runtimeMinutes, row.runtimeMinutes),
        "genreIds" to genres.sortedBy { it.toString() },
        "version" to row.version + 1,
    )

    private fun <T> patchValue(patch: PatchValue<T>, current: T): Any? =
        when (patch) {
            is PatchValue.Value -> patch.value
            is PatchValue.Clear -> null
            else -> current
        }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun intentFields(request: MovieWrite, isCreate: Boolean): List<Pair<String, String?>> {
        val fields = mutableListOf<Pair<String, String?>>()
        if (!isCreate) fields.add(Pair("id", request.id))
        fields.add(Pair("title", request.values.title))
        fields.add(Pair("synopsis", request.values.synopsis))
        fields.add(Pair("genreIds", request.values.genreIdsList.sorted().joinToString(",")))
        val year: String? = if (request.values.release.hasYear()) request.values.release.year.toString() else null
        fields.add(Pair("releaseYear", year))
        fields.add(Pair("updateMask", request.updateMask.pathsList.sorted().joinToString(",")))
        return fields
    }

    private fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }

    private fun outcomeProto(outcome: com.spoolin.support.receipts.ReceiptOutcome): Outcome {
        val b = Outcome.newBuilder()
            .setOperationId(outcome.id.toString())
            .setState(outcome.state.name)
            .setResultExpired(outcome.resultExpired)
        outcome.result?.let { r ->
            b.resultId = r.resultId.toString()
            r.version?.let { b.version = it }
        }
        outcome.errorCode?.let { b.problem = spoolin.v1.Spoolin.Problem.newBuilder().setCode(it).build() }
        return b.build()
    }
}
