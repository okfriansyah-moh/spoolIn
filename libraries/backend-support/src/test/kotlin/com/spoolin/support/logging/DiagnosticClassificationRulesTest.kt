package com.spoolin.support.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.reflect.Proxy
import java.sql.SQLException

class DiagnosticClassificationRulesTest {
    @Test
    fun `actual startup failure retains safe deepest SQL type and code without secret messages or arguments`() {
        val failure =
            IllegalStateException(
                "private bean and configuration",
                SQLException("password=secret jdbc=private", "28P01"),
            )
        val event = startup()
        event.setThrowableProxy(ThrowableProxy(failure))
        val raw = append(event)
        val node = JsonMapper.builder().build().readTree(raw)
        assertEquals("fatal", node["level"].asText())
        assertEquals("Application startup failed", node["message"].asText())
        assertEquals("application_start_failed", node["attributes"]["event"].asText())
        assertEquals("SQLException", node["attributes"]["error_class"].asText())
        assertEquals("DATABASE_AUTHENTICATION_FAILED", node["attributes"]["code"].asText())
        assertFalse(raw.contains("private") || raw.contains("secret") || raw.contains("stack") || raw.contains("jdbc"))
        assertEquals(1, raw.count { it == '\n' })
        assertTrue(raw.toByteArray().size <= DIAGNOSTIC_BYTES)
    }

    @Test
    fun `closed SQL state categories are useful and arbitrary state values are never serialized`() {
        val states =
            mapOf(
                "42501" to "DATABASE_ACCESS_DENIED",
                "42P01" to "DATABASE_SCHEMA_MISSING",
                "08006" to "DATABASE_CONNECTION_UNAVAILABLE",
                "private-state-value" to "DATABASE_FAILURE",
            )
        for ((state, expected) in states) {
            val event = startup()
            event.setThrowableProxy(ThrowableProxy(SQLException("private message", state)))
            val raw = append(event)
            assertEquals(
                expected,
                JsonMapper
                    .builder()
                    .build()
                    .readTree(raw)["attributes"]["code"]
                    .asText(),
            )
            assertFalse(raw.contains(state) || raw.contains("private message"))
        }
    }

    @Test
    fun `unknown exception class names and exception object content are not diagnostic fields`() {
        val event = startup()
        event.setThrowableProxy(ThrowableProxy(PrivateNamedCanaryException()))
        val raw = append(event)
        assertEquals(
            "unclassified",
            JsonMapper
                .builder()
                .build()
                .readTree(raw)["attributes"]["error_class"]
                .asText(),
        )
        assertFalse(raw.contains("PrivateNamedCanaryException") || raw.contains("sensitive"))
    }

    @Test
    fun `bounded proxy traversal retains recognized framework type and never accesses throwable text or frames`() {
        var reads = 0
        val cause =
            Proxy.newProxyInstance(javaClass.classLoader, arrayOf(IThrowableProxy::class.java)) { self, method, _ ->
                when (method.name) {
                    "getClassName" -> {
                        reads += 1
                        "org.springframework.beans.factory.NoUniqueBeanDefinitionException"
                    }
                    "getCause" -> self
                    else -> error("Throwable details must not be inspected")
                }
            } as IThrowableProxy
        val event = startup()
        val input =
            Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ILoggingEvent::class.java)) { _, method, arguments ->
                if (method.name == "getThrowableProxy") cause else method.invoke(event, *(arguments ?: emptyArray()))
            } as ILoggingEvent
        val raw = append(input)
        val attributes = JsonMapper.builder().build().readTree(raw)["attributes"]
        assertEquals("NoUniqueBeanDefinitionException", attributes["error_class"].asText())
        assertEquals("BEAN_AMBIGUOUS", attributes["code"].asText())
        assertEquals(16, reads)
    }

    @Test
    fun `structured draft diagnostics retain only their approved event and never emit arbitrary maps`() {
        val encoded =
            SafeJsonDiagnostics.emit(
                mapOf("event" to "draft_encrypt_failed", "draftId" to "private-row", "password" to "secret"),
            )
        assertEquals("{\"event\":\"draft_encrypt_failed\"}", encoded)
        val event = LoggingEvent()
        event.level = Level.WARN
        event.timeStamp = 1
        event.loggerName = "com.spoolin.core.features.draft.SaveDraft"
        event.message = encoded
        val raw = append(event)
        val node = JsonMapper.builder().build().readTree(raw)
        assertEquals("Draft encryption failed", node["message"].asText())
        assertEquals("draft_encrypt_failed", node["attributes"]["event"].asText())
        assertFalse(raw.contains("private-row") || raw.contains("secret"))
        assertEquals("{\"event\":\"diagnostic\"}", SafeJsonDiagnostics.emit(mapOf("arbitrary" to "private")))
    }

    @Test
    fun `legacy message conversion uses fixed event summary and never formatted private framework text`() {
        val event = startup()
        assertEquals("\"Application startup failed\"", JsonLogMessageConverter().convert(event))
        val privateEvent = LoggingEvent().also {
            it.loggerName = "org.springframework.boot.SpringApplication"
            it.level = Level.ERROR
            it.message = "private user={}\nforged"
            it.argumentArray = arrayOf("secret")
        }
        assertEquals("\"Diagnostic event\"", JsonLogMessageConverter().convert(privateEvent))
    }

    @Test
    fun `classified records retain current real SDK trace binding before asynchronous delivery`() {
        SdkTracerProvider.builder().build().use { provider ->
            val span = provider.get("diagnostic-test").spanBuilder("startup-observation").startSpan()
            val raw = tracedStartup(span)
            val node = JsonMapper.builder().build().readTree(raw)
            assertEquals(span.spanContext.traceId, node["trace_id"].asText())
            assertEquals(span.spanContext.spanId, node["span_id"].asText())
        }
    }

    private fun tracedStartup(span: io.opentelemetry.api.trace.Span): String =
        try {
            span.makeCurrent().use { append(startup()) }
        } finally {
            span.end()
        }

    @Test
    fun `loss of a classified fatal record counts its encoded level rather than its original framework level`() {
        val registry = SimpleMeterRegistry()
        try {
            DiagnosticDropMetrics.shared.bind("core", registry)
            val previous = fatalLoss(registry)
            val failed =
                object : OutputStream() {
                    override fun write(value: Int): Unit = throw IOException("private sink error")
                }
            val appender = BoundedJsonAppender { failed to failed }
            appender.serviceName = "core"
            appender.start()
            appender.doAppend(startup())
            appender.stop()
            assertEquals(previous + 1.0, fatalLoss(registry))
        } finally {
            registry.close()
        }
    }

    private class PrivateNamedCanaryException : IllegalStateException("sensitive exception payload")
}

private fun fatalLoss(registry: SimpleMeterRegistry): Double =
    registry
        .find("spoolin.log.dropped")
        .tag("level", "fatal")
        .tag("reason", "sink_failed")
        .functionCounter()
        ?.count() ?: 0.0

private fun append(event: ILoggingEvent): String {
    val output = ByteArrayOutputStream()
    val appender = BoundedJsonAppender { output to output }
    appender.serviceName = "core"
    appender.serviceEnvironment = "test"
    appender.start()
    appender.doAppend(event)
    appender.stop()
    return output.toString(Charsets.UTF_8)
}

private fun startup(): LoggingEvent =
    LoggingEvent().also {
        it.level = Level.ERROR
        it.timeStamp = 1
        it.loggerName = "org.springframework.boot.SpringApplication"
        it.message = "Application run failed"
        it.argumentArray = arrayOf("private credential")
        it.mdcPropertyMap = mapOf("token" to "private token")
    }
