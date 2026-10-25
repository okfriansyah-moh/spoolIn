package com.spoolin.support.notifications

import com.spoolin.support.fixtures.JdbcBoundary
import com.spoolin.support.work.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.net.ssl.SSLContext

class NotificationBoundaryRulesTest {
    @TempDir lateinit var root: Path
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val now = Instant.parse("2026-01-02T03:04:05Z")
    private fun work() = WorkClaim(id, id, "NOTIFICATION", "{}", 1, 4, now.plusSeconds(120), id, 2, id, 100, 1000, 2, 0)
    private fun item(summary: String = "{\"service\":\"media\",\"summary\":\"Job exhausted\"}") = NotificationClaim(work(), "slack", summary)
    private fun time() = mapOf("1" to Timestamp.from(now))
    private fun selected() = mapOf<String, Any?>("id" to id, "operation_id" to id, "payload" to "{}", "attempt_count" to 0,
        "max_attempts" to 4, "deadline_at" to Timestamp.from(now.plusSeconds(120)), "lease_epoch" to 1L,
        "correlation_id" to id, "base_delay_ms" to 100, "cap_delay_ms" to 1000, "backoff_multiplier" to 2,
        "generation" to 0, "notification_mode" to "slack", "notification_summary" to "{}")
    private fun claimDb() = JdbcBoundary().rows().rows(mapOf("1" to 0)).rows(time()).rows(selected()).rows()
    private fun response(value: String, mode: String = "slack") = WebhookResponse.read(ByteArrayInputStream(value.toByteArray()), item(), mode)
    private fun secret(name: String, value: String): String {
        val path = root.resolve(name)
        Files.writeString(path, value)
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        return path.toString()
    }

    @Test fun `webhook acknowledgment accepts only supported provider receipts`() {
        val accepted = response("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok")
        assertEquals(AttemptOutcome.SUCCEEDED, accepted.outcome)
        assertEquals("slack:http-200:$id", accepted.reference)
        val body = "{\"name\":\"spaces/abc/messages/def-1\"}"
        assertEquals("spaces/abc/messages/def-1", response("HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\n\r\n$body", "google_chat").reference)
        assertEquals(AttemptOutcome.SUCCEEDED, response("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n2\r\nok\r\n0\r\n\r\n").outcome)
        assertEquals(AttemptOutcome.UNCERTAIN, response("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nno").outcome)
        assertEquals(AttemptOutcome.UNCERTAIN, response("HTTP/1.1 503 Busy\r\n\r\n").outcome)
        assertEquals(AttemptOutcome.UNCERTAIN, response("invalid\r\n").outcome)
        assertEquals(AttemptOutcome.PERMANENT, response("HTTP/1.1 403 Forbidden\r\n\r\n").outcome)
    }

    @Test fun `rate limiting caps retry delay and malformed wire replies cannot masquerade as acceptance`() {
        for ((value, expected) in listOf("0" to 1000L, "999999" to 3600000L, "bad" to 1000L)) {
            val result = response("HTTP/1.1 429 Limited\r\nRetry-After: $value\r\n\r\n")
            assertEquals(AttemptOutcome.TRANSIENT, result.outcome)
            assertEquals(expected, result.retryAfterMs)
        }
        for (wire in listOf("HTTP/1.1 200 OK\r\nContent-Length: 3\r\n\r\nok", "HTTP/1.1 200 OK\r\nContent-Length: 8193\r\n\r\n",
            "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nContent-Length: 2\r\n\r\nok",
            "HTTP/1.1 200 OK\r\nTransfer-Encoding: gzip\r\n\r\nok", "HTTP/1.1 200 OK\r\nBadHeader\r\n\r\n")) {
            assertThrows(IllegalArgumentException::class.java) { response(wire) }
        }
        assertThrows(IllegalArgumentException::class.java) { boundedLine(ByteArrayInputStream("bad\n".toByteArray())) }
        assertThrows(IllegalArgumentException::class.java) { boundedLine(ByteArrayInputStream("x".repeat(8193).toByteArray())) }
    }

