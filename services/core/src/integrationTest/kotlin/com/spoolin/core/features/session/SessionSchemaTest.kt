package com.spoolin.core.features.session

import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.recovery.crypto.CryptoFixture
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import java.sql.SQLException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionSchemaTest {
    private lateinit var fixture: CryptoFixture
    private val account = UUID.randomUUID()

    @BeforeAll
    fun prepare() {
        fixture = CryptoFixture("t008-session-schema.json")
        fixture.accounts.insert(
            account,
            IdentityRules().identity("A Person", "a@example.test"),
            StoredPasswordHash("unusable"),
            AccountRole.VIEWER,
        )
    }

    @AfterAll
    fun cleanup() = fixture.close()

    @TestFactory
    fun `nine source columns retain omission explicit null defaults and declared constraints`() =
        cases().map { row ->
            DynamicTest.dynamicTest(row.name) { probe(row) }
        }

    private fun cases(): List<SessionColumnProbe> {
        val base = values()
        val optional = setOf("revoked_at", "created_at", "last_seen_at")
        val missing =
            base.keys.flatMap { field ->
                listOf(
                    SessionColumnProbe("$field omitted", base - field, if (field in optional) null else "23502"),
                    SessionColumnProbe("$field SQL NULL", base + (field to "NULL"), if (field == "revoked_at") null else "23502"),
                )
            }
        val lengths =
            listOf("token_hash", "csrf_hash").flatMap { field ->
                listOf(31, 33).map { length ->
                    SessionColumnProbe("$field rejects $length bytes", base + (field to "decode(repeat('ab',$length),'hex')"), "23514")
                }
            }
        return missing + lengths +
            listOf(
                SessionColumnProbe("epoch rejects negative", base + ("auth_epoch" to "-1"), "23514"),
                SessionColumnProbe("epoch permits signed upper bound", base + ("auth_epoch" to "9223372036854775807")),
                SessionColumnProbe("missing account denies FK", base + ("account_id" to "'${UUID.randomUUID()}'"), "23503"),
            )
    }

    private fun probe(row: SessionColumnProbe) {
        fixture.pool.connection.use { connection ->
            connection.autoCommit = false
            try {
                val columns = row.values.keys.joinToString()
                val values = row.values.values.joinToString()
                val sql = "INSERT INTO appdata.browser_session ($columns) VALUES ($values)"
                executeProbe(connection, sql, row.failure)
            } finally {
                connection.rollback()
            }
        }
        assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.browser_session"))
    }

    private fun executeProbe(
        connection: java.sql.Connection,
        sql: String,
        failure: String?,
    ) {
        connection.createStatement().use { statement ->
            if (failure == null) {
                assertEquals(1, statement.executeUpdate(sql))
            } else {
                assertEquals(failure, assertThrows(SQLException::class.java) { statement.executeUpdate(sql) }.sqlState)
            }
        }
    }

    private fun values(): Map<String, String> =
        linkedMapOf(
            "token_hash" to "decode(repeat('ab',32),'hex')",
            "id" to "'${UUID.randomUUID()}'",
            "account_id" to "'$account'",
            "auth_epoch" to "0",
            "csrf_hash" to "decode(repeat('cd',32),'hex')",
            "created_at" to "'2026-10-20T00:00:00Z'",
            "last_seen_at" to "'2026-10-20T00:00:00Z'",
            "expires_at" to "'2026-10-20T12:00:00Z'",
            "revoked_at" to "NULL",
        )
}

private class SessionColumnProbe(
    val name: String,
    val values: Map<String, String>,
    val failure: String? = null,
)
