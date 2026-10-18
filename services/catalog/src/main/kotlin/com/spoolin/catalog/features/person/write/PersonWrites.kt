@file:Suppress("TooManyFunctions", "LongMethod", "ThrowsCount", "SwallowedException", "SpreadOperator")

package com.spoolin.catalog.features.person.write

import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.boundary.policy.CatalogWriteIntent
import com.spoolin.catalog.boundary.policy.PolicyWrite
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
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.PersonWrite
import spoolin.v1.Spoolin.Problem
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

private const val STATUS_ACTIVE = "ACTIVE"
private const val STATUS_ARCHIVED = "ARCHIVED"
private const val WRITE_KIND = "WritePerson"

/**
 * Authoritative person create and update handler for the Catalog gRPC service.
 *
 * T018: createPerson + updatePerson with receipt-guarded idempotency, audit and name/biography writes.
 * PRD-BOUND-01 / AC-140: name 1–100 code points; 0 and 101 reject with field error, no partial save.
 * AC-023: Preserve one shared person identity; reject archived without restoration.
 * AC-104: Stale-version conflict produces VERSION_CONFLICT; draft preserved for review.
 * D09: Optimistic concurrency; version increments on each domain change.
 */
class PersonWrites(
    private val receipts: OwnerReceipts,
    private val clock: Clock = Clock.systemUTC(),
    private val policy: CatalogWritePolicy = CatalogWritePolicy(),
) {
    fun writePerson(
        request: PersonWrite,
        response: StreamObserver<Outcome>,
    ) {
        try {
            val outcome = executePerson(request)
            response.onNext(outcomeProto(outcome))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: com.spoolin.support.receipts.ReceiptConflict) {
            response.onError(Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException())
        } catch (_: com.spoolin.support.receipts.ReceiptHidden) {
            response.onError(Status.NOT_FOUND.asRuntimeException())
        } catch (e: ActorRejected) {
            response.onError(
                Status.PERMISSION_DENIED.withDescription("UNAUTHORISED").asRuntimeException(),
            )
        } catch (e: InputViolation) {
            response.onNext(invalidOutcome(request.context.operationId, e))
            response.onCompleted()
        }
    }

    private fun executePerson(request: PersonWrite): com.spoolin.support.receipts.ReceiptOutcome {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw ActorRejected()
        val operationId = parseId(request.context.operationId)
        val correlationId = parseId(request.context.correlationId)
        val actorScope = "account:${claims.accountId}"
        val isCreate = !request.hasId()
        val intentFields = CatalogWriteIntent.person(request)
        val intent =
            OperationIntent(
                operationId,
                actorScope,
                WRITE_KIND,
                request.context.policyRevision,
                intentFields,
            )
        return receipts.execute(intent, correlationId, true) { conn ->
            val command = policy.bind(request, request.context.policyRevision)
            if (!isCreate && (!request.hasExpectedVersion() || request.expectedVersion < 0)) {
                throw InputViolation("expectedVersion", "Supply the current version.")
            }
            if (isCreate) {
                create(conn, command, claims.accountId, operationId, correlationId)
            } else {
                update(conn, command, claims.accountId, operationId, correlationId)
            }
        }
    }

    private fun create(
        conn: Connection,
        command: PolicyWrite<PersonWrite>,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val request = command.input
        val validated = PersonValidation.validateCreate(request.values, command.policy)
        val personId = UUID.randomUUID()
        val now = Timestamp.from(clock.instant())
        conn.executeBound(
            "INSERT INTO person (id, name, biography, status, impact_version, version, created_at, updated_at)" +
                " VALUES (?, ?, ?, 'ACTIVE', 0, 0, ?, ?)",
            personId,
            validated.name,
            validated.biography,
            now,
            now,
        )
        val afterSnapshot = createSnapshot(validated)
        AuditInsert.record(
            conn,
            operationId,
            actorId,
            "HUMAN",
            "CREATE_PERSON",
            "PERSON",
            personId,
            "SUCCEEDED",
            AuditDiff.person(emptyMap(), afterSnapshot),
            correlationId,
            clock.instant(),
        )
        return SafeResult(personId, version = 0)
    }

    private fun update(
        conn: Connection,
        command: PolicyWrite<PersonWrite>,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val request = command.input
        val personId = parseId(request.id)
        val expectedVersion = request.expectedVersion
        val current =
            lockPerson(conn, personId)
                ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        if (current.status == STATUS_ARCHIVED) {
            throw Status.FAILED_PRECONDITION.withDescription("PERSON_ARCHIVED").asRuntimeException()
        }
        if (current.status != STATUS_ACTIVE) {
            throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        }
        if (current.version != expectedVersion) {
            throw Status.ABORTED.withDescription("VERSION_CONFLICT").asRuntimeException()
        }
        val patch = PersonValidation.validatePatch(request, command.policy)
        val newVersion = expectedVersion + 1
        val now = Timestamp.from(clock.instant())
        applyPatch(conn, personId, patch, newVersion, now)
        val beforeSnapshot = currentSnapshot(current)
        val afterSnapshot = patchedSnapshot(current, patch)
        AuditInsert.record(
            conn,
            operationId,
            actorId,
            "HUMAN",
            "UPDATE_PERSON",
            "PERSON",
            personId,
            "SUCCEEDED",
            AuditDiff.person(beforeSnapshot, afterSnapshot),
            correlationId,
            clock.instant(),
        )
        return SafeResult(personId, version = newVersion)
    }

    private fun applyPatch(
        conn: Connection,
        personId: UUID,
        patch: ValidatedPersonPatch,
        newVersion: Long,
        now: Timestamp,
    ) {
        val sets = mutableListOf<String>()
        val params = mutableListOf<Any?>()
        if (patch.name is PatchValue.Value) {
            sets.add("name = ?")
            params.add(patch.name.value)
        }
        when (patch.biography) {
            is PatchValue.Value -> {
                sets.add("biography = ?")
                params.add(patch.biography.value)
            }
            is PatchValue.Clear -> sets.add("biography = NULL")
            else -> {}
        }
        sets.add("version = ?")
        params.add(newVersion)
        sets.add("updated_at = ?")
        params.add(now)
        params.add(personId)
        params.add(newVersion - 1)
        val updated =
            conn.executeBound(
                "UPDATE person SET ${sets.joinToString(",")} WHERE id = ? AND version = ?",
                *params.toTypedArray(),
            )
        check(updated == 1) { "Stale write — person was modified concurrently" }
    }

    // ── snapshot helpers ─────────────────────────────────────────────────────

    private data class PersonRow(
        val name: String,
        val biography: String?,
        val status: String,
        val version: Long,
    )

    private fun lockPerson(
        conn: Connection,
        personId: UUID,
    ): PersonRow? =
        conn
            .queryBound(
                "SELECT name, biography, status, version FROM person WHERE id = ? FOR UPDATE",
                listOf(personId),
            ) { rs -> mapPersonRow(rs) }
            .firstOrNull()

    private fun mapPersonRow(rs: ResultSet) =
        PersonRow(
            name = rs.getString("name"),
            biography = rs.getString("biography"),
            status = rs.getString("status"),
            version = rs.getLong("version"),
        )

    private fun createSnapshot(validated: ValidatedPersonCreate): Map<String, Any?> =
        mapOf(
            "name" to validated.name,
            "biography" to validated.biography,
            "status" to STATUS_ACTIVE,
            "version" to 0L,
        )

    private fun currentSnapshot(row: PersonRow): Map<String, Any?> =
        mapOf(
            "name" to row.name,
            "biography" to row.biography,
            "status" to row.status,
            "version" to row.version,
        )

    private fun patchedSnapshot(
        row: PersonRow,
        patch: ValidatedPersonPatch,
    ): Map<String, Any?> =
        mapOf(
            "name" to patchValue(patch.name, row.name),
            "biography" to patchValue(patch.biography, row.biography),
            "status" to row.status,
            "version" to row.version + 1,
        )

    private fun <T> patchValue(
        patch: PatchValue<T>,
        current: T,
    ): Any? =
        when (patch) {
            is PatchValue.Value -> patch.value
            is PatchValue.Clear -> null
            else -> current
        }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }

    private fun outcomeProto(outcome: com.spoolin.support.receipts.ReceiptOutcome): Outcome {
        val b =
            Outcome
                .newBuilder()
                .setOperationId(outcome.id.toString())
                .setState(outcome.state.name)
                .setResultExpired(outcome.resultExpired)
        outcome.result?.let { r ->
            b.resultId = r.resultId.toString()
            r.version?.let { b.version = it }
        }
        outcome.errorCode?.let { b.problem = Problem.newBuilder().setCode(it).build() }
        return b.build()
    }
}

private fun invalidOutcome(operation: String, violation: InputViolation): Outcome = Outcome.newBuilder()
    .setOperationId(operation).setState("FAILED")
    .setProblem(spoolin.v1.Spoolin.Problem.newBuilder().setCode("FIELD_INVALID")
        .addFields(spoolin.v1.Spoolin.FieldIssue.newBuilder().setPath(violation.path).setCode("INVALID")
            .setMessage(violation.correction)))
    .build()
