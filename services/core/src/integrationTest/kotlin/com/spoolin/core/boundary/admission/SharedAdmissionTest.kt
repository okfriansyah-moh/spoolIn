package com.spoolin.core.boundary.admission

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.core.features.ratelimit.RateAdmission
import com.spoolin.core.features.ratelimit.RateAgreement
import com.spoolin.core.features.ratelimit.RateDemand
import com.spoolin.core.features.ratelimit.RateLimited
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.support.identity.RateProfile
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.identity.RateScope
import com.spoolin.support.policy.ConfigurationSources
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SharedAdmissionTest {
    @Test
    fun `two independent replicas admit one remaining slot and restart preserves counters`() {
        OwnerFixture("t008-admission-replicas.json").start().use { fixture ->
            fixture.membership("core", "grant")
            fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
            fixture.grants("core")
            fixture.membership("core", "revoke")
            fixture.pool("core").use { pool ->
                val profiles = profiles(5)
                val secret = ByteArray(32) { 19 }
                val agreement = agreement(fixture, profiles, secret)
                val replicas =
                    listOf(
                        RateAdmission(pool, profiles, secret, agreement::check),
                        RateAdmission(pool, profiles, secret, agreement::check),
                    )
                val demand = listOf(RateDemand(RateProfile.READ, RateScope.SOURCE, "192.0.2.1"))
                repeat(4) { replicas[0].admit(demand) }
                val barrier = CyclicBarrier(8)
                val executor = Executors.newFixedThreadPool(8)
                val outcomes =
                    try {
                        (0..7)
                            .map { index ->
                                executor.submit<Boolean> {
                                    barrier.await(10, TimeUnit.SECONDS)
                                    try {
                                        replicas[index % 2].admit(demand)
                                        true
                                    } catch (_: RateLimited) {
                                        false
                                    }
                                }
                            }.map { it.get(20, TimeUnit.SECONDS) }
                    } finally {
                        executor.shutdownNow()
                    }
                assertEquals(1, outcomes.count { it })
                assertThrows(RateLimited::class.java) { RateAdmission(pool, profiles, secret, agreement::check).admit(demand) }
                fixture.connection("core", "runtime").use { fresh ->
                    fresh.createStatement().use { statement ->
                        statement.executeQuery("SELECT hits FROM appdata.request_rate_window").use { rows ->
                            check(rows.next())
                            assertEquals(5, rows.getInt(1))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `denied profile consumes available companion and deduplicates exact dimension only`() {
        OwnerFixture("t008-admission-companions.json").start().use { fixture ->
            fixture.membership("core", "grant")
            fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
            fixture.grants("core")
            fixture.membership("core", "revoke")
            fixture.pool("core").use { pool ->
                val profiles = profiles(1)
                val secret = ByteArray(32) { 20 }
                val agreement = agreement(fixture, profiles, secret)
                val limiter = RateAdmission(pool, profiles, secret, agreement::check)
                val source = RateDemand(RateProfile.READ, RateScope.SOURCE, "same")
                val credential = RateDemand(RateProfile.READ, RateScope.CREDENTIAL, "same")
                limiter.admit(listOf(source, source))
                assertThrows(RateLimited::class.java) { limiter.admit(listOf(source, credential)) }
                assertThrows(RateLimited::class.java) { limiter.admit(listOf(credential)) }
                fixture.connection("core", "runtime").use { fresh ->
                    fresh.createStatement().use { statement ->
                        statement.executeQuery("SELECT count(*),sum(hits) FROM appdata.request_rate_window").use { rows ->
                            check(rows.next())
                            assertEquals(2, rows.getInt(1))
                            assertEquals(2, rows.getInt(2))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `explicit max and duration transition retains prior counters and outage never admits`() {
        OwnerFixture("t008-admission-transition.json").start().use { fixture ->
            fixture.membership("core", "grant")
            fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
            fixture.grants("core")
            fixture.membership("core", "revoke")
            val pool = fixture.pool("core")
            try {
                val secret = ByteArray(32) { 25 }
                val demand = listOf(RateDemand(RateProfile.READ, RateScope.SOURCE, "192.0.2.3"))
                val initial = profiles(2)
                val oldAgreement = agreement(fixture, initial, secret)
                val old = RateAdmission(pool, initial, secret, oldAgreement::check)
                repeat(2) { old.admit(demand) }
                val raised = profiles(3)
                val nextAgreement = agreement(fixture, raised, secret)
                assertThrows(RateUnavailable::class.java) { old.admit(demand) }
                val next = RateAdmission(pool, raised, secret, nextAgreement::check)
                next.admit(demand)
                assertThrows(RateLimited::class.java) { next.admit(demand) }
                val duration =
                    RateProfiles(
                        ConfigurationSources(
                            mapOf(
                                "RATE_LIMIT_READ_MAX_REQUESTS" to "1",
                                "RATE_LIMIT_READ_WINDOW_SECONDS" to "43200",
                            ),
                        ),
                    )
                val durationAgreement = agreement(fixture, duration, secret)
                val changed = RateAdmission(pool, duration, secret, durationAgreement::check)
                changed.admit(demand)
                assertThrows(RateLimited::class.java) { changed.admit(demand) }
                fixture.connection("core").use { fresh ->
                    fresh.createStatement().use { statement ->
                        statement.executeQuery("SELECT count(*),sum(hits) FROM appdata.request_rate_window").use { rows ->
                            check(rows.next())
                            assertEquals(2, rows.getInt(1))
                            assertEquals(4, rows.getInt(2))
                        }
                    }
                }
                pool.close()
                assertThrows(RateUnavailable::class.java) { changed.admit(demand) }
            } finally {
                pool.close()
            }
        }
    }

    private fun agreement(
        fixture: OwnerFixture,
        profiles: RateProfiles,
        secret: ByteArray,
    ): RateAgreement {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val canary =
            mac
                .doFinal("spoolin-rate-agreement-v1".toByteArray())
                .joinToString("") { "%02x".format(it) }
        val path = fixture.root.resolve("rate-agreement.json")
        Files.writeString(path, """{"profileRevision":"${profiles.revision}","keyCanary":"$canary"}""")
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        return RateAgreement(path, profiles.revision, secret)
    }

    private fun profiles(maximum: Int) =
        RateProfiles(
            ConfigurationSources(
                mapOf(
                    "RATE_LIMIT_READ_MAX_REQUESTS" to maximum.toString(),
                    "RATE_LIMIT_READ_WINDOW_SECONDS" to "86400",
                ),
            ),
        )
}
