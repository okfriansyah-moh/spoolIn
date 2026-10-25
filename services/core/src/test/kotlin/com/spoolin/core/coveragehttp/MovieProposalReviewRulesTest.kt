package com.spoolin.core.coveragehttp

import com.google.protobuf.ByteString
import com.spoolin.core.boundary.http.BrowserBoundary
import com.spoolin.core.boundary.http.BrowserIdentity
import com.spoolin.core.boundary.http.MovieProposalReview
import com.spoolin.core.features.artworkproxy.PreparedPosterUpload
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.core.features.session.AnonymousCsrf
import com.spoolin.core.features.session.SessionCredential
import com.spoolin.core.features.session.SessionCsrf
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.core.features.session.SessionStore
import com.spoolin.support.policy.PolicyEnvironment
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.ManagedChannel
import io.grpc.MethodDescriptor
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.grpc.client.ChannelBuilderOptions
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.mock.web.MockHttpServletRequest
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.sql.Timestamp
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class HttpManagedChannel(private val replies: HttpRpcReplies) : ManagedChannel() {
    override fun authority() = replies.authority()
    override fun <ReqT : Any, RespT : Any> newCall(method: MethodDescriptor<ReqT, RespT>, options: CallOptions): ClientCall<ReqT, RespT> = replies.newCall(method, options)
    override fun shutdown(): ManagedChannel = this
    override fun shutdownNow(): ManagedChannel = this
    override fun isShutdown() = false
    override fun isTerminated() = false
    override fun awaitTermination(timeout: Long, unit: TimeUnit) = true
}

class MovieProposalReviewRulesTest {
    private val id = UUID.randomUUID().toString()
    private val now = Instant.now()
    private val poster = Spoolin.PreparedPoster.newBuilder().setRefId(id).setAssetId(id).setState("READY").setMediaType("image/png").setWidth(4).setHeight(5).setContentHash("poster-hash").setSourceUrl("https://example.test/poster").setReuseBasis("permission").build()

    @Test
    fun `proposal read maps complete and absent optional values without fabricating identity`() {
        for (complete in listOf(true, false)) {
            val revision = revision(complete)
            val h = Harness(revision)
            val result = h.handler.get(id, if (complete) 2 else null, h.request())
            assertEquals(200, result.statusCode.value())
            assertEquals(id, result.body!!["proposalId"])
            assertEquals("no-store", result.headers.getFirst("Cache-Control"))
            val values = result.body!!["values"] as Map<*, *>
            assertEquals(if (complete) "Original" else null, values["originalTitle"])
            assertEquals(if (complete) 100 else null, values["runtimeMinutes"])
            assertEquals(if (complete) 10 else null, (values["release"] as Map<*, *>)["month"])
            assertEquals(if (complete) 9 else null, (values["release"] as Map<*, *>)["day"])
            val credit = (result.body!!["credits"] as List<*>).single() as Map<*, *>
            assertEquals(if (complete) "Person" else null, credit["personName"])
            assertEquals(if (complete) 1 else null, credit["billingOrder"])
            assertEquals(complete, result.body!!["poster"] != null)
            assertEquals(complete, (h.rpc.requests.single() as Spoolin.MovieProposalRequest).hasRevision())
        }
    }

    @Test
    fun `approval and rejection send exact bound revision and return safe outcome`() {
        for (approve in listOf(true, false)) {
            val outcome = Spoolin.Outcome.newBuilder().setOperationId(id).setState("FAILED").setResultId(id).setVersion(4).setProblem(Spoolin.Problem.newBuilder().setCode("CONFLICT")).build()
            val h = Harness(outcome)
            val body = MovieProposalReview.Decision(2, "hash", "challenge")
            val result = if (approve) h.handler.approve(id, body, h.request()) else h.handler.reject(id, body, h.request())
            assertEquals(200, result.statusCode.value())
            assertEquals("CONFLICT", (result.body!!["problem"] as Map<*, *>)["code"])
            assertEquals(4L, result.body!!["version"])
            val request = h.rpc.requests.single() as Spoolin.ApprovalInput
            assertEquals("hash", request.contentHash)
            assertEquals("challenge", request.challenge)
            assertEquals(2, request.revision)
        }
        val h = Harness(Spoolin.Outcome.newBuilder().setOperationId(id).setState("SUCCEEDED").build())
        val result = h.handler.approve(id, MovieProposalReview.Decision(1, "hash", "challenge"), h.request())
        assertNull(result.body!!["resultId"])
        assertNull(result.body!!["version"])
        assertNull(result.body!!["problem"])
    }

