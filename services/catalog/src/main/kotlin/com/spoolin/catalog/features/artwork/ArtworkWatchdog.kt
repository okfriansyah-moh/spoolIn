package com.spoolin.catalog.features.artwork

import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.util.UUID
import javax.sql.DataSource

/** Reconciles interrupted sends through the same immutable downstream child receipts. */
class ArtworkWatchdog(
    private val dataSource: DataSource,
) {
    fun reconcile() =
        dataSource.committed { connection ->
            val expired =
                connection.queryBound(
                    "SELECT id FROM durable_work WHERE " +
                        "kind IN ('ARTWORK_ATTACH','ARTWORK_CONFIRM','ARTWORK_RELEASE') " +
                        "AND ((state='LEASED' AND lease_until<=clock_timestamp()) OR (state='READY' " +
                        "AND (deadline_at<=clock_timestamp() OR attempt_count>=max_attempts))) " +
                        "ORDER BY updated_at,id LIMIT 10 FOR UPDATE SKIP LOCKED",
                    emptyList(),
                ) { it.getObject("id", UUID::class.java) }
            expired.forEach { id ->
                connection.executeBound(
                    "INSERT INTO operational_event(id,operation_id,actor_kind,action,subject_kind,subject_id,outcome," +
                        "correlation_id,event_type,work_id,generation,attempt_no,lease_epoch," +
                        "attempt_outcome,error_code) " +
                        "SELECT ?,operation_id,'SERVICE','WORK_ATTEMPT','WORK',id,'FAILED',correlation_id," +
                        "'ATTEMPT_FINISHED',id,generation,attempt_count,lease_epoch,'UNCERTAIN','LEASE_EXPIRED' " +
                        "FROM durable_work WHERE id=? AND state='LEASED' " +
                        "ON CONFLICT(work_id,generation,attempt_no,event_type) " +
                        "WHERE event_type IN ('ATTEMPT_STARTED','ATTEMPT_FINISHED') DO NOTHING",
                    UUID.randomUUID(),
                    id,
                )
                connection.executeBound(
                    "UPDATE durable_work SET state=CASE WHEN deadline_at<=clock_timestamp() " +
                        "OR attempt_count>=max_attempts THEN 'EXHAUSTED' ELSE 'READY' END," +
                        "lease_owner=NULL,lease_until=NULL,next_attempt_at=clock_timestamp()," +
                        "updated_at=clock_timestamp(),last_error='LEASE_EXPIRED' WHERE id=?",
                    id,
                )
            }
        }
}
