package com.spoolin.support.logging

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

fun applyNormalizedLogLevel(raw: String?) {
    try {
        System.setProperty("LOG_LEVEL", normalizeConfiguredLogLevel(raw))
    } catch (error: InvalidLogLevelException) {
        System.err.println("startup: ${error.message}")
        kotlin.system.exitProcess(78)
    }
}
