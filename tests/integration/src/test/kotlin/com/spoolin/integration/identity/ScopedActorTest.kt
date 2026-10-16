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
        CryptoFixture("t008-signed-receipt.json").use { fixture ->
            val owner = fixture.owner
            owner.membership("catalog", "grant")
            owner.pool("catalog", "migrator").use { pool ->
                org.flywaydb.core.Flyway
                    .configure()
                    .dataSource(pool)
                    .schemas("appdata")
                    .defaultSchema("appdata")
                    .locations("filesystem:${owner.repository}/services/catalog/src/main/resources/db/migration")
                    .createSchemas(false)
                    .failOnMissingLocations(true)
                    .validateMigrationNaming(true)
                    .load()
                    .migrate()
            }
            owner.grants("catalog")
            owner.membership("catalog", "revoke")
            val account = UUID.randomUUID()
            fixture.accounts.insert(
                account,
                IdentityRules().identity("A Person", "a@example.test"),
                StoredPasswordHash("unusable"),
                AccountRole.MAINTAINER,
            )
            val sessions = SessionStore(fixture.pool, SessionCsrf(ByteArray(32) { 29 }))
            val issued = fixture.pool.committed { sessions.issue(it, account, 0, null) }
            val core = server(owner.root, "core", IdentityRpc { sessions }, com.spoolin.core.ServiceIdentityInterceptor())
            val identityChannel = channel(owner.root, "catalog", "core", core.port)
            owner.pool("catalog").use { pool ->
                val operation = UUID.randomUUID()
                val intent = OperationIntent(operation, "account:$account", "WriteMovie", "p1", emptyList())
                OwnerReceipts(pool, IntentHashes(mapOf("v1" to ByteArray(32) { 30 }), "v1"))
                    .execute(intent, UUID.randomUUID(), true) { SafeResult(operation, version = 7) }
                val pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
                val privateFile = owner.root.resolve("signer.der")
                Files.write(privateFile, pair.private.encoded)
                Files.setPosixFilePermissions(privateFile, PosixFilePermissions.fromString("rw-------"))
                val publicFile = owner.root.resolve("verifiers.json")
                Files.writeString(publicFile, """{"key_A":"${Base64.getEncoder().encodeToString(pair.public.encoded)}"}""")
                Files.setPosixFilePermissions(publicFile, PosixFilePermissions.fromString("rw-------"))
                val signer = ActorSigner("key_A", ActorTrust.signer(privateFile))
                val live = CatalogActors(IdentityServiceGrpc.newBlockingStub(identityChannel))
                val actors =
                    ActorInterceptor(
                        "catalog",
                        CatalogActors.protectedMethods,
                        ActorVerifier(ActorTrust.verifiers(publicFile)),
                        live::check,
                    )
                val lookups = AtomicInteger()
                val rpc =
                    ReceiptRpc(ReceiptAuthority { ActorRpc.current()?.let { "account:${it.accountId}" } }) { id, scope ->
                        lookups.incrementAndGet()
                        OwnerReceiptReads(pool).find(id, scope)
                    }
                val catalog = server(owner.root, "catalog", rpc, com.spoolin.catalog.ServiceIdentityInterceptor(actors))
                val caller = channel(owner.root, "core", "catalog", catalog.port)
                try {
                    val now = Instant.now().epochSecond
                    val claims =
                        ActorClaims(
                            "catalog",
                            "/spoolin.v1.CatalogService/GetOperation",
                            account,
                            issued.principal.id,
                            null,
                            "MAINTAINER",
                            0,
                            now,
                            now + 30,
                            "AAAAAAAAAAAAAAAAAAAAAA",
                            UUID.randomUUID(),
                            operation,
                        )
                    val token = signer.sign(claims)
                    assertEquals("SUCCEEDED", client(caller, token).getOperation(Id.newBuilder().setId(operation.toString()).build()).state)
                    assertEquals(1, lookups.get())
                    assertEquals(
                        Status.Code.PERMISSION_DENIED,
                        assertThrows(StatusRuntimeException::class.java) {
                            client(caller, "$token-x").getOperation(Id.newBuilder().setId(operation.toString()).build())
                        }.status.code,
                    )
                    hostile(signer, claims, pair.private).forEach { (name, hostile) ->
                        val failure =
                            assertThrows(StatusRuntimeException::class.java, {
                                client(caller, hostile).getOperation(Id.newBuilder().setId(operation.toString()).build())
                            }, name)
                        assertEquals(Status.Code.PERMISSION_DENIED, failure.status.code, name)
                        assertEquals(1, lookups.get(), name)
                    }
                    val wrongSan = channel(owner.root, "media", "catalog", catalog.port)
                    try {
                        assertEquals(
                            Status.Code.PERMISSION_DENIED,
                            assertThrows(StatusRuntimeException::class.java) {
                                client(wrongSan, token).getOperation(Id.newBuilder().setId(operation.toString()).build())
                            }.status.code,
                        )
                    } finally {
                        wrongSan.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                    }
                    assertEquals(
                        Status.Code.PERMISSION_DENIED,
                        assertThrows(StatusRuntimeException::class.java) {
                            client(caller, token).getOperation(Id.newBuilder().setId(UUID.randomUUID().toString()).build())
                        }.status.code,
                    )
                    fixture.pool.committed { sessions.revoke(it, issued.principal.id, account) }
                    assertEquals(
                        Status.Code.PERMISSION_DENIED,
                        assertThrows(StatusRuntimeException::class.java) {
                            client(caller, token).getOperation(Id.newBuilder().setId(operation.toString()).build())
                        }.status.code,
                    )
                    assertEquals(1, lookups.get())
                } finally {
                    caller.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                    catalog.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                    identityChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                    core.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                }
            }
        }
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
