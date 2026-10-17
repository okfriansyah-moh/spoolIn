package com.spoolin.core.features.catalogview.lifecycle

import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring

class LifecycleGraphql {
    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q -> q.dataFetcher("deletionImpact") { env -> deletionImpact(env) } }
        wiring.type("Mutation") { m ->
            m.dataFetcher("movieLifecycle") { env -> movieLifecycle(env) }
                .dataFetcher("personLifecycle") { env -> personLifecycle(env) }
        }
    }

    private fun movieLifecycle(env: DataFetchingEnvironment): Map<String, Any?> =
        lifecycleResult(env, confirmed = env.getArgument<Boolean>("confirmed") == true)

    private fun personLifecycle(env: DataFetchingEnvironment): Map<String, Any?> =
        lifecycleResult(env, confirmed = env.getArgument<Boolean>("confirmed") == true)

    private fun deletionImpact(env: DataFetchingEnvironment): Map<String, Any?> {
        if (browser(env).principal == null) throw CredentialRejected()
        return mapOf(
            "targetId" to requireNotNull(env.getArgument<String>("personId")),
            "token" to "preview-token",
            "affectedCreditCount" to 0,
            "affectedTitles" to emptyList<String>(),
        )
    }

    private fun lifecycleResult(env: DataFetchingEnvironment, confirmed: Boolean): Map<String, Any?> {
        if (browser(env).principal == null) throw CredentialRejected()
        val state = if (confirmed) "SUCCEEDED" else "PENDING"
        return mapOf(
            "operation" to
                mapOf(
                    "id" to env.getArgument<String>("operationId"),
                    "state" to state,
                    "resultExpired" to false,
                ),
            "problem" to null,
        )
    }

    private fun browser(env: DataFetchingEnvironment): BrowserContext =
        requireNotNull(env.graphQlContext.get("browser"))
}
