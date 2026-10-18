package com.spoolin.catalog

import com.spoolin.catalog.features.movie.ManagedMovieReads
import com.spoolin.catalog.features.movie.MovieWrites
import com.spoolin.catalog.features.person.read.ManagedPersonReads
import com.spoolin.catalog.features.person.write.PersonWrites
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.ManagedReadCursors
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.operations.audit.AuditReads
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.receipts.ComparisonKeys
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
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.filter.OncePerRequestFilter
import spoolin.v1.CatalogServiceGrpc
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
private const val HTTP_UNAVAILABLE = 503
private const val HTTP_FORBIDDEN = 403
private const val HTTP_INTERNAL_ERROR = 500

private const val OWNER = "catalog"
private val serviceNames = listOf("spoolin.v1.CatalogService")

@SpringBootApplication(exclude = [GraphQlAutoConfiguration::class])
@EnableScheduling
class Application {
    @Bean(destroyMethod = "close")
    @org.springframework.context.annotation.Primary
    fun dataSource(environment: Environment): HikariDataSource {
        val key = Path.of(environment.getRequiredProperty("SERVICE_KEY_FILE"))
        requireProtectedFile(key, TLS_KEY_SIZE_LIMIT)
        return DatabaseSettings.load(environment, OWNER, false).pool()
    }
}

@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
class ObservabilityConfiguration {
    @Bean(initMethod = "start", destroyMethod = "close")
    fun notificationDispatcher(
        dataSource: DataSource,
        environment: Environment,
    ): com.spoolin.support.notifications.NotificationDispatcher? =
        com.spoolin.support.notifications.NotificationSettings
            .transport { environment.getProperty(it) }
            ?.let {
                com.spoolin.support.notifications
                    .NotificationDispatcher(dataSource, it)
            }

    @Bean
    fun ownerMetrics(
        registry: io.micrometer.core.instrument.MeterRegistry,
        dataSource: HikariDataSource,
        schema: SchemaReadiness,
    ): com.spoolin.support.observability.OwnerMetrics =
        com.spoolin.support.observability.OwnerMetrics(
            registry,
            dataSource,
            "catalog",
            com.spoolin.support.observability.OwnerMetricRuntime(
                { dataSource.hikariPoolMXBean.activeConnections },
                { dataSource.maximumPoolSize },
                { schema.ready() },
            ),
        )

    @Bean(destroyMethod = "close")
    fun ownerTracing(): com.spoolin.support.tracing.OwnerTracing =
        com.spoolin.support.tracing
            .OwnerTracing("catalog")

    @Bean
    @GlobalServerInterceptor
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1)
    fun traceServer(tracing: com.spoolin.support.tracing.OwnerTracing): com.spoolin.support.tracing.TraceServerInterceptor =
        com.spoolin.support.tracing.TraceServerInterceptor(tracing) { attributes ->
            runCatching {
                val certificate =
                    attributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION)?.peerCertificates?.firstOrNull() as? X509Certificate
                certificate?.let(::certificateIdentity) in setOf("core", "catalog", "media", "mcp")
            }.getOrDefault(false)
        }

    @Bean
    @org.springframework.grpc.client.GlobalClientInterceptor
    fun traceClient(tracing: com.spoolin.support.tracing.OwnerTracing): com.spoolin.support.tracing.TraceClientInterceptor =
        com.spoolin.support.tracing
            .TraceClientInterceptor(tracing)

    @Bean
    fun logicalMetrics(registry: io.micrometer.core.instrument.MeterRegistry): com.spoolin.support.observability.LogicalMetrics {
        com.spoolin.support.logging.DiagnosticDropMetrics.shared
            .bind("catalog", registry)
        return com.spoolin.support.observability
            .LogicalMetrics(registry, "catalog")
    }

    @Bean
    @GlobalServerInterceptor
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    fun logicalRpcMetrics(
        metrics: com.spoolin.support.observability.LogicalMetrics,
    ): com.spoolin.support.observability.LogicalRpcMetricsInterceptor =
        com.spoolin.support.observability
            .LogicalRpcMetricsInterceptor(metrics)
}

