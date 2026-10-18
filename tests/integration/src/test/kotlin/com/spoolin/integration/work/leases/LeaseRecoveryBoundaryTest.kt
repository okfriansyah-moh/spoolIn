package com.spoolin.integration.work.leases

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkAttemptCompletion
import com.spoolin.support.work.WorkClaim
import com.spoolin.support.work.WorkRecoveryCommit
import com.spoolin.support.work.WorkSql
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

/** Real PostgreSQL lease/deadline fencing and controlled recovery regressions for TD7.3/7.7. */
class LeaseRecoveryBoundaryTest {
    @Test
    fun `kind-specific consumers cannot lease a ready external notification`() {
        fixture("kind-scope") { pool ->
            val now = databaseNow(pool)
            val media = insertWork(pool, now)
            val notification = insertWork(pool, now)
            pool.committed { connection ->
                connection.executeBound("UPDATE durable_work SET kind='MEDIA_PROCESS' WHERE id=?", media)
                connection.executeBound(
                    "UPDATE durable_work SET kind='NOTIFICATION',notification_key=?,notification_mode='slack'," +
                        "notification_state='PENDING',notification_summary='{}'::jsonb WHERE id=?",
                    notification.toString(),
                    notification,
                )
                val selected = WorkSql.selectForClaim(connection, now, allowedKinds = setOf("MEDIA_PROCESS"))
                assertEquals(listOf(media), selected.map { it.workId })
                assertNotNull(WorkSql.commitClaim(connection, selected.single(), UUID.randomUUID(), now))
            }
            val state =
                pool.connection.use { connection ->
                    connection
                        .queryBound(
                            "SELECT state,attempt_count FROM durable_work WHERE id=?",
                            listOf(notification),
                        ) { it.getString("state") to it.getInt("attempt_count") }
                        .single()
                }
            assertEquals("READY" to 0, state)
        }
    }

    @Test
    fun `expiry equality fences renewal and completion without recording a false finish`() {
        fixture("expiry") { pool ->
            for (column in listOf("lease_until", "deadline_at")) {
                val claim = claim(pool)
                val now = databaseNow(pool)
                pool.committed { connection ->
                    connection.executeBound(
                        "UPDATE durable_work SET $column=? WHERE id=?", Timestamp.from(now), claim.workId,
                    )
                }
                assertFalse(pool.committed { WorkSql.renewLease(it, claim, now) })
                assertFalse(complete(pool, claim, now))
                assertEquals(0, eventCount(pool, claim.workId, "ATTEMPT_FINISHED"))
                assertEquals("LEASED", pool.committed { WorkSql.leaseStatus(it, claim.workId) }?.state)
            }
        }
    }

    @Test
    fun `spent ready budget is excluded and stale candidate cannot bypass claim conditions`() {
        fixture("spent-budget") { pool ->
            val now = databaseNow(pool)
            val id = insertWork(pool, now)
            val candidate = pool.committed { WorkSql.selectForClaim(it, now, 1).single() }
            pool.committed { it.executeBound("UPDATE durable_work SET attempt_count=max_attempts WHERE id=?", id) }
            pool.committed {
                assertTrue(WorkSql.selectForClaim(it, now, 1).isEmpty())
                assertNull(WorkSql.commitClaim(it, candidate, UUID.randomUUID(), now))
            }
            assertEquals(0, eventCount(pool, id, "ATTEMPT_STARTED"))
        }
    }

    @Test
    fun `current epoch alone cannot finish another generation or attempt tuple`() {
        fixture("claim-tuple") { pool ->
            val claim = claim(pool)
            val now = databaseNow(pool)
            assertFalse(complete(pool, claim.copy(generation = claim.generation + 1), now))
            assertFalse(complete(pool, claim.copy(attemptCount = claim.attemptCount + 1), now))
            assertFalse(pool.committed { WorkSql.renewLease(it, claim.copy(generation = claim.generation + 1), now) })
            assertTrue(complete(pool, claim, now))
            assertEquals(1, eventCount(pool, claim.workId, "ATTEMPT_FINISHED"))
        }
    }

    @Test
    fun `completion without a matching started event cannot create success evidence`() {
        fixture("started-evidence") { pool ->
            val claim = claim(pool)
            pool.committed {
                it.executeBound("UPDATE durable_work SET lease_epoch=lease_epoch+1 WHERE id=?", claim.workId)
            }
            assertFalse(complete(pool, claim.copy(leaseEpoch = claim.leaseEpoch + 1), databaseNow(pool)))
            assertEquals(0, eventCount(pool, claim.workId, "ATTEMPT_FINISHED"))
        }
    }

