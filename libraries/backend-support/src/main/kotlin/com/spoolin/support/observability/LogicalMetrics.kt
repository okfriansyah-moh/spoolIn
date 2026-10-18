package com.spoolin.support.observability

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.TimeUnit

private const val FAST_BUCKET_MS = 10L
private const val SHORT_BUCKET_MS = 50L
private const val TENTH_SECOND_MS = 100L
private const val RESPONSE_BUCKET_MS = 300L
private const val HALF_SECOND_MS = 500L
private const val SLOW_BUCKET_SECONDS = 3L
private const val EFFECT_BUCKET_SECONDS = 5L
private const val RECOVERY_BUCKET_SECONDS = 15L
private const val LONG_BUCKET_SECONDS = 30L

private val FAST_BUCKET = Duration.ofMillis(FAST_BUCKET_MS)
private val SHORT_BUCKET = Duration.ofMillis(SHORT_BUCKET_MS)
private val TENTH_SECOND_BUCKET = Duration.ofMillis(TENTH_SECOND_MS)
private val RESPONSE_BUCKET = Duration.ofMillis(RESPONSE_BUCKET_MS)
private val HALF_SECOND_BUCKET = Duration.ofMillis(HALF_SECOND_MS)
private val SECOND_BUCKET = Duration.ofSeconds(1)
private val SLOW_BUCKET = Duration.ofSeconds(SLOW_BUCKET_SECONDS)
private val EFFECT_BUCKET = Duration.ofSeconds(EFFECT_BUCKET_SECONDS)
private val RECOVERY_BUCKET = Duration.ofSeconds(RECOVERY_BUCKET_SECONDS)
private val LONG_BUCKET = Duration.ofSeconds(LONG_BUCKET_SECONDS)

/** Only real completion events create series; no label values come from requests, errors or payload identifiers. */
class LogicalMetrics(
    private val registry: MeterRegistry,
    internal val service: String,
) {
    init {
        require(service in setOf("core", "catalog", "media"))
    }

    fun complete(
        operation: String,
        boundary: String,
        result: LogicalResult,
        startedNanos: Long,
    ): Boolean {
        val selected = MetricOperations.named(service, operation, boundary) ?: return false
        record(selected, result, startedNanos)
        return true
    }

    internal fun record(
        operation: MetricOperation,
        result: LogicalResult,
        startedNanos: Long,
    ) {
        Timer
            .builder("spoolin.operation.duration")
            .tags("service", service, "operation", operation.name, "boundary", operation.boundary)
            .serviceLevelObjectives(
                FAST_BUCKET,
                SHORT_BUCKET,
                TENTH_SECOND_BUCKET,
                RESPONSE_BUCKET,
                HALF_SECOND_BUCKET,
                SECOND_BUCKET,
                SLOW_BUCKET,
                EFFECT_BUCKET,
                RECOVERY_BUCKET,
                LONG_BUCKET,
            )
            .register(registry)
            .record((System.nanoTime() - startedNanos).coerceAtLeast(0), TimeUnit.NANOSECONDS)
        Counter
            .builder("spoolin.operation.results")
            .tags(
                "service",
                service,
                "operation",
                operation.name,
                "boundary",
                operation.boundary,
                "outcome",
                result.outcome.name,
                "code",
                result.code.name,
            ).register(registry)
            .increment()
    }

}
