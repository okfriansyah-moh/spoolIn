package com.spoolin.catalog.recovery.lifecycle

import com.spoolin.catalog.MigrationRunner
import com.spoolin.catalog.ServiceIdentityInterceptor
import com.spoolin.catalog.boundary.bootstrap.OwnerFixture
import com.spoolin.catalog.features.lifecycle.DeletionImpact
import com.spoolin.catalog.features.lifecycle.ImpactToken
import com.spoolin.catalog.features.lifecycle.MovieLifecycle
import com.spoolin.catalog.features.lifecycle.PersonLifecycle
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorVerifier
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import io.netty.handler.ssl.ClientAuth
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Real PostgreSQL/mTLS and production lifecycle handlers; synthetic signer admission isolates owner consistency. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LifecycleBoundaryTest {
    private lateinit var fixture: OwnerFixture
    private lateinit var pool: com.zaxxer.hikari.HikariDataSource
    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var signer: ActorSigner
    private val actor = UUID.randomUUID()
    private val credential = UUID.randomUUID()
    private val correlation = UUID.randomUUID()

    @BeforeAll
    fun start() {
        fixture = OwnerFixture("catalog-lifecycle-boundary.json").start()
        fixture.membership("catalog", "grant")
        fixture.pool("catalog", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("catalog")
        fixture.membership("catalog", "revoke")
        pool = fixture.pool("catalog")
        val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        signer = ActorSigner("test", keys.private)
        val receipts = OwnerReceipts(pool, IntentHashes(mapOf("test" to ByteArray(32) { 31 }), "test"))
        val tokens = ImpactToken("test", ByteArray(32) { 41 })
        val movies = MovieLifecycle(receipts) { "p1" }
        val people = PersonLifecycle(pool, receipts, tokens) { "p1" }
        val impacts = DeletionImpact(pool, tokens)
        val rpc =
            object : CatalogServiceGrpc.CatalogServiceImplBase() {
                override fun movieLifecycle(
                    request: Spoolin.Lifecycle,
                    response: StreamObserver<Spoolin.Outcome>,
                ) = movies.apply(request, response)

                override fun personLifecycle(
                    request: Spoolin.Lifecycle,
                    response: StreamObserver<Spoolin.Outcome>,
                ) = people.apply(request, response)

                override fun deletionImpact(
                    request: Spoolin.Id,
                    response: StreamObserver<Spoolin.Impact>,
                ) = impacts.preview(request, response)
            }
        val methods = setOf("MovieLifecycle", "PersonLifecycle", "DeletionImpact").map { "spoolin.v1.CatalogService/$it" }.toSet()
        val actors =
            ActorInterceptor("catalog", methods, ActorVerifier(mapOf("test" to keys.public)), { claims ->
                require(claims.accountId == actor && claims.credentialId == credential && claims.authEpoch == 1L)
            })
        val tls =
            GrpcSslContexts
                .forServer(fixture.root.resolve("catalog.crt").toFile(), fixture.root.resolve("catalog.key").toFile())
                .trustManager(fixture.root.resolve("ca.crt").toFile())
                .clientAuth(ClientAuth.REQUIRE)
                .build()
        server =
            NettyServerBuilder
                .forAddress(InetSocketAddress("127.0.0.1", 0))
                .sslContext(tls)
                .addService(ServerInterceptors.intercept(rpc, ServiceIdentityInterceptor(actors)))
                .build()
                .start()
        val clientTls =
            GrpcSslContexts
                .forClient()
                .trustManager(fixture.root.resolve("ca.crt").toFile())
                .keyManager(fixture.root.resolve("core.crt").toFile(), fixture.root.resolve("core.key").toFile())
                .build()
        channel =
            NettyChannelBuilder
                .forAddress("127.0.0.1", server.port)
                .overrideAuthority("catalog")
                .sslContext(clientTls)
                .build()
    }

    @AfterAll
    fun stop() {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        pool.close()
        fixture.close()
    }

    @Test
    fun `all twenty five credit tuples bind deletion and fresh confirmation commits tombstones and audit once`() {
        val seeded = seed(25)
        val before = preview(seeded.person)
        assertEquals(25, before.affectedCreditCount)
        assertEquals(25, before.affectedTitlesCount)
        assertEquals(3L, before.version)
        assertTrue(before.expiresAt.seconds > Instant.now().epochSecond)
        pool.committed { it.executeBound("UPDATE credit SET version=version+1 WHERE id=?", seeded.credits.last()) }
        val deniedOperation = UUID.randomUUID()
        val denied = request(seeded.person, "DELETE", 3, before.token, deniedOperation)
        assertEquals("IMPACT_CHANGED", stub("PersonLifecycle", deniedOperation).personLifecycle(denied).problem.code)
        assertEquals(25, count("SELECT count(*) FROM credit WHERE person_id=?", seeded.person))
        assertEquals(0, count("SELECT count(*) FROM operation_receipt WHERE id=?", deniedOperation))
        val fresh = preview(seeded.person)
        val operation = UUID.randomUUID()
        val command = request(seeded.person, "DELETE", 3, fresh.token, operation)
        assertEquals("SUCCEEDED", stub("PersonLifecycle", operation).personLifecycle(command).state)
        assertEquals("SUCCEEDED", stub("PersonLifecycle", operation).personLifecycle(command).state)
        assertEquals(0, count("SELECT count(*) FROM credit WHERE person_id=?", seeded.person))
        assertEquals(1, count("SELECT count(*) FROM person WHERE id=? AND status='DELETED' AND version=4", seeded.person))
        assertEquals(1, count("SELECT count(*) FROM catalog_audit_event WHERE operation_id=? AND action='DELETE_PERSON'", operation))
        assertEquals(26, count("SELECT count(*) FROM seed_member WHERE seed_id=? AND removed_at IS NOT NULL", seeded.seed))
        assertEquals(1, count("SELECT count(*) FROM provenance_record WHERE subject_id=? AND deleted_at IS NOT NULL", seeded.person))
        seeded.titles.forEach { assertEquals(1, count("SELECT count(*) FROM title WHERE id=? AND version=5", it)) }
    }

    @Test
    fun `movie deletion cascades relationships and durably queues selected artwork release while TV rejects writes`() {
        val seeded = seed(1)
        val title = seeded.titles.single()
        val hold = attach(title)
        val operation = UUID.randomUUID()
        val command = request(title, "DELETE", 5, operation = operation)
        assertEquals("SUCCEEDED", stub("MovieLifecycle", operation).movieLifecycle(command).state)
        assertEquals("SUCCEEDED", stub("MovieLifecycle", operation).movieLifecycle(command).state)
        assertEquals(0, count("SELECT count(*) FROM credit WHERE title_id=?", title))
        assertEquals(0, count("SELECT count(*) FROM title_genre WHERE title_id=?", title))
        assertEquals(
            1,
            count("SELECT count(*) FROM title WHERE id=? AND status='DELETED' AND asset_id IS NULL AND attachment_id IS NULL", title),
        )
        assertEquals(1, count("SELECT count(*) FROM durable_work WHERE operation_id=? AND kind='ARTWORK_RELEASE'", operation))
        assertEquals(1, count("SELECT count(*) FROM catalog_audit_event WHERE operation_id=? AND action='DELETE_MOVIE'", operation))
        assertEquals(2, count("SELECT count(*) FROM seed_member WHERE seed_id=? AND removed_at IS NOT NULL", seeded.seed))
        assertEquals(1, count("SELECT count(*) FROM provenance_record WHERE subject_id=? AND deleted_at IS NOT NULL", title))
        assertEquals(1, count("SELECT count(*) FROM durable_work WHERE payload->>'subjectId'=?", hold.toString()))
        val tv = seed(1, "TV").titles.single()
        listOf("ARCHIVE", "RESTORE", "DELETE").forEach { action ->
            val id = UUID.randomUUID()
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                assertThrows(StatusRuntimeException::class.java) {
                    stub("MovieLifecycle", id).movieLifecycle(request(tv, action, 4, operation = id))
                }.status.code,
            )
        }
        assertEquals(1, count("SELECT count(*) FROM title WHERE id=? AND kind='TV' AND status='ARCHIVED' AND version=4", tv))
    }

    @Test
    fun `actual complete database preview exceeds response envelope without a partial token or mutation`() {
        val seeded = seed(200, titleName = "界".repeat(300))
        val actual =
            Spoolin.Impact
                .newBuilder()
                .setTargetId(seeded.person.toString())
                .setVersion(3)
                .setAffectedCreditCount(200)
                .addAllAffectedTitles(List(200) { "界".repeat(300) })
                .build()
        assertTrue(actual.serializedSize > 131072)
        assertEquals(
            Status.Code.RESOURCE_EXHAUSTED,
            assertThrows(StatusRuntimeException::class.java) {
                preview(seeded.person)
            }.status.code,
        )
        assertEquals(200, count("SELECT count(*) FROM credit WHERE person_id=?", seeded.person))
        assertEquals(1, count("SELECT count(*) FROM person WHERE id=? AND status='ARCHIVED' AND version=3", seeded.person))
    }

    @Test
    fun `metadata and title lifecycle changes invalidate existing person impact version`() {
        val seeded = seed(1)
        val initial = preview(seeded.person)
        pool.committed { it.executeBound("UPDATE person SET biography='Changed',version=version+1 WHERE id=?", seeded.person) }
        val operation = UUID.randomUUID()
        assertEquals(
            "IMPACT_CHANGED",
            stub("PersonLifecycle", operation).personLifecycle(request(seeded.person, "DELETE", 4, initial.token, operation)).problem.code,
        )
        val refreshed = preview(seeded.person)
        val title = seeded.titles.single()
        pool.committed { it.executeBound("UPDATE title SET display_title='Renamed',version=version+1 WHERE id=?", title) }
        val next = UUID.randomUUID()
        assertEquals(
            "IMPACT_CHANGED",
            stub("PersonLifecycle", next).personLifecycle(request(seeded.person, "DELETE", 4, refreshed.token, next)).problem.code,
        )
        assertEquals(1, count("SELECT count(*) FROM person WHERE id=? AND impact_version>=9", seeded.person))
    }

    private fun preview(person: UUID): Spoolin.Impact =
        stub("DeletionImpact", null).deletionImpact(
            Spoolin.Id
                .newBuilder()
                .setId(person.toString())
                .build(),
        )

    private fun stub(
        method: String,
        operation: UUID?,
    ): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val now = Instant.now().epochSecond
        val claims =
            ActorClaims(
                "catalog",
                "/spoolin.v1.CatalogService/$method",
                actor,
                credential,
                null,
                "MAINTAINER",
                1,
                now,
                now + 30,
                Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(16) { 7 }),
                correlation,
                operation,
            )
        val headers = Metadata().apply { put(ActorRpc.metadata, signer.sign(claims)) }
        return CatalogServiceGrpc
            .newBlockingStub(channel)
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
    }

    private fun request(
        target: UUID,
        action: String,
        version: Long,
        token: String? = null,
        operation: UUID,
    ): Spoolin.Lifecycle =
        Spoolin.Lifecycle
            .newBuilder()
            .setTarget(
                Spoolin.VersionedId
                    .newBuilder()
                    .setId(target.toString())
                    .setExpectedVersion(version)
                    .setContext(
                        Spoolin.Context
                            .newBuilder()
                            .setOperationId(operation.toString())
                            .setCorrelationId(correlation.toString())
                            .setPolicyRevision("p1"),
                    ),
            ).setAction(action)
            .setConfirmed(true)
            .also { if (token != null) it.impactToken = token }
            .build()

    private fun count(
        sql: String,
        argument: Any,
    ): Int =
        pool.connection.use { connection ->
            connection.queryBound(sql, listOf(argument)) { it.getInt(1) }.single()
        }

    private data class Seeded(
        val person: UUID,
        val titles: List<UUID>,
        val credits: List<UUID>,
        val seed: String,
    )

    private fun seed(
        creditCount: Int,
        kind: String = "MOVIE",
        titleName: String = "Related",
    ): Seeded {
        val person = UUID.randomUUID()
        val titles = List(creditCount) { UUID.randomUUID() }
        val credits = List(creditCount) { UUID.randomUUID() }
        val seed = "fixture-${UUID.randomUUID()}"
        pool.committed { connection ->
            val initialization = UUID.randomUUID()
            connection.executeBound(
                "INSERT INTO operation_receipt " +
                    "(id,actor_scope,kind,receipt_type,request_hash,state,correlation_id,seed_version,seed_snapshot_hash," +
                    "seed_movie_count,seed_tv_count,seed_summary) " +
                    "VALUES (?,'SERVICE_INITIALIZER','INITIALIZE_CATALOG','CATALOG_INITIALIZATION',?,'PENDING',?,?,?,0,0,'{}')",
                initialization,
                ByteArray(32),
                correlation,
                seed,
                ByteArray(32),
            )
            connection.executeBound(
                "INSERT INTO person(id,name,status,version,impact_version) VALUES (?,'Fixture person','ARCHIVED',3,7)",
                person,
            )
            marker(connection, seed, person, "PERSON")
            titles.forEachIndexed { index, title ->
                connection.executeBound(
                    "INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,version) " +
                        "VALUES (?,?,'ARCHIVED',?,'Fixture synopsis','und',4)",
                    title,
                    kind,
                    titleName,
                )
                connection.executeBound(
                    "INSERT INTO credit(id,title_id,person_id,role_id) " +
                        "VALUES (?,?,?,'b0000001-0000-4000-8000-000000000001')",
                    credits[index],
                    title,
                    person,
                )
                marker(connection, seed, title, "TITLE")
                marker(connection, seed, credits[index], "CREDIT")
            }
        }
        return Seeded(person, titles, credits, seed)
    }

    private fun marker(
        connection: java.sql.Connection,
        seed: String,
        subject: UUID,
        kind: String,
    ) {
        connection.executeBound(
            "INSERT INTO seed_member(seed_id,member_key,subject_kind,subject_id,content_hash) VALUES (?,?,?,?,?)",
            seed,
            subject.toString(),
            kind,
            subject,
            ByteArray(32),
        )
        if (kind in setOf("PERSON", "TITLE")) {
            connection.executeBound(
                "INSERT INTO provenance_record " +
                    "(source,external_id,subject_kind,subject_id,retrieved_at,reuse_note,field_sources) " +
                    "VALUES ('synthetic',?,?,?,CURRENT_TIMESTAMP,'Synthetic boundary fixture','{}')",
                subject.toString(),
                kind,
                subject,
            )
        }
    }

    private fun attach(title: UUID): UUID {
        val hold = UUID.randomUUID()
        pool.committed { connection ->
            connection.executeBound(
                "INSERT INTO operation_receipt(id,actor_scope,kind,request_hash,state,correlation_id,expires_at) " +
                    "VALUES (?,?,'BeginArtwork',?,'SUCCEEDED',?,CURRENT_TIMESTAMP+interval '7 days')",
                hold,
                "account:$actor",
                ByteArray(32),
                correlation,
            )
            connection.executeBound(
                "INSERT INTO artwork_attachment_request " +
                    "(id,title_id,actor_id,expected_version,asset_id,state,policy_revision) VALUES (?,?,?,4,?,'ATTACHED','p1')",
                hold,
                title,
                actor,
                UUID.randomUUID(),
            )
            connection.executeBound("UPDATE title SET asset_id=?,attachment_id=?,version=5 WHERE id=?", UUID.randomUUID(), hold, title)
            val genre = UUID.randomUUID()
            connection.executeBound("INSERT INTO genre(id,label,normalized_label) VALUES (?,'Fixture',?)", genre, genre.toString())
            connection.executeBound("INSERT INTO title_genre(title_id,genre_id) VALUES (?,?)", title, genre)
        }
        return hold
    }
}
