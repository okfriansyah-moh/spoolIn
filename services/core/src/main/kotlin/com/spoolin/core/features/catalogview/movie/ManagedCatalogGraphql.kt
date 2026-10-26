@file:Suppress("TooManyFunctions")

package com.spoolin.core.features.catalogview.movie

import com.spoolin.core.boundary.graphql.GraphqlLongArguments
import com.spoolin.core.boundary.graphql.GraphqlOutcomeProjection
import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.CatalogActorRpc
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.catalogview.CatalogGraphqlArguments
import com.spoolin.core.features.catalogview.read.CatalogReadProjection
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.ReadPolicy
import com.spoolin.support.receipts.OperationOwner
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.time.Instant
import java.util.UUID

/**
 * GraphQL resolvers for Maintainer-only catalog mutations and managed reads.
 *
 * Mutations: createMovie, updateMovie.
 * Queries:   managedTitle, managedTitles, audit.
 *
 * F-PERMISSIONS: each operation requires an authenticated MAINTAINER session.
 * AC-127: actorRef is the account UUID only; no email or credential data exposed.
 * ATDD-251-T1-R23/R24: createMovie / updateMovie at their authoritative boundary.
 */
class ManagedCatalogGraphql(
    catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    issuer: ActorIssuer?,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
) {
    private val catalogRpc = CatalogActorRpc(catalog, issuer)

    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q ->
            q
                .dataFetcher("managedTitle") { env -> managedTitle(env) }
                .dataFetcher("managedTitles") { env -> managedTitles(env) }
                .dataFetcher("audit") { env -> audit(env) }
        }
        wiring.type("Mutation") { m ->
            m
                .dataFetcher("createMovie") { env -> createMovie(env) }
                .dataFetcher("updateMovie") { env -> updateMovie(env) }
        }
    }

    // ── createMovie ───────────────────────────────────────────────────────────

    private fun createMovie(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val input = requireNotNull(env.getArgument<Map<String, Any?>>("input"))
        val values = movieValuesFromInput(input)
        val request =
            Spoolin.MovieWrite
                .newBuilder()
                .setContext(CatalogGraphqlArguments.writeContext(operationId, context.correlation, policyRevision))
                .setValues(values)
                .build()
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/WriteMovie",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).writeMovie(request)
        val result = GraphqlOutcomeProjection.operation(outcome)
        context.rememberOperation(operationId, OperationOwner.CATALOG, result)
        return GraphqlOutcomeProjection.mutation(outcome)
    }

    // ── updateMovie ───────────────────────────────────────────────────────────

    private fun updateMovie(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val titleId = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(titleId)
        val expectedVersion = GraphqlLongArguments.required(env)
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val patch = requireNotNull(env.getArgument<Map<String, Any?>>("patch"))
        val (values, mask) = moviePatchFromInput(patch)
        val request =
            Spoolin.MovieWrite
                .newBuilder()
                .setContext(CatalogGraphqlArguments.writeContext(operationId, context.correlation, policyRevision))
                .setId(titleId)
                .setExpectedVersion(expectedVersion)
                .setValues(values)
                .setUpdateMask(mask)
                .build()
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/WriteMovie",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).writeMovie(request)
        val result = GraphqlOutcomeProjection.operation(outcome)
        context.rememberOperation(operationId, OperationOwner.CATALOG, result)
        return GraphqlOutcomeProjection.mutation(outcome)
    }

    // ── managedTitle ─────────────────────────────────────────────────────────

    private fun managedTitle(env: DataFetchingEnvironment): Map<String, Any?>? {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val titleId = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(titleId)
        val opId = UUID.randomUUID()
        val token = catalogRpc.issue(principal, "/spoolin.v1.CatalogService/ManagedTitle", opId, context.correlation)
        val result =
            catalogRpc.stub(token).managedTitle(
                Spoolin.Id
                    .newBuilder()
                    .setId(titleId)
                    .build(),
            )
        if (result.id.isEmpty()) return null
        return CatalogReadProjection.title(result, management = true)
    }

    // ── managedTitles ─────────────────────────────────────────────────────────

    private fun managedTitles(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val request = CatalogGraphqlArguments.managedPage(env, policy)
        val opId = UUID.randomUUID()
        val token = catalogRpc.issue(principal, "/spoolin.v1.CatalogService/ManagedTitles", opId, context.correlation)
        val result = catalogRpc.stub(token).managedTitles(request)
        return CatalogReadProjection.titlePage(result, management = true)
    }

    // ── audit ─────────────────────────────────────────────────────────────────

    private fun audit(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val subjectId = env.getArgument<String>("subjectId")?.also { CatalogGraphqlArguments.validateId(it) }
        val first = ReadPolicy(policy()).pageSize(env.getArgument<Int>("first"))
        val after = env.getArgument<String>("after")
        val opId = UUID.randomUUID()
        val token = catalogRpc.issue(principal, "/spoolin.v1.CatalogService/Audit", opId, context.correlation)
        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setFirst(first)
                .also { b ->
                    subjectId?.let { b.targetId = it }
                    after?.let { b.after = it }
                }.build()
        val result = catalogRpc.stub(token).audit(request)
        return auditPageMap(result)
    }

    // ── proto → GraphQL maps ──────────────────────────────────────────────────

    private fun auditPageMap(proto: Spoolin.AuditPage): Map<String, Any?> =
        CatalogReadProjection.page(proto.nodesList.map { auditEventMap(it) }, proto.endCursor, proto.hasNextPage)

    private fun auditEventMap(e: Spoolin.AuditEvent): Map<String, Any?> =
        mapOf(
            "id" to e.id,
            "actorRef" to e.actorRef,
            "action" to e.action,
            "outcome" to e.outcome,
            "subjectKind" to e.subjectKind,
            "subjectId" to e.subjectId.takeIf { e.hasSubjectId() }?.let { UUID.fromString(it).toString() },
            "changes" to
                e.changesList.associate {
                    it.path to ManagedAuditValues.change(it)
                },
            "operationId" to e.operationId.takeIf { e.hasOperationId() }?.let { UUID.fromString(it).toString() },
            "at" to Instant.ofEpochSecond(e.at.seconds, e.at.nanos.toLong()).toString(),
        )
}

/** JSON wire values remain typed values; omission metadata is not presented as the original field value. */
private object ManagedAuditValues {
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

    fun change(change: spoolin.v1.Spoolin.AuditChange): Map<String, Any?> {
        val before = if (change.hasBeforeJson()) decode(change.beforeJson) else null
        val after = if (change.hasAfterJson()) decode(change.afterJson) else null
        return if (after is Map<*, *> && after["valueOmitted"] == true) {
            mapOf(
                "valueOmitted" to true,
                "byteLength" to after["byteLength"],
                "beforeDigest" to after["beforeDigest"],
                "afterDigest" to after["afterDigest"],
            )
        } else {
            mapOf("before" to before, "after" to after)
        }
    }

    private fun decode(value: String): Any? =
        try {
            require(value.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
            mapper.readValue(value, Any::class.java)
        } catch (_: Exception) {
            throw io.grpc.Status.DATA_LOSS
                .withDescription("Audit projection unavailable")
                .asRuntimeException()
        }
}
