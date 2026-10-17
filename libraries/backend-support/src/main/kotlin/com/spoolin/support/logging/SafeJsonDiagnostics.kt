package com.spoolin.support.logging

import tools.jackson.databind.json.JsonMapper

private const val MAX_BYTES = 8192

object SafeJsonDiagnostics {
    private val mapper = JsonMapper.builder().build()

    fun emit(fields: Map<String, Any?>): String {
        val json = mapper.writeValueAsString(fields)
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Diagnostic envelope too large" }
        return json
    }
}
