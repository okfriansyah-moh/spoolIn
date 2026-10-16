package com.spoolin.core.boundary.graphql

import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PolicyJson
import com.spoolin.support.policy.PublicPolicy
import graphql.ExecutionInput
import graphql.language.Field
import graphql.language.FragmentDefinition
import graphql.language.FragmentSpread
import graphql.language.InlineFragment
import graphql.language.OperationDefinition
import graphql.language.Selection
import graphql.language.SelectionSet
import graphql.parser.Parser

private const val ENVELOPE_CORRECTION = "Supply one bounded GraphQL operation with valid typed variables."

private fun invalidEnvelope(): Nothing = throw InputViolation("request", ENVELOPE_CORRECTION)

class GraphqlEnvelope(
    private val policy: PublicPolicy,
) {
    fun parse(input: Map<String, Any?>): ExecutionInput {
        val query = query(input)
        val name = input["operationName"]
        if (name != null && name !is String) invalidEnvelope()
        val variables = variables(input)
        val document = Parser().parseDocument(query)
        val operations = document.getDefinitionsOfType(OperationDefinition::class.java)
        if (operations.size != 1) invalidEnvelope()
        val operation = operations.single()
        val fragments = document.getDefinitionsOfType(FragmentDefinition::class.java).associateBy { it.name }
        val budget = SelectionBudget(policy.requestLimits, fragments)
        budget.count(operation.selectionSet)
        checkBody(input, operation, budget.roots(operation.selectionSet))
        return ExecutionInput
            .newExecutionInput(query)
            .operationName(name as? String)
            .variables(variables)
            .build()
    }

    private fun query(input: Map<String, Any?>): String {
        if (input.keys.any { it !in setOf("query", "operationName", "variables") }) invalidEnvelope()
        val query = input["query"] as? String ?: invalidEnvelope()
        if (query.toByteArray(Charsets.UTF_8).size > limit("graphqlQueryBytes")) invalidEnvelope()
        return query
    }

    private fun variables(input: Map<String, Any?>): Map<String, Any?> {
        val variables = input["variables"] ?: emptyMap<String, Any?>()
        if (variables !is Map<*, *> || variables.keys.any { it !is String }) invalidEnvelope()
        return variables.entries.associate { it.key.toString() to it.value }
    }

    private fun checkBody(
        input: Map<String, Any?>,
        operation: OperationDefinition,
        root: List<Field>,
    ) {
        if (operation.operation == OperationDefinition.Operation.MUTATION && root.size != 1) invalidEnvelope()
        val upload = root.singleOrNull()?.name == "uploadArtwork"
        val bytes = PolicyJson.encode(input).toByteArray(Charsets.UTF_8).size
        if (bytes > limit(if (upload) "uploadEnvelopeBytes" else "metadataBytes")) invalidEnvelope()
    }

    private fun limit(name: String): Int = policy.requestLimits.getValue(name)
}

private class SelectionBudget(
    private val limits: Map<String, Int>,
    private val fragments: Map<String, FragmentDefinition>,
) {
    fun count(
        selection: SelectionSet?,
        seen: Set<String> = emptySet(),
        depth: Int = 1,
    ): Pair<Int, Int> {
        var fields = 0
        var aliases = 0
        if (depth > limit("maxDepth") && selection != null) invalidEnvelope()
        selection?.selections.orEmpty().forEach { node ->
            val part = part(node, seen, depth)
            fields += part.first
            aliases += part.second
            if (fields > limit("maxFields") || aliases > limit("maxAliases")) invalidEnvelope()
            if (fields > limit("maxCost")) invalidEnvelope()
        }
        return fields to aliases
    }

    fun roots(selection: SelectionSet): List<Field> =
        selection.selections.flatMap { node ->
            when (node) {
                is Field -> listOf(node)
                is InlineFragment -> roots(node.selectionSet)
                is FragmentSpread -> roots(fragments[node.name]?.selectionSet ?: invalidEnvelope())
                else -> invalidEnvelope()
            }
        }

    private fun part(
        node: Selection<*>,
        seen: Set<String>,
        depth: Int,
    ): Pair<Int, Int> =
        when (node) {
            is Field -> field(node, seen, depth)
            is InlineFragment -> count(node.selectionSet, seen, depth)
            is FragmentSpread -> {
                if (node.name in seen) invalidEnvelope()
                count(fragments[node.name]?.selectionSet ?: invalidEnvelope(), seen + node.name, depth)
            }
            else -> invalidEnvelope()
        }

    private fun field(
        node: Field,
        seen: Set<String>,
        depth: Int,
    ): Pair<Int, Int> {
        val (fields, aliases) = count(node.selectionSet, seen, depth + 1)
        val alias = if (node.alias == null) 0 else 1
        return fields + 1 to aliases + alias
    }

    private fun limit(name: String): Int = limits.getValue(name)
}
