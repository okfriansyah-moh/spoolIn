package com.spoolin.catalog.features.proposal

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.security.MessageDigest
import java.util.UUID

private const val PENDING_QUOTA = 20

class ProposalBoundaryTest {
    @Test
    fun `receipt replay returns immutable stored expiry and hash without a new challenge`() {
        ProposalFixture().use { f ->
            val request = proposal()
            val first = propose(f, request)
            assertEquals("PENDING", f.receipt(UUID.fromString(request.context.operationId)).first)
            val row = f.row(first.proposalId, first.revision)
            assertEquals("A movie", first.values.title)
            assertTrue(
                MessageDigest.isEqual(
                    row.contentHash,
                    java.util.Base64
                        .getDecoder()
                        .decode(first.contentHash),
                ),
            )
            assertTrue(
                MessageDigest.isEqual(
                    row.challengeHash,
                    MessageDigest.getInstance("SHA-256").digest(first.uiChallenge.toByteArray()),
                ),
            )
            f.now = f.now.plusSeconds(1)
            val replay = propose(f, request)
            assertEquals(first.expiresAt, replay.expiresAt)
            assertEquals(first.values, replay.values)
            assertEquals(first.contentHash, replay.contentHash)
            assertFalse(replay.hasUiChallenge())
            val changed = request.toBuilder().setValues(request.values.toBuilder().setTitle("Changed")).build()
            assertEquals(Status.Code.ALREADY_EXISTS, failure { propose(f, changed) })
            assertEquals(1, f.count("SELECT count(*) FROM movie_proposal WHERE id=?", UUID.fromString(first.proposalId)))
        }
    }

