package com.spoolin.core.features.catalogview.lifecycle

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.ActorTarget
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.OperationOwner
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DEADLINE_SECONDS = 5L

class LifecycleGraphql(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val issuer: ActorIssuer?,
) {
    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q -> q.dataFetcher("deletionImpact") { env -> deletionImpact(env) } }
        wiring.type("Mutation") { m ->
            m.dataFetcher("movieLifecycle") { env -> movieLifecycle(env) }
                .dataFetcher("personLifecycle") { env -> personLifecycle(env) }
        }
    }

    private fun movieLifecycle(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val request = lifecycleRequest(env, operationId, context, impactToken = null)
        val token = issueToken(principal, "/spoolin.v1.CatalogService/MovieLifecycle", operationId, context.correlation)
        val outcome = stub(token).movieLifecycle(request)
        context.rememberOperation(operationId, OperationOwner.CATALOG, outcomeMap(outcome))
        return mutationResult(outcome)
    }

    private fun personLifecycle(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val impactToken = env.getArgument<String>("impactToken")
        val request = lifecycleRequest(env, operationId, context, impactToken)
        val token = issueToken(principal, "/spoolin.v1.CatalogService/PersonLifecycle", operationId, context.correlation)
        val outcome = stub(token).personLifecycle(request)
        context.rememberOperation(operationId, OperationOwner.CATALOG, outcomeMap(outcome))
        return mutationResult(outcome)
    }

    private fun deletionImpact(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val personId = requireNotNull(env.getArgument<String>("personId"))
        val opId = UUID.randomUUID()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/DeletionImpact", opId, context.correlation)
        val impact = stub(token).deletionImpact(Spoolin.Id.newBuilder().setId(personId).build())
        return mapOf(
            "targetId" to impact.targetId,
            "token" to impact.token,
            "affectedCreditCount" to impact.affectedCreditCount,
            "affectedTitles" to impact.affectedTitlesList,
        )
    }

    private fun lifecycleRequest(
        env: DataFetchingEnvironment,
        operationId: UUID,
        context: BrowserContext,
        impactToken: String?,
    ): Spoolin.Lifecycle {
        val targetId = requireNotNull(env.getArgument<String>("id"))
        val version = requireNotNull(env.getArgument<Long>("expectedVersion"))
        val action = requireNotNull(env.getArgument<String>("action"))
        val confirmed = env.getArgument<Boolean>("confirmed") == true
        val grpcContext =
            Spoolin.Context.newBuilder()
                .setOperationId(operationId.toString())
                .setCorrelationId(context.correlation.toString())
                .setPolicyRevision("")
                .build()
        val versioned =
            Spoolin.VersionedId.newBuilder()
                .setId(targetId)
                .setExpectedVersion(version)
                .setContext(grpcContext)
                .build()
        val builder =
            Spoolin.Lifecycle.newBuilder()
                .setTarget(versioned)
                .setAction(action)
                .setConfirmed(confirmed)
        impactToken?.let { builder.impactToken = it }
        return builder.build()
    }

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

    private fun browser(env: DataFetchingEnvironment): BrowserContext =
        requireNotNull(env.graphQlContext.get("browser"))

    private fun id(env: DataFetchingEnvironment): UUID =
        UUID.fromString(env.getArgument<String>("operationId"))
}
