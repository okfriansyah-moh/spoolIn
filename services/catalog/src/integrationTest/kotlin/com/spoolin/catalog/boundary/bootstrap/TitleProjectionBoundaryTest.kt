package com.spoolin.catalog.boundary.bootstrap

import com.spoolin.catalog.ServiceIdentityInterceptor
import com.spoolin.catalog.boundary.grpc.PublicCatalogReads
import com.spoolin.catalog.boundary.grpc.PublicReadContext
import com.spoolin.catalog.features.movie.ManagedMovieReads
import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.CatalogCursor
import com.spoolin.catalog.features.search.PeopleCursor
import com.spoolin.catalog.features.search.TitleContent
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.features.taxonomy.read.TaxonomyReads
import com.spoolin.catalog.operations.audit.AuditReads
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorVerifier
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import io.netty.handler.ssl.ClientAuth
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.sql.SQLException
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Real production schema, PostgreSQL arrays and row locks, plus both title projections over mutual TLS. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TitleProjectionBoundaryTest {
    private lateinit var fixture: OwnerFixture
    private lateinit var pool: com.zaxxer.hikari.HikariDataSource
    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var signer: ActorSigner
    private val actor = UUID.randomUUID()
    private val credential = UUID.randomUUID()
    private val full = UUID.randomUUID()
    private val sparse = UUID.randomUUID()
    private val asset = UUID.randomUUID()
    private val yearOnly = UUID.randomUUID()
    private lateinit var search: TitleSearchSql

    @BeforeAll
    fun start() {
        fixture = OwnerFixture("catalog-title-projection-boundary.json").start()
        fixture.membership("catalog", "grant")
        fixture.pool("catalog", "migrator").use { source ->
            // Bootstrap tests package disposable V1 resources; select the maintained owner migrations explicitly.
            Flyway
                .configure()
                .dataSource(source)
                .schemas("appdata")
                .defaultSchema("appdata")
                .locations("filesystem:${fixture.repository}/services/catalog/src/main/resources/db/migration")
                .createSchemas(false)
                .cleanDisabled(true)
                .load()
                .migrate()
        }
        fixture.grants("catalog")
        fixture.membership("catalog", "revoke")
        pool = fixture.pool("catalog")
        seedRows()
        search = TitleSearchSql(pool)
        val provenance = ProvenanceReads(pool)
        val managed = ManagedMovieReads(pool, provenance, search, AuditReads(pool))
        val public =
            PublicCatalogReads(
                search,
                PersonSearch(pool),
                TaxonomyReads(pool),
                provenance,
                PublicReadContext(
                    CatalogCursor(ByteArray(32) { 11 }),
                    PeopleCursor(ByteArray(32) { 12 }),
                    Clock.systemUTC(),
                ) {
                    PolicyEnvironment.resolve(System::getenv)
                },
            )
        val rpc =
            object : CatalogServiceGrpc.CatalogServiceImplBase() {
                override fun managedTitle(
                    request: Spoolin.Id,
                    response: StreamObserver<Spoolin.Title>,
                ) = managed.managedTitle(request, response)

                override fun getTitle(
                    request: Spoolin.Id,
                    response: StreamObserver<Spoolin.Title>,
                ) = public.getTitle(request, response)
            }
        val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        signer = ActorSigner("test", keys.private)
        val methods = setOf("spoolin.v1.CatalogService/ManagedTitle")
        val actors =
            ActorInterceptor("catalog", methods, ActorVerifier(mapOf("test" to keys.public)), { claims ->
                require(claims.accountId == actor && claims.credentialId == credential && claims.authEpoch == 1L)
            })
        server =
            NettyServerBuilder
                .forAddress(InetSocketAddress("127.0.0.1", 0))
                .sslContext(
                    GrpcSslContexts
                        .forServer(
                            fixture.root.resolve("catalog.crt").toFile(),
                            fixture.root.resolve("catalog.key").toFile(),
                        ).trustManager(fixture.root.resolve("ca.crt").toFile())
                        .clientAuth(ClientAuth.REQUIRE)
                        .build(),
                ).addService(ServerInterceptors.intercept(rpc, ServiceIdentityInterceptor(actors)))
                .build()
                .start()
        channel =
            NettyChannelBuilder
                .forAddress("127.0.0.1", server.port)
                .overrideAuthority("catalog")
                .sslContext(
                    GrpcSslContexts
                        .forClient()
                        .trustManager(fixture.root.resolve("ca.crt").toFile())
                        .keyManager(
                            fixture.root.resolve("core.crt").toFile(),
                            fixture.root.resolve("core.key").toFile(),
                        ).build(),
                ).build()
    }

    private fun seedRows() {
        pool.connection.use { connection ->
            connection.executeBound(
                "INSERT INTO title(id,kind,status,display_title,original_title,synopsis,synopsis_language," +
                    "original_language,countries,release_year,release_month,release_day,release_context,runtime_minutes," +
                    "tv_status,end_year,end_month,end_day,asset_id,version) " +
                    "VALUES (?,'TV','ACTIVE',?,?,?,?,?,?::text[],2024,2,29,?,95,'ENDED',2025,3,4,?,7)",
                full,
                "River",
                "Rivière",
                "A journey",
                "en",
                "fr",
                connection.createArrayOf("text", arrayOf("FR", "US")),
                "Premiere",
                asset,
            )
            connection.executeBound(
                "INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,countries,release_year,version) " +
                    "VALUES (?,'MOVIE','ACTIVE','Year only','Unknown month and day','en',ARRAY['GB'],2021,3)",
                yearOnly,
            )
            connection.executeBound(
                "INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,countries,version) " +
                    "VALUES (?,'MOVIE','ARCHIVED','Hidden river','An archived journey','en','{}'::text[],0)",
                sparse,
            )
        }
    }

    @AfterAll
    fun stop() {
        if (::channel.isInitialized) channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        if (::server.isInitialized) server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        if (::pool.isInitialized) pool.close()
        if (::fixture.isInitialized) fixture.close()
    }

    private fun stub(managed: Boolean): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val base = CatalogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS)
        if (!managed) return base
        val now = Instant.now().epochSecond
        val claims =
            ActorClaims(
                "catalog",
                "/spoolin.v1.CatalogService/ManagedTitle",
                actor,
                credential,
                null,
                "MAINTAINER",
                1,
                now,
                now + 30,
                Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(16) { 21 }),
                UUID.randomUUID(),
                null,
            )
        val headers = Metadata().apply { put(ActorRpc.metadata, signer.sign(claims)) }
        return base.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
    }

    private fun id(value: UUID) =
        Spoolin.Id
            .newBuilder()
            .setId(value.toString())
            .build()

    @Test
    fun `active full title maps every field consistently through SQL and public and managed TLS projections`() {
        val row = requireNotNull(search.findById(full))
        assertEquals(full, row.id)
        assertEquals("TV", row.kind)
        assertEquals("ACTIVE", row.status)
        assertEquals("River", row.displayTitle)
        assertEquals("Rivière", row.originalTitle)
        assertEquals("A journey", row.synopsis)
        assertEquals("en", row.synopsisLanguage)
        assertEquals("fr", row.originalLanguage)
        assertEquals(listOf("FR", "US"), row.countries)
        assertEquals(2024, row.releaseYear)
        assertEquals(2, row.releaseMonth)
        assertEquals(29, row.releaseDay)
        assertEquals("Premiere", row.releaseContext)
        assertEquals(95, row.runtimeMinutes)
        assertEquals("ENDED", row.tvStatus)
        assertEquals(2025, row.endYear)
        assertEquals(3, row.endMonth)
        assertEquals(4, row.endDay)
        assertEquals(asset, row.assetId)
        assertEquals(7L, row.version)
        val visible = stub(false).getTitle(id(full))
        assertEquals(visible, stub(true).managedTitle(id(full)))
        assertEquals(listOf("FR", "US"), visible.values.countriesList)
        assertEquals(2024, visible.values.release.year)
        assertEquals(29, visible.values.release.day)
        assertEquals(asset.toString(), visible.assetId)
        assertEquals(7L, visible.version)
        fixture.record("full-title-projection", mapOf("sqlFields" to 20, "publicAndManagedMatch" to true))
    }

    @Test
    fun `archived sparse title preserves SQL nulls and empty arrays and stays hidden from public reads`() {
        assertNull(search.findById(sparse))
        val row =
            pool.connection.use { connection ->
                connection
                    .queryBound("SELECT ${TitleSearchSql.TITLE_COLUMNS} FROM title t WHERE t.id=?", listOf(sparse)) {
                        TitleSearchSql.readRow(it)
                    }.single()
            }
        assertEquals("MOVIE", row.kind)
        assertEquals("ARCHIVED", row.status)
        assertEquals("Hidden river", row.displayTitle)
        assertEquals(emptyList<String>(), row.countries)
        assertEquals(0L, row.version)
        listOf(
            row.originalTitle,
            row.originalLanguage,
            row.releaseYear,
            row.releaseMonth,
            row.releaseDay,
            row.releaseContext,
            row.runtimeMinutes,
            row.tvStatus,
            row.endYear,
            row.endMonth,
            row.endDay,
            row.assetId,
        ).forEach { assertNull(it) }
        assertEquals(Spoolin.Title.getDefaultInstance(), stub(false).getTitle(id(sparse)))
        val managed = stub(true).managedTitle(id(sparse))
        assertEquals(sparse.toString(), managed.id)
        assertEquals("ARCHIVED", managed.state)
        assertEquals(emptyList<String>(), managed.values.countriesList)
        assertFalse(managed.values.hasOriginalTitle())
        assertFalse(managed.values.hasOriginalLanguage())
        assertFalse(managed.values.hasRuntimeMinutes())
        assertFalse(managed.values.release.hasYear())
        assertFalse(managed.values.release.hasMonth())
        assertFalse(managed.values.release.hasDay())
        assertFalse(managed.hasReleaseContext())
        assertFalse(managed.hasTvStatus())
        assertFalse(managed.hasAssetId())
        assertFalse(managed.tvEnd.hasYear())
        assertFalse(managed.tvEnd.hasMonth())
        assertFalse(managed.tvEnd.hasDay())
    }

    @Test
    fun `known release year does not invent a month or day in either wire projection`() {
        val row = requireNotNull(search.findById(yearOnly))
        assertEquals(2021, row.releaseYear)
        assertNull(row.releaseMonth)
        assertNull(row.releaseDay)
        assertEquals(listOf("GB"), row.countries)
        val public = stub(false).getTitle(id(yearOnly))
        assertEquals(public, stub(true).managedTitle(id(yearOnly)))
        assertEquals(2021, public.values.release.year)
        assertFalse(public.values.release.hasMonth())
        assertFalse(public.values.release.hasDay())
        assertEquals(3L, public.version)
    }

    @Test
    fun `edit projection retains partial dates and its update lock excludes a concurrent writer`() {
        pool.connection.use { connection ->
            connection.autoCommit = false
            try {
                val row =
                    connection
                        .queryBound(
                            "SELECT kind,status,display_title,original_title,synopsis,synopsis_language," +
                                "original_language,countries,release_year,release_month,release_day," +
                                "runtime_minutes,version FROM title WHERE id = ? FOR UPDATE",
                            listOf(full),
                        ) { TitleContent.read(it) }
                        .single()
                assertEquals("TV", row.kind)
                assertEquals("ACTIVE", row.status)
                assertEquals("River", row.displayTitle)
                assertEquals("Rivière", row.originalTitle)
                assertEquals("A journey", row.synopsis)
                assertEquals("en", row.synopsisLanguage)
                assertEquals("fr", row.originalLanguage)
                assertEquals(listOf("FR", "US"), row.countries)
                assertEquals(2024, row.releaseYear)
                assertEquals(2, row.releaseMonth)
                assertEquals(29, row.releaseDay)
                assertEquals(95, row.runtimeMinutes)
                assertEquals(7L, row.version)
                pool.connection.use { competing ->
                    val failure =
                        assertThrows(SQLException::class.java) {
                            competing.queryBound(
                                "SELECT id FROM title WHERE id=? FOR UPDATE NOWAIT",
                                listOf(full),
                            ) { it.getObject(1) }
                        }
                    assertEquals("55P03", failure.sqlState)
                }
            } finally {
                connection.rollback()
            }
        }
        pool.connection.use { connection ->
            val sparseEdit =
                connection
                    .queryBound(
                        "SELECT ${TitleSearchSql.TITLE_COLUMNS} FROM title t WHERE t.id=? FOR UPDATE",
                        listOf(sparse),
                    ) { TitleContent.read(it) }
                    .single()
            assertEquals("ARCHIVED", sparseEdit.status)
            assertEquals(emptyList<String>(), sparseEdit.countries)
            listOf(
                sparseEdit.originalTitle,
                sparseEdit.originalLanguage,
                sparseEdit.releaseYear,
                sparseEdit.releaseMonth,
                sparseEdit.releaseDay,
                sparseEdit.runtimeMinutes,
            ).forEach { assertNull(it) }
        }
    }
}
