package com.spoolin.support.logging

import tools.jackson.databind.json.JsonMapper

/** Only supported event identifiers survive; arbitrary maps, identifiers and payloads never become log text. */
object SafeJsonDiagnostics {
    private val mapper = JsonMapper.builder().build()

    fun emit(fields: Map<String, Any?>): String {
        val event = if ((fields["event"] as? String) == "draft_encrypt_failed") "draft_encrypt_failed" else "diagnostic"
        return mapper.writeValueAsString(mapOf("event" to event))
    }

    internal fun isDraftFailure(message: String?): Boolean = message == "{\"event\":\"draft_encrypt_failed\"}"
}
