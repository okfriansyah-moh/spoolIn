package com.spoolin.support.observability

import com.spoolin.support.fixtures.JdbcBoundary
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.SQLException

class OwnerMetricsRulesTest {
    @TempDir lateinit var root: Path
    private fun snapshot(db: JdbcBoundary): JdbcBoundary = db.rows(mapOf("1" to "READY", "2" to 5.0), mapOf("1" to "LEASED", "2" to 2.0))
        .rows(mapOf("1" to "smtp", "2" to "PENDING", "3" to 3.0)).rows(mapOf("1" to 12.5))

    @Test fun `scrape exposes one coherent owner snapshot including pool readiness and disk capacity`() {
        SimpleMeterRegistry().use { registry ->
            val db = snapshot(JdbcBoundary())
            val metrics = OwnerMetrics(registry, db.source, "media", OwnerMetricRuntime({ 2 }, { 8 }, { true }), mapOf("artwork" to root))
            assertTrue(metrics.prepareScrape())
            assertEquals(5.0, registry.get("spoolin.work.items").tag("state", "READY").gauge().value())
            assertEquals(0.0, registry.get("spoolin.work.items").tag("state", "DONE").gauge().value())
            assertEquals(3.0, registry.get("spoolin.notification.events").tags("mode", "smtp", "state", "PENDING").gauge().value())
            assertEquals(0.0, registry.get("spoolin.notification.events").tags("mode", "slack", "state", "FAILED").gauge().value())
            assertEquals(12.5, registry.get("spoolin.oldest.due.work.seconds").gauge().value())
            assertEquals(2.0, registry.get("spoolin.pool.active.connections").gauge().value())
            assertEquals(8.0, registry.get("spoolin.pool.max.connections").gauge().value())
            assertEquals(1.0, registry.get("spoolin.service.ready").gauge().value())
            assertTrue(registry.get("spoolin.disk.capacity.bytes").gauge().value() > 0)
            assertTrue(registry.get("spoolin.disk.available.bytes").gauge().value() >= 0)
            assertEquals(1, db.commits)
        }
    }

    @Test fun `a failed collection clears the prior measurement and a later successful scrape recovers`() {
        SimpleMeterRegistry().use { registry ->
            val db = snapshot(JdbcBoundary())
            val metrics = OwnerMetrics(registry, db.source, "core", OwnerMetricRuntime({ 0 }, { 4 }, { false }))
            assertTrue(metrics.prepareScrape())
            assertEquals(0.0, registry.get("spoolin.service.ready").gauge().value())
            db.failure = SQLException("connection failed")
            assertFalse(metrics.prepareScrape())
            assertTrue(registry.get("spoolin.service.ready").gauge().value().isNaN())
            db.failure = null
            snapshot(db)
            assertTrue(metrics.prepareScrape())
            assertEquals(0.0, registry.get("spoolin.service.ready").gauge().value())
        }
    }

    @Test fun `invalid pool measurements and unavailable volumes fail the scrape`() {
        for ((active, maximum) in listOf(-1 to 5, 6 to 5, 0 to 0)) {
            SimpleMeterRegistry().use { registry ->
                val metrics = OwnerMetrics(registry, snapshot(JdbcBoundary()).source, "catalog", OwnerMetricRuntime({ active }, { maximum }, { true }))
                assertFalse(metrics.prepareScrape())
            }
        }
        SimpleMeterRegistry().use { registry ->
            assertFalse(OwnerMetrics(registry, snapshot(JdbcBoundary()).source, "core", OwnerMetricRuntime({ 0 }, { 5 }, { true }),
                mapOf("backup" to root.resolve("missing"))).prepareScrape())
        }
    }

    @Test fun `metric dimensions accept only maintained owner and volume labels`() {
        val runtime = OwnerMetricRuntime({ 0 }, { 1 }, { true })
        SimpleMeterRegistry().use { registry ->
            assertThrows(IllegalArgumentException::class.java) { OwnerMetrics(registry, JdbcBoundary().source, "customer-input", runtime) }
            assertThrows(IllegalArgumentException::class.java) { OwnerMetrics(registry, JdbcBoundary().source, "core", runtime, mapOf("arbitrary" to root)) }
        }
    }
}
