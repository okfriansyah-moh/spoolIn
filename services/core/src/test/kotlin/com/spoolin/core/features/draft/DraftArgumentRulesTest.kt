package com.spoolin.core.features.draft

import com.spoolin.core.boundary.graphql.PolicyScalars
import graphql.ExceptionWhileDataFetching
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class DraftArgumentRulesTest {
    @Test
    fun `actual save document permits an omitted version for first backup`() {
        val observed = assertInstanceOf(ObservedVersion::class.java, execute(emptyMap()))
        assertNull(observed.version)
    }

    @Test
    fun `actual save document rejects an explicitly null version`() {
        val failure =
            assertInstanceOf(
                IllegalArgumentException::class.java,
                execute(mapOf("expectedVersion" to null)),
            )
        assertEquals(
            "Explicit null for expectedVersion is not permitted; omit the argument for create",
            failure.message,
        )
    }

    @Test
    fun `actual Long scalar retains zero and versions beyond JavaScript integer precision`() {
        listOf(0L, 9007199254740993L).forEach { version ->
            val observed =
                assertInstanceOf(
                    ObservedVersion::class.java,
                    execute(mapOf("expectedVersion" to version.toString())),
                )
            assertEquals(version, observed.version)
        }
    }

    @Test
    fun `actual input coercion retains the original Catalog base version`() {
        val version = 9007199254740993L
        val input = requireNotNull(variables()["input"] as? Map<*, *>)
        val observed =
            assertInstanceOf(
                ObservedVersion::class.java,
                execute(mapOf("input" to (input + ("baseVersion" to version.toString())))),
            )
        assertEquals(version, observed.baseVersion)
    }

    @Test
    fun `actual discard document retains the exact original draft version`() {
        val variables = variables() - "input" + ("expectedVersion" to "9007199254740993")
        val observed =
            assertInstanceOf(
                ObservedVersion::class.java,
                execute(variables, "discard-draft.graphql"),
            )
        assertEquals(9007199254740993L, observed.version)
    }

    private fun execute(
        extra: Map<String, Any?>,
        documentName: String = "save-draft.graphql",
    ): Throwable {
        val schema =
            requireNotNull(javaClass.getResourceAsStream("/graphql/schema.graphql"))
                .use { it.readBytes().toString(Charsets.UTF_8) }
        val wiring = RuntimeWiring.newRuntimeWiring()
        PolicyScalars.all().forEach(wiring::scalar)
        // Observe real coercion before persistence; this deliberately supplies no successful write receipt.
        wiring.type("Mutation") {
            it.dataFetcher("saveDraft") { env ->
                val input = parseDraftGraphqlInput(requireNotNull(env.getArgument<Map<String, Any?>>("input")))
                throw ObservedVersion(draftExpectedVersion(env), input.baseVersion)
            }.dataFetcher("discardDraft") { env -> throw ObservedVersion(draftExpectedVersion(env)) }
        }
        val engine =
            GraphQL.newGraphQL(
                SchemaGenerator().makeExecutableSchema(SchemaParser().parse(schema), wiring.build()),
            ).build()
        val document = Files.readString(Path.of("../../frontend/src/lib/features/draft/$documentName"))
        val result =
            engine.execute(
                ExecutionInput.newExecutionInput()
                    .query(document)
                    .variables(variables() + extra)
                    .build(),
            )
        assertEquals(1, result.errors.size)
        return assertInstanceOf(ExceptionWhileDataFetching::class.java, result.errors.single()).exception
    }

    private fun variables(): Map<String, Any?> =
        mapOf(
            "operationId" to "33333333-3333-4333-8333-333333333333",
            "id" to "44444444-4444-4444-8444-444444444444",
            "input" to
                mapOf(
                    "targetKind" to "MOVIE",
                    "values" to mapOf("title" to "Draft coercion boundary"),
                    "policyRevision" to "test-policy",
                ),
        )

    private class ObservedVersion(
        val version: Long?,
        val baseVersion: Long? = null,
    ) : RuntimeException("Coercion observed before persistence")
}
