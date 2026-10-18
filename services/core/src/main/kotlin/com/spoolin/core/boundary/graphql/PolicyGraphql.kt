package com.spoolin.core.boundary.graphql

import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.receipts.OperationOwner
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.util.UUID

typealias ScopedOperationQuery = (UUID, OperationOwner?, BrowserContext?) -> Map<String, Any?>?

class PolicyGraphql(
    schema: String,
    private val provider: PolicyProvider,
    views: PolicyGraphqlViews = PolicyGraphqlViews(),
    operations: ScopedOperationQuery? = null,
    operation: (UUID, OperationOwner?) -> Map<String, Any?>? = { _, _ -> null },
) {
    private val envelope = GraphqlEnvelope(provider.policy)
    private val engine: GraphQL

    init {
        val wiring = RuntimeWiring.newRuntimeWiring()
        PolicyScalars.all().forEach(wiring::scalar)
        wiring.type("Query") { it.dataFetcher("policy") { provider.projection() } }
        wiring.type("Query") { query ->
            query.dataFetcher("operation") { environment ->
                val id = UUID.fromString(environment.getArgument<String>("id"))
                val owner =
                    environment.getArgument<String>("owner")?.let {
                        OperationOwner
                            .valueOf(it)
                    }
                if (operations == null) {
                    operation(id, owner)
                } else {
                    operations(id, owner, environment.graphQlContext.get("browser"))
                }
            }
        }
        views.accounts?.bind(wiring)
        views.catalog?.bind(wiring)
        views.managedCatalog?.bind(wiring)
        views.managedPerson?.bind(wiring)
        views.managedCredit?.bind(wiring)
        views.artwork?.bind(wiring)
        views.lifecycle?.bind(wiring)
        views.drafts?.bind(wiring)
        val definitions = EffectiveReadDefaults.apply(
            SchemaParser().parse(schema), com.spoolin.support.policy.ReadPolicy(provider.policy),
        )
        val executable = SchemaGenerator().makeExecutableSchema(definitions, wiring.build())
        engine = GraphQL.newGraphQL(executable).build()
    }

    private fun failureCode(errors: List<graphql.GraphQLError>): String {
        val failure = errors.filterIsInstance<graphql.ExceptionWhileDataFetching>().firstOrNull()?.exception
        return when (failure) {
            null -> "FIELD_INVALID"
            is com.spoolin.core.operations.receipts.AmbiguousOperation -> "AMBIGUOUS_OPERATION"
            is com.spoolin.support.receipts.ReceiptConflict -> "IDEMPOTENCY_CONFLICT"
            is com.spoolin.support.receipts.ReceiptHidden -> "NOT_FOUND"
            else -> authenticationFailure(failure)
        }
    }

    private fun authenticationFailure(failure: Throwable): String =
        when (failure) {
            is com.spoolin.core.features.ratelimit.RateLimited -> "RATE_LIMITED"
            is com.spoolin.core.features.account.application.CredentialRejected -> "UNAUTHENTICATED"
            is com.spoolin.core.features.account.persistence.AccountIdentityConflict -> "FIELD_INVALID"
            is IllegalArgumentException -> "FIELD_INVALID"
            is com.spoolin.core.features.crypto.HashingBusy -> "RESOURCE_EXHAUSTED"
            is com.spoolin.core.features.account.application.AuthenticationDeadlineExceeded -> "DEADLINE_EXCEEDED"
            else -> "UNAVAILABLE"
        }

    private fun failureDetails(errors: List<graphql.GraphQLError>): Map<String, Any?> {
        val exception = errors.filterIsInstance<graphql.ExceptionWhileDataFetching>().firstOrNull()?.exception
        val details = mutableMapOf<String, Any?>("code" to failureCode(errors))
        if (exception is com.spoolin.core.features.ratelimit.RateLimited) {
            details["retryAfterMs"] = exception.retryAfterMs
        }
        return details
    }

    fun execute(
        input: Map<String, Any?>,
        context: BrowserContext? = null,
    ): Map<String, Any?> {
        val result =
            runCatching {
                val parsed = envelope.parse(input)
                val execution =
                    if (context == null) {
                        parsed
                    } else {
                        parsed.transform { it.graphQLContext(mapOf("browser" to context)) }
                    }
                engine.execute(execution)
            }.getOrNull()
        if (result == null || result.errors.isNotEmpty()) {
            return mapOf(
                "data" to null,
                "errors" to
                    listOf(
                        mapOf(
                            "message" to "The request could not be completed.",
                            "extensions" to failureDetails(result?.errors.orEmpty()),
                        ),
                    ),
            )
        }
        return mapOf("data" to result.getData<Any?>())
    }
}

@Configuration
class PolicyGraphqlConfiguration {
    @Bean
    @Suppress("LongParameterList")
    fun policyGraphqlProviders(
        accounts: org.springframework.beans.factory.ObjectProvider<AccountGraphql>,
        catalog: org.springframework.beans.factory.ObjectProvider<
            com.spoolin.core.features.catalogview.read.CatalogViewGraphql,
        >,
        drafts: org.springframework.beans.factory.ObjectProvider<
            com.spoolin.core.features.draft.DraftGraphql,
        >,
        managedCatalog: org.springframework.beans.factory.ObjectProvider<
            com.spoolin.core.features.catalogview.movie.ManagedCatalogGraphql,
        >,
        managedPerson: org.springframework.beans.factory.ObjectProvider<
            com.spoolin.core.features.catalogview.person.ManagedPersonGraphql,
        >,
        managedCredit: org.springframework.beans.factory.ObjectProvider<
            com.spoolin.core.features.catalogview.credit.ManagedCreditGraphql,
        >,
        artwork: org.springframework.beans.factory.ObjectProvider<
            com.spoolin.core.features.artworkproxy.ArtworkGraphql,
        >,
        lifecycle: org.springframework.beans.factory.ObjectProvider<
            com.spoolin.core.features.catalogview.lifecycle.LifecycleGraphql,
        >,
    ): PolicyGraphqlProviders =
        PolicyGraphqlProviders(
            accounts = accounts,
            catalog = catalog,
            drafts = drafts,
            managedCatalog = managedCatalog,
            managedPerson = managedPerson,
            managedCredit = managedCredit,
            artwork = artwork,
            lifecycle = lifecycle,
        )

    @Bean
    fun policyGraphql(
        provider: PolicyProvider,
        modules: PolicyGraphqlProviders,
    ): PolicyGraphql {
        val schema =
            ClassPathResource("graphql/schema.graphql")
                .inputStream
                .use { it.readBytes().toString(Charsets.UTF_8) }
        return PolicyGraphql(
            schema,
            provider,
            views = modules.resolved(),
            operations = { id, owner, context -> context?.operation(id, owner) },
        )
    }
}
