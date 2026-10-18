package com.spoolin.catalog.features.artwork

import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/** Database mode records exhaustion once; external modes retain a pending delivery obligation. */
class ArtworkNotifications(private val dataSource: DataSource, private val mode: String) {
    private val mapper = JsonMapper.builder().build()
    init { require(mode in setOf("database", "smtp", "slack", "google_chat")) }

    fun recordExhaustion() = dataSource.committed { connection ->
        val work = connection.queryBound("SELECT w.id,w.operation_id,w.generation,w.correlation_id FROM durable_work w " +
            "WHERE w.kind IN ('ARTWORK_ATTACH','ARTWORK_CONFIRM','ARTWORK_RELEASE') AND w.state='EXHAUSTED' " +
            "AND NOT EXISTS(SELECT 1 FROM durable_work n WHERE n.notification_key=w.id::text||':'||w.generation::text) " +
            "ORDER BY w.updated_at,w.id LIMIT 10 FOR UPDATE OF w SKIP LOCKED", emptyList()) { row ->
            Exhausted(row.getObject("id", UUID::class.java), row.getObject("operation_id", UUID::class.java), row.getInt("generation"), row.getObject("correlation_id", UUID::class.java))
        }
        work.forEach { source ->
            val notification = WorkRequest("notification-${source.generation}", "ARTWORK", source.id, Instant.MAX).id(source.id)
            val summary = mapper.writeValueAsString(mapOf("code" to "ARTWORK_WORK_EXHAUSTED", "workId" to source.id.toString(), "generation" to source.generation))
            connection.executeBound("INSERT INTO durable_work(id,operation_id,kind,payload,state,max_attempts,deadline_at,correlation_id," +
                "base_delay_ms,cap_delay_ms,backoff_multiplier,notification_key,notification_mode,notification_state,notification_summary) " +
                "VALUES(?,?,'NOTIFICATION',?::jsonb,?,3,clock_timestamp()+INTERVAL '5 minutes',?,500,5000,2,?,?,?,?::jsonb) ON CONFLICT(notification_key) DO NOTHING",
                notification, source.operation, "{\"subjectId\":\"${source.id}\"}", if (mode == "database") "DONE" else "READY", source.correlation,
                "${source.id}:${source.generation}", mode, if (mode == "database") "RECORDED" else "PENDING", summary)
        }
    }

    private data class Exhausted(val id: UUID, val operation: UUID?, val generation: Int, val correlation: UUID)
}
