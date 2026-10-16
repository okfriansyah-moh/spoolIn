package com.spoolin.core.operations.receipts

import com.spoolin.core.boundary.graphql.GraphqlEnvelope
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicySettings
import com.spoolin.support.receipts.OperationOwner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class OperationSelectionRulesTest {
    private val envelope = GraphqlEnvelope(PolicySettings.resolve(ConfigurationSources()))
    private val id = UUID.randomUUID()
    private val other = UUID.randomUUID()
    private val query = "query(\$id:UUID!=\"$id\",\$owner:OperationOwner=CORE){operation(id:\$id,owner:\$owner){id}}"

    @Test
    fun omittedVariablesUseDefaults() {
        assertEquals(listOf(OperationSelection(id, OperationOwner.CORE)), selections(emptyMap()))
    }

    @Test
    fun suppliedVariablesOverrideDefaultsIncludingExplicitNull() {
        assertEquals(
            listOf(OperationSelection(other, OperationOwner.MEDIA)),
            selections(mapOf("id" to other.toString(), "owner" to "MEDIA")),
        )
        assertEquals(listOf(OperationSelection(id, null)), selections(mapOf("owner" to null)))
    }

    private fun selections(variables: Map<String, Any?>): List<OperationSelection> =
        operationSelections(envelope.prepare(mapOf("query" to query, "variables" to variables)))
}
