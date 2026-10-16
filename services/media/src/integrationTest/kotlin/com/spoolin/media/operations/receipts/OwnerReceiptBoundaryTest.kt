package com.spoolin.media.operations.receipts

import com.spoolin.media.MigrationRunner
import com.spoolin.media.boundary.bootstrap.OwnerFixture
import com.spoolin.media.operations.maintenance.ReceiptExpiry
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OwnerReceiptBoundaryTest {
    @Test
    fun `concurrent intent commits once and replay after expiry never repeats effect`() {
        OwnerFixture("t010-media-observations.json").start().use { fixture ->
            prepare(fixture)
            fixture.pool("media").use { pool ->
                concurrentClaims(fixture, pool)
            }
        }
    }

    private fun concurrentClaims(
        fixture: OwnerFixture,
        pool: javax.sql.DataSource,
    ) {
        val now = Instant.parse("2026-10-20T00:00:00Z")
        val hashes = IntentHashes(mapOf("v1" to ByteArray(32) { 11 }), "v1")
        val receipts = OwnerReceipts(pool, hashes, Clock.fixed(now, ZoneOffset.UTC))
        val intent = OperationIntent(UUID.randomUUID(), "account:A", "WriteMovie", "p1", listOf("title" to "Movie"))
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first =
                executor.submit<com.spoolin.support.receipts.ReceiptOutcome> {
                    receipts.execute(
                        intent,
                        UUID.randomUUID(),
                        true,
                        listOf(
                            com.spoolin.support.receipts
                                .WorkRequest("followup", "SYNC", intent.id, now.plusSeconds(3600)),
                        ),
                    ) { connection ->
                        firstEffect(connection, entered, release)
                    }
                }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            val second =
                executor.submit<com.spoolin.support.receipts.ReceiptOutcome> {
                    receipts.execute(intent, UUID.randomUUID(), true) { error("Replay performed a second effect") }
                }
            release.countDown()
            assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))
            assertEquals(1, count(fixture, "receipt_domain"))
            assertEquals(1, count(fixture, "operation_receipt"))
            assertEquals(1, count(fixture, "operational_event"))
            assertEquals(1, count(fixture, "durable_work"))
            assertNull(receipts.find(intent.id, "account:B"))
            assertThrows(ReceiptConflict::class.java) {
                receipts.execute(intent.copy(policyRevision = "p2"), UUID.randomUUID(), true) { error("Conflict mutated") }
            }
            expireAndReplay(fixture, intent, hashes, now)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    private fun firstEffect(
        connection: java.sql.Connection,
        entered: CountDownLatch,
        release: CountDownLatch,
    ): SafeResult {
        entered.countDown()
        check(release.await(10, TimeUnit.SECONDS))
        connection.executeBound("INSERT INTO receipt_domain VALUES (1)")
        return SafeResult(UUID.fromString("b9c56a35-1651-4cdf-a355-d9f790c30d29"), version = 0)
    }

    @Test
    fun `failed domain mutation rolls back receipt and audit atomically`() {
        OwnerFixture("t010-media-rollback.json").start().use { fixture ->
            prepare(fixture)
            fixture.pool("media").use { pool ->
                rollbackClaims(fixture, pool)
            }
        }
    }

    private fun rollbackClaims(
        fixture: OwnerFixture,
        pool: javax.sql.DataSource,
    ) {
        val receipts = OwnerReceipts(pool, IntentHashes(mapOf("v1" to ByteArray(32) { 12 }), "v1"))
        val intent = OperationIntent(UUID.randomUUID(), "account:A", "WriteMovie", "p1", emptyList())
        assertThrows(IllegalStateException::class.java) {
            receipts.execute(intent, UUID.randomUUID(), true) { connection ->
                connection.createStatement().use { it.executeUpdate("INSERT INTO receipt_domain VALUES (1)") }
                error("Synthetic pre-commit failure")
            }
        }
        listOf("receipt_domain", "operation_receipt", "operational_event").forEach { assertEquals(0, count(fixture, it)) }
    }

    private fun prepare(fixture: OwnerFixture) {
        fixture.membership("media", "grant")
        fixture.pool("media", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("media")
        fixture.pool("media", "migrator").use { pool ->
            pool.connection.use { connection ->
                connection.executeBound("CREATE TABLE receipt_domain(value integer PRIMARY KEY)")
                connection.executeBound("GRANT SELECT,INSERT,DELETE ON receipt_domain TO spoolin_media_runtime")
            }
        }
        fixture.membership("media", "revoke")
    }

    private fun count(
        fixture: OwnerFixture,
        table: String,
    ): Int =
        fixture.connection("media").use { connection ->
            connection.queryBound("SELECT count(*) FROM $table", emptyList()) { it.getInt(1) }.single()
        }

    private fun expireAndReplay(
        fixture: OwnerFixture,
        intent: OperationIntent,
        hashes: IntentHashes,
        now: Instant,
    ) {
        fixture.pool("media").use { pool ->
            val clock = Clock.fixed(now.plusSeconds(604800), ZoneOffset.UTC)
            assertEquals(1, ReceiptExpiry(pool, clock).purgeBodies())
            pool.connection.use { it.executeBound("DELETE FROM receipt_domain") }
            val expired =
                OwnerReceipts(pool, hashes, clock).execute(intent, UUID.randomUUID(), true) {
                    error("Expired receipt repeated effect")
                }
            assertTrue(expired.resultExpired)
            assertEquals("RESULT_EXPIRED", expired.errorCode)
            assertNull(expired.result)
            assertEquals(0, count(fixture, "receipt_domain"))
            assertEquals(1, count(fixture, "operation_receipt"))
        }
    }
}
