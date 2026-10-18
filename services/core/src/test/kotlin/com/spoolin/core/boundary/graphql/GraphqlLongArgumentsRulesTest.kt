package com.spoolin.core.boundary.graphql

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GraphqlLongArgumentsRulesTest {
    @Test
    fun `actual scalar coercion preserves zero negatives and every signed64bit edge at the DTO boundary`() {
        val seen = mutableListOf<Long>()
        val engine = engine(seen)
        val values = listOf("0", "-1", "9007199254740993", Long.MIN_VALUE.toString(), Long.MAX_VALUE.toString())
        for (value in values) {
            val result =
                engine.execute(
                    ExecutionInput
                        .newExecutionInput()
                        .query("query(\$version:Long!) { version(expectedVersion:\$version) }")
                        .variables(mapOf("version" to value))
                        .build(),
                )
            assertTrue(result.errors.isEmpty(), result.errors.toString())
            assertEquals(mapOf("version" to value), result.getData<Map<String, String>>())
        }
        assertEquals(values.map(String::toLong), seen)
    }

    @Test
    fun `actual scalar rejects out of range numbers numeric JSON missing and null before resolver execution`() {
        val seen = mutableListOf<Long>()
        val engine = engine(seen)
        val invalid = listOf<Any?>("9223372036854775808", "-9223372036854775809", "1.5", 1L, null)
        for (value in invalid) {
            val result =
                engine.execute(
                    ExecutionInput
                        .newExecutionInput()
                        .query("query(\$version:Long!) { version(expectedVersion:\$version) }")
                        .variables(mapOf("version" to value))
                        .build(),
                )
            assertTrue(result.errors.isNotEmpty())
        }
        assertTrue(engine.execute("{ version }").errors.isNotEmpty())
        assertTrue(seen.isEmpty())
    }

    @Test
    fun `typed string literal uses the same production argument adapter`() {
        val seen = mutableListOf<Long>()
        val result = engine(seen).execute("""{ version(expectedVersion:"9223372036854775807") }""")
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertEquals(listOf(Long.MAX_VALUE), seen)
    }

    private fun engine(seen: MutableList<Long>): GraphQL {
        val definitions = SchemaParser().parse("scalar Long\ntype Query { version(expectedVersion:Long!):String! }")
        val wiring =
            RuntimeWiring
                .newRuntimeWiring()
                .scalar(PolicyScalars.all().single { it.name == "Long" })
                .type("Query") { type ->
                    type.dataFetcher("version") { env ->
                        val version = GraphqlLongArguments.required(env)
                        seen.add(version)
                        version.toString()
                    }
                }.build()
        return GraphQL.newGraphQL(SchemaGenerator().makeExecutableSchema(definitions, wiring)).build()
    }
}
