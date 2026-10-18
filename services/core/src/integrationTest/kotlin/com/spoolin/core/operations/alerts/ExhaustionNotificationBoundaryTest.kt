package com.spoolin.core.operations.alerts

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.support.notifications.ExhaustionNotifications
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.executeBound
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExhaustionNotificationBoundaryTest {
    private lateinit var fixture: OwnerFixture

    @BeforeAll
    fun start() {
        fixture = OwnerFixture("t034-exhaustion-notification-boundaries.json").start()
        fixture.membership("core", "grant")
        fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("core")
    }

    @AfterAll
    fun stop() = fixture.close()

    @Test
    fun `concurrent repeated observations record exact database shape and one atomic activity per generation`() {
        fixture.pool("core").use { pool ->
            val source = source()
            val recorder = ExhaustionNotifications(pool, "core", environment = "ci")
            val executor = Executors.newFixedThreadPool(4)
            try {
                val calls = (1..8).map { executor.submit<Int> { recorder.recordExhaustion() } }
                assertEquals(1, calls.sumOf { it.get(5, TimeUnit.SECONDS) })
            } finally { executor.shutdownNow() }
            assertEquals(0, recorder.recordExhaustion())
            inspect(source, 0)
            fixture.connection("core").use { it.executeBound("UPDATE durable_work SET generation=1 WHERE id=?", source) }
            assertEquals(1, recorder.recordExhaustion()); assertEquals(0, recorder.recordExhaustion())
            inspect(source, 1)
            fixture.connection("core").use { connection -> connection.prepareStatement(
                "SELECT count(*) FROM operational_event WHERE action='WORK_EXHAUSTION_RECORDED' AND subject_id IN " +
                    "(SELECT id FROM durable_work WHERE notification_key IN (?,?))").use {
                it.setString(1, "work:$source:generation:0"); it.setString(2, "work:$source:generation:1")
                it.executeQuery().use { rows -> assertTrue(rows.next()); assertEquals(2, rows.getInt(1)) }
            } }
        }
    }

    @Test
    fun `bounded scans drain every exhausted source rather than impose a notification count cap`() {
        val sources = List(35) { source() }
        fixture.pool("core").use { pool ->
            val recorder = ExhaustionNotifications(pool, "core")
            assertEquals(listOf(10, 10, 10, 5, 0), List(5) { recorder.recordExhaustion() })
            assertEquals(35, sources.sumOf(::notificationCount))
        }
    }

    @Test
    fun `activity insert failure rolls back notification and scan retries the retained source`() {
        fixture.pool("core").use { pool ->
            val source = source()
            eventFailure(true)
            try {
                assertThrows(SQLException::class.java) { ExhaustionNotifications(pool, "core").recordExhaustion() }
                assertEquals(0, notificationCount(source))
            } finally { eventFailure(false) }
            assertEquals(1, ExhaustionNotifications(pool, "core").recordExhaustion())
            inspect(source, 0)
        }
    }

    @Test
    fun `selected external mode saves bounded retry settings but never reports provider acceptance`() {
        fixture.pool("core").use { pool ->
            val source = source()
            val configuration = mapOf("NOTIFICATION_MODE" to "slack", "SPOOLIN_ENVIRONMENT" to "ci",
                "RETRY_MAX_RETRIES" to "1", "RETRY_BASE_DELAY_MS" to "20", "RETRY_MAX_DELAY_MS" to "40",
                "RETRY_MULTIPLIER" to "3", "RETRY_DEADLINE_MS" to "2000")
            assertEquals(1, ExhaustionNotifications.configured(pool, "media") { configuration[it] }.recordExhaustion())
            fixture.connection("core").use { connection -> connection.prepareStatement(
                "SELECT state,notification_state,max_attempts,base_delay_ms,cap_delay_ms,backoff_multiplier," +
                    "extract(epoch FROM deadline_at-created_at)*1000,notification_summary->>'service' FROM durable_work WHERE notification_key=?").use {
                it.setString(1, "work:$source:generation:0"); it.executeQuery().use { rows ->
                    assertTrue(rows.next()); assertEquals("READY", rows.getString(1)); assertEquals("PENDING", rows.getString(2))
                    assertEquals(2, rows.getInt(3)); assertEquals(20, rows.getInt(4)); assertEquals(40, rows.getInt(5))
                    assertEquals(3, rows.getInt(6)); assertEquals(2000, rows.getInt(7)); assertEquals("media", rows.getString(8))
                }
            } }
        }
    }

    @Test
    fun `failed notification never recursively produces another notification and legacy observations do not resend`() {
        fixture.pool("core").use { pool ->
            val source = source()
            val old = WorkRequest("notification-0", "ARTWORK", source, Instant.MAX).id(source)
            fixture.connection("core").use { connection ->
                connection.executeBound("INSERT INTO durable_work(id,kind,payload,state,max_attempts,deadline_at,correlation_id," +
                    "base_delay_ms,cap_delay_ms,backoff_multiplier,notification_key,notification_mode,notification_state,notification_summary) " +
                    "VALUES (?,'NOTIFICATION','{}','EXHAUSTED',3,clock_timestamp(),?,500,5000,2,?,'slack','UNKNOWN','{}')",
                    old, UUID.randomUUID(), "$source:0")
            }
            assertEquals(0, ExhaustionNotifications(pool, "catalog").recordExhaustion())
            assertEquals(0, notificationCount(source))
            fixture.connection("core").use { connection -> connection.prepareStatement(
                "SELECT notification_state FROM durable_work WHERE id=?").use {
                it.setObject(1, old); it.executeQuery().use { rows -> assertTrue(rows.next()); assertEquals("UNKNOWN", rows.getString(1)) }
            } }
        }
    }

    private fun source(): UUID {
        val id = UUID.randomUUID()
        fixture.connection("core").use { it.executeBound("INSERT INTO durable_work(id,kind,payload,state,attempt_count," +
            "max_attempts,deadline_at,correlation_id,base_delay_ms,cap_delay_ms,backoff_multiplier) " +
            "VALUES (?,'TEST_WORK','{\"private\":\"do-not-forward\"}','EXHAUSTED',3,3,clock_timestamp(),?,500,5000,2)",
            id, UUID.randomUUID()) }
        return id
    }

    private fun inspect(source: UUID, generation: Int) {
        fixture.connection("core").use { connection -> connection.prepareStatement(
            "SELECT state,notification_state,payload::text,deadline_at=created_at,attempt_count,generation,lease_epoch," +
                "notification_summary::text,notification_summary->>'workId',notification_summary->>'runbook' " +
                "FROM durable_work WHERE notification_key=?").use {
            it.setString(1, "work:$source:generation:$generation"); it.executeQuery().use { rows ->
                assertTrue(rows.next()); assertEquals("DONE", rows.getString(1)); assertEquals("RECORDED", rows.getString(2))
                assertEquals("{}", rows.getString(3)); assertTrue(rows.getBoolean(4)); assertEquals(0, rows.getInt(5))
                assertEquals(0, rows.getInt(6)); assertEquals(0L, rows.getLong(7)); assertFalse(rows.getString(8).contains("do-not-forward"))
                assertEquals(source.toString(), rows.getString(9)); assertTrue(rows.getString(10).startsWith("docs/04-spoolin-techdoc.md#"))
                assertFalse(rows.next())
            }
        } }
    }

    private fun notificationCount(source: UUID): Int = fixture.connection("core").use { connection -> connection.prepareStatement(
        "SELECT count(*) FROM durable_work WHERE notification_key=?").use {
        it.setString(1, "work:$source:generation:0"); it.executeQuery().use { rows -> assertTrue(rows.next()); rows.getInt(1) }
    } }

    private fun eventFailure(enabled: Boolean) {
        fixture.connection("core", "migrator").use { connection -> connection.createStatement().use {
            if (enabled) {
                it.execute("CREATE FUNCTION appdata.reject_notification_fixture() RETURNS trigger LANGUAGE plpgsql AS " +
                    "\u0024\u0024 BEGIN IF NEW.action='WORK_EXHAUSTION_RECORDED' THEN RAISE EXCEPTION 'Fixture event write failure'; END IF; RETURN NEW; END; \u0024\u0024")
                it.execute("CREATE TRIGGER reject_notification_fixture BEFORE INSERT ON operational_event FOR EACH ROW EXECUTE FUNCTION appdata.reject_notification_fixture()")
            } else {
                it.execute("DROP TRIGGER reject_notification_fixture ON operational_event")
                it.execute("DROP FUNCTION appdata.reject_notification_fixture()")
            }
        } }
    }
}