@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
class RuntimeConfiguration {
    @Bean("maintenanceDataSource")
    fun maintenanceDataSource(environment: Environment): DataSource =
        com.spoolin.support.maintenance.MaintenanceDataSources.open(
            OWNER,
            environment.getRequiredProperty("CATALOG_DB_URL"),
            Path.of(environment.getRequiredProperty("CATALOG_DB_MAINTENANCE_PASSWORD_FILE")),
        )

    @Bean
    fun receiptExpiry(
        @org.springframework.beans.factory.annotation.Qualifier("maintenanceDataSource") dataSource: DataSource,
    ): com.spoolin.catalog.operations.maintenance.ReceiptExpiry =
        com.spoolin.catalog.operations.maintenance
            .ReceiptExpiry(dataSource)

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
    ): ReadinessLifecycle = ReadinessLifecycle(schema, manager)

    @Bean
    @GlobalServerInterceptor
    fun serviceIdentity(
        environment: Environment,
        channels: org.springframework.grpc.client.GrpcChannelFactory,
    ): ServiceIdentityInterceptor =
        ServiceIdentityInterceptor(
            com.spoolin.catalog.boundary.grpc.CatalogActors
                .interceptor(environment, channels),
        )

    @Bean
    fun httpSecurity(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests { it.anyRequest().permitAll() }
        return http.build()
    }

    @Bean
    fun listenerBoundary(
        environment: Environment,
        metrics: org.springframework.beans.factory.ObjectProvider<com.spoolin.support.observability.OwnerMetrics>,
    ): ListenerBoundary = ListenerBoundary(environment, metrics.ifAvailable)
}

@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
class OwnerServiceConfiguration {
    @Bean
    fun catalogCursorKey(): ByteArray =
        com.spoolin.catalog.features.search.CatalogCursor
            .newRandomKey()

    @Bean
    @Suppress("LongParameterList")
    fun catalogService(
        dataSource: DataSource,
        cursorKey: ByteArray,
        environment: Environment,
        movieWritesProvider: ObjectProvider<MovieWrites>,
        managedReadsProvider: ObjectProvider<ManagedMovieReads>,
        personWritesProvider: ObjectProvider<PersonWrites>,
        managedPersonReadsProvider: ObjectProvider<ManagedPersonReads>,
        roleWritesProvider: ObjectProvider<com.spoolin.catalog.features.taxonomy.roles.RoleWrites>,
        creditWritesProvider: ObjectProvider<com.spoolin.catalog.features.credit.CreditWrites>,
        managedCreditReadsProvider: ObjectProvider<com.spoolin.catalog.features.credit.ManagedCreditReads>,
        artworkRpcProvider: ObjectProvider<com.spoolin.catalog.features.artwork.ArtworkRpc>,
        movieLifecycleProvider: ObjectProvider<com.spoolin.catalog.features.lifecycle.MovieLifecycle>,
        personLifecycleProvider: ObjectProvider<com.spoolin.catalog.features.lifecycle.PersonLifecycle>,
        deletionImpactProvider: ObjectProvider<com.spoolin.catalog.features.lifecycle.DeletionImpact>,
        proposalWritesProvider: ObjectProvider<com.spoolin.catalog.features.proposal.ProposalWrites>,
    ): io.grpc.BindableService {
        val policy = effectiveReadPolicy(environment)
        val reads =
            com.spoolin.catalog.operations.receipts
                .OwnerReceiptReads(dataSource)
        val authority =
            com.spoolin.support.receipts.ReceiptAuthority {
                com.spoolin.support.identity.ActorRpc
                    .current()
                    ?.let { "account:${it.accountId}" }
            }
        val titleCursor =
            com.spoolin.catalog.features.search
                .CatalogCursor(cursorKey)
        val peopleCursor =
            com.spoolin.catalog.features.search
                .PeopleCursor(cursorKey)
        return com.spoolin.catalog.boundary.grpc.CatalogServiceRpc(
            titleSearch =
                com.spoolin.catalog.features.search
                    .TitleSearchSql(dataSource, policy),
            personSearch =
                com.spoolin.catalog.features.person.read
                    .PersonSearch(dataSource, policy),
            taxonomy =
                com.spoolin.catalog.features.taxonomy.read
                    .TaxonomyReads(dataSource),
            provenance =
                com.spoolin.catalog.features.provenance
                    .ProvenanceReads(dataSource),
            receiptReads = reads,
            receiptAuthority = authority,
            titleCursor = titleCursor,
            peopleCursor = peopleCursor,
            movieWrites = movieWritesProvider.ifAvailable,
            managedReads = managedReadsProvider.ifAvailable,
            personWrites = personWritesProvider.ifAvailable,
            managedPersonReads = managedPersonReadsProvider.ifAvailable,
            roleWrites = roleWritesProvider.ifAvailable,
            creditWrites = creditWritesProvider.ifAvailable,
            managedCreditReads = managedCreditReadsProvider.ifAvailable,
            artwork = artworkRpcProvider.ifAvailable,
            movieLifecycle = movieLifecycleProvider.ifAvailable,
            personLifecycle = personLifecycleProvider.ifAvailable,
            deletionImpact = deletionImpactProvider.ifAvailable,
            proposalWrites = proposalWritesProvider.ifAvailable,
            policy = policy,
        )
    }
}

