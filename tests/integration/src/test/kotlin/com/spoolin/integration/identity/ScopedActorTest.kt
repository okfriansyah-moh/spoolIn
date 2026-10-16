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

    private fun server(
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

    private fun channel(
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
