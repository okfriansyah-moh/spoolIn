package com.spoolin.support.logging

/** TD-LOG-CONFIG: only info and debug are permitted; others fall back to info. */
fun normalizeConfiguredLogLevel(raw: String?): String =
    when (raw?.trim()?.lowercase()) {
        "debug" -> "DEBUG"
        "info", null, "" -> "INFO"
        else -> "INFO"
    }

fun applyNormalizedLogLevel(raw: String?) {
    System.setProperty("LOG_LEVEL", normalizeConfiguredLogLevel(raw))
}
