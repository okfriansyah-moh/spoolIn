package com.spoolin.core.boundary.graphql

import com.spoolin.core.features.policy.PolicyProvider
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource

class PolicyGraphql(
    schema: String,
    private val provider: PolicyProvider,
    operation: (java.util.UUID, com.spoolin.support.receipts.OperationOwner?) -> Map<String, Any?>? = { _, _ -> null },
) {
    private val envelope = GraphqlEnvelope(provider.policy)
    private val engine: GraphQL

    init {
        val wiring = RuntimeWiring.newRuntimeWiring()
        PolicyScalars.all().forEach(wiring::scalar)
        wiring.type("Query") { it.dataFetcher("policy") { provider.projection() } }
        wiring.type("Query") { query ->
            query.dataFetcher("operation") { environment ->
                val id = java.util.UUID.fromString(environment.getArgument<String>("id"))
                val owner =
                    environment.getArgument<String>("owner")?.let {
                        com.spoolin.support.receipts.OperationOwner
                            .valueOf(it)
                    }
                operation(id, owner)
            }
        }
        val executable = SchemaGenerator().makeExecutableSchema(SchemaParser().parse(schema), wiring.build())
        engine = GraphQL.newGraphQL(executable).build()
    }

    fun execute(input: Map<String, Any?>): Map<String, Any?> {
        val result = runCatching { engine.execute(envelope.parse(input)) }.getOrNull()
        if (result == null || result.errors.isNotEmpty()) {
            return mapOf(
                "data" to null,
                "errors" to
                    listOf(
                        mapOf(
                            "message" to "The request could not be completed.",
                            "extensions" to mapOf("code" to "FIELD_INVALID"),
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
    fun policyGraphql(provider: PolicyProvider): PolicyGraphql {
        val schema =
            ClassPathResource(
                "graphql/schema.graphql",
            ).inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        return PolicyGraphql(schema, provider)
    }
}
