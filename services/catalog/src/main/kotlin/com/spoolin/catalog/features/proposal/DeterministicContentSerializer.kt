package com.spoolin.catalog.features.proposal

import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest

/**
 * Deterministic JSON bytes for proposal content hashing (ATDD-283 / F-DETERMINISTIC-CONTENT).
 */
object DeterministicContentSerializer {
    private val mapper = JsonMapper.builder().build()

    private val unorderedStringSetKeys =
        setOf("genres", "genre_ids", "countries")

    fun orderedContent(
        value: Any?,
        keyHint: String? = null,
    ): Any? =
        when (value) {
            null -> null
            is Map<*, *> ->
                value.entries
                    .sortedBy { it.key.toString() }
                    .associate { entry ->
                        entry.key.toString() to orderedContent(entry.value, entry.key.toString())
                    }
            is List<*> -> {
                val items = value.map { orderedContent(it) }
                if (keyHint in unorderedStringSetKeys && items.isNotEmpty() && items.all { it is String }) {
                    @Suppress("UNCHECKED_CAST")
                    (items as List<String>).sorted()
                } else {
                    items
                }
            }
            else -> value
        }

    fun json(content: Map<String, Any?>): String {
        @Suppress("UNCHECKED_CAST")
        val ordered = orderedContent(content) as Map<String, Any?>
        return mapper.writeValueAsString(ordered)
    }

    fun bytes(content: Map<String, Any?>): ByteArray = json(content).toByteArray(Charsets.UTF_8)

    fun digest(content: Map<String, Any?>): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes(content))
}
