package com.spoolin.core

import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.core.features.ratelimit.RateMaintenance
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.protobuf.services.HealthStatusManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.filter.OncePerRequestFilter
import spoolin.v1.IdentityServiceGrpc
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.security.cert.X509Certificate
import java.sql.Connection
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.LockSupport
import javax.sql.DataSource

private const val RUNTIME_POOL_SIZE = 8
private const val MIGRATION_POOL_SIZE = 3
private const val CONNECT_TIMEOUT_MS = 2000L
private const val VALIDATION_TIMEOUT_MS = 1000L
private const val MIGRATION_STATEMENT_MS = 120000
private const val RUNTIME_STATEMENT_MS = 3000
private const val SECRET_SIZE_LIMIT = 4096L
private const val TLS_KEY_SIZE_LIMIT = 16384L
private const val MIGRATION_LOCK_SECONDS = 120L
private const val LOCK_POLL_MS = 20L
private const val HTTP_FORBIDDEN = 403
private const val HTTP_INTERNAL_ERROR = 500

private const val OWNER = "core"
private val serviceNames = listOf("spoolin.v1.IdentityService", "spoolin.v1.McpGatewayService")

@SpringBootApplication(exclude = [GraphQlAutoConfiguration::class])
@EnableScheduling
class Application {
    @Bean(destroyMethod = "close")
    fun dataSource(environment: Environment): HikariDataSource {
        val key = Path.of(environment.getRequiredProperty("SERVICE_KEY_FILE"))
        requireProtectedFile(key, TLS_KEY_SIZE_LIMIT)
        return DatabaseSettings.load(environment, OWNER, false).pool()
    }

    @Bean
    fun schema(
        environment: Environment,
        dataSource: DataSource,
    ): SchemaReadiness =
        SchemaReadiness(
            dataSource,
            environment.getRequiredProperty("spoolin.schema.minimum"),
            environment.getRequiredProperty("spoolin.schema.maximum"),
        )

    @Bean
    fun healthStatus(): HealthStatusManager =
        HealthStatusManager().also { manager ->
            (serviceNames + "").forEach { manager.setStatus(it, ServingStatus.NOT_SERVING) }
        }

    @Bean
    fun standardHealth(manager: HealthStatusManager): io.grpc.BindableService = manager.healthService

    @Bean
    fun readinessLifecycle(
        schema: SchemaReadiness,
        manager: HealthStatusManager,
        environment: Environment,
        rates: ObjectProvider<RateMaintenance>,
    ): ReadinessLifecycle = ReadinessLifecycle(schema, manager) { rateReady(environment, rates) }

    @Bean
    @GlobalServerInterceptor
    fun serviceIdentity(): ServiceIdentityInterceptor = ServiceIdentityInterceptor()

    @Bean
    fun httpSecurity(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests { it.anyRequest().permitAll() }
        http.csrf { it.ignoringRequestMatchers("/graphql") }
        return http.build()
    }

    @Bean
    fun listenerBoundary(environment: Environment): ListenerBoundary = ListenerBoundary(environment)

    @Bean
    fun identityService(
        sessions: org.springframework.beans.factory.ObjectProvider<com.spoolin.core.features.session.SessionStore>,
        connectors: org.springframework.beans.factory.ObjectProvider<com.spoolin.core.features.connector.ConnectorStore>,
    ): io.grpc.BindableService =
        com.spoolin.core.boundary.grpc
            .IdentityRpc({ sessions.ifAvailable }, { connectors.ifAvailable })

    @Bean
    @GlobalServerInterceptor
    fun connectorAuth(): com.spoolin.core.features.mcpgateway.ConnectorAuthInterceptor =
        com.spoolin.core.features.mcpgateway.ConnectorAuthInterceptor()

    @Bean
    fun mcpGatewayService(gateway: com.spoolin.core.features.mcpgateway.McpGatewayRpc): io.grpc.BindableService =
        gateway

    @Bean
    fun workPendingMetrics(
        registry: org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry>,
        dataSource: DataSource,
    ): com.spoolin.core.operations.metrics.WorkPendingMetrics? =
        registry.ifAvailable?.let { com.spoolin.core.operations.metrics.WorkPendingMetrics(it, dataSource) }

    @Bean
    fun alertRecorder(
        dataSource: DataSource,
        workPendingMetrics: org.springframework.beans.factory.ObjectProvider<com.spoolin.core.operations.metrics.WorkPendingMetrics>,
    ): com.spoolin.core.features.alerts.AlertRecorder =
        com.spoolin.core.features.alerts.AlertRecorder(dataSource, workPendingMetrics.ifAvailable)
}

