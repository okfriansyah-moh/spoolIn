package com.spoolin.core.operations.metrics

import com.spoolin.core.features.session.SessionJdbcFixture
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorkPendingMetricsBehaviorTest {
    @Test
    fun `pending metric combines durable work and pending request receipts without caching stale depth`() {
        val db = SessionJdbcFixture(listOf(listOf(mapOf("1" to 4L)), listOf(mapOf("1" to 3L))))
        assertEquals(7L, WorkPendingMetrics.pendingDepth(db))
        val registry = SimpleMeterRegistry()
        try {
            val metrics = WorkPendingMetrics(registry, db)
            metrics.markAlertEpisode()
            metrics.markAlertEpisode()
            assertEquals(2.0, registry.get("spoolin.alert.episodes").counter().count())
            db.enqueue(mapOf("1" to 1L))
            db.enqueue(mapOf("1" to 2L))
            assertEquals(3.0, registry.get("spoolin.work.pending").gauge().value())
            db.enqueue(mapOf("1" to 0L))
            db.enqueue(mapOf("1" to 0L))
            assertEquals(0.0, registry.get("spoolin.work.pending").gauge().value())
        } finally {
            registry.close()
        }
    }
}
