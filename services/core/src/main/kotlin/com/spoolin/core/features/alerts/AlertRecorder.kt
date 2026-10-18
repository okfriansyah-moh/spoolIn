package com.spoolin.core.features.alerts

import com.spoolin.core.operations.metrics.WorkPendingMetrics
import com.spoolin.support.receipts.executeBound
import tools.jackson.databind.json.JsonMapper
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

/** Each new episode and its activity event share one commit. */
class AlertRecorder(
    private val dataSource: DataSource,
    private val workPendingMetrics: WorkPendingMetrics? = null,
) {
    private val mapper = JsonMapper.builder().build()

    fun recordBatch(episodes: List<AlertEpisode>, policy: AlertPolicy): Int {
        require(episodes.size in 1..20)
        val inserted = dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use {
                    it.execute("SET LOCAL statement_timeout = '1500ms'")
                    it.execute("SET LOCAL lock_timeout = '1000ms'")
                }
                val count = episodes.count { insert(connection, it, policy) }
                connection.commit()
                count
            } catch (failure: Exception) {
                connection.rollback()
                throw failure
            }
        }
        repeat(inserted) { workPendingMetrics?.markAlertEpisode() }
        return inserted
    }

    private fun insert(connection: Connection, episode: AlertEpisode, policy: AlertPolicy): Boolean {
        val id = UUID.randomUUID()
        val correlation = UUID.randomUUID()
        val database = policy.mode == AlertMode.DATABASE
        val sql = "INSERT INTO durable_work (id,kind,payload,state,max_attempts,deadline_at,correlation_id," +
            "base_delay_ms,cap_delay_ms,backoff_multiplier,notification_key,notification_mode," +
            "notification_state,notification_summary) VALUES (?,'NOTIFICATION','{}'::jsonb,?,?," +
            "CURRENT_TIMESTAMP + (? * INTERVAL '1 millisecond'),?,?,?,?,?,?,?,?::jsonb) " +
            "ON CONFLICT (notification_key) DO NOTHING"
        val created = connection.executeBound(sql, id, if (database) "DONE" else "READY", policy.maxAttempts,
            if (database) 0 else policy.deadlineMs, correlation, policy.baseDelayMs, policy.capDelayMs,
            policy.multiplier, episode.key, policy.mode.value, if (database) "RECORDED" else "PENDING",
            mapper.writeValueAsString(episode.summary)) == 1
        if (created) check(connection.executeBound(
            "INSERT INTO operational_event (id,actor_kind,action,subject_kind,subject_id,outcome,changes," +
                "correlation_id,event_type) VALUES (?,'SERVICE','ALERT_RECORDED','NOTIFICATION',?,'SUCCEEDED',?::jsonb,?,'ACTIVITY')",
            UUID.randomUUID(), id, mapper.writeValueAsString(episode.summary), correlation) == 1)
        return created
    }
}
