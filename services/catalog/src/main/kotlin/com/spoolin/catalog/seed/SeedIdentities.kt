package com.spoolin.catalog.seed

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

private const val LENGTH_PREFIX_BYTES = 4

object SeedIdentities {
    private val mapper = JsonMapper.builder().build()
    private val excluded = setOf("generatedAt", "retrievedAt", "snapshotSHA256", "contentSHA256")

    fun record(
        namespace: UUID,
        source: String,
        kind: String,
        externalId: String,
    ): UUID {
        val parts = listOf(source, kind, externalId).map { it.toByteArray(Charsets.UTF_8) }
        val name = ByteBuffer.allocate(parts.sumOf { it.size + LENGTH_PREFIX_BYTES })
        parts.forEach { name.putInt(it.size).put(it) }
        return namedIdentity(namespace, name.array())
    }

    fun initialization(
        namespace: UUID,
        version: String,
    ): UUID =
        namedIdentity(namespace, "initialization/catalog/CATALOG_INITIALIZATION/$version".toByteArray(Charsets.UTF_8))

    fun named(
        namespace: UUID,
        name: String,
    ): UUID = namedIdentity(namespace, name.toByteArray(Charsets.UTF_8))

    private fun namedIdentity(
        namespace: UUID,
        name: ByteArray,
    ): UUID =
        com.spoolin.support.identity.NamedIdentities
            .id(namespace, name)

    fun hash(node: JsonNode): ByteArray {
        val encoded = mapper.writeValueAsBytes(stableValue(node))
        return MessageDigest.getInstance("SHA-256").digest(encoded)
    }

    fun hashHex(node: JsonNode): String = HexFormat.of().formatHex(hash(node))

    private fun stableValue(node: JsonNode): Any? =
        when {
            node.isObject -> stableObject(node)
            node.isArray -> node.toList().map(::stableValue)
            node.isNull -> null
            node.isBoolean -> node.asBoolean()
            node.isIntegralNumber -> node.asLong()
            node.isNumber -> node.decimalValue()
            else -> node.asText()
        }

    private fun stableObject(node: JsonNode): Map<String, Any?> =
        node
            .propertyNames()
            .asSequence()
            .filter { it !in excluded }
            .sorted()
            .associateWith { stableValue(node[it]) }
            .toSortedMap()
}
