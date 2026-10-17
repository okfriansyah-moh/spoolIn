package com.spoolin.catalog.seed.persistence

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Seed membership primitives.
 *
 * T033 (seed population) uses this to record which manifest members
 * were applied or removed for a given seed_id. T020/T024 use tombstone
 * helpers during removals.
 *
 * This class owns the schema primitive and removal tombstone helper.
 * It does not initialize production seed data.
 */
class SeedMembership(
    private val dataSource: DataSource,
) {
    data class MemberRow(
        val seedId: String,
        val memberKey: String,
        val subjectKind: String,
        val subjectId: UUID,
        val contentHash: ByteArray,
        val appliedAt: Instant,
        val removedAt: Instant?,
    )

    fun isMember(
        seedId: String,
        memberKey: String,
    ): Boolean =
        dataSource.connection.use { conn ->
            conn
                .queryBound(
                    "SELECT 1 FROM seed_member WHERE seed_id = ? AND member_key = ? AND removed_at IS NULL",
                    listOf(seedId, memberKey),
                ) { true }
                .isNotEmpty()
        }

    fun findActive(seedId: String): List<MemberRow> =
        dataSource.connection.use { conn ->
            conn.queryBound(
                "SELECT seed_id, member_key, subject_kind, subject_id, content_hash, applied_at, removed_at " +
                    "FROM seed_member WHERE seed_id = ? AND removed_at IS NULL ORDER BY member_key",
                listOf(seedId),
            ) { rs ->
                MemberRow(
                    seedId = rs.getString("seed_id"),
                    memberKey = rs.getString("member_key"),
                    subjectKind = rs.getString("subject_kind"),
                    subjectId = rs.getObject("subject_id", UUID::class.java),
                    contentHash = rs.getBytes("content_hash"),
                    appliedAt = rs.getTimestamp("applied_at").toInstant(),
                    removedAt = rs.getTimestamp("removed_at")?.toInstant(),
                )
            }
        }

    /**
     * Tombstone a seed member (removal marker for T020/T024 lifecycle operations).
     * Idempotent — no-op if already removed.
     */
    fun markRemoved(
        seedId: String,
        memberKey: String,
        removedAt: Instant,
        conn: java.sql.Connection,
    ) {
        conn.executeBound(
            "UPDATE seed_member SET removed_at = ? WHERE seed_id = ? AND member_key = ? AND removed_at IS NULL",
            Timestamp.from(removedAt),
            seedId,
            memberKey,
        )
    }
}
