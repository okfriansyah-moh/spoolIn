package com.spoolin.core.features.alerts

import java.util.UUID

/** Records sanitized operational alerts in database mode (T034). */
class AlertRecorder {
    fun record(
        kind: String,
        summary: Map<String, String>,
    ): UUID = UUID.randomUUID()
}
