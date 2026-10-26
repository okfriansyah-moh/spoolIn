@file:Suppress("TooManyFunctions")

package com.spoolin.core.features.catalogview.credit

import com.spoolin.core.boundary.graphql.GraphqlLongArguments
import com.spoolin.core.boundary.graphql.GraphqlOutcomeProjection
import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.CatalogActorRpc
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.catalogview.CatalogGraphqlArguments
import com.spoolin.core.features.catalogview.read.CatalogReadProjection
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.ReadPolicy
import com.spoolin.support.receipts.OperationOwner
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID

class ManagedCreditGraphql(
    catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    issuer: ActorIssuer?,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
) {
    private val catalogRpc = CatalogActorRpc(catalog, issuer)

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
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val titleId = env.getArgument<String>("titleId")
        val personId = env.getArgument<String>("personId")
        if ((titleId == null) == (personId == null)) throw InputViolation("selector", "Choose a title or person.")
        val first = ReadPolicy(policy()).pageSize(env.getArgument<Int>("first"))
        val after = env.getArgument<String>("after")
        val opId = UUID.randomUUID()
        val token = catalogRpc.issue(principal, "/spoolin.v1.CatalogService/ManagedCredits", opId, context.correlation)
        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setFirst(first)
                .setTargetKind(if (titleId != null) "TITLE" else "PERSON")
                .setTargetId(titleId ?: personId!!)
                .also { b -> after?.let { b.after = it } }
                .build()
        val result = catalogRpc.stub(token).managedCredits(request)
        return CatalogReadProjection.creditPage(result)
    }

    private fun createRole(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val category = requireNotNull(env.getArgument<String>("category"))
        val label = requireNotNull(env.getArgument<String>("label"))
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val request =
            Spoolin.RoleWrite
                .newBuilder()
                .setContext(CatalogGraphqlArguments.writeContext(operationId, context.correlation, policyRevision))
                .setCategory(category)
                .setLabel(label)
                .build()
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/CreateRole",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).createRole(request)
        context.rememberOperation(operationId, OperationOwner.CATALOG, GraphqlOutcomeProjection.operation(outcome))
        return GraphqlOutcomeProjection.mutation(outcome)
    }

    private fun addCredit(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val titleId = requireNotNull(env.getArgument<String>("titleId"))
        val expectedVersion = GraphqlLongArguments.required(env)
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val input = requireNotNull(env.getArgument<Map<String, Any?>>("input"))
        CatalogGraphqlArguments.validateId(titleId)
        val request =
            Spoolin.CreditWrite
                .newBuilder()
                .setContext(CatalogGraphqlArguments.writeContext(operationId, context.correlation, policyRevision))
                .setTitleId(titleId)
                .setExpectedVersion(expectedVersion)
                .setPersonId(requireNotNull(input["personId"] as? String))
                .setRoleId(requireNotNull(input["roleId"] as? String))
                .addAllCharacters((input["characters"] as? List<*>)?.map { it.toString() } ?: emptyList())
                .build()
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/WriteCredit",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).writeCredit(request)
        context.rememberOperation(operationId, OperationOwner.CATALOG, GraphqlOutcomeProjection.operation(outcome))
        return GraphqlOutcomeProjection.mutation(outcome)
    }

    private fun updateCredit(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val creditId = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(creditId)
        val expectedVersion = GraphqlLongArguments.required(env)
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val patch = requireNotNull(env.getArgument<Map<String, Any?>>("patch"))
        val paths = mutableListOf<String>()
        val builder =
            Spoolin.CreditWrite
                .newBuilder()
                .setContext(CatalogGraphqlArguments.writeContext(operationId, context.correlation, policyRevision))
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
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/WriteCredit",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).writeCredit(builder.build())
        context.rememberOperation(operationId, OperationOwner.CATALOG, GraphqlOutcomeProjection.operation(outcome))
        return GraphqlOutcomeProjection.mutation(outcome)
    }

    private fun removeCredit(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val creditId = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(creditId)
        val expectedVersion = GraphqlLongArguments.required(env)
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
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/RemoveCredit",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).removeCredit(request)
        context.rememberOperation(operationId, OperationOwner.CATALOG, GraphqlOutcomeProjection.operation(outcome))
        return GraphqlOutcomeProjection.mutation(outcome)
    }
}
