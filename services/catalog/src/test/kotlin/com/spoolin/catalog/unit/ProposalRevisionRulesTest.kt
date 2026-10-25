package com.spoolin.catalog.unit

import com.spoolin.catalog.features.proposal.*
import com.spoolin.support.identity.ActorClaims
import io.grpc.Status
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Instant
import java.util.Base64

class ProposalRevisionRulesTest {
    private val content = """{"policy_revision":"policy","values":{"title":"River","synopsis":"Journey","synopsis_language":"en","release":{"year":2024}}}"""
    private fun claims(client: String? = "CLAUDE") = ActorClaims("catalog", "/spoolin.v1.CatalogService/ProposeMovie", unitActor,
        unitCredential, client, "MAINTAINER", 1, 1, 31, "nonce", unitCorrelation, unitOperation)
    private fun request() = Spoolin.Proposal.newBuilder().setContext(Spoolin.Context.newBuilder().setOperationId(unitOperation.toString())
        .setCorrelationId(unitCorrelation.toString()).setPolicyRevision("policy"))
        .setValues(Spoolin.MovieValues.newBuilder().setTitle("River").setSynopsis("Journey").addGenreIds(unitAsset.toString())
            .setRelease(Spoolin.PartialDate.newBuilder().setYear(2024))).build()
    private fun row(state: String = "REVIEW", expiry: Instant = unitNow.plusSeconds(3600)) = ProposalRow(unitTarget, 1,
        unitActor, unitCredential, "CLAUDE", content, ByteArray(32) { 2 }, ByteArray(32) { 3 }, state, unitOperation, null, expiry)
    private fun stored(row: ProposalRow = row()) = mapOf("id" to row.id, "revision" to row.revision, "actor_id" to row.actorId,
        "connector_id" to row.connectorId, "client_kind" to row.clientKind, "content_json" to row.contentJson,
        "content_hash" to row.contentHash, "challenge_hash" to row.challengeHash, "state" to row.state,
        "operation_id" to row.operationId, "movie_id" to row.movieId, "expires_at" to Timestamp.from(row.expiresAt))

    @Test fun `initial connector proposal inserts review with digest challenge and one day expiry`() {
        val db = JdbcRows(listOf(mapOf("count" to 0)))
        val result = db.source.connection.use { ProposalRevisions.insert(it, request(), claims(), "challenge", unitNow) }
        assertEquals(1L, result.version)
        assertTrue(db.writes.single().contains(Timestamp.from(unitNow.plusSeconds(86400))))
        assertTrue(db.writes.single().filterIsInstance<ByteArray>().any {
            it.contentEquals(MessageDigest.getInstance("SHA-256").digest("challenge".toByteArray()))
        })
        assertTrue(db.writes.single().filterIsInstance<String>().any { it.contains("River") && it.contains("policy") })
    }

    @Test fun `browser revision preserves original connector and operation while superseding old review`() {
        val old = row()
        val db = JdbcRows(listOf(stored(old)), listOf(mapOf("count" to 0)))
        val request = request().toBuilder().setProposalId(unitTarget.toString()).setExpectedRevision(1).build()
        val result = db.source.connection.use { ProposalRevisions.insert(it, request, claims(null), "new-challenge", unitNow) }
        assertEquals(unitTarget, result.resultId)
        assertEquals(2L, result.version)
        assertTrue(db.writes.any { unitTarget in it && 1 in it && it.size == 2 })
        assertTrue(db.writes.any { unitCredential in it && unitOperation in it && 2 in it })
    }

