package com.spoolin.support.logging

import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import kotlin.system.exitProcess

class InvalidLogLevelException(
    message: String,
) : RuntimeException(message)

/** TD-LOG-CONFIG: only info and debug are permitted; omitted defaults to info. */
fun normalizeConfiguredLogLevel(raw: String?): String {
    val normalized = raw?.trim()?.lowercase()
    return when (normalized) {
        null, "info" -> "INFO"
        "debug" -> "DEBUG"
        "" -> throw InvalidLogLevelException("LOG_LEVEL must be info or debug")
        else -> throw InvalidLogLevelException("LOG_LEVEL must be info or debug")
    }
}

fun applyNormalizedLogLevel(
    raw: String?,
    service: String,
) {
    applyNormalizedLogLevel(raw, service, { bytes ->
        BoundedDiagnosticSink(
            service,
            FileOutputStream(FileDescriptor.err),
            FileOutputStream(FileDescriptor.out),
            DiagnosticDropMetrics.shared,
        ).use { it.submit(bytes, DiagnosticLevel.FATAL) }
    }, ::exitProcess)
}

internal fun applyNormalizedLogLevel(
    raw: String?,
    service: String,
    diagnostic: (ByteArray) -> Unit,
    terminate: (Int) -> Nothing,
) {
    require(service in DiagnosticDropMetrics.SERVICES)
    val normalized =
        try {
            normalizeConfiguredLogLevel(raw)
        } catch (_: InvalidLogLevelException) {
            null
        }
    if (normalized != null) {
        System.setProperty("LOG_LEVEL", normalized)
        return
    }
    val environment = System.getenv("SPOOLIN_ENVIRONMENT").takeIf { it in setOf("local", "test") } ?: "local"
    val bytes = DiagnosticEnvelope(service, "0.0.0-foundation", environment).invalidLogLevel(System.currentTimeMillis())
    try {
        diagnostic(bytes)
    } catch (_: IOException) {
        DiagnosticDropMetrics.shared.record(service, DiagnosticLevel.FATAL, DiagnosticDrop.SINK_FAILED)
    } finally {
        terminate(INVALID_CONFIG_EXIT)
    }
}

private const val INVALID_CONFIG_EXIT = 78
