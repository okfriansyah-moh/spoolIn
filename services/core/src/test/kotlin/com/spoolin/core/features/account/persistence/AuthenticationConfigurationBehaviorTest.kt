package com.spoolin.core.features.account.persistence

import com.spoolin.core.features.account.application.AuthenticationAction
import com.spoolin.core.features.account.application.AuthenticationConfiguration
import com.spoolin.core.features.account.application.AuthenticationDenied
import com.spoolin.core.features.account.application.AuthenticationRequest
import com.spoolin.core.features.crypto.ProtectionFailure
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.core.features.session.SessionCredential
import com.spoolin.support.policy.PolicyEnvironment
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID

class AuthenticationConfigurationBehaviorTest {
    @TempDir lateinit var root: Path

    @Test
    fun `configured csrf services share protected key while anonymous and session tokens stay distinct`() {
        val file = root.resolve("csrf"); Files.write(file, ByteArray(32) { 113 }); protect(file)
        val environment = StandardEnvironment().also { it.propertySources.addFirst(MapPropertySource("fixture", mapOf("CSRF_KEY_FILE" to file.toString()))) }
        val configuration = AuthenticationConfiguration()
        val session = configuration.sessionCsrf(environment)
        val anonymous = configuration.anonymousCsrf(environment)
        val submission = anonymous.issue()
        assertNotNull(anonymous.verify(submission.scopeCookieValue(), submission.csrfToken))
        assertTrue(session.matches(submission.csrfToken, session.digest(submission.csrfToken)))
        assertNotEquals(submission.csrfToken, session.derive(SessionCredential.issue()))
        assertThrows(AuthenticationDenied::class.java) { configuration.authenticationAdmission().check(AuthenticationAction.REGISTER, null, AuthenticationRequest("127.0.0.1", null)) }
        Files.write(file, ByteArray(31))
        assertThrows(IllegalArgumentException::class.java) { configuration.sessionCsrf(environment) }
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { configuration.anonymousCsrf(environment) }
    }

    @Test
    fun `configured identity and password policies reject invalid inputs and missing protection purposes`() {
        val configuration = AccountProtectionConfiguration()
        assertThrows(IllegalArgumentException::class.java) { configuration.mountedKeys(AccountProtectionSettings()) }
        val provider = PolicyProvider(PolicyEnvironment.resolve { null })
        val rules = configuration.identityRules(provider)
        assertThrows(IllegalArgumentException::class.java) { rules.identity("", "invalid") }
        configuration.passwordHasher(provider).use { hasher ->
            assertThrows(IllegalArgumentException::class.java) { hasher.hashNew("short") }
        }
        val registry = SimpleMeterRegistry()
        try {
            val alert = configuration.protectionAlert(configuration.protectionMetrics(registry))
            alert.failed(ProtectionFailure.KEY)
            alert.unavailable(UUID.randomUUID())
            assertEquals(1.0, registry.get("spoolin_crypto_failures_total").tag("code", "KEY").counter().count())
        } finally { registry.close() }
    }

    @Test
    fun `configured browser identity bootstraps anonymous cookies and requires verified csrf for mutation`() {
        val file = root.resolve("csrf"); Files.write(file, ByteArray(32) { 127 }); protect(file)
        val environment = StandardEnvironment().also { it.propertySources.addFirst(MapPropertySource("fixture", mapOf("CSRF_KEY_FILE" to file.toString()))) }
        val config = AuthenticationConfiguration()
        val csrf = config.sessionCsrf(environment)
        val anonymous = config.anonymousCsrf(environment)
        val db = com.spoolin.core.features.session.SessionJdbcFixture(emptyList())
        val identity = config.browserIdentity(config.sessionStore(db, csrf), anonymous, csrf)
        val request = org.springframework.mock.web.MockHttpServletRequest("POST", "/graphql")
        val bootstrapped = identity.context(request, false, false)
        assertNull(bootstrapped.principal)
        assertEquals(2, bootstrapped.cookies().size)
        assertNotNull(bootstrapped.anonymous)
        assertTrue(identity.context(request, false, false, bootstrap = false).cookies().isEmpty())
        val submission = anonymous.issue()
        request.setCookies(jakarta.servlet.http.Cookie("__Host-spoolin_registration", submission.scopeCookieValue()), jakarta.servlet.http.Cookie("__Host-spoolin_csrf", submission.csrfToken))
        request.addHeader("X-CSRF-Token", submission.csrfToken)
        val verified = identity.context(request, true, false)
        assertEquals(submission.scope, verified.anonymous!!.scope)
        assertTrue(verified.cookies().isEmpty())
        request.removeHeader("X-CSRF-Token")
        assertThrows(com.spoolin.core.boundary.http.BrowserRejected::class.java) { identity.context(request, true, false) }
        request.setCookies(jakarta.servlet.http.Cookie("duplicate", "one"), jakarta.servlet.http.Cookie("duplicate", "two"))
        assertThrows(com.spoolin.core.boundary.http.BrowserRejected::class.java) { identity.context(request, false, false) }
        assertEquals(0, db.queries)
    }

    private fun protect(path: Path) { Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------")) }
}
