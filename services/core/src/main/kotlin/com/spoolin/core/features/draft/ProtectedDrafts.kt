package com.spoolin.core.features.draft

import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.crypto.ProtectionUnavailable
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.features.crypto.readRows
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

private const val DRAFT_TTL_HOURS = 24L
private const val MAX_DRAFTS_PER_ACCOUNT = 20

/** Thrown when a draft CAS check fails (expected version does not match the current version). */
class DraftStaleVersion : IllegalStateException("STALE_VERSION")

/** Thrown when the account already has [MAX_DRAFTS_PER_ACCOUNT] live drafts and create is requested. */
class DraftQuotaExceeded : IllegalStateException("RESOURCE_EXHAUSTED")

/**
 * Projected view returned by [ProtectedDrafts.list].
 * Field naming follows the GraphQL Draft type; [values] is the decrypted JSON body.
 */
data class DraftProjection(
    val id: UUID,
    val targetKind: String,
    val targetId: UUID?,
    val baseVersion: Long?,
    val operationId: UUID?,
    val values: String,
    val version: Long,
    val expiresAt: Instant,
)

/**
 * Persistent store for encrypted account-owned recovery drafts.
 *
 * Lock order inside a committed transaction (§7.3.1): operation → account → draft.
 * Callers using [saveInTransaction] / [discardInTransaction] are already inside a receipt
 * transaction; they supply a live [Connection] and the account row lock is taken here.
 */
