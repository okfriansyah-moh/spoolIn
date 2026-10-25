package com.spoolin.catalog.unit

import com.spoolin.catalog.features.artwork.ArtworkWorkHandler
import com.spoolin.catalog.features.artwork.ArtworkWorkStore
import com.spoolin.catalog.operations.work.WorkLeaseGuard
import com.spoolin.support.work.AttemptOutcome
import io.grpc.Status
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ArtworkWorkerRulesTest {
    private val one = listOf(mapOf("one" to 1))
    private fun handler(db: JdbcRows, media: GrpcReplies, identity: GrpcReplies = GrpcReplies()) = ArtworkWorkHandler(
        ArtworkWorkStore(db.source, ownerReceipts(db)) { "policy" }, MediaServiceGrpc.newBlockingStub(media),
        IdentityServiceGrpc.newBlockingStub(identity), db.source,
    )
    private fun actor() = Spoolin.Actor.newBuilder().setAccountId(unitActor.toString()).setCredentialId(unitCredential.toString())
        .setAuthEpoch(1).setRole("MAINTAINER").setExpiresAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(Instant.now().plusSeconds(60).epochSecond)).build()

    @Test fun `confirmation and release use catalog version and stable hold identifiers`() {
        for (kind in listOf("ARTWORK_CONFIRM", "ARTWORK_RELEASE")) {
            val db = JdbcRows(artworkRow("ATTACHED"), one)
            val channel = GrpcReplies(Spoolin.Outcome.getDefaultInstance())
            handler(db, channel).handle(artworkClaim(kind))
            val sent = channel.requests.single() as Spoolin.Hold
            assertEquals(unitOperation.toString(), sent.holdId)
            assertEquals(unitTarget.toString(), sent.titleId)
            assertEquals(unitAsset.toString(), sent.assetId)
            assertEquals(8L, sent.catalogVersion)
            assertTrue(db.writes.any { "DONE" in it })
        }
    }

    @Test fun `worker retries transient downstream errors and exhausts permanent or unsupported work`() {
        for ((reply, state) in listOf(Status.UNAVAILABLE to "READY", Status.INVALID_ARGUMENT to "EXHAUSTED")) {
            val db = JdbcRows(artworkRow("ATTACHED"), one)
            handler(db, GrpcReplies(reply)).handle(artworkClaim("ARTWORK_CONFIRM"))
            assertTrue(db.writes.any { state in it })
            assertTrue(db.writes.any { "ARTWORK_DOWNSTREAM_UNAVAILABLE" in it })
        }
        val db = JdbcRows(artworkRow(), one)
        handler(db, GrpcReplies()).handle(artworkClaim("UNKNOWN"))
        assertTrue(db.writes.any { "UNSUPPORTED_WORK_KIND" in it })
    }

    @Test fun `already settled attachment avoids media effects`() {
        val db = JdbcRows(artworkRow("ABORTED"), one)
        val channel = GrpcReplies()
        handler(db, channel).handle(artworkClaim())
        assertTrue(channel.requests.isEmpty())
        assertTrue(db.writes.any { "DONE" in it })
    }

    @Test fun `ready media requires live actor checks before and after reserve then selects catalog asset`() {
        val db = JdbcRows(artworkRow(), one, one, one, artworkRow(), listOf(mapOf("version" to 7L, "status" to "ACTIVE")),
            listOf(mapOf("correlation_id" to unitCorrelation)), one)
        val channel = GrpcReplies(Spoolin.Asset.newBuilder().setState("READY").build(), Spoolin.Outcome.getDefaultInstance())
        val identity = GrpcReplies(actor(), actor())
        handler(db, channel, identity).handle(artworkClaim())
        val check = identity.requests.first() as Spoolin.CheckActorRequest
        assertEquals(unitActor.toString(), check.accountId)
        assertEquals(unitCredential.toString(), check.credentialId)
        assertEquals("catalog", check.audience)
        val hold = channel.requests.last() as Spoolin.Hold
        assertFalse(hold.hasCatalogVersion())
        assertTrue(db.writes.any { unitAsset in it && 8L in it })
        assertTrue(db.writes.any { "ARTWORK_CONFIRM" in it })
    }

    @Test fun `invalid media or revoked actor aborts preparation and releases reserved hold`() {
        for (revoked in listOf(false, true)) {
            val db = JdbcRows(artworkRow(), one, one, one, artworkRow(), listOf(mapOf("version" to 8L)), one)
            val media = GrpcReplies(Spoolin.Asset.newBuilder().setState(if (revoked) "READY" else "REJECTED").build())
            val identity = if (revoked) GrpcReplies(actor().toBuilder().setRole("VIEWER").build()) else GrpcReplies()
            handler(db, media, identity).handle(artworkClaim())
            assertTrue(db.writes.any { (if (revoked) "ARTWORK_UNAVAILABLE" else "FILE_INVALID") in it })
            assertTrue(db.writes.any { "ARTWORK_RELEASE" in it })
        }
    }

    @Test fun `processing asset remains retryable while bounded attempts remain`() {
        val db = JdbcRows(artworkRow(), one)
        handler(db, GrpcReplies(Spoolin.Asset.newBuilder().setState("PROCESSING").build())).handle(artworkClaim())
        assertTrue(db.writes.any { "READY" in it })
    }

    @Test fun `lease completion maps each logical outcome and honors exhausted attempts`() {
        val now = Instant.now()
        for ((outcome, expected) in listOf(AttemptOutcome.SUCCEEDED to "DONE", AttemptOutcome.CANCELLED to "CANCELLED",
            AttemptOutcome.PERMANENT to "EXHAUSTED", AttemptOutcome.UNCERTAIN to "EXHAUSTED", AttemptOutcome.TRANSIENT to "READY")) {
            val db = JdbcRows(one)
            assertTrue(WorkLeaseGuard(db.source, Clock.fixed(now, ZoneOffset.UTC)).complete(artworkClaim(), outcome))
            assertTrue(db.writes.any { expected in it })
        }
        val exhausted = JdbcRows(one)
        WorkLeaseGuard(exhausted.source).complete(artworkClaim().copy(attemptCount = 3), AttemptOutcome.TRANSIENT)
        assertTrue(exhausted.writes.any { "EXHAUSTED" in it })
        val renew = JdbcRows(one)
        assertTrue(WorkLeaseGuard(renew.source).renew(artworkClaim()))
    }
}
