package com.spoolin.core.operations.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.atomic.AtomicLong

/** Exposes `spoolin_work_pending` for ops/monitoring/alerts.yml (T034/T036). */
class WorkPendingMetrics(
    registry: MeterRegistry,
) {
    private val pending = AtomicLong(0)

    init {
        Gauge.builder("spoolin.work.pending") { pending.get().toDouble() }
            .description("Durable work backlog episodes")
            .register(registry)
    }

    fun markAlertEpisode() {
        pending.incrementAndGet()
    }
}
