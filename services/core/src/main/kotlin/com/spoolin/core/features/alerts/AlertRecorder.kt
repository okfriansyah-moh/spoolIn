package com.spoolin.core.features.alerts

import com.spoolin.core.operations.metrics.WorkPendingMetrics
import com.spoolin.support.receipts.executeBound
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import javax.sql.DataSource

enum class AlertMode {
    DATABASE,
    SMTP,
    SLACK,
    GOOGLE_CHAT,
}

/** Records sanitized operational alerts (T034). */
class AlertRecorder(
    private val dataSource: DataSource,
    private val workPendingMetrics: WorkPendingMetrics? = null,
) {
    private val mapper = JsonMapper.builder().build()

    fun record(
        kind: String,
        summary: Map<String, String>,
        mode: AlertMode = AlertMode.DATABASE,
    ): UUID {
        val id = UUID.randomUUID()
        val json = mapper.writeValueAsString(summary)
        dataSource.connection.use { connection ->
            connection.executeBound(
                "INSERT INTO actionable_alert (id, kind, mode, summary) VALUES (?, ?, ?, ?::jsonb)",
                id,
                kind,
                mode.name.lowercase(),
                json,
            )
        }
        workPendingMetrics?.markAlertEpisode()
        return id
    }
}
