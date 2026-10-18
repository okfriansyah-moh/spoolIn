package com.spoolin.media.operations.work

import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Timestamp
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

/** Replays only deterministic local processing: accepted input and immutable output remain intact. */
class MediaWorkWatchdog(private val dataSource: DataSource, private val clock: Clock = Clock.systemUTC()) {
    fun reconcile() = dataSource.committed { connection ->
        val now = Timestamp.from(clock.instant())
        val expired = connection.queryBound(
            "SELECT id FROM durable_work WHERE kind='MEDIA_PROCESS' AND " +
                "((state='LEASED' AND lease_until<=?) OR " +
                "(state='READY' AND (deadline_at<=? OR attempt_count>=max_attempts))) " +
                "ORDER BY updated_at,id LIMIT 10 FOR UPDATE SKIP LOCKED",
            listOf(now, now),
        ) { it.getObject("id", UUID::class.java) }
        expired.forEach { id ->
            connection.executeBound(
                "INSERT INTO operational_event " +
                    "(id,operation_id,actor_kind,action,subject_kind,subject_id,outcome,correlation_id," +
                    "event_type,work_id,generation,attempt_no,lease_epoch,attempt_outcome,error_code) " +
                    "SELECT ?,operation_id,'SERVICE','WORK_ATTEMPT','WORK',id,'FAILED',correlation_id," +
                    "'ATTEMPT_FINISHED',id,generation,attempt_count,lease_epoch,'UNCERTAIN','LEASE_EXPIRED' " +
                    "FROM durable_work WHERE id=? AND state='LEASED' " +
                    "ON CONFLICT (work_id,generation,attempt_no,event_type) " +
                    "WHERE event_type IN ('ATTEMPT_STARTED','ATTEMPT_FINISHED') DO NOTHING",
                UUID.randomUUID(), id,
            )
            connection.executeBound(
                "UPDATE durable_work SET state=CASE WHEN attempt_count>=max_attempts OR deadline_at<=? " +
                    "THEN 'EXHAUSTED' ELSE 'READY' END,lease_owner=NULL,lease_until=NULL," +
                    "next_attempt_at=?,updated_at=?,last_error='LEASE_EXPIRED' WHERE id=?",
                now, now, now, id,
            )
        }
    }
}
