package com.spoolin.catalog.features.proposal

import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest

/**
 * Canonical JSON bytes for proposal content hashing (ATDD-283 / F-DETERMINISTIC-CONTENT).
 */
object DeterministicContentSerializer {
    private val mapper = JsonMapper.builder().build()

    fun canonicalize(value: Any?): Any? =
        when (value) {
            null -> null
            is Map<*, *> ->
                value.entries
                    .sortedBy { it.key.toString() }
                    .associate { entry -> entry.key.toString() to canonicalize(entry.value) }
            is List<*> -> {
                val items = value.map { canonicalize(it) }
                if (items.isNotEmpty() && items.all { it is String }) {
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
        val canonical = canonicalize(content) as Map<String, Any?>
        return mapper.writeValueAsString(canonical)
    }

    fun bytes(content: Map<String, Any?>): ByteArray = json(content).toByteArray(Charsets.UTF_8)

    fun digest(content: Map<String, Any?>): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes(content))
}
