package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.account.persistence.AccountInsert
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.PasswordInput
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.features.restore.RestoreCheckpointCli
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Actual PostgreSQL and fresh JVM exercise the restore-only crypto epoch and revocation path. */
class RestoreEpochBoundaryTest {
    @Test
    fun `fresh restore epochs preserve protected identity revoke credentials and never consume old nonce counters`() {
        CryptoFixture("restore-epoch-success.json").use { fixture ->
            val account = account(fixture)
            val plan = plan(fixture)
            assertEquals(0, child(fixture, plan))
            assertEquals(1, fixture.scalar("SELECT auth_epoch FROM appdata.user_account"))
            assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.browser_session WHERE revoked_at IS NULL"))
            assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.mcp_credential WHERE revoked_at IS NULL"))
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup WHERE key_id LIKE 'restore-%'"))
            assertEquals(3, fixture.scalar("SELECT count(*) FROM appdata.crypto_key_version WHERE state='WRITE' AND id LIKE 'restore-%'"))
            val report = JsonMapper.builder().build().readTree(Files.readAllBytes(fixture.owner.root.resolve("restore-crypto-result.json")))
            assertTrue(report["freshWriteKeys"].asBoolean())
            assertTrue(report["oldNonceCountersUnchanged"].asBoolean())
            assertEquals(1, report["authenticatedRows"]["accounts"].asInt())
            assertEquals(0, child(fixture, plan))
            assertEquals(1, fixture.scalar("SELECT auth_epoch FROM appdata.user_account"))
            val hash =
                fixture.pool.connection.use {
                    it
                        .queryBound(
                            "SELECT password_hash FROM appdata.user_account WHERE id=?",
                            listOf(account),
                        ) { row -> row.getString(1) }
                        .single()
                }
            assertTrue(
                Argon2Engine()
                    .verify(
                        PasswordInput.exactBytes("Restore source password 123!"),
                        com.spoolin.core.features.crypto
                            .StoredPasswordHash(hash),
                    ).matched,
            )
        }
    }

    @Test
    fun `mismatched marker refuses restore verification without false success report`() {
        CryptoFixture("restore-epoch-refused.json").use { fixture ->
            account(fixture)
            val plan = plan(fixture)
            val marker = fixture.owner.root.resolve("restore-pending-validation")
            Files.writeString(marker, """{"checkpointSha256":"${"2".repeat(64)}","epoch":"${UUID.randomUUID()}"}""")
            assertEquals(4, child(fixture, plan))
            assertEquals(0, fixture.scalar("SELECT auth_epoch FROM appdata.user_account"))
            assertFalse(Files.exists(fixture.owner.root.resolve("restore-crypto-result.json")))
        }
    }

    @Test
    fun `damaged retained key fails authenticated source validation while old nonce counters remain unchanged`() {
        CryptoFixture("restore-epoch-damaged-key.json").use { fixture ->
            account(fixture)
            val plan = plan(fixture)
            val before = fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'")
            Files.write(fixture.mounts.getValue("pii-write").file, ByteArray(32) { 99 })
            assertEquals(4, child(fixture, plan))
            assertEquals(before, fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'"))
            assertFalse(Files.exists(fixture.owner.root.resolve("restore-crypto-result.json")))
            assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.browser_session WHERE revoked_at IS NULL"))
        }
    }

    private fun account(fixture: CryptoFixture): UUID {
        val id = UUID.randomUUID()
        val hash = Argon2Engine().hash(PasswordInput.exactBytes("Restore source password 123!"))
        val stored =
            fixture.accounts.insert(
                id,
                IdentityRules().identity("Restore canary", "restore@example.invalid"),
                hash,
                AccountRole.MAINTAINER,
            )
        assertTrue(stored is AccountInsert.Stored)
        fixture.pool.connection.use {
            it.executeBound(
                "INSERT INTO appdata.browser_session(token_hash,id,account_id,auth_epoch,csrf_hash,expires_at) " +
                    "VALUES (?,?,?,0,?,clock_timestamp()+INTERVAL '1 hour')",
                ByteArray(32) { 1 },
                UUID.randomUUID(),
                id,
                ByteArray(32) { 2 },
            )
            it.executeBound(
                "INSERT INTO appdata.mcp_credential(id,token_hash,account_id,client_kind,auth_epoch,scope,expires_at) " +
                    "VALUES (?,?,?,'CLAUDE',0,'movie:propose movie:approve',clock_timestamp()+INTERVAL '1 hour')",
                UUID.randomUUID(),
                ByteArray(32) { 3 },
                id,
            )
        }
        return id
    }

    private fun plan(fixture: CryptoFixture): Path {
        val directory = fixture.owner.root
        val mounts = fixture.mounts.toMutableMap()
        val targets =
            KeyPurpose.entries.associate { purpose ->
                val id = "restore-" + purpose.name.lowercase()
                val file = directory.resolve(id)
                Files.write(file, ByteArray(32).also { SecureRandom().nextBytes(it) })
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
                mounts[id] = KeyMount(purpose, file)
                purpose.name to id
            }
        val keyFile = directory.resolve("restore-keys.json")
        val mapper = JsonMapper.builder().build()
        protected(
            keyFile,
            mapper.writeValueAsString(
                mounts.mapValues { (_, mount) ->
                    mapOf("purpose" to mount.purpose.name, "file" to mount.file.toString())
                },
            ),
        )
        val counters =
            fixture.pool.connection.use {
                it
                    .queryBound(
                        "SELECT id,next_nonce FROM appdata.crypto_key_version",
                    ) { row -> row.getString(1) to row.getLong(2).toString() }
                    .toMap()
            }
        val epoch = UUID.randomUUID().toString()
        val checkpoint = "1".repeat(64)
        protected(
            directory.resolve("restore-pending-validation"),
            mapper.writeValueAsString(
                mapOf(
                    "checkpointSha256" to checkpoint,
                    "epoch" to epoch,
                ),
            ),
        )
        val value =
            mapOf(
                "checkpointSha256" to checkpoint,
                "epoch" to epoch,
                "keys" to keyFile.toString(),
                "originals" to KeyPurpose.entries.associate { it.name to fixture.registry.writeKey(it).id },
                "targets" to targets,
                "originalCounters" to counters,
            )
        return directory.resolve("restore-plan.json").also { protected(it, mapper.writeValueAsString(value)) }
    }

    private fun protected(
        path: Path,
        value: String,
    ) {
        Files.writeString(path, value)
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
    }

    private fun child(
        fixture: CryptoFixture,
        plan: Path,
    ): Int {
        val log = fixture.owner.root.resolve("restore-process.log")
        val builder =
            ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-cp",
                System.getProperty("crypto.test.classpath"),
                RestoreCheckpointCli::class.java.name,
                "--plan=$plan",
            )
        builder.environment().putAll(
            mapOf(
                "CORE_DB_URL" to fixture.owner.url("core"),
                "CORE_DB_USER" to "spoolin_core_runtime",
                "CORE_DB_PASSWORD_FILE" to
                    fixture.owner.passwords
                        .getValue("core-runtime")
                        .toString(),
            ),
        )
        val process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start()
        assertTrue(process.waitFor(60, TimeUnit.SECONDS))
        val output = Files.readString(log)
        assertFalse(output.contains("Restore source password"))
        assertFalse(output.contains("restore@example.invalid"))
        return process.exitValue()
    }
}
