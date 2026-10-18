package com.spoolin.integration.work.leases

import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.media.MigrationRunner
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.WorkRecoveryAction
import com.spoolin.support.work.WorkRecoveryCommit
import com.spoolin.support.work.WorkRecoveryOperations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class OperatorRecoveryBoundaryTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `verified owner-local input and pending receipt permit one fenced recovery without changing asset identity`() {
        fixture("operator-local-recovery") { pool ->
            val work = setup(pool)
            val before = requireNotNull(pool.connection.use { WorkRecoveryOperations.inspect(it, work.id) })
            assertEquals("EXHAUSTED", before.state)
            assertTrue(recover(pool, work))
            val after = requireNotNull(pool.connection.use { WorkRecoveryOperations.inspect(it, work.id) })
            assertEquals("READY", after.state)
            assertEquals(1, after.generation)
            assertEquals(before.leaseEpoch, after.leaseEpoch)
            assertEquals(before.maximumAttempts, after.maximumAttempts)
            assertEquals(3, count(pool, work.id))
            assertEquals(
                "PROCESSING",
                pool.connection.use {
                    it
                        .queryBound("SELECT state FROM appdata.asset WHERE id=?", listOf(work.asset)) { row ->
                            row.getString(1)
                        }.single()
                },
            )
            assertThrows(IllegalArgumentException::class.java) { recover(pool, work) }
        }
    }

    @Test
    fun `changed generation unknown handlers terminal receipts and input corruption refuse resets`() {
        fixture("operator-refused-recovery") { pool ->
            val changed = setup(pool)
            assertThrows(IllegalArgumentException::class.java) {
                pool.committed {
                    WorkRecoveryOperations.recover(
                        it,
                        WorkRecoveryAction(
                            changed.id,
                            1,
                            1,
                            WorkRecoveryCommit("local-uid:1001", "Review", UUID.randomUUID()),
                        ),
                        directory,
                    )
                }
            }
            val unsupported = setup(pool)
            pool.committed {
                it.executeBound("UPDATE appdata.durable_work SET kind='UNSUPPORTED' WHERE id=?", unsupported.id)
            }
            assertThrows(IllegalArgumentException::class.java) { recover(pool, unsupported) }
            val terminal = setup(pool)
            pool.committed {
                it.executeBound("UPDATE appdata.operation_receipt SET state='FAILED' WHERE id=?", terminal.operation)
            }
            assertThrows(IllegalArgumentException::class.java) { recover(pool, terminal) }
            val corrupt = setup(pool)
            Files.write(directory.resolve("quarantine/${corrupt.asset}"), byteArrayOf(4, 5, 6))
            assertThrows(IllegalArgumentException::class.java) { recover(pool, corrupt) }
            for (work in listOf(changed, unsupported, terminal, corrupt)) {
                assertEquals("EXHAUSTED", pool.connection.use { WorkRecoveryOperations.inspect(it, work.id) }?.state)
                assertEquals(2, count(pool, work.id))
            }
        }
    }

    @Test
    fun `recent uncertain processor cannot be reauthorized while its effect may still run`() {
        fixture("operator-undrained-recovery") { pool ->
            val work = setup(pool, true)
            assertThrows(IllegalArgumentException::class.java) { recover(pool, work) }
            assertEquals(2, count(pool, work.id))
            assertEquals("EXHAUSTED", pool.connection.use { WorkRecoveryOperations.inspect(it, work.id) }?.state)
        }
    }

    private data class Pending(
        val id: UUID,
        val asset: UUID,
        val operation: UUID,
    )

    private fun setup(
        pool: DataSource,
        recent: Boolean = false,
    ): Pending {
        val work = Pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        Files.createDirectories(directory.resolve("quarantine"))
        val bytes = byteArrayOf(1, 2, 3)
        Files.write(directory.resolve("quarantine/${work.asset}"), bytes)
        pool.committed { connection ->
            val now = connection.queryBound("SELECT clock_timestamp()") { it.getTimestamp(1).toInstant() }.single()
            connection.executeBound(
                "INSERT INTO appdata.operation_receipt(" +
                    "id,actor_scope,kind,request_hash,state,correlation_id,expires_at) " +
                    "VALUES (?,'SERVICE_TEST','UPLOAD',?,'PENDING',?,?)",
                work.operation,
                ByteArray(32),
                UUID.randomUUID(),
                Timestamp.from(now.plusSeconds(3600)),
            )
            connection.executeBound(
                "INSERT INTO appdata.durable_work(" +
                    "id,operation_id,kind,payload,state,attempt_count,max_attempts,lease_epoch," +
                    "deadline_at,correlation_id,base_delay_ms,cap_delay_ms,backoff_multiplier,last_error) " +
                    "VALUES (?,?,'MEDIA_PROCESS',?::jsonb,'EXHAUSTED',1,3,1,?,?,500,5000,2,'UNCERTAIN')",
                work.id,
                work.operation,
                """{"subjectId":"${work.asset}"}""",
                Timestamp.from(now.minusSeconds(1)),
                UUID.randomUUID(),
            )
            connection.executeBound(
                "INSERT INTO appdata.asset(" +
                    "id,upload_operation_id,actor_id,state,input_bytes,policy_revision,processing_work_id," +
                    "processing_staging_key,processing_input_hash,processing_created_at) " +
                    "VALUES (?,?,?,'PROCESSING',3,'test-policy',?,?,?,?)",
                work.asset,
                work.operation,
                UUID.randomUUID(),
                work.id,
                "quarantine/${work.asset}",
                MessageDigest.getInstance("SHA-256").digest(bytes),
                Timestamp.from(now.minusSeconds(20)),
            )
            attempts(connection, work, if (recent) now else now.minusSeconds(20))
        }
        return work
    }

    private fun attempts(
        connection: java.sql.Connection,
        work: Pending,
        time: Instant,
    ) {
        connection.executeBound(
            "INSERT INTO appdata.operational_event(" +
                "id,operation_id,actor_kind,action,subject_kind,subject_id,correlation_id," +
                "event_type,work_id,generation,attempt_no,lease_epoch,created_at) " +
                "VALUES (?,?,'SERVICE','WORK_ATTEMPT','WORK',?,?,'ATTEMPT_STARTED',?,0,1,1,?)",
            UUID.randomUUID(),
            work.operation,
            work.id,
            UUID.randomUUID(),
            work.id,
            Timestamp.from(time),
        )
        connection.executeBound(
            "INSERT INTO appdata.operational_event(" +
                "id,operation_id,actor_kind,action,subject_kind,subject_id,correlation_id," +
                "event_type,work_id,generation,attempt_no,lease_epoch,created_at,outcome,attempt_outcome) " +
                "VALUES (?,?,'SERVICE','WORK_ATTEMPT','WORK',?,?,'ATTEMPT_FINISHED',?,0,1,1,?,'FAILED','UNCERTAIN')",
            UUID.randomUUID(),
            work.operation,
            work.id,
            UUID.randomUUID(),
            work.id,
            Timestamp.from(time),
        )
    }

    private fun recover(
        pool: DataSource,
        work: Pending,
    ): Boolean =
        pool.committed {
            WorkRecoveryOperations.recover(
                it,
                WorkRecoveryAction(
                    work.id,
                    0,
                    1,
                    WorkRecoveryCommit("local-uid:1001", "Inspected local processing destination", UUID.randomUUID()),
                ),
                directory,
            )
        }

    private fun count(
        pool: DataSource,
        work: UUID,
    ): Int =
        pool.connection.use {
            it
                .queryBound("SELECT count(*) FROM appdata.operational_event WHERE work_id=?", listOf(work)) { row ->
                    row.getInt(1)
                }.single()
        }

    private fun fixture(
        name: String,
        block: (DataSource) -> Unit,
    ) {
        OwnerFixture("$name.json").start().use { fixture ->
            fixture.membership("media", "grant")
            fixture.pool("media", "migrator").use { MigrationRunner(it).migrate() }
            fixture.grants("media")
            fixture.membership("media", "revoke")
            fixture.pool("media").use(block)
        }
    }
}
