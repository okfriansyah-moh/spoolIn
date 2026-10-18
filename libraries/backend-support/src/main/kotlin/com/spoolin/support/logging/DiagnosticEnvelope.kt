package com.spoolin.support.logging

import tools.jackson.databind.json.JsonMapper
import com.spoolin.support.tracing.TraceContexts
import java.time.Instant

internal const val DIAGNOSTIC_BYTES = 16 * 1024
internal const val DIAGNOSTIC_QUEUE = 1024
private const val VERSION_BYTES = 128

/** Framework text, argument arrays, MDC and exceptions never enter the diagnostic envelope. */
internal class DiagnosticEnvelope(val service: String, version: String, environment: String) {
    private val resource: Map<String, String>
    init {
        require(service in DiagnosticDropMetrics.SERVICES)
        require(environment in setOf("local", "test"))
        require(version.toByteArray(Charsets.UTF_8).size in 1..VERSION_BYTES)
        require(version.matches(Regex("[A-Za-z0-9._+-]+")))
        resource = mapOf("service.name" to service, "service.version" to version,
            "service.namespace" to "spoolin", "service.env" to environment)
    }

    fun encode(level: DiagnosticLevel, timestamp: Long, logger: String?): ByteArray {
        val message = when (logger) {
            "com.spoolin.core.features.draft.SaveDraft" -> "Draft encryption failed"
            "com.spoolin.core.features.draft.DraftCryptography" -> "Protected draft unavailable"
            "com.spoolin.core.features.account.persistence.AccountCryptography" -> "Protected account unavailable"
            else -> "Diagnostic event"
        }
        return record(level, timestamp, message)
    }

    fun invalid(timestamp: Long): ByteArray = record(DiagnosticLevel.ERROR, timestamp, "Logger contract invalid")

    fun invalidLogLevel(timestamp: Long): ByteArray =
        record(DiagnosticLevel.FATAL, timestamp, "LOG_LEVEL must be info or debug", traced = false)

    private fun record(level: DiagnosticLevel, timestamp: Long, message: String, traced: Boolean = true): ByteArray {
        val (trace, span) = if (traced) TraceContexts.identifiers() else
            "00000000000000000000000000000000" to "0000000000000000"
        val fields = linkedMapOf<String, Any>(
            "level" to level.label, "timestamp" to Instant.ofEpochMilli(timestamp).toString(),
            "trace_id" to trace, "span_id" to span,
            "message" to message, "resource" to resource,
        )
        val encoded = mapper.writeValueAsBytes(fields) + byteArrayOf('\n'.code.toByte())
        check(encoded.size <= DIAGNOSTIC_BYTES)
        return encoded
    }

    companion object { private val mapper = JsonMapper.builder().build() }
}
