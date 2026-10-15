package com.spoolin.core.boundary.graphql

import com.spoolin.support.policy.PolicyJson
import com.spoolin.support.policy.TextRules
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import java.util.Base64
import java.util.Locale

private const val JSON_BYTES_LIMIT = 131072
private const val CURSOR_BYTES_LIMIT = 1024
private const val MAXIMUM_YEAR = 9999

object PolicyScalars {
    fun all(): List<GraphQLScalarType> =
        listOf("UUID", "Long", "DateTime", "Cursor", "Base64", "JSON").map { name ->
            GraphQLScalarType
                .newScalar()
                .name(name)
                .coercing(scalar(name))
                .build()
        }

    private fun scalar(name: String): Coercing<Any, Any> =
        object : Coercing<Any, Any> {
            override fun serialize(
                dataFetcherResult: Any,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): Any =
                runCatching {
                    validate(
                        name,
                        dataFetcherResult,
                    )
                }.getOrElse { throw CoercingSerializeException("Invalid $name output") }

            override fun parseValue(
                input: Any,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): Any =
                runCatching {
                    validate(
                        name,
                        input,
                    )
                }.getOrElse { throw CoercingParseValueException("Invalid $name input") }

            override fun parseLiteral(
                input: Value<*>,
                variables: CoercedVariables,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): Any {
                if (input !is StringValue ||
                    name == "JSON"
                ) {
                    throw CoercingParseLiteralException("Use a typed variable for $name")
                }
                return runCatching {
                    validate(
                        name,
                        requireNotNull(input.value),
                    )
                }.getOrElse { throw CoercingParseLiteralException("Invalid $name input") }
            }
        }

    fun validate(
        name: String,
        value: Any,
    ): Any {
        if (name == "JSON") {
            require(PolicyJson.encode(value).toByteArray(Charsets.UTF_8).size <= JSON_BYTES_LIMIT)
            return value
        }
        require(value is String)
        TextRules.scalars(value, name)
        when (name) {
            "UUID" -> require(value.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
            "Long" -> longString(value)
            "DateTime" -> timestamp(value)
            "Cursor" -> require(value.toByteArray(Charsets.UTF_8).size <= CURSOR_BYTES_LIMIT)
            "Base64" ->
                require(
                    value.matches(Regex("(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?")) &&
                        Base64.getEncoder().encodeToString(Base64.getDecoder().decode(value)) == value,
                )
            else -> error("Unknown scalar")
        }
        return value
    }

    private fun longString(value: String) {
        require(value.matches(Regex("-?[0-9]+")) && value.toLongOrNull() != null)
    }

    private fun timestamp(value: String) {
        require(value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]{1,6})?Z")))
        val parsed =
            try {
                java.time.OffsetDateTime
                    .parse(value)
                    .toInstant()
            } catch (_: java.time.DateTimeException) {
                throw IllegalArgumentException("Use a valid UTC timestamp")
            }
        require(parsed.atOffset(java.time.ZoneOffset.UTC).year in 1..MAXIMUM_YEAR)
    }
}
