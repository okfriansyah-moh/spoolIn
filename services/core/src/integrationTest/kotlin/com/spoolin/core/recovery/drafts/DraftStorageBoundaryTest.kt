package com.spoolin.core.recovery.drafts

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.features.draft.DraftCryptography
import com.spoolin.core.features.draft.DraftInput
import com.spoolin.core.features.draft.DraftPayload
import com.spoolin.core.features.draft.DraftPersistCommand
import com.spoolin.core.features.draft.DraftQuotaExceeded
import com.spoolin.core.features.draft.DraftStaleVersion
import com.spoolin.core.features.draft.ProtectedDrafts
import com.spoolin.core.operations.events.ReceiptAction
import com.spoolin.core.operations.events.ReceiptAudit
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.SafeResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * ATDD-178/329/330/331/332 — Durable encrypted draft storage and receipts.
 *
 * Exercises real PostgreSQL18.6 commit/rollback boundaries for:
 * - ATDD-178: owner isolation, CAS, quota, decryption
 * - ATDD-329: save replay returns original outcome without second mutation
 * - ATDD-330: discard replay without reading deleted content
 * - ATDD-331: concurrent duplicate vs stale independent edit isolation
 * - ATDD-332: rollback of whole draft+receipt+audit boundary
 */
class DraftStorageBoundaryTest {
    @Test
    fun `owner save and discard round-trip commits correctly and replay is idempotent`() {
        prepare().use { env ->
            val accountId = insertAccount(env)
            val draftId = UUID.randomUUID()
            val operationId = UUID.randomUUID()

            // ATDD-329: first save creates draft at version 0
            val firstOutcome =
                env.receipts.executeAudited(
                    env.intent(operationId, accountId, draftId, null),
                    UUID.randomUUID(),
                    ReceiptAudit(ReceiptAction.DRAFT_SAVED, accountId, draftId),
                ) { connection ->
                    val v =
                        env.persistDraft(
                            connection,
                            accountId,
                            draftId,
                            null,
                            "MOVIE",
                            """{"title":"First"}""",
                        )
                    SafeResult(draftId, version = v)
                }
            assertEquals(draftId, firstOutcome.result?.resultId)
            assertEquals(0L, firstOutcome.result?.version)

            // Verify one operational_event ACTIVITY was created
            assertEquals(1, countEvents(env, operationId))

            // ATDD-329: replay returns same outcome without second mutation
            val replayOutcome =
                env.receipts.executeAudited(
                    env.intent(operationId, accountId, draftId, null),
                    UUID.randomUUID(),
                    ReceiptAudit(ReceiptAction.DRAFT_SAVED, accountId, draftId),
                ) { connection ->
                    SafeResult(draftId, version = 99L) // must never be executed
                }
            assertEquals(firstOutcome.id, replayOutcome.id)
            assertEquals(firstOutcome.result?.version, replayOutcome.result?.version)
            assertEquals(1, countEvents(env, operationId))

            // Update at version 0 → new version 1
            val updateId = UUID.randomUUID()
            val updateOutcome =
                env.receipts.executeAudited(
                    env.intent(updateId, accountId, draftId, 0L),
                    UUID.randomUUID(),
                    ReceiptAudit(ReceiptAction.DRAFT_SAVED, accountId, draftId),
                ) { connection ->
                    val v =
                        env.persistDraft(
                            connection,
                            accountId,
                            draftId,
                            0L,
                            "MOVIE",
                            """{"title":"Updated"}""",
                        )
                    SafeResult(draftId, version = v)
                }
            assertEquals(1L, updateOutcome.result?.version)

            // ATDD-178: drafts list returns the updated draft decrypted
            val listed = env.drafts.list(accountId)
            assertEquals(1, listed.size)
            assertEquals(draftId, listed.single().id)
            assertTrue(listed.single().values.contains("Updated"))

            // ATDD-330: discard deletes with correct CAS
            val discardId = UUID.randomUUID()
            val discardOutcome =
                env.receipts.executeAudited(
                    env.discardIntent(discardId, accountId, draftId, 1L),
                    UUID.randomUUID(),
                    ReceiptAudit(ReceiptAction.DRAFT_DISCARDED, accountId, draftId),
                ) { connection ->
                    val d = env.drafts.discardInTransaction(connection, accountId, draftId, 1L)
                    SafeResult(draftId, discarded = d)
                }
            assertTrue(discardOutcome.result?.discarded == true)

            // After discard, list is empty
            assertEquals(0, env.drafts.list(accountId).size)

            // ATDD-330: discard replay returns original discarded=true without deleting again
            val discardReplay =
                env.receipts.executeAudited(
                    env.discardIntent(discardId, accountId, draftId, 1L),
                    UUID.randomUUID(),
                    ReceiptAudit(ReceiptAction.DRAFT_DISCARDED, accountId, draftId),
                ) { _ ->
                    SafeResult(draftId, discarded = false) // must not execute
                }
            assertEquals(true, discardReplay.result?.discarded)
        }
    }

