package com.spoolin.core.features.alerts

import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.OffsetDateTime

private const val MAXIMUM_ALERT_BYTES = 65536
private const val MAXIMUM_ALERT_COUNT = 20
private const val MAXIMUM_SUMMARY_BYTES = 8192
private const val MAXIMUM_SOURCE_FIELDS = 64
private const val MAXIMUM_FIELD_CODE_POINTS = 1024

/** Incoming fields never select a destination, template, executable or URL fetch. */
class AlertWebhook(
    private val policy: AlertPolicy,
) {
    private val mapper =
        JsonMapper
            .builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build()

    fun parse(bytes: ByteArray): List<AlertEpisode> {
        if (bytes.size > MAXIMUM_ALERT_BYTES) throw AlertCapacity()
        return runCatching { parseEnvelope(bytes) }.getOrElse(::invalidFailure)
    }

    private fun invalidFailure(failure: Throwable): Nothing =
        when (failure) {
            is Error -> throw failure
            is AlertCapacity -> throw failure
            else -> invalid()
        }

    private fun parseEnvelope(bytes: ByteArray): List<AlertEpisode> {
        val root = mapper.readTree(bytes)
        validateEnvelope(root)
        val alerts = root.get("alerts") ?: invalid()
        if (!alerts.isArray || alerts.size() !in 1..MAXIMUM_ALERT_COUNT) invalid()
        val episodes = alerts.toList().map(::episode)
        if (AlertValues.text(root, "status") == "resolved" && episodes.any { it.status != "resolved" }) invalid()
        return episodes
    }

    private fun validateEnvelope(root: JsonNode) {
        if (!root.isObject || AlertValues.text(root, "version") != "4") invalid()
        if (AlertValues.text(root, "status") !in setOf("firing", "resolved")) invalid()
        val truncated = root.get("truncatedAlerts")
        if (truncated != null && (!truncated.isIntegralNumber || truncated.asLong() != 0L)) invalid()
    }

    private fun episode(node: JsonNode): AlertEpisode {
        if (!node.isObject) invalid()
        val status = AlertValues.status(node)
        val fingerprint = AlertValues.fingerprint(node)
        val start = AlertValues.instant(AlertValues.text(node, "startsAt"))
        val end = AlertValues.instant(AlertValues.text(node, "endsAt"))
        if (status == "resolved" && end < start) invalid()
        val labels = fields(node.get("labels") ?: invalid())
        val annotations = fields(node.get("annotations") ?: invalid())
        val service = allowed(labels, "service", policy.services)
        val severity = allowed(labels, "severity", policy.severities)
        val name = allowed(labels, "alertname", policy.names)
        val summary =
            mutableMapOf(
                "environment" to policy.environment,
                "service" to service,
                "severity" to severity,
                "alertname" to name,
                "status" to status,
                "startsAt" to start.toString(),
                "endsAt" to end.toString(),
            )
        annotations.forEach { (key, value) ->
            if (value in policy.annotationValues[key].orEmpty()) summary[key] = value
        }
        if (mapper.writeValueAsBytes(summary).size > MAXIMUM_SUMMARY_BYTES) throw AlertCapacity()
        return AlertEpisode(fingerprint, start, status, summary.toMap())
    }

    private fun allowed(
        labels: Map<String, String>,
        name: String,
        values: Set<String>,
    ): String = labels[name]?.takeIf { it in values } ?: invalid()

    private fun fields(node: JsonNode): Map<String, String> {
        if (!node.isObject || node.size() > MAXIMUM_SOURCE_FIELDS) invalid()
        return node.propertyNames().associateWith { key ->
            AlertValues.text(node, key).also { value ->
                val oversized = value.codePointCount(0, value.length) > MAXIMUM_FIELD_CODE_POINTS
                if (oversized || value.any(Char::isISOControl)) invalid()
            }
        }
    }
}

private object AlertValues {
    fun status(node: JsonNode): String =
        text(node, "status").also {
            if (it !in setOf("firing", "resolved")) invalid()
        }

    fun fingerprint(node: JsonNode): String =
        text(node, "fingerprint").also {
            if (!it.matches(Regex("[0-9a-f]{16}"))) invalid()
        }

    fun instant(value: String): Instant {
        if (!value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T.*(Z|[+-][0-9]{2}:[0-9]{2})"))) invalid()
        return OffsetDateTime.parse(value).toInstant()
    }

    fun text(
        node: JsonNode,
        name: String,
    ): String = node.get(name)?.takeIf { it.isString }?.asString() ?: invalid()
}

private fun invalid(): Nothing = throw InvalidAlert()

data class AlertEpisode(
    val fingerprint: String,
    val startedAt: Instant,
    val status: String,
    val summary: Map<String, String>,
) {
    val key: String get() = "alert:$fingerprint:episode:$startedAt:$status"

    override fun toString(): String = "AlertEpisode(redacted)"
}
