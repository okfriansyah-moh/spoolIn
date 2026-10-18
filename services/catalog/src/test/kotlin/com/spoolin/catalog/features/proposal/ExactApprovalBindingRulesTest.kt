package com.spoolin.catalog.features.proposal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

class ExactApprovalBindingRulesTest {
    @Test
    fun verify_accepts_matching_challenge_and_actor() {
        val challenge = "abc"
        val challengeHash = MessageDigest.getInstance("SHA-256").digest(challenge.toByteArray())
        val contentHash = ByteArray(32) { 1 }
        val actor = UUID.randomUUID()
        val connector = UUID.randomUUID()
        val proposalId = UUID.randomUUID()
        val row =
            ProposalRow(
                id = proposalId,
                revision = 1,
                actorId = actor,
                connectorId = connector,
                clientKind = "CLAUDE",
                contentJson = """{"policy_revision":"policy-v1"}""",
                contentHash = contentHash,
                challengeHash = challengeHash,
                state = "REVIEW",
                operationId = UUID.randomUUID(),
                movieId = null,
                expiresAt = Instant.parse("2030-01-01T00:00:00Z"),
            )
        val binding =
            ExactApprovalBinding(
                proposalId = proposalId,
                revision = 1,
                actorId = actor,
                connectorId = connector,
                clientKind = "CLAUDE",
                contentHash = contentHash,
                challenge = challenge,
                policyRevision = "policy-v1",
                now = Instant.parse("2029-01-01T00:00:00Z"),
            )
        assertNull(binding.verify(row, challengeHash))
    }

    @Test
    fun verify_rejects_policy_revision_mismatch() {
        val binding =
            ExactApprovalBinding(
                proposalId = UUID.randomUUID(),
                revision = 1,
                actorId = UUID.randomUUID(),
                connectorId = UUID.randomUUID(),
                clientKind = "CLAUDE",
                contentHash = ByteArray(32),
                challenge = "x",
                policyRevision = "policy-v2",
                now = Instant.parse("2029-01-01T00:00:00Z"),
            )
        val row =
            ProposalRow(
                id = binding.proposalId,
                revision = 1,
                actorId = binding.actorId,
                connectorId = binding.connectorId,
                clientKind = "CLAUDE",
                contentJson = """{"policy_revision":"policy-v1"}""",
                contentHash = binding.contentHash,
                challengeHash =
                    java.security.MessageDigest
                        .getInstance("SHA-256")
                        .digest(binding.challenge.toByteArray()),
                state = "REVIEW",
                operationId = UUID.randomUUID(),
                movieId = null,
                expiresAt = Instant.parse("2030-01-01T00:00:00Z"),
            )
        assertEquals("POLICY_CHANGED", binding.verify(row, row.challengeHash)?.description)
    }

    @Test
    fun verify_rejects_wrong_revision() {
        val binding =
            ExactApprovalBinding(
                proposalId = UUID.randomUUID(),
                revision = 2,
                actorId = UUID.randomUUID(),
                connectorId = UUID.randomUUID(),
                clientKind = "CLAUDE",
                contentHash = ByteArray(32),
                challenge = "x",
                policyRevision = "",
                now = Instant.now(),
            )
        val row =
            ProposalRow(
                id = binding.proposalId,
                revision = 1,
                actorId = binding.actorId,
                connectorId = binding.connectorId,
                clientKind = "CLAUDE",
                contentJson = "{}",
                contentHash = binding.contentHash,
                challengeHash = ByteArray(32),
                state = "REVIEW",
                operationId = UUID.randomUUID(),
                movieId = null,
                expiresAt = Instant.parse("2030-01-01T00:00:00Z"),
            )
        assertEquals("REVISION_MISMATCH", binding.verify(row, row.challengeHash)?.description)
    }
}