    @Test fun `smtp envelope and multiline replies classify temporary and permanent rejections`() {
        val output = ByteArrayOutputStream()
        SmtpSession(ByteArrayInputStream("250-smtp\r\n250 OK\r\n250 OK\r\n354 continue\r\n".toByteArray()), output)
            .envelope("sender@example.test", "owner@example.test")
        assertEquals("MAIL FROM:<sender@example.test>\r\nRCPT TO:<owner@example.test>\r\nDATA\r\n", output.toString())
        for ((code, temporary) in listOf("450" to true, "550" to false, "bad" to false)) {
            val failure = assertThrows(SmtpRejection::class.java) {
                SmtpSession(ByteArrayInputStream("$code no\r\n".toByteArray()), ByteArrayOutputStream()).expect(250)
            }
            assertEquals(temporary, failure.temporary)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SmtpSession(ByteArrayInputStream("250-a\r\n550 b\r\n".toByteArray()), ByteArrayOutputStream()).expect(250)
        }
    }

    @Test fun `notification text is allowlisted bounded and never contains undeclared private fields`() {
        val text = notificationText(item("{\"service\":\"media\",\"password\":\"private-secret\",\"summary\":\"Job exhausted\"}"))
        assertTrue(text.contains("service: media"))
        assertTrue(text.contains("notificationWorkId: $id"))
        assertFalse(text.contains("private-secret"))
        for (summary in listOf("[]", "{\"summary\":123}", "{\"summary\":\"bad\\nline\"}", "{\"summary\":\"${"x".repeat(1025)}\"}")) {
            assertThrows(IllegalArgumentException::class.java) { notificationText(item(summary)) }
        }
        assertEquals("NotificationClaim(redacted)", item().toString())
    }

    @Test fun `protected notification configuration accepts declared endpoints and rejects unsafe authorities`() {
        assertNull(NotificationSettings.transport { null })
        val slack = secret("slack", "https://hooks.slack.com/services/A/B/C\n")
        val google = secret("google", "https://chat.googleapis.com/v1/spaces/abc/messages?key=a&token=b")
        for ((mode, key, value) in listOf(Triple("slack", "NOTIFICATION_SLACK_WEBHOOK_FILE", slack),
            Triple("google_chat", "NOTIFICATION_GOOGLE_CHAT_WEBHOOK_FILE", google))) {
            val settings = mapOf("NOTIFICATION_MODE" to mode, key to value)
            assertEquals(mode, NotificationSettings.transport(settings::get)!!.mode)
        }
        val smtp = mapOf("NOTIFICATION_MODE" to "smtp", "NOTIFICATION_SMTP_HOST" to "mail.example.test",
            "NOTIFICATION_SMTP_FROM" to "from@example.test", "NOTIFICATION_SMTP_TO" to "to@example.test")
        assertEquals("smtp", NotificationSettings.transport(smtp::get)!!.mode)
        for (extra in listOf(mapOf("NOTIFICATION_SMTP_HOST" to "bad\n"), mapOf("NOTIFICATION_SMTP_TLS" to "plain"),
            mapOf("NOTIFICATION_SMTP_PORT" to "0"), mapOf("NOTIFICATION_SMTP_FROM" to "from\n@example.test"))) {
            assertThrows(IllegalArgumentException::class.java) { NotificationSettings.transport((smtp + extra)::get) }
        }
        for ((index, endpoint) in listOf("http://hooks.slack.com/services/A/B/C", "https://evil.test/services/A/B/C",
            "https://hooks.slack.com/services/A/B/C?token=secret", "https://user@hooks.slack.com/services/A/B/C").withIndex()) {
            val path = secret("bad$index", endpoint)
            val values = mapOf("NOTIFICATION_MODE" to "slack", "NOTIFICATION_SLACK_WEBHOOK_FILE" to path)
            assertThrows(IllegalArgumentException::class.java) { NotificationSettings.transport(values::get) }
        }
        assertThrows(IllegalArgumentException::class.java) { NotificationSettings.transport { "unknown" } }
    }

