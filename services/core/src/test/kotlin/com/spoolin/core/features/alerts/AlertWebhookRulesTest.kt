package com.spoolin.core.features.alerts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AlertWebhookRulesTest {
    private val parser = AlertWebhook(AlertPolicy())

    @Test
    fun `episode key normalizes timestamp and strips unapproved labels annotations and URLs`() {
        val episode = parser.parse(payload().toByteArray()).single()
        assertEquals("alert:0123456789abcdef:episode:2026-10-18T01:00:00Z:firing", episode.key)
        assertEquals("Durable work backlog elevated", episode.summary["summary"])
        assertFalse(episode.summary.containsKey("secret"))
        assertFalse(episode.summary.values.any { it.contains("private.invalid") })
        assertEquals(episode.key, parser.parse(payload().replace("01:00:00Z", "02:00:00+01:00").toByteArray()).single().key)
    }

    @Test
    fun `invalid fingerprints statuses timestamps services and duplicate properties fail the entire payload`() {
        listOf(
            "0123456789abcdef" to "0123456789abcdeF",
            "firing" to "unknown",
            "core" to "foreign",
            "2026-10-18T01:00:00Z" to "yesterday",
            "\"version\":\"4\"" to "\"version\":\"4\",\"version\":\"4\"",
        ).forEach { (before, after) ->
            assertThrows(InvalidAlert::class.java) {
                parser.parse(payload().replace(before, after).toByteArray())
            }
        }
        assertThrows(InvalidAlert::class.java) { parser.parse((payload() + "{}").toByteArray()) }
    }

    @Test
    fun `wire count body and field bounds reject rather than truncate`() {
        assertThrows(AlertCapacity::class.java) { parser.parse(ByteArray(65537)) }
        assertThrows(InvalidAlert::class.java) { parser.parse(payload().replace("private.invalid", "x".repeat(1025)).toByteArray()) }
        assertThrows(
            InvalidAlert::class.java,
        ) { parser.parse(payload().replace("\"truncatedAlerts\":0", "\"truncatedAlerts\":1").toByteArray()) }
    }

    companion object {
        fun payload(): String =
            """{"version":"4","status":"firing","truncatedAlerts":0,"alerts":[{
          "status":"firing","fingerprint":"0123456789abcdef","startsAt":"2026-10-18T01:00:00Z",
          "endsAt":"0001-01-01T00:00:00Z","labels":{"service":"core","severity":"warning",
          "alertname":"WorkQueueBacklog","secret":"private.invalid"},"annotations":{
          "summary":"Durable work backlog elevated","runbook":"https://private.invalid"},
          "generatorURL":"https://private.invalid"}]}"""
    }
}