    @Test
    fun `poster response preserves media bytes and maps dependency failures safely`() {
        val image = Spoolin.MoviePosterImage.newBuilder().setPoster(poster).setImageBytes(ByteString.copyFrom(byteArrayOf(1, 2))).build()
        val h = Harness(image)
        val result = h.handler.poster(id, h.request())
        assertEquals(200, result.statusCode.value())
        assertEquals("image/png", result.headers.getFirst("Content-Type"))
        assertEquals("nosniff", result.headers.getFirst("X-Content-Type-Options"))
        assertTrue(result.body!!.contentEquals(byteArrayOf(1, 2)))
        for ((failure, status) in failures()) {
            val failed = Harness(failure)
            assertEquals(status, failed.handler.get(id, null, failed.request()).statusCode.value())
            val posterFailure = Harness(failure)
            val expected = when (status) { 401, 403, 404 -> status; else -> 503 }
            assertEquals(expected, posterFailure.handler.poster(id, posterFailure.request()).statusCode.value())
        }
    }

    @Test
    fun `poster replacement preserves form credits and sources and replaces only poster reference`() {
        val current = revision(true)
        val h = Harness(current, poster, current.toBuilder().setRevision(3).build())
        val body = MovieProposalReview.PosterReplacement(id, 2, "hash", Base64.getEncoder().encodeToString(byteArrayOf(1)), "image/png", "https://example.test/poster", "permission")
        val result = h.handler.replace(id, body, h.request())
        assertEquals(200, result.statusCode.value())
        assertEquals(3, result.body!!["revision"])
        val request = h.rpc.requests.last() as Spoolin.Proposal
        assertEquals(id, request.posterRef)
        assertEquals(2, request.expectedRevision)
        assertEquals(current.values, request.values)
        assertEquals(current.sourcesList, request.sourcesList)
        assertFalse(request.creditsList.single().hasPersonLabel())
        assertFalse(request.creditsList.single().hasRoleLabel())
        val pending = Harness(current, poster.toBuilder().setState("FAILED").build())
        assertEquals("FAILED", pending.handler.replace(id, body, pending.request()).body!!["state"])
        val stale = Harness(current)
        assertEquals(400, stale.handler.replace(id, body.copy(contentHash = "stale"), stale.request()).statusCode.value())
    }

    @Test
    fun `browser authority csrf input and unavailable signing services fail before writes`() {
        val viewer = Harness()
        assertEquals(403, viewer.handler.get(id, null, viewer.request("VIEWER")).statusCode.value())
        val anonymous = Harness()
        assertEquals(401, anonymous.handler.get(id, null, MockHttpServletRequest()).statusCode.value())
        val missingIdentity = Harness(identityAvailable = false)
        assertEquals(503, missingIdentity.handler.get(id, null, MockHttpServletRequest()).statusCode.value())
        val signer = Harness(signerAvailable = false)
        assertEquals(503, signer.handler.get(id, null, signer.request()).statusCode.value())
        val invalidId = Harness()
        assertEquals(400, invalidId.handler.get("invalid", null, invalidId.request()).statusCode.value())
        val invalidRevision = Harness()
        assertEquals(400, invalidRevision.handler.get(id, 0, invalidRevision.request()).statusCode.value())
        for (body in listOf(MovieProposalReview.Decision(0, "hash", "challenge"), MovieProposalReview.Decision(1, "", "challenge"), MovieProposalReview.Decision(1, "hash", ""))) {
            val h = Harness()
            assertEquals(400, h.handler.approve(id, body, h.request()).statusCode.value())
        }
        val insecure = Harness()
        val request = insecure.request(); request.isSecure = false
        assertEquals(403, insecure.handler.approve(id, MovieProposalReview.Decision(1, "hash", "challenge"), request).statusCode.value())
        assertEquals(403, insecure.handler.poster(id, MockHttpServletRequest().also { it.setCookies(jakarta.servlet.http.Cookie("x", "1"), jakarta.servlet.http.Cookie("x", "2")) }).statusCode.value())
    }

