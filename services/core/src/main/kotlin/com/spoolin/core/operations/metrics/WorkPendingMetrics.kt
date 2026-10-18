package com.spoolin.core.operations.metrics

import com.spoolin.support.receipts.queryBound
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import javax.sql.DataSource

/** Exposes `spoolin_work_pending` for ops/monitoring/alerts.yml (T034/T036). */
class WorkPendingMetrics(
    registry: MeterRegistry,
    dataSource: DataSource,
) {
    private val alertEpisodes: Counter =
        Counter.builder("spoolin.alert.episodes")
            .description("Recorded alert episodes")
            .register(registry)

    init {
        Gauge.builder("spoolin.work.pending") { pendingDepth(dataSource).toDouble() }
            .description("Durable work backlog depth")
            .register(registry)
    }

    fun markAlertEpisode() {
        alertEpisodes.increment()
    }

    companion object {
        fun pendingDepth(dataSource: DataSource): Long {
            dataSource.connection.use { connection ->
                val work =
                    connection
                        .queryBound(
                            "SELECT count(*) FROM durable_work WHERE state IN ('READY', 'LEASED')",
                            emptyList(),
                        ) { it.getLong(1) }
                        .single()
                val receipts =
                    connection
                        .queryBound(
                            "SELECT count(*) FROM operation_receipt " +
                                "WHERE receipt_type = 'REQUEST' AND state = 'PENDING'",
                            emptyList(),
                        ) { it.getLong(1) }
                        .single()
                return work + receipts
            }
        }
    }
}
