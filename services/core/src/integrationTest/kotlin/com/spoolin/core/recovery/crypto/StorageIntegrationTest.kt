package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.crypto.executeBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID

class StorageIntegrationTest {
    @Test
    fun registryConstraintsAndDefaultsMatchTheSourceDdl() =
        CryptoFixture().use { fixture ->
            fixture.owner.connection("core").use { connection ->
                failure("22001") {
                    connection.executeBound(
                        "INSERT INTO appdata.crypto_key_version(id,purpose,state) VALUES (?,'PII','READ')",
                        "x".repeat(65),
                    )
                }
                failure("23502") {
                    connection.executeBound(
                        "INSERT INTO appdata.crypto_key_version(purpose,state) VALUES ('PII','READ')",
                    )
                }
                failure("23514") {
                    connection.executeBound(
                        "INSERT INTO appdata.crypto_key_version(id,purpose,state) VALUES ('bad-purpose','OTHER','READ')",
                    )
                }
                failure("23514") {
                    connection.executeBound(
                        "INSERT INTO appdata.crypto_key_version(id,purpose,state) VALUES ('bad-state','PII','OTHER')",
                    )
                }
                listOf(-1L, 0L).forEach { next ->
                    failure("23514") {
                        connection.executeBound(
                            "INSERT INTO appdata.crypto_key_version(id,purpose,state,next_nonce) VALUES ('bad-counter','PII','READ',?)",
                            next,
                        )
                    }
                }
                failure("23505") {
                    connection.executeBound(
                        "INSERT INTO appdata.crypto_key_version(id,purpose,state) VALUES ('second-write','PII','WRITE')",
                    )
                }
                connection.executeBound(
                    "INSERT INTO appdata.crypto_key_version(id,purpose,state,created_at,retire_after) VALUES (?,'PII','READ',?,NULL)",
                    "x".repeat(64),
                    java.time.OffsetDateTime.parse("2026-10-15T12:34:56.123456Z"),
                )
            }
            assertEquals(1, fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id=repeat('x',64)"))
            assertEquals(
                123456,
                fixture.scalar(
                    "SELECT (extract(microseconds FROM created_at)::bigint % 1000000) FROM appdata.crypto_key_version WHERE id=repeat('x',64)",
                ),
            )
            fixture.observe(
                "t005a1",
                "registry-ddl",
                mapOf(
                    "physicalBoundariesChecked" to 8,
                    "partialWriteUniqueness" to true,
                    "defaultCounter" to 1,
                    "microseconds" to 123456,
                ),
            )
        }

    @Test
    fun protectedAccountAndAliasConstraintsRejectInvalidOctetsAndReferences() =
        CryptoFixture().use { fixture ->
            val id = UUID.randomUUID()
            fixture.owner.connection("core").use { connection ->
                failure("23503") { account(connection, UUID.randomUUID(), key = "missing") }
                listOf(11, 13).forEach { size ->
                    failure("23514") { account(connection, UUID.randomUUID(), nonce = ByteArray(size)) }
                }
                listOf(15, 65553).forEach { size ->
                    failure("23514") { account(connection, UUID.randomUUID(), cipher = ByteArray(size)) }
                }
                failure("22001") { account(connection, UUID.randomUUID(), hash = "x".repeat(513)) }
                failure("23514") { account(connection, UUID.randomUUID(), role = "OTHER") }
                failure(
                    "23502",
                ) { connection.executeBound("INSERT INTO appdata.user_account(id) VALUES (?)", UUID.randomUUID()) }
                account(connection, id)
                failure("23505") { account(connection, UUID.randomUUID()) }
                failure(
                    "23514",
                ) { connection.executeBound("UPDATE appdata.user_account SET auth_epoch=-1 WHERE id=?", id) }
                failure(
                    "23514",
                ) { connection.executeBound("UPDATE appdata.user_account SET version=-1 WHERE id=?", id) }
                listOf(31, 33).forEach { size -> failure("23514") { alias(connection, id, ByteArray(size)) } }
                failure("23503") { alias(connection, UUID.randomUUID(), ByteArray(32)) }
                alias(connection, id, ByteArray(32))
                failure("23505") { alias(connection, id, ByteArray(32)) }
                failure("23505") { alias(connection, id, ByteArray(32) { 1 }) }
                failure("23001") { connection.executeBound("DELETE FROM appdata.user_account WHERE id=?", id) }
                failure(
                    "42501",
                ) { connection.executeBound("DELETE FROM appdata.crypto_key_version WHERE id='pii-write'") }
            }
            fixture.owner.membership("core", "grant")
            try {
                fixture.owner.connection("core", "migrator").use { privileged ->
                    failure("23001") { privileged.executeBound("DELETE FROM appdata.crypto_key_version WHERE id='pii-write'") }
                }
            } finally {
                fixture.owner.membership("core", "revoke")
            }
            assertEquals(0, fixture.scalar("SELECT auth_epoch FROM appdata.user_account"))
            assertEquals(0, fixture.scalar("SELECT version FROM appdata.user_account"))
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup"))
            fixture.observe(
                "t005a1",
                "account-ddl",
                mapOf(
                    "accountRows" to 1,
                    "aliasRows" to 1,
                    "invalidRowsRetained" to 0,
                    "deleteRestricted" to true,
                ),
            )
        }

    private fun account(
        connection: Connection,
        id: UUID,
        key: String = "pii-write",
        nonce: ByteArray = ByteArray(12),
        cipher: ByteArray = ByteArray(16),
        hash: String = "synthetic-verifier",
        role: String = "VIEWER",
    ) {
        connection.executeBound(
            "INSERT INTO appdata.user_account(id,pii_key_id,pii_nonce,pii_cipher,password_hash,role) VALUES (?,?,?,?,?,?)",
            id,
            key,
            nonce,
            cipher,
            hash,
            role,
        )
    }

    private fun alias(
        connection: Connection,
        id: UUID,
        digest: ByteArray,
    ) {
        connection.executeBound(
            "INSERT INTO appdata.account_email_lookup(key_id,digest,account_id) VALUES ('email-write',?,?)",
            digest,
            id,
        )
    }

    private fun failure(
        state: String,
        action: () -> Unit,
    ) {
        assertEquals(state, assertThrows(SQLException::class.java, action).sqlState)
    }
}
