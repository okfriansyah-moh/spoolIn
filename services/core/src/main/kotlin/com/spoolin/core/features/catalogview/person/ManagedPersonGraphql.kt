package com.spoolin.core.features.catalogview.person

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.ActorTarget
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.receipts.ReceiptState
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DEADLINE_SECONDS = 5L
private const val DEFAULT_FIRST = 24
private const val MAX_PAGE = 100

/**
 * GraphQL resolvers for Maintainer-only person mutations and managed reads.
 *
 * Mutations: createPerson, updatePerson.
 * Queries:   managedPerson, managedPeople.
 *
 * F-PERMISSIONS: each operation requires an authenticated MAINTAINER session.
 * ATDD-251-T1-R12: managedPerson — private active/archived person projection.
 * ATDD-251-T1-R15: managedPeople — explicit ACTIVE/ARCHIVED state; name/UUID order.
 * ATDD-251-T1-R26: createPerson — required Unicode name; optional biography; independent identity.
 * ATDD-251-T1-R27: updatePerson — active person/current version; changed fields only.
 * ATDD-253-T1-R17: WritePerson(PersonWrite) → Outcome.
 */
class ManagedPersonGraphql(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val issuer: ActorIssuer?,
) {

    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q ->
            q.dataFetcher("managedPerson") { env -> managedPerson(env) }
             .dataFetcher("managedPeople") { env -> managedPeople(env) }
        }
        wiring.type("Mutation") { m ->
            m.dataFetcher("createPerson") { env -> createPerson(env) }
             .dataFetcher("updatePerson") { env -> updatePerson(env) }
        }
    }

    // ── createPerson ──────────────────────────────────────────────────────────

    private fun createPerson(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val input = requireNotNull(env.getArgument<Map<String, Any?>>("input"))
        val values = personValues(input)
        val request = Spoolin.PersonWrite.newBuilder()
            .setContext(grpcContext(operationId, context.correlation, policyRevision))
            .setValues(values)
            .build()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/WritePerson", operationId, context.correlation)
        val outcome = stub(token).writePerson(request)
        val result = outcomeMap(outcome)
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CATALOG, result)
        return mutationResult(outcome)
    }

    // ── updatePerson ──────────────────────────────────────────────────────────

    private fun updatePerson(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val personId = requireNotNull(env.getArgument<String>("id"))
        validateUuid(personId)
        val expectedVersion = requireNotNull(env.getArgument<Long>("expectedVersion"))
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val patch = requireNotNull(env.getArgument<Map<String, Any?>>("patch"))
        val (values, mask) = personPatch(patch)
        val request = Spoolin.PersonWrite.newBuilder()
            .setContext(grpcContext(operationId, context.correlation, policyRevision))
            .setId(personId)
            .setExpectedVersion(expectedVersion)
            .setValues(values)
            .setUpdateMask(mask)
            .build()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/WritePerson", operationId, context.correlation)
        val outcome = stub(token).writePerson(request)
        val result = outcomeMap(outcome)
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CATALOG, result)
        return mutationResult(outcome)
    }

    // ── managedPerson ─────────────────────────────────────────────────────────

    private fun managedPerson(env: DataFetchingEnvironment): Map<String, Any?>? {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val personId = requireNotNull(env.getArgument<String>("id"))
        validateUuid(personId)
        val opId = UUID.randomUUID()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/ManagedPerson", opId, context.correlation)
        val result = stub(token).managedPerson(Spoolin.Id.newBuilder().setId(personId).build())
        if (result.id.isEmpty()) return null
        return personMap(result)
    }

    // ── managedPeople ─────────────────────────────────────────────────────────

    private fun managedPeople(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val state = requireNotNull(env.getArgument<String>("state"))
        val first = (env.getArgument<Int>("first") ?: DEFAULT_FIRST).coerceIn(1, MAX_PAGE)
        val after = env.getArgument<String>("after")
        val opId = UUID.randomUUID()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/ManagedPeople", opId, context.correlation)
        val request = Spoolin.PageRequest.newBuilder().setState(state).setFirst(first)
            .also { b -> after?.let { b.after = it } }.build()
        val result = stub(token).managedPeople(request)
        return personPageMap(result)
    }

    // ── proto → GraphQL maps ──────────────────────────────────────────────────

    private fun personPageMap(proto: Spoolin.PersonPage): Map<String, Any?> = mapOf(
        "nodes" to proto.nodesList.map { personMap(it) },
        "pageInfo" to mapOf(
            "endCursor" to proto.endCursor.takeIf { it.isNotEmpty() },
            "hasNextPage" to proto.hasNextPage,
        ),
    )

    private fun personMap(p: Spoolin.Person): Map<String, Any?> = mapOf(
        "id" to p.id,
        "name" to p.values.name,
        "biography" to p.values.biography.takeIf { it.isNotEmpty() },
        "state" to p.state,
        "version" to p.version.toString(),
    )

    private fun mutationResult(outcome: Spoolin.Outcome): Map<String, Any?> = mapOf(
        "operation" to outcomeMap(outcome),
        "problem" to if (outcome.hasProblem()) problemMap(outcome.problem) else null,
    )

    private fun outcomeMap(outcome: Spoolin.Outcome): Map<String, Any?> = mapOf(
        "id" to outcome.operationId,
        "state" to outcome.state,
        "resultId" to if (outcome.hasResultId()) outcome.resultId else null,
        "version" to if (outcome.hasVersion()) outcome.version.toString() else null,
        "resultExpired" to outcome.resultExpired,
        "problem" to if (outcome.hasProblem()) problemMap(outcome.problem) else null,
    )

    private fun problemMap(p: Spoolin.Problem): Map<String, Any?> = mapOf(
        "code" to p.code,
        "message" to "The operation could not be completed.",
        "fields" to p.fieldsList.map { mapOf("path" to it.path, "code" to it.code, "message" to it.message) },
    )

    // ── input mappers ─────────────────────────────────────────────────────────

    private fun personValues(input: Map<String, Any?>): Spoolin.PersonValues {
        val builder = Spoolin.PersonValues.newBuilder()
        (input["name"] as? String)?.let { builder.name = it }
        (input["biography"] as? String)?.let { builder.biography = it }
        return builder.build()
    }

    private fun personPatch(patch: Map<String, Any?>): Pair<Spoolin.PersonValues, com.google.protobuf.FieldMask> {
        val paths = mutableListOf<String>()
        val builder = Spoolin.PersonValues.newBuilder()
        if (patch.containsKey("name")) {
            (patch["name"] as? String)?.let { builder.name = it }
            paths.add("values.name")
        }
        if (patch.containsKey("biography")) {
            (patch["biography"] as? String)?.let { builder.biography = it }
            paths.add("values.biography")
        }
        val mask = com.google.protobuf.FieldMask.newBuilder().addAllPaths(paths).build()
        return Pair(builder.build(), mask)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun issueToken(
        principal: com.spoolin.core.features.session.SessionPrincipal,
        method: String,
        operationId: UUID,
        correlation: UUID,
    ): String {
        val signer = issuer ?: throw RateUnavailable()
        return signer.issue(principal, ActorTarget("catalog", method, operationId, correlation))
    }

    private fun stub(token: String): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val meta = Metadata().apply { put(ActorRpc.metadata, token) }
        return catalog.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(meta))
            .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
    }

    private fun grpcContext(operationId: UUID, correlationId: UUID, policyRevision: String): Spoolin.Context =
        Spoolin.Context.newBuilder()
            .setOperationId(operationId.toString())
            .setCorrelationId(correlationId.toString())
            .setPolicyRevision(policyRevision)
            .build()

    private fun context(env: DataFetchingEnvironment): BrowserContext =
        requireNotNull(env.graphQlContext.get("browser"))

    private fun id(env: DataFetchingEnvironment): UUID =
        UUID.fromString(env.getArgument<String>("operationId"))

    private fun validateUuid(id: String) {
        runCatching { UUID.fromString(id).also { require(it.toString() == id) } }
            .onFailure { throw InputViolation("id", "Supply a lowercase, hyphenated UUID.") }
    }
}
