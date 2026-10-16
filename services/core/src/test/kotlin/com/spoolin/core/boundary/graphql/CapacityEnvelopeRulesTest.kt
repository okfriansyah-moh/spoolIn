package com.spoolin.core.boundary.graphql

import graphql.parser.Parser
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.validation.Validator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.util.Locale

class CapacityEnvelopeRulesTest {
    @Test
    fun `C01 independent complete capacity requests remain valid against generated SDL`() {
        val schema =
            requireNotNull(javaClass.getResourceAsStream("/graphql/schema.graphql"))
                .use { it.readBytes().toString(Charsets.UTF_8) }
        val wiring = RuntimeWiring.newRuntimeWiring()
        PolicyScalars.all().forEach(wiring::scalar)
        val executable = SchemaGenerator().makeExecutableSchema(SchemaParser().parse(schema), wiring.build())
        val vectors =
            JsonMapper
                .builder()
                .build()
                .readTree(
                    Path.of("../../tests/contracts/policy/request-capacity-vectors.json").toFile(),
                ).get("vectors")
        val queries =
            vectors
                .properties()
                .map {
                    it.value
                        .get("envelope")
                        .get("query")
                        .asString()
                }.distinct()
        assertTrue(queries.isNotEmpty())
        queries.forEach { query ->
            val errors = Validator().validateDocument(executable, Parser().parseDocument(query), Locale.ROOT)
            assertTrue(errors.isEmpty(), "Independent capacity request must satisfy the actual generated SDL: $errors")
        }
    }
}
