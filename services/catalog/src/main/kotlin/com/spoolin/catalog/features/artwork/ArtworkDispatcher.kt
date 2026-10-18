package com.spoolin.catalog.features.artwork

import com.spoolin.catalog.SchemaReadiness
import com.spoolin.catalog.operations.work.WorkLeaseGuard
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.WorkClaim
import com.spoolin.support.work.WorkSql
import org.springframework.scheduling.annotation.Scheduled
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

class ArtworkDispatcher(private val dataSource: DataSource, private val readiness: SchemaReadiness, private val handler: ArtworkWorkHandler) : AutoCloseable {
    private val watchdog = ArtworkWatchdog(dataSource)
    private val workers = Executors.newFixedThreadPool(2)
    private val heartbeat = Executors.newScheduledThreadPool(1)
    private val guard = WorkLeaseGuard(dataSource)
    private val lanes = List(2) { AtomicBoolean(false) }
    private val stopped = AtomicBoolean(false)

    @Scheduled(fixedDelay = 1000)
    fun dispatch() {
        if (stopped.get() || !readiness.ready()) return
        watchdog.reconcile()
        lanes.forEach { lane -> if (lane.compareAndSet(false, true)) workers.submit {
            try { claim()?.let(::execute) } finally { lane.set(false) }
        } }
    }

    private fun claim(): WorkClaim? = dataSource.committed { connection ->
        val rows = connection.queryBound("SELECT * FROM durable_work WHERE kind IN ('ARTWORK_ATTACH','ARTWORK_CONFIRM','ARTWORK_RELEASE') " +
            "AND state='READY' AND next_attempt_at<=clock_timestamp() AND deadline_at>clock_timestamp() AND attempt_count<max_attempts " +
            "ORDER BY next_attempt_at,id LIMIT 1 FOR UPDATE SKIP LOCKED", emptyList()) { row ->
            WorkClaim(row.getObject("id", UUID::class.java), row.getObject("operation_id", UUID::class.java), row.getString("kind"),
                row.getString("payload"), row.getInt("attempt_count"), row.getInt("max_attempts"), row.getTimestamp("deadline_at").toInstant(),
                UUID.randomUUID(), row.getLong("lease_epoch"), row.getObject("correlation_id", UUID::class.java), row.getInt("base_delay_ms"),
                row.getInt("cap_delay_ms"), row.getInt("backoff_multiplier"), row.getInt("generation"))
        }
        rows.singleOrNull()?.let { WorkSql.commitClaim(connection, it, UUID.randomUUID(), Instant.now()) }
    }

    private fun execute(claim: WorkClaim) {
        val thread = Thread.currentThread()
        val renewal = heartbeat.scheduleAtFixedRate({ if (!guard.renew(claim)) thread.interrupt() }, 10, 10, TimeUnit.SECONDS)
        try { handler.handle(claim) }
        catch (_: Exception) { guard.complete(claim, com.spoolin.support.work.AttemptOutcome.TRANSIENT, "ARTWORK_WORK_UNAVAILABLE") }
        finally { renewal.cancel(false); Thread.interrupted() }
    }

    override fun close() {
        stopped.set(true)
        workers.shutdown()
        if (!workers.awaitTermination(30, TimeUnit.SECONDS)) workers.shutdownNow()
        heartbeat.shutdownNow()
    }
}
