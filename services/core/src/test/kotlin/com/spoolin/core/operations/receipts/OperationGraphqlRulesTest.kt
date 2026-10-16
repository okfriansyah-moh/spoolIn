package com.spoolin.core.operations.receipts

import com.spoolin.core.boundary.graphql.PolicyGraphql
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicySettings
import com.spoolin.support.receipts.OperationOwner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class OperationGraphqlRulesTest {
    private val schema =
        requireNotNull(javaClass.getResourceAsStream("/graphql/schema.graphql"))
            .use { it.readBytes().toString(Charsets.UTF_8) }
    private val provider = PolicyProvider(PolicySettings.resolve(ConfigurationSources()))

    @Test
    fun `generated SDL operation composes typed routing without replacing policy`() {
        val id = UUID.randomUUID()
        val engine =
            PolicyGraphql(schema, provider) { requested, owner ->
                assertEquals(id, requested)
                assertEquals(OperationOwner.CORE, owner)
                mapOf("id" to id.toString(), "state" to "SUCCEEDED", "resultExpired" to false)
            }
        val result =
            engine.execute(
                mapOf(
                    "query" to (
                        "query(\$id: UUID!) { " +
                            "operation(id: \$id, owner: CORE) { id state resultExpired } }"
                    ),
                    "variables" to mapOf("id" to id.toString()),
                ),
            )
        assertEquals(
            mapOf(
                "data" to
                    mapOf(
                        "operation" to
                            mapOf(
                                "id" to id.toString(),
                                "state" to "SUCCEEDED",
                                "resultExpired" to false,
                            ),
                    ),
            ),
            result,
        )
        assertEquals(
            mapOf("data" to mapOf("policy" to mapOf("revision" to provider.policy.revision))),
            engine.execute(mapOf("query" to "{ policy { revision } }")),
        )
    }

    @Test
    fun `ambiguous UUID yields explicit safe problem and default resolver remains hidden`() {
        val id = UUID.randomUUID()
        val query = "query(\$id: UUID!) { operation(id: \$id) { id } }"
        val request = mapOf("query" to query, "variables" to mapOf("id" to id.toString()))
        val engine = PolicyGraphql(schema, provider) { _, _ -> throw AmbiguousOperation() }
        assertEquals(
            mapOf(
                "data" to null,
                "errors" to
                    listOf(
                        mapOf(
                            "message" to "The request could not be completed.",
                            "extensions" to mapOf("code" to "AMBIGUOUS_OPERATION"),
                        ),
                    ),
            ),
            engine.execute(request),
        )
        assertEquals(mapOf("data" to mapOf("operation" to null)), PolicyGraphql(schema, provider).execute(request))
    }
}
