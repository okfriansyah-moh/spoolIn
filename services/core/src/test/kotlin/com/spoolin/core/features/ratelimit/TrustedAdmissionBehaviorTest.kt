package com.spoolin.core.features.ratelimit

import com.spoolin.core.features.account.application.AuthenticationAction
import com.spoolin.core.features.account.application.AuthenticationRequest
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.support.identity.RateProfile
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.identity.RateScope
import com.spoolin.support.policy.ConfigurationSources
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TrustedAdmissionBehaviorTest {
    @TempDir lateinit var root: Path
    private val key = ByteArray(32) { 73 }
    private val profiles = RateProfiles(ConfigurationSources())

    @Test
    fun `real agreement and source admission consume one window per request even across repeated reads`() =
        fixture { db, trusted ->
            val request = AuthenticationRequest("127.0.0.1", null)
            db.enqueue(mapOf("1" to 1800000000L))
            db.enqueue(emptyMap())
            trusted.reads(request, true, false)
            trusted.reads(request, true, false)
            assertEquals(1, db.commits)
            assertEquals(2, db.queries)
        }

    @Test
    fun `sign in uses independent account and source demands and mutations require credential identity`() =
        fixture { db, trusted ->
            val request = AuthenticationRequest("127.0.0.1", UUID.randomUUID())
            db.enqueue(mapOf("1" to 1800000000L))
            db.enqueue(emptyMap())
            db.enqueue(emptyMap())
            trusted.check(AuthenticationAction.SIGN_IN, "person@example.test", request)
            db.enqueue(mapOf("1" to 1800000000L))
            db.enqueue(emptyMap())
            trusted.check(AuthenticationAction.SIGN_OUT, null, request)
            trusted.mutation(request)
            assertEquals(2, db.commits)
            assertThrows(RateUnavailable::class.java) { trusted.mutation(AuthenticationRequest("127.0.0.1", null)) }
            assertThrows(RateUnavailable::class.java) { trusted.check(AuthenticationAction.SIGN_IN, null, request) }
        }

    @Test
    fun `rate exhausted bucket returns positive retry and missing database time fails closed`() =
        fixture { db, trusted ->
            db.enqueue(mapOf("1" to 1800000000L))
            db.enqueue()
            val limited =
                assertThrows(
                    RateLimited::class.java,
                ) { trusted.check(AuthenticationAction.REGISTER, null, AuthenticationRequest("127.0.0.1", null)) }
            assertTrue(limited.retryAfterMs > 0)
            db.enqueue()
            assertThrows(RateUnavailable::class.java) { trusted.reads(AuthenticationRequest("127.0.0.1", null), true, false) }
        }

    @Test
    fun `agreement tampering invalidates subsequent admission before database consumption`() =
        fixture { db, trusted ->
            Files.writeString(root.resolve("agreement.json"), "{}")
            assertThrows(RateUnavailable::class.java) { trusted.reads(AuthenticationRequest("127.0.0.1", null), true, false) }
            assertEquals(0, db.queries)
            assertThrows(IllegalArgumentException::class.java) { RateAdmission(db, profiles, ByteArray(31)) }
            assertThrows(IllegalArgumentException::class.java) { RateDemand(RateProfile.READ, RateScope.SOURCE, "") }
        }

    @Test
    fun `configured admission health validates agreement and database and maintenance removes expired windows`() =
        fixture { db, trusted ->
            val agreement = RateAgreement(root.resolve("agreement.json"), profiles.revision, key)
            val config = RateConfiguration()
            val maintenance = config.admissionHealth(db, agreement)
            db.enqueue()
            assertTrue(maintenance.ready())
            assertEquals(1, maintenance.purge())
            val admission = config.admission(trusted)
            db.enqueue(mapOf("1" to 1800000000L))
            db.enqueue(emptyMap())
            admission.check(AuthenticationAction.REGISTER, null, AuthenticationRequest("127.0.0.1", null))
            Files.writeString(root.resolve("agreement.json"), "{}")
            assertFalse(maintenance.ready())
        }

    private fun fixture(test: (SessionJdbcFixture, TrustedAdmission) -> Unit) {
        val path = root.resolve("agreement.json")
        val canary =
            Mac
                .getInstance("HmacSHA256")
                .also {
                    it.init(SecretKeySpec(key, "HmacSHA256"))
                }.doFinal("spoolin-rate-agreement-v1".toByteArray())
        Files.writeString(
            path,
            "{\"profileRevision\":\"${profiles.revision}\",\"keyCanary\":\"${java.util.HexFormat.of().formatHex(canary)}\"}",
        )
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        val keyFile = root.resolve("rate-key")
        Files.write(keyFile, key)
        Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-------"))
        val environment =
            org.springframework.core.env.StandardEnvironment().also {
                it.propertySources.addFirst(
                    org.springframework.core.env.MapPropertySource(
                        "fixture",
                        mapOf("RATE_LIMIT_KEY_FILE" to keyFile.toString(), "spoolin.rate-limit.agreement-file" to path.toString()),
                    ),
                )
            }
        val config = RateConfiguration()
        val configuredProfiles = config.rateProfiles(environment)
        assertEquals(profiles.revision, configuredProfiles.revision)
        val agreement = config.rateAgreement(environment, configuredProfiles)
        val db = SessionJdbcFixture(emptyList())
        val limiter = config.rateAdmission(db, environment, configuredProfiles, agreement)
        test(db, config.trustedAdmission(limiter))
    }
}
