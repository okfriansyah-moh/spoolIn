package com.spoolin.catalog.unit

import com.spoolin.catalog.BoundaryErrors
import com.spoolin.catalog.DatabaseSettings
import com.spoolin.catalog.ListenerBoundary
import com.spoolin.catalog.MigrationRunner
import com.spoolin.catalog.SchemaReadiness
import com.spoolin.catalog.ServiceIdentityInterceptor
import com.spoolin.catalog.protectedValue
import com.spoolin.catalog.requireProtectedFile
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

class RuntimeBoundaryRulesTest {
    @TempDir lateinit var directory: Path

    private fun environment(values: Map<String, Any>) =
        StandardEnvironment().also {
            it.propertySources.addFirst(MapPropertySource("unit", values))
        }

    private fun secret(value: String): Path =
        directory.resolve("secret").also {
            Files.writeString(it, value)
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }

    @Test fun `database settings require owner database role and verified TLS`() {
        val password = secret("password")
        for (migrating in listOf(false, true)) {
            val role = if (migrating) "migrator" else "runtime"
            val settings =
                DatabaseSettings.load(
                    environment(
                        mapOf(
                            "CATALOG_DB_URL" to
                                "jdbc:postgresql://catalog-db:5432/spoolin_catalog?sslmode=verify-full&sslrootcert=/tls/ca.crt&connectTimeout=2",
                            "CATALOG_DB_USER" to "spoolin_catalog_$role",
                            "CATALOG_DB_PASSWORD_FILE" to password.toString(),
                        ),
                    ),
                    "catalog",
                    migrating,
                )
            assertEquals("spoolin_catalog_$role", settings.username)
            assertEquals(password, settings.passwordFile)
            assertEquals(migrating, settings.migrating)
        }
        for (url in listOf(
            "jdbc:h2:mem:test",
            "jdbc:postgresql://host/spoolin_core?sslmode=verify-full",
            "jdbc:postgresql://user@host/spoolin_catalog?sslmode=verify-full",
            "jdbc:postgresql://host/spoolin_catalog?sslmode=require",
            "jdbc:postgresql://host/spoolin_catalog?sslmode=verify-full&sslmode=verify-full",
            "jdbc:postgresql://host/spoolin_catalog?sslmode=verify-full&password=secret",
            "jdbc:postgresql://host/spoolin_catalog",
        )) {
            assertThrows(IllegalArgumentException::class.java) { DatabaseSettings.validateUrl(url, "catalog") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            DatabaseSettings.load(
                environment(
                    mapOf(
                        "CATALOG_DB_URL" to "jdbc:postgresql://host/spoolin_catalog?sslmode=verify-full",
                        "CATALOG_DB_USER" to "postgres",
                        "CATALOG_DB_PASSWORD_FILE" to password.toString(),
                    ),
                ),
                "catalog",
                false,
            )
        }
    }

    @Test fun `secret validation enforces regular private bounded single line files`() {
        assertEquals("value", protectedValue(secret("value\r\n")))
        for (value in listOf("", " ", "first\nsecond", "first\rsecond", "a".repeat(4097))) {
            assertThrows(IllegalArgumentException::class.java) { protectedValue(secret(value)) }
        }
        val file = secret("value")
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { protectedValue(file) }
        assertThrows(IllegalArgumentException::class.java) { requireProtectedFile(directory, 100) }
        assertThrows(IllegalArgumentException::class.java) { requireProtectedFile(directory.resolve("absent"), 100) }
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        val link = directory.resolve("link")
        Files.createSymbolicLink(link, file)
        assertThrows(IllegalArgumentException::class.java) { protectedValue(link) }
    }

    @Test fun `service identity matrix distinguishes media attachment reads and core catalog calls`() {
        val interceptor = ServiceIdentityInterceptor()
        val coreMethods =
            listOf(
                "SearchTitles",
                "GetTitle",
                "SearchPeople",
                "GetPerson",
                "PersonCredits",
                "TitleCredits",
                "ListGenres",
                "GetCatalogFilterOptions",
                "ListRoles",
                "CreateRole",
                "ManagedTitles",
                "ManagedPeople",
                "ManagedTitle",
                "ManagedPerson",
                "ManagedCredits",
                "Audit",
                "WriteMovie",
                "WritePerson",
                "WriteCredit",
                "RemoveCredit",
                "MovieLifecycle",
                "PersonLifecycle",
                "DeletionImpact",
                "BeginArtwork",
                "CancelArtwork",
                "GetOperation",
                "ResolveArtworkAccess",
                "ProposeMovie",
                "PrepareMoviePoster",
                "GetMovieProposal",
                "ListMovieProposals",
                "ReadMoviePoster",
                "ApproveAndCreate",
                "RejectProposal",
            )
        for (method in coreMethods) {
            assertTrue(interceptor.allowed("spoolin.v1.CatalogService/$method", "core"), method)
            assertFalse(interceptor.allowed("spoolin.v1.CatalogService/$method", "media"), method)
            assertFalse(interceptor.allowed("spoolin.v1.CatalogService/$method", null), method)
        }
        assertTrue(interceptor.allowed("spoolin.v1.CatalogService/GetAttachment", "media"))
        assertFalse(interceptor.allowed("spoolin.v1.CatalogService/GetAttachment", "core"))
        assertFalse(interceptor.allowed("spoolin.v1.CatalogService/Unknown", "core"))
        assertTrue(interceptor.allowed("grpc.health.v1.Health/Check", "operator"))
        assertTrue(interceptor.allowed("grpc.health.v1.Health/Watch", "mcp"))
        assertFalse(interceptor.allowed("grpc.health.v1.Health/Check", "prometheus"))
        assertFalse(interceptor.allowed("malformed/path/extra", "core"))
    }

    @Test fun `runtime listener sends private errors for unauthenticated management and protects public responses`() {
        for (path in listOf("/actuator/health", "/actuator/prometheus", "/catalog")) {
            val headers = mutableMapOf<String, String>()
            var status: Int? = null
            var dispatched = false
            val request =
                proxy<HttpServletRequest> { name, _ ->
                    when (name) {
                        "getRequestURI", "getServletPath" -> path
                        "getAttribute" -> null
                        "isSecure" -> false
                        "getLocalPort" -> 8080
                        "getDispatcherType" -> jakarta.servlet.DispatcherType.REQUEST
                        else -> null
                    }
                }
            val response =
                proxy<HttpServletResponse> { name, args ->
                    when (name) {
                        "setHeader" -> {
                            headers[args[0].toString()] = args[1].toString()
                            null
                        }
                        "sendError" -> {
                            status = args[0] as Int
                            null
                        }
                        "isCommitted" -> false
                        else -> null
                    }
                }
            ListenerBoundary(environment(mapOf("local.management.port" to 8081)))
                .doFilter(request, response, FilterChain { _, _ -> dispatched = true })
            if (path.startsWith("/actuator")) {
                assertEquals(403, status)
                assertFalse(dispatched)
            } else {
                assertNull(status)
                assertTrue(dispatched)
                assertEquals("no-store", headers["Cache-Control"])
                assertEquals("nosniff", headers["X-Content-Type-Options"])
            }
        }
    }

    @Test fun `error endpoint exposes fixed body and caller status without exception details`() {
        for (status in listOf(null, 403, 503)) {
            val request = proxy<HttpServletRequest> { name, _ -> if (name == "getAttribute") status else null }
            val result = BoundaryErrors().error(request)
            assertEquals(status ?: 500, result.statusCode.value())
            assertEquals(mapOf("error" to "REQUEST_FAILED"), result.body)
            assertEquals("no-store", result.headers.getFirst("Cache-Control"))
        }
    }

    @Test fun `migration admission and readiness reject invalid budgets and unavailable schema`() {
        val source = JdbcRows().source
        for (budget in listOf(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofSeconds(121))) {
            assertThrows(IllegalArgumentException::class.java) { MigrationRunner(source).migrate(budget) }
        }
        assertThrows(IllegalArgumentException::class.java) { SchemaReadiness(source, "2", "1") }
        val schema = SchemaReadiness(source, "1", "2")
        assertFalse(schema.ready())
        assertEquals("DOWN", schema.health().status.code)
        assertEquals("SCHEMA_UNAVAILABLE", schema.health().details["reason"])
    }
}
