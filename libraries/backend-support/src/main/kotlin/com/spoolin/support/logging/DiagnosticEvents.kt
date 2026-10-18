package com.spoolin.support.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxy
import java.sql.SQLException

private const val MAX_CAUSES = 16
private const val BROWSER_GRAPHQL_LOGGER = "com.spoolin.core.boundary.http.BrowserGraphql"
private const val BROWSER_REQUESTS_LOGGER = "com.spoolin.core.boundary.http.BrowserRequests"
private const val RATE_LOGGER = "com.spoolin.core.features.ratelimit.RateAdmission"
private const val DRAFT_LOGGER = "com.spoolin.core.features.draft.SaveDraft"

internal data class DiagnosticDetails(
    val message: String,
    val attributes: Map<String, String>,
    val level: DiagnosticLevel? = null,
)

/** Exact source events and a closed exception catalog, never exception messages or stack frames. */
internal object DiagnosticEvents {
    fun classify(event: ILoggingEvent): DiagnosticDetails {
        val details =
            if (startupFailed(event)) {
                DiagnosticDetails(
                    "Application startup failed",
                    mapOf("event" to "application_start_failed", "code" to "APPLICATION_START_FAILED"),
                    DiagnosticLevel.FATAL,
                )
            } else {
                source(event.loggerName, event.message)
            }
        val failure = failure(event.throwableProxy) ?: return details
        return details.copy(
            message = if (details.attributes["event"] == "diagnostic") "Diagnostic failure" else details.message,
            attributes = details.attributes + mapOf("error_class" to failure.name, "code" to failure.code),
        )
    }

    fun generic(): DiagnosticDetails = DiagnosticDetails("Diagnostic event", mapOf("event" to "diagnostic"))

    fun origin(logger: String?): DiagnosticDetails =
        if (logger == DRAFT_LOGGER) {
            generic().copy(attributes = mapOf("event" to "diagnostic", "operation" to "saveDraft"))
        } else {
            generic()
        }

    private fun startupFailed(event: ILoggingEvent): Boolean =
        event.loggerName == "org.springframework.boot.SpringApplication" &&
            event.message == "Application run failed" && event.level == Level.ERROR

    private val boundaryEvents =
        mapOf(
            (RATE_LOGGER to "Rate admission failed") to "rate_admission_failed",
            ("com.spoolin.core.boundary.graphql.PolicyGraphql" to "Graphql execution failed") to
                "graphql_execution_failed",
            (BROWSER_REQUESTS_LOGGER to "Browser execution failed") to "browser_execution_failed",
            (BROWSER_GRAPHQL_LOGGER to "Browser identity unavailable") to "browser_identity_unavailable",
            (BROWSER_GRAPHQL_LOGGER to "Browser admission unavailable") to "browser_admission_unavailable",
        )

    private fun source(
        logger: String?,
        message: String?,
    ): DiagnosticDetails =
        boundaryEvents[logger to message]?.let {
            DiagnosticDetails("Browser boundary unavailable", mapOf("event" to it))
        } ?: knownSource(logger, message)

    private fun knownSource(
        logger: String?,
        message: String?,
    ): DiagnosticDetails =
        when {
            logger == DRAFT_LOGGER && SafeJsonDiagnostics.isDraftFailure(message) ->
                DiagnosticDetails(
                    "Draft encryption failed",
                    mapOf("event" to "draft_encrypt_failed", "operation" to "saveDraft"),
                )
            logger == "com.spoolin.core.features.crypto.AccountCryptography" &&
                message == "Protected account unavailable row={}" ->
                DiagnosticDetails("Protected account unavailable", mapOf("event" to "account_protection_unavailable"))
            logger == "com.spoolin.core.features.draft.DraftCryptography" &&
                message == "Protected draft unavailable row={}" ->
                DiagnosticDetails("Protected draft unavailable", mapOf("event" to "draft_protection_unavailable"))
            else -> origin(logger)
        }

    private fun failure(first: IThrowableProxy?): KnownFailure? = first?.let(::causedFailure)

    private fun causedFailure(first: IThrowableProxy): KnownFailure {
        var current = first
        var selected = KnownFailure("unclassified", "UNCLASSIFIED_FAILURE")
        repeat(MAX_CAUSES) {
            knownFailures[current.className]?.let { selected = it.copy(code = failureCode(current, it.code)) }
            current = current.cause ?: return selected
        }
        return selected
    }

