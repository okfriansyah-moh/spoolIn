package com.spoolin.core.features.catalogview.read

import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.util.concurrent.atomic.AtomicInteger

class CatalogMetadataProjectionRulesTest {
    @Test
    fun `all title genre labels use one actual taxonomy result per execution`() {
        val calls = AtomicInteger()
        val engine = engine(calls)
        repeat(2) {
            val result = engine.execute("{ titles { genres { id label } } }")
            assertTrue(result.errors.isEmpty(), result.errors.toString())
            val data = result.getData<Map<String, Any?>>()
            val titles = data["titles"] as List<*>
            assertEquals(100, titles.size)
            assertTrue(titles.all { it.toString().contains("Drama") })
        }
        assertEquals(2, calls.get(), "Taxonomy is cached only within its GraphQL execution")
    }

    @Test
    fun `missing taxonomy association produces a real field error instead of a fake label`() {
        val result = engine(AtomicInteger(), missing = true).execute("{ titles { genres { label } } }")
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `public provenance preserves typed owner source facts`() {
        val source = Spoolin.FieldSource.newBuilder().setField("title").setUrl("https://example.invalid/fact")
            .setReuseBasis("CC0 fact").setRetrievedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(1)).build()
        val value = CatalogMetadataProjection.fieldSources(Spoolin.Provenance.newBuilder().addFieldSources(source).build())
        val title = value["title"] as Map<*, *>
        assertEquals("https://example.invalid/fact", title["url"])
        assertEquals("CC0 fact", title["reuseBasis"])
        assertEquals("1970-01-01T00:00:01Z", title["retrievedAt"])
        assertEquals(setOf("url", "reuseBasis", "retrievedAt", "precision"), title.keys)
    }

    private fun engine(calls: AtomicInteger, missing: Boolean = false): GraphQL {
        val wiring = RuntimeWiring.newRuntimeWiring()
        wiring.type("Query") { type -> type.dataFetcher("titles") {
            List(100) { mapOf("genres" to listOf(mapOf("id" to "genre-one"))) }
        } }
        CatalogMetadataProjection {
            calls.incrementAndGet()
            Spoolin.Genres.newBuilder().also { builder ->
                if (!missing) builder.addNodes(Spoolin.Genre.newBuilder().setId("genre-one").setLabel("Drama"))
            }.build()
        }.bind(wiring)
        val schema = "type Query { titles: [Title!]! } type Title { genres: [Genre!]! } type Genre { id: ID! label: String! }"
        return GraphQL.newGraphQL(SchemaGenerator().makeExecutableSchema(SchemaParser().parse(schema), wiring.build())).build()
    }
}
