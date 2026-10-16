package com.spoolin.support.problem

import com.spoolin.support.policy.TextRules

internal object ProjectionValues {
    private val uuidFields =
        setOf("id", "assetId", "personId", "titleId", "accountId", "proposalId", "subjectId", "operationId")
    private val listFields = setOf("characters", "countries", "locallyChanged", "genreIds")
    private val integerFields = setOf("width", "height", "runtimeMinutes", "year", "month", "day")
    private val booleanFields = setOf("authenticated", "allowsCharacters", "hasNextPage")

    private fun scalar(
        name: String,
        value: Any?,
    ): Any? {
        if (value == null) return null
        return when (name) {
            in integerFields -> integer(value)
            in booleanFields -> boolean(value)
            in listFields -> strings(value)
            "version", "revision" -> longString(value)
            else -> string(name, value)
        }
    }

    fun strings(value: Any): List<String> {
        require(value is List<*>)
        return value.map {
            require(it is String)
            TextRules.scalars(it, "projection")
            it
        }
    }

    private fun string(
        name: String,
        value: Any,
    ): String {
        require(value is String)
        TextRules.scalars(value, "projection")
        if (name in
            uuidFields
        ) {
            require(value.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
        }
        return value
    }

    fun fields(vararg names: String): Map<String, (Any?) -> Any?> =
        names.associateWith { name -> { value -> scalar(name, value) } }

    fun objectValue(
        value: Any?,
        shape: ProjectionShape,
    ): Any? {
        if (value == null) return null
        require(value is Map<*, *> && value.keys.all { it is String })
        return shape.project(value.entries.associate { it.key.toString() to it.value })
    }

    fun objects(
        value: Any?,
        shape: ProjectionShape,
    ): List<Any?> {
        require(value is List<*>)
        return value.map { requireNotNull(objectValue(it, shape)) }
    }

    private fun longString(value: Any): String {
        val encoded = if (value is Long) value.toString() else value
        require(encoded is String && encoded.matches(Regex("-?[0-9]+")) && encoded.toLongOrNull() != null)
        return encoded
    }

    private fun integer(value: Any): Int {
        require(value is Int)
        return value
    }

    private fun boolean(value: Any): Boolean {
        require(value is Boolean)
        return value
    }
}