    @Test
    fun `absent or foreign account draft discard returns false without revealing existence`() {
        prepare().use { env ->
            val accountId = insertAccount(env)
            val otherAccount = insertAccount(env)
            val draftId = UUID.randomUUID()
            val operationId = UUID.randomUUID()

            // Create draft under accountId
            env.receipts.executeAudited(
                env.intent(operationId, accountId, draftId, null),
                UUID.randomUUID(),
                ReceiptAudit(ReceiptAction.DRAFT_SAVED, accountId, draftId),
            ) { connection ->
                env.persistDraft(connection, accountId, draftId, null, "PERSON", """{"name":"Alice"}""")
                SafeResult(draftId, version = 0L)
            }

            // otherAccount tries to discard accountId's draft → false (no disclosure)
            val foreignDiscard =
                env.receipts.executeAudited(
                    env.discardIntent(UUID.randomUUID(), otherAccount, draftId, 0L),
                    UUID.randomUUID(),
                    ReceiptAudit(ReceiptAction.DRAFT_DISCARDED, otherAccount, draftId),
                ) { connection ->
                    val d = env.drafts.discardInTransaction(connection, otherAccount, draftId, 0L)
                    SafeResult(draftId, discarded = d)
                }
            assertFalse(foreignDiscard.result?.discarded ?: true)

            // Original draft still exists for accountId
            assertEquals(1, env.drafts.list(accountId).size)
        }
    }

    @Test
    fun `stale version on update throws DraftStaleVersion`() {
        prepare().use { env ->
            val accountId = insertAccount(env)
            val draftId = UUID.randomUUID()
            val operationId = UUID.randomUUID()

            env.receipts.executeAudited(
                env.intent(operationId, accountId, draftId, null),
                UUID.randomUUID(),
                ReceiptAudit(ReceiptAction.DRAFT_SAVED, accountId, draftId),
            ) { connection ->
                env.persistDraft(connection, accountId, draftId, null, "MOVIE", """{"title":"Initial"}""")
                SafeResult(draftId, version = 0L)
            }

            // Attempt update with stale expected version = 5 (current is 0)
            assertThrows(DraftStaleVersion::class.java) {
                env.pool.connection.use { connection ->
                    connection.autoCommit = false
                    try {
                        env.persistDraft(connection, accountId, draftId, 5L, "MOVIE", """{"title":"Stale"}""")
                    } finally {
                        connection.rollback()
                    }
                }
            }
        }
    }

    @Test
    fun `quota of 20 drafts is enforced and the 21st create is rejected`() {
        prepare().use { env ->
            val accountId = insertAccount(env)
            repeat(20) { index ->
                val did = UUID.randomUUID()
                env.pool.connection.use { c ->
                    c.autoCommit = false
                    env.persistDraft(c, accountId, did, null, "PERSON", """{"name":"Person $index"}""")
                    c.commit()
                }
            }
            assertThrows(DraftQuotaExceeded::class.java) {
                val did = UUID.randomUUID()
                env.pool.connection.use { c ->
                    c.autoCommit = false
                    try {
                        env.persistDraft(c, accountId, did, null, "PERSON", """{"name":"Overflow"}""")
                        c.commit()
                    } catch (e: Exception) {
                        c.rollback()
                        throw e
                    }
                }
            }
        }
    }