class ProtectedDrafts(
    private val dataSource: DataSource,
    private val crypto: DraftCryptography,
    private val payload: DraftPayload,
    private val alert: ProtectionAlert,
    private val clock: Clock = Clock.systemUTC(),
) {
    /**
     * Lists up to [MAX_DRAFTS_PER_ACCOUNT] non-expired drafts for [accountId], ordered by
     * most-recently-updated first.  Decryption failures are skipped with an alert.
     */
    fun list(accountId: UUID): List<DraftProjection> =
        dataSource.readRows { connection ->
            val rows =
                connection.queryBound(
                    "SELECT id,target_kind,target_id,base_version,operation_id," +
                        "body_key_id,body_nonce,body_cipher,version,expires_at " +
                        "FROM form_draft " +
                        "WHERE account_id=? AND expires_at > CURRENT_TIMESTAMP " +
                        "ORDER BY updated_at DESC " +
                        "LIMIT $MAX_DRAFTS_PER_ACCOUNT",
                    listOf(accountId),
                ) { it.toRow() }
            rows.mapNotNull { row -> decrypt(row) }
        }

    /**
     * Saves a draft inside an existing [connection] (already inside a receipt transaction).
     *
     * Steps:
     * 1. Lock the account row.
     * 2. For create (null [expectedVersion]): check quota ≤ 20, then insert.
     * 3. For update (non-null [expectedVersion]): CAS-update, throw [DraftStaleVersion] on mismatch.
     *
     * The [envelope] must already be encrypted (nonce reserved before the domain lock per §7.3.1).
     * Returns the committed [version] for use in the receipt result.
     *
     * @throws DraftStaleVersion   if expectedVersion is present but does not match.
     * @throws DraftQuotaExceeded  if the account already has 20 live drafts on create.
     */
    fun saveInTransaction(
        connection: Connection,
        accountId: UUID,
        draftId: UUID,
        expectedVersion: Long?,
        input: DraftInput,
        envelope: ProtectedEnvelope,
    ): Long {
        lockAccount(connection, accountId)
        val now = Timestamp.from(clock.instant())
        val expiresAt = Timestamp.from(clock.instant().plus(Duration.ofHours(DRAFT_TTL_HOURS)))
        return if (expectedVersion == null) {
            create(connection, accountId, draftId, input, envelope, now, expiresAt)
        } else {
            update(connection, accountId, draftId, expectedVersion, input, envelope, now, expiresAt)
        }
    }

    /**
     * Discards a draft inside an existing [connection] (already inside a receipt transaction).
     *
     * - Absent / other-account draft → `discarded = false` (without disclosing existence).
     * - Owned draft with matching [expectedVersion] → deleted, `discarded = true`.
     * - Owned draft with stale version → throws [DraftStaleVersion].
     */
    fun discardInTransaction(
        connection: Connection,
        accountId: UUID,
        draftId: UUID,
        expectedVersion: Long,
    ): Boolean {
        lockAccount(connection, accountId)
        val existing =
            connection
                .queryBound(
                    "SELECT version FROM form_draft WHERE id=? AND account_id=? FOR UPDATE",
                    listOf(draftId, accountId),
                ) { it.getLong("version") }
                .singleOrNull() ?: return false
        if (existing != expectedVersion) throw DraftStaleVersion()
        val deleted =
            connection.executeBound(
                "DELETE FROM form_draft WHERE id=? AND account_id=? AND version=?",
                draftId,
                accountId,
                expectedVersion,
            )
        return deleted == 1
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private fun create(
        connection: Connection,
        accountId: UUID,
        draftId: UUID,
        input: DraftInput,
        envelope: ProtectedEnvelope,
        now: Timestamp,
        expiresAt: Timestamp,
    ): Long {
        val count =
            connection
                .queryBound(
                    "SELECT COUNT(*) FROM form_draft " +
                        "WHERE account_id=? AND expires_at > CURRENT_TIMESTAMP",
                    listOf(accountId),
                ) { it.getLong(1) }
                .single()
        if (count >= MAX_DRAFTS_PER_ACCOUNT) throw DraftQuotaExceeded()
        connection.executeBound(
            "INSERT INTO form_draft " +
                "(id,account_id,target_kind,target_id,base_version,operation_id," +
                "policy_revision,body_key_id,body_nonce,body_cipher,version,updated_at,expires_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,0,?,?)",
            draftId,
            accountId,
            input.targetKind,
            input.targetId,
            input.baseVersion,
            input.operationId,
            input.policyRevision,
            envelope.keyId,
            envelope.nonce(),
            envelope.cipher(),
            now,
            expiresAt,
        )
        return 0L
    }

    private fun update(
        connection: Connection,
        accountId: UUID,
        draftId: UUID,
        expectedVersion: Long,
        input: DraftInput,
        envelope: ProtectedEnvelope,
        now: Timestamp,
        expiresAt: Timestamp,
    ): Long {
        val newVersion = expectedVersion + 1
        val updated =
            connection.executeBound(
                "UPDATE form_draft SET " +
                    "target_kind=?,target_id=?,base_version=?,operation_id=?," +
                    "policy_revision=?,body_key_id=?,body_nonce=?,body_cipher=?," +
                    "version=?,updated_at=?,expires_at=? " +
                    "WHERE id=? AND account_id=? AND version=?",
                input.targetKind,
                input.targetId,
                input.baseVersion,
                input.operationId,
                input.policyRevision,
                envelope.keyId,
                envelope.nonce(),
                envelope.cipher(),
                newVersion,
                now,
                expiresAt,
                draftId,
                accountId,
                expectedVersion,
            )
        if (updated != 1) throw DraftStaleVersion()
        return newVersion
    }

    private fun lockAccount(
        connection: Connection,
        accountId: UUID,
    ) {
        connection
            .queryBound(
                "SELECT id FROM user_account WHERE id=? FOR UPDATE",
                listOf(accountId),
            ) { it.getObject("id", UUID::class.java) }
            .singleOrNull() ?: throw ProtectionUnavailable()
    }

    private fun decrypt(row: DraftRow): DraftProjection? {
        val envelope =
            ProtectedEnvelope(1, row.bodyKeyId, row.bodyNonce, row.bodyCipher)
        return when (val decrypted = crypto.decrypt(row.id, envelope)) {
            is ProtectedRead.Value ->
                try {
                    DraftProjection(
                        id = row.id,
                        targetKind = row.targetKind,
                        targetId = row.targetId,
                        baseVersion = row.baseVersion,
                        operationId = row.operationId,
                        values = payload.decode(decrypted.value),
                        version = row.version,
                        expiresAt = row.expiresAt,
                    )
                } finally {
                    decrypted.value.fill(0)
                }
            ProtectedRead.Unavailable -> {
                alert.unavailable(row.id)
                null
            }
        }
    }

    private fun ResultSet.toRow(): DraftRow =
        DraftRow(
            id = getObject("id", UUID::class.java),
            targetKind = getString("target_kind"),
            targetId = getObject("target_id", UUID::class.java),
            baseVersion = getLong("base_version").takeUnless { wasNull() },
            operationId = getObject("operation_id", UUID::class.java),
            bodyKeyId = getString("body_key_id"),
            bodyNonce = getBytes("body_nonce"),
            bodyCipher = getBytes("body_cipher"),
            version = getLong("version"),
            expiresAt = getTimestamp("expires_at").toInstant(),
        )
}

private data class DraftRow(
    val id: UUID,
    val targetKind: String,
    val targetId: UUID?,
    val baseVersion: Long?,
    val operationId: UUID?,
    val bodyKeyId: String,
    val bodyNonce: ByteArray,
    val bodyCipher: ByteArray,
    val version: Long,
    val expiresAt: Instant,
)
