package com.spoolin.core.coveragehttp

import com.spoolin.core.boundary.http.McpReviewApprovalGateway
import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.core.features.mcpgateway.McpActorIssuer
import com.spoolin.core.features.mcpgateway.McpGatewayRpc
import com.spoolin.core.features.mcpgateway.ProposalUiChallengeStore
import com.spoolin.core.features.mcpgateway.UiAssertionVerifier
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.policy.PolicyEnvironment
import io.grpc.Context
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import tools.jackson.databind.ObjectMapper
import java.security.KeyPairGenerator
import java.sql.Timestamp
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class McpApprovalHttpRulesTest {
    private val id = UUID.randomUUID().toString()
    private val connector = UUID.randomUUID()
    private val account = UUID.randomUUID()
    private val secret = ByteArray(32) { 11 }
    private val hash = Base64.getEncoder().encodeToString(ByteArray(32) { 13 })
    private val body = McpReviewApprovalGateway.ApprovalBody(id, 2, hash, "private-challenge")

    @Test
    fun `missing malformed empty bearer and incomplete body refuse approval without downstream access`() {
        val h = Harness()
        for (authorization in listOf(null, "", "Basic token", "Bearer", "Bearer   ")) {
            assertEquals(
                401,
                h.http
                    .approveMovie(authorization, body)
                    .statusCode
                    .value(),
            )
            assertEquals(
                401,
                h.http
                    .rejectMovie(authorization, body)
                    .statusCode
                    .value(),
            )
        }
        for (invalid in listOf(
            body.copy(proposalId = ""),
            body.copy(contentHash = ""),
            body.copy(challenge = ""),
            body.copy(revision = 0),
        )) {
            assertEquals(
                400,
                h.http
                    .approveMovie("Bearer token", invalid)
                    .statusCode
                    .value(),
            )
        }
        assertEquals(
            401,
            h.http
                .approveMovie("Bearer token", body.copy(challenge = "other"))
                .statusCode
                .value(),
        )
        assertEquals(
            401,
            h.http
                .rejectMovie("Bearer token", body.copy(contentHash = "other"))
                .statusCode
                .value(),
        )
        assertEquals(0, h.db.queries)
    }

    @Test
    fun `trusted approval and rejection preserve exact binding and safe optional result projection`() {
        for (approve in listOf(true, false)) {
            val outcome =
                Spoolin.Outcome
                    .newBuilder()
                    .setOperationId(
                        id,
                    ).setState("FAILED")
                    .setResultId(id)
                    .setVersion(7)
                    .setProblem(Spoolin.Problem.newBuilder().setCode("CONFLICT"))
                    .build()
            val h = Harness(outcome)
            h.authorize()
            val result =
                h.trusted(
                    approve,
                ) { if (approve) h.http.approveMovie("  bEaReR token ", body) else h.http.rejectMovie("Bearer token", body) }
            assertEquals(200, result.statusCode.value())
            assertEquals(id, result.body!!["operationId"])
            assertEquals(7L, result.body!!["version"])
            assertEquals("CONFLICT", (result.body!!["problem"] as Map<*, *>)["code"])
            val input = h.rpc.requests.single() as Spoolin.ApprovalInput
            assertEquals(hash, input.contentHash)
            assertEquals(body.challenge, input.challenge)
            h.verifier.close()
        }
        val h =
            Harness(
                Spoolin.Outcome
                    .newBuilder()
                    .setOperationId(id)
                    .setState("SUCCEEDED")
                    .build(),
            )
        h.authorize()
        val result = h.trusted(true) { h.http.approveMovie("Bearer token", body) }
        assertNull(result.body!!["version"])
        assertNull(result.body!!["resultId"])
        assertNull(result.body!!["problem"])
        assertEquals(false, result.body!!["resultExpired"])
        h.verifier.close()
    }

    @Test
    fun `trusted downstream invalid forbidden and unavailable failures map to HTTP failure without body`() {
        for ((status, expected) in listOf(Status.INVALID_ARGUMENT to 400, Status.PERMISSION_DENIED to 403, Status.UNAVAILABLE to 502)) {
            val h = Harness(status)
            h.authorize()
            val result = h.trusted(true) { h.http.approveMovie("Bearer token", body) }
            assertEquals(expected, result.statusCode.value())
            assertNull(result.body)
            h.verifier.close()
        }
        val denied = Harness()
        denied.db.enqueue()
        assertEquals(
            403,
            denied.http
                .approveMovie("Bearer token", body)
                .statusCode
                .value(),
        )
        assertTrue(denied.rpc.requests.isEmpty())
        denied.verifier.close()
    }

    private inner class Harness(
        vararg replies: Any,
    ) {
        val db = SessionJdbcFixture(emptyList())
        val rpc = HttpRpcReplies(*replies)
        val verifier = UiAssertionVerifier(true, "test", secret, ObjectMapper())
        private val challenges = ProposalUiChallengeStore()
        val http: McpReviewApprovalGateway

        init {
            challenges.put(id, 2, hash, body.challenge)
            val issuer = McpActorIssuer(ActorSigner("test", KeyPairGenerator.getInstance("Ed25519").generateKeyPair().private))
            val gateway =
                McpGatewayRpc(
                    ConnectorStore(db),
                    PolicyProvider(
                        PolicyEnvironment.resolve {
                            null
                        },
                    ),
                    CatalogServiceGrpc.newBlockingStub(rpc),
                    issuer,
                    challenges,
                    verifier,
                )
            http = McpReviewApprovalGateway(gateway, challenges)
        }

        fun authorize() {
            repeat(2) {
                db.enqueue(
                    mapOf(
                        "id" to connector,
                        "account_id" to account,
                        "client_kind" to "CHATGPT",
                        "auth_epoch" to 1L,
                        "scope" to "movie:propose movie:approve",
                        "expires_at" to Timestamp.from(Instant.now().plusSeconds(600)),
                        "revoked_at" to null,
                    ),
                )
            }
            repeat(2) { db.enqueue(mapOf("role" to "MAINTAINER")) }
        }

        fun <T> trusted(
            approve: Boolean,
            action: () -> T,
        ): T {
            // Simulates an authenticated host assertion at the private external boundary, not host UI trust proof.
            val claims =
                sortedMapOf<String, Any>(
                    "audience" to "core",
                    "connector" to connector.toString(),
                    "client" to "CHATGPT",
                    "session" to UUID.randomUUID().toString(),
                    "proposal" to id,
                    "revision" to 2,
                    "hash" to hash,
                    "method" to "/spoolin.v1.McpGatewayService/${if (approve) "ApproveMovie" else "RejectMovie"}",
                    "invocation" to UUID.randomUUID().toString(),
                    "expiry" to Instant.now().epochSecond + 30,
                )
            val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(ObjectMapper().writeValueAsBytes(claims))
            val signed = "test.$payload"
            val mac = Mac.getInstance("HmacSHA256").also { it.init(SecretKeySpec(secret, "HmacSHA256")) }
            val assertion = "$signed.${Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(signed.toByteArray()))}"
            return Context.current().withValue(UiAssertionVerifier.assertionContext, assertion).call(action)
        }
    }
}
