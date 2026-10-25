package com.spoolin.core.coveragehttp

import com.spoolin.core.boundary.http.ArtworkServing
import com.spoolin.core.boundary.http.BrowserIdentity
import com.spoolin.core.features.artworkproxy.ArtworkReadProxy
import com.spoolin.core.features.ratelimit.RateAdmission
import com.spoolin.core.features.ratelimit.RateAgreement
import com.spoolin.core.features.ratelimit.TrustedAdmission
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.policy.ConfigurationSources
import io.grpc.Status
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ArtworkServingHttpRulesTest {
    @TempDir lateinit var root: Path

    @Test
    fun `public artwork transfer serves verified bytes and headers and completes asynchronous request`() {
        fixture(HttpRpcReplies(artworkGrant()), HttpRpcReplies(artworkChunks())) { handler, db ->
            admit(db)
            val request = AsyncRequest()
            val response = MockHttpServletResponse()
            handler.get(artworkAsset.toString(), request, response)
            assertTrue(request.completed.await(5, TimeUnit.SECONDS))
            assertEquals(200, response.status)
            assertEquals("image/png", response.contentType)
            assertEquals("processed-png", response.contentAsString)
            assertEquals("no-store", response.getHeader("Cache-Control"))
            assertEquals("nosniff", response.getHeader("X-Content-Type-Options"))
        }
    }

    @Test
    fun `public missing forbidden or unavailable remote artwork maps safe asynchronous status`() {
        for ((failure, expected) in listOf(Status.NOT_FOUND to 404, Status.PERMISSION_DENIED to 404, Status.UNAUTHENTICATED to 401, Status.UNAVAILABLE to 503)) {
            fixture(HttpRpcReplies(failure), HttpRpcReplies()) { handler, db ->
                admit(db)
                val request = AsyncRequest(); val response = MockHttpServletResponse()
                handler.get(artworkAsset.toString(), request, response)
                assertTrue(request.completed.await(5, TimeUnit.SECONDS))
                assertEquals(expected, response.status)
                assertTrue(response.contentAsByteArray.isEmpty())
            }
        }
    }

    @Test
    fun `invalid ids unsupported range and malformed view selectors never reach transport`() {
        fixture(HttpRpcReplies(), HttpRpcReplies()) { handler, db ->
            val ranged = MockHttpServletRequest().also { it.addHeader("Range", "bytes=0-10") }
            val response = MockHttpServletResponse(); handler.get(artworkAsset.toString(), ranged, response)
            assertEquals(416, response.status)
            for ((id, request) in listOf("invalid" to MockHttpServletRequest(), artworkAsset.toString() to MockHttpServletRequest().also { it.addParameter("view", "public") }, artworkAsset.toString() to MockHttpServletRequest().also { it.addParameter("unexpected", "x") }, artworkAsset.toString() to MockHttpServletRequest().also { it.addParameter("view", "management", "management") })) {
                val invalid = MockHttpServletResponse(); handler.get(id, request, invalid)
                assertEquals(404, invalid.status)
            }
            assertEquals(0, db.queries)
        }
    }

    @Test
    fun `rate limit unavailable admission and missing protected identity fail before asynchronous work`() {
        fixture(HttpRpcReplies(), HttpRpcReplies()) { handler, db ->
            db.enqueue(mapOf("1" to Instant.now().epochSecond)); db.enqueue()
            val limited = MockHttpServletResponse(); handler.get(artworkAsset.toString(), MockHttpServletRequest(), limited)
            assertEquals(429, limited.status)
            assertTrue(limited.getHeader("Retry-After")!!.toLong() >= 1)
            Files.writeString(root.resolve("agreement.json"), "{}")
            val unavailable = MockHttpServletResponse(); handler.get(artworkAsset.toString(), MockHttpServletRequest(), unavailable)
            assertEquals(503, unavailable.status)
            val protected = MockHttpServletResponse(); handler.get(artworkAsset.toString(), MockHttpServletRequest().also { it.addParameter("view", "management") }, protected)
            assertEquals(503, protected.status)
            assertFalse(protected.isCommitted && protected.contentAsByteArray.isNotEmpty())
        }
    }

    @Test
    fun `closed transfer executor rejects newly admitted work without leaving async request open`() {
        fixture(HttpRpcReplies(), HttpRpcReplies()) { handler, db ->
            admit(db); handler.close()
            val request = AsyncRequest(); val response = MockHttpServletResponse()
            handler.get(artworkAsset.toString(), request, response)
            assertTrue(request.completed.await(5, TimeUnit.SECONDS))
            assertEquals(503, response.status)
        }
    }

    private fun admit(db: SessionJdbcFixture) { db.enqueue(mapOf("1" to Instant.now().epochSecond)); db.enqueue(emptyMap()) }
    private fun fixture(catalog: HttpRpcReplies, media: HttpRpcReplies, test: (ArtworkServing, SessionJdbcFixture) -> Unit) {
        val db = SessionJdbcFixture(emptyList()); val profiles = RateProfiles(ConfigurationSources()); val secret = ByteArray(32) { 17 }
        val canary = Mac.getInstance("HmacSHA256").also { it.init(SecretKeySpec(secret, "HmacSHA256")) }.doFinal("spoolin-rate-agreement-v1".toByteArray())
        val path = root.resolve("agreement.json")
        Files.writeString(path, "{\"profileRevision\":\"${profiles.revision}\",\"keyCanary\":\"${java.util.HexFormat.of().formatHex(canary)}\"}")
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        val beans = StaticListableBeanFactory()
        beans.addBean("admission", TrustedAdmission(RateAdmission(db, profiles, secret, RateAgreement(path, profiles.revision, secret)::check)))
        val proxy = ArtworkReadProxy(CatalogServiceGrpc.newBlockingStub(catalog), MediaServiceGrpc.newBlockingStub(media), artworkIssuer())
        val handler = ArtworkServing(proxy, beans.getBeanProvider(BrowserIdentity::class.java), beans.getBeanProvider(TrustedAdmission::class.java))
        try { test(handler, db) } finally { handler.close() }
    }
    private class AsyncRequest : MockHttpServletRequest() {
        val completed = CountDownLatch(1)
        init { isAsyncSupported = true }
        override fun startAsync(): AsyncContext = super.startAsync().also { async ->
            async.addListener(object : AsyncListener {
                override fun onComplete(event: AsyncEvent) { completed.countDown() }
                override fun onTimeout(event: AsyncEvent) = Unit
                override fun onError(event: AsyncEvent) = Unit
                override fun onStartAsync(event: AsyncEvent) = Unit
            })
        }
    }
}
