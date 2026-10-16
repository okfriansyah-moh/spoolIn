package com.spoolin.core.boundary.admission

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.core.features.crypto.queryBound
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
            prepare(fixture)
            fixture.pool("core").use { pool -> replicaClaims(fixture, pool) }
        }
    }

    private fun prepare(fixture: OwnerFixture) {
        fixture.membership("core", "grant")
        fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("core")
        fixture.membership("core", "revoke")
    }

    private fun replicaClaims(
        fixture: OwnerFixture,
        pool: javax.sql.DataSource,
    ) {
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
        assertEquals(1, compete(replicas, demand).count { it })
        assertThrows(RateLimited::class.java) { RateAdmission(pool, profiles, secret, agreement::check).admit(demand) }
        assertCounters(fixture, "SELECT hits FROM appdata.request_rate_window", listOf(5))
    }

    private fun compete(
        replicas: List<RateAdmission>,
        demand: List<RateDemand>,
    ): List<Boolean> {
        val barrier = CyclicBarrier(8)
        val executor = Executors.newFixedThreadPool(8)
        val futures = (0..7).map { index -> executor.submit<Boolean> { admitAtBarrier(replicas[index % 2], demand, barrier) } }
        return try {
            futures.map { it.get(20, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun admitAtBarrier(
        replica: RateAdmission,
        demand: List<RateDemand>,
        barrier: CyclicBarrier,
    ): Boolean {
        barrier.await(10, TimeUnit.SECONDS)
        return try {
            replica.admit(demand)
            true
        } catch (_: RateLimited) {
            false
        }
    }

    @Test
    fun `denied profile consumes available companion and deduplicates exact dimension only`() {
        OwnerFixture("t008-admission-companions.json").start().use { fixture ->
            prepare(fixture)
            fixture.pool("core").use { pool -> companionClaims(fixture, pool) }
        }
    }

    private fun companionClaims(
        fixture: OwnerFixture,
        pool: javax.sql.DataSource,
    ) {
        val profiles = profiles(1)
        val secret = ByteArray(32) { 20 }
        val agreement = agreement(fixture, profiles, secret)
        val limiter = RateAdmission(pool, profiles, secret, agreement::check)
        val source = RateDemand(RateProfile.READ, RateScope.SOURCE, "same")
        val credential = RateDemand(RateProfile.READ, RateScope.CREDENTIAL, "same")
        limiter.admit(listOf(source, source))
        assertThrows(RateLimited::class.java) { limiter.admit(listOf(source, credential)) }
        assertThrows(RateLimited::class.java) { limiter.admit(listOf(credential)) }
        assertCounters(fixture, "SELECT count(*),sum(hits) FROM appdata.request_rate_window", listOf(2, 2))
    }

    @Test
    fun `explicit max and duration transition retains prior counters and outage never admits`() {
        OwnerFixture("t008-admission-transition.json").start().use { fixture ->
            prepare(fixture)
            fixture.pool("core").use { pool -> transitionClaims(fixture, pool) }
        }
    }

    private fun transitionClaims(
        fixture: OwnerFixture,
        pool: com.zaxxer.hikari.HikariDataSource,
    ) {
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
        val duration = durationProfile()
        val durationAgreement = agreement(fixture, duration, secret)
        val changed = RateAdmission(pool, duration, secret, durationAgreement::check)
        changed.admit(demand)
        assertThrows(RateLimited::class.java) { changed.admit(demand) }
        assertCounters(fixture, "SELECT count(*),sum(hits) FROM appdata.request_rate_window", listOf(2, 4))
        pool.close()
        assertThrows(RateUnavailable::class.java) { changed.admit(demand) }
    }

    private fun durationProfile() =
        RateProfiles(
            ConfigurationSources(
                mapOf("RATE_LIMIT_READ_MAX_REQUESTS" to "1", "RATE_LIMIT_READ_WINDOW_SECONDS" to "43200"),
            ),
        )

    private fun assertCounters(
        fixture: OwnerFixture,
        sql: String,
        expected: List<Int>,
    ) {
        val actual =
            fixture.connection("core", "runtime").use { fresh ->
                fresh.queryBound(sql, emptyList()) { row -> readCounters(row, expected.size) }.single()
            }
        assertEquals(expected, actual)
    }

    private fun readCounters(
        row: java.sql.ResultSet,
        size: Int,
    ): List<Int> = (1..size).map { row.getInt(it) }

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
