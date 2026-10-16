package com.spoolin.core.browser

import com.spoolin.core.features.account.persistence.AccountInsert
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.session.IdentityHttpFixture
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import java.util.UUID

private const val FIXTURE_MINUTES = 45L
private const val RELEASE_POLL_MILLIS = 1000L
private const val PASSWORD_RANDOM_BYTES = 24

class AccountBrowserFixtureTest {
    @Test
    fun `hold actual HTTPS app for bounded integrated browser observation`() {
        val publicFile = Path.of(requireNotNull(System.getProperty("account.browser.handoff")))
        val release = publicFile.resolveSibling("account-browser.release")
        Files.deleteIfExists(release)
        val credentials =
            Files.createTempFile(
                "spoolin-browser-",
                ".json",
                PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rw-------"),
                ),
            )
        try {
            IdentityHttpFixture(
                admitted = false,
                trusted = true,
                rateOverrides =
                    mapOf(
                        "RATE_LIMIT_REGISTER_MAX_REQUESTS" to "100",
                        "RATE_LIMIT_SIGNIN_SOURCE_MAX_REQUESTS" to "1000",
                        "RATE_LIMIT_SIGNIN_ACCOUNT_MAX_REQUESTS" to "100",
                    ),
            ).use { fixture ->
                hold(fixture, publicFile, credentials, release)
            }
        } finally {
            Files.deleteIfExists(credentials)
            Files.deleteIfExists(publicFile)
        }
    }

    private fun hold(
        fixture: IdentityHttpFixture,
        publicFile: Path,
        credentials: Path,
        release: Path,
    ) {
        val password = password()
        val people = seed(fixture, password)
        ObjectMapper().writeValue(credentials.toFile(), people + mapOf("password" to password))
        val handoff =
            mapOf(
                "origin" to fixture.origin,
                "ca" to
                    fixture.crypto.owner.root
                        .resolve("ca.crt")
                        .toString(),
                "credentialsFile" to credentials.toString(),
                "releaseFile" to release.toString(),
                "deadlineMinutes" to FIXTURE_MINUTES,
                "admission" to "production",
            )
        ObjectMapper().writeValue(publicFile.toFile(), handoff)
        val deadline = System.nanoTime() + Duration.ofMinutes(FIXTURE_MINUTES).toNanos()
        while (!Files.exists(release) && System.nanoTime() < deadline) Thread.sleep(RELEASE_POLL_MILLIS)
        assertTrue(Files.exists(release), "Browser observation fixture reached its finite deadline")
    }

    private fun seed(
        fixture: IdentityHttpFixture,
        password: String,
    ): Map<String, String> {
        val accounts = fixture.context.getBean(ProtectedAccounts::class.java)
        val hasher = fixture.context.getBean(PasswordHasher::class.java)
        val identities = fixture.context.getBean(IdentityRules::class.java)
        val suffix = UUID.randomUUID().toString()
        val emails =
            mapOf(
                "viewerEmail" to "viewer-$suffix@example.test",
                "maintainerEmail" to "maintainer-$suffix@example.test",
                "registrationEmail" to "new-$suffix@example.test",
            )
        val roles = listOf("viewerEmail" to AccountRole.VIEWER, "maintainerEmail" to AccountRole.MAINTAINER)
        roles.forEach { (key, role) ->
            val stored =
                accounts.insert(
                    UUID.randomUUID(),
                    identities.identity("Browser ${role.name.lowercase()}", emails.getValue(key)),
                    hasher.hashNew(password),
                    role,
                )
            assertTrue(stored is AccountInsert.Stored, "Synthetic browser account was not stored")
        }
        return emails
    }

    private fun password(): String {
        val bytes = ByteArray(PASSWORD_RANDOM_BYTES).also(SecureRandom()::nextBytes)
        return "Aa1!" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
