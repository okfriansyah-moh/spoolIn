package com.spoolin.integration.work.leases

import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.support.observability.OwnerMetricRuntime
import com.spoolin.support.observability.OwnerMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import javax.sql.DataSource

class OwnerMetricsBoundaryTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `real work state oldest due pool disk and notification measurements retain distinct meanings`() =
        fixture { owner ->
            owner.pool("media").use { pool ->
                seedMeasurements(pool)
                SimpleMeterRegistry().useRegistry { registry ->
                    val metrics =
                        OwnerMetrics(
                            registry,
                            pool,
                            "media",
                            OwnerMetricRuntime({ 2 }, { 8 }, { true }),
                            mapOf("artwork" to directory),
                        )
                    assertTrue(metrics.prepareScrape())
                    assertWorkMeasurements(registry)
                    assertEquals(
                        1.0,
                        registry
                            .get("spoolin.notification.events")
                            .tags("mode", "database", "state", "RECORDED")
                            .gauge()
                            .value(),
                    )
                    assertEquals(
                        1.0,
                        registry
                            .get("spoolin.notification.events")
                            .tags("mode", "smtp", "state", "UNKNOWN")
                            .gauge()
                            .value(),
                    )
                    assertEquals(2.0, registry.get("spoolin.pool.active.connections").gauge().value())
                    assertEquals(8.0, registry.get("spoolin.pool.max.connections").gauge().value())
                    assertTrue(registry.get("spoolin.disk.capacity.bytes").gauge().value() > 0)
                    assertTrue(registry.get("spoolin.disk.available.bytes").gauge().value() >= 0)
                    val labels = registry.meters.flatMap { it.id.tags }
                    assertTrue(labels.all { it.key in setOf("service", "state", "mode", "volume") })
                }
            }
        }

    @Test
    fun `database permission failure fails scrape and invalidates prior measurements`() =
        fixture { owner ->
            owner.pool("media").use { pool ->
                SimpleMeterRegistry().useRegistry { registry ->
                    val metrics = OwnerMetrics(registry, pool, "media", OwnerMetricRuntime({ 0 }, { 8 }, { true }))
                    assertTrue(metrics.prepareScrape())
                    assertEquals(
                        0.0,
                        registry
                            .get("spoolin.work.items")
                            .tag("state", "READY")
                            .gauge()
                            .value(),
                    )
                    owner.sql("media", "REVOKE SELECT ON appdata.durable_work FROM spoolin_media_runtime")
                    assertFalse(metrics.prepareScrape())
                    assertTrue(
                        registry
                            .get("spoolin.work.items")
                            .tag("state", "READY")
                            .gauge()
                            .value()
                            .isNaN(),
                    )
                    owner.grants("media")
                    assertTrue(metrics.prepareScrape())
                }
            }
        }

    @Test
    fun `unready service differs from unreachable exporter and no due work has measured zero age`() =
        fixture { owner ->
            owner.pool("media").use { pool ->
                SimpleMeterRegistry().useRegistry { registry ->
                    val metrics = OwnerMetrics(registry, pool, "media", OwnerMetricRuntime({ 1 }, { 8 }, { false }))
                    assertTrue(metrics.prepareScrape())
                    assertEquals(0.0, registry.get("spoolin.service.ready").gauge().value())
                    assertEquals(0.0, registry.get("spoolin.oldest.due.work.seconds").gauge().value())
                }
            }
        }

    private fun seedMeasurements(pool: DataSource) {
        pool.connection.use { connection ->
            connection.createStatement().executeUpdate(
                """
                INSERT INTO durable_work(id,kind,payload,state,max_attempts,deadline_at,correlation_id,
                  base_delay_ms,cap_delay_ms,backoff_multiplier,next_attempt_at)
                VALUES ('${UUID.randomUUID()}','MEDIA_PROCESS','{}','READY',3,CURRENT_TIMESTAMP,
                  '${UUID.randomUUID()}',500,5000,2,CURRENT_TIMESTAMP-INTERVAL '150 seconds')
                """.trimIndent(),
            )
            for ((mode, state) in listOf("database" to "RECORDED", "smtp" to "UNKNOWN")) {
                connection.createStatement().executeUpdate(
                    """
                    INSERT INTO durable_work(id,kind,payload,state,max_attempts,deadline_at,correlation_id,
                      base_delay_ms,cap_delay_ms,backoff_multiplier,notification_key,notification_mode,
                      notification_state,notification_summary)
                    VALUES ('${UUID.randomUUID()}','NOTIFICATION','{}',
                      '${if (mode == "database") "DONE" else "EXHAUSTED"}',
                      3,CURRENT_TIMESTAMP,'${UUID.randomUUID()}',500,5000,2,
                      '${UUID.randomUUID()}','$mode','$state','{}')
                    """.trimIndent(),
                )
            }
        }
    }

    private fun assertWorkMeasurements(registry: SimpleMeterRegistry) {
        assertEquals(
            1.0,
            registry
                .get("spoolin.work.items")
                .tag("state", "READY")
                .gauge()
                .value(),
        )
        assertEquals(
            1.0,
            registry
                .get("spoolin.work.items")
                .tag("state", "EXHAUSTED")
                .gauge()
                .value(),
        )
        assertEquals(
            0.0,
            registry
                .get("spoolin.work.items")
                .tag("state", "DONE")
                .gauge()
                .value(),
        )
        assertTrue(registry.get("spoolin.oldest.due.work.seconds").gauge().value() >= 150)
    }

    private fun fixture(block: (OwnerFixture) -> Unit) {
        OwnerFixture("owner-metrics.json").start().use { owner ->
            owner.membership("media", "grant")
            migrateOwner(owner, "media")
            owner.grants("media")
            owner.membership("media", "revoke")
            block(owner)
        }
    }
}

private inline fun <R> SimpleMeterRegistry.useRegistry(action: (SimpleMeterRegistry) -> R): R =
    try {
        action(this)
    } finally {
        close()
    }
