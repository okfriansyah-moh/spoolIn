package com.spoolin.catalog.operations.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry

class ProposalMetrics(registry: MeterRegistry) {
    private val approved: Counter =
        Counter.builder("spoolin.proposal.approved").description("Trusted proposal approvals").register(registry)
    private val rejected: Counter =
        Counter.builder("spoolin.proposal.rejected").description("Trusted proposal rejections").register(registry)
    private val alertEpisodes: Counter =
        Counter.builder("spoolin.alert.episodes").description("Recorded alert episodes").register(registry)

    fun markApproved() = approved.increment()

    fun markRejected() = rejected.increment()

    fun markAlertEpisode() = alertEpisodes.increment()
}
