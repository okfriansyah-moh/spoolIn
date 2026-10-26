@file:Suppress("SwallowedException")

package com.spoolin.catalog.features.taxonomy.roles

import com.spoolin.catalog.boundary.grpc.CatalogWriteContext
import com.spoolin.catalog.boundary.grpc.CatalogWriteRpc
import com.spoolin.catalog.boundary.policy.CatalogWriteIntent
import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.operations.audit.AuditDiff
import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.policy.InputViolation
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

private const val WRITE_KIND = "CreateRole"

/**
 * Custom contribution role creation (T020).
 * Starter Actor permits characters; custom roles default to allowsCharacters=false.
 * CREATOR category forbids characters on credits at validation time.
 */
class RoleWrites(
    private val receipts: OwnerReceipts,
    private val clock: Clock = Clock.systemUTC(),
    private val policy: CatalogWritePolicy = CatalogWritePolicy(),
) {
    fun createRole(
        request: RoleWrite,
        response: StreamObserver<Outcome>,
    ) {
        try {
            CatalogWriteRpc.receipt(response) { executeRole(request) }
        } catch (_: InputViolation) {
            response.onError(Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException())
        }
    }

    private fun executeRole(request: RoleWrite): com.spoolin.support.receipts.ReceiptOutcome {
        val context = CatalogWriteContext.management(request.context)
        val intent = context.intent(WRITE_KIND, CatalogWriteIntent.role(request))
        return receipts.execute(intent, context.correlation, true) { conn ->
            val effective = policy.bind(request, request.context.policyRevision).policy
            val validated = RoleValidation.validateCreate(request.category, request.label, effective)
            create(conn, validated, context.actor, context.operation, context.correlation)
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
            AuditDiff.compute(emptyMap(), mapOf("label" to validated.label), setOf("label")),
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

}
