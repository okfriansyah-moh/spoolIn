package com.spoolin.core.operations.work

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.core.features.connector.ConnectorOperatorCli
import com.spoolin.core.features.connector.ConnectorOperatorCommand
import com.spoolin.core.features.connector.ConnectorTokenDigest
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.WorkRecoveryAction
import com.spoolin.support.work.WorkRecoveryCommit
import com.spoolin.support.work.WorkRecoveryOperations
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID

/** Synthetic account metadata isolates this operator boundary; it does not establish login or PII proof. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkRecoveryBoundaryTest {
    private lateinit var fixture: OwnerFixture

    @TempDir lateinit var directory: Path

    @BeforeAll
    fun start() {
        fixture = OwnerFixture("t007-core-operator-recovery.json").start()
        fixture.membership("core", "grant")
        fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("core")
        fixture.membership("core", "revoke")
    }

    @AfterAll
    fun stop() = fixture.close()

    @Test
    fun `known unaccepted delivery recovers once retaining attempt history and original identity`() {
        val id = notification()
        assertTrue(recover(id))
        fixture.connection("core").use { connection ->
            val work = requireNotNull(WorkRecoveryOperations.inspect(connection, id))
            assertEquals("READY", work.state)
            assertEquals(1, work.generation)
            assertEquals(0, work.attempts)
            assertEquals(1L, work.leaseEpoch)
            assertEquals(3, work.phaseHistory.size)
            assertEquals(
                "PENDING",
                connection
                    .queryBound(
                        "SELECT notification_state FROM durable_work WHERE id=?",
                        listOf(id),
                    ) { it.getString(1) }
                    .single(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) { recover(id) }
    }

    @Test
    fun `unknown delivery incomplete history unsafe error and stale fence remain exhausted`() {
        val unknown = notification()
        mutate("UPDATE durable_work SET notification_state='UNKNOWN' WHERE id=?", unknown)
        val incomplete = notification()
        mutate("DELETE FROM operational_event WHERE work_id=? AND event_type='ATTEMPT_FINISHED'", incomplete)
        val unsafe = notification()
        mutate("UPDATE operational_event SET error_code='NOTIFICATION_TIMEOUT' WHERE work_id=? AND event_type='ATTEMPT_FINISHED'", unsafe)
        val stale = notification()
        for (id in listOf(unknown, incomplete, unsafe)) {
            assertThrows(IllegalArgumentException::class.java) { recover(id) }
        }
        assertThrows(IllegalArgumentException::class.java) { recover(stale, 1) }
        for (id in listOf(unknown, incomplete, unsafe, stale)) {
            fixture.connection("core").use { assertEquals("EXHAUSTED", WorkRecoveryOperations.inspect(it, id)?.state) }
        }
    }

    @Test
    fun `connector bearer is file only hashed client bound and repeatedly revocable`() {
        val account = account("MAINTAINER")
        val output = protectedDirectory().resolve("connector-token")
        val issued = command(ConnectorOperatorCommand("issue", account, "CLAUDE", output, ttl = 60))
        val raw = Files.readString(output)
        assertTrue(raw.matches(Regex("[A-Za-z0-9_-]{43}")))
        assertFalse(issued.toString().contains(raw))
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(output))
        val id = UUID.fromString(issued.getValue("id").toString())
        fixture.connection("core").use { connection ->
            val saved =
                connection
                    .queryBound("SELECT token_hash,auth_epoch FROM mcp_credential WHERE id=?", listOf(id)) {
                        it.getBytes(1) to it.getLong(2)
                    }.single()
            assertArrayEquals(ConnectorTokenDigest.sha256(raw.toByteArray()), saved.first)
            assertEquals(7L, saved.second)
        }
        val revoke = ConnectorOperatorCommand("revoke", account, "CLAUDE", credential = id)
        command(revoke)
        val before = revokedAt(id)
        command(revoke)
        assertEquals(before, revokedAt(id))
        assertFalse(command(ConnectorOperatorCommand("list", account, "CHATGPT")).toString().contains(id.toString()))
        assertThrows(IllegalArgumentException::class.java) { command(revoke.copy(client = "CHATGPT")) }
    }

    @Test
    fun `connector issue refuses viewer and existing output without credentials or file loss`() {
        val output = protectedDirectory().resolve("connector-token")
        val viewer = account("VIEWER")
        assertThrows(IllegalArgumentException::class.java) {
            command(ConnectorOperatorCommand("issue", viewer, "CHATGPT", output))
        }
        assertFalse(Files.exists(output))
        Files.writeString(output, "existing protected operator material")
        val maintainer = account("MAINTAINER")
        assertThrows(java.nio.file.FileAlreadyExistsException::class.java) {
            command(ConnectorOperatorCommand("issue", maintainer, "CHATGPT", output))
        }
        assertEquals("existing protected operator material", Files.readString(output))
        fixture.connection("core").use { connection ->
            assertEquals(
                0,
                connection
                    .queryBound("SELECT count(*) FROM mcp_credential WHERE account_id=?", listOf(maintainer)) {
                        it.getInt(1)
                    }.single(),
            )
        }
    }

    private fun notification(): UUID {
        val id = UUID.randomUUID()
        fixture.connection("core").use { connection ->
            connection.executeBound(
                "INSERT INTO durable_work(id,kind,payload,state,attempt_count,max_attempts,lease_epoch,deadline_at," +
                    "correlation_id,base_delay_ms,cap_delay_ms,backoff_multiplier,notification_key,notification_mode," +
                    "notification_state,notification_summary) VALUES (?,'NOTIFICATION','{}','EXHAUSTED',1,3,1," +
                    "clock_timestamp()-interval '1 second',?,500,5000,2,?,'slack','FAILED','{}')",
                id,
                UUID.randomUUID(),
                "operator-test:$id",
            )
            event(connection, id, false)
            event(connection, id, true)
        }
        return id
    }

    private fun event(
        connection: Connection,
        id: UUID,
        finished: Boolean,
    ) {
        val type = if (finished) "ATTEMPT_FINISHED" else "ATTEMPT_STARTED"
        connection.executeBound(
            "INSERT INTO operational_event(id,actor_kind,action,subject_kind,subject_id,correlation_id,event_type," +
                "work_id,generation,attempt_no,lease_epoch,outcome,attempt_outcome,error_code) " +
                "VALUES (?,'SERVICE','WORK_ATTEMPT','WORK',?,?,?, ?,0,1,1,?,?,?)",
            UUID.randomUUID(),
            id,
            UUID.randomUUID(),
            type,
            id,
            if (finished) "FAILED" else null,
            if (finished) "TRANSIENT" else null,
            if (finished) "NOTIFICATION_CONNECT_FAILED" else null,
        )
    }

    private fun recover(
        id: UUID,
        generation: Int = 0,
    ): Boolean =
        fixture.connection("core").use { connection ->
            connection.autoCommit = false
            try {
                val result =
                    WorkRecoveryOperations.recover(
                        connection,
                        WorkRecoveryAction(
                            id,
                            generation,
                            1,
                            WorkRecoveryCommit("local-uid:1001", "Destination unaccepted evidence reviewed", UUID.randomUUID()),
                        ),
                        directory,
                    )
                connection.commit()
                result
            } finally {
                connection.rollback()
            }
        }

    private fun account(role: String): UUID =
        fixture.connection("core").use { connection ->
            val key = UUID.randomUUID().toString()
            val id = UUID.randomUUID()
            connection.executeBound("INSERT INTO crypto_key_version(id,purpose,state) VALUES (?,'PII','READ')", key)
            connection.executeBound(
                "INSERT INTO user_account(id,pii_key_id,pii_nonce,pii_cipher,password_hash,role,auth_epoch) VALUES (?,?,?,?,?,?,7)",
                id,
                key,
                ByteArray(12),
                ByteArray(16),
                "synthetic-operator-metadata",
                role,
            )
            id
        }

    private fun command(value: ConnectorOperatorCommand): Map<String, Any?> =
        fixture.connection("core").use {
            ConnectorOperatorCli.execute(it, value, "local-uid:${Files.getAttribute(directory, "unix:uid")}")
        }

    private fun protectedDirectory(): Path =
        Files.createTempDirectory(directory, "operator-").also {
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rwx------"))
        }

    private fun revokedAt(id: UUID) =
        fixture.connection("core").use { connection ->
            connection.queryBound("SELECT revoked_at FROM mcp_credential WHERE id=?", listOf(id)) { it.getTimestamp(1) }.single()
        }

    private fun mutate(
        sql: String,
        id: UUID,
    ) {
        // Explicit fixture corruption exercises recovery refusal; the runtime audit permissions stay unchanged.
        require(sql.count { it == '?' } == 1)
        if (sql.contains("operational_event")) {
            val denied =
                assertThrows(SQLException::class.java) {
                    fixture.connection("core").use { it.executeBound(sql, id) }
                }
            assertEquals("42501", denied.sqlState)
        }
        fixture.sql("core", "SET search_path=appdata; " + sql.replace("?", "'$id'::uuid"))
    }
}
