package com.spoolin.media.boundary.bootstrap

import com.spoolin.media.Application
import com.spoolin.media.DatabaseSettings
import com.spoolin.media.ServiceIdentityInterceptor
import com.spoolin.media.protectedValue
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class FoundationRulesTest {
    @Test
    fun databaseConfigurationRejectsWrongOwnerAndTlsDowngrade() {
        DatabaseSettings.validateUrl("jdbc:postgresql://localhost/spoolin_media?sslmode=verify-full", "media")
        listOf(
            "jdbc:postgresql://localhost/postgres?sslmode=verify-full",
            "jdbc:postgresql://localhost/spoolin_media?sslmode=require",
            "jdbc:postgresql://localhost/spoolin_media?sslmode=disable&sslmode=verify-full",
            "jdbc:postgresql://user@localhost/spoolin_media?sslmode=verify-full",
        ).forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { DatabaseSettings.validateUrl(url, "media") }
        }
        val env = StandardEnvironment()
        env.propertySources.addFirst(
            MapPropertySource(
                "fixture",
                mapOf(
                    "MEDIA_DB_URL" to "jdbc:postgresql://localhost/spoolin_media?sslmode=verify-full",
                    "MEDIA_DB_USER" to "spoolin_foreign_runtime",
                    "MEDIA_DB_PASSWORD_FILE" to "/unread/secret",
                ),
            ),
        )
        assertThrows(IllegalArgumentException::class.java) { DatabaseSettings.load(env, "media", false) }
    }

    @Test
    fun namespaceAndProtectedSecretValidationFailClosed() {
        listOf("spoolin", "production", "atdd-not-a-uuid", "atdd-00000000-0000-0000-0000-000000000000").forEach { namespace ->
            assertThrows(IllegalArgumentException::class.java) { OwnerFixture.validateNamespace(namespace) }
        }
        val directory = Files.createTempDirectory("foundation-secret-test-")
        try {
            val secret = directory.resolve("secret")
            Files.writeString(secret, "synthetic\n")
            Files.setPosixFilePermissions(secret, PosixFilePermissions.fromString("rw-------"))
            assertTrue(protectedValue(secret) == "synthetic")
            Files.setPosixFilePermissions(secret, PosixFilePermissions.fromString("rw-r--r--"))
            assertThrows(IllegalArgumentException::class.java) { protectedValue(secret) }
            Files.setPosixFilePermissions(secret, PosixFilePermissions.fromString("rw-------"))
            Files.writeString(secret, " \n")
            assertThrows(IllegalArgumentException::class.java) { protectedValue(secret) }
            Files.createSymbolicLink(directory.resolve("link"), secret)
            assertThrows(IllegalArgumentException::class.java) { protectedValue(directory.resolve("link")) }
        } finally {
            OwnerFixture.deleteTree(directory)
        }
    }

    @Test
    fun exactMethodAllowlistRejectsUnlistedAndMissingIdentities() {
        val interceptor = ServiceIdentityInterceptor()
        assertTrue(interceptor.allowed("grpc.health.v1.Health/Check", "core"))
        assertFalse(interceptor.allowed("grpc.health.v1.Health/Check", null))
        assertFalse(interceptor.allowed("spoolin.v1.Unknown/Execute", "core"))
        assertFalse(interceptor.allowed("not-a-method", "core"))
        assertFalse(interceptor.allowed("spoolin.v1.IdentityService/ResolveActor", "core"))
        assertTrue(interceptor.allowed("spoolin.v1.MediaService/GetAsset", "core"))
        assertTrue(interceptor.allowed("spoolin.v1.MediaService/Reserve", "catalog"))
        assertFalse(interceptor.allowed("spoolin.v1.MediaService/Reserve", "core"))
    }
}

class FoundationArchitectureTest {
    private val classes =
        ClassFileImporter().importPath(
            Path.of(
                Application::class.java.protectionDomain.codeSource.location
                    .toURI(),
            ),
        )

    @Test
    fun ownerClassesCannotDependOnAnotherOwnersImplementation() {
        assertTrue(classes.size > 0)
        noClasses()
            .that()
            .resideInAPackage(
                "com.spoolin.media..",
            ).should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.spoolin.core..", "com.spoolin.catalog..")
            .check(classes)
    }

    @Test
    fun springDependenciesNeverUseFieldInjection() {
        noFields().should().beAnnotatedWith(Autowired::class.java).check(classes)
    }
}
