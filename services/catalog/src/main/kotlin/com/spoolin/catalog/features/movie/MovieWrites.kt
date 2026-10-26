@file:Suppress(
    "TooManyFunctions",
    "ThrowsCount",
    "SwallowedException",
    "SpreadOperator",
    "MaxLineLength",
    "LongParameterList",
)

package com.spoolin.catalog.features.movie

import com.spoolin.catalog.boundary.grpc.CatalogWriteContext
import com.spoolin.catalog.boundary.grpc.CatalogWriteExecution
import com.spoolin.catalog.boundary.grpc.CatalogWriteRpc
import com.spoolin.catalog.boundary.grpc.CatalogWriteVersion
import com.spoolin.catalog.boundary.policy.CatalogWriteIntent
import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.boundary.policy.PolicyWrite
import com.spoolin.catalog.features.search.TitleContent
import com.spoolin.catalog.features.taxonomy.write.GenreTagging
import com.spoolin.catalog.operations.audit.AuditDiff
import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.catalog.operations.audit.auditPatchedValue
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.policy.PatchValue
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.MovieValues
import spoolin.v1.Spoolin.MovieWrite
import spoolin.v1.Spoolin.Outcome
import java.sql.Connection
import java.sql.Timestamp
import java.time.Clock
import java.util.UUID

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
    private val receipts: OwnerReceipts,
    private val clock: Clock = Clock.systemUTC(),
    private val policy: CatalogWritePolicy = CatalogWritePolicy(),
) {
    /** Authoritative movie insert used by trusted proposal approval (same path as WriteMovie create). */
    fun publishFromProposal(
        conn: Connection,
        values: MovieValues,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val request = MovieWrite.newBuilder().setValues(values).build()
        return create(conn, PolicyWrite(request, policy.effective()), actorId, operationId, correlationId)
    }

    fun writeMovie(
        request: MovieWrite,
        response: StreamObserver<Outcome>,
    ) {
        CatalogWriteRpc.fields(request.context.operationId, response) { executeMovie(request) }
    }

    private fun executeMovie(request: MovieWrite): com.spoolin.support.receipts.ReceiptOutcome {
        val context = CatalogWriteContext.management(request.context)
        val isCreate = !request.hasId()
        val intent = context.intent(WRITE_KIND, CatalogWriteIntent.movie(request))
        return CatalogWriteExecution(receipts, context).execute(
            intent,
            CatalogWriteVersion(isCreate, request.hasExpectedVersion(), request.expectedVersion),
            bind = { policy.bind(request, request.context.policyRevision) },
            create = ::create,
            update = ::update,
        )
    }

    private fun create(
        conn: Connection,
        command: PolicyWrite<MovieWrite>,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val request = command.input
        val validated = MovieValidation.validateCreate(request.values, command.policy)
        val genreUuids = GenreTagging.resolve(conn, validated.genreIds)
        val titleId = UUID.randomUUID()
        val now = Timestamp.from(clock.instant())
        conn.executeBound(
            "INSERT INTO title (id,kind,status,display_title,original_title,synopsis,synopsis_language," +
                "original_language,countries,release_year,release_month,release_day," +
                "runtime_minutes,version,created_at,updated_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?::text[],?,?,?,?,0,?,?)",
            titleId,
            KIND_MOVIE,
            STATUS_ACTIVE,
            validated.displayTitle,
            validated.originalTitle,
            validated.synopsis,
            validated.synopsisLanguage,
            validated.originalLanguage,
            conn.createArrayOf("text", validated.countries.toTypedArray()),
            validated.releaseYear,
            validated.releaseMonth,
            validated.releaseDay,
            validated.runtimeMinutes,
            now,
            now,
        )
        GenreTagging.replace(conn, titleId, genreUuids)
        val afterSnapshot = createSnapshot(validated, genreUuids)
        AuditInsert.record(
            conn,
            operationId,
            actorId,
            "HUMAN",
            "CREATE_MOVIE",
            "TITLE",
            titleId,
            "SUCCEEDED",
            AuditDiff.movie(emptyMap(), afterSnapshot),
            correlationId,
            clock.instant(),
        )
        return SafeResult(titleId, version = 0)
    }

    private fun update(
        conn: Connection,
        command: PolicyWrite<MovieWrite>,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val request = command.input
        val titleId = CatalogWriteRpc.id(request.id)
        val expectedVersion = request.expectedVersion
        val current =
            lockTitle(conn, titleId) ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        if (current.kind !=
            KIND_MOVIE
        ) {
            throw Status.PERMISSION_DENIED.withDescription("TV_WRITE_DENIED").asRuntimeException()
        }
        if (current.status != STATUS_ACTIVE) throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        if (current.version !=
            expectedVersion
        ) {
            throw Status.ABORTED.withDescription("VERSION_CONFLICT").asRuntimeException()
        }
        val patch = MovieValidation.validatePatch(request, command.policy)
        val currentGenres = GenreTagging.current(conn, titleId)
        val newVersion = expectedVersion + 1
        val now = Timestamp.from(clock.instant())
        applyPatch(conn, titleId, patch, conn, newVersion, now)
        val afterGenres = resolveNewGenres(conn, patch, currentGenres)
        val beforeSnapshot = currentSnapshot(current, currentGenres)
        val afterSnapshot = patchedSnapshot(current, patch, afterGenres)
        AuditInsert.record(
            conn,
            operationId,
            actorId,
            "HUMAN",
            "UPDATE_MOVIE",
            "TITLE",
            titleId,
            "SUCCEEDED",
            AuditDiff.movie(beforeSnapshot, afterSnapshot),
            correlationId,
            clock.instant(),
        )
        return SafeResult(titleId, version = newVersion)
    }

    private fun applyPatch(
        conn: Connection,
        titleId: UUID,
        patch: ValidatedMoviePatch,
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
        val updated =
            conn.executeBound(
                "UPDATE title SET ${sets.joinToString(",")} WHERE id = ? AND version = ?",
                *params.toTypedArray(),
            )
        check(updated == 1) { "Stale write — title was modified concurrently" }
        if (patch.genreIds is PatchValue.Value) {
            val newGenres = GenreTagging.resolve(conn, patch.genreIds.value)
            GenreTagging.replace(conn, titleId, newGenres)
        }
    }

    private fun applyTextField(
        sets: MutableList<String>,
        params: MutableList<Any?>,
        v: PatchValue<String>,
        col: String,
    ) {
        if (v is PatchValue.Value) {
            sets.add("$col = ?")
            params.add(v.value)
        }
    }

    private fun applyOptTextField(
        sets: MutableList<String>,
        params: MutableList<Any?>,
        v: PatchValue<String>,
        col: String,
    ) {
        if (v is PatchValue.Value) {
            sets.add("$col = ?")
            params.add(v.value)
        } else if (v is PatchValue.Clear) {
            sets.add("$col = NULL")
        }
    }

    private fun applyNullableInt(
        sets: MutableList<String>,
        params: MutableList<Any?>,
        v: PatchValue<Int?>,
        col: String,
    ) {
        if (v is PatchValue.Value) {
            sets.add("$col = ?")
            params.add(v.value)
        } else if (v is PatchValue.Clear) {
            sets.add("$col = NULL")
        }
    }

    private fun applyCountries(
        sets: MutableList<String>,
        params: MutableList<Any?>,
        v: PatchValue<List<String>>,
        conn: Connection,
    ) {
        if (v is PatchValue.Value) {
            sets.add("countries = ?::text[]")
            params.add(conn.createArrayOf("text", v.value.toTypedArray()))
        }
    }

    private fun resolveNewGenres(
        conn: Connection,
        patch: ValidatedMoviePatch,
        current: Set<UUID>,
    ): Set<UUID> = if (patch.genreIds is PatchValue.Value) GenreTagging.resolve(conn, patch.genreIds.value) else current

    // ── snapshot helpers (ATDD-298) ─────────────────────────────────────────

    private fun lockTitle(
        conn: Connection,
        titleId: UUID,
    ): TitleContent? =
        conn
            .queryBound(
                "SELECT kind,status,display_title,original_title,synopsis,synopsis_language," +
                    "original_language,countries,release_year,release_month,release_day," +
                    "runtime_minutes,version FROM title WHERE id = ? FOR UPDATE",
                listOf(titleId),
            ) { TitleContent.read(it) }
            .firstOrNull()

    private fun currentSnapshot(
        row: TitleContent,
        genres: Set<UUID>,
    ): Map<String, Any?> =
        mapOf(
            "displayTitle" to row.displayTitle,
            "originalTitle" to row.originalTitle,
            "synopsis" to row.synopsis,
            "synopsisLanguage" to row.synopsisLanguage,
            "originalLanguage" to row.originalLanguage,
            "countries" to row.countries,
            "releaseYear" to row.releaseYear,
            "releaseMonth" to row.releaseMonth,
            "releaseDay" to row.releaseDay,
            "runtimeMinutes" to row.runtimeMinutes,
            "genreIds" to genres.sortedBy { it.toString() },
            "version" to row.version,
        )

    private fun createSnapshot(
        validated: ValidatedMovieCreate,
        genres: Set<UUID>,
    ): Map<String, Any?> =
        mapOf(
            "displayTitle" to validated.displayTitle,
            "originalTitle" to validated.originalTitle,
            "synopsis" to validated.synopsis,
            "synopsisLanguage" to validated.synopsisLanguage,
            "originalLanguage" to validated.originalLanguage,
            "countries" to validated.countries,
            "releaseYear" to validated.releaseYear,
            "releaseMonth" to validated.releaseMonth,
            "releaseDay" to validated.releaseDay,
            "runtimeMinutes" to validated.runtimeMinutes,
            "genreIds" to genres.sortedBy { it.toString() },
            "version" to 0L,
        )

    private fun patchedSnapshot(
        row: TitleContent,
        patch: ValidatedMoviePatch,
        genres: Set<UUID>,
    ): Map<String, Any?> =
        mapOf(
            "displayTitle" to auditPatchedValue(patch.displayTitle, row.displayTitle),
            "originalTitle" to auditPatchedValue(patch.originalTitle, row.originalTitle),
            "synopsis" to auditPatchedValue(patch.synopsis, row.synopsis),
            "synopsisLanguage" to auditPatchedValue(patch.synopsisLanguage, row.synopsisLanguage),
            "originalLanguage" to auditPatchedValue(patch.originalLanguage, row.originalLanguage),
            "countries" to auditPatchedValue(patch.countries, row.countries),
            "releaseYear" to auditPatchedValue(patch.releaseYear, row.releaseYear),
            "releaseMonth" to auditPatchedValue(patch.releaseMonth, row.releaseMonth),
            "releaseDay" to auditPatchedValue(patch.releaseDay, row.releaseDay),
            "runtimeMinutes" to auditPatchedValue(patch.runtimeMinutes, row.runtimeMinutes),
            "genreIds" to genres.sortedBy { it.toString() },
            "version" to row.version + 1,
        )
}
