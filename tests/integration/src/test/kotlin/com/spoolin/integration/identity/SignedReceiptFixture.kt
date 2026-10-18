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
import spoolin.v1.IdentityServiceGrpc
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal class SignedReceiptFixture(
    evidenceName: String = "t009-signed-receipt.json",
) : AutoCloseable {
    val fixture = CryptoFixture(evidenceName)
    val root = fixture.owner.root
    private val transport = ScopedActorTest()
    val account =
        UUID.randomUUID().also {
            fixture.accounts.insert(
                it,
                IdentityRules().identity("A Person", "a@example.test"),
                StoredPasswordHash("unusable"),
                AccountRole.MAINTAINER,
            )
        }
    val sessions = SessionStore(fixture.pool, SessionCsrf(ByteArray(32) { 29 }))
    val issued = fixture.pool.committed { sessions.issue(it, account, 0, null) }
    private val core =
        transport.server(
            root,
            "core",
            IdentityRpc(sessions = { sessions }, connectors = { null }),
            com.spoolin.core.ServiceIdentityInterceptor(),
        )
    val identity = transport.channel(root, "catalog", "core", core.port)
    private val pool = migrate().let { fixture.owner.pool("catalog") }
    val operation = UUID.randomUUID()
    val pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    val signer = signer()
    val lookups = AtomicInteger()
    private val rpc =
        ReceiptRpc(ReceiptAuthority { ActorRpc.current()?.let { "account:${it.accountId}" } }) { id, scope ->
            lookups.incrementAndGet()
            OwnerReceiptReads(pool).find(id, scope)
        }
    private val live = CatalogActors(IdentityServiceGrpc.newBlockingStub(identity))
    private val verifier = ActorVerifier(mapOf("key_A" to pair.public))
    private val actors = ActorInterceptor("catalog", CatalogActors.protectedMethods, verifier, live::check)
    val catalog = transport.server(root, "catalog", rpc, com.spoolin.catalog.ServiceIdentityInterceptor(actors))
    val caller = transport.channel(root, "core", "catalog", catalog.port)
    val claims =
        Instant.now().epochSecond.let {
            ActorClaims(
                "catalog",
                "/spoolin.v1.CatalogService/GetOperation",
                account,
                issued.principal.id,
                null,
                "MAINTAINER",
                0,
                it,
                it + 30,
                "AAAAAAAAAAAAAAAAAAAAAA",
                UUID.randomUUID(),
                operation,
            )
        }

    init {
        val intent = OperationIntent(operation, "account:$account", "WriteMovie", "p1", emptyList())
        OwnerReceipts(pool, IntentHashes(mapOf("v1" to ByteArray(32) { 30 }), "v1"))
            .execute(intent, UUID.randomUUID(), true) { SafeResult(operation, version = 7) }
    }

    private fun migrate() {
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
    }

    private fun signer(): ActorSigner {
        val privateFile = root.resolve("signer.der")
        Files.write(privateFile, pair.private.encoded)
        Files.setPosixFilePermissions(privateFile, PosixFilePermissions.fromString("rw-------"))
        val publicFile = root.resolve("verifiers.json")
        val encoded = Base64.getEncoder().encodeToString(pair.public.encoded)
        Files.writeString(publicFile, """{"key_A":"$encoded"}""")
        Files.setPosixFilePermissions(publicFile, PosixFilePermissions.fromString("rw-------"))
        check(ActorTrust.verifiers(publicFile).getValue("key_A") == pair.public)
        return ActorSigner("key_A", ActorTrust.signer(privateFile))
    }

    override fun close() {
        caller.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        catalog.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        identity.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        core.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        pool.close()
        fixture.close()
    }
}
