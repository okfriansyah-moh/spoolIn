package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.account.persistence.AccountInsert
import com.spoolin.core.features.account.persistence.AccountProjection
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.PasswordInput
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AccountIntegrationTest {
    private val password = " a synthetic password é "

    @Test
    fun storedAccountHasOnlyCiphertextAliasesAndOneWayCredentialWithSafeProjection() =
        CryptoFixture().use { fixture ->
            val id = UUID.randomUUID()
            val identity = IdentityRules().identity("O'Name canary UNION SELECT", "A+Keep.Dot@Example.org")
            val hash = Argon2Engine().hash(PasswordInput.exactBytes(password))
            assertInstanceOf(
                AccountInsert.Stored::class.java,
                fixture.accounts.insert(id, identity, hash, AccountRole.VIEWER),
            )
            fixture.owner.connection("core").use { connection ->
                val rows =
                    connection
                        .queryBound(
                            "SELECT row_to_json(a)::text AS row,pii_cipher,pii_nonce FROM appdata.user_account a WHERE id=?",
                            listOf(id),
                        ) {
                            Triple(it.getString("row"), it.getBytes("pii_cipher"), it.getBytes("pii_nonce"))
                        }.single()
                listOf(
                    identity.name,
                    identity.emailDisplay,
                    identity.emailNormalized,
                    password,
                ).forEach { assertFalse(rows.first.contains(it)) }
                assertEquals(12, rows.third.size)
                assertTrue(rows.second.size in 16..65552)
                assertEquals(
                    2,
                    connection
                        .queryBound(
                            "SELECT count(*) AS count FROM appdata.account_email_lookup WHERE account_id=?",
                            listOf(id),
                        ) {
                            it.getInt("count")
                        }.single(),
                )
            }
            val read = (fixture.accounts.identity(id) as ProtectedRead.Value).value
            assertEquals(identity.name, read.name)
            assertEquals(identity.emailDisplay, read.emailDisplay)
            assertEquals(identity.emailNormalized, read.emailNormalized)
            val credential = (fixture.accounts.credential(identity.emailNormalized) as ProtectedRead.Value).value
            assertTrue(Argon2Engine().verify(PasswordInput.exactBytes(password), credential.password).matched)
            assertFalse(
                ObjectMapper().writeValueAsString(AccountProjection(id, AccountRole.VIEWER, 0)).contains("password"),
            )
            assertFalse(ObjectMapper().writeValueAsString(credential).contains(hash.encoded))
            fixture.observe(
                "t005a1",
                "stored",
                mapOf(
                    "accounts" to 1,
                    "aliases" to 2,
                    "plaintextAbsent" to true,
                    "exactDecrypt" to true,
                    "rawPasswordAbsent" to true,
                ),
            )
        }

    @Test
    fun caseVariantRaceCommitsOneAccountAndRollsBackTheLosingAliases() =
        CryptoFixture().use { fixture ->
            val hash = Argon2Engine().hash(PasswordInput.exactBytes(password))
            val barrier = CyclicBarrier(2)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val results =
                    listOf("Race@example.org", "race@EXAMPLE.ORG")
                        .map { email ->
                            executor.submit<AccountInsert> {
                                val identity = IdentityRules().identity("Synthetic race", email)
                                barrier.await(10, TimeUnit.SECONDS)
                                fixture.accounts.insert(UUID.randomUUID(), identity, hash, AccountRole.VIEWER)
                            }
                        }.map { it.get(20, TimeUnit.SECONDS) }
                assertEquals(1, results.count { it is AccountInsert.Stored })
                assertEquals(1, results.count { it == AccountInsert.Conflict })
                val digest =
                    fixture.crypto.lookupDigest(
                        com.spoolin.core.features.crypto
                            .RegisteredKey("email-old", KeyPurpose.EMAIL_LOOKUP),
                        "race@example.org",
                    )
                val serverLogs = fixture.owner.postgres.logs
                val aliasLogged =
                    serverLogs.contains(
                        java.util.HexFormat
                            .of()
                            .formatHex(digest),
                    )
                val hashLogged = serverLogs.contains(hash.encoded)
                assertFalse(aliasLogged)
                assertFalse(hashLogged)

                assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
                assertEquals(2, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup"))
                assertEquals(
                    3,
                    fixture.scalar("SELECT next_nonce FROM appdata.crypto_key_version WHERE id='pii-write'"),
                )
                fixture.observe(
                    "t005a3",
                    "duplicate-race",
                    mapOf(
                        "attempts" to 2,
                        "stored" to 1,
                        "conflicts" to 1,
                        "aliases" to 2,
                        "consumedNonces" to 2,
                        "aliasInServerLogs" to aliasLogged,
                        "hashInServerLogs" to hashLogged,
                    ),
                )
            } finally {
                executor.shutdownNow()
            }
        }

    @Test
    fun readCompatibleAliasProtectsAnOldOnlyAccountDuringPartialBackfill() =
        CryptoFixture().use { fixture ->
            fixture.owner.sql(
                "core",
                "DELETE FROM appdata.crypto_key_version WHERE id='email-write'; UPDATE " +
                    "appdata.crypto_key_version SET state='WRITE' WHERE id='email-old'",
            )
            val hash = Argon2Engine().hash(PasswordInput.exactBytes(password))
            val id = UUID.randomUUID()
            val identity = IdentityRules().identity("Old synthetic account", "Old@Example.org")
            assertInstanceOf(
                AccountInsert.Stored::class.java,
                fixture.accounts.insert(id, identity, hash, AccountRole.VIEWER),
            )
            fixture.owner.sql("core", "UPDATE appdata.crypto_key_version SET state='READ' WHERE id='email-old'")
            fixture.register("email-write", KeyPurpose.EMAIL_LOOKUP, "WRITE")
            val duplicate = IdentityRules().identity("Different name", "old@example.org")
            assertEquals(
                AccountInsert.Conflict,
                fixture.accounts.insert(UUID.randomUUID(), duplicate, hash, AccountRole.VIEWER),
            )
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup"))
            assertEquals(id, (fixture.accounts.credential("old@example.org") as ProtectedRead.Value).value.id)
            assertEquals(ProtectedRead.Unavailable, fixture.accounts.credential("unknown@example.org"))
            fixture.observe(
                "t005a3",
                "old-only-alias",
                mapOf(
                    "oldOnlyAccounts" to 1,
                    "duplicateRejected" to true,
                    "unrelatedAccountCreated" to false,
                ),
            )
        }

    @Test
    fun tamperedCipherAndMissingReadKeyFailClosedWithoutChangingValidRows() =
        CryptoFixture().use { fixture ->
            val hash = Argon2Engine().hash(PasswordInput.exactBytes(password))
            val bad = UUID.randomUUID()
            val good = UUID.randomUUID()
            assertInstanceOf(
                AccountInsert.Stored::class.java,
                fixture.accounts.insert(
                    bad,
                    IdentityRules().identity("Bad canary", "bad@example.org"),
                    hash,
                    AccountRole.VIEWER,
                ),
            )
            assertInstanceOf(
                AccountInsert.Stored::class.java,
                fixture.accounts.insert(
                    good,
                    IdentityRules().identity("Good canary", "good@example.org"),
                    hash,
                    AccountRole.MAINTAINER,
                ),
            )
            val before = cipher(fixture, good)
            fixture.pool.connection.use {
                it.executeBound(
                    "UPDATE appdata.user_account SET pii_cipher=set_byte(pii_cipher,0,get_byte(pii_cipher,0)#1) WHERE id=?",
                    bad,
                )
            }
            assertEquals(ProtectedRead.Unavailable, fixture.accounts.identity(bad))
            assertEquals("Good canary", (fixture.accounts.identity(good) as ProtectedRead.Value).value.name)
            assertArrayEquals(before, cipher(fixture, good))
            assertEquals(listOf(bad), fixture.alerts)
            fixture.owner.sql("core", "UPDATE appdata.crypto_key_version SET state='RETIRED' WHERE id='pii-write'")
            assertEquals(ProtectedRead.Unavailable, fixture.accounts.identity(good))
            assertEquals(
                AccountInsert.Unavailable,
                fixture.accounts.insert(
                    UUID.randomUUID(),
                    IdentityRules().identity("Unavailable", "new@example.org"),
                    hash,
                    AccountRole.VIEWER,
                ),
            )
            assertEquals(2, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
            fixture.observe(
                "t005a1",
                "tamper",
                mapOf(
                    "tamperUnavailable" to true,
                    "alertRowOnly" to true,
                    "validCipherUnchanged" to true,
                    "retiredKeyUnavailable" to true,
                ),
            )
        }

    @Test
    fun passwordReplacementUsesCompareAndSetAndDoesNotRewriteIdentity() =
        CryptoFixture().use { fixture ->
            val first = Argon2Engine().hash(PasswordInput.exactBytes(password))
            val replacement = Argon2Engine().hash(PasswordInput.exactBytes(password))
            val id = UUID.randomUUID()
            assertInstanceOf(
                AccountInsert.Stored::class.java,
                fixture.accounts.insert(
                    id,
                    IdentityRules().identity("Rehash account", "rehash@example.org"),
                    first,
                    AccountRole.VIEWER,
                ),
            )
            val before = cipher(fixture, id)
            assertTrue(fixture.accounts.replacePassword(id, first, replacement))
            assertFalse(fixture.accounts.replacePassword(id, first, replacement))
            assertArrayEquals(before, cipher(fixture, id))
            assertEquals(1, fixture.scalar("SELECT version FROM appdata.user_account"))
            assertEquals(0, fixture.scalar("SELECT auth_epoch FROM appdata.user_account"))
            assertEquals(
                replacement.encoded,
                (fixture.accounts.credential("rehash@example.org") as ProtectedRead.Value).value.password.encoded,
            )
        }

    @Test
    fun postMigrationOwnersAndRuntimeForeignKeysSurviveMembershipRevocation() =
        CryptoFixture().use { fixture ->
            fixture.owner.connection("core").use { connection ->
                val owners =
                    connection.queryBound(
                        "SELECT tableowner FROM pg_tables WHERE schemaname='appdata' AND tablename IN " +
                            "('crypto_key_version','user_account','account_email_lookup')",
                    ) { it.getString("tableowner") }
                assertEquals(listOf("spoolin_core_owner", "spoolin_core_owner", "spoolin_core_owner"), owners)
                assertFalse(
                    connection
                        .queryBound("SELECT rolcanlogin FROM pg_roles WHERE rolname='spoolin_core_owner'") {
                            it.getBoolean("rolcanlogin")
                        }.single(),
                )
                assertFalse(
                    connection
                        .queryBound(
                            "SELECT pg_has_role('spoolin_core_migrator','spoolin_core_owner','MEMBER') AS member",
                        ) {
                            it.getBoolean("member")
                        }.single(),
                )
                assertThrows(
                    SQLException::class.java,
                ) { connection.executeBound("ALTER TABLE appdata.user_account ADD COLUMN forbidden integer") }
            }
            val hash = Argon2Engine().hash(PasswordInput.exactBytes(password))
            assertInstanceOf(
                AccountInsert.Stored::class.java,
                fixture.accounts.insert(
                    UUID.randomUUID(),
                    IdentityRules().identity("FK traversal", "fk@example.org"),
                    hash,
                    AccountRole.VIEWER,
                ),
            )
            fixture.observe(
                "t005a1",
                "owners",
                mapOf(
                    "owner" to "spoolin_core_owner",
                    "tables" to 3,
                    "ownerLogin" to false,
                    "migrationMembership" to false,
                    "runtimeDdlDenied" to true,
                    "bothForeignKeysTraversed" to true,
                ),
            )
        }

    @Test
    fun missingLookupMaterialAndWriteEpochCannotCreatePartialAccounts() =
        CryptoFixture().use { fixture ->
            val hash = Argon2Engine().hash(PasswordInput.exactBytes(password))
            val identity = IdentityRules().identity("Unavailable lookup", "unavailable@example.org")
            com.spoolin.core.features.crypto.MountedKeys(fixture.mounts.filterKeys { it != "email-old" }).use { partial ->
                val crypto =
                    com.spoolin.core.features.crypto.AccountCryptography(
                        fixture.registry,
                        partial,
                        fixture.allocator,
                        com.spoolin.core.features.crypto
                            .AeadCipher(),
                        fixture.alert,
                    )
                val accounts =
                    com.spoolin.core.features.account.persistence.ProtectedAccounts(
                        fixture.pool,
                        fixture.registry,
                        crypto,
                        com.spoolin.core.features.account.persistence
                            .AccountPayload(),
                        fixture.alert,
                    )
                assertEquals(AccountInsert.Unavailable, accounts.insert(UUID.randomUUID(), identity, hash, AccountRole.VIEWER))
            }
            fixture.owner.sql("core", "UPDATE appdata.crypto_key_version SET state='READ' WHERE id='email-write'")
            assertEquals(AccountInsert.Unavailable, fixture.accounts.insert(UUID.randomUUID(), identity, hash, AccountRole.VIEWER))
            fixture.owner.sql(
                "core",
                "UPDATE appdata.crypto_key_version SET state='WRITE' WHERE id='email-write'; REVOKE INSERT ON appdata.user_account FROM spoolin_core_runtime",
            )
            assertEquals(AccountInsert.Unavailable, fixture.accounts.insert(UUID.randomUUID(), identity, hash, AccountRole.VIEWER))
            assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
            assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup"))
            fixture.observe(
                "t005a1",
                "unavailable-lookup",
                mapOf(
                    "deniedAttempts" to 3,
                    "accountRows" to 0,
                    "aliasRows" to 0,
                    "safeUnavailable" to true,
                ),
            )
        }

    private fun cipher(
        fixture: CryptoFixture,
        id: UUID,
    ): ByteArray =
        fixture.owner.connection("core").use { connection ->
            connection
                .queryBound(
                    "SELECT pii_cipher FROM appdata.user_account WHERE id=?",
                    listOf(id),
                ) { it.getBytes("pii_cipher") }
                .single()
        }
}
