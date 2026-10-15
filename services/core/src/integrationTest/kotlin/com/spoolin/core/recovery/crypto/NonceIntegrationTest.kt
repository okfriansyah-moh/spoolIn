package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionUnavailable
import com.spoolin.core.features.crypto.queryBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class NonceIntegrationTest {
    @Test
    fun concurrentEncryptionConsumesOneHundredAndOneCommittedDistinctCounters() =
        CryptoFixture().use { fixture ->
            fixture.backupCheckpoint("concurrent-initial").actionStarted()
            val ids = concurrentAccounts(fixture)
            fixture.backupCheckpoint("concurrent-committed")
            val envelopes =
                fixture.owner.connection("core").use { connection ->
                    connection.queryBound("SELECT pii_key_id,pii_nonce,pii_cipher FROM appdata.user_account") {
                        ProtectedEnvelope(1, it.getString("pii_key_id"), it.getBytes("pii_nonce"), it.getBytes("pii_cipher"))
                    }
                }
            assertEquals((1L..101L).toSet(), envelopes.map { ByteBuffer.wrap(it.nonce()).getLong(4) }.toSet())
            assertTrue(envelopes.all { it.nonce().take(4) == listOf<Byte>(0, 0, 0, 0) })
            assertEquals(101, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
            val next = fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'")
            assertEquals(102, next)
            assertEquals("Concurrent canary 1", (fixture.accounts.identity(ids.first()) as ProtectedRead.Value).value.name)
            assertEquals("Concurrent canary 101", (fixture.accounts.identity(ids.last()) as ProtectedRead.Value).value.name)
            fixture.observe(
                "atdd181",
                "concurrent",
                mapOf(
                    "allocations" to 101,
                    "persistedRows" to 101,
                    "distinctCounters" to 101,
                    "expectedNext" to 102,
                    "actualNext" to next,
                ),
            )
        }

    private fun concurrentAccounts(fixture: CryptoFixture): List<UUID> {
        val executor = Executors.newFixedThreadPool(8)
        val barrier = CyclicBarrier(8)
        val hash =
            com.spoolin.core.features.crypto
                .PasswordHasher(
                    com.spoolin.core.features.crypto
                        .Argon2Engine(),
                ).use {
                    it.hashNew("synthetic concurrent credential")
                }
        return try {
            (1..101)
                .map { index ->
                    executor.submit<UUID> {
                        if (index <= 8) barrier.await(10, TimeUnit.SECONDS)
                        val id = UUID.randomUUID()
                        val identity =
                            com.spoolin.core.features.account.persistence.IdentityRules().identity(
                                "Concurrent canary $index",
                                "nonce-$index@example.org",
                            )
                        val result =
                            fixture.accounts.insert(
                                id,
                                identity,
                                hash,
                                com.spoolin.core.features.account.persistence.AccountRole.VIEWER,
                            )
                        assertInstanceOf(com.spoolin.core.features.account.persistence.AccountInsert.Stored::class.java, result)
                        id
                    }
                }.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun domainRollbackCannotRefundACommittedReservation() =
        CryptoFixture().use { fixture ->
            fixture.backupCheckpoint("domain-rollback-initial").actionStarted()
            val first = fixture.allocator.reserve("pii-write", KeyPurpose.PII)
            fixture.owner.connection("core").use { domain ->
                domain.autoCommit = false
                domain
                    .prepareStatement(
                        "INSERT INTO appdata.user_account(id,pii_key_id,pii_nonce,pii_cipher,password_hash,role) " +
                            "VALUES (?,'pii-write',?,?,'synthetic-verifier','VIEWER')",
                    ).use {
                        it.setObject(1, UUID.randomUUID())
                        it.setBytes(2, first.bytes())
                        it.setBytes(3, ByteArray(16))
                        it.executeUpdate()
                    }
                assertEquals(
                    2,
                    fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'"),
                )
                domain.rollback()
            }
            assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
            val retry = fixture.allocator.reserve("pii-write", KeyPurpose.PII)
            assertEquals(2, ByteBuffer.wrap(retry.bytes()).getLong(4))
            fixture.observe(
                "atdd181",
                "domain-rollback",
                mapOf(
                    "rolledBackRows" to 0,
                    "consumedCounter" to 1,
                    "retryCounter" to 2,
                    "actualNext" to
                        fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'"),
                ),
            )
        }

    @Test
    fun abruptProcessLossAfterCommitRequiresAFreshCounterInTheNextProcess() =
        CryptoFixture().use { fixture ->
            fixture.backupCheckpoint("process-loss-initial").actionStarted()
            val config = fixture.owner.root.resolve("crash-config.json")
            Files.writeString(
                config,
                ObjectMapper().writeValueAsString(
                    mapOf(
                        "url" to fixture.owner.url("core"),
                        "user" to "spoolin_core_runtime",
                        "passwordFile" to
                            fixture.owner.passwords
                                .getValue("core-runtime")
                                .toString(),
                    ),
                ),
            )
            Files.setPosixFilePermissions(config, PosixFilePermissions.fromString("rw-------"))
            val first = fixture.owner.root.resolve("crash-nonce")
            val second = fixture.owner.root.resolve("retry-nonce")
            assertEquals(73, child(config.toString(), first.toString(), "crash"))
            assertEquals(2, fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'"))
            assertEquals(0, child(config.toString(), second.toString(), "restart"))
            assertEquals("000000000000000000000001", Files.readString(first))
            assertEquals("000000000000000000000002", Files.readString(second))
            assertEquals(3, fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'"))
            fixture.observe(
                "atdd181",
                "process-loss",
                mapOf(
                    "abruptExit" to 73,
                    "freshProcessExit" to 0,
                    "reservedBeforeLoss" to 1,
                    "retryCounter" to 2,
                    "actualNext" to 3,
                ),
            )
        }

    @Test
    fun rolledBackAndUncertainAllocationsNeverReturnAUsableNonce() =
        CryptoFixture().use { fixture ->
            fixture.backupCheckpoint("allocation-rollback-initial").actionStarted()
            assertThrows(
                SQLException::class.java,
            ) { NonceAllocator(failingCommit(fixture.pool, false)).reserve("pii-write", KeyPurpose.PII) }
            assertEquals(1, fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'"))
            assertThrows(
                SQLException::class.java,
            ) { NonceAllocator(failingCommit(fixture.pool, true)).reserve("pii-write", KeyPurpose.PII) }
            assertEquals(2, fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'"))
            assertEquals(2, ByteBuffer.wrap(fixture.allocator.reserve("pii-write", KeyPurpose.PII).bytes()).getLong(4))
            fixture.observe(
                "atdd181",
                "allocation-rollback",
                mapOf(
                    "rollbackNext" to 1,
                    "uncertainCommitNext" to 2,
                    "retryCounter" to 2,
                    "allocationReturnedOnFailure" to false,
                ),
            )
        }

    @Test
    fun allocatorRefusesTheMaximumCounterAndWrongPurposeUntilANewWriteKeyIsAvailable() =
        CryptoFixture().use { fixture ->
            fixture.backupCheckpoint("exhaustion-initial").actionStarted()
            fixture.owner.sql(
                "core",
                "UPDATE appdata.crypto_key_version SET next_nonce=9223372036854775806 WHERE id='pii-write'",
            )
            assertEquals(
                Long.MAX_VALUE - 1,
                ByteBuffer.wrap(fixture.allocator.reserve("pii-write", KeyPurpose.PII).bytes()).getLong(4),
            )
            assertThrows(
                ProtectionUnavailable::class.java,
            ) { fixture.crypto.encrypt(UUID.randomUUID(), byteArrayOf(1)) }
            assertEquals(
                Long.MAX_VALUE,
                fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'"),
            )
            assertThrows(ProtectionUnavailable::class.java) { fixture.allocator.reserve("pii-old", KeyPurpose.PII) }
            assertThrows(ProtectionUnavailable::class.java) { fixture.allocator.reserve("draft-write", KeyPurpose.PII) }
            val resumed = encryptionWithNewEpoch(fixture)
            assertEquals("pii-next", resumed.keyId)
            assertEquals(1, ByteBuffer.wrap(resumed.nonce()).getLong(4))
            fixture.observe(
                "atdd181",
                "exhaustion",
                mapOf(
                    "exhaustedNext" to Long.MAX_VALUE.toString(),
                    "maximumIssued" to false,
                    "newKeyCounter" to 1,
                ),
            )
        }

    private fun encryptionWithNewEpoch(fixture: CryptoFixture): ProtectedEnvelope {
        val file = fixture.owner.root.resolve("pii-next")
        Files.write(file, ByteArray(32).also { java.security.SecureRandom().nextBytes(it) })
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        fixture.owner.sql("core", "UPDATE appdata.crypto_key_version SET state='READ' WHERE id='pii-write'")
        fixture.register("pii-next", KeyPurpose.PII, "WRITE")
        val mounts =
            fixture.mounts + (
                "pii-next" to
                    com.spoolin.core.features.crypto
                        .KeyMount(KeyPurpose.PII, file)
            )
        return com.spoolin.core.features.crypto.MountedKeys(mounts).use { keys ->
            com.spoolin.core.features.crypto
                .AccountCryptography(
                    fixture.registry,
                    keys,
                    fixture.allocator,
                    com.spoolin.core.features.crypto
                        .AeadCipher(),
                    fixture.alert,
                ).encrypt(UUID.randomUUID(), byteArrayOf(2))
        }
    }

    private fun child(
        config: String,
        output: String,
        mode: String,
    ): Int {
        val java =
            java.nio.file.Path
                .of(System.getProperty("java.home"), "bin/java")
                .toString()
        val process =
            ProcessBuilder(
                java,
                "-cp",
                System.getProperty("crypto.test.classpath"),
                CrashAfterReservation::class.java.name,
                config,
                output,
                mode,
            ).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        assertTrue(process.waitFor(30, TimeUnit.SECONDS))
        return process.exitValue()
    }

    private fun failingCommit(
        source: DataSource,
        commitFirst: Boolean,
    ): DataSource =
        Proxy.newProxyInstance(
            DataSource::class.java.classLoader,
            arrayOf(DataSource::class.java),
        ) { _, method, arguments ->
            if (method.name == "getConnection") {
                val real = source.connection
                Proxy.newProxyInstance(
                    Connection::class.java.classLoader,
                    arrayOf(Connection::class.java),
                ) { _, call, args ->
                    if (call.name == "commit") {
                        if (commitFirst) real.commit()
                        throw SQLException("Injected commit outcome")
                    }
                    call.invoke(real, *(args ?: emptyArray()))
                }
            } else {
                method.invoke(source, *(arguments ?: emptyArray()))
            }
        } as DataSource
}
