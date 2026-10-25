package com.spoolin.catalog.unit

import com.spoolin.catalog.features.movie.MovieWrites
import com.spoolin.catalog.features.proposal.DeterministicContentSerializer
import com.spoolin.catalog.features.proposal.ProposalContent
import com.spoolin.catalog.features.proposal.ProposalDraft
import com.spoolin.catalog.features.proposal.ProposalWrites
import com.spoolin.support.receipts.OperationIntent
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Instant
import java.util.Base64
import java.util.UUID

class ProposalCommandRulesTest {
    private val policy =
        com.spoolin.catalog.boundary.policy.CatalogWritePolicy
            .environment()
            .revision
    private val content =
        mapOf(
            "policy_revision" to policy,
            "values" to
                mapOf(
                    "title" to "River",
                    "synopsis" to "Journey",
                    "synopsis_language" to "en",
                    "genre_ids" to listOf(unitAsset.toString()),
                    "release" to mapOf("year" to 2024),
                ),
        )
    private val digest = DeterministicContentSerializer.digest(content)
    private val challenge = MessageDigest.getInstance("SHA-256").digest("challenge".toByteArray())

    private fun row(
        state: String = "REVIEW",
        expires: Instant = Instant.parse("2100-01-01T00:00:00Z"),
    ) = listOf(
        mapOf(
            "id" to unitTarget,
            "revision" to 1,
            "actor_id" to unitActor,
            "connector_id" to unitCredential,
            "client_kind" to "CLAUDE",
            "content_json" to DeterministicContentSerializer.json(content),
            "content_hash" to digest,
            "challenge_hash" to challenge,
            "state" to state,
            "operation_id" to unitOperation,
            "movie_id" to null,
            "expires_at" to Timestamp.from(expires),
        ),
    )

    private fun service(db: JdbcRows) =
        ProposalWrites(db.source, ownerReceipts(db), MovieWrites(ownerReceipts(db), unitClock), clock = unitClock, livePolicyRevision = {
            policy
        })

    private fun approval() =
        Spoolin.ApprovalInput
            .newBuilder()
            .setProposalId(unitTarget.toString())
            .setRevision(1)
            .setChallenge("challenge")
            .setContentHash(Base64.getEncoder().encodeToString(digest))
            .build()

    @Test fun `browser reads rotate challenge while connector reads only expose review content`() {
        for (client in listOf(null, "CLAUDE")) {
            withMaintainer("GetMovieProposal", client) {
                val db = JdbcRows(row(), row())
                val request =
                    Spoolin.MovieProposalRequest
                        .newBuilder()
                        .setProposalId(unitTarget.toString())
                        .build()
                val latest = RpcResult<Spoolin.ProposalRevision>()
                service(db).read(request, latest)
                assertEquals("River", latest.value().values.title)
                assertEquals(client == null, !latest.value().uiChallenge.isEmpty)
                val exact = RpcResult<Spoolin.ProposalRevision>()
                service(db).read(request.toBuilder().setRevision(1).build(), exact)
                assertEquals(1, exact.value().revision)
                assertEquals(client == null, db.writes.isNotEmpty())
            }
        }
    }

