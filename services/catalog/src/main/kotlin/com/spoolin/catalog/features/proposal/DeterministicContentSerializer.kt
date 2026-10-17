package com.spoolin.catalog.features.proposal

import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest

/**
 * Canonical JSON bytes for proposal content hashing (ATDD-283 / F-DETERMINISTIC-CONTENT).
 */
object DeterministicContentSerializer {
    private val mapper = JsonMapper.builder().build()

    fun json(content: Map<String, Any?>): String = mapper.writeValueAsString(content)

    fun bytes(content: Map<String, Any?>): ByteArray = json(content).toByteArray(Charsets.UTF_8)

    fun digest(content: Map<String, Any?>): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes(content))
}
