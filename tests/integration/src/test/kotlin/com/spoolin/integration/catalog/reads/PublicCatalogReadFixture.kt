package com.spoolin.integration.catalog.reads

import com.spoolin.catalog.boundary.grpc.CatalogServiceRpc
import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.CatalogCursor
import com.spoolin.catalog.features.search.PeopleCursor
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.features.taxonomy.read.TaxonomyReads
import com.spoolin.catalog.operations.receipts.OwnerReceiptReads
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.integration.identity.ScopedActorTest
import com.spoolin.support.receipts.ReceiptAuthority
import com.spoolin.support.receipts.executeBound
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Explicit synthetic SQL setup; requests exercise production handlers over real mTLS/PostgreSQL. */
internal class PublicCatalogReadFixture(
    private val policy: () -> com.spoolin.support.policy.PublicPolicy = {
        com.spoolin.support.policy.PolicySettings
            .resolve(
                com.spoolin.support.policy
                    .ConfigurationSources(),
            )
    },
    private val proof: com.spoolin.integration.identity.SignedReceiptFixture? = null,
) : AutoCloseable {
    val fixture = proof?.fixture ?: CryptoFixture("catalog-public-read-boundary.json")
    private val transport = ScopedActorTest()
    private val owner = fixture.owner
    val pool = migrate().let { owner.pool("catalog") }
    val title = UUID.randomUUID()
    val archived = UUID.randomUUID()
    val person = UUID.randomUUID()
    val genre = UUID.randomUUID()
    private val cursor = ByteArray(32) { 17 }
    private val rpc =
        CatalogServiceRpc(
            TitleSearchSql(pool, policy),
            PersonSearch(pool, policy),
            TaxonomyReads(pool),
            ProvenanceReads(pool),
            OwnerReceiptReads(pool),
            ReceiptAuthority { null },
            CatalogCursor(cursor),
            PeopleCursor(cursor),
            managedReads =
                com.spoolin.catalog.features.movie.ManagedMovieReads(
                    pool,
                    ProvenanceReads(pool),
                    TitleSearchSql(pool, policy),
                    com.spoolin.catalog.operations.audit
                        .AuditReads(pool),
                    policy,
                    com.spoolin.catalog.features.search
                        .ManagedReadCursors(cursor),
                ),
            managedPersonReads =
                com.spoolin.catalog.features.person.read.ManagedPersonReads(
                    pool,
                    policy,
                    com.spoolin.catalog.features.search
                        .ManagedReadCursors(cursor),
                ),
            managedCreditReads =
                com.spoolin.catalog.features.credit.ManagedCreditReads(
                    pool,
                    policy,
                    com.spoolin.catalog.features.search
                        .ManagedReadCursors(cursor),
                ),
            policy = policy,
        )
    val server = transport.server(owner.root, "catalog", rpc, interceptor())
    val caller = transport.channel(owner.root, "core", "catalog", server.port)
    val wrongCaller = transport.channel(owner.root, "mcp", "catalog", server.port)

    init {
        seed()
    }

    fun graphql(query: String): Map<String, Any?> {
        val effective = policy()
        val stub = spoolin.v1.CatalogServiceGrpc.newBlockingStub(caller)
        val views =
            com.spoolin.core.boundary.graphql.PolicyGraphqlViews(
                catalog =
                    com.spoolin.core.features.catalogview.read
                        .CatalogViewGraphql(stub, effective),
                managedCatalog =
                    proof?.let {
                        com.spoolin.core.features.catalogview.movie.ManagedCatalogGraphql(
                            stub,
                            com.spoolin.core.boundary.grpc
                                .ActorIssuer(it.signer),
                            policy,
                        )
                    },
            )
        val schema =
            java.nio.file.Files
                .readString(owner.repository.resolve("contracts/graphql/schema.graphql"))
        return com.spoolin.core.boundary.graphql
            .PolicyGraphql(
                schema,
                com.spoolin.core.features.policy
                    .PolicyProvider(effective),
                views,
            ).execute(
                mapOf("query" to query),
                proof?.let {
                    com.spoolin.core.boundary.http
                        .BrowserContext(it.issued.principal, null, null, null)
                },
            )
    }

    private fun interceptor(): com.spoolin.catalog.ServiceIdentityInterceptor {
        val authority = proof ?: return com.spoolin.catalog.ServiceIdentityInterceptor()
        val live =
            com.spoolin.catalog.boundary.grpc.CatalogActors(
                spoolin.v1.IdentityServiceGrpc.newBlockingStub(authority.identity),
            )
        val actors =
            com.spoolin.support.identity.ActorInterceptor(
                "catalog",
                com.spoolin.catalog.boundary.grpc.CatalogActors.protectedMethods,
                com.spoolin.support.identity
                    .ActorVerifier(mapOf("key_A" to authority.pair.public)),
                live::check,
            )
        return com.spoolin.catalog.ServiceIdentityInterceptor(actors)
    }

    fun managed(
        method: String,
        credential: UUID? = null,
    ): spoolin.v1.CatalogServiceGrpc.CatalogServiceBlockingStub {
        val authority = requireNotNull(proof)
        val now =
            java.time.Instant
                .now()
                .epochSecond
        val claims =
            authority.claims.copy(
                method = "/spoolin.v1.CatalogService/$method",
                credentialId = credential ?: authority.claims.credentialId,
                operationId = UUID.randomUUID(),
                issuedAt = now,
                expiresAt = now + 30,
                nonce = UUID.randomUUID().toString().replace("-", ""),
            )
        val headers =
            io.grpc.Metadata().apply {
                put(com.spoolin.support.identity.ActorRpc.metadata, authority.signer.sign(claims))
            }
        return spoolin.v1.CatalogServiceGrpc
            .newBlockingStub(caller)
            .withDeadlineAfter(3, TimeUnit.SECONDS)
            .withInterceptors(
                io.grpc.stub.MetadataUtils
                    .newAttachHeadersInterceptor(headers),
            )
    }

    private fun migrate() {
        owner.membership("catalog", "grant")
        owner.pool("catalog", "migrator").use { migration ->
            org.flywaydb.core.Flyway
                .configure()
                .dataSource(migration)
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

    private fun seed() {
        pool.connection.use { connection ->
            connection.executeBound(
                "INSERT INTO title(id,kind,display_title,synopsis,synopsis_language,release_year) VALUES(?,?,?,?,?,?)",
                title,
                "MOVIE",
                "Boundary movie",
                "Synthetic integration title.",
                "en",
                2025,
            )
            connection.executeBound(
                "INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language) VALUES(?,?,?,?,?,?)",
                archived,
                "MOVIE",
                "ARCHIVED",
                "Hidden title",
                "Archived fixture.",
                "en",
            )
            connection.executeBound("INSERT INTO person(id,name) VALUES(?,?)", person, "Boundary person")
            connection.executeBound(
                "INSERT INTO genre(id,label,normalized_label) VALUES(?,?,?)",
                genre,
                "Boundary drama",
                "boundary drama",
            )
            connection.executeBound("INSERT INTO title_genre(title_id,genre_id) VALUES(?,?)", title, genre)
            connection.executeBound(
                "INSERT INTO provenance_record(" +
                    "source,external_id,subject_kind,subject_id,retrieved_at,reuse_note,field_sources) " +
                    "VALUES(?,?,'TITLE',?,'2025-01-01T00:00:00Z',?,?::jsonb)",
                "synthetic-test",
                "boundary-movie",
                title,
                "Synthetic facts for this test only",
                SOURCE,
            )
        }
    }

    override fun close() {
        wrongCaller.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        caller.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        pool.close()
        if (proof == null) fixture.close()
    }

    companion object {
        private const val SOURCE = """{"title":{"field":"title","url":"https://example.invalid/fact",
            "permissionBasis":"Synthetic test permission","retrievedAt":"2025-01-01T00:00:00Z",
            "internalNote":"must-not-leak"}}"""
    }
}
