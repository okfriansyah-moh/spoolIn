package com.spoolin.media.features.processing

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.TimeUnit

/** Codec attempts are timed; a successful owner result is counted only after its fenced commit. */
class ProcessingMetrics(private val registry: MeterRegistry) {
    fun measure(action: () -> ProcessorRunner.Outcome): ProcessorRunner.Outcome {
        val started = System.nanoTime()
        var returned = false
        return try {
            action().also { returned = true }
        } finally {
            Timer.builder("spoolin.media.processing.duration")
                .tag("service", "media")
                .serviceLevelObjectives(*BUCKETS)
                .register(registry)
                .record((System.nanoTime() - started).coerceAtLeast(0), TimeUnit.NANOSECONDS)
            if (!returned) count("FAILED")
        }
    }

    fun committed(outcome: ProcessorRunner.Outcome) {
        count(if (outcome is ProcessorRunner.Outcome.Success) "SUCCESS" else "FAILED")
    }

    private fun count(outcome: String) {
        Counter.builder("spoolin.media.processing")
            .tags("service", "media", "outcome", outcome)
            .register(registry)
            .increment()
    }

    companion object {
        private const val FAST_MS = 10L
        private const val SHORT_MS = 50L
        private const val TENTH_MS = 100L
        private const val RESPONSE_MS = 300L
        private const val HALF_SECOND_MS = 500L
        private const val SECOND_MS = 1000L
        private const val SLOW_MS = 3000L
        private const val EFFECT_MS = 5000L
        private const val RECOVERY_MS = 15000L
        private const val LONG_MS = 30000L
        private val BUCKETS = longArrayOf(
            FAST_MS, SHORT_MS, TENTH_MS, RESPONSE_MS, HALF_SECOND_MS,
            SECOND_MS, SLOW_MS, EFFECT_MS, RECOVERY_MS, LONG_MS,
        ).map(Duration::ofMillis).toTypedArray()
    }
}
