@file:Suppress(
    "TooManyFunctions",
    "LongMethod",
    "CyclomaticComplexMethod",
    "ThrowsCount",
    "SwallowedException",
    "LongParameterList",
    "MaxLineLength",
)

package com.spoolin.catalog.features.credit

import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.boundary.policy.CatalogWriteIntent
import com.spoolin.catalog.boundary.policy.PolicyWrite
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
import spoolin.v1.Spoolin.CreditWrite
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.VersionedId
import java.sql.Connection
import java.sql.Timestamp
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

private const val STATUS_ACTIVE = "ACTIVE"
private const val KIND_MOVIE = "MOVIE"
private const val WRITE_KIND = "WriteCredit"
private const val REMOVE_KIND = "RemoveCredit"

class CreditWrites(
    private val receipts: OwnerReceipts,
    private val clock: Clock = Clock.systemUTC(),
    private val policy: CatalogWritePolicy = CatalogWritePolicy(),
) {
    fun writeCredit(
        request: CreditWrite,
        response: StreamObserver<Outcome>,
    ) {
        try {
            val outcome = executeCredit(request)
            response.onNext(outcomeProto(outcome))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: com.spoolin.support.receipts.ReceiptConflict) {
            response.onError(Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException())
        } catch (_: com.spoolin.support.receipts.ReceiptHidden) {
            response.onError(Status.NOT_FOUND.asRuntimeException())
        } catch (e: ActorRejected) {
            response.onError(Status.PERMISSION_DENIED.withDescription("UNAUTHORISED").asRuntimeException())
        } catch (e: InputViolation) {
            response.onNext(invalidOutcome(request.context.operationId, e))
            response.onCompleted()
        }
    }

    fun removeCredit(
        request: VersionedId,
        response: StreamObserver<Outcome>,
    ) {
        try {
            val outcome = executeRemoval(request)
            response.onNext(outcomeProto(outcome))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: com.spoolin.support.receipts.ReceiptConflict) {
            response.onError(Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException())
        } catch (_: com.spoolin.support.receipts.ReceiptHidden) {
            response.onError(Status.NOT_FOUND.asRuntimeException())
        } catch (e: ActorRejected) {
            response.onError(Status.PERMISSION_DENIED.withDescription("UNAUTHORISED").asRuntimeException())
        }
    }

    private fun executeCredit(request: CreditWrite): com.spoolin.support.receipts.ReceiptOutcome {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw ActorRejected()
        val operationId = parseId(request.context.operationId)
        val correlationId = parseId(request.context.correlationId)
        val actorScope = "account:${claims.accountId}"
        val isCreate = !request.hasId()
        val intentFields = CatalogWriteIntent.credit(request)
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
            if (!request.hasExpectedVersion() || request.expectedVersion < 0) {
                throw InputViolation("expectedVersion", "Supply the current version.")
            }
            if (isCreate) {
                create(conn, command, claims.accountId, operationId, correlationId)
            } else {
                update(conn, command, claims.accountId, operationId, correlationId)
            }
        }
    }

    private fun executeRemoval(request: VersionedId): com.spoolin.support.receipts.ReceiptOutcome {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw ActorRejected()
        val operationId = parseId(request.context.operationId)
        val correlationId = parseId(request.context.correlationId)
        val actorScope = "account:${claims.accountId}"
        val creditId = parseId(request.id)
        val expectedVersion = request.expectedVersion
        val intent =
            OperationIntent(
                operationId,
                actorScope,
                REMOVE_KIND,
                request.context.policyRevision,
                CatalogWriteIntent.remove(request),
            )
        return receipts.execute(intent, correlationId, true) { conn ->
            policy.bind(request, request.context.policyRevision)
            if (!request.hasExpectedVersion() || request.expectedVersion < 0) {
                throw Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException()
            }
            remove(conn, creditId, expectedVersion, claims.accountId, operationId, correlationId)
        }
    }

    private fun create(
        conn: Connection,
        command: PolicyWrite<CreditWrite>,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val request = command.input
        val validated = CreditValidation.validateCreate(request, command.policy)
        val titleId = parseId(request.titleId)
        val personId = parseId(validated.personId)
        val roleId = parseId(validated.roleId)
        val title = lockTitle(conn, titleId) ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        requireActiveMovie(title)
        if (title.version != request.expectedVersion) {
            throw Status.ABORTED.withDescription("VERSION_CONFLICT").asRuntimeException()
        }
        val person =
            loadPerson(conn, personId)
                ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        if (person.status != STATUS_ACTIVE) {
            throw Status.FAILED_PRECONDITION.withDescription("PERSON_ARCHIVED").asRuntimeException()
        }
        val role = loadRole(conn, roleId) ?: throw InputViolation("roleId", "Choose a known role.")
        CreditValidation.validateCharactersForRole(role.allowsCharacters, role.category, validated.characters)
        if (creditExists(conn, titleId, personId, roleId)) {
            throw Status.ALREADY_EXISTS.withDescription("CREDIT_DUPLICATE").asRuntimeException()
        }
        val creditId = UUID.randomUUID()
        val now = Timestamp.from(clock.instant())
        conn.executeBound(
            "INSERT INTO credit (id, title_id, person_id, role_id, characters, version, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?::text[], 0, ?, ?)",
            creditId,
            titleId,
            personId,
            roleId,
            textArray(conn, validated.characters),
            now,
            now,
        )
        bumpTitleAndPerson(conn, titleId, personId, title.version + 1, now)
        AuditInsert.record(
            conn,
            operationId,
            actorId,
            "HUMAN",
            "ADD_CREDIT",
            "CREDIT",
            creditId,
            "SUCCEEDED",
            """{"changes":[{"path":"credit","before":null,"after":"created"}],"summarised":false}""",
            correlationId,
            clock.instant(),
        )
        return SafeResult(creditId, version = 0)
    }

    private fun update(
        conn: Connection,
        command: PolicyWrite<CreditWrite>,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val request = command.input
        val creditId = parseId(request.id)
        val expectedVersion = request.expectedVersion
        val current =
            lockCredit(conn, creditId)
                ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        if (current.version != expectedVersion) {
            throw Status.ABORTED.withDescription("VERSION_CONFLICT").asRuntimeException()
        }
        val title =
            lockTitle(conn, current.titleId)
                ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        requireActiveMovie(title)
        val person =
            loadPerson(conn, current.personId)
                ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        if (person.status != STATUS_ACTIVE) {
            throw Status.FAILED_PRECONDITION.withDescription("PERSON_ARCHIVED").asRuntimeException()
        }
        val patch = CreditValidation.validatePatch(request, command.policy)
        val roleId =
            when (patch.roleId) {
                is PatchValue.Value -> parseId(patch.roleId.value)
                else -> current.roleId
            }
        val characters =
            when (patch.characters) {
                is PatchValue.Value -> patch.characters.value
                else -> current.characters
            }
        val role = loadRole(conn, roleId) ?: throw InputViolation("roleId", "Choose a known role.")
        CreditValidation.validateCharactersForRole(role.allowsCharacters, role.category, characters)
        if (roleId != current.roleId && creditExists(conn, current.titleId, current.personId, roleId)) {
            throw Status.ALREADY_EXISTS.withDescription("CREDIT_DUPLICATE").asRuntimeException()
        }
        val newVersion = expectedVersion + 1
        val now = Timestamp.from(clock.instant())
        conn.executeBound(
            "UPDATE credit SET role_id = ?, characters = ?::text[], version = ?, updated_at = ? " +
                "WHERE id = ? AND version = ?",
            roleId,
            textArray(conn, characters),
            newVersion,
            now,
            creditId,
            expectedVersion,
        )
        bumpTitleAndPerson(conn, current.titleId, current.personId, title.version + 1, now)
        AuditInsert.record(
            conn,
            operationId,
            actorId,
            "HUMAN",
            "UPDATE_CREDIT",
            "CREDIT",
            creditId,
            "SUCCEEDED",
            """{"changes":[{"path":"credit","before":"v$expectedVersion","after":"v$newVersion"}],"summarised":false}""",
            correlationId,
            clock.instant(),
        )
        return SafeResult(creditId, version = newVersion)
    }

    private fun remove(
        conn: Connection,
        creditId: UUID,
        expectedVersion: Long,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val current =
            lockCredit(conn, creditId)
                ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        if (current.version != expectedVersion) {
            throw Status.ABORTED.withDescription("VERSION_CONFLICT").asRuntimeException()
        }
        val title =
            lockTitle(conn, current.titleId)
                ?: throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException()
        requireActiveMovie(title)
        val person = loadPerson(conn, current.personId)
        if (person != null && person.status != STATUS_ACTIVE) {
            // Retained archived-person credit may be removed from an active movie.
        }
        val now = Timestamp.from(clock.instant())
        conn.executeBound("DELETE FROM credit WHERE id = ? AND version = ?", creditId, expectedVersion)
        bumpTitleAndPerson(conn, current.titleId, current.personId, title.version + 1, now)
        AuditInsert.record(
            conn,
            operationId,
            actorId,
            "HUMAN",
            "REMOVE_CREDIT",
            "CREDIT",
            creditId,
            "SUCCEEDED",
            """{"changes":[{"path":"credit","before":"present","after":null}],"summarised":false}""",
            correlationId,
            clock.instant(),
        )
        return SafeResult(creditId, version = expectedVersion + 1)
    }

    private fun bumpTitleAndPerson(
        conn: Connection,
        titleId: UUID,
        personId: UUID,
        titleVersion: Long,
        now: Timestamp,
    ) {
        conn.executeBound(
            "UPDATE title SET version = ?, updated_at = ? WHERE id = ?",
            titleVersion,
            now,
            titleId,
        )
        conn.executeBound(
            "UPDATE person SET impact_version = impact_version + 1, updated_at = ? WHERE id = ?",
            now,
            personId,
        )
    }

    private data class TitleRow(
        val kind: String,
        val status: String,
        val version: Long,
    )

    private data class PersonRow(
        val status: String,
    )

    private data class RoleRow(
        val category: String,
        val allowsCharacters: Boolean,
    )

    private data class CreditRow(
        val titleId: UUID,
        val personId: UUID,
        val roleId: UUID,
        val characters: List<String>,
        val version: Long,
    )

    private fun requireActiveMovie(title: TitleRow) {
        if (title.status != STATUS_ACTIVE) throw Status.FAILED_PRECONDITION.withDescription("TITLE_ARCHIVED").asRuntimeException()
        if (title.kind != KIND_MOVIE) throw Status.FAILED_PRECONDITION.withDescription("TV_CREDIT_DENIED").asRuntimeException()
    }

    private fun lockTitle(
        conn: Connection,
        titleId: UUID,
    ): TitleRow? =
        conn
            .queryBound(
                "SELECT kind, status, version FROM title WHERE id = ? FOR UPDATE",
                listOf(titleId),
            ) { rs ->
                TitleRow(rs.getString("kind"), rs.getString("status"), rs.getLong("version"))
            }.firstOrNull()

    private fun loadPerson(
        conn: Connection,
        personId: UUID,
    ): PersonRow? =
        conn
            .queryBound(
                "SELECT status FROM person WHERE id = ?",
                listOf(personId),
            ) { rs -> PersonRow(rs.getString("status")) }
            .firstOrNull()

    private fun loadRole(
        conn: Connection,
        roleId: UUID,
    ): RoleRow? =
        conn
            .queryBound(
                "SELECT category, allows_characters FROM credit_role WHERE id = ?",
                listOf(roleId),
            ) { rs ->
                RoleRow(rs.getString("category"), rs.getBoolean("allows_characters"))
            }.firstOrNull()

    private fun lockCredit(
        conn: Connection,
        creditId: UUID,
    ): CreditRow? =
        conn
            .queryBound(
                "SELECT title_id, person_id, role_id, characters, version FROM credit WHERE id = ? FOR UPDATE",
                listOf(creditId),
            ) { rs ->
                val raw = rs.getArray("characters")?.array
                val characters = (raw as? Array<*>)?.map { it.toString() } ?: emptyList()
                CreditRow(
                    rs.getObject("title_id", UUID::class.java),
                    rs.getObject("person_id", UUID::class.java),
                    rs.getObject("role_id", UUID::class.java),
                    characters,
                    rs.getLong("version"),
                )
            }.firstOrNull()

    private fun creditExists(
        conn: Connection,
        titleId: UUID,
        personId: UUID,
        roleId: UUID,
    ): Boolean =
        conn
            .queryBound(
                "SELECT 1 FROM credit WHERE title_id = ? AND person_id = ? AND role_id = ? LIMIT 1",
                listOf(titleId, personId, roleId),
            ) { rs -> rs.next() }
            .isNotEmpty()

    private fun textArray(
        conn: Connection,
        values: List<String>,
    ): java.sql.Array = conn.createArrayOf("text", values.toTypedArray())

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
        outcome.errorCode?.let {
            b.problem =
                spoolin.v1.Spoolin.Problem
                    .newBuilder()
                    .setCode(it)
                    .build()
        }
        return b.build()
    }
}

private fun invalidOutcome(operation: String, violation: InputViolation): Outcome = Outcome.newBuilder()
    .setOperationId(operation).setState("FAILED")
    .setProblem(spoolin.v1.Spoolin.Problem.newBuilder().setCode("FIELD_INVALID")
        .addFields(spoolin.v1.Spoolin.FieldIssue.newBuilder().setPath(violation.path).setCode("INVALID")
            .setMessage(violation.correction)))
    .build()