fun main(args: Array<String>) {
    com.spoolin.support.logging.applyNormalizedLogLevel(System.getenv("LOG_LEVEL"))
    if (args.contentEquals(arrayOf("--migrate"))) {
        val environment =
            org.springframework.core.env
                .StandardEnvironment()
        DatabaseSettings.load(environment, OWNER, true).pool().use { MigrationRunner(it).migrate() }
    } else if (args.size == 1 && args[0].startsWith("--initialize-accounts=")) {
        com.spoolin.core.features.bootstrap.runInitializationCli(args[0])
    } else {
        check(args.isEmpty()) { "Use environment configuration or a supported maintenance argument" }
        org.springframework.boot.SpringApplication
            .run(Application::class.java)
    }
}

data class DatabaseSettings(
    val url: String,
    val username: String,
    val passwordFile: Path,
    val migrating: Boolean,
) {
    fun pool(): HikariDataSource {
        val config = HikariConfig()
        config.jdbcUrl = url
        config.username = username
        config.password = protectedValue(passwordFile)
        config.maximumPoolSize = if (migrating) MIGRATION_POOL_SIZE else RUNTIME_POOL_SIZE
        config.minimumIdle = 1
        config.connectionTimeout = CONNECT_TIMEOUT_MS
        config.validationTimeout = VALIDATION_TIMEOUT_MS
        config.connectionInitSql = "SET TIME ZONE 'UTC'"
        val timeout = if (migrating) MIGRATION_STATEMENT_MS else RUNTIME_STATEMENT_MS
        config.addDataSourceProperty(
            "options",
            "-c search_path=appdata -c statement_timeout=$timeout -c lock_timeout=2000 -c " +
                "idle_in_transaction_session_timeout=5000",
        )
        return HikariDataSource(config)
    }

    companion object {
        fun load(
            environment: Environment,
            owner: String,
            migrating: Boolean,
        ): DatabaseSettings {
            val prefix = owner.uppercase() + "_DB_"
            val url = environment.getRequiredProperty(prefix + "URL")
            validateUrl(url, owner)
            val role = if (migrating) "migrator" else "runtime"
            val username = environment.getRequiredProperty(prefix + "USER")
            require(username == "spoolin_${owner}_$role") { prefix + "USER must select the owner role" }
            val passwordFile = Path.of(environment.getRequiredProperty(prefix + "PASSWORD_FILE"))
            return DatabaseSettings(url, username, passwordFile, migrating)
        }

        fun validateUrl(
            url: String,
            owner: String,
        ) {
            require(url.startsWith("jdbc:postgresql://")) { "DB_URL requires PostgreSQL TCP" }
            val uri = URI(url.removePrefix("jdbc:"))
            val parameters =
                uri.rawQuery
                    .orEmpty()
                    .split('&')
                    .map { it.split('=', limit = 2) }
            require(uri.path == "/spoolin_$owner" && uri.userInfo == null) { "DB_URL requires the owned database" }
            val tls =
                parameters.count { it.first() == "sslmode" } == 1 &&
                    parameters.any { it == listOf("sslmode", "verify-full") }
            val supported = setOf("sslmode", "sslrootcert", "connectTimeout")
            require(parameters.all { it.size == 2 && it.first() in supported }) { "DB_URL option is unsupported" }
            require(tls) {
                "DB_URL requires sslmode=verify-full"
            }
        }
    }
}

fun requireProtectedFile(
    path: Path,
    maximumBytes: Long,
) {
    require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) { "Secret file must be a regular file" }
    val permissions = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
    require(Files.getPosixFilePermissions(path) == permissions) { "Secret file requires mode 0600" }
    require(Files.size(path) in 1..maximumBytes) { "Secret file size is invalid" }
}

fun protectedValue(path: Path): String {
    requireProtectedFile(path, SECRET_SIZE_LIMIT)
    return Files.readString(path).trimEnd('\n', '\r').also {
        require(it.isNotBlank() && !it.contains('\n') && !it.contains('\r')) {
            "Secret file must contain one nonempty value"
        }
    }
}

fun ownerFlyway(dataSource: DataSource): Flyway =
    Flyway
        .configure()
        .dataSource(dataSource)
        .schemas("appdata")
        .defaultSchema("appdata")
        .locations("classpath:db/migration")
        .createSchemas(false)
        .failOnMissingLocations(true)
        .validateMigrationNaming(true)
        .cleanDisabled(true)
        .lockRetryCount(0)
        .load()