    @Test fun `notification secret files reject links permissive modes oversized bytes and control characters`() {
        val safe = Path.of(secret("safe", "secret\r\n"))
        assertEquals("secret", ProtectedNotificationSecret.read(safe.toString()))
        val link = root.resolve("link")
        Files.createSymbolicLink(link, safe)
        assertThrows(IllegalArgumentException::class.java) { ProtectedNotificationSecret.read(link.toString()) }
        for ((index, content) in listOf("", "x".repeat(4097), "line\nsecond").withIndex()) {
            val path = secret("invalid$index", content)
            assertThrows(IllegalArgumentException::class.java) { ProtectedNotificationSecret.read(path) }
        }
        Files.setPosixFilePermissions(safe, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { ProtectedNotificationSecret.read(safe.toString()) }
        assertThrows(IllegalArgumentException::class.java) { SmtpCredentials("username", null) }
        assertThrows(IllegalArgumentException::class.java) { SmtpCredentials("username", "bad\nsecret") }
        assertEquals("SmtpCredentials(redacted)", SmtpCredentials(null, null).toString())
    }

    @Test fun `store claims a bounded notification and reports no work at capacity`() {
        val claimed = requireNotNull(NotificationStore(claimDb().source).claim())
        assertEquals("slack", claimed.mode)
        assertEquals(1, claimed.work.attemptCount)
        assertEquals(2L, claimed.work.leaseEpoch)
        val full = JdbcBoundary().rows().rows(mapOf("1" to 2))
        assertNull(NotificationStore(full.source).claim())
        val empty = JdbcBoundary().rows().rows(mapOf("1" to 0)).rows(time()).rows()
        assertNull(NotificationStore(empty.source).claim())
    }

    @Test fun `send budget is capped by live lease and never permits a superseded sender`() {
        val lease = mapOf("id" to id, "state" to "LEASED", "lease_owner" to id, "lease_epoch" to 2L,
            "lease_until" to Timestamp.from(now.plusSeconds(30)), "deadline_at" to Timestamp.from(now.plusSeconds(120)))
        assertEquals(14000L, NotificationStore(JdbcBoundary().rows(time()).rows(lease).source).budgetMs(work()))
        assertEquals(0L, NotificationStore(JdbcBoundary().rows(time()).rows(lease + ("lease_epoch" to 3L)).source).budgetMs(work()))
        assertEquals(0L, NotificationStore(JdbcBoundary().rows(time()).rows().source).budgetMs(work()))
    }

    @Test fun `notification completion handles accepted uncertain rejected and retryable outcomes through the work fence`() {
        for (outcome in AttemptOutcome.entries) {
            val db = JdbcBoundary().rows(time()).rows()
            assertTrue(NotificationStore(db.source).finish(work(), NotificationResult(outcome, "SAFE_CODE", reference = "receipt")))
            assertEquals(1, db.commits)
        }
        val superseded = JdbcBoundary().rows(time()).rows().apply { updates.add(0) }
        assertFalse(NotificationStore(superseded.source).finish(work(), NotificationResult(AttemptOutcome.SUCCEEDED)))
        val expired = JdbcBoundary().rows(time()).rows()
        assertTrue(NotificationStore(expired.source).finish(work().copy(attemptCount = 4), NotificationResult(AttemptOutcome.TRANSIENT)))
    }

    @Test fun `exhaustion recording counts newly persisted notifications and refuses invalid configuration`() {
        val source = mapOf("id" to id, "operation_id" to id, "kind" to "MEDIA_PROCESS", "generation" to 1,
            "attempt_count" to 4, "max_attempts" to 4, "correlation_id" to id)
        for (mode in listOf("database", "slack")) {
            val db = JdbcBoundary().rows(source).rows(time())
            assertEquals(1, ExhaustionNotifications(db.source, "media", mode).recordExhaustion())
            assertEquals(1, db.commits)
        }
        val duplicate = JdbcBoundary().rows(source).rows(time()).apply { updates.add(0) }
        assertEquals(0, ExhaustionNotifications(duplicate.source, "catalog").recordExhaustion())
        assertEquals(0, ExhaustionNotifications.configured(JdbcBoundary().rows().source, "core") { null }.recordExhaustion())
        for ((owner, mode, environment) in listOf(Triple("invalid", "database", "local"), Triple("core", "unknown", "local"), Triple("core", "database", "bad space"))) {
            assertThrows(IllegalArgumentException::class.java) { ExhaustionNotifications(JdbcBoundary().source, owner, mode, environment) }
        }
    }
}