    @Test
    fun `revision requires exact current owner connector client and revision and rolls back rejected receipts`() {
        ProposalFixture().use { f ->
            val initial = proposal()
            val first = propose(f, initial)
            val correction = proposal(first.proposalId, first.revision)
            val operation = UUID.fromString(correction.context.operationId)
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                failure {
                    f
                        .stub(
                            "ProposeMovie",
                            operation,
                            account = UUID.randomUUID(),
                            correlation = UUID.fromString(correction.context.correlationId),
                        ).proposeMovie(correction)
                },
            )
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                failure {
                    f
                        .stub(
                            "ProposeMovie",
                            operation,
                            credential = UUID.randomUUID(),
                            correlation = UUID.fromString(correction.context.correlationId),
                        ).proposeMovie(correction)
                },
            )
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                failure {
                    f
                        .stub(
                            "ProposeMovie",
                            operation,
                            client = "CHATGPT",
                            correlation = UUID.fromString(correction.context.correlationId),
                        ).proposeMovie(correction)
                },
            )
            assertEquals("REVIEW", f.row(first.proposalId, 1).state)
            assertEquals(0, f.count("SELECT count(*) FROM operation_receipt WHERE id=?", operation))
            val second = propose(f, correction)
            assertEquals(2, second.revision)
            assertTrue(first.uiChallenge != second.uiChallenge, "A corrected revision requires a fresh challenge")
            assertEquals("SUPERSEDED", f.row(first.proposalId, 1).state)
            assertEquals(
                Status.Code.FAILED_PRECONDITION,
                failure {
                    f.stub("ApproveAndCreate").approveAndCreate(approval(first))
                },
            )
            assertEquals(UUID.fromString(initial.context.operationId), f.row(first.proposalId, 2).operationId)
            val stale = proposal(first.proposalId, 1)
            assertEquals(Status.Code.ABORTED, failure { propose(f, stale) })
            assertEquals(0, f.count("SELECT count(*) FROM operation_receipt WHERE id=?", UUID.fromString(stale.context.operationId)))
        }
    }

    @Test
    fun `expired proposals free quota but cannot revise approve or replay and live policy denies new input`() {
        ProposalFixture().use { f ->
            val request = proposal()
            val first = propose(f, request)
            repeat(PENDING_QUOTA - 1) { propose(f, proposal()) }
            assertEquals(Status.Code.RESOURCE_EXHAUSTED, failure { propose(f, proposal()) })
            f.now = java.time.Instant.ofEpochSecond(first.expiresAt.seconds, first.expiresAt.nanos.toLong())
            assertTrue(propose(f, proposal()).hasUiChallenge())
            assertEquals(Status.Code.FAILED_PRECONDITION, failure { propose(f, request) })
            assertEquals(Status.Code.FAILED_PRECONDITION, failure { propose(f, proposal(first.proposalId, 1)) })
            assertEquals(
                Status.Code.FAILED_PRECONDITION,
                failure {
                    f.stub("ApproveAndCreate").approveAndCreate(approval(first))
                },
            )
            f.policy = "p2"
            val oldPolicy = proposal()
            assertEquals(Status.Code.FAILED_PRECONDITION, failure { propose(f, oldPolicy) })
            assertEquals(0, f.count("SELECT count(*) FROM operation_receipt WHERE id=?", UUID.fromString(oldPolicy.context.operationId)))
        }
    }

    @Test
    fun `revision insert failure restores original review and leaves no receipt or partial revision`() {
        ProposalFixture().use { f ->
            val first = propose(f, proposal())
            f.rejectSecondRevision()
            val correction = proposal(first.proposalId, 1)
            assertThrows(StatusRuntimeException::class.java) { propose(f, correction) }
            assertEquals("REVIEW", f.row(first.proposalId, 1).state)
            assertEquals("PENDING", f.receipt(f.row(first.proposalId, 1).operationId).first)
            assertEquals(1, f.count("SELECT count(*) FROM movie_proposal WHERE id=?", UUID.fromString(first.proposalId)))
            assertEquals(0, f.count("SELECT count(*) FROM operation_receipt WHERE id=?", UUID.fromString(correction.context.operationId)))
        }
    }

    @Test
    fun `approval retains identity binding after receipt completion and publishes only once`() {
        ProposalFixture().use { f ->
            val original = proposal()
            val first = propose(f, original)
            val input = approval(first)
            val created = f.stub("ApproveAndCreate").approveAndCreate(input)
            assertEquals("SUCCEEDED", created.state)
            assertEquals(original.context.operationId, created.operationId)
            assertEquals(created, f.stub("ApproveAndCreate").approveAndCreate(input))
            val originalOutcome = f.receipt(UUID.fromString(original.context.operationId))
            assertEquals("SUCCEEDED", originalOutcome.first)
            assertTrue(requireNotNull(originalOutcome.second).contains(created.resultId))
            val afterCreation = f.stub("RejectProposal").rejectProposal(input)
            assertEquals(created.resultId, afterCreation.resultId)
            assertEquals(originalOutcome, f.receipt(UUID.fromString(original.context.operationId)))
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                failure {
                    f.stub("ApproveAndCreate", credential = UUID.randomUUID()).approveAndCreate(input)
                },
            )
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                failure {
                    f.stub("ApproveAndCreate", account = UUID.randomUUID()).approveAndCreate(input)
                },
            )
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                failure {
                    f.stub("ApproveAndCreate", client = "CHATGPT").approveAndCreate(input)
                },
            )
            assertEquals(
                Status.Code.FAILED_PRECONDITION,
                failure {
                    f.stub("ApproveAndCreate").approveAndCreate(input.toBuilder().setChallenge("wrong").build())
                },
            )
            assertEquals(1, f.count("SELECT count(*) FROM title WHERE id=?", UUID.fromString(created.resultId)))
        }
    }

    @Test
    fun `rejection returns original failed receipt and repeated rejection cannot publish`() {
        ProposalFixture().use { f ->
            val rejectedRequest = proposal()
            val rejected = propose(f, rejectedRequest)
            val rejection = approval(rejected)
            val result = f.stub("RejectProposal").rejectProposal(rejection)
            assertEquals(rejectedRequest.context.operationId, result.operationId)
            assertEquals("FAILED", result.state)
            assertEquals("REJECTED", result.problem.code)
            assertEquals(result, f.stub("RejectProposal").rejectProposal(rejection))
            assertEquals("REJECTED", f.row(rejected.proposalId, rejected.revision).state)
            assertEquals("FAILED", f.receipt(UUID.fromString(rejectedRequest.context.operationId)).first)
            assertEquals(
                Status.Code.FAILED_PRECONDITION,
                failure {
                    f.stub("ApproveAndCreate").approveAndCreate(rejection)
                },
            )
        }
    }

    private fun propose(
        f: ProposalFixture,
        request: Spoolin.Proposal,
    ): Spoolin.ProposalRevision =
        f
            .stub(
                "ProposeMovie",
                UUID.fromString(request.context.operationId),
                correlation = UUID.fromString(request.context.correlationId),
            ).proposeMovie(request)

    private fun failure(action: () -> Unit): Status.Code = assertThrows(StatusRuntimeException::class.java) { action() }.status.code

    private fun proposal(
        id: String? = null,
        revision: Int? = null,
    ): Spoolin.Proposal {
        val values =
            Spoolin.MovieValues
                .newBuilder()
                .setTitle("  A movie  ")
                .setSynopsis("A synopsis")
                .setSynopsisLanguage("en")
                .addGenreIds(PROPOSAL_GENRE_ID)
                .setRelease(Spoolin.PartialDate.newBuilder().setYear(2020))
        return Spoolin.Proposal
            .newBuilder()
            .setValues(values)
            .setContext(
                Spoolin.Context
                    .newBuilder()
                    .setOperationId(UUID.randomUUID().toString())
                    .setCorrelationId(UUID.randomUUID().toString())
                    .setPolicyRevision("p1"),
            ).also { b ->
                id?.let(b::setProposalId)
                revision?.let(b::setExpectedRevision)
            }.build()
    }

    private fun approval(revision: Spoolin.ProposalRevision): Spoolin.ApprovalInput =
        Spoolin.ApprovalInput
            .newBuilder()
            .setProposalId(revision.proposalId)
            .setRevision(revision.revision)
            .setContentHash(revision.contentHash)
            .setChallenge(revision.uiChallenge.toStringUtf8())
            .build()
}
