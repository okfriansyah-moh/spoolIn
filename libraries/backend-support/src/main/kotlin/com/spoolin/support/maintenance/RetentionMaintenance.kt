package com.spoolin.support.maintenance

import java.sql.Connection
import javax.sql.DataSource

/** Each call advances fixed-size ordered batches; no payload or identity is logged. */
class RetentionMaintenance(private val maintenance: DataSource, private val owner: String) {
    init { require(owner in setOf("core", "catalog", "media")) }

    fun hourly(): Map<String, Int> = transaction { connection ->
        val counts = linkedMapOf("receiptBodies" to receiptBodies(connection))
        if (owner == "core") {
            counts["sessions"] = deleteExpired(connection, "browser_session", "id",
                "LEAST(expires_at,last_seen_at+INTERVAL '30 minutes',COALESCE(revoked_at,expires_at))<=CURRENT_TIMESTAMP-INTERVAL '7 days'")
            counts["credentials"] = deleteExpired(connection, "mcp_credential", "id",
                "expires_at<=CURRENT_TIMESTAMP OR revoked_at<=CURRENT_TIMESTAMP")
            counts["drafts"] = deleteExpired(connection, "form_draft", "id", "expires_at<=CURRENT_TIMESTAMP")
            counts["rateWindows"] = purgeRateWindows(connection)
        }
        counts
    }