    private fun failureCode(
        proxy: IThrowableProxy,
        default: String,
    ): String {
        val throwable = (proxy as? ThrowableProxy)?.throwable
        if (throwable is io.grpc.StatusRuntimeException) return "RPC_" + throwable.status.code.name
        if (throwable is io.grpc.StatusException) return "RPC_" + throwable.status.code.name
        val failure = throwable as? SQLException ?: return default
        return when (failure.sqlState) {
            "28P01", "28000" -> "DATABASE_AUTHENTICATION_FAILED"
            "42501" -> "DATABASE_ACCESS_DENIED"
            "42P01", "42703", "3F000" -> "DATABASE_SCHEMA_MISSING"
            "08001", "08003", "08004", "08006", "57P03" -> "DATABASE_CONNECTION_UNAVAILABLE"
            else -> default
        }
    }
}

private data class KnownFailure(
    val name: String,
    val code: String,
)

private val knownFailures =
    listOf(
        "org.springframework.beans.factory.BeanCreationException" to "BEAN_CREATION_FAILED",
        "org.springframework.beans.factory.UnsatisfiedDependencyException" to "DEPENDENCY_UNSATISFIED",
        "org.springframework.beans.factory.NoSuchBeanDefinitionException" to "BEAN_MISSING",
        "org.springframework.beans.factory.NoUniqueBeanDefinitionException" to "BEAN_AMBIGUOUS",
        "org.springframework.beans.factory.BeanCurrentlyInCreationException" to "BEAN_CYCLE",
        "org.springframework.beans.factory.BeanDefinitionStoreException" to "BEAN_DEFINITION_INVALID",
        "org.springframework.boot.context.properties.ConfigurationPropertiesBindException" to "CONFIGURATION_INVALID",
        "org.springframework.boot.context.properties.bind.BindException" to "CONFIGURATION_INVALID",
        "org.springframework.boot.context.properties.bind.validation.BindValidationException" to
            "CONFIGURATION_INVALID",
        "org.springframework.boot.web.server.WebServerException" to "WEB_SERVER_FAILED",
        "java.net.BindException" to "PORT_UNAVAILABLE",
        "java.net.ConnectException" to "CONNECTION_REFUSED",
        "java.net.UnknownHostException" to "HOST_UNRESOLVED",
        "io.grpc.StatusRuntimeException" to "RPC_FAILED",
        "io.grpc.StatusException" to "RPC_FAILED",
        "java.sql.SQLException" to "DATABASE_FAILURE",
        "java.sql.SQLTransientConnectionException" to "DATABASE_CONNECTION_UNAVAILABLE",
        "java.sql.SQLInvalidAuthorizationSpecException" to "DATABASE_AUTHENTICATION_FAILED",
        "java.sql.SQLTimeoutException" to "DATABASE_TIMEOUT",
        "org.postgresql.util.PSQLException" to "DATABASE_FAILURE",
        "org.flywaydb.core.api.FlywayException" to "MIGRATION_FAILED",
        "org.flywaydb.core.api.exception.FlywayValidateException" to "MIGRATION_VALIDATION_FAILED",
        "com.zaxxer.hikari.pool.HikariPool\$PoolInitializationException" to "DATABASE_POOL_FAILED",
        "java.lang.IllegalArgumentException" to "ARGUMENT_INVALID",
        "java.lang.IllegalStateException" to "STATE_INVALID",
        "java.lang.NoSuchMethodError" to "DEPENDENCY_INCOMPATIBLE",
        "java.lang.NoClassDefFoundError" to "DEPENDENCY_MISSING",
        "java.nio.file.AccessDeniedException" to "FILE_ACCESS_DENIED",
        "java.nio.file.NoSuchFileException" to "FILE_MISSING",
        "java.nio.file.InvalidPathException" to "FILE_CONFIGURATION_INVALID",
        "javax.net.ssl.SSLHandshakeException" to "TLS_HANDSHAKE_FAILED",
        "javax.net.ssl.SSLException" to "TLS_FAILED",
        "java.security.GeneralSecurityException" to "PROTECTION_UNAVAILABLE",
        "javax.crypto.AEADBadTagException" to "PROTECTION_AUTHENTICATION_FAILED",
        "com.spoolin.core.features.crypto.ProtectionUnavailable" to "PROTECTION_UNAVAILABLE",
    ).associate { (type, code) -> type to KnownFailure(type.substringAfterLast('.').substringAfterLast('$'), code) }
