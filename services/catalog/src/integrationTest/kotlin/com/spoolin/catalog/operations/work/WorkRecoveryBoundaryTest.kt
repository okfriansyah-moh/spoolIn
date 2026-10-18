package com.spoolin.catalog.operations.work

import com.spoolin.catalog.MigrationRunner
import com.spoolin.catalog.boundary.bootstrap.OwnerFixture
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.WorkRecoveryAction
import com.spoolin.support.work.WorkRecoveryCommit
import com.spoolin.support.work.WorkRecoveryOperations
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.Status
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.netty.handler.ssl.ClientAuth
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Real PostgreSQL and mutual TLS with explicit synthetic downstream receipt fault injection. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkRecoveryBoundaryTest {
    private lateinit var fixture: OwnerFixture
    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var destination: WorkRecoveryDestination

    @Volatile private var receipt: Spoolin.Outcome? = null

    @Volatile private var failure = Status.NOT_FOUND
    private val observed = java.util.concurrent.CopyOnWriteArrayList<String>()

    @BeforeAll
    fun start() {
        fixture = OwnerFixture("t007-catalog-operator-recovery.json").start()
        fixture.membership("catalog", "grant")
        fixture.pool("catalog", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("catalog")
        fixture.membership("catalog", "revoke")
        startTransport()
    }

    private fun startTransport() {
        val tls =
            GrpcSslContexts
                .forServer(
                    fixture.root.resolve("media.crt").toFile(),
                    fixture.root.resolve("media.key").toFile(),
                ).trustManager(fixture.root.resolve("ca.crt").toFile())
                .clientAuth(ClientAuth.REQUIRE)
                .build()
        server =
            NettyServerBuilder
                .forAddress(InetSocketAddress("127.0.0.1", 0))
                .sslContext(tls)
                .addService(
                    object : MediaServiceGrpc.MediaServiceImplBase() {
                        override fun getOperation(
                            request: Spoolin.Id,
                            response: StreamObserver<Spoolin.Outcome>,
                        ) {
                            observed.add(request.id)
                            val value = receipt
                            if (value == null) {
                                response.onError(failure.asRuntimeException())
                            } else {
                                response.onNext(value)
                                response.onCompleted()
                            }
                        }
                    },
                ).build()
                .start()
        val client =
            GrpcSslContexts
                .forClient()
                .trustManager(fixture.root.resolve("ca.crt").toFile())
                .keyManager(fixture.root.resolve("catalog.crt").toFile(), fixture.root.resolve("catalog.key").toFile())
                .build()
        channel =
            NettyChannelBuilder
                .forAddress("127.0.0.1", server.port)
                .overrideAuthority("media")
                .sslContext(client)
                .build()
        destination = WorkRecoveryDestination(MediaServiceGrpc.newBlockingStub(channel))
    }

    @AfterAll
    fun stop() {
        if (::channel.isInitialized) channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        if (::server.isInitialized) server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        if (::fixture.isInitialized) fixture.close()
    }

    @Test
    fun `original completed Media receipt permits one generation with unchanged child effect id`() {
        val target = setup()
        receipt = outcome(target)
        observed.clear()
        assertTrue(recover(target))
        assertEquals(listOf(target.original.toString()), observed.toList())
        fixture.connection("catalog").use { connection ->
            val work = requireNotNull(WorkRecoveryOperations.inspect(connection, target.work))
            assertEquals("READY", work.state)
            assertEquals(1, work.generation)
            assertEquals(target.hold, work.operationId)
        }
        assertThrows(IllegalArgumentException::class.java) { recover(target) }
    }

    @Test
    fun `absent original receipt allows idempotent retry while unavailable destination refuses`() {
        receipt = null
        failure = Status.NOT_FOUND
        assertTrue(recover(setup()))
        failure = Status.UNAVAILABLE
        val uncertain = setup()
        assertThrows(io.grpc.StatusRuntimeException::class.java) { recover(uncertain) }
        assertExhausted(uncertain)
    }

    @Test
    fun `pending expired wrong identity and wrong version destination receipts cannot authorize recovery`() {
        val target = setup()
        val base = outcome(target)
        for (invalid in listOf(
            base.toBuilder().setState("PENDING").build(),
            base.toBuilder().setResultExpired(true).build(),
            base.toBuilder().setOperationId(UUID.randomUUID().toString()).build(),
            base.toBuilder().setResultId(UUID.randomUUID().toString()).build(),
            base.toBuilder().setVersion(3).build(),
        )) {
            receipt = invalid
            assertThrows(IllegalArgumentException::class.java) { recover(target) }
            assertExhausted(target)
        }
    }

    @Test
    fun `changed title selected hold is rechecked before receipt query`() {
        val target = setup()
        receipt = outcome(target)
        fixture.connection("catalog").use { connection ->
            connection.executeBound("UPDATE title SET attachment_id=NULL WHERE id=?", target.title)
        }
        observed.clear()
        assertThrows(IllegalArgumentException::class.java) { recover(target) }
        assertTrue(observed.isEmpty())
        assertExhausted(target)
    }

    @Test
    fun `release requires exact integer target version and an unselected hold`() {
        val target = setup()
        val original = WorkRequest("media-release", "ARTWORK", target.hold, Instant.MAX).id(target.hold)
        receipt = outcome(target).toBuilder().setOperationId(original.toString()).build()
        fixture.connection("catalog").use { connection ->
            connection.executeBound(
                "UPDATE durable_work SET kind='ARTWORK_RELEASE',payload=?::jsonb WHERE id=?",
                """{"subjectId":"${target.hold}","catalogVersion":"2"}""",
                target.work,
            )
            connection.executeBound("UPDATE title SET attachment_id=NULL WHERE id=?", target.title)
        }
        assertThrows(IllegalArgumentException::class.java) { recover(target) }
        assertExhausted(target)
        fixture.connection("catalog").use { connection ->
            connection.executeBound(
                "UPDATE durable_work SET payload=?::jsonb WHERE id=?",
                """{"subjectId":"${target.hold}","catalogVersion":2}""",
                target.work,
            )
        }
        assertTrue(recover(target))
        assertEquals(original.toString(), observed.last())
    }

    private data class Target(
        val hold: UUID,
        val title: UUID,
        val work: UUID,
    ) {
        val original = WorkRequest("media-confirm", "ARTWORK", hold, Instant.MAX).id(hold)
    }

    private fun setup(): Target {
        val target = Target(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        fixture.connection("catalog").use { connection ->
            connection.executeBound(
                "INSERT INTO operation_receipt(id,actor_scope,kind,request_hash,state,correlation_id,expires_at) " +
                    "VALUES (?,'SERVICE_TEST','ATTACH',?,'SUCCEEDED',?,clock_timestamp()+interval '1 hour')",
                target.hold,
                ByteArray(32),
                UUID.randomUUID(),
            )
            connection.executeBound(
                "INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,version,attachment_id) " +
                    "VALUES (?,'MOVIE','ACTIVE','Recovery boundary','Synthetic target','en',2,?)",
                target.title,
                target.hold,
            )
            connection.executeBound(
                "INSERT INTO artwork_attachment_request(id,title_id,actor_id,expected_version,asset_id,state,policy_revision) " +
                    "VALUES (?,?,?,1,?,'ATTACHED','test-policy')",
                target.hold,
                target.title,
                UUID.randomUUID(),
                UUID.randomUUID(),
            )
            connection.executeBound(
                "INSERT INTO durable_work(id,operation_id,kind,payload,state,max_attempts,deadline_at,correlation_id," +
                    "base_delay_ms,cap_delay_ms,backoff_multiplier) VALUES (?,?,'ARTWORK_CONFIRM',?::jsonb,'EXHAUSTED',3," +
                    "clock_timestamp()-interval '1 second',?,500,5000,2)",
                target.work,
                target.hold,
                """{"subjectId":"${target.hold}"}""",
                UUID.randomUUID(),
            )
        }
        return target
    }

    private fun outcome(target: Target): Spoolin.Outcome =
        Spoolin.Outcome
            .newBuilder()
            .setOperationId(target.original.toString())
            .setState("SUCCEEDED")
            .setResultId(target.hold.toString())
            .setVersion(2)
            .build()

    private fun recover(target: Target): Boolean =
        fixture.connection("catalog").use { connection ->
            connection.autoCommit = false
            try {
                val result =
                    WorkRecoveryOperations.recover(
                        connection,
                        WorkRecoveryAction(
                            target.work,
                            0,
                            0,
                            WorkRecoveryCommit("local-uid:1001", "Original destination receipt inspected", UUID.randomUUID()),
                        ),
                        Path.of("/unused-media-boundary"),
                        destination,
                    )
                connection.commit()
                result
            } finally {
                connection.rollback()
            }
        }

    private fun assertExhausted(target: Target) =
        fixture.connection("catalog").use {
            assertEquals("EXHAUSTED", WorkRecoveryOperations.inspect(it, target.work)?.state)
            assertEquals(
                0,
                it
                    .queryBound(
                        "SELECT count(*) FROM operational_event WHERE work_id=? AND event_type='RECOVERY'",
                        listOf(target.work),
                    ) { row -> row.getInt(1) }
                    .single(),
            )
        }
}
