@file:Suppress("SwallowedException")

package com.spoolin.catalog.features.taxonomy.roles

import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.ActorRejected
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.RoleWrite
import java.sql.Connection
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

private const val WRITE_KIND = "CreateRole"

/**
 * Custom contribution role creation (T020).
 * Starter Actor permits characters; custom roles default to allowsCharacters=false.
 * CREATOR category forbids characters on credits at validation time.
 */
class RoleWrites(
    private val receipts: OwnerReceipts,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun createRole(
        request: RoleWrite,
        response: StreamObserver<Outcome>,
    ) {
        try {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!Permissions.management(claims.role)) throw ActorRejected()
            val operationId = parseId(request.context.operationId)
            val correlationId = parseId(request.context.correlationId)
            val actorScope = "account:${claims.accountId}"
            val validated = RoleValidation.validateCreate(request.category, request.label)
            val intent =
                OperationIntent(
                    operationId,
                    actorScope,
                    WRITE_KIND,
                    request.context.policyRevision,
                    listOf("category" to validated.category, "label" to validated.label),
                )
            val outcome =
                receipts.execute(intent, correlationId, true) { conn ->
                    create(conn, validated, claims.accountId, operationId, correlationId)
                }
            response.onNext(outcomeProto(outcome))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (e: ActorRejected) {
            response.onError(Status.PERMISSION_DENIED.withDescription("UNAUTHORISED").asRuntimeException())
        } catch (e: InputViolation) {
            response.onError(Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException())
        }
    }

    private fun create(
        conn: Connection,
        validated: ValidatedRoleCreate,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        if (roleExists(conn, validated.label)) {
            throw Status.ALREADY_EXISTS.withDescription("ROLE_EXISTS").asRuntimeException()
        }
        val roleId = UUID.randomUUID()
        conn.executeBound(
            "INSERT INTO credit_role (id, category, label, normalized_label, allows_characters) " +
                "VALUES (?, ?, ?, casefold(?), false)",
            roleId,
            validated.category,
            validated.label,
            validated.label,
        )
        AuditInsert.record(
            conn,
            operationId,
            actorId,
            "HUMAN",
            "CREATE_ROLE",
            "ROLE",
            roleId,
            "SUCCEEDED",
            """{"changes":[{"path":"label","before":null,"after":"${validated.label}"}],"summarised":false}""",
            correlationId,
            clock.instant(),
        )
        return SafeResult(roleId, version = 0)
    }

    private fun roleExists(
        conn: Connection,
        label: String,
    ): Boolean =
        conn
            .queryBound(
                "SELECT 1 FROM credit_role WHERE normalized_label = casefold(?) LIMIT 1",
                listOf(label),
            ) { rs -> rs.next() }
            .isNotEmpty()

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
