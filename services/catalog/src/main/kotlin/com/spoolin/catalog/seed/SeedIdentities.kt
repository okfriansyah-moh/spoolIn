package com.spoolin.catalog.seed

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

private const val LENGTH_PREFIX_BYTES = 4
private const val UUID_BYTES = 16
private const val VERSION_BYTE_INDEX = 6
private const val VARIANT_BYTE_INDEX = 8
private const val VERSION_LOW_MASK = 0x0f
private const val VERSION_FIVE_BITS = 0x50
private const val VARIANT_LOW_MASK = 0x3f
private const val IETF_VARIANT_BITS = 0x80

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
        return uuid5(namespace, name.array())
    }

    fun initialization(
        namespace: UUID,
        version: String,
    ): UUID = uuid5(namespace, "initialization/catalog/CATALOG_INITIALIZATION/$version".toByteArray(Charsets.UTF_8))

    private fun uuid5(
        namespace: UUID,
        name: ByteArray,
    ): UUID {
        val prefix =
            ByteBuffer
                .allocate(UUID_BYTES)
                .putLong(namespace.mostSignificantBits)
                .putLong(namespace.leastSignificantBits)
                .array()
        val digest = MessageDigest.getInstance("SHA-1").digest(prefix + name)
        digest[VERSION_BYTE_INDEX] =
            ((digest[VERSION_BYTE_INDEX].toInt() and VERSION_LOW_MASK) or VERSION_FIVE_BITS).toByte()
        digest[VARIANT_BYTE_INDEX] =
            ((digest[VARIANT_BYTE_INDEX].toInt() and VARIANT_LOW_MASK) or IETF_VARIANT_BITS).toByte()
        val bytes = ByteBuffer.wrap(digest)
        return UUID(bytes.long, bytes.long)
    }

    fun hash(node: JsonNode): ByteArray = MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(stableValue(node)))

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
