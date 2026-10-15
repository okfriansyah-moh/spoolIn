package com.spoolin.media.boundary.bootstrap

import com.spoolin.media.DatabaseSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.util.Base64
import java.util.Properties
import java.util.UUID

private const val POSTGRES = "postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c"

class PrivatePostgres : GenericContainer<PrivatePostgres>(DockerImageName.parse(POSTGRES))

class OwnerFixture : AutoCloseable {
    val namespace: String
    val root: Path
    private val ownRoot: Boolean
    private val parent: Path
    val repository: Path =
        Path
            .of(System.getProperty("user.dir"))
            .toAbsolutePath()
            .parent.parent
    val passwords = mutableMapOf<String, Path>()
    val observed = mutableListOf<Map<String, Any>>()
    private val mapper = ObjectMapper()
    lateinit var postgres: PrivatePostgres
        private set

    init {
        val suppliedNamespace = System.getenv("SPOOLIN_TEST_NAMESPACE")
        val suppliedConfig = System.getenv("SPOOLIN_TEST_CONFIG")
        require((suppliedNamespace == null) == (suppliedConfig == null)) { "Incomplete protected fixture handoff" }
        namespace = suppliedNamespace ?: "atdd-${UUID.randomUUID()}"
        validateNamespace(namespace)
        ownRoot = suppliedConfig == null
        parent = if (ownRoot) Files.createTempDirectory("$namespace-") else Path.of(suppliedConfig).parent
        if (ownRoot) prepareParent() else validateParent(Path.of(suppliedConfig))
        root = parent.resolve("media-${UUID.randomUUID()}")
        Files.createDirectory(root, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")))
        Files.writeString(root.resolve("fixture.json"), """{"namespace":"$namespace"}""")
    }

    fun start(): OwnerFixture {
        createCertificates()
        createPasswords()
        postgres = configuredPostgres()
        copyInputs()
        try {
            postgres.start()
        } catch (failure: Exception) {
            val log = passwords.values.fold(postgres.logs) { text, path -> text.replace(Files.readString(path), "[redacted]") }
            throw IllegalStateException("Disposable database startup failed: " + log.takeLast(5000), failure)
        }
        val bindings =
            postgres.containerInfo.networkSettings.ports.bindings.values
                .flatMap { it.toList() }
        assertTrue(bindings.all { it.hostIp == "127.0.0.1" })
        record("private-postgres", mapOf("host" to "127.0.0.1", "port" to postgres.getMappedPort(5432)))
        return this
    }

    private fun configuredPostgres(): PrivatePostgres =
        PrivatePostgres()
            .withLabel("spoolin.test.namespace", namespace)
            .withEnv("POSTGRES_PASSWORD_FILE", "/run/secrets/postgres-password")
            .withEnv("POSTGRES_INITDB_ARGS", "--auth-host=scram-sha-256 --auth-local=scram-sha-256")
            .withExposedPorts(5432)
            .withCreateContainerCmdModifier { command ->
                command.hostConfig?.withPortBindings(
                    com.github.dockerjava.api.model.PortBinding(
                        com.github.dockerjava.api.model.Ports.Binding
                            .bindIpAndPort("127.0.0.1", 0),
                        com.github.dockerjava.api.model.ExposedPort
                            .tcp(5432),
                    ),
                )
            }.withCommand(
                "bash",
                "/opt/spoolin/entrypoint.sh",
                "postgres",
                "-c",
                "ssl=on",
                "-c",
                "ssl_cert_file=/run/spoolin/server.crt",
                "-c",
                "ssl_key_file=/run/spoolin/server.key",
                "-c",
                "hba_file=/etc/postgresql/pg_hba.conf",
            ).waitingFor(
                Wait
                    .forLogMessage(
                        ".*database system is ready to accept connections.*\n",
                        2,
                    ).withStartupTimeout(Duration.ofSeconds(90)),
            )

    fun url(owner: String): String =
        "jdbc:postgresql://localhost:${postgres.getMappedPort(
            5432,
        )}/spoolin_$owner?sslmode=verify-full&sslrootcert=${root.resolve("ca.crt")}"

    fun connection(
        owner: String,
        role: String = "runtime",
    ): Connection {
        val properties = Properties()
        properties["user"] = "spoolin_${owner}_$role"
        properties["password"] = Files.readString(passwords.getValue("$owner-$role"))
        properties["connectTimeout"] = "2"
        return DriverManager.getConnection(url(owner), properties)
    }

    fun pool(
        owner: String,
        role: String = "runtime",
    ) = DatabaseSettings(url(owner), "spoolin_${owner}_$role", passwords.getValue("$owner-$role"), role == "migrator").pool()

    fun sql(
        owner: String,
        sql: String,
    ) {
        val result =
            postgres.execInContainer(
                "bash",
                "-c",
                "export PGPASSWORD=\"$(cat /run/spoolin/postgres-password)\"; exec " +
                    "psql -X -q -v ON_ERROR_STOP=1 -U postgres -d spoolin_$owner -c \"\$1\"",
                "fixture",
                sql,
            )
        check(result.exitCode == 0) { "Privileged fixture action failed: ${result.stderr.take(200)}" }
    }

    fun grants(owner: String) {
        val result = postgres.execInContainer("/opt/spoolin/membership.sh", "grants", owner)
        check(result.exitCode == 0) { "Owner grants failed" }
    }

    fun membership(
        owner: String,
        action: String,
    ) {
        val result = postgres.execInContainer("/opt/spoolin/membership.sh", action, owner)
        check(result.exitCode == 0) { "Owner membership action failed" }
    }

    fun record(
        name: String,
        values: Map<String, Any>,
    ) {
        observed += mapOf("name" to name, "namespace" to namespace) + values
    }

    override fun close() {
        val evidence = repository.resolve(".ai/evidence/t003-media-observations.json")
        Files.createDirectories(evidence.parent)
        if (::postgres.isInitialized && postgres.isRunning) {
            assertEquals(
                namespace,
                postgres.containerInfo.config.labels
                    ?.get("spoolin.test.namespace"),
            )
            postgres.stop()
            record("cleanup", mapOf("containerRemoved" to !postgres.isRunning))
        }
        Files.writeString(evidence, mapper.writeValueAsString(observed))
        check(mapper.readTree(Files.readString(root.resolve("fixture.json"))).get("namespace").asString() == namespace)
        deleteTree(root)
        if (ownRoot) deleteTree(parent)
    }

    private fun prepareParent() {
        Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("rwx------"))
        Files.createDirectory(parent.resolve("private"), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")))
        Files.writeString(parent.resolve("fixture.json"), """{"namespace":"$namespace"}""")
    }

    private fun validateParent(config: Path) {
        require(!Files.isSymbolicLink(parent) && Files.isDirectory(parent))
        require(Files.getPosixFilePermissions(parent) == PosixFilePermissions.fromString("rwx------"))
        require(!Files.isSymbolicLink(parent.resolve("private")))
        require(!Files.isSymbolicLink(config) && Files.isRegularFile(config))
        require(Files.getPosixFilePermissions(config) == PosixFilePermissions.fromString("rw-------"))
        val parsed = mapper.readTree(Files.readString(config))
        require(parsed.get("namespace").asString() == namespace && parent.fileName.toString().startsWith("$namespace-"))
        require(mapper.readTree(Files.readString(parent.resolve("fixture.json"))).get("namespace").asString() == namespace)
        require(Path.of(parsed.get("synthetic_secrets").asString()).toRealPath() == parent.resolve("private").toRealPath())
        require(Files.getPosixFilePermissions(parent.resolve("private")) == PosixFilePermissions.fromString("rwx------"))
    }

    private fun createPasswords() {
        val names =
            listOf("postgres") +
                listOf("core", "catalog", "media").flatMap { owner ->
                    listOf("runtime", "migrator", "maintenance", "backup").map { "$owner-$it" }
                }
        names.forEach { name ->
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val path = root.resolve("$name-password")
            Files.writeString(path, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
            passwords[name] = path
        }
    }

    private fun createCertificates() {
        command(
            "openssl",
            "req",
            "-x509",
            "-newkey",
            "rsa:2048",
            "-nodes",
            "-keyout",
            "ca.key",
            "-out",
            "ca.crt",
            "-days",
            "2",
            "-subj",
            "/CN=Disposable fixture CA",
        )
        listOf("postgres", "core", "catalog", "media", "mcp", "operator", "unknown").forEachIndexed { index, name ->
            createCertificate(name, index)
        }
        Files.list(root).use { paths ->
            paths
                .filter {
                    it.toString().endsWith(
                        ".key",
                    )
                }.forEach { Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------")) }
        }
    }

    private fun createCertificate(
        name: String,
        index: Int,
    ) {
        command(
            "openssl",
            "req",
            "-new",
            "-newkey",
            "rsa:2048",
            "-nodes",
            "-keyout",
            "$name.key",
            "-out",
            "$name.csr",
            "-subj",
            "/CN=$name",
        )
        Files.writeString(root.resolve("$name.ext"), "subjectAltName=DNS:$name,DNS:localhost\nextendedKeyUsage=serverAuth,clientAuth\n")
        command(
            "openssl",
            "x509",
            "-req",
            "-in",
            "$name.csr",
            "-CA",
            "ca.crt",
            "-CAkey",
            "ca.key",
            "-set_serial",
            "${index + 1}",
            "-out",
            "$name.crt",
            "-days",
            "2",
            "-extfile",
            "$name.ext",
        )
        command("openssl", "pkcs8", "-topk8", "-nocrypt", "-in", "$name.key", "-out", "$name.pk8")
        Files.move(root.resolve("$name.pk8"), root.resolve("$name.key"), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }

    private fun command(vararg arguments: String) {
        val process =
            ProcessBuilder(
                *arguments,
            ).directory(
                root.toFile(),
            ).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        check(
            process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS) && process.exitValue() == 0,
        ) { "Fixture certificate command failed" }
    }

    private fun copyInputs() {
        passwords.forEach { (name, path) -> copy(path, "/run/secrets/$name-password", 0b110000000) }
        copy(root.resolve("postgres.crt"), "/run/secrets/postgres-cert")
        copy(root.resolve("postgres.key"), "/run/secrets/postgres-key", 0b110000000)
        listOf("entrypoint.sh", "membership.sh").forEach {
            copy(repository.resolve("ops/containers/postgres/$it"), "/opt/spoolin/$it", 0b111101101)
        }
        copy(repository.resolve("ops/containers/postgres/init.sh"), "/docker-entrypoint-initdb.d/10-owner-bootstrap.sh", 0b111101101)
        copy(repository.resolve("ops/containers/postgres/pg_hba.conf"), "/etc/postgresql/pg_hba.conf")
        listOf("bootstrap.sql", "grants.sql").forEach { copy(repository.resolve("ops/postgres/$it"), "/opt/spoolin/$it") }
    }

    private fun copy(
        source: Path,
        target: String,
        mode: Int = 0b110100100,
    ) {
        postgres.withCopyFileToContainer(MountableFile.forHostPath(source, mode), target)
    }

    companion object {
        fun validateNamespace(namespace: String) {
            require(namespace.startsWith("atdd-")) { "Normal installation is forbidden" }
            val id = UUID.fromString(namespace.removePrefix("atdd-"))
            require(id.version() == 4 && "atdd-$id" == namespace) { "Fixture requires a lowercase UUIDv4" }
        }

        fun deleteTree(path: Path) {
            Files.walk(path).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
        }
    }
}
