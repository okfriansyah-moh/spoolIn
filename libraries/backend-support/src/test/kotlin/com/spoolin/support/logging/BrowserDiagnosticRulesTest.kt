package com.spoolin.support.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.sql.SQLException

class BrowserDiagnosticRulesTest {
    private val events =
        listOf(
            Triple(
                "com.spoolin.core.boundary.graphql.PolicyGraphql",
                "Graphql execution failed",
                "graphql_execution_failed",
            ),
            Triple(
                "com.spoolin.core.features.ratelimit.RateAdmission",
                "Rate admission failed",
                "rate_admission_failed",
            ),
            Triple(
                "com.spoolin.core.boundary.http.BrowserRequests",
                "Browser execution failed",
                "browser_execution_failed",
            ),
            Triple(
                "com.spoolin.core.boundary.http.BrowserGraphql",
                "Browser identity unavailable",
                "browser_identity_unavailable",
            ),
            Triple(
                "com.spoolin.core.boundary.http.BrowserGraphql",
                "Browser admission unavailable",
                "browser_admission_unavailable",
            ),
        )

    @Test
    fun `boundary failures retain fixed classifications and exclude exception bodies and arguments`() {
        for ((logger, message, expected) in events) {
            val event =
                LoggingEvent().also {
                    it.loggerName = logger
                    it.message = message
                    it.level = Level.WARN
                    it.argumentArray = arrayOf("private-secret")
                    it.setThrowableProxy(ThrowableProxy(SQLException("private-secret", "42501")))
                }
            val raw =
                DiagnosticEnvelope("core", "test", "test")
                    .encode(
                        DiagnosticLevel.WARNING,
                        0,
                        DiagnosticEvents.classify(event),
                    ).toString(Charsets.UTF_8)
            val node = JsonMapper.builder().build().readTree(raw)
            assertEquals(expected, node["attributes"]["event"].asString())
            assertEquals("DATABASE_ACCESS_DENIED", node["attributes"]["code"].asString())
            assertFalse(raw.contains("private-secret") || raw.contains("42501"))
        }
    }

    @Test
    fun `RPC failures retain only the fixed status and never descriptions or metadata`() {
        val event =
            LoggingEvent().also {
                it.loggerName = "com.spoolin.core.boundary.graphql.PolicyGraphql"
                it.message = "Graphql execution failed"
                it.level = Level.WARN
                it.setThrowableProxy(
                    ThrowableProxy(
                        io.grpc.Status.UNAVAILABLE
                            .withDescription("private-secret")
                            .asRuntimeException(),
                    ),
                )
            }
        val raw =
            DiagnosticEnvelope("core", "test", "test")
                .encode(
                    DiagnosticLevel.WARNING,
                    0,
                    DiagnosticEvents.classify(event),
                ).toString(Charsets.UTF_8)
        val node = JsonMapper.builder().build().readTree(raw)
        assertEquals("RPC_UNAVAILABLE", node["attributes"]["code"].asString())
        assertFalse(raw.contains("private-secret"))
    }
}