class MigrationRunner(
    private val dataSource: DataSource,
) {
    fun migrate(
        acquisitionBudget: Duration = Duration.ofSeconds(MIGRATION_LOCK_SECONDS),
        acquired: () -> Unit = {},
    ) {
        val validBudget =
            !acquisitionBudget.isNegative && !acquisitionBudget.isZero &&
                acquisitionBudget <= Duration.ofSeconds(MIGRATION_LOCK_SECONDS)
        require(validBudget) { "Migration acquisition budget is invalid" }
        dataSource.connection.use { lock ->
            acquire(lock, acquisitionBudget)
            try {
                acquired()
                performMigrations()
            } finally {
                unlock(lock)
            }
        }
    }

    private fun performMigrations() {
        val flyway = ownerFlyway(dataSource)
        require(flyway.info().all().isNotEmpty()) { "migration: packaged owner migrations are missing" }
        flyway.migrate()
        require(flyway.validateWithResult().validationSuccessful) { "migration: validation failed" }
    }

    private fun unlock(connection: Connection) {
        connection.createStatement().use { it.execute("SELECT pg_advisory_unlock(1936749166)") }
    }

    private fun acquire(
        connection: Connection,
        budget: Duration,
    ) {
        val deadline = System.nanoTime() + budget.toNanos()
        while (System.nanoTime() < deadline) {
            if (tryLock(connection)) return
            if (Thread.currentThread().isInterrupted) error("migration: lock interrupted")
            val remaining = maxOf(1L, deadline - System.nanoTime())
            LockSupport.parkNanos(minOf(Duration.ofMillis(LOCK_POLL_MS).toNanos(), remaining))
        }
        error("migration: owner lock acquisition deadline exceeded")
    }

    private fun tryLock(connection: Connection): Boolean =
        connection.createStatement().use { statement ->
            statement.queryTimeout = 2
            statement.executeQuery("SELECT pg_try_advisory_lock(1936749166)").use {
                it.next()
                it.getBoolean(1)
            }
        }
}

class SchemaReadiness(
    private val dataSource: DataSource,
    minimum: String,
    maximum: String,
) : HealthIndicator {
    private val minimumVersion = MigrationVersion.fromVersion(minimum)
    private val maximumVersion = MigrationVersion.fromVersion(maximum)

    init {
        require(minimumVersion <= maximumVersion) { "Schema minimum must not exceed maximum" }
    }

    fun ready(): Boolean =
        runCatching {
            val flyway = ownerFlyway(dataSource)
            val info = flyway.info()
            val current = info.current() ?: return@runCatching false
            current.version in minimumVersion..maximumVersion && info.pending().isEmpty() &&
                flyway.validateWithResult().validationSuccessful
        }.getOrDefault(false)

    override fun health(): Health {
        val status = if (ready()) Health.up() else Health.down().withDetail("reason", "SCHEMA_UNAVAILABLE")
        return status.build()
    }
}

class ReadinessLifecycle(
    private val schema: SchemaReadiness,
    private val manager: HealthStatusManager,
    private val admissionReady: () -> Boolean = { true },
) : ApplicationRunner {
    @Volatile
    private var started = false

    override fun run(args: ApplicationArguments) {
        check(schema.ready()) { "startup: owner schema missing, invalid or unsupported" }
        started = true
        refresh()
    }

    @Scheduled(fixedDelay = 1000)
    fun refresh() {
        val ready = started && schema.ready() && admissionReady()
        val status = if (ready) ServingStatus.SERVING else ServingStatus.NOT_SERVING
        (serviceNames + "").forEach { manager.setStatus(it, status) }
    }

    @jakarta.annotation.PreDestroy
    fun stop() = manager.enterTerminalState()
}

fun certificateIdentity(certificate: X509Certificate): String? {
    val names =
        certificate.subjectAlternativeNames
            .orEmpty()
            .filter { it[0] == 2 }
            .mapNotNull { it[1] as? String }
    val identities = names.filter { it in setOf("core", "catalog", "media", "mcp", "operator", "prometheus") }
    return identities.singleOrNull()
}

class ServiceIdentityInterceptor : ServerInterceptor {
    val dispatched = AtomicLong()

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val identity =
            runCatching {
                val certificate =
                    call.attributes
                        .get(Grpc.TRANSPORT_ATTR_SSL_SESSION)
                        ?.peerCertificates
                        ?.firstOrNull() as? X509Certificate
                certificate?.let(::certificateIdentity)
            }.getOrNull()
        if (!allowed(call.methodDescriptor.fullMethodName, identity)) {
            val denied = Status.PERMISSION_DENIED.withDescription("Service identity is not permitted for this method")
            call.close(denied, Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }
        dispatched.incrementAndGet()
        return next.startCall(call, headers)
    }

