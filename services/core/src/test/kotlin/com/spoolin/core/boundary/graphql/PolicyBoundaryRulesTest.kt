package com.spoolin.core.boundary.graphql

import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicySettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class PolicyBoundaryRulesTest {
    @Test
    fun `ATDD-251-T1-R02 generated SDL engine resolves exact public policy locally`() {
        val provider = PolicyProvider(PolicySettings.resolve(ConfigurationSources()))
        val schema =
            requireNotNull(
                javaClass.getResourceAsStream("/graphql/schema.graphql"),
            ).use { it.readBytes().toString(Charsets.UTF_8) }
        val engine = PolicyGraphql(schema, provider)
        val result = engine.execute(mapOf("query" to "{ policy { revision textBounds artwork requestLimits } }"))
        assertEquals(mapOf("data" to mapOf("policy" to provider.projection())), result)
        val invalid = engine.execute(mapOf("query" to "query { private_password_field }"))
        assertFalse(invalid.toString().contains("private_password_field"))
        assertTrue(invalid.containsKey("errors"))
    }

    @TestFactory
    fun scalarRecipes() =
        listOf(
            Triple("UUID", "c1bb0e81-232b-5f83-9d73-a5273dcac001", true),
            Triple("UUID", "C1BB0E81-232B-5F83-9D73-A5273DCAC001", false),
            Triple("Long", "9223372036854775807", true),
            Triple("Long", "-9223372036854775808", true),
            Triple("Long", "9223372036854775808", false),
            Triple("Long", 5, false),
            Triple("DateTime", "2026-10-12T03:00:00.123456Z", true),
            Triple("DateTime", "2026-10-12T03:00:00.1234567Z", false),
            Triple("DateTime", "2025-02-29T00:00:00Z", false),
            Triple("DateTime", "2026-10-12T03:00:00+00:00", false),
            Triple("Base64", "YQ==", true),
            Triple("Base64", "YR==", false),
            Triple("Base64", "data:text/plain,YQ==", false),
            Triple("Base64", " YQ==", false),
            Triple("Cursor", "x".repeat(1024), true),
            Triple("Cursor", "x".repeat(1025), false),
        ).mapIndexed { index, (name, value, accepted) ->
            dynamicTest("T009 strict scalar recipe $index $name") {
                if (accepted) {
                    assertEquals(value, PolicyScalars.validate(name, value))
                } else {
                    assertThrows(IllegalArgumentException::class.java) { PolicyScalars.validate(name, value) }
                }
            }
        }

    @Test
    fun `T009 envelope rejects unknown and excessive fields without leaking values`() {
        val policy = PolicySettings.resolve(ConfigurationSources())
        val envelope = GraphqlEnvelope(policy)
        assertEquals("{ policy { revision } }", envelope.parse(mapOf("query" to "{ policy { revision } }")).query)
        assertThrows(IllegalArgumentException::class.java) {
            envelope.parse(
                mapOf(
                    "query" to "{ policy { revision } }",
                    "unknown" to "private",
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) { envelope.parse(mapOf("query" to "x".repeat(16385))) }
        assertThrows(IllegalArgumentException::class.java) {
            envelope.parse(
                mapOf("query" to "{ " + (1..21).joinToString(" ") { "p$it: policy { revision }" } + " }"),
            )
        }
        assertEquals(
            "{ policy { ...P } } fragment P on Policy { revision }",
            envelope
                .parse(
                    mapOf("query" to "{ policy { ...P } } fragment P on Policy { revision }"),
                ).query,
        )
    }

    @Test
    fun `T009 mutation root count includes named and inline fragments`() {
        val envelope = GraphqlEnvelope(PolicySettings.resolve(ConfigurationSources()))
        val named =
            "mutation { ...Actions } fragment Actions on Mutation { " +
                "a: signOut { problem { code } } b: signOut { problem { code } } }"
        val inline = "mutation { ... on Mutation { a: signOut { problem { code } } b: signOut { problem { code } } } }"
        for (query in listOf(named, inline)) {
            assertThrows(IllegalArgumentException::class.java) { envelope.parse(mapOf("query" to query)) }
        }
    }
}
