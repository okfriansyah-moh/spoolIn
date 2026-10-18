package com.spoolin.media.features.processing

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ProcessingMetricsRulesTest {
    @Test
    fun `no measurements create no synthetic processing series`() {
        val registry = SimpleMeterRegistry()
        ProcessingMetrics(registry)
        assertTrue(registry.meters.isEmpty())
    }

    @Test
    fun `codec result is timed but success counter waits for owner commit`() {
        val registry = SimpleMeterRegistry()
        val metrics = ProcessingMetrics(registry)
        val result = metrics.measure { success() }
        assertEquals(null, registry.find("spoolin.media.processing").counter())
        assertEquals(1L, registry.get("spoolin.media.processing.duration").timer().count())
        metrics.committed(result)
        assertEquals(
            1.0,
            registry
                .get("spoolin.media.processing")
                .tag("outcome", "SUCCESS")
                .counter()
                .count(),
        )
        assertEquals(null, registry.find("spoolin.media.processing").tag("outcome", "FAILED").counter())
        val buckets =
            registry
                .get("spoolin.media.processing.duration")
                .timer()
                .takeSnapshot()
                .histogramCounts()
        assertEquals(
            listOf(0.01, 0.05, 0.1, 0.3, 0.5, 1.0, 3.0, 5.0, 15.0, 30.0),
            buckets.map { it.bucket(java.util.concurrent.TimeUnit.SECONDS) },
        )
        assertFixedLabels(registry)
        assertFalse(registry.meters.any { it.id.tags.any { tag -> tag.value.contains("private") } })
    }

    private fun assertFixedLabels(registry: SimpleMeterRegistry) {
        val limits = setOf(0.01, 0.05, 0.1, 0.3, 0.5, 1.0, 3.0, 5.0, 15.0, 30.0)
        registry.meters.forEach { meter ->
            val expected =
                when (meter.id.name) {
                    "spoolin.media.processing" -> setOf("service", "outcome")
                    "spoolin.media.processing.duration" -> setOf("service")
                    "spoolin.media.processing.duration.histogram" -> setOf("service", "le")
                    else -> error("Unexpected processing meter")
                }
            assertEquals(
                expected,
                meter.id.tags
                    .map { it.key }
                    .toSet(),
            )
            if ("le" in expected) {
                val boundary = requireNotNull(meter.id.getTag("le")).toDouble()
                assertTrue(boundary in limits)
            }
        }
    }

    @Test
    fun `failed codec result counts failed only after commit with no error label`() {
        val registry = SimpleMeterRegistry()
        val metrics = ProcessingMetrics(registry)
        val outcome = metrics.measure { ProcessorRunner.Outcome.Rejected("private-input-path") }
        assertEquals(null, registry.find("spoolin.media.processing").counter())
        metrics.committed(outcome)
        assertEquals(
            1.0,
            registry
                .get("spoolin.media.processing")
                .tag("outcome", "FAILED")
                .counter()
                .count(),
        )
        assertTrue(registry.meters.flatMap { it.id.tags }.none { it.value.contains("private") })
    }

    @Test
    fun `aborted codec exception is timed and counted failed before dispatcher retry`() {
        val registry = SimpleMeterRegistry()
        val metrics = ProcessingMetrics(registry)
        assertThrows(CodecFailure::class.java) { metrics.measure { throw CodecFailure("PROCESSING_UNAVAILABLE") } }
        assertEquals(1L, registry.get("spoolin.media.processing.duration").timer().count())
        assertEquals(
            1.0,
            registry
                .get("spoolin.media.processing")
                .tag("outcome", "FAILED")
                .counter()
                .count(),
        )
    }

    private fun success(): ProcessorRunner.Outcome.Success =
        ProcessorRunner.Outcome.Success(
            Path.of("/private/output.png"),
            ImageFormat.PNG,
            1,
            1,
            1,
            ByteArray(32),
        )
}
