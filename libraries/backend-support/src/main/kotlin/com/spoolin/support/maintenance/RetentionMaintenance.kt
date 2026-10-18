package com.spoolin.support.maintenance

import java.sql.Connection
import javax.sql.DataSource

/** Each call advances fixed-size ordered batches; no payload or identity is logged. */
class RetentionMaintenance(
    private val maintenance: DataSource,
    private val owner: String,
) {
    init {
        require(owner in setOf("core", "catalog", "media"))
    }

    fun hourly(): Map<String, Int> =
        transaction { connection ->
            val counts = linkedMapOf("receiptBodies" to receiptBodies(connection))
            if (owner == "core") {
                counts["sessions"] =
                    deleteExpired(
                        connection,
                        "browser_session",
                        "id",
                        "LEAST(expires_at,last_seen_at+INTERVAL '30 minutes',COALESCE(revoked_at,expires_at))" +
                            "<=CURRENT_TIMESTAMP-INTERVAL '7 days'",
                    )
                counts["credentials"] =
                    deleteExpired(
                        connection,
                        "mcp_credential",
                        "id",
                        "expires_at<=CURRENT_TIMESTAMP OR revoked_at<=CURRENT_TIMESTAMP",
                    )
                counts["drafts"] = deleteExpired(connection, "form_draft", "id", "expires_at<=CURRENT_TIMESTAMP")
                counts["rateWindows"] = purgeRateWindows(connection)
            }
            counts
        }

    fun daily(): Map<String, Int> {
        val work = WorkRetention(owner)
        val ids = maintenance.connection.use(work::candidates)
        val counts = linkedMapOf("payloads" to 0, "attemptEvents" to 0, "work" to 0)
        for (id in ids) {
            val result = transaction { work.purge(it, id) }
            for ((key, value) in result) counts[key] = counts.getValue(key) + value
        }
        counts["activityEvents"] = transaction { purgeActivity(it) }
        return counts
    }

    private fun receiptBodies(connection: Connection): Int =
        connection.createStatement().use {
            it.executeUpdate(
                """
                WITH expired AS (SELECT id FROM appdata.operation_receipt WHERE receipt_type='REQUEST'
                  AND state IN ('SUCCEEDED','FAILED','EXPIRED') AND expires_at<=CURRENT_TIMESTAMP AND result IS NOT NULL
                  ORDER BY expires_at,id LIMIT 100 FOR UPDATE SKIP LOCKED)
                UPDATE appdata.operation_receipt r SET result=NULL FROM expired e WHERE r.id=e.id
                """.trimIndent(),
            )
        }

    private fun deleteExpired(
        connection: Connection,
        table: String,
        key: String,
        predicate: String,
    ): Int =
        connection.createStatement().use {
            it.executeUpdate(
                """
                WITH expired AS (SELECT $key FROM appdata.$table WHERE $predicate
                  ORDER BY expires_at,$key LIMIT 100)
                DELETE FROM appdata.$table d USING expired e WHERE d.$key=e.$key AND ($predicate)
                """.trimIndent(),
            )
        }

    private fun purgeActivity(connection: Connection): Int =
        connection.createStatement().use {
            it.executeUpdate(
                """
                WITH expired AS (SELECT e.id FROM appdata.operational_event e
                  JOIN appdata.operation_receipt r ON r.id=e.operation_id
                  WHERE e.work_id IS NULL AND e.event_type='ACTIVITY' AND r.receipt_type='REQUEST'
                    AND r.state IN ('SUCCEEDED','FAILED','EXPIRED')
                    AND e.created_at<=CURRENT_TIMESTAMP-INTERVAL '90 days'
                    AND NOT EXISTS (SELECT 1 FROM appdata.durable_work w WHERE w.operation_id=r.id)
                  ORDER BY e.created_at,e.id LIMIT 100)
                DELETE FROM appdata.operational_event e USING expired x WHERE e.id=x.id
                """.trimIndent(),
            )
        }

    private fun <T> transaction(block: (Connection) -> T): T =
        maintenance.connection.use {
            it.autoCommit = false
            try {
                val result = block(it)
                it.commit()
                result
            } finally {
                it.rollback()
            }
        }

    companion object {
        fun purgeRateWindows(connection: Connection): Int =
            connection.createStatement().use {
                it.executeUpdate(
                    """
                    WITH expired AS (SELECT bucket_key,window_start FROM appdata.request_rate_window
                      WHERE expires_at<=CURRENT_TIMESTAMP ORDER BY expires_at,bucket_key,window_start
                      LIMIT 100)
                    DELETE FROM appdata.request_rate_window w USING expired e
                      WHERE w.bucket_key=e.bucket_key AND w.window_start=e.window_start
                        AND w.expires_at<=CURRENT_TIMESTAMP
                    """.trimIndent(),
                )
            }
    }
}
