package com.spoolin.integration.identity

import com.spoolin.catalog.boundary.grpc.CatalogActors
import com.spoolin.catalog.operations.receipts.OwnerReceiptReads
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.catalog.operations.receipts.ReceiptRpc
import com.spoolin.core.boundary.grpc.IdentityRpc
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.features.session.SessionCsrf
import com.spoolin.core.features.session.SessionStore
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorTrust
import com.spoolin.support.identity.ActorVerifier
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptAuthority
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.committed
import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.MetadataUtils
import io.netty.handler.ssl.ClientAuth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin.Id
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ScopedActorTest {
    @Test
    fun `real signed receipt call verifies mTLS and live Core revocation before owner lookup`() {
        SignedReceiptFixture().use { proof ->
            verifyCalls(proof)
        }
    }

    private fun verifyCalls(proof: SignedReceiptFixture) {
        val token = proof.signer.sign(proof.claims)
        val request = Id.newBuilder().setId(proof.operation.toString()).build()
        assertEquals("SUCCEEDED", client(proof.caller, token).getOperation(request).state)
        assertEquals(1, proof.lookups.get())
        assertDenied { client(proof.caller, "$token-x").getOperation(request) }
        hostile(proof.signer, proof.claims, proof.pair.private).forEach { (name, value) ->
            val failure =
                assertThrows(StatusRuntimeException::class.java, {
                    client(proof.caller, value).getOperation(request)
                }, name)
            assertEquals(Status.Code.PERMISSION_DENIED, failure.status.code, name)
            assertEquals(1, proof.lookups.get(), name)
        }
        wrongSan(proof, token, request)
        assertDenied {
            client(proof.caller, token).getOperation(Id.newBuilder().setId(UUID.randomUUID().toString()).build())
        }
        proof.fixture.pool.committed { proof.sessions.revoke(it, proof.issued.principal.id, proof.account) }
        assertDenied { client(proof.caller, token).getOperation(request) }
        assertEquals(1, proof.lookups.get())
    }

    private fun wrongSan(
        proof: SignedReceiptFixture,
        token: String,
        request: Id,
    ) {
        val wrong = channel(proof.root, "media", "catalog", proof.catalog.port)
        try {
            assertDenied { client(wrong, token).getOperation(request) }
        } finally {
            wrong.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun assertDenied(call: () -> Unit) {
        assertEquals(Status.Code.PERMISSION_DENIED, assertThrows(StatusRuntimeException::class.java, call).status.code)
    }

    private fun hostile(
        signer: ActorSigner,
        claims: ActorClaims,
        privateKey: java.security.PrivateKey,
    ): List<Pair<String, String>> {
        val foreign = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val variants =
            listOf(
                "audience" to claims.copy(audience = "media"),
                "method" to claims.copy(method = "/spoolin.v1.CatalogService/WriteMovie"),
                "account" to claims.copy(accountId = UUID.randomUUID()),
                "credential" to claims.copy(credentialId = UUID.randomUUID()),
                "epoch" to claims.copy(authEpoch = 1),
                "role" to claims.copy(role = "VIEWER"),
                "credit role" to claims.copy(role = "Actor"),
                "client binding" to claims.copy(clientKind = "CHATGPT"),
                "operation" to claims.copy(operationId = UUID.randomUUID()),
                "missing operation" to claims.copy(operationId = null),
                "nonce" to claims.copy(nonce = "AA"),
                "padded nonce" to claims.copy(nonce = "AAAAAAAAAAAAAAAAAAAAAA=="),
                "zero duration" to claims.copy(expiresAt = claims.issuedAt),
                "duration31" to claims.copy(expiresAt = claims.issuedAt + 31),
                "expired" to claims.copy(issuedAt = claims.issuedAt - 60, expiresAt = claims.issuedAt - 30),
                "future" to claims.copy(issuedAt = claims.issuedAt + 20, expiresAt = claims.issuedAt + 30),
            ).map { (name, value) -> name to signer.sign(value) }
        val valid = signer.sign(claims)
        val input = valid.split('.')[1]
        val json = String(Base64.getUrlDecoder().decode(input), Charsets.UTF_8)
        val corrupt =
            listOf(
                "duplicate JSON" to json.replace("\"issuer\":\"core\"", "\"issuer\":\"core\",\"issuer\":\"core\""),
                "caller algorithm" to json.replace("{", "{\"alg\":\"EdDSA\","),
                "issuer" to json.replace("\"issuer\":\"core\"", "\"issuer\":\"media\""),
                "epoch encoding" to json.replace("\"authEpoch\":\"0\"", "\"authEpoch\":0"),
                "trailing JSON" to (json + "{}"),
            ).map { (name, payload) -> name to raw(payload.toByteArray(), privateKey) }
        return variants + corrupt +
            listOf(
                "wrong signing key" to ActorSigner("key_A", foreign.private).sign(claims),
                "unknown key" to ActorSigner("unknown", foreign.private).sign(claims),
                "oversized metadata" to "x".repeat(4097),
                "padded encoding" to (signer.sign(claims) + "="),
                "extra segment" to (signer.sign(claims) + ".extra"),
            )
    }

    private fun raw(
        payload: ByteArray,
        key: java.security.PrivateKey,
    ): String {
        val input = "key_A.${Base64.getUrlEncoder().withoutPadding().encodeToString(payload)}"
        val signature = java.security.Signature.getInstance("Ed25519")
        signature.initSign(key)
        signature.update(input.toByteArray(Charsets.US_ASCII))
        return "$input.${Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign())}"
    }

    private fun client(
        channel: ManagedChannel,
        token: String,
    ): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
        return CatalogServiceGrpc
            .newBlockingStub(channel)
            .withDeadlineAfter(3, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
    }

    internal fun server(
        root: Path,
        owner: String,
        rpc: BindableService,
        interceptor: ServerInterceptor,
    ): Server {
        val tls =
            GrpcSslContexts
                .forServer(root.resolve("$owner.crt").toFile(), root.resolve("$owner.key").toFile())
                .trustManager(root.resolve("ca.crt").toFile())
                .clientAuth(ClientAuth.REQUIRE)
                .build()
        return NettyServerBuilder
            .forAddress(InetSocketAddress("127.0.0.1", 0))
            .sslContext(tls)
            .addService(ServerInterceptors.intercept(rpc, interceptor))
            .build()
            .start()
    }

    internal fun channel(
        root: Path,
        from: String,
        receiver: String,
        port: Int,
    ): ManagedChannel {
        val tls =
            GrpcSslContexts
                .forClient()
                .trustManager(root.resolve("ca.crt").toFile())
                .keyManager(root.resolve("$from.crt").toFile(), root.resolve("$from.key").toFile())
                .build()
        return NettyChannelBuilder
            .forAddress("127.0.0.1", port)
            .overrideAuthority(receiver)
            .sslContext(tls)
            .build()
    }
}
