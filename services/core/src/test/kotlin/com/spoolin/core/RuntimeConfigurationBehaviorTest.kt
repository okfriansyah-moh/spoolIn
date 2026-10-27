package com.spoolin.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class RuntimeConfigurationBehaviorTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun `database settings require owned runtime or migration role and verified TLS`() {
        for (migrating in listOf(false, true)) {
            val role = if (migrating) "migrator" else "runtime"
            val settings = DatabaseSettings.load(environment("spoolin_core_$role"), "core", migrating)
            assertEquals("spoolin_core_$role", settings.username)
            assertEquals(migrating, settings.migrating)
            assertEquals(Path.of("/run/secrets/password"), settings.passwordFile)
        }
        assertThrows(IllegalArgumentException::class.java) { DatabaseSettings.load(environment("postgres"), "core", false) }
        assertThrows(IllegalArgumentException::class.java) { DatabaseSettings.load(environment("spoolin_core_migrator"), "core", false) }
    }

    @Test
    fun `database URL rejects foreign database credentials TLS downgrade duplicate options and unsupported parameters`() {
        val bad =
            listOf(
                "jdbc:h2:mem:core",
                "jdbc:postgresql://db/spoolin_catalog?sslmode=verify-full",
                "jdbc:postgresql://user:password@db/spoolin_core?sslmode=verify-full",
                "jdbc:postgresql://db/spoolin_core?sslmode=require",
                "jdbc:postgresql://db/spoolin_core?sslmode=verify-full&sslmode=disable",
                "jdbc:postgresql://db/spoolin_core?sslmode=verify-full&password=secret",
                "jdbc:postgresql://db/spoolin_core?sslmode=verify-full&connectTimeout",
                "jdbc:postgresql://db/spoolin_core",
            )
        for (url in bad) assertThrows(IllegalArgumentException::class.java) { DatabaseSettings.validateUrl(url, "core") }
        DatabaseSettings.validateUrl(
            "jdbc:postgresql://db:5432/spoolin_core?sslmode=verify-full&sslrootcert=/run/ca.pem&connectTimeout=2",
            "core",
        )
    }

    @Test
    fun `protected secret values strip line ending and retain interior spaces`() {
        val file = secret("  secret value  \r\n")
        assertEquals("  secret value  ", protectedValue(file))
        requireProtectedFile(file, 4096)
    }

    @Test
    fun `protected secret validation rejects empty multiline oversized public and linked files`() {
        for (value in listOf("", " \n", "line\nline", "line\rline", "x".repeat(4097))) {
            val file = secret(value)
            assertThrows(IllegalArgumentException::class.java) { protectedValue(file) }
        }
        val public = secret("value")
        Files.setPosixFilePermissions(public, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { protectedValue(public) }
        val target = secret("value")
        val link = root.resolve("link")
        Files.createSymbolicLink(link, target)
        assertThrows(IllegalArgumentException::class.java) { protectedValue(link) }
        assertThrows(IllegalArgumentException::class.java) { requireProtectedFile(root, 4096) }
    }

    @Test
    fun `unsupported maintenance arguments fail without starting application`() {
        assertThrows(IllegalStateException::class.java) { main(arrayOf("--unknown")) }
        assertThrows(IllegalStateException::class.java) { main(arrayOf("--migrate", "--unknown")) }
    }

    private fun secret(value: String): Path =
        Files.createTempFile(root, "secret", ".txt").also {
            Files.writeString(it, value)
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }

    private fun environment(role: String) =
        StandardEnvironment().also {
            it.propertySources.addFirst(
                MapPropertySource(
                    "fixture",
                    mapOf(
                        "CORE_DB_URL" to "jdbc:postgresql://db/spoolin_core?sslmode=verify-full",
                        "CORE_DB_USER" to role,
                        "CORE_DB_PASSWORD_FILE" to "/run/secrets/password",
                    ),
                ),
            )
        }
}