    fun allowed(
        method: String,
        identity: String?,
    ): Boolean {
        if (method in setOf("grpc.health.v1.Health/Check", "grpc.health.v1.Health/Watch")) {
            return identity in setOf("core", "catalog", "media", "mcp", "operator")
        }
        return method.split('/').size == 2 && identity in methodCallers[method].orEmpty()
    }

    private val methodCallers: Map<String, Set<String>> =
        buildMap {
            put("spoolin.v1.IdentityService/CheckActor", setOf("catalog"))
            put("spoolin.v1.McpGatewayService/GetPolicy", setOf("mcp"))
            put("spoolin.v1.McpGatewayService/GetMovieContext", setOf("mcp"))
            put("spoolin.v1.McpGatewayService/ProposeMovie", setOf("mcp"))
            put("spoolin.v1.McpGatewayService/ApproveMovie", setOf("mcp"))
            put("spoolin.v1.McpGatewayService/RejectMovie", setOf("mcp"))
            put("spoolin.v1.McpGatewayService/GetMovieOperation", setOf("mcp"))
        }
}

class ListenerBoundary(
    private val environment: Environment,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val path = request.requestURI
        if (path.startsWith("/actuator")) {
            val certificates = request.getAttribute("jakarta.servlet.request.X509Certificate") as? Array<*>
            val identity = (certificates?.firstOrNull() as? X509Certificate)?.let(::certificateIdentity)
            val permitted =
                if (path ==
                    "/actuator/prometheus"
                ) {
                    identity == "prometheus"
                } else {
                    identity in setOf("operator", "core", "catalog", "media", "mcp")
                }
            val privatePort = environment.getProperty("local.management.port", Int::class.java)
            if (!request.isSecure || request.localPort != privatePort || !permitted) {
                response.sendError(HTTP_FORBIDDEN)
                return
            }
        }
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("Cache-Control", "no-store")
        chain.doFilter(request, response)
    }
}

@RestController
class FrontendNavigation {
    @GetMapping(
        "/",
        "/movies",
        "/people",
        "/sign-in",
        "/register",
        "/manage",
        "/manage/movies",
        "/manage/movies/new",
        "/manage/people",
        "/manage/people/new",
        "/manage/archives",
        "/manage/audit",
        produces = [MediaType.TEXT_HTML_VALUE],
    )
    fun navigation(): ResponseEntity<ByteArray> {
        val resource = ClassPathResource("static/200.html")
        if (!resource.exists()) return ResponseEntity.notFound().build()
        val bytes = resource.contentAsByteArray
        return ResponseEntity
            .ok()
            .contentType(
                MediaType.TEXT_HTML,
            ).header(
                "Content-Security-Policy",
                com.spoolin.core.boundary.FrontendPolicy
                    .forHtml(bytes),
            ).body(bytes)
    }

    @GetMapping(
        "/movies/{id}",
        "/titles/{id}",
        "/people/{id}",
        "/manage/movies/{id}",
        "/manage/people/{id}",
        produces = [MediaType.TEXT_HTML_VALUE],
    )
    fun recordNavigation(
        @PathVariable id: String,
    ): ResponseEntity<ByteArray> =
        if (id.matches(
                Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
            )
        ) {
            navigation()
        } else {
            ResponseEntity.notFound().build()
        }
}

@org.springframework.web.bind.annotation.RestController
class BoundaryErrors : org.springframework.boot.webmvc.error.ErrorController {
    @org.springframework.web.bind.annotation.RequestMapping("/error")
    fun error(request: HttpServletRequest): org.springframework.http.ResponseEntity<Map<String, String>> {
        val status = request.getAttribute("jakarta.servlet.error.status_code") as? Int ?: HTTP_INTERNAL_ERROR
        return org.springframework.http.ResponseEntity
            .status(
                status,
            ).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .header("Cache-Control", "no-store")
            .body(
                mapOf("error" to "REQUEST_FAILED"),
            )
    }
}

private fun rateReady(
    environment: Environment,
    rates: ObjectProvider<RateMaintenance>,
): Boolean {
    val account = environment.getProperty("spoolin.account-protection.enabled", Boolean::class.java, false)
    val admission = environment.getProperty("spoolin.rate-limit.enabled", Boolean::class.java, false)
    return if (account || admission) rates.ifAvailable?.ready() == true else true
}

@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
class AdmissionReadinessConfiguration {
    @Bean
    fun rateReadiness(
        environment: Environment,
        rates: ObjectProvider<RateMaintenance>,
    ): HealthIndicator =
        HealthIndicator {
            if (rateReady(environment, rates)) {
                Health.up().build()
            } else {
                Health.down().withDetail("reason", "ADMISSION_UNAVAILABLE").build()
            }
        }
}
