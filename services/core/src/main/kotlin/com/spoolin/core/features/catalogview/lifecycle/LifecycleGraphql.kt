@file:Suppress("TooManyFunctions")

package com.spoolin.core.features.catalogview.lifecycle

import com.spoolin.core.boundary.graphql.GraphqlLongArguments
import com.spoolin.core.boundary.graphql.GraphqlOutcomeProjection
import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.CatalogActorRpc
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.catalogview.CatalogGraphqlArguments
import com.spoolin.support.receipts.OperationOwner
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID

class LifecycleGraphql(
    catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    issuer: ActorIssuer?,
    private val policyRevision: () -> String = {
        com.spoolin.support.policy.PolicyEnvironment
            .resolve(System::getenv)
            .revision
    },
) {
    private val catalogRpc = CatalogActorRpc(catalog, issuer)

    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q -> q.dataFetcher("deletionImpact") { env -> deletionImpact(env) } }
        wiring.type("Mutation") { m ->
            m
                .dataFetcher("movieLifecycle") { env -> movieLifecycle(env) }
                .dataFetcher("personLifecycle") { env -> personLifecycle(env) }
        }
    }

    private fun movieLifecycle(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val request = lifecycleRequest(env, operationId, context, impactToken = null)
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/MovieLifecycle",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).movieLifecycle(request)
        context.rememberOperation(operationId, OperationOwner.CATALOG, GraphqlOutcomeProjection.operation(outcome))
        return GraphqlOutcomeProjection.mutation(outcome)
    }

    private fun personLifecycle(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val impactToken = env.getArgument<String>("impactToken")
        val request = lifecycleRequest(env, operationId, context, impactToken)
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/PersonLifecycle",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).personLifecycle(request)
        context.rememberOperation(operationId, OperationOwner.CATALOG, GraphqlOutcomeProjection.operation(outcome))
        return GraphqlOutcomeProjection.mutation(outcome)
    }

    private fun deletionImpact(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val personId = requireNotNull(env.getArgument<String>("personId"))
        val opId = UUID.randomUUID()
        val token = catalogRpc.issue(principal, "/spoolin.v1.CatalogService/DeletionImpact", opId, context.correlation)
        val impact =
            catalogRpc.stub(token).deletionImpact(
                Spoolin.Id
                    .newBuilder()
                    .setId(personId)
                    .build(),
            )
        return mapDeletionImpact(impact)
    }

    private fun lifecycleRequest(
        env: DataFetchingEnvironment,
        operationId: UUID,
        context: BrowserContext,
        impactToken: String?,
    ): Spoolin.Lifecycle {
        val targetId = requireNotNull(env.getArgument<String>("id"))
        val version = GraphqlLongArguments.required(env)
        val action = requireNotNull(env.getArgument<String>("action"))
        val confirmed = env.getArgument<Boolean>("confirmed") == true
        val grpcContext =
            Spoolin.Context
                .newBuilder()
                .setOperationId(operationId.toString())
                .setCorrelationId(context.correlation.toString())
                .setPolicyRevision(policyRevision())
                .build()
        val versioned =
            Spoolin.VersionedId
                .newBuilder()
                .setId(targetId)
                .setExpectedVersion(version)
                .setContext(grpcContext)
                .build()
        val builder =
            Spoolin.Lifecycle
                .newBuilder()
                .setTarget(versioned)
                .setAction(action)
                .setConfirmed(confirmed)
        impactToken?.let { builder.impactToken = it }
        return builder.build()
    }
}

internal fun mapDeletionImpact(impact: Spoolin.Impact): Map<String, Any?> =
    mapOf(
        "targetId" to impact.targetId,
        "token" to impact.token,
        "affectedCreditCount" to impact.affectedCreditCount,
        "affectedTitles" to impact.affectedTitlesList,
    )