    @Test
    fun `ATDD-332 rollback leaves no partial draft receipt or audit`() {
        prepare().use { env ->
            val accountId = insertAccount(env)
            val draftId = UUID.randomUUID()
            val operationId = UUID.randomUUID()

            // Force rollback inside the mutation lambda
            runCatching {
                env.receipts.executeAudited(
                    env.intent(operationId, accountId, draftId, null),
                    UUID.randomUUID(),
                    ReceiptAudit(ReceiptAction.DRAFT_SAVED, accountId, draftId),
                ) { _ ->
                    throw RuntimeException("Simulated pre-commit failure")
                }
            }

            // No receipt, no draft, no audit event committed
            assertEquals(0, env.drafts.list(accountId).size)
            assertEquals(0, countReceipts(env, operationId))
            assertEquals(0, countEvents(env, operationId))
        }
    }

    @Test
    fun `ATDD-331 concurrent identical saves commit once with original version`() {
        prepare().use { env ->
            val accountId = insertAccount(env)
            val draftId = UUID.randomUUID()
            val operationId = UUID.randomUUID()

            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val first =
                    executor.submit<com.spoolin.support.receipts.ReceiptOutcome> {
                        env.receipts.executeAudited(
                            env.intent(operationId, accountId, draftId, null),
                            UUID.randomUUID(),
                            ReceiptAudit(ReceiptAction.DRAFT_SAVED, accountId, draftId),
                        ) { connection ->
                            entered.countDown()
                            release.await(10, TimeUnit.SECONDS)
                            env.persistDraft(
                                connection,
                                accountId,
                                draftId,
                                null,
                                "MOVIE",
                                """{"title":"Concurrent"}""",
                            )
                            SafeResult(draftId, version = 0L)
                        }
                    }
                assertTrue(entered.await(10, TimeUnit.SECONDS))
                val second =
                    executor.submit<com.spoolin.support.receipts.ReceiptOutcome> {
                        env.receipts.executeAudited(
                            env.intent(operationId, accountId, draftId, null),
                            UUID.randomUUID(),
                            ReceiptAudit(ReceiptAction.DRAFT_SAVED, accountId, draftId),
                        ) { _ -> error("Concurrent replay should not re-execute") }
                    }
                release.countDown()
                val r1 = first.get(15, TimeUnit.SECONDS)
                val r2 = second.get(15, TimeUnit.SECONDS)
                // Both return the same original outcome
                assertEquals(r1.id, r2.id)
                assertEquals(r1.result?.version, r2.result?.version)
                // Only one draft row exists
                assertEquals(1, env.drafts.list(accountId).size)
            } finally {
                release.countDown()
                executor.shutdownNow()
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun prepare(): DraftEnv {
        val fixture = OwnerFixture("t014-draft-observations.json").start()
        fixture.membership("core", "grant")
        fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("core")
        fixture.membership("core", "revoke")
        val pool = fixture.pool("core")
        val mounts = mutableMapOf<String, KeyMount>()
        listOf("pii-write", "draft-write", "email-write").forEach { id ->
            val purpose =
                when {
                    id.startsWith("pii") -> KeyPurpose.PII
                    id.startsWith("draft") -> KeyPurpose.DRAFT
                    else -> KeyPurpose.EMAIL_LOOKUP
                }
            val file = fixture.root.resolve(id)
            Files.write(file, ByteArray(32).also { SecureRandom().nextBytes(it) })
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
            mounts[id] = KeyMount(purpose, file)
            registerKey(fixture, id, purpose, "WRITE")
        }
        val keys = MountedKeys(mounts)
        val registry = KeyRegistry(pool)
        val allocator = NonceAllocator(pool)
        val alerts = mutableListOf<UUID>()
        val alert = ProtectionAlert { alerts.add(it) }
        val crypto = DraftCryptography(registry, keys, allocator, AeadCipher())
        val payload = DraftPayload()
        val drafts = ProtectedDrafts(pool, crypto, payload, alert)
        val hashes = IntentHashes(mapOf("v1" to ByteArray(32) { 42 }), "v1")
        val receipts = OwnerReceipts(pool, hashes)
        return DraftEnv(fixture, pool, crypto, payload, drafts, receipts, hashes)
    }

    private fun registerKey(
        fixture: OwnerFixture,
        id: String,
        purpose: KeyPurpose,
        state: String,
    ) {
        fixture.pool("core").connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO appdata.crypto_key_version(id,purpose,state) VALUES (?,?,?) ON CONFLICT DO NOTHING",
            ).use { statement ->
                statement.setString(1, id)
                statement.setString(2, purpose.name)
                statement.setString(3, state)
                statement.executeUpdate()
            }
        }
    }

