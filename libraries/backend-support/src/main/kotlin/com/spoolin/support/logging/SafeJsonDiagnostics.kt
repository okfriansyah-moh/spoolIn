package com.spoolin.support.logging

import tools.jackson.databind.json.JsonMapper

private const val MAX_BYTES = 8192

private val REDACT_KEYS =
    setOf(
        "password",
        "token",
        "secret",
        "authorization",
        "csrf",
        "challenge",
        "ui_challenge",
        "apikey",
        "api_key",
        "credential",
    )

object SafeJsonDiagnostics {
    private val mapper = JsonMapper.builder().build()

    fun emit(fields: Map<String, Any?>): String {
        val sanitized =
            fields.mapValues { (key, value) ->
                if (REDACT_KEYS.any { key.equals(it, ignoreCase = true) || key.contains(it, ignoreCase = true) }) {
                    "***"
                } else {
                    value
                }
            }
        val json = mapper.writeValueAsString(sanitized)
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Diagnostic envelope too large" }
        return json
    }
}
