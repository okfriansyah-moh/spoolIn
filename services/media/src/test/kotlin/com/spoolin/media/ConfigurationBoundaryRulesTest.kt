package com.spoolin.media

import com.spoolin.media.fixtures.JdbcBoundary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration

class ConfigurationBoundaryRulesTest {
    @TempDir lateinit var root: Path

    private fun environment(values: Map<String, Any>) =
        StandardEnvironment().apply {
            propertySources.addFirst(MapPropertySource("unit", values))
        }

    @Test fun `database settings enforce owner identity verified TLS and supported options`() {
        val url = "jdbc:postgresql://database:5432/spoolin_media?sslmode=verify-full&sslrootcert=/cert/ca.pem&connectTimeout=2"
        val properties =
            mapOf<String, Any>(
                "MEDIA_DB_URL" to url,
                "MEDIA_DB_USER" to "spoolin_media_runtime",
                "MEDIA_DB_PASSWORD_FILE" to root.resolve("password").toString(),
            )
        val settings = DatabaseSettings.load(environment(properties), "media", false)
        assertEquals(url, settings.url)
        assertEquals("spoolin_media_runtime", settings.username)
        assertFalse(settings.migrating)
        assertTrue(DatabaseSettings.load(environment(properties + ("MEDIA_DB_USER" to "spoolin_media_migrator")), "media", true).migrating)
        assertThrows(IllegalArgumentException::class.java) {
            DatabaseSettings.load(
                environment(properties + ("MEDIA_DB_USER" to "spoolin_core_runtime")),
                "media",
                false,
            )
        }
        for (invalid in listOf(
            "jdbc:h2:mem:test",
            "jdbc:postgresql://database/spoolin_core?sslmode=verify-full",
            "jdbc:postgresql://user:pass@database/spoolin_media?sslmode=verify-full",
            "jdbc:postgresql://database/spoolin_media?sslmode=disable",
            "jdbc:postgresql://database/spoolin_media?sslmode=verify-full&sslmode=verify-full",
            "jdbc:postgresql://database/spoolin_media?sslmode=verify-full&password=secret",
        )) {
            assertThrows(IllegalArgumentException::class.java) { DatabaseSettings.validateUrl(invalid, "media") }
        }
    }

    @Test fun `protected values require regular bounded private files and one nonempty line`() {
        val path = root.resolve("password")
        Files.writeString(path, "secret\r\n")
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        assertEquals("secret", protectedValue(path))
        requireProtectedFile(path, 10)
        for (content in listOf("", " ", "first\nsecond", "x".repeat(4097))) {
            Files.writeString(path, content)
            assertThrows(IllegalArgumentException::class.java) { protectedValue(path) }
        }
        Files.writeString(path, "secret")
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { protectedValue(path) }
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        val link = root.resolve("link")
        Files.createSymbolicLink(link, path)
        assertThrows(IllegalArgumentException::class.java) { protectedValue(link) }
    }

    @Test fun `peer allowlist gives each owner only its declared methods and protects health`() {
        val interceptor = ServiceIdentityInterceptor()
        for (method in listOf("Upload", "ReadAsset")) {
            assertTrue(interceptor.allowed("spoolin.v1.MediaService/$method", "core"))
            assertFalse(interceptor.allowed("spoolin.v1.MediaService/$method", "catalog"))
        }
        for (method in listOf("Reserve", "Confirm", "Release", "ReadMoviePoster")) {
            assertTrue(interceptor.allowed("spoolin.v1.MediaService/$method", "catalog"))
            assertFalse(interceptor.allowed("spoolin.v1.MediaService/$method", "core"))
        }
        for (method in listOf("GetAsset", "GetOperation")) {
            for (peer in listOf("core", "catalog")) {
                assertTrue(interceptor.allowed("spoolin.v1.MediaService/$method", peer))
            }
        }
        for (peer in listOf(
            "core",
            "catalog",
            "media",
            "mcp",
            "operator",
        )) {
            assertTrue(interceptor.allowed("grpc.health.v1.Health/Check", peer))
        }
        for (peer in listOf(null, "prometheus", "unknown")) assertFalse(interceptor.allowed("grpc.health.v1.Health/Watch", peer))
        assertFalse(interceptor.allowed("spoolin.v1.MediaService/Unknown", "core"))
        assertFalse(interceptor.allowed("malformed", "catalog"))
    }

    @Test fun `readiness fails closed on unavailable schema and migration rejects unsafe budgets`() {
        val source = JdbcBoundary(java.sql.SQLException("unavailable")).source
        val readiness = SchemaReadiness(source, "1", "10")
        assertFalse(readiness.ready())
        assertEquals("DOWN", readiness.health().status.code)
        assertThrows(IllegalArgumentException::class.java) { SchemaReadiness(source, "10", "1") }
        for (budget in listOf(Duration.ZERO, Duration.ofMillis(-1), Duration.ofSeconds(121))) {
            assertThrows(IllegalArgumentException::class.java) { MigrationRunner(source).migrate(budget) }
        }
    }

    @Test fun `service configuration creates private storage and default notification mode without starting workers`() {
        val store = OwnerServiceConfiguration().quarantineStore(environment(mapOf("spoolin.media.volume" to root.toString())))
        assertTrue(Files.isDirectory(store.outputDirectory()))
        assertNull(ObservabilityConfiguration().notificationDispatcher(JdbcBoundary().source, environment(emptyMap())))
        ObservabilityConfiguration().ownerTracing().use { span -> span.recovery().finish(true) }
        assertNotNull(RuntimeConfiguration().healthStatus())
        assertNotNull(RuntimeConfiguration().standardHealth(RuntimeConfiguration().healthStatus()))
        assertNotNull(OwnerServiceConfiguration().receiptExpiry(JdbcBoundary().source))
    }
}
