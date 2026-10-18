package com.spoolin.catalog.seed

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

object SeedIdentities {
    private val mapper = JsonMapper.builder().build()
    private val excluded = setOf("generatedAt", "retrievedAt", "snapshotSHA256", "contentSHA256")

    fun record(namespace: UUID, source: String, kind: String, externalId: String): UUID {
        val parts = listOf(source, kind, externalId).map { it.toByteArray(Charsets.UTF_8) }
        val name = ByteBuffer.allocate(parts.sumOf { it.size + 4 })
        parts.forEach { name.putInt(it.size).put(it) }
        return uuid5(namespace, name.array())
    }

    fun initialization(namespace: UUID, version: String): UUID =
        uuid5(namespace, "initialization/catalog/CATALOG_INITIALIZATION/$version".toByteArray(Charsets.UTF_8))

    private fun uuid5(namespace: UUID, name: ByteArray): UUID {
        val prefix = ByteBuffer.allocate(16).putLong(namespace.mostSignificantBits).putLong(namespace.leastSignificantBits).array()
        val digest = MessageDigest.getInstance("SHA-1").digest(prefix + name)
        digest[6] = ((digest[6].toInt() and 15) or 80).toByte()
        digest[8] = ((digest[8].toInt() and 63) or 128).toByte()
        val bytes = ByteBuffer.wrap(digest)
        return UUID(bytes.long, bytes.long)
    }

    fun hash(node: JsonNode): ByteArray = MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(stableValue(node)))
    fun hashHex(node: JsonNode): String = HexFormat.of().formatHex(hash(node))

    private fun stableValue(node: JsonNode): Any? = when {
        node.isObject -> node.propertyNames().asSequence().filter { it !in excluded }.sorted().associateWith { stableValue(node[it]) }.toSortedMap()
        node.isArray -> node.toList().map(::stableValue)
        node.isNull -> null
        node.isBoolean -> node.asBoolean()
        node.isIntegralNumber -> node.asLong()
        node.isNumber -> node.decimalValue()
        else -> node.asText()
    }
}
