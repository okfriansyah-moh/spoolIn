package com.spoolin.core.features.alerts

import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.OffsetDateTime

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
        if (bytes.size > 65536) throw AlertCapacity()
        return try {
            val root = mapper.readTree(bytes)
            if (!root.isObject || text(root, "version") != "4" || text(root, "status") !in setOf("firing", "resolved")) invalid()
            val truncated = root.get("truncatedAlerts")
            if (truncated != null && (!truncated.isIntegralNumber || truncated.asLong() != 0L)) invalid()
            val alerts = root.get("alerts") ?: invalid()
            if (!alerts.isArray || alerts.size() !in 1..20) invalid()
            alerts.map { episode(it) }.also { episodes ->
                if (text(root, "status") == "resolved" && episodes.any { it.status != "resolved" }) invalid()
            }
        } catch (failure: AlertCapacity) {
            throw failure
        } catch (_: Exception) {
            throw InvalidAlert()
        }
    }

    private fun episode(node: JsonNode): AlertEpisode {
        if (!node.isObject) invalid()
        val status = text(node, "status")
        if (status !in setOf("firing", "resolved")) invalid()
        val fingerprint = text(node, "fingerprint")
        if (!fingerprint.matches(Regex("[0-9a-f]{16}"))) invalid()
        val start = instant(text(node, "startsAt"))
        val end = instant(text(node, "endsAt"))
        if (status == "resolved" && end < start) invalid()
        val labels = fields(node.get("labels") ?: invalid())
        val annotations = fields(node.get("annotations") ?: invalid())
        val service = labels["service"]?.takeIf { it in policy.services } ?: invalid()
        val severity = labels["severity"]?.takeIf { it in policy.severities } ?: invalid()
        val name = labels["alertname"]?.takeIf { it in policy.names } ?: invalid()
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
        annotations.forEach { (key, value) -> if (value in policy.annotationValues[key].orEmpty()) summary[key] = value }
        if (mapper.writeValueAsBytes(summary).size > 8192) throw AlertCapacity()
        return AlertEpisode(fingerprint, start, status, summary.toMap())
    }

    private fun fields(node: JsonNode): Map<String, String> {
        if (!node.isObject || node.size() > 64) invalid()
        return node.propertyNames().associateWith { key ->
            text(node, key).also { value ->
                if (value.codePointCount(0, value.length) > 1024 || value.any(Char::isISOControl)) invalid()
            }
        }
    }

    private fun instant(value: String): Instant {
        if (!value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T.*(Z|[+-][0-9]{2}:[0-9]{2})"))) invalid()
        return OffsetDateTime.parse(value).toInstant()
    }

    private fun text(
        node: JsonNode,
        name: String,
    ): String = node.get(name)?.takeIf { it.isString }?.asString() ?: invalid()

    private fun invalid(): Nothing = throw InvalidAlert()
}

data class AlertEpisode(
    val fingerprint: String,
    val startedAt: Instant,
    val status: String,
    val summary: Map<String, String>,
) {
    val key: String get() = "alert:$fingerprint:episode:$startedAt:$status"

    override fun toString(): String = "AlertEpisode(redacted)"
}
