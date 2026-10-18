package com.spoolin.support.maintenance

import java.sql.Connection
import java.sql.ResultSet

/** Preserve media asset-before-work lock ordering and recheck resolved eligibility in the caller transaction. */
internal class WorkRetention(
    private val owner: String,
) {
    fun candidates(connection: Connection): List<Any> =
        connection.createStatement().use { statement ->
            statement
                .executeQuery(
                    """
                    SELECT w.id FROM appdata.durable_work w WHERE ${resolved()} ${mediaResolved()}
                      AND ((w.kind<>'NOTIFICATION' AND w.payload<>'{}'::jsonb
                        AND w.updated_at<=CURRENT_TIMESTAMP-INTERVAL '7 days')
                        OR w.updated_at<=CURRENT_TIMESTAMP-INTERVAL '90 days')
                    ORDER BY w.updated_at,w.id LIMIT 100
                    """.trimIndent(),
                ).use(::readIds)
        }

    fun purge(
        connection: Connection,
        id: Any,
    ): Map<String, Int> {
        val checkpoint = connection.setSavepoint()
        val assets = assets(connection, id, true)
        val locked =
            connection.prepareStatement("SELECT id FROM appdata.durable_work WHERE id=? FOR UPDATE").use {
                it.setObject(1, id)
                it.executeQuery().use { rows -> rows.next() }
            }
        val resolved = locked && eligible(connection, id)
        val stableAssets = resolved && assets == assets(connection, id, false)
        if (!stableAssets) {
            connection.rollback(checkpoint)
            return emptyMap()
        }
        val payloads =
            update(
                connection,
                """
                UPDATE appdata.durable_work SET payload='{}'::jsonb WHERE id=? AND kind<>'NOTIFICATION'
                  AND payload<>'{}'::jsonb AND updated_at<=CURRENT_TIMESTAMP-INTERVAL '7 days'
                """.trimIndent(),
                id,
            )
        return if (expiredHistory(connection, id)) {
            if (owner == "media") clearProcessing(connection, id)
            val events = update(connection, "DELETE FROM appdata.operational_event WHERE work_id=?", id)
            val work = update(connection, "DELETE FROM appdata.durable_work WHERE id=?", id)
            connection.releaseSavepoint(checkpoint)
            mapOf("payloads" to payloads, "attemptEvents" to events, "work" to work)
        } else {
            mapOf("payloads" to payloads)
        }
    }

    private fun assets(
        connection: Connection,
        id: Any,
        lock: Boolean,
    ): List<Any> {
        if (owner != "media") return emptyList()
        return connection
            .prepareStatement(
                "SELECT id FROM appdata.asset WHERE processing_work_id=? ORDER BY id" +
                    if (lock) " FOR UPDATE" else "",
            ).use {
                it.setObject(1, id)
                it.executeQuery().use(::readIds)
            }
    }

    private fun eligible(
        connection: Connection,
        id: Any,
    ): Boolean =
        connection
            .prepareStatement(
                "SELECT w.id FROM appdata.durable_work w WHERE w.id=? AND ${resolved()} ${mediaResolved()}",
            ).use {
                it.setObject(1, id)
                it.executeQuery().use { rows -> rows.next() }
            }

    private fun expiredHistory(
        connection: Connection,
        id: Any,
    ): Boolean =
        connection
            .prepareStatement(
                """
                SELECT w.id FROM appdata.durable_work w WHERE w.id=?
                  AND w.updated_at<=CURRENT_TIMESTAMP-INTERVAL '90 days'
                  AND NOT EXISTS (SELECT 1 FROM appdata.operational_event e WHERE e.work_id=w.id
                    AND e.created_at>CURRENT_TIMESTAMP-INTERVAL '90 days')
                """.trimIndent(),
            ).use {
                it.setObject(1, id)
                it.executeQuery().use { rows -> rows.next() }
            }

    private fun clearProcessing(
        connection: Connection,
        id: Any,
    ) {
        update(
            connection,
            """
            UPDATE appdata.asset SET processing_work_id=NULL,processing_staging_key=NULL,
              processing_input_hash=NULL,processing_created_at=NULL WHERE processing_work_id=?
            """.trimIndent(),
            id,
        )
    }

    private fun mediaResolved(): String =
        if (owner == "media") {
            "AND NOT EXISTS (SELECT 1 FROM appdata.asset a WHERE a.processing_work_id=w.id AND a.state='PROCESSING')"
        } else {
            ""
        }

    private fun resolved(): String =
        """
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

    private fun update(
        connection: Connection,
        sql: String,
        id: Any,
    ): Int =
        connection.prepareStatement(sql).use {
            it.setObject(1, id)
            it.executeUpdate()
        }
}

private fun readIds(rows: ResultSet): List<Any> = buildList { while (rows.next()) add(rows.getObject(1)) }