    private fun failures() = listOf(Status.UNAUTHENTICATED to 401, Status.PERMISSION_DENIED to 403, Status.NOT_FOUND to 404, Status.ALREADY_EXISTS to 409, Status.FAILED_PRECONDITION to 409, Status.ABORTED to 409, Status.INVALID_ARGUMENT to 400, Status.RESOURCE_EXHAUSTED to 429, Status.UNAVAILABLE to 503)
    private fun revision(complete: Boolean): Spoolin.ProposalRevision {
        val values = Spoolin.MovieValues.newBuilder().setTitle("Film").setSynopsis("Story").addGenreIds("drama").addCountries("JP").setRelease(Spoolin.PartialDate.newBuilder().setYear(2024).also { if (complete) it.setMonth(10).setDay(9) })
        if (complete) values.setOriginalTitle("Original").setRuntimeMinutes(100).setOriginalLanguage("ja").setSynopsisLanguage("en")
        val credit = Spoolin.ProposalCredit.newBuilder().setRoleId("actor").addCharacterNames("Lead")
        if (complete) credit.setPersonId(id).setPersonName("Person").setPersonLabel("Person label").setRoleLabel("Actor").setBillingOrder(1)
        return Spoolin.ProposalRevision.newBuilder().setProposalId(id).setRevision(2).setContentHash("hash").setValues(values).addCredits(credit).setState("PENDING").addSources(Spoolin.FieldSource.newBuilder().setField("title").setUrl("https://example.test/source").setReuseBasis("permission")).also { if (complete) it.poster = poster }.build()
    }
    private inner class Harness(vararg replies: Any, identityAvailable: Boolean = true, signerAvailable: Boolean = true) {
        val rpc = HttpRpcReplies(*replies)
        val db = SessionJdbcFixture(emptyList())
        private val credential = SessionCredential.issue()
        private val csrf = SessionCsrf(ByteArray(32) { 7 })
        private val account = UUID.randomUUID()
        private val session = UUID.randomUUID()
        val handler: MovieProposalReview
        init {
            val channel = HttpManagedChannel(rpc)
            val channels = object : GrpcChannelFactory {
                override fun supports(target: String) = true
                override fun createChannel(target: String, options: ChannelBuilderOptions): ManagedChannel = channel
            }
            val beans = StaticListableBeanFactory()
            if (identityAvailable) beans.addBean("identity", BrowserIdentity(SessionStore(db, csrf), AnonymousCsrf(ByteArray(32) { 9 }), csrf))
            if (signerAvailable) beans.addBean("issuer", artworkIssuer())
            handler = MovieProposalReview(channels, beans.getBeanProvider(BrowserIdentity::class.java), BrowserBoundary("https://localhost:8443"), beans.getBeanProvider(com.spoolin.core.boundary.grpc.ActorIssuer::class.java), PolicyProvider(PolicyEnvironment.resolve { null }), PreparedPosterUpload(MediaServiceGrpc.newStub(channel), 5242880))
        }
        fun request(role: String = "MAINTAINER"): MockHttpServletRequest {
            db.enqueue(mapOf("account_id" to account)); db.enqueue(mapOf("role" to role, "auth_epoch" to 3L))
            db.enqueue(mapOf("id" to session, "auth_epoch" to 3L, "csrf_hash" to csrf.digest(csrf.derive(credential)), "created_at" to Timestamp.from(now.minusSeconds(10)), "last_seen_at" to Timestamp.from(now), "expires_at" to Timestamp.from(now.plusSeconds(3600)), "revoked_at" to null))
            return MockHttpServletRequest("POST", "/api/movie-proposals/$id").also {
                it.isSecure = true; it.contentType = "application/json"
                it.addHeader("Host", "localhost:8443"); it.addHeader("Origin", "https://localhost:8443"); it.addHeader("X-Spoolin-Client", "web"); it.addHeader("X-CSRF-Token", csrf.derive(credential))
                it.setCookies(jakarta.servlet.http.Cookie("__Host-spoolin_session", credential.cookie().substringAfter('=').substringBefore(';')))
            }
        }
    }
}