/** Wires movie write and managed-read handlers when catalog write keys are configured. */
@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["spoolin.catalog.write.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
@Suppress("TooManyFunctions")
class CatalogWriteConfiguration {
    @Bean
    fun catalogOwnerReceipts(
        dataSource: DataSource,
        environment: Environment,
    ): OwnerReceipts {
        val keyId = environment.getRequiredProperty("CATALOG_HMAC_KEY_ID")
        val keyFile = environment.getRequiredProperty("CATALOG_HMAC_KEY_FILE")
        val hashes = ComparisonKeys.load(mapOf(keyId to keyFile), keyId, keyFile)
        return OwnerReceipts(dataSource, hashes)
    }

    @Bean
    fun catalogWritePolicy(environment: Environment): com.spoolin.catalog.boundary.policy.CatalogWritePolicy =
        com.spoolin.catalog.boundary.policy.CatalogWritePolicy {
            com.spoolin.support.policy.PolicyEnvironment
                .resolve { environment.getProperty(it) }
        }

    @Bean
    fun movieWrites(
        receipts: OwnerReceipts,
        policy: com.spoolin.catalog.boundary.policy.CatalogWritePolicy,
    ): MovieWrites = MovieWrites(receipts, policy = policy)

    @Bean
    fun managedMovieReads(
        dataSource: DataSource,
        cursorKey: ByteArray,
        environment: Environment,
    ): ManagedMovieReads {
        val policy = effectiveReadPolicy(environment)
        val provenance = ProvenanceReads(dataSource)
        val titleSearch = TitleSearchSql(dataSource, policy)
        val auditReads = AuditReads(dataSource)
        return ManagedMovieReads(
            dataSource,
            provenance,
            titleSearch,
            auditReads,
            policy = policy,
            cursors = ManagedReadCursors(cursorKey),
        )
    }

    @Bean
    fun personWrites(
        receipts: OwnerReceipts,
        policy: com.spoolin.catalog.boundary.policy.CatalogWritePolicy,
    ): PersonWrites = PersonWrites(receipts, policy = policy)

    @Bean
    fun artworkRpc(
        dataSource: DataSource,
        receipts: OwnerReceipts,
        environment: Environment,
    ): com.spoolin.catalog.features.artwork.ArtworkRpc =
        com.spoolin.catalog.features.artwork
            .ArtworkRpc(dataSource, receipts, policyRevision = {
                com.spoolin.support.policy.PolicyEnvironment
                    .resolve { environment.getProperty(it) }
                    .revision
            })

    @Bean
    fun movieLifecycle(
        receipts: OwnerReceipts,
        environment: Environment,
    ): com.spoolin.catalog.features.lifecycle.MovieLifecycle =
        com.spoolin.catalog.features.lifecycle
            .MovieLifecycle(receipts, policyRevision = {
                com.spoolin.support.policy.PolicyEnvironment
                    .resolve { environment.getProperty(it) }
                    .revision
            })

    @Bean
    fun personLifecycle(
        dataSource: DataSource,
        receipts: OwnerReceipts,
        tokens: com.spoolin.catalog.features.lifecycle.ImpactToken,
        environment: Environment,
    ): com.spoolin.catalog.features.lifecycle.PersonLifecycle =
        com.spoolin.catalog.features.lifecycle
            .PersonLifecycle(dataSource, receipts, tokens, policyRevision = {
                com.spoolin.support.policy.PolicyEnvironment
                    .resolve { environment.getProperty(it) }
                    .revision
            })

    @Bean
    fun impactToken(environment: Environment): com.spoolin.catalog.features.lifecycle.ImpactToken =
        com.spoolin.catalog.features.lifecycle.ImpactToken.fromKeyFile(
            java.nio.file.Path
                .of(environment.getRequiredProperty("CATALOG_HMAC_KEY_FILE")),
            environment.getRequiredProperty("CATALOG_HMAC_KEY_ID"),
        )

    @Bean
    fun deletionImpact(
        dataSource: DataSource,
        tokens: com.spoolin.catalog.features.lifecycle.ImpactToken,
    ): com.spoolin.catalog.features.lifecycle.DeletionImpact =
        com.spoolin.catalog.features.lifecycle
            .DeletionImpact(dataSource, tokens)

    @Bean
    fun managedPersonReads(
        dataSource: DataSource,
        cursorKey: ByteArray,
        environment: Environment,
    ): ManagedPersonReads =
        ManagedPersonReads(
            dataSource,
            policy = effectiveReadPolicy(environment),
            cursors = ManagedReadCursors(cursorKey),
        )

    @Bean
    fun roleWrites(
        receipts: OwnerReceipts,
        policy: com.spoolin.catalog.boundary.policy.CatalogWritePolicy,
    ): com.spoolin.catalog.features.taxonomy.roles.RoleWrites =
        com.spoolin.catalog.features.taxonomy.roles
            .RoleWrites(receipts, policy = policy)

    @Bean
    fun creditWrites(
        receipts: OwnerReceipts,
        policy: com.spoolin.catalog.boundary.policy.CatalogWritePolicy,
    ): com.spoolin.catalog.features.credit.CreditWrites =
        com.spoolin.catalog.features.credit
            .CreditWrites(receipts, policy = policy)

    @Bean
    fun managedCreditReads(
        dataSource: DataSource,
        cursorKey: ByteArray,
        environment: Environment,
    ): com.spoolin.catalog.features.credit.ManagedCreditReads =
        com.spoolin.catalog.features.credit
            .ManagedCreditReads(
                dataSource,
                policy = effectiveReadPolicy(environment),
                cursors = ManagedReadCursors(cursorKey),
            )

    @Bean
    fun proposalMetrics(
        registry: org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry>,
        dataSource: DataSource,
    ): com.spoolin.catalog.operations.metrics.ProposalMetrics? =
        registry.ifAvailable?.let {
            com.spoolin.catalog.operations.metrics
                .ProposalMetrics(it, dataSource)
        }

    @Bean
    fun proposalWrites(
        dataSource: DataSource,
        receipts: OwnerReceipts,
        movieWrites: MovieWrites,
        proposalMetrics: com.spoolin.catalog.operations.metrics.ProposalMetrics?,
        environment: Environment,
    ): com.spoolin.catalog.features.proposal.ProposalWrites =
        com.spoolin.catalog.features.proposal.ProposalWrites(
            dataSource,
            receipts,
            movieWrites,
            proposalMetrics,
            livePolicyRevision = {
                com.spoolin.support.policy.PolicyEnvironment
                    .resolve { environment.getProperty(it) }
                    .revision
            },
        )
}

fun main(args: Array<String>) {
    com.spoolin.support.logging
        .applyNormalizedLogLevel(System.getenv("LOG_LEVEL"), OWNER)
    when {
        args.contentEquals(arrayOf("--migrate")) -> {
            val environment =
                org.springframework.core.env
                    .StandardEnvironment()
            DatabaseSettings.load(environment, OWNER, true).pool().use { MigrationRunner(it).migrate() }
        }
        args.any { it.startsWith("--seed-manifest=") } ->
            com.spoolin.catalog.seed
                .runSeedCli(args)
        args.isEmpty() ->
            org.springframework.boot.SpringApplication
                .run(Application::class.java)
        else -> error("Use environment configuration; accepted args: --migrate, --seed-manifest=PATH")
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
        val status = if (started && schema.ready()) ServingStatus.SERVING else ServingStatus.NOT_SERVING
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

class ServiceIdentityInterceptor(
    private val actors: ServerInterceptor? = null,
) : ServerInterceptor {
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
        return actors?.interceptCall(call, headers, next) ?: next.startCall(call, headers)
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
            put("spoolin.v1.CatalogService/SearchTitles", setOf("core"))
            put("spoolin.v1.CatalogService/GetTitle", setOf("core"))
            put("spoolin.v1.CatalogService/SearchPeople", setOf("core"))
            put("spoolin.v1.CatalogService/GetPerson", setOf("core"))
            put("spoolin.v1.CatalogService/PersonCredits", setOf("core"))
            put("spoolin.v1.CatalogService/TitleCredits", setOf("core"))
            put("spoolin.v1.CatalogService/ListGenres", setOf("core"))
            put("spoolin.v1.CatalogService/ListRoles", setOf("core"))
            put("spoolin.v1.CatalogService/CreateRole", setOf("core"))
            put("spoolin.v1.CatalogService/ManagedTitles", setOf("core"))
            put("spoolin.v1.CatalogService/ManagedPeople", setOf("core"))
            put("spoolin.v1.CatalogService/ManagedTitle", setOf("core"))
            put("spoolin.v1.CatalogService/ManagedPerson", setOf("core"))
            put("spoolin.v1.CatalogService/ManagedCredits", setOf("core"))
            put("spoolin.v1.CatalogService/Audit", setOf("core"))
            put("spoolin.v1.CatalogService/WriteMovie", setOf("core"))
            put("spoolin.v1.CatalogService/WritePerson", setOf("core"))
            put("spoolin.v1.CatalogService/WriteCredit", setOf("core"))
            put("spoolin.v1.CatalogService/RemoveCredit", setOf("core"))
            put("spoolin.v1.CatalogService/MovieLifecycle", setOf("core"))
            put("spoolin.v1.CatalogService/PersonLifecycle", setOf("core"))
            put("spoolin.v1.CatalogService/DeletionImpact", setOf("core"))
            put("spoolin.v1.CatalogService/BeginArtwork", setOf("core"))
            put("spoolin.v1.CatalogService/CancelArtwork", setOf("core"))
            put("spoolin.v1.CatalogService/GetOperation", setOf("core"))
            put("spoolin.v1.CatalogService/ResolveArtworkAccess", setOf("core"))
            put("spoolin.v1.CatalogService/ProposeMovie", setOf("core"))
            put("spoolin.v1.CatalogService/ApproveAndCreate", setOf("core"))
            put("spoolin.v1.CatalogService/RejectProposal", setOf("core"))
            put("spoolin.v1.CatalogService/GetAttachment", setOf("media"))
        }
}

class ListenerBoundary(
    private val environment: Environment,
    private val metrics: com.spoolin.support.observability.OwnerMetrics? = null,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val rejection = rejectionStatus(request)
        if (rejection != null) {
            response.sendError(rejection)
        } else {
            response.setHeader("X-Content-Type-Options", "nosniff")
            response.setHeader("Cache-Control", "no-store")
            if (request.requestURI == "/actuator/prometheus") {
                serveMetrics(request, response, chain)
            } else {
                chain.doFilter(request, response)
            }
        }
    }

    private fun rejectionStatus(request: HttpServletRequest): Int? {
        val path = request.requestURI
        if (!(path.startsWith("/actuator"))) return null
        val certificates = request.getAttribute("jakarta.servlet.request.X509Certificate") as? Array<*>
        val identity = (certificates?.firstOrNull() as? X509Certificate)?.let(::certificateIdentity)
        val permitted =
            when (path) {
                "/actuator/prometheus" -> identity == "prometheus"
                else -> identity in setOf("operator", "core", "catalog", "media", "mcp")
            }
        val privatePort = environment.getProperty("local.management.port", Int::class.java)
        return if (request.isSecure && request.localPort == privatePort && permitted) {
            null
        } else {
            HTTP_FORBIDDEN
        }
    }

    private fun serveMetrics(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val measurement = metrics
        if (measurement == null) {
            response.sendError(HTTP_UNAVAILABLE)
        } else {
            synchronized(measurement) {
                if (measurement.prepareScrape()) {
                    chain.doFilter(request, response)
                } else {
                    response.sendError(HTTP_UNAVAILABLE)
                }
            }
        }
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

private fun effectiveReadPolicy(environment: Environment): () -> PublicPolicy =
    { PolicyEnvironment.resolve { environment.getProperty(it) } }
