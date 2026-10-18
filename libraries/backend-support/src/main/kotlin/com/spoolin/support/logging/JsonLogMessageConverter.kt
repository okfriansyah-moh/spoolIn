package com.spoolin.support.logging

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

/** Legacy pattern use receives the same fixed event summary as the root JSON appender. */
class JsonLogMessageConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String = jsonString(DiagnosticEvents.classify(event).message)
}

internal fun jsonString(raw: String): String {
    val escaped =
        buildString(raw.length + 2) {
            append('"')
            for (ch in raw) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else ->
                        if (ch.code < 0x20) {
                            append("\\u")
                            append("%04x".format(ch.code))
                        } else {
                            append(ch)
                        }
                }
            }
            append('"')
        }
    return escaped
}
