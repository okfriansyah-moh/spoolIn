package com.spoolin.integration.identity

import com.spoolin.catalog.boundary.grpc.CatalogActors
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
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
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
    private val transport = ScopedActorTest()

    @Test
    fun `actual receivers overlap public keys and retire after sixty seconds without private distribution`() {
        CryptoFixture("t008-actor-rotation.json").use { fixture ->
            val root = fixture.owner.root
            val account = UUID.randomUUID()
            fixture.accounts.insert(
                account,
                IdentityRules().identity("A Person", "rotation@example.test"),
                StoredPasswordHash("unusable"),
                AccountRole.MAINTAINER,
            )
            val sessions = SessionStore(fixture.pool, SessionCsrf(ByteArray(32) { 33 }))
            val issued = fixture.pool.committed { sessions.issue(it, account, 0, null) }
            val core = transport.server(root, "core", IdentityRpc { sessions }, com.spoolin.core.ServiceIdentityInterceptor())
            val identity = transport.channel(root, "catalog", "core", core.port)
            val now = Instant.now().epochSecond
            val clock = ActorClock(Instant.ofEpochSecond(now))
            val live = CatalogActors(IdentityServiceGrpc.newBlockingStub(identity), clock)
            val old = pair(root, "old")
            val next = pair(root, "next")
            try {
                listOf("catalog", "media").forEach { owner ->
                    migrate(fixture, owner)
                    fixture.owner.pool(owner).use { pool ->
                        val operation = UUID.randomUUID()
                        val intent = OperationIntent(operation, "account:$account", "WriteMovie", "p1", emptyList())
                        val hashes = IntentHashes(mapOf("v1" to ByteArray(32) { 34 }), "v1")
                        val authority = ReceiptAuthority { ActorRpc.current()?.let { "account:${it.accountId}" } }
                        val rpc: BindableService =
                            if (owner == "catalog") {
                                com.spoolin.catalog.operations.receipts
                                    .OwnerReceipts(pool, hashes)
                                    .execute(intent, UUID.randomUUID(), true) { SafeResult(operation) }
                                com.spoolin.catalog.operations.receipts.ReceiptRpc(
                                    authority,
                                    com.spoolin.catalog.operations.receipts
                                        .OwnerReceiptReads(pool)::find,
                                )
                            } else {
                                com.spoolin.media.operations.receipts
                                    .OwnerReceipts(pool, hashes)
                                    .execute(intent, UUID.randomUUID(), true) { SafeResult(operation) }
                                com.spoolin.media.operations.receipts.ReceiptRpc(
                                    authority,
                                    com.spoolin.media.operations.receipts
                                        .OwnerReceiptReads(pool)::find,
                                )
                            }
                        val method = "/spoolin.v1.${if (owner == "catalog") "Catalog" else "Media"}Service/GetOperation"
                        val original =
                            ActorClaims(
                                owner,
                                method,
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
                        val oldToken = old.second.sign(original)
                        val newToken = next.second.sign(original.copy(issuedAt = now + 10, expiresAt = now + 40))
                        verify(root, owner, rpc, public(root, owner, mapOf("old" to old.first)), live, clock) { call ->
                            clock.time = Instant.ofEpochSecond(now + 10)
                            assertEquals("SUCCEEDED", call(oldToken, operation))
                            assertDenied { call(newToken, operation) }
                        }
                        verify(
                            root,
                            owner,
                            rpc,
                            public(root, owner, mapOf("old" to old.first, "next" to next.first)),
                            live,
                            clock,
                        ) { call ->
                            clock.time = Instant.ofEpochSecond(now + 10)
                            assertEquals("SUCCEEDED", call(newToken, operation))
                            clock.time = Instant.ofEpochSecond(now + 32)
                            assertEquals("SUCCEEDED", call(oldToken, operation))
                            clock.time = Instant.ofEpochSecond(now + 33)
                            assertDenied { call(oldToken, operation) }
                        }
                        verify(root, owner, rpc, public(root, owner, mapOf("next" to next.first)), live, clock) { call ->
                            clock.time = Instant.ofEpochSecond(now + 70)
                            val fresh = original.copy(issuedAt = now + 70, expiresAt = now + 100)
                            assertDenied { call(old.second.sign(fresh), operation) }
                            assertEquals("SUCCEEDED", call(next.second.sign(fresh), operation))
                            assertEquals(60L, clock.instant().epochSecond - (now + 10))
                        }
                    }
                }
            } finally {
                identity.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                core.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }

    private fun verify(
        root: Path,
        owner: String,
        rpc: BindableService,
        file: Path,
        live: CatalogActors,
        clock: Clock,
        action: ((String, UUID) -> String) -> Unit,
    ) {
        val method = "spoolin.v1.${if (owner == "catalog") "Catalog" else "Media"}Service/GetOperation"
        val actors =
            ActorInterceptor(
                owner,
                setOf(method),
                ActorVerifier(ActorTrust.verifiers(file)),
                { if (owner == "catalog") live.check(it) },
                clock,
            )
        val guard =
            if (owner == "catalog") {
                com.spoolin.catalog.ServiceIdentityInterceptor(actors)
            } else {
                com.spoolin.media.ServiceIdentityInterceptor(actors)
            }
        val server = transport.server(root, owner, rpc, guard)
        val channel = transport.channel(root, "core", owner, server.port)
        try {
            action { token, id ->
                val headers = Metadata().apply { put(ActorRpc.metadata, token) }
                val interceptor = MetadataUtils.newAttachHeadersInterceptor(headers)
                val request = Id.newBuilder().setId(id.toString()).build()
                if (owner == "catalog") {
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
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
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
        assertEquals(Status.Code.PERMISSION_DENIED, assertThrows(StatusRuntimeException::class.java, action).status.code)
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
