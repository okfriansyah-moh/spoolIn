package com.spoolin.integration.identity

import com.spoolin.catalog.boundary.grpc.CatalogActors
import com.spoolin.core.boundary.grpc.IdentityRpc
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.features.session.SessionCsrf
import com.spoolin.core.features.session.SessionStore
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.media.features.upload.QuarantineStore
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
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticApplicationContext
import org.springframework.core.env.StandardEnvironment
import org.springframework.grpc.client.ClientInterceptorsConfigurer
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer
import org.springframework.grpc.client.NettyGrpcChannelFactory
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin.Id
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

class ActorRotationTest {
    @Test
    fun `actual receivers overlap public keys and retire after sixty seconds without private distribution`() {
        SignedReceiptFixture("t009-actor-rotation.json").use { fixture ->
            RotationProof(fixture).run()
        }
    }
}

private data class Receiver(
    val owner: String,
    val rpc: BindableService,
)

private class RotationProof(
    private val base: SignedReceiptFixture,
) {
    private val transport = ScopedActorTest()
    private val root = base.root
    private val now = Instant.now().epochSecond
    private val clock = ActorClock(Instant.ofEpochSecond(now))
    private val live = CatalogActors(IdentityServiceGrpc.newBlockingStub(base.identity), clock)
    private val old = pair(root, "old")
    private val next = pair(root, "next")
    private val clientContext = StaticApplicationContext().apply { refresh() }
    private val channels = mediaChannels()

    fun run() {
        try {
            listOf("catalog", "media").forEach(::runOwner)
        } finally {
            try {
                channels.destroy()
            } finally {
                clientContext.close()
            }
        }
    }

    private fun runOwner(owner: String) {
        migrate(base.fixture, owner)
        base.fixture.owner
            .pool(owner)
            .use { pool -> ownerProof(owner, pool) }
    }

    private fun mediaChannels(): NettyGrpcChannelFactory {
        val customizer =
            GrpcChannelBuilderCustomizer<io.grpc.netty.NettyChannelBuilder> { target, builder ->
                require(target == "catalog")
                builder.overrideAuthority("catalog")
            }
        val interceptors = ClientInterceptorsConfigurer(clientContext).apply { afterPropertiesSet() }
        return NettyGrpcChannelFactory(listOf(customizer), interceptors).apply {
            setVirtualTargets { target ->
                require(target == "catalog")
                "127.0.0.1:${base.catalog.port}"
            }
            setCredentialsProvider { target ->
                require(target == "catalog")
                io.grpc.TlsChannelCredentials
                    .newBuilder()
                    .trustManager(root.resolve("ca.crt").toFile())
                    .keyManager(root.resolve("media.crt").toFile(), root.resolve("media.key").toFile())
                    .build()
            }
        }
    }

    private fun ownerProof(
        owner: String,
        pool: javax.sql.DataSource,
    ) {
        val operation = UUID.randomUUID()
        val intent = OperationIntent(operation, "account:${base.account}", "WriteMovie", "p1", emptyList())
        val receiver = Receiver(owner, receipt(owner, pool, intent))
        val service = if (owner == "catalog") "Catalog" else "Media"
        val claims =
            ActorClaims(
                owner,
                "/spoolin.v1.$service" + "Service/GetOperation",
                base.account,
                base.issued.principal.id,
                null,
                "MAINTAINER",
                0,
                now,
                now + 30,
                "AAAAAAAAAAAAAAAAAAAAAA",
                UUID.randomUUID(),
                operation,
            )
        rotate(receiver, claims)
    }

    private fun receipt(
        owner: String,
        pool: javax.sql.DataSource,
        intent: OperationIntent,
    ): BindableService {
        val hashes = IntentHashes(mapOf("v1" to ByteArray(32) { 34 }), "v1")
        val authority = ReceiptAuthority { ActorRpc.current()?.let { "account:${it.accountId}" } }
        return if (owner == "catalog") {
            com.spoolin.catalog.operations.receipts
                .OwnerReceipts(pool, hashes)
                .execute(intent, UUID.randomUUID(), true) { SafeResult(intent.id) }
            com.spoolin.catalog.operations.receipts.ReceiptRpc(
                authority,
                com.spoolin.catalog.operations.receipts
                    .OwnerReceiptReads(pool)::find,
            )
        } else {
            com.spoolin.media.operations.receipts
                .OwnerReceipts(pool, hashes)
                .execute(intent, UUID.randomUUID(), true) { SafeResult(intent.id) }
            val environment = StandardEnvironment()
            val quarantine = QuarantineStore(Files.createTempDirectory(root, "media-actor-rotation-"))
            com.spoolin.media.RuntimeConfiguration().mediaService(
                dataSource = pool,
                environment = environment,
                quarantineStore = quarantine,
                channels = channels,
            )
        }
    }

    private fun rotate(
        receiver: Receiver,
        original: ActorClaims,
    ) {
        val operation = requireNotNull(original.operationId)
        val oldToken = old.second.sign(original)
        val newToken = next.second.sign(original.copy(issuedAt = now + 10, expiresAt = now + 40))
        verify(receiver, public(root, receiver.owner, mapOf("old" to old.first))) { call ->
            clock.time = Instant.ofEpochSecond(now + 10)
            assertEquals("SUCCEEDED", call(oldToken, operation))
            assertDenied { call(newToken, operation) }
        }
        verify(receiver, public(root, receiver.owner, mapOf("old" to old.first, "next" to next.first))) { call ->
            clock.time = Instant.ofEpochSecond(now + 10)
            assertEquals("SUCCEEDED", call(newToken, operation))
            clock.time = Instant.ofEpochSecond(now + 32)
            assertEquals("SUCCEEDED", call(oldToken, operation))
            clock.time = Instant.ofEpochSecond(now + 33)
            assertDenied { call(oldToken, operation) }
        }
        verify(receiver, public(root, receiver.owner, mapOf("next" to next.first))) { call ->
            clock.time = Instant.ofEpochSecond(now + 70)
            val fresh = original.copy(issuedAt = now + 70, expiresAt = now + 100)
            assertDenied { call(old.second.sign(fresh), operation) }
            assertEquals("SUCCEEDED", call(next.second.sign(fresh), operation))
            assertEquals(60L, clock.instant().epochSecond - (now + 10))
        }
    }

    private fun actors(
        owner: String,
        file: Path,
    ): io.grpc.ServerInterceptor {
        if (owner == "catalog") {
            return ActorInterceptor(
                owner,
                CatalogActors.protectedMethods,
                ActorVerifier(ActorTrust.verifiers(file)),
                live::check,
                clock,
            )
        }
        val environment =
            org.springframework.core.env
                .StandardEnvironment()
        environment.propertySources.addFirst(
            org.springframework.core.env.MapPropertySource(
                "receiver",
                mapOf("spoolin.actor.enabled" to "true", "ACTOR_VERIFY_KEYS_FILE" to file.toString()),
            ),
        )
        return com.spoolin.media.boundary.grpc.MediaActors
            .interceptor(environment, clock)
    }

    private fun verify(
        receiver: Receiver,
        file: Path,
        action: ((String, UUID) -> String) -> Unit,
    ) {
        val actors = actors(receiver.owner, file)
        val guard =
            if (receiver.owner == "catalog") {
                com.spoolin.catalog.ServiceIdentityInterceptor(actors)
            } else {
                com.spoolin.media.ServiceIdentityInterceptor(actors)
            }
        val server = transport.server(root, receiver.owner, receiver.rpc, guard)
        val channel = transport.channel(root, "core", receiver.owner, server.port)
        try {
            action { token, id -> call(channel, receiver.owner, token, id) }
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun call(
        channel: io.grpc.ManagedChannel,
        owner: String,
        token: String,
        id: UUID,
    ): String {
        val headers = Metadata().apply { put(ActorRpc.metadata, token) }
        val interceptor = MetadataUtils.newAttachHeadersInterceptor(headers)
        val request = Id.newBuilder().setId(id.toString()).build()
        return if (owner == "catalog") {
            CatalogServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(interceptor)
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .getOperation(request)
                .state
        } else {
            MediaServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(interceptor)
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .getOperation(request)
                .state
        }
    }

    private fun pair(
        root: Path,
        id: String,
    ): Pair<KeyPair, ActorSigner> {
        val pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val file = root.resolve("core-signer-$id.der")
        Files.write(file, pair.private.encoded)
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        return pair to ActorSigner(id, ActorTrust.signer(file))
    }

    private fun public(
        root: Path,
        owner: String,
        pairs: Map<String, KeyPair>,
    ): Path {
        val file = root.resolve("$owner-public.json")
        val values =
            pairs.entries.joinToString(",") {
                "\"${it.key}\":\"${Base64.getEncoder().encodeToString(it.value.public.encoded)}\""
            }
        Files.writeString(file, "{$values}")
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        return file
    }

    private fun migrate(
        fixture: CryptoFixture,
        owner: String,
    ) {
        fixture.owner.membership(owner, "grant")
        fixture.owner.pool(owner, "migrator").use { pool ->
            org.flywaydb.core.Flyway
                .configure()
                .dataSource(pool)
                .schemas("appdata")
                .defaultSchema("appdata")
                .locations("filesystem:${fixture.owner.repository}/services/$owner/src/main/resources/db/migration")
                .createSchemas(false)
                .failOnMissingLocations(true)
                .load()
                .migrate()
        }
        fixture.owner.grants(owner)
        fixture.owner.membership(owner, "revoke")
    }

    private fun assertDenied(action: () -> Unit) {
        val failure = assertThrows(StatusRuntimeException::class.java, action)
        assertEquals(Status.Code.PERMISSION_DENIED, failure.status.code)
    }
}

private class ActorClock(
    var time: Instant,
) : Clock() {
    override fun instant(): Instant = time

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock {
        require(zone == ZoneOffset.UTC)
        return this
    }
}
