@file:Suppress("TooManyFunctions")

package com.spoolin.core.features.catalogview.person

import com.spoolin.core.boundary.graphql.GraphqlLongArguments
import com.spoolin.core.boundary.graphql.GraphqlOutcomeProjection
import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.CatalogActorRpc
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.catalogview.CatalogGraphqlArguments
import com.spoolin.core.features.catalogview.read.CatalogReadProjection
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.receipts.OperationOwner
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID

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
    catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    issuer: ActorIssuer?,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
) {
    private val catalogRpc = CatalogActorRpc(catalog, issuer)

    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q ->
            q
                .dataFetcher("managedPerson") { env -> managedPerson(env) }
                .dataFetcher("managedPeople") { env -> managedPeople(env) }
        }
        wiring.type("Mutation") { m ->
            m
                .dataFetcher("createPerson") { env -> createPerson(env) }
                .dataFetcher("updatePerson") { env -> updatePerson(env) }
        }
    }

    // ── createPerson ──────────────────────────────────────────────────────────

    private fun createPerson(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val input = requireNotNull(env.getArgument<Map<String, Any?>>("input"))
        val values = personValues(input)
        val request =
            Spoolin.PersonWrite
                .newBuilder()
                .setContext(CatalogGraphqlArguments.writeContext(operationId, context.correlation, policyRevision))
                .setValues(values)
                .build()
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/WritePerson",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).writePerson(request)
        val result = GraphqlOutcomeProjection.operation(outcome)
        context.rememberOperation(operationId, OperationOwner.CATALOG, result)
        return GraphqlOutcomeProjection.mutation(outcome)
    }

    // ── updatePerson ──────────────────────────────────────────────────────────

    private fun updatePerson(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = CatalogGraphqlArguments.operationId(env)
        val personId = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(personId)
        val expectedVersion = GraphqlLongArguments.required(env)
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val patch = requireNotNull(env.getArgument<Map<String, Any?>>("patch"))
        val (values, mask) = personPatch(patch)
        val request =
            Spoolin.PersonWrite
                .newBuilder()
                .setContext(CatalogGraphqlArguments.writeContext(operationId, context.correlation, policyRevision))
                .setId(personId)
                .setExpectedVersion(expectedVersion)
                .setValues(values)
                .setUpdateMask(mask)
                .build()
        val token =
            catalogRpc.issue(
                principal,
                "/spoolin.v1.CatalogService/WritePerson",
                operationId,
                context.correlation,
            )
        val outcome = catalogRpc.stub(token).writePerson(request)
        val result = GraphqlOutcomeProjection.operation(outcome)
        context.rememberOperation(operationId, OperationOwner.CATALOG, result)
        return GraphqlOutcomeProjection.mutation(outcome)
    }

    // ── managedPerson ─────────────────────────────────────────────────────────

    private fun managedPerson(env: DataFetchingEnvironment): Map<String, Any?>? {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val personId = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(personId)
        val opId = UUID.randomUUID()
        val token = catalogRpc.issue(principal, "/spoolin.v1.CatalogService/ManagedPerson", opId, context.correlation)
        val result =
            catalogRpc.stub(token).managedPerson(
                Spoolin.Id
                    .newBuilder()
                    .setId(personId)
                    .build(),
            )
        if (result.id.isEmpty()) return null
        return CatalogReadProjection.person(result)
    }

    // ── managedPeople ─────────────────────────────────────────────────────────

    private fun managedPeople(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = CatalogGraphqlArguments.browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val request = CatalogGraphqlArguments.managedPage(env, policy)
        val opId = UUID.randomUUID()
        val token = catalogRpc.issue(principal, "/spoolin.v1.CatalogService/ManagedPeople", opId, context.correlation)
        val result = catalogRpc.stub(token).managedPeople(request)
        return CatalogReadProjection.personPage(result)
    }

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
        val mask =
            com.google.protobuf.FieldMask
                .newBuilder()
                .addAllPaths(paths)
                .build()
        return Pair(builder.build(), mask)
    }
}
