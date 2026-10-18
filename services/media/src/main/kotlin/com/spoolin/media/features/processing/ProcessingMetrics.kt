package com.spoolin.media.features.processing

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.TimeUnit

/** Codec attempts are timed; a successful owner result is counted only after its fenced commit. */
class ProcessingMetrics(
    private val registry: MeterRegistry,
) {
    fun measure(action: () -> ProcessorRunner.Outcome): ProcessorRunner.Outcome {
        val started = System.nanoTime()
        var returned = false
        return try {
            action().also { returned = true }
        } finally {
            Timer
                .builder("spoolin.media.processing.duration")
                .tag("service", "media")
                .serviceLevelObjectives(
                    FAST_BUCKET,
                    SHORT_BUCKET,
                    TENTH_BUCKET,
                    RESPONSE_BUCKET,
                    HALF_SECOND_BUCKET,
                    SECOND_BUCKET,
                    SLOW_BUCKET,
                    EFFECT_BUCKET,
                    RECOVERY_BUCKET,
                    LONG_BUCKET,
                ).register(registry)
                .record((System.nanoTime() - started).coerceAtLeast(0), TimeUnit.NANOSECONDS)
            if (!returned) count("FAILED")
        }
    }

    fun committed(outcome: ProcessorRunner.Outcome) {
        count(if (outcome is ProcessorRunner.Outcome.Success) "SUCCESS" else "FAILED")
    }

    private fun count(outcome: String) {
        Counter
            .builder("spoolin.media.processing")
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
        private val FAST_BUCKET = Duration.ofMillis(FAST_MS)
        private val SHORT_BUCKET = Duration.ofMillis(SHORT_MS)
        private val TENTH_BUCKET = Duration.ofMillis(TENTH_MS)
        private val RESPONSE_BUCKET = Duration.ofMillis(RESPONSE_MS)
        private val HALF_SECOND_BUCKET = Duration.ofMillis(HALF_SECOND_MS)
        private val SECOND_BUCKET = Duration.ofMillis(SECOND_MS)
        private val SLOW_BUCKET = Duration.ofMillis(SLOW_MS)
        private val EFFECT_BUCKET = Duration.ofMillis(EFFECT_MS)
        private val RECOVERY_BUCKET = Duration.ofMillis(RECOVERY_MS)
        private val LONG_BUCKET = Duration.ofMillis(LONG_MS)
    }
}