    @Test fun `proposal queue validates limits ids and projects a newly challenged review`() =
        withMaintainer("ListMovieProposals") {
            val db = JdbcRows(listOf(mapOf("id" to unitTarget)), row(), emptyList())
            val request =
                Spoolin.MovieProposalQueueRequest
                    .newBuilder()
                    .addProposalIds(unitTarget.toString())
                    .setLimit(1)
                    .build()
            val first = RpcResult<Spoolin.MovieProposalQueue>()
            service(db).list(request, first)
            assertEquals(
                unitTarget.toString(),
                first
                    .value()
                    .proposalsList
                    .single()
                    .proposalId,
            )
            assertFalse(
                first
                    .value()
                    .proposalsList
                    .single()
                    .uiChallenge.isEmpty,
            )
            val empty = RpcResult<Spoolin.MovieProposalQueue>()
            service(db).list(Spoolin.MovieProposalQueueRequest.getDefaultInstance(), empty)
            assertEquals(0, empty.value().proposalsCount)
            for (invalid in listOf(
                request.toBuilder().setLimit(21).build(),
                request
                    .toBuilder()
                    .clearProposalIds()
                    .addProposalIds("bad")
                    .build(),
            )) {
                val result = RpcResult<Spoolin.MovieProposalQueue>()
                service(JdbcRows()).list(invalid, result)
                assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(result.failure).code)
            }
        }

    @Test fun `approval and rejection require exact admitted method and valid digest bytes`() {
        withMaintainer {
            val result = RpcResult<Spoolin.Outcome>()
            service(JdbcRows()).approve(approval(), result)
            assertEquals("MODEL_CONSENT_DENIED", Status.fromThrowable(result.failure).description)
        }
        for (method in listOf("ApproveAndCreate", "RejectProposal")) {
            withMaintainer(method) {
                for (hash in listOf("bad-base64!", Base64.getEncoder().encodeToString(ByteArray(3)))) {
                    val result = RpcResult<Spoolin.Outcome>()
                    val request = approval().toBuilder().setContentHash(hash).build()
                    if (method ==
                        "ApproveAndCreate"
                    ) {
                        service(JdbcRows()).approve(request, result)
                    } else {
                        service(JdbcRows()).reject(request, result)
                    }
                    assertEquals("CONTENT_HASH_INVALID", Status.fromThrowable(result.failure).description)
                }
            }
        }
    }

    @Test fun `approval refuses wrong challenge changed digest expired or nonreviewable revision`() =
        withMaintainer("ApproveAndCreate") {
            for ((request, stored, code) in listOf(
                Triple(approval().toBuilder().setChallenge("wrong").build(), row(), "CHALLENGE_MISMATCH"),
                Triple(approval().toBuilder().clearChallenge().build(), row(), "CHALLENGE_REQUIRED"),
                Triple(
                    approval().toBuilder().setContentHash(Base64.getEncoder().encodeToString(ByteArray(32))).build(),
                    row(),
                    "CONTENT_HASH_MISMATCH",
                ),
                Triple(approval(), row(expires = unitNow), "PROPOSAL_EXPIRED"),
                Triple(approval(), row("SUPERSEDED"), "PROPOSAL_NOT_REVIEWABLE"),
            )) {
                val db = JdbcRows(row(), stored)
                val result = RpcResult<Spoolin.Outcome>()
                service(db).approve(request, result)
                assertEquals(code, Status.fromThrowable(result.failure).description)
                assertTrue(db.writes.isEmpty())
            }
        }

    @Test fun `approved review creates movie completes original receipt and returns its safe result`() =
        withMaintainer("ApproveAndCreate") {
            val operation = UUID.nameUUIDFromBytes("approve:$unitTarget:1".toByteArray())
            val intent =
                OperationIntent(
                    operation,
                    "account:$unitActor",
                    "ApproveAndCreate",
                    policy,
                    listOf(
                        "proposalId" to unitTarget.toString(),
                        "revision" to "1",
                        "connectorId" to unitCredential.toString(),
                        "clientKind" to null,
                        "contentHash" to Base64.getEncoder().encodeToString(digest),
                    ),
                )
            val completed = receiptRow(intent, "SUCCEEDED", """{"resultId":"$unitAsset","version":"0"}""")
            val originalIntent = OperationIntent(unitOperation, "account:$unitActor", "ProposeMovie", policy, emptyList())
            val original = receiptRow(originalIntent, "SUCCEEDED", """{"resultId":"$unitAsset","version":"0"}""")
            val db =
                JdbcRows(
                    row(),
                    row(),
                    listOf(mapOf("lock" to 1)),
                    listOf(receiptRow(intent)),
                    row(),
                    listOf(mapOf("count" to 1)),
                    listOf(mapOf("id" to unitAsset)),
                    listOf(mapOf("correlation_id" to unitCorrelation)),
                    listOf(completed),
                    row("CREATED"),
                    listOf(original),
                )
            val result = RpcResult<Spoolin.Outcome>()
            service(db).approve(approval(), result)
            assertEquals(unitOperation.toString(), result.value().operationId)
            assertEquals("SUCCEEDED", result.value().state)
            assertEquals(unitAsset.toString(), result.value().resultId)
            assertTrue(db.writes.any { "River" in it && "Journey" in it })
            assertTrue(
                db.writes
                    .flatten()
                    .filterIsInstance<String>()
                    .any { it.contains("resultId") },
            )
        }

    @Test fun `rejected review marks original receipt rejected and returns retained problem`() =
        withMaintainer("RejectProposal") {
            val operation = UUID.nameUUIDFromBytes("reject:$unitTarget:1".toByteArray())
            val intent =
                OperationIntent(
                    operation,
                    "account:$unitActor",
                    "RejectProposal",
                    policy,
                    listOf(
                        "proposalId" to unitTarget.toString(),
                        "revision" to "1",
                        "connectorId" to unitCredential.toString(),
                        "clientKind" to null,
                        "contentHash" to Base64.getEncoder().encodeToString(digest),
                    ),
                )
            val completed = receiptRow(intent, "SUCCEEDED", """{"resultId":"$unitTarget","version":"1"}""")
            val originalIntent = OperationIntent(unitOperation, "account:$unitActor", "ProposeMovie", policy, emptyList())
            val db =
                JdbcRows(
                    row(),
                    row(),
                    listOf(mapOf("lock" to 1)),
                    listOf(receiptRow(intent)),
                    row(),
                    listOf(mapOf("count" to 1)),
                    listOf(completed),
                    row("REJECTED"),
                    listOf(receiptRow(originalIntent, "FAILED", error = "REJECTED")),
                )
            val result = RpcResult<Spoolin.Outcome>()
            service(db).reject(approval(), result)
            assertEquals("FAILED", result.value().state)
            assertEquals("REJECTED", result.value().problem.code)
            assertTrue(db.writes.any { "REJECTED" in it })
        }

    @Test fun `connector proposal reserves pending review and returns fresh challenge`() =
        withMaintainer("ProposeMovie", "CLAUDE") {
            val request =
                Spoolin.Proposal
                    .newBuilder()
                    .setContext(
                        Spoolin.Context
                            .newBuilder()
                            .setOperationId(unitOperation.toString())
                            .setCorrelationId(unitCorrelation.toString())
                            .setPolicyRevision(policy),
                    ).setValues(
                        Spoolin.MovieValues
                            .newBuilder()
                            .setTitle(" River ")
                            .setSynopsis(" Journey ")
                            .setSynopsisLanguage("en")
                            .addGenreIds(unitAsset.toString())
                            .setRelease(Spoolin.PartialDate.newBuilder().setYear(2024)),
                    ).build()
            val normalized = ProposalDraft.normalize(request)
            val intent =
                OperationIntent(
                    unitOperation,
                    "account:$unitActor",
                    "ProposeMovie",
                    policy,
                    listOf(
                        "proposalId" to "",
                        "expectedRevision" to null,
                        "connectorId" to unitCredential.toString(),
                        "clientKind" to "CLAUDE",
                        "content" to DeterministicContentSerializer.json(ProposalContent.fromProposal(normalized)),
                    ),
                )
            val db =
                JdbcRows(
                    listOf(receiptRow(intent)),
                    listOf(mapOf("count" to 0)),
                    listOf(receiptRow(intent)),
                    row(),
                )
            val result = RpcResult<Spoolin.ProposalRevision>()
            service(db).propose(request, result)
            assertEquals("River", result.value().values.title)
            assertFalse(result.value().uiChallenge.isEmpty)
            assertEquals("REVIEW", result.value().state)
            assertTrue(
                db.writes
                    .flatten()
                    .filterIsInstance<String>()
                    .any { it.contains("River") && it.contains("Journey") },
            )
        }

    @Test fun `proposal input and live policy failures return meaningful transport statuses`() =
        withMaintainer("ProposeMovie", "CLAUDE") {
            val request =
                Spoolin.Proposal
                    .newBuilder()
                    .setContext(Spoolin.Context.newBuilder().setPolicyRevision("old"))
                    .setValues(
                        Spoolin.MovieValues
                            .newBuilder()
                            .setTitle("River")
                            .setSynopsis("Journey")
                            .addGenreIds(unitAsset.toString())
                            .setRelease(Spoolin.PartialDate.newBuilder().setYear(2024)),
                    ).build()
            val changed = RpcResult<Spoolin.ProposalRevision>()
            service(JdbcRows()).propose(request, changed)
            assertEquals("POLICY_CHANGED", Status.fromThrowable(changed.failure).description)
            val invalid = RpcResult<Spoolin.ProposalRevision>()
            service(JdbcRows()).propose(request.toBuilder().clearValues().build(), invalid)
            assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(invalid.failure).code)
        }
}