    @Test
    fun `authorized recovery restores bounded claimability and retains original effect and attempts`() {
        fixture("recovery") { pool ->
            val first = claim(pool)
            assertTrue(
                pool.committed {
                    WorkSql.completeAttempt(
                        it,
                        first,
                        WorkAttemptCompletion(AttemptOutcome.PERMANENT, "PERMISSION_DENIED", "EXHAUSTED"),
                        databaseNow(pool),
                    )
                },
            )
            val now = databaseNow(pool)
            pool.committed {
                it.executeBound(
                    "UPDATE durable_work SET deadline_at=? WHERE id=?",
                    Timestamp.from(now.minusSeconds(1)),
                    first.workId,
                )
            }
            val recovery =
                WorkRecoveryCommit(
                    "verified-operator", "Destination outcome checked\bexplicit reason", UUID.randomUUID(),
                )
            assertTrue(pool.committed { WorkSql.recoverWork(it, first.workId, recovery, now) })
            assertFalse(pool.committed { WorkSql.recoverWork(it, first.workId, recovery, now) })
            val second =
                pool.committed {
                    WorkSql.commitClaim(
                        it, WorkSql.selectForClaim(it, databaseNow(pool), 1).single(),
                        UUID.randomUUID(), databaseNow(pool),
                    )
                }
            assertNotNull(second)
            assertEquals(first.workId, second?.workId)
            assertNotNull(first.operationId)
            assertEquals(first.operationId, second?.operationId)
            assertEquals(first.payload, second?.payload)
            assertEquals(first.maxAttempts, second?.maxAttempts)
            assertEquals(1, second?.generation)
            assertEquals(1, second?.attemptCount)
            assertEquals(now.plusSeconds(60), second?.deadlineAt)
            assertEquals(2, eventCount(pool, first.workId, "ATTEMPT_STARTED"))
            assertEquals(1, eventCount(pool, first.workId, "ATTEMPT_FINISHED"))
            assertEquals(1, eventCount(pool, first.workId, "RECOVERY"))
            val audit = recoveryChanges(pool, first.workId)
            assertEquals(listOf(recovery.reason, now.minusSeconds(1).toString(), now.plusSeconds(60).toString()), audit)
        }
    }

    @Test
    fun `row lock waiting cannot renew or complete an expired lease`() {
        fixture("lock-expiry") { pool ->
            for (renew in listOf(false, true)) {
                val claim = claim(pool)
                val now = databaseNow(pool)
                pool.committed {
                    it.executeBound(
                        "UPDATE durable_work SET lease_until=? WHERE id=?",
                        Timestamp.from(now.plusMillis(500)), claim.workId,
                    )
                }
                assertFalse(blockedMutation(pool, claim, now, renew))
                assertEquals(0, eventCount(pool, claim.workId, "ATTEMPT_FINISHED"))
            }
        }
    }