    @Test fun `proposal quota and revision target mismatches reject before creating new review`() {
        val full = JdbcRows(listOf(mapOf("count" to 20)))
        assertEquals(Status.Code.RESOURCE_EXHAUSTED, assertThrows(io.grpc.StatusRuntimeException::class.java) {
            full.source.connection.use { ProposalRevisions.insert(it, request(), claims(), "challenge", unitNow) }
        }.status.code)
        assertTrue(full.writes.isEmpty())
        val invalid = listOf(request().toBuilder().setProposalId(unitTarget.toString()).build(),
            request().toBuilder().setProposalId(unitTarget.toString()).setExpectedRevision(0).build())
        for (request in invalid) {
            JdbcRows().source.connection.use { conn -> assertThrows(IllegalArgumentException::class.java) {
                ProposalRevisions.insert(conn, request, claims(), "challenge", unitNow)
            } }
        }
        val browser = JdbcRows()
        assertThrows(IllegalArgumentException::class.java) { browser.source.connection.use {
            ProposalRevisions.insert(it, request(), claims(null), "challenge", unitNow)
        } }
        val stale = JdbcRows(listOf(stored()))
        assertEquals("REVISION_MISMATCH", assertThrows(io.grpc.StatusRuntimeException::class.java) {
            stale.source.connection.use { ProposalRevisions.insert(it, request().toBuilder().setProposalId(unitTarget.toString()).setExpectedRevision(2).build(), claims(), "challenge", unitNow) }
        }.status.description)
    }

    @Test fun `proposal ownership and review expiry are checked independently`() {
        ProposalRevisions.assertOwner(row(), claims())
        ProposalRevisions.assertOwner(row(), claims(null).copy(credentialId = unitAsset))
        for (claims in listOf(claims().copy(accountId = unitAsset), claims().copy(credentialId = unitAsset), claims("CHATGPT"))) {
            assertEquals("ACTOR_BINDING_MISMATCH", assertThrows(io.grpc.StatusRuntimeException::class.java) {
                ProposalRevisions.assertOwner(row(), claims)
            }.status.description)
        }
        assertEquals("PROPOSAL_EXPIRED", assertThrows(io.grpc.StatusRuntimeException::class.java) {
            ProposalRevisions.requireReview(row(expiry = unitNow), unitNow)
        }.status.description)
        assertEquals("PROPOSAL_NOT_REVIEWABLE", assertThrows(io.grpc.StatusRuntimeException::class.java) {
            ProposalRevisions.requireReview(row("REJECTED"), unitNow)
        }.status.description)
        for (count in listOf(0, 2)) {
            JdbcRows(listOf(mapOf("count" to count))).source.connection.use { conn ->
                assertEquals("ORIGINAL_INTENT_NOT_PENDING", assertThrows(io.grpc.StatusRuntimeException::class.java) {
                    ProposalRevisions.requirePendingIntent(conn, row())
                }.status.description)
            }
        }
        JdbcRows(listOf(mapOf("count" to 1))).source.connection.use { ProposalRevisions.requirePendingIntent(it, row()) }
    }

    @Test fun `proposal store maps exact review and marks creation rejection without disclosing content`() {
        val db = JdbcRows(listOf(stored()), listOf(stored()), listOf(stored()), emptyList())
        db.source.connection.use { conn ->
            assertEquals(unitTarget, ProposalStore.loadForUpdate(conn, unitTarget, 1)?.id)
            assertEquals("CLAUDE", ProposalStore.initialForOperation(conn, unitOperation)?.clientKind)
            assertEquals(1, ProposalStore.latestForUpdate(conn, unitTarget)?.revision)
            assertEquals("PROPOSAL_NOT_FOUND", assertThrows(io.grpc.StatusRuntimeException::class.java) {
                ProposalStore.requireForUpdate(conn, unitTarget, 3)
            }.status.description)
            ProposalStore.markCreated(conn, row(), unitAsset, unitNow, unitNow.plusSeconds(120))
            ProposalStore.markRejected(conn, unitTarget, 1)
        }
        assertTrue(db.writes.any { unitAsset in it && Timestamp.from(unitNow.plusSeconds(120)) in it })
    }

    @Test fun `proposal response delivers challenge only when supplied and expires retained review`() {
        val expired = ProposalResponse.fromRow(row(expiry = Instant.parse("2000-01-01T00:00:00Z")), null)
        assertEquals("EXPIRED", expired.state)
        assertTrue(expired.uiChallenge.isEmpty)
        val ready = ProposalResponse.fromRow(row(expiry = Instant.parse("2100-01-01T00:00:00Z")), "challenge")
        assertEquals("REVIEW", ready.state)
        assertEquals("challenge", ready.uiChallenge.toStringUtf8())
        assertEquals(Base64.getEncoder().encodeToString(ByteArray(32) { 2 }), ready.contentHash)
        assertEquals("River", ready.values.title)
        assertEquals(unitOperation.toString(), ready.operationId)
    }
}
