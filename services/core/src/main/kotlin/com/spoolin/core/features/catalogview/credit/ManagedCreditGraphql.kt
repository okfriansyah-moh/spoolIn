@file:Suppress("TooManyFunctions")

package com.spoolin.core.features.catalogview.credit

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.ActorTarget
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.ReadPolicy
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DEADLINE_SECONDS = 5L

class ManagedCreditGraphql(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val issuer: ActorIssuer?,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
) {
    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q ->
            q.dataFetcher("managedCredits") { env -> managedCredits(env) }
        }
        wiring.type("Mutation") { m ->
            m
                .dataFetcher("createRole") { env -> createRole(env) }
                .dataFetcher("addCredit") { env -> addCredit(env) }
                .dataFetcher("updateCredit") { env -> updateCredit(env) }
                .dataFetcher("removeCredit") { env -> removeCredit(env) }
        }
    }

    private fun managedCredits(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val titleId = env.getArgument<String>("titleId")
        val personId = env.getArgument<String>("personId")
        if ((titleId == null) == (personId == null)) throw InputViolation("selector", "Choose a title or person.")
        val first = ReadPolicy(policy()).pageSize(env.getArgument<Int>("first"))
        val after = env.getArgument<String>("after")
        val opId = UUID.randomUUID()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/ManagedCredits", opId, context.correlation)
        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setFirst(first)
                .setTargetKind(if (titleId != null) "TITLE" else "PERSON")
                .setTargetId(titleId ?: personId!!)
                .also { b -> after?.let { b.after = it } }
                .build()
        val result = stub(token).managedCredits(request)
        return creditPageMap(result)
    }

    private fun createRole(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val category = requireNotNull(env.getArgument<String>("category"))
        val label = requireNotNull(env.getArgument<String>("label"))
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val request =
            Spoolin.RoleWrite
                .newBuilder()
                .setContext(grpcContext(operationId, context.correlation, policyRevision))
                .setCategory(category)
                .setLabel(label)
                .build()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/CreateRole", operationId, context.correlation)
        val outcome = stub(token).createRole(request)
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CATALOG, outcomeMap(outcome))
        return mutationResult(outcome)
    }

    private fun addCredit(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val titleId = requireNotNull(env.getArgument<String>("titleId"))
        val expectedVersion = requireNotNull(env.getArgument<Long>("expectedVersion"))
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val input = requireNotNull(env.getArgument<Map<String, Any?>>("input"))
        validateUuid(titleId)
        val request =
            Spoolin.CreditWrite
                .newBuilder()
                .setContext(grpcContext(operationId, context.correlation, policyRevision))
                .setTitleId(titleId)
                .setExpectedVersion(expectedVersion)
                .setPersonId(requireNotNull(input["personId"] as? String))
                .setRoleId(requireNotNull(input["roleId"] as? String))
                .addAllCharacters((input["characters"] as? List<*>)?.map { it.toString() } ?: emptyList())
                .build()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/WriteCredit", operationId, context.correlation)
        val outcome = stub(token).writeCredit(request)
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CATALOG, outcomeMap(outcome))
        return mutationResult(outcome)
    }

    private fun updateCredit(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val creditId = requireNotNull(env.getArgument<String>("id"))
        validateUuid(creditId)
        val expectedVersion = requireNotNull(env.getArgument<Long>("expectedVersion"))
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val patch = requireNotNull(env.getArgument<Map<String, Any?>>("patch"))
        val paths = mutableListOf<String>()
        val builder =
            Spoolin.CreditWrite
                .newBuilder()
                .setContext(grpcContext(operationId, context.correlation, policyRevision))
                .setId(creditId)
                .setExpectedVersion(expectedVersion)
        if (patch.containsKey("roleId")) {
            builder.roleId = patch["roleId"] as? String ?: ""
            paths.add("roleId")
        }
        if (patch.containsKey("characters")) {
            builder.addAllCharacters((patch["characters"] as? List<*>)?.map { it.toString() } ?: emptyList())
            paths.add("characters")
        }
        builder.updateMask =
            com.google.protobuf.FieldMask
                .newBuilder()
                .addAllPaths(paths)
                .build()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/WriteCredit", operationId, context.correlation)
        val outcome = stub(token).writeCredit(builder.build())
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CATALOG, outcomeMap(outcome))
        return mutationResult(outcome)
    }

    private fun removeCredit(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val creditId = requireNotNull(env.getArgument<String>("id"))
        validateUuid(creditId)
        val expectedVersion = requireNotNull(env.getArgument<Long>("expectedVersion"))
        val request =
            Spoolin.VersionedId
                .newBuilder()
                .setContext(
                    Spoolin.Context
                        .newBuilder()
                        .setOperationId(operationId.toString())
                        .setCorrelationId(context.correlation.toString())
                        .setPolicyRevision("")
                        .build(),
                ).setId(creditId)
                .setExpectedVersion(expectedVersion)
                .build()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/RemoveCredit", operationId, context.correlation)
        val outcome = stub(token).removeCredit(request)
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CATALOG, outcomeMap(outcome))
        return mutationResult(outcome)
    }

    private fun creditPageMap(proto: Spoolin.CreditPage): Map<String, Any?> =
        mapOf(
            "nodes" to proto.nodesList.map { creditMap(it) },
            "pageInfo" to
                mapOf(
                    "endCursor" to proto.endCursor.takeIf { it.isNotEmpty() },
                    "hasNextPage" to proto.hasNextPage,
                ),
        )

    private fun creditMap(c: Spoolin.Credit): Map<String, Any?> =
        mapOf(
            "id" to c.id,
            "personId" to c.personId,
            "personName" to c.personName,
            "titleId" to c.titleId,
            "titleName" to c.titleName,
            "role" to
                mapOf(
                    "id" to c.role.id,
                    "label" to c.role.label,
                    "category" to c.role.category,
                    "allowsCharacters" to c.role.allowsCharacters,
                ),
            "characters" to c.charactersList,
            "version" to c.version.toString(),
        )

    private fun mutationResult(outcome: Spoolin.Outcome): Map<String, Any?> =
        mapOf(
            "operation" to outcomeMap(outcome),
            "problem" to if (outcome.hasProblem()) problemMap(outcome.problem) else null,
        )

    private fun outcomeMap(outcome: Spoolin.Outcome): Map<String, Any?> =
        mapOf(
            "id" to outcome.operationId,
            "state" to outcome.state,
            "resultId" to if (outcome.hasResultId()) outcome.resultId else null,
            "version" to if (outcome.hasVersion()) outcome.version.toString() else null,
            "resultExpired" to outcome.resultExpired,
            "problem" to if (outcome.hasProblem()) problemMap(outcome.problem) else null,
        )

    private fun problemMap(p: Spoolin.Problem): Map<String, Any?> =
        mapOf(
            "code" to p.code,
            "message" to "The operation could not be completed.",
            "fields" to p.fieldsList.map { mapOf("path" to it.path, "code" to it.code, "message" to it.message) },
        )

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
        return catalog
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(meta))
            .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
    }

    private fun grpcContext(
        operationId: UUID,
        correlationId: UUID,
        policyRevision: String,
    ): Spoolin.Context =
        Spoolin.Context
            .newBuilder()
            .setOperationId(operationId.toString())
            .setCorrelationId(correlationId.toString())
            .setPolicyRevision(policyRevision)
            .build()

    private fun context(env: DataFetchingEnvironment): BrowserContext =
        requireNotNull(env.graphQlContext.get("browser"))

    private fun id(env: DataFetchingEnvironment): UUID = UUID.fromString(env.getArgument<String>("operationId"))

    private fun validateUuid(id: String) {
        runCatching { UUID.fromString(id).also { require(it.toString() == id) } }
            .onFailure { throw InputViolation("id", "Supply a lowercase, hyphenated UUID.") }
    }
}
