package com.spoolin.media.operations.work

import com.spoolin.media.SchemaReadiness
import com.spoolin.media.features.processing.MediaProcessingHandler
import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkClaim
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

/** Exactly two worker lanes; each lane holds at most one committed claim. */
class MediaWorkDispatcher(
    dataSource: DataSource,
    private val readiness: SchemaReadiness,
    private val handler: MediaProcessingHandler,
    private val sweepScratch: () -> Unit = {},
) : AutoCloseable {
    private val watchdog = MediaWorkWatchdog(dataSource)
    private val claimer = WorkClaimer(dataSource)
    private val guard = WorkLeaseGuard(dataSource)
    private val workers = Executors.newFixedThreadPool(2)
    private val heartbeats = Executors.newScheduledThreadPool(1)
    private val lanes = List(2) { AtomicBoolean(false) }
    private val stopped = AtomicBoolean(false)

    @Scheduled(fixedDelay = 1000)
    fun dispatch() {
        if (stopped.get() || !readiness.ready()) return
        watchdog.reconcile()
        sweepScratch()
        lanes.forEach { lane ->
            if (lane.compareAndSet(false, true)) workers.submit {
                try { claimer.claim(1).singleOrNull()?.let(::execute) } finally { lane.set(false) }
            }
        }
    }

    private fun execute(claim: WorkClaim) {
        val currentThread = Thread.currentThread()
        val renewal = heartbeats.scheduleAtFixedRate({
            if (!guard.renew(claim)) currentThread.interrupt()
        }, 10, 10, TimeUnit.SECONDS)
        try {
            when (claim.kind) {
                "MEDIA_PROCESS" -> handler.handle(claim)
                else -> guard.complete(claim, AttemptOutcome.PERMANENT, "UNSUPPORTED_WORK_KIND")
            }
        } catch (failure: com.spoolin.media.features.processing.CodecFailure) {
            guard.complete(claim, AttemptOutcome.TRANSIENT, failure.errorCode)
        } catch (_: Exception) {
            guard.complete(claim, AttemptOutcome.TRANSIENT, "MEDIA_PROCESSING_UNAVAILABLE")
        } finally {
            renewal.cancel(false)
            Thread.interrupted()
        }
    }

    override fun close() {
        stopped.set(true)
        workers.shutdown()
        if (!workers.awaitTermination(30, TimeUnit.SECONDS)) workers.shutdownNow()
        heartbeats.shutdownNow()
    }
}
