package com.spoolin.catalog.operations.metrics

import com.spoolin.support.receipts.queryBound
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import javax.sql.DataSource

class ProposalMetrics(
    registry: MeterRegistry,
    dataSource: DataSource,
) {
    private val approved: Counter =
        Counter.builder("spoolin.proposal.approved").description("Trusted proposal approvals").register(registry)
    private val rejected: Counter =
        Counter.builder("spoolin.proposal.rejected").description("Trusted proposal rejections").register(registry)

    init {
        Gauge.builder("spoolin.work.pending") { reviewProposalDepth(dataSource).toDouble() }
            .description("Pending REVIEW proposals awaiting trusted approval")
            .register(registry)
    }

    fun markApproved() = approved.increment()

    fun markRejected() = rejected.increment()

    companion object {
        fun reviewProposalDepth(dataSource: DataSource): Long =
            dataSource.connection.use { connection ->
                connection
                    .queryBound(
                        "SELECT count(*) FROM movie_proposal WHERE state = 'REVIEW'",
                        emptyList(),
                    ) { it.getLong(1) }
                    .single()
            }
    }
}
