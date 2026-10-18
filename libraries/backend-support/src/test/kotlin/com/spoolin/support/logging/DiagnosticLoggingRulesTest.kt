package com.spoolin.support.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.LoggingEvent
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DiagnosticLoggingRulesTest {
    @Test
    fun `configured appender emits one bounded JSON line without raw message args or MDC`() {
        val primary = ByteArrayOutputStream()
        val alternate = ByteArrayOutputStream()
        val appender = BoundedJsonAppender { primary to alternate }
        appender.serviceName = "core"
        appender.serviceEnvironment = "test"
        appender.start()
        val event = LoggingEvent()
        event.level = Level.WARN
        event.timeStamp = 1
        event.loggerName = "org.framework.PrivateLogger"
        event.message = "secret password={}\nmalicious-line"
        event.argumentArray = arrayOf("private-credential")
        event.mdcPropertyMap = mapOf("token" to "private-token")
        appender.doAppend(event)
        appender.stop()
        val raw = primary.toString(Charsets.UTF_8)
        val json = JsonMapper.builder().build().readTree(raw)
        assertEquals(1, raw.count { it == '\n' })
        assertEquals("warning", json["level"].asText())
        assertTrue(json["timestamp"].asText().endsWith("Z"))
        assertEquals("core", json["resource"]["service.name"].asText())
        assertEquals("test", json["resource"]["service.env"].asText())
        assertFalse(raw.contains("secret") || raw.contains("private-") || raw.contains("malicious"))
        assertTrue(primary.size() <= DIAGNOSTIC_BYTES)
        assertEquals(0, alternate.size())
    }

    @Test
    fun `real full queue loses one whole record with bounded labels and no fabricated initial series`() {
        val registry = SimpleMeterRegistry()
        val metrics = DiagnosticDropMetrics()
        metrics.bind("media", registry)
        assertTrue(registry.meters.isEmpty())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val blocked =
            object : OutputStream() {
                override fun write(value: Int) {
                    entered.countDown()
                    check(release.await(2, TimeUnit.SECONDS))
                }
            }
        val sink = BoundedDiagnosticSink("media", blocked, ByteArrayOutputStream(), metrics, capacity = 1)
        try {
            val bytes = DiagnosticEnvelope("media", "test-version", "test").invalid(1)
            sink.submit(bytes, DiagnosticLevel.WARNING)
            assertTrue(entered.await(1, TimeUnit.SECONDS))
            sink.submit(bytes, DiagnosticLevel.WARNING)
            sink.submit(bytes, DiagnosticLevel.WARNING)
            assertEquals(
                1.0,
                registry
                    .get("spoolin.log.dropped")
                    .tag("reason", "queue_full")
                    .functionCounter()
                    .count(),
            )
            assertTrue(registry.meters.flatMap { it.id.tags }.all { it.key in setOf("service", "level", "reason") })
        } finally {
            release.countDown()
            sink.close()
        }
    }

    @Test
    fun `alternate stream preserves encoded line and only total sink loss counts dropped`() {
        val registry = SimpleMeterRegistry()
        val metrics = DiagnosticDropMetrics()
        metrics.bind("catalog", registry)
        val failed =
            object : OutputStream() {
                override fun write(value: Int): Unit = throw IOException("private-sink-error")
            }
        val alternate = ByteArrayOutputStream()
        val bytes = DiagnosticEnvelope("catalog", "test-version", "test").invalid(1)
        BoundedDiagnosticSink("catalog", failed, alternate, metrics).use { it.submit(bytes, DiagnosticLevel.ERROR) }
        assertEquals(bytes.toList(), alternate.toByteArray().toList())
        assertTrue(registry.meters.isEmpty())
        BoundedDiagnosticSink("catalog", failed, failed, metrics).use { it.submit(bytes, DiagnosticLevel.ERROR) }
        assertEquals(
            1.0,
            registry
                .get("spoolin.log.dropped")
                .tag("reason", "sink_failed")
                .functionCounter()
                .count(),
        )
    }

    @Test
    fun `invalid Logback level emits only fixed contract error and increments actual loss`() {
        val output = ByteArrayOutputStream()
        val registry = SimpleMeterRegistry()
        DiagnosticDropMetrics.shared.bind("core", registry)
        val appender = BoundedJsonAppender { output to output }
        appender.serviceName = "core"
        appender.start()
        val event = LoggingEvent()
        event.level = Level.ALL
        event.message = "private-invalid-payload"
        event.timeStamp = 1
        appender.doAppend(event)
        appender.stop()
        val raw = output.toString(Charsets.UTF_8)
        assertTrue(raw.contains("Logger contract invalid"))
        assertFalse(raw.contains("private-invalid-payload"))
        val counter = registry.get("spoolin.log.dropped").tag("reason", "contract_invalid").functionCounter()
        assertTrue(counter.count() >= 1.0)
    }

    @Test
    fun `oversized serialized record is discarded intact and boot loss survives registry binding`() {
        val metrics = DiagnosticDropMetrics()
        val output = ByteArrayOutputStream()
        BoundedDiagnosticSink("core", output, output, metrics).use {
            it.submit(ByteArray(DIAGNOSTIC_BYTES + 1), DiagnosticLevel.INFO)
        }
        val registry = SimpleMeterRegistry()
        metrics.bind("core", registry)
        val dropped = registry.get("spoolin.log.dropped").tag("reason", "contract_invalid").functionCounter()
        assertEquals(1.0, dropped.count())
        assertEquals(0, output.size())
    }
}