    fun daily(): Map<String, Int> {
        val ids = maintenance.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("""
                    SELECT w.id FROM appdata.durable_work w WHERE ${resolved()} ${mediaResolved()}
                      AND ((w.kind<>'NOTIFICATION' AND w.payload<>'{}'::jsonb
                        AND w.updated_at<=CURRENT_TIMESTAMP-INTERVAL '7 days')
                        OR w.updated_at<=CURRENT_TIMESTAMP-INTERVAL '90 days')
                    ORDER BY w.updated_at,w.id LIMIT 100
                """.trimIndent()).use { rows -> buildList { while (rows.next()) add(rows.getObject(1)) } }
            }
        }
        val counts = linkedMapOf("payloads" to 0, "attemptEvents" to 0, "work" to 0)
        for (id in ids) {
            val result = transaction { purgeWork(it, id) }
            for ((key, value) in result) counts[key] = counts.getValue(key) + value
        }
        counts["activityEvents"] = transaction { purgeActivity(it) }
        return counts
    }

    private fun purgeWork(connection: Connection, id: Any): Map<String, Int> {
        val checkpoint = connection.setSavepoint()
        val assets = if (owner == "media") assets(connection, id, true) else emptyList()
        val locked = connection.prepareStatement("SELECT id FROM appdata.durable_work WHERE id=? FOR UPDATE").use {
            it.setObject(1, id)
            it.executeQuery().use { rows -> rows.next() }
        }
        if (!locked || !eligible(connection, id) || (owner == "media" && assets != assets(connection, id, false))) {
            connection.rollback(checkpoint)
            return emptyMap()
        }
        val payloads = update(connection, """
            UPDATE appdata.durable_work SET payload='{}'::jsonb WHERE id=? AND kind<>'NOTIFICATION'
              AND payload<>'{}'::jsonb AND updated_at<=CURRENT_TIMESTAMP-INTERVAL '7 days'
        """.trimIndent(), id)
        if (!expiredHistory(connection, id)) return mapOf("payloads" to payloads)
        if (owner == "media") clearProcessing(connection, id)
        val events = update(connection, "DELETE FROM appdata.operational_event WHERE work_id=?", id)
        val work = update(connection, "DELETE FROM appdata.durable_work WHERE id=?", id)
        connection.releaseSavepoint(checkpoint)
        return mapOf("payloads" to payloads, "attemptEvents" to events, "work" to work)
    }

    private fun assets(connection: Connection, id: Any, lock: Boolean): List<Any> =
        connection.prepareStatement("SELECT id FROM appdata.asset WHERE processing_work_id=? ORDER BY id" +
            if (lock) " FOR UPDATE" else "").use {
            it.setObject(1, id)
            it.executeQuery().use { rows -> buildList { while (rows.next()) add(rows.getObject(1)) } }
        }

    private fun eligible(connection: Connection, id: Any): Boolean =
        connection.prepareStatement("SELECT w.id FROM appdata.durable_work w WHERE w.id=? AND ${resolved()} ${mediaResolved()}").use {
            it.setObject(1, id)
            it.executeQuery().use { rows -> rows.next() }
        }

    private fun expiredHistory(connection: Connection, id: Any): Boolean =
        connection.prepareStatement("""
            SELECT w.id FROM appdata.durable_work w WHERE w.id=?
              AND w.updated_at<=CURRENT_TIMESTAMP-INTERVAL '90 days'
              AND NOT EXISTS (SELECT 1 FROM appdata.operational_event e WHERE e.work_id=w.id
                AND e.created_at>CURRENT_TIMESTAMP-INTERVAL '90 days')
        """.trimIndent()).use {
            it.setObject(1, id)
            it.executeQuery().use { rows -> rows.next() }
        }

    private fun receiptBodies(connection: Connection): Int = connection.createStatement().use {
        it.executeUpdate("""
            WITH expired AS (SELECT id FROM appdata.operation_receipt WHERE receipt_type='REQUEST'
              AND state IN ('SUCCEEDED','FAILED','EXPIRED') AND expires_at<=CURRENT_TIMESTAMP AND result IS NOT NULL
              ORDER BY expires_at,id LIMIT 100 FOR UPDATE SKIP LOCKED)
            UPDATE appdata.operation_receipt r SET result=NULL FROM expired e WHERE r.id=e.id
        """.trimIndent())
    }

    private fun deleteExpired(connection: Connection, table: String, key: String, predicate: String): Int =
        connection.createStatement().use {
            it.executeUpdate("""
                WITH expired AS (SELECT $key FROM appdata.$table WHERE $predicate
                  ORDER BY expires_at,$key LIMIT 100)
                DELETE FROM appdata.$table d USING expired e WHERE d.$key=e.$key AND ($predicate)
            """.trimIndent())
        }

    private fun clearProcessing(connection: Connection, id: Any) {
        update(connection, """
            UPDATE appdata.asset SET processing_work_id=NULL,processing_staging_key=NULL,
              processing_input_hash=NULL,processing_created_at=NULL WHERE processing_work_id=?
        """.trimIndent(), id)
    }

    private fun purgeActivity(connection: Connection): Int = connection.createStatement().use {
        it.executeUpdate("""
            WITH expired AS (SELECT e.id FROM appdata.operational_event e
              JOIN appdata.operation_receipt r ON r.id=e.operation_id
              WHERE e.work_id IS NULL AND e.event_type='ACTIVITY' AND r.receipt_type='REQUEST'
                AND r.state IN ('SUCCEEDED','FAILED','EXPIRED')
                AND e.created_at<=CURRENT_TIMESTAMP-INTERVAL '90 days'
                AND NOT EXISTS (SELECT 1 FROM appdata.durable_work w WHERE w.operation_id=r.id)
              ORDER BY e.created_at,e.id LIMIT 100)
            DELETE FROM appdata.operational_event e USING expired x WHERE e.id=x.id
        """.trimIndent())
    }

    private fun mediaResolved(): String = if (owner == "media")
        "AND NOT EXISTS (SELECT 1 FROM appdata.asset a WHERE a.processing_work_id=w.id AND a.state='PROCESSING')"
    else ""

    private fun resolved(): String = """
        w.state IN ('DONE','CANCELLED')
        AND (w.notification_state IS NULL OR w.notification_state IN ('RECORDED','ACCEPTED','DELIVERY_CONFIRMED'))
        AND (w.operation_id IS NULL OR EXISTS (SELECT 1 FROM appdata.operation_receipt r
          WHERE r.id=w.operation_id AND r.receipt_type='REQUEST' AND r.state IN ('SUCCEEDED','FAILED','EXPIRED')))
        AND NOT EXISTS (SELECT 1 FROM appdata.durable_work dependency
          WHERE dependency.operation_id=w.operation_id AND (dependency.state NOT IN ('DONE','CANCELLED')
            OR dependency.notification_state IN ('PENDING','UNKNOWN','FAILED')))
        AND NOT EXISTS (SELECT 1 FROM appdata.operational_event s WHERE s.work_id=w.id
          AND s.event_type='ATTEMPT_STARTED' AND NOT EXISTS (SELECT 1 FROM appdata.operational_event f
            WHERE f.work_id=s.work_id AND f.generation=s.generation AND f.attempt_no=s.attempt_no
              AND f.lease_epoch=s.lease_epoch AND f.event_type='ATTEMPT_FINISHED'
              AND f.attempt_outcome<>'UNCERTAIN'))
        AND NOT EXISTS (SELECT 1 FROM appdata.operational_event f WHERE f.work_id=w.id
          AND f.event_type='ATTEMPT_FINISHED' AND (f.attempt_outcome='UNCERTAIN' OR NOT EXISTS (
            SELECT 1 FROM appdata.operational_event s WHERE s.work_id=f.work_id
              AND s.event_type='ATTEMPT_STARTED' AND s.generation=f.generation
              AND s.attempt_no=f.attempt_no AND s.lease_epoch=f.lease_epoch)))
    """.trimIndent()

    private fun update(connection: Connection, sql: String, id: Any): Int =
        connection.prepareStatement(sql).use { it.setObject(1, id); it.executeUpdate() }

    private fun <T> transaction(block: (Connection) -> T): T = maintenance.connection.use {
        it.autoCommit = false
        try { val result = block(it); it.commit(); result } finally { it.rollback() }
    }

    companion object {
        fun purgeRateWindows(connection: Connection): Int = connection.createStatement().use {
            it.executeUpdate("""
                WITH expired AS (SELECT bucket_key,window_start FROM appdata.request_rate_window
                  WHERE expires_at<=CURRENT_TIMESTAMP ORDER BY expires_at,bucket_key,window_start
                  LIMIT 100)
                DELETE FROM appdata.request_rate_window w USING expired e
                  WHERE w.bucket_key=e.bucket_key AND w.window_start=e.window_start AND w.expires_at<=CURRENT_TIMESTAMP
            """.trimIndent())
        }
    }
}
