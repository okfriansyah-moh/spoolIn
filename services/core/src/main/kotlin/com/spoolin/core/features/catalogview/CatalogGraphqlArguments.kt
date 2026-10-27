package com.spoolin.core.features.catalogview

import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.ReadPolicy
import graphql.schema.DataFetchingEnvironment
import spoolin.v1.Spoolin
import java.util.UUID

internal object CatalogGraphqlArguments {
    fun browser(env: DataFetchingEnvironment): BrowserContext {
        val browser: BrowserContext? = env.graphQlContext.get("browser")
        return requireNotNull(browser)
    }

    fun operationId(env: DataFetchingEnvironment): UUID = UUID.fromString(env.getArgument<String>("operationId"))

    fun validateId(id: String) {
        runCatching { UUID.fromString(id).also { require(it.toString() == id) } }
            .onFailure { throw InputViolation("id", "Supply a lowercase, hyphenated UUID.") }
    }

    fun writeContext(
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

    fun managedPage(
        env: DataFetchingEnvironment,
        policy: () -> PublicPolicy,
    ): Spoolin.PageRequest {
        val state = requireNotNull(env.getArgument<String>("state"))
        val first = ReadPolicy(policy()).pageSize(env.getArgument<Int>("first"))
        val after = env.getArgument<String>("after")
        return Spoolin.PageRequest
            .newBuilder()
            .setState(state)
            .setFirst(first)
            .also { builder -> after?.let { builder.after = it } }
            .build()
    }
}