    @Test
    fun `concurrent authorized recoveries append one generation and one recovery event`() {
        fixture("concurrent-recovery") { pool ->
            val claim = claim(pool)
            assertTrue(
                pool.committed {
                    WorkSql.completeAttempt(
                        it,
                        claim,
                        WorkAttemptCompletion(AttemptOutcome.PERMANENT, nextState = "EXHAUSTED"),
                        databaseNow(pool),
                    )
                },
            )
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val attempts =
                    (1..2).map {
                        executor.submit<Boolean> { recoverAfterSignal(pool, claim.workId, start) }
                    }
                start.countDown()
                assertEquals(1, attempts.count { it.get(5, TimeUnit.SECONDS) })
                assertEquals(1, eventCount(pool, claim.workId, "RECOVERY"))
                assertEquals(1, eventCount(pool, claim.workId, "ATTEMPT_FINISHED"))
            } finally {
                executor.shutdownNow()
            }
        }
    }

    private fun recoveryChanges(
        pool: DataSource,
        work: UUID,
    ): List<String> =
        pool.committed {
            it
                .queryBound(
                    "SELECT changes->>'reason', changes->>'previousDeadlineAt', changes->>'deadlineAt' " +
                        "FROM operational_event WHERE work_id=? AND event_type='RECOVERY'",
                    listOf(work),
                ) { row ->
                    listOf(row.getString(1), row.getString(2), row.getString(3))
                }.single()
        }

    private fun recoverAfterSignal(
        pool: DataSource,
        workId: UUID,
        signal: CountDownLatch,
    ): Boolean {
        require(signal.await(2, TimeUnit.SECONDS))
        val commit = WorkRecoveryCommit("verified-operator", "Destination receipt checked", UUID.randomUUID())
        return pool.committed { WorkSql.recoverWork(it, workId, commit, databaseNow(pool)) }
    }

    private fun blockedMutation(
        pool: DataSource,
        claim: WorkClaim,
        now: Instant,
        renew: Boolean,
    ): Boolean {
        val executor = Executors.newSingleThreadExecutor()
        val started = CountDownLatch(1)
        val backend = AtomicInteger()
        try {
            pool.connection.use { blocker ->
                blocker.autoCommit = false
                blocker.queryBound("SELECT id FROM durable_work WHERE id=? FOR UPDATE", listOf(claim.workId)) { true }
                val finished =
                    executor.submit<Boolean> {
                        mutateOnBackend(pool, claim, now, renew, BackendSignal(backend, started))
                    }
                assertTrue(started.await(2, TimeUnit.SECONDS))
                awaitBlocked(pool, backend.get())
                blocker.queryBound("SELECT pg_sleep(0.75)") { true }
                blocker.commit()
                return finished.get(5, TimeUnit.SECONDS)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun mutateOnBackend(
        pool: DataSource,
        claim: WorkClaim,
        now: Instant,
        renew: Boolean,
        signal: BackendSignal,
    ): Boolean =
        pool.committed { connection ->
            signal.backend.set(connection.queryBound("SELECT pg_backend_pid()") { it.getInt(1) }.single())
            signal.started.countDown()
            if (renew) {
                WorkSql.renewLease(connection, claim, now)
            } else {
                WorkSql.completeAttempt(
                    connection,
                    claim,
                    WorkAttemptCompletion(AttemptOutcome.SUCCEEDED, nextState = "DONE"),
                    now,
                )
            }
        }

    private fun awaitBlocked(
        pool: DataSource,
        backend: Int,
    ) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (System.nanoTime() < deadline) {
            val blocked =
                pool.committed {
                    it.queryBound("SELECT cardinality(pg_blocking_pids(?)) > 0", listOf(backend)) { row ->
                        row.getBoolean(1)
                    }.single()
                }
            if (blocked) return
            Thread.sleep(10)
        }
        throw AssertionError("Completion did not reach the held PostgreSQL row lock")
    }

    private fun claim(pool: DataSource): WorkClaim {
        val now = databaseNow(pool)
        insertWork(pool, now)
        return pool.committed {
            requireNotNull(WorkSql.commitClaim(it, WorkSql.selectForClaim(it, now, 1).single(), UUID.randomUUID(), now))
        }
    }

    private fun insertWork(
        pool: DataSource,
        now: Instant,
    ): UUID {
        val id = UUID.randomUUID()
        val operationId = UUID.randomUUID()
        pool.committed {
            it.executeBound(
                "INSERT INTO operation_receipt(id,actor_scope,kind,request_hash,state,correlation_id,expires_at) " +
                    "VALUES (?,'SERVICE_TEST','TEST_EFFECT',?,'PENDING',?,?)",
                operationId,
                ByteArray(32),
                UUID.randomUUID(),
                Timestamp.from(now.plusSeconds(3600)),
            )
            it.executeBound(
                "INSERT INTO durable_work(" +
                    "id,operation_id,kind,payload,max_attempts,deadline_at,correlation_id,next_attempt_at," +
                    "base_delay_ms,cap_delay_ms,backoff_multiplier) " +
                    "VALUES (?,?,'SYNC_CATALOG',?::jsonb,3,?,?,?,500,5000,2)",
                id,
                operationId,
                """{"effectId":"$id"}""",
                Timestamp.from(now.plusSeconds(60)),
                UUID.randomUUID(),
                Timestamp.from(now.minusSeconds(1)),
            )
        }
        return id
    }

    private fun complete(
        pool: DataSource,
        claim: WorkClaim,
        now: Instant,
    ): Boolean =
        pool.committed {
            WorkSql.completeAttempt(it, claim, WorkAttemptCompletion(AttemptOutcome.SUCCEEDED, nextState = "DONE"), now)
        }

    private fun databaseNow(pool: DataSource): Instant =
        pool.committed {
            it.queryBound("SELECT clock_timestamp()") { row -> row.getTimestamp(1).toInstant() }.single()
        }

    private fun eventCount(
        pool: DataSource,
        workId: UUID,
        type: String,
    ): Int =
        pool.committed {
            it
                .queryBound(
                    "SELECT count(*) FROM operational_event WHERE work_id=? AND event_type=?",
                    listOf(workId, type),
                ) { row -> row.getInt(1) }
                .single()
        }

    private fun fixture(
        case: String,
        block: (DataSource) -> Unit,
    ) {
        OwnerFixture("lease-recovery-$case.json").start().use { fixture ->
            fixture.membership("core", "grant")
            fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
            fixture.grants("core")
            fixture.membership("core", "revoke")
            fixture.pool("core").use(block)
        }
    }
}

private data class BackendSignal(
    val backend: AtomicInteger,
    val started: CountDownLatch,
)
