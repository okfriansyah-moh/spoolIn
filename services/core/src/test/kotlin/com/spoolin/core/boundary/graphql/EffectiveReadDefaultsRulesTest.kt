package com.spoolin.core.boundary.graphql

import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicySettings
import com.spoolin.support.policy.ReadPolicy
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EffectiveReadDefaultsRulesTest {
    @Test
    fun `actual schema coercion and introspection expose configured default without changing explicit first`() {
        val reads = ReadPolicy(PolicySettings.resolve(ConfigurationSources(emptyMap(), mapOf(
            "REQUEST_DEFAULT_PAGE" to "2", "REQUEST_MAX_PAGE" to "3",
        ))))
        val definitions = EffectiveReadDefaults.apply(
            SchemaParser().parse("type Query { titles(first: Int = 24): Int! }"), reads,
        )
        val wiring = RuntimeWiring.newRuntimeWiring().type("Query") {
            it.dataFetcher("titles") { env -> env.getArgument<Int>("first") }
        }.build()
        val schema = SchemaGenerator().makeExecutableSchema(definitions, wiring)
        val engine = GraphQL.newGraphQL(schema).build()
        assertEquals(mapOf("titles" to 2), engine.execute("{ titles }").getData<Map<String, Int>>())
        assertEquals(mapOf("titles" to 3), engine.execute("{ titles(first: 3) }").getData<Map<String, Int>>())
        val introspection = engine.execute(
            """{ __type(name: "Query") { fields { name args { name defaultValue } } } }""",
        )
        assertEquals(mapOf("__type" to mapOf("fields" to listOf(mapOf(
            "name" to "titles", "args" to listOf(mapOf("name" to "first", "defaultValue" to "2")),
        )))), introspection.getData<Map<String, Any>>())
    }
}
