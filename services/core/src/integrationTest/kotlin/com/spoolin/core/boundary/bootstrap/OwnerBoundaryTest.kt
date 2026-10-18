package com.spoolin.core.boundary.bootstrap

import com.spoolin.core.Application
import com.spoolin.core.MigrationRunner
import com.spoolin.core.SchemaReadiness
import com.spoolin.core.ServiceIdentityInterceptor
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.ProtectionMetrics
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.IntentHashes
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.health.v1.HealthGrpc
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.ssl.SslBundles
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.sql.SQLException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OwnerBoundaryTest {
    private val owner = "core"
    private lateinit var fixture: OwnerFixture

    @BeforeAll
    fun start() {
        fixture = OwnerFixture()
        try {
            fixture.start()
        } catch (failure: Exception) {
            fixture.close()
            throw failure
        }
    }

    @AfterAll
    fun cleanup() = fixture.close()

    @BeforeEach
    fun reset() {
        fixture.sql(
            owner,
            "DROP TABLE IF EXISTS appdata.operation_receipt, appdata.operational_event, " +
                "appdata.catalog_audit_event, appdata.private_key_material, " +
                "appdata.flyway_schema_history, appdata.failed_fixture, appdata.crypto_key_version CASCADE",
        )
    }

    @Test
    fun mountedPasswordsAuthenticateWithoutEscapeChanges() {
        listOf("core", "catalog", "media").forEach { databaseOwner ->
            listOf("runtime", "migrator", "maintenance", "backup").forEach { role ->
                assertPasswordAuthenticates(databaseOwner, role)
                fixture.record(
                    "special-character-authentication",
                    mapOf(
                        "databaseOwner" to databaseOwner,
                        "role" to role,
                        "authenticated" to true,
                        "patternId" to fixture.passwordPatternIds.getValue("$databaseOwner-$role"),
                    ),
                )
            }
        }
    }

    private fun assertPasswordAuthenticates(
        databaseOwner: String,
        role: String,
    ) {
        fixture.connection(databaseOwner, role).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT current_user").use { result ->
                    assertTrue(result.next())
                    assertEquals("spoolin_${databaseOwner}_$role", result.getString(1))
                }
            }
        }
    }

    @Test
    fun ownershipAndIndependentReads() {
        migrate()
        val id = UUID.randomUUID()
        fixture.connection(owner).use { connection ->
            connection.prepareStatement("INSERT INTO appdata.operation_receipt(id) VALUES (?)").use {
                it.setObject(1, id)
                assertEquals(1, it.executeUpdate())
            }
            assertDenied(connection, "CREATE ROLE escaped_runtime")
            assertDenied(connection, "CREATE EXTENSION IF NOT EXISTS hstore")
            assertDenied(connection, "CREATE TABLE appdata.escaped_runtime(id integer)")
            assertDenied(connection, "CREATE TEMP TABLE escaped_runtime(id integer)")
            assertDenied(connection, "UPDATE appdata.flyway_schema_history SET description='changed'")
            assertDenied(connection, "DELETE FROM appdata.flyway_schema_history")
            assertDenied(connection, "SELECT * FROM appdata.private_key_material")
            if (owner != "catalog") assertDenied(connection, "SELECT * FROM appdata.catalog_audit_event")
            val auditTables = if (owner == "catalog") listOf("operational_event", "catalog_audit_event") else listOf("operational_event")
            auditTables.forEach { table ->
                connection.prepareStatement("INSERT INTO appdata.$table(id) VALUES (?)").use {
                    it.setObject(1, UUID.randomUUID())
                    assertEquals(1, it.executeUpdate())
                }
                assertDenied(connection, "UPDATE appdata.$table SET id=id")
                assertDenied(connection, "DELETE FROM appdata.$table")
                assertDenied(connection, "TRUNCATE appdata.$table")
            }
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT ssl,version FROM pg_stat_ssl WHERE pid=pg_backend_pid()").use { rows ->
                    assertTrue(rows.next())
                    assertTrue(rows.getBoolean(1))
                    assertTrue(rows.getString(2).startsWith("TLS"))
                }
            }
        }
        freshRead(id)
        if (owner == "core") assertNonsecretKeyMetadata()
        fixture.connection(owner, "backup").use { connection -> assertDenied(connection, "DELETE FROM appdata.operation_receipt") }
        fixture.connection(owner, "maintenance").use { connection ->
            assertDenied(connection, "CREATE TABLE appdata.maintenance_escape(id integer)")
            assertDenied(connection, "DELETE FROM appdata.catalog_audit_event")
            assertDenied(connection, "SELECT * FROM appdata.private_key_material")
        }
        connectionMatrix()
        assertCredentialAndTlsDenial()
        assertScramAndMembership()
    }

    @Test
    fun migrationReadinessAndTransactionalFailure() {
        fixture.pool(owner).use { runtime -> assertFalse(SchemaReadiness(runtime, "1", "1").ready()) }
        assertThrows(IllegalStateException::class.java) { application().close() }
        migrate()
        fixture.pool(owner).use { runtime ->
            assertTrue(SchemaReadiness(runtime, "1", "1").ready())
            assertFalse(SchemaReadiness(runtime, "2", "3").ready())
            assertFalse(SchemaReadiness(runtime, "0", "0").ready())
            val migration = Path.of(requireNotNull(javaClass.getResource("/db/migration/V1__disposable_boundary.sql")).toURI())
            val bytes = Files.readAllBytes(migration)
            try {
                Files.writeString(migration, String(bytes) + "\nSELECT 1;\n")
                assertFalse(SchemaReadiness(runtime, "1", "1").ready())
            } finally {
                Files.write(migration, bytes)
            }
            assertTrue(SchemaReadiness(runtime, "1", "1").ready())
        }
        assertTransactionalRollback()
        fixture.record(
            "schema-failure-boundaries",
            mapOf(
                "missingStartupRejected" to true,
                "checksumDriftRejected" to true,
                "schemaRangeRejected" to true,
                "transactionalDdlAbsent" to true,
                "successfulVersion" to "1",
            ),
        )
    }

    @Test
    fun serializedMigrationAndBoundedLock() {
        fixture.membership(owner, "grant")
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            fixture.pool(owner, "migrator").use { first ->
                fixture.pool(owner, "migrator").use { second ->
                    val running =
                        executor.submit {
                            MigrationRunner(first).migrate {
                                held.countDown()
                                check(release.await(150, TimeUnit.SECONDS))
                            }
                        }
                    assertTrue(held.await(10, TimeUnit.SECONDS))
                    fixture.connection(owner).use { observer ->
                        observer.createStatement().use {
                            it.executeQuery("SELECT pg_try_advisory_lock(1936749166)").use { rows ->
                                assertTrue(rows.next())
                                assertFalse(rows.getBoolean(1))
                            }
                        }
                    }
                    val elapsed = assertLockDeadline(second)
                    release.countDown()
                    running.get(30, TimeUnit.SECONDS)
                    MigrationRunner(second).migrate()
                    fixture.record(
                        "migration-lock",
                        mapOf(
                            "owner" to owner,
                            "deadlineMilliseconds" to 120000L,
                            "observedMilliseconds" to elapsed.toMillis(),
                            "independentLockDenied" to true,
                            "subsequentRunValidated" to true,
                        ),
                    )
                }
            }
        } finally {
            release.countDown()
            executor.shutdownNow()
            fixture.membership(owner, "revoke")
        }
        fixture.grants(owner)
    }

    @Test
    fun authenticatedTransportAndPrivateHttp() {
        migrate()
        application().use { context ->
            val port = context.getBean(GrpcServerLifecycle::class.java).port
            val interceptor = context.getBean(ServiceIdentityInterceptor::class.java)
            val names = listOf("spoolin.v1.IdentityService", "spoolin.v1.McpGatewayService")
            assertNamedHealth(port, names)
            assertDeniedTransport(interceptor, port, names)
            assertNavigation(context)
            assertPrivateReadiness(context)
        }
    }

    private fun assertNamedHealth(
        port: Int,
        names: List<String>,
    ) {
        channel("core", port).useChannel { channel ->
            names.forEach { name ->
                val response =
                    HealthGrpc
                        .newBlockingStub(
                            channel,
                        ).withDeadlineAfter(2, TimeUnit.SECONDS)
                        .check(HealthCheckRequest.newBuilder().setService(name).build())
                assertEquals(ServingStatus.SERVING, response.status)
            }
        }
    }

    private fun assertUnknownIdentity(port: Int) {
        channel("unknown", port).useChannel { channel ->
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                assertThrows(StatusRuntimeException::class.java) {
                    HealthGrpc
                        .newBlockingStub(
                            channel,
                        ).withDeadlineAfter(2, TimeUnit.SECONDS)
                        .check(HealthCheckRequest.getDefaultInstance())
                }.status.code,
            )
        }
    }

    private fun assertDeniedTransport(
        interceptor: ServiceIdentityInterceptor,
        port: Int,
        names: List<String>,
    ) {
        val before = interceptor.dispatched.get()
        channel("media", port).useChannel { channel ->
            val failure =
                assertThrows(StatusRuntimeException::class.java) {
                    IdentityServiceGrpc
                        .newBlockingStub(
                            channel,
                        ).withDeadlineAfter(2, TimeUnit.SECONDS)
                        .checkActor(Spoolin.CheckActorRequest.getDefaultInstance())
                }
            assertEquals(Status.Code.PERMISSION_DENIED, failure.status.code)
        }
        assertEquals(before, interceptor.dispatched.get())
        assertUnknownIdentity(port)
        fixture.record(
            "grpc-boundary",
            mapOf(
                "server" to owner,
                "port" to port,
                "allowedHealth" to names,
                "wrongIdentity" to "media",
                "deniedStatus" to "PERMISSION_DENIED",
                "handlerCountBefore" to before,
                "handlerCountAfter" to interceptor.dispatched.get(),
            ),
        )
    }

    private fun assertNonsecretKeyMetadata() {
        fixture.connection(owner).use { connection ->
            connection.prepareStatement("INSERT INTO appdata.crypto_key_version(id) VALUES (?)").use { statement ->
                statement.setObject(1, UUID.randomUUID())
                assertEquals(1, statement.executeUpdate())
            }
            connection.createStatement().use { assertEquals(1, it.executeUpdate("UPDATE appdata.crypto_key_version SET id=id")) }
            assertDenied(connection, "DELETE FROM appdata.crypto_key_version")
        }
        fixture.connection(owner).use { independent ->
            independent.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) FROM appdata.crypto_key_version").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(1, rows.getInt(1))
                    fixture.record("nonsecret-key-metadata", mapOf("freshReaderRows" to rows.getInt(1), "keyBytesStored" to false))
                }
            }
        }
    }

    private fun assertCredentialAndTlsDenial() {
        val properties = java.util.Properties()
        properties["user"] = "spoolin_${owner}_runtime"
        properties["password"] = "invalid-disposable-password"
        val wrongPassword = assertThrows(SQLException::class.java) { java.sql.DriverManager.getConnection(fixture.url(owner), properties) }
        assertEquals("28P01", wrongPassword.sqlState)
        properties["password"] = Files.readString(fixture.passwords.getValue("$owner-runtime"))
        val plaintext = fixture.url(owner).replace("sslmode=verify-full", "sslmode=disable")
        val missingTls = assertThrows(SQLException::class.java) { java.sql.DriverManager.getConnection(plaintext, properties) }
        assertEquals("28000", missingTls.sqlState)
        fixture.record(
            "database-credential-denial",
            mapOf(
                "owner" to owner,
                "wrongPasswordState" to wrongPassword.sqlState,
                "plaintextState" to missingTls.sqlState,
            ),
        )
    }

    private fun assertScramAndMembership() {
        val security =
            fixture.postgres.execInContainer(
                "bash",
                "-c",
                "export PGPASSWORD=\"$(cat /run/spoolin/postgres-password)\"; psql -X " +
                    "-At -U postgres -d spoolin_core -c \"SELECT count(*) FROM pg_authid WHERE rolname " +
                    "LIKE 'spoolin_%_runtime' AND rolpassword LIKE 'SCRAM-SHA-256%'; SELECT count(*) FROM " +
                    "pg_auth_members m JOIN pg_roles r ON r.oid=m.member WHERE r.rolname='spoolin_core_migrator';\"",
            )
        assertEquals(0, security.exitCode)
        assertEquals(listOf("3", "0"), security.stdout.trim().lines())
        fixture.record(
            "runtime-permissions",
            mapOf(
                "owner" to owner,
                "freshReaderRows" to 1,
                "tls" to true,
                "scramRuntimeRoles" to 3,
                "migratorMembershipAfterRun" to 0,
            ),
        )
    }

    private fun assertNavigation(context: ConfigurableApplicationContext) {
        val httpPort = requireNotNull((context as ServletWebServerApplicationContext).webServer).port
        val client =
            HttpClient
                .newBuilder()
                .sslContext(
                    context.getBean(SslBundles::class.java).getBundle("service").createSslContext(),
                ).build()
        graphqlMethodDenials(client, httpPort)
        val navigation = get(client, httpPort, "/movies")
        assertEquals(200, navigation.statusCode())
        assertTrue(navigation.body().contains("Representative navigation fixture"))
        listOf(
            "/api/missing",
            "/artwork/00000000-0000-4000-8000-000000000000",
            "/graphql/missing",
            "/artwork/missing",
            "/health/missing",
            "/internal/alerts",
            "/private/missing",
            "/missing.js",
            "/actuator/health/readiness",
        ).forEach { path ->
            val response = get(client, httpPort, path)
            // This valid artwork resource reaches an unavailable Catalog; missing routes still reject directly.
            val expected =
                if (path == "/artwork/00000000-0000-4000-8000-000000000000") setOf(503) else setOf(403, 404)
            assertTrue(response.statusCode() in expected, "$path returned ${response.statusCode()}")
            assertFalse(response.body().contains("Representative navigation fixture"))
            assertFalse(
                response
                    .headers()
                    .firstValue("Content-Type")
                    .orElse("")
                    .contains("text/html"),
            )
        }
        fixture.record(
            "https-navigation",
            mapOf(
                "port" to httpPort,
                "navigationStatus" to 200,
                "missingApiStatus" to get(client, httpPort, "/api/missing").statusCode(),
                "fixtureLabel" to "Representative navigation fixture",
            ),
        )
    }

    private fun assertPrivateReadiness(context: ConfigurableApplicationContext) {
        val bundle = context.getBean(SslBundles::class.java).getBundle("service")
        val client = HttpClient.newBuilder().sslContext(bundle.createSslContext()).build()
        val port = context.environment.getRequiredProperty("local.management.port", Int::class.java)
        assertEquals(200, get(client, port, "/actuator/health/readiness").statusCode())
        val migration = Path.of(requireNotNull(javaClass.getResource("/db/migration/V1__disposable_boundary.sql")).toURI())
        val original = Files.readAllBytes(migration)
        try {
            Files.writeString(migration, String(original) + "\nSELECT 1;\n")
            context.getBean(com.spoolin.core.ReadinessLifecycle::class.java).refresh()
            assertEquals(503, get(client, port, "/actuator/health/readiness").statusCode())
            val grpcPort = context.getBean(GrpcServerLifecycle::class.java).port
            channel("core", grpcPort).useChannel { rpc ->
                val response =
                    HealthGrpc
                        .newBlockingStub(
                            rpc,
                        ).withDeadlineAfter(2, TimeUnit.SECONDS)
                        .check(HealthCheckRequest.getDefaultInstance())
                assertEquals(ServingStatus.NOT_SERVING, response.status)
            }
        } finally {
            Files.write(migration, original)
            context.getBean(com.spoolin.core.ReadinessLifecycle::class.java).refresh()
        }
        assertEquals(200, get(client, port, "/actuator/health/readiness").statusCode())
        fixture.record(
            "private-readiness",
            mapOf(
                "port" to port,
                "before" to 200,
                "checksumDrift" to 503,
                "grpcAfterDrift" to "NOT_SERVING",
                "restored" to 200,
            ),
        )
    }

    private fun assertTransactionalRollback() {
        val resourceDirectory = Path.of(requireNotNull(javaClass.getResource("/db/migration/V1__disposable_boundary.sql")).toURI()).parent
        val failing = resourceDirectory.resolve("V2__transactional_failure.sql")
        try {
            Files.writeString(failing, "CREATE TABLE appdata.failed_fixture(id integer); SELECT 1/0;\n")
            fixture.membership(owner, "grant")
            fixture.pool(owner, "migrator").use { pool -> assertThrows(Exception::class.java) { MigrationRunner(pool).migrate() } }
            fixture.pool(owner).use { runtime -> assertFalse(SchemaReadiness(runtime, "1", "2").ready()) }
            fixture.connection(owner).use { connection ->
                connection.createStatement().use { statement ->
                    statement
                        .executeQuery(
                            "SELECT to_regclass('appdata.failed_fixture'), count(*) FILTER (WHERE NOT success), " +
                                "max(version) FROM appdata.flyway_schema_history",
                        ).use { rows ->
                            assertTrue(rows.next())
                            assertEquals(null, rows.getString(1))
                            assertEquals(0, rows.getInt(2))
                            assertEquals("1", rows.getString(3))
                        }
                }
            }
        } finally {
            Files.deleteIfExists(failing)
            fixture.membership(owner, "revoke")
        }
    }

    private fun assertLockDeadline(second: javax.sql.DataSource): Duration {
        val budget = Duration.ofSeconds(120)
        val start = System.nanoTime()
        assertThrows(IllegalStateException::class.java) { MigrationRunner(second).migrate(budget) }
        val elapsed = Duration.ofNanos(System.nanoTime() - start)
        assertTrue(elapsed >= budget && elapsed < budget.plusSeconds(3))
        return elapsed
    }

    private fun migrate() {
        fixture.membership(owner, "grant")
        try {
            fixture.pool(owner, "migrator").use { MigrationRunner(it).migrate() }
        } finally {
            fixture.membership(owner, "revoke")
        }
        fixture.grants(owner)
    }

    private fun assertDenied(
        connection: java.sql.Connection,
        sql: String,
    ) {
        val exception = assertThrows(SQLException::class.java) { connection.createStatement().use { it.execute(sql) } }
        assertEquals("42501", exception.sqlState)
        fixture.record("sql-denial", mapOf("owner" to owner, "sql" to sql, "sqlState" to exception.sqlState))
    }

    private fun freshRead(id: UUID) {
        fixture.connection(owner).use { independent ->
            independent.prepareStatement("SELECT count(*) FROM appdata.operation_receipt WHERE id=?").use {
                it.setObject(1, id)
                it.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    assertEquals(1, rows.getInt(1))
                }
            }
            independent.createStatement().use {
                it.executeQuery("SELECT count(*) FROM pg_roles WHERE rolname='escaped_runtime'").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(0, rows.getInt(1))
                }
            }
        }
    }

    private fun connectionMatrix() {
        listOf("core", "catalog", "media", "postgres", "template1").forEach { target ->
            val url =
                if (target in
                    listOf("postgres", "template1")
                ) {
                    fixture.url(owner).replace("/spoolin_$owner?", "/$target?")
                } else {
                    fixture.url(target)
                }
            val properties = java.util.Properties()
            properties["user"] = "spoolin_${owner}_runtime"
            properties["password"] = Files.readString(fixture.passwords.getValue("$owner-runtime"))
            if (target == owner) {
                java.sql.DriverManager
                    .getConnection(url, properties)
                    .use { assertTrue(it.isValid(2)) }
            } else {
                assertEquals(
                    "42501",
                    assertThrows(SQLException::class.java) { java.sql.DriverManager.getConnection(url, properties) }.sqlState,
                )
            }
            fixture.record("connection-matrix", mapOf("runtimeOwner" to owner, "target" to target, "connected" to (target == owner)))
        }
    }

    private fun graphqlMethodDenials(
        client: HttpClient,
        port: Int,
    ) {
        val html = get(client, port, "/graphql")
        assertEquals(406, html.statusCode())
        assertFalse(html.body().contains("Representative navigation fixture"))
        assertFalse(
            html
                .headers()
                .firstValue("Content-Type")
                .orElse("")
                .contains("text/html"),
        )
        val json = get(client, port, "/graphql", "application/json")
        assertEquals(403, json.statusCode())
        assertFalse(
            json
                .headers()
                .firstValue("Content-Type")
                .orElse("")
                .contains("text/html"),
        )
    }

    private fun application(): ConfigurableApplicationContext {
        val app = SpringApplication(Application::class.java, FoundationDependencies::class.java)
        app.setDefaultProperties(mapOf("spring.main.banner-mode" to "off", "logging.level.root" to "ERROR"))
        return app.run(
            "--spoolin.schema.minimum=1",
            "--spoolin.schema.maximum=1",
            "--spoolin.foundation-fixture.enabled=true",
            "--${owner.uppercase()}_DB_URL=${fixture.url(owner)}",
            "--${owner.uppercase()}_DB_USER=spoolin_${owner}_runtime",
            "--${owner.uppercase()}_DB_PASSWORD_FILE=${fixture.passwords.getValue("$owner-runtime")}",
            "--${owner.uppercase()}_DB_MAINTENANCE_PASSWORD_FILE=${fixture.passwords.getValue("$owner-maintenance")}",
            "--SERVICE_CERT_FILE=${fixture.root.resolve("$owner.crt")}",
            "--SERVICE_KEY_FILE=${fixture.root.resolve("$owner.key")}",
            "--SERVICE_CA_FILE=${fixture.root.resolve("ca.crt")}",
            "--HTTP_ADDRESS=127.0.0.1",
            "--HTTP_PORT=0",
            "--GRPC_ADDRESS=127.0.0.1",
            "--GRPC_PORT=0",
            "--MANAGEMENT_ADDRESS=127.0.0.1",
            "--MANAGEMENT_PORT=0",
        )
    }

    private fun channel(
        identity: String,
        port: Int,
    ): ManagedChannel =
        NettyChannelBuilder
            .forAddress("localhost", port)
            .sslContext(
                GrpcSslContexts
                    .forClient()
                    .trustManager(
                        fixture.root.resolve("ca.crt").toFile(),
                    ).keyManager(fixture.root.resolve("$identity.crt").toFile(), fixture.root.resolve("$identity.key").toFile())
                    .build(),
            ).build()

    private fun ManagedChannel.useChannel(action: (ManagedChannel) -> Unit) {
        try {
            action(this)
        } finally {
            shutdownNow()
            assertTrue(awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    private fun get(
        client: HttpClient,
        port: Int,
        path: String,
        accept: String = if (path.startsWith("/actuator")) "application/json" else "text/html",
    ): HttpResponse<String> =
        client.send(
            HttpRequest
                .newBuilder(
                    URI("https://localhost:$port$path"),
                ).header("Accept", accept)
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = ["spoolin.foundation-fixture.enabled"], havingValue = "true")
    class FoundationDependencies {
        @Bean
        fun ownerReceipts(source: DataSource): OwnerReceipts =
            OwnerReceipts(source, IntentHashes(mapOf("fixture" to ByteArray(32) { 7 }), "fixture"))

        @Bean
        fun keyRegistry(source: DataSource): KeyRegistry = KeyRegistry(source)

        @Bean
        fun nonceAllocator(source: DataSource): NonceAllocator = NonceAllocator(source)

        @Bean
        fun protectionMetrics(registry: MeterRegistry): ProtectionMetrics = ProtectionMetrics(registry)

        @Bean
        fun mountedKeys(): MountedKeys {
            val file = Files.createTempFile("foundation-draft-key-", ".key")
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
            val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
            try {
                Files.write(file, bytes)
                return MountedKeys(mapOf("fixture" to KeyMount(KeyPurpose.DRAFT, file)), null)
            } finally {
                bytes.fill(0)
                Files.deleteIfExists(file)
            }
        }
    }
}
