package com.spoolin.support.notifications

import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.OperationalSettings
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.json.JsonMapper
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/** The terminal source row is durable input; a failed recording transaction is retried on the next scan. */
class ExhaustionNotifications(
    private val dataSource: DataSource,
    private val service: String,
    private val mode: String = "database",
    private val environment: String = "local",
    private val retry: NotificationRetry = NotificationRetry(),
) {
    init {
        require(service in setOf("core", "catalog", "media"))
        require(mode in setOf("database", "smtp", "slack", "google_chat"))
        require(environment.matches(Regex("[A-Za-z0-9_-]{1,128}")))
    }

    private val mapper = JsonMapper.builder().build()

    fun recordExhaustion(): Int =
        dataSource.committed { connection ->
            val rows =
                connection.queryBound(
                    "SELECT w.id,w.operation_id,w.kind,w.generation,w.attempt_count,w.max_attempts," +
                        "w.correlation_id FROM durable_work w WHERE w.state='EXHAUSTED' AND w.kind<>'NOTIFICATION' " +
                        "AND NOT EXISTS (SELECT 1 FROM durable_work n WHERE n.kind='NOTIFICATION' " +
                        "AND n.notification_key IN " +
                        "('work:'||w.id::text||':generation:'||w.generation::text," +
                        "w.id::text||':'||w.generation::text)) " +
                        "ORDER BY w.updated_at,w.id LIMIT 10 FOR UPDATE OF w SKIP LOCKED",
                    emptyList(),
                ) { row ->
                    Exhausted(
                        row.getObject("id", UUID::class.java),
                        row.getObject("operation_id", UUID::class.java),
                        row.getString("kind"),
                        row.getInt("generation"),
                        row.getInt("attempt_count"),
                        row.getInt("max_attempts"),
                        row.getObject("correlation_id", UUID::class.java),
                    )
                }
            rows.count { insert(connection, it) }
        }

    private fun insert(
        connection: Connection,
        source: Exhausted,
    ): Boolean {
        val id = WorkRequest("notification-${source.generation}", "WORK", source.id, Instant.MAX).id(source.id)
        val now =
            connection
                .queryBound(
                    "SELECT clock_timestamp()",
                    emptyList(),
                ) { it.getTimestamp(1).toInstant() }
                .single()
        val timestamp = Timestamp.from(now)
        val database = mode == "database"
        val summary = summary(source)
        val inserted =
            connection.executeBound(
                "INSERT INTO durable_work (id,operation_id,kind,payload,state,max_attempts," +
                    "deadline_at,correlation_id,base_delay_ms,cap_delay_ms,backoff_multiplier," +
                    "notification_key,notification_mode," +
                    "notification_state,notification_summary,created_at,updated_at,next_attempt_at) " +
                    "VALUES (?,?,'NOTIFICATION','{}'::jsonb,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?) ON CONFLICT DO NOTHING",
                id,
                source.operation,
                if (database) "DONE" else "READY",
                retry.maxAttempts,
                Timestamp.from(if (database) now else now.plusMillis(retry.deadlineMs.toLong())),
                source.correlation,
                retry.baseDelayMs,
                retry.capDelayMs,
                retry.multiplier,
                "work:${source.id}:generation:${source.generation}",
                mode,
                if (database) "RECORDED" else "PENDING",
                summary,
                timestamp,
                timestamp,
                timestamp,
            ) == 1
        if (inserted) appendEvent(connection, source, id, summary)
        return inserted
    }

    private fun appendEvent(connection: Connection, source: Exhausted, id: UUID, summary: String) {
        check(
            connection.executeBound(
                "INSERT INTO operational_event (id,operation_id,actor_kind,action," +
                    "subject_kind,subject_id,outcome,changes,correlation_id,event_type) " +
                    "VALUES (?,?,'SERVICE','WORK_EXHAUSTION_RECORDED','NOTIFICATION',?," +
                    "'SUCCEEDED',?::jsonb,?,'ACTIVITY')",
                UUID.randomUUID(), source.operation, id, summary, source.correlation,
            ) == 1,
        )
    }

    private fun summary(source: Exhausted): String =
        mapper.writeValueAsString(
            buildMap {
                put("environment", environment)
                put("service", service)
                put("workId", source.id.toString())
                source.operation?.let { put("operationId", it.toString()) }
                put("class", if (source.kind.matches(Regex("[A-Z][A-Z0-9_]{0,63}"))) source.kind else "WORK")
                put(
                    "attemptSummary",
                    "${source.attempts} of ${source.maximum}; generation ${source.generation}; exhausted",
                )
                put("runbook", "docs/04-spoolin-techdoc.md#77-durable-work-retries-and-notifications")
            },
        )

    private data class Exhausted(
        val id: UUID,
        val operation: UUID?,
        val kind: String,
        val generation: Int,
        val attempts: Int,
        val maximum: Int,
        val correlation: UUID,
    )

    companion object {
        fun configured(
            dataSource: DataSource,
            service: String,
            read: (String) -> String?,
        ): ExhaustionNotifications {
            val source =
                ConfigurationSources(
                    OperationalSettings.retry
                        .mapNotNull { setting ->
                            read(setting.name)?.let {
                                setting.name to
                                    it
                            }
                        }.toMap(),
                )
            val retry =
                OperationalSettings.retry.associate { setting ->
                    setting.name to
                        source.integer(setting.name, setting.default, setting.min, setting.max).value
                }
            return ExhaustionNotifications(
                dataSource,
                service,
                read("NOTIFICATION_MODE") ?: "database",
                read("SPOOLIN_ENVIRONMENT") ?: "local",
                NotificationRetry(
                    retry.getValue("RETRY_MAX_RETRIES") + 1,
                    retry.getValue("RETRY_BASE_DELAY_MS"),
                    retry.getValue("RETRY_MAX_DELAY_MS"),
                    retry.getValue("RETRY_MULTIPLIER"),
                    retry.getValue("RETRY_DEADLINE_MS"),
                ),
            )
        }
    }
}
