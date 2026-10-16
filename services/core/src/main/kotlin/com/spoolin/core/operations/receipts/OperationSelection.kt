package com.spoolin.core.operations.receipts

import com.spoolin.core.boundary.graphql.PreparedGraphql
import com.spoolin.support.receipts.OperationOwner
import graphql.language.EnumValue
import graphql.language.NullValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.language.VariableReference
import java.util.UUID

internal data class OperationSelection(
    val id: UUID,
    val owner: OperationOwner?,
)

internal fun operationSelections(parsed: PreparedGraphql): List<OperationSelection> =
    parsed.fields
        .filter { it.name == "operation" }
        .map { field ->
            val args = field.arguments.associate { it.name to value(it.value, parsed.execution.variables) }
            val text = requireNotNull(args["id"] as? String)
            val id = UUID.fromString(text).also { require(it.toString() == text) }
            OperationSelection(id, (args["owner"] as? String)?.let(OperationOwner::valueOf))
        }.distinct()

private fun value(
    node: Value<*>,
    variables: Map<String, Any>,
): Any? =
    when (node) {
        is StringValue -> node.value
        is EnumValue -> node.name
        is NullValue -> null
        is VariableReference -> variables[node.name]
        else -> throw IllegalArgumentException("Invalid operation lookup")
    }
