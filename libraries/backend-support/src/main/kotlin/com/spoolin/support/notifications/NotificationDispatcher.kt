package com.spoolin.support.notifications

import com.spoolin.support.work.AttemptOutcome
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

/** Two bounded lanes; no notification failure recursively produces another notification. */
class NotificationDispatcher(
    dataSource: DataSource,
    private val transport: NotificationTransport,
) : AutoCloseable {
    private val store = NotificationStore(dataSource)
    private val running = AtomicBoolean(false)
    private val executor =
        Executors.newScheduledThreadPool(2) { task ->
            Thread(task, "notification-worker").apply {
                isDaemon =
                    true
            }
        }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        repeat(2) {
            executor.scheduleWithFixedDelay({ if (running.get()) safeRun() }, 0, POLL_MS, TimeUnit.MILLISECONDS)
        }
    }

    fun runOnce(): Boolean {
        val item = store.claim() ?: return false
        val remaining = store.budgetMs(item.work)
        val result =
            if (remaining <= 0) {
                NotificationResult(AttemptOutcome.PERMANENT, "NOTIFICATION_DEADLINE")
            } else if (item.mode !=
                transport.mode
            ) {
                NotificationResult(AttemptOutcome.PERMANENT, "NOTIFICATION_MODE_CHANGED")
            } else {
                transport.send(item, remaining)
            }
        store.finish(item.work, result)
        return true
    }

    private fun safeRun() {
        try {
            runOnce()
        } catch (
            _: Exception,
        ) {
            // Durable expired leases become UNKNOWN; never log provider credentials.
        }
    }

    override fun close() {
        running.set(false)
        executor.shutdown()
        if (!executor.awaitTermination(SHUTDOWN_SECONDS, TimeUnit.SECONDS)) executor.shutdownNow()
    }
    companion object {
        private const val POLL_MS = 250L
        private const val SHUTDOWN_SECONDS = 15L
    }
}

interface NotificationTransport {
    val mode: String

    fun send(
        item: NotificationClaim,
        timeoutMs: Long,
    ): NotificationResult
}

data class NotificationResult(
    val outcome: AttemptOutcome,
    val errorCode: String? = null,
    val reference: String? = null,
    val retryAfterMs: Long = 0,
)
