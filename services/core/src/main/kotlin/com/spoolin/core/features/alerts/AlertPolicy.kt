package com.spoolin.core.features.alerts

import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.OperationalSettings
import org.springframework.core.env.Environment

private const val ALERT_NAMES =
    "ServiceUnavailable,WorkQueueBacklog,SystemMutationFailure,ExhaustedWork,DelayedWork," +
        "NotificationUnresolved,PoolPressure,DiskWarning,DiskCritical,CryptoFault,ProcessingFailures,TunnelUnavailable"
private const val ALERT_SUMMARIES =
    "Selected service unavailable|Durable work backlog elevated|" +
        "System mutation failure ratio elevated|Work requires controlled recovery|Due work delayed|" +
        "Notification requires reconciliation|Database pool pressure elevated|Owned volume headroom low|" +
        "Owned volume headroom critical|Cryptographic protection failure|Media processing failure ratio elevated|" +
        "Selected tunnel disconnected"

enum class AlertMode(
    val value: String,
) {
    DATABASE("database"),
    SMTP("smtp"),
    SLACK("slack"),
    GOOGLE_CHAT("google_chat"),
}

data class AlertPolicy(
    val services: Set<String> = setOf("core", "catalog", "media", "mcp", "postgres"),
    val severities: Set<String> = setOf("warning", "critical"),
    val names: Set<String> = ALERT_NAMES.split(',').toSet(),
    val annotationValues: Map<String, Set<String>> = mapOf("summary" to ALERT_SUMMARIES.split('|').toSet()),
    val environment: String = "local",
    val mode: AlertMode = AlertMode.DATABASE,
    val maxAttempts: Int = 3,
    val baseDelayMs: Int = 500,
    val capDelayMs: Int = 5000,
    val multiplier: Int = 2,
    val deadlineMs: Int = 60000,
) {
    init {
        require(services.isNotEmpty() && severities.isNotEmpty() && names.isNotEmpty())
        require((services + severities + names + environment).all { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) })
        require(maxAttempts in 1..10 && baseDelayMs in 1..60000 && capDelayMs in baseDelayMs..3600000 && multiplier in 1..10)
        require(deadlineMs in 1000..3600000)
        require(annotationValues.keys.all { it in setOf("summary", "description", "runbook") })
        require(annotationValues.values.flatten().all { it.codePointCount(0, it.length) <= 1024 && it.none(Char::isISOControl) })
    }

    companion object {
        fun fromEnvironment(environment: Environment): AlertPolicy {
            val values =
                OperationalSettings.retry
                    .mapNotNull { setting ->
                        environment.getProperty(setting.name)?.let { setting.name to it }
                    }.toMap()
            val source = ConfigurationSources(values)
            val retry =
                OperationalSettings.retry.associate { setting ->
                    setting.name to
                        source.integer(setting.name, setting.default, setting.min, setting.max).value
                }

            fun set(
                name: String,
                default: String,
            ): Set<String> =
                (environment.getProperty(name) ?: default)
                    .split(',')
                    .map(String::trim)
                    .toSet()

            fun annotations(
                name: String,
                default: String,
            ): Set<String> =
                (environment.getProperty(name) ?: default)
                    .split('|')
                    .filter(String::isNotEmpty)
                    .toSet()
            val mode =
                AlertMode.entries.singleOrNull { it.value == environment.getProperty("NOTIFICATION_MODE", "database") }
                    ?: throw IllegalArgumentException("NOTIFICATION_MODE: Select a supported mode")
            return AlertPolicy(
                set("ALERT_ALLOWED_SERVICES", "core,catalog,media,mcp,postgres"),
                set("ALERT_ALLOWED_SEVERITIES", "warning,critical"),
                set("ALERT_ALLOWED_NAMES", ALERT_NAMES),
                mapOf(
                    "summary" to annotations("ALERT_ALLOWED_SUMMARIES", ALERT_SUMMARIES),
                    "description" to annotations("ALERT_ALLOWED_DESCRIPTIONS", ""),
                    "runbook" to annotations("ALERT_ALLOWED_RUNBOOKS", ""),
                ),
                environment.getProperty("SPOOLIN_ENVIRONMENT", "local"),
                mode,
                retry.getValue("RETRY_MAX_RETRIES") + 1,
                retry.getValue("RETRY_BASE_DELAY_MS"),
                retry.getValue("RETRY_MAX_DELAY_MS"),
                retry.getValue("RETRY_MULTIPLIER"),
                retry.getValue("RETRY_DEADLINE_MS"),
            )
        }
    }
}

class InvalidAlert : IllegalArgumentException("Alert payload is invalid")

class AlertCapacity : IllegalArgumentException("Alert payload exceeds its bounded representation")