    private fun insertAccount(env: DraftEnv): UUID {
        val id = UUID.randomUUID()
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = ByteArray(32).also { SecureRandom().nextBytes(it) }
        env.pool.connection.use { connection ->
            connection.autoCommit = false
            connection.executeBound(
                "INSERT INTO appdata.user_account(id,pii_key_id,pii_nonce,pii_cipher,password_hash,role) " +
                    "VALUES (?,?,?,?,?,?)",
                id,
                "pii-write",
                nonce,
                cipher,
                "\$argon2id\$synthetic",
                "VIEWER",
            )
            connection.commit()
        }
        return id
    }

    private fun countReceipts(
        env: DraftEnv,
        operationId: UUID,
    ): Int =
        env.pool.connection.use { c ->
            c
                .queryBound("SELECT count(*) FROM appdata.operation_receipt WHERE id=?", listOf(operationId)) {
                    it.getInt(1)
                }.single()
        }

    private fun countEvents(
        env: DraftEnv,
        operationId: UUID,
    ): Int =
        env.pool.connection.use { c ->
            c
                .queryBound("SELECT count(*) FROM appdata.operational_event WHERE operation_id=?", listOf(operationId)) {
                    it.getInt(1)
                }.single()
        }

    private inner class DraftEnv(
        val fixture: OwnerFixture,
        val pool: javax.sql.DataSource,
        val crypto: DraftCryptography,
        val payload: DraftPayload,
        val drafts: ProtectedDrafts,
        val receipts: OwnerReceipts,
        val hashes: IntentHashes,
    ) : AutoCloseable {
        fun persistDraft(
            connection: java.sql.Connection,
            accountId: UUID,
            draftId: UUID,
            expectedVersion: Long?,
            kind: String,
            values: String,
        ): Long {
            val draftInput = input(kind, values)
            val envelope = crypto.encrypt(draftId, payload.encode(kind, values))
            return drafts.saveInTransaction(
                connection,
                DraftPersistCommand(
                    accountId = accountId,
                    draftId = draftId,
                    expectedVersion = expectedVersion,
                    input = draftInput,
                    envelope = envelope,
                ),
            )
        }

        fun input(
            kind: String,
            values: String,
        ) = DraftInput(
            targetKind = kind,
            targetId = null,
            baseVersion = null,
            values = values,
            operationId = null,
            policyRevision = "rev1",
        )

        fun intent(
            operationId: UUID,
            accountId: UUID,
            draftId: UUID,
            expectedVersion: Long?,
            values: String = """{"title":"Draft"}""",
        ) = OperationIntent(
            id = operationId,
            actorScope = "account:$accountId",
            kind = "DRAFT_SAVE",
            policyRevision = "rev1",
            fields =
                buildList {
                    add("draftId" to draftId.toString())
                    add("targetKind" to "MOVIE")
                    add("targetId" to null)
                    add("baseVersion" to null)
                    add("operationId" to null)
                    add("expectedVersion" to expectedVersion?.toString())
                    add("values" to values)
                },
        )

        fun discardIntent(
            operationId: UUID,
            accountId: UUID,
            draftId: UUID,
            expectedVersion: Long,
        ) = OperationIntent(
            id = operationId,
            actorScope = "account:$accountId",
            kind = "DRAFT_DISCARD",
            policyRevision = "discard",
            fields =
                listOf(
                    "draftId" to draftId.toString(),
                    "expectedVersion" to expectedVersion.toString(),
                ),
        )

        override fun close() {
            (pool as? AutoCloseable)?.close()
            fixture.close()
        }
    }
}
