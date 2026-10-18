package com.spoolin.core.boundary.graphql

import com.spoolin.support.policy.ReadPolicy
import graphql.language.FieldDefinition
import graphql.language.InputValueDefinition
import graphql.language.IntValue
import graphql.language.ObjectTypeDefinition
import graphql.schema.idl.TypeDefinitionRegistry
import java.math.BigInteger

/** Apply configured defaults before schema generation so coercion and introspection agree with policy. */
internal object EffectiveReadDefaults {
    fun apply(
        registry: TypeDefinitionRegistry,
        reads: ReadPolicy,
    ): TypeDefinitionRegistry {
        val first = reads.pageSize(null)
        registry.types().values.filterIsInstance<ObjectTypeDefinition>().toList().forEach { type ->
            val updated = defaults(type, first)
            registry.remove(type)
            check(registry.add(updated).isEmpty)
        }
        return registry
    }

    private fun defaults(
        type: ObjectTypeDefinition,
        first: Int,
    ): ObjectTypeDefinition =
        type.transform {
            it.fieldDefinitions(type.fieldDefinitions.map { field -> defaults(field, first) })
        }

    private fun defaults(
        field: FieldDefinition,
        first: Int,
    ): FieldDefinition =
        field.transform {
            it.inputValueDefinitions(field.inputValueDefinitions.map { input -> defaults(input, first) })
        }

    private fun defaults(
        input: InputValueDefinition,
        first: Int,
    ): InputValueDefinition =
        if (input.name == "first") {
            input.transform {
                it.defaultValue(IntValue.newIntValue(BigInteger.valueOf(first.toLong())).build())
            }
        } else {
            input
        }
}
