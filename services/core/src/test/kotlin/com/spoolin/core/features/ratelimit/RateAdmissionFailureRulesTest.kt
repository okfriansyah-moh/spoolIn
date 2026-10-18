package com.spoolin.core.features.ratelimit

import com.spoolin.support.identity.RateProfile
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.identity.RateScope
import com.spoolin.support.policy.ConfigurationSources
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.AbstractDataSource
import java.security.GeneralSecurityException
import java.sql.Connection
import java.sql.SQLException

/** Unit failure injection at the JDBC provider boundary; this does not replace PostgreSQL rate tests. */
class RateAdmissionFailureRulesTest {
    private val demand = listOf(RateDemand(RateProfile.READ, RateScope.CREDENTIAL, "synthetic-credential"))

    @Test
    fun `checked and runtime provider failures all reject admission without exposing their cause`() {
        for (dependencyFailure in listOf(
            SQLException("synthetic private database detail"),
            IllegalStateException("synthetic private pool detail"),
            GeneralSecurityException("synthetic private key detail"),
            InterruptedException("synthetic interrupted dependency"),
        )) {
            val admission = admission { throw dependencyFailure }
            val failure = assertThrows(RateUnavailable::class.java) { admission.admit(demand) }
            assertEquals("Request admission unavailable", failure.message)
            assertNull(failure.cause)
        }
    }

    @Test
    fun `fatal errors and agreement exceptions retain their original propagation`() {
        val fatal = AssertionError("synthetic fatal provider")
        assertSame(fatal, assertThrows(AssertionError::class.java) { admission { throw fatal }.admit(demand) })
        val agreementFailure = IllegalStateException("synthetic agreement failure")
        val admission = admission({ throw agreementFailure }) { error("Database must not be reached") }
        assertSame(agreementFailure, assertThrows(IllegalStateException::class.java) { admission.admit(demand) })
    }

    private fun admission(
        agreement: () -> Unit = {},
        connect: () -> Connection,
    ): RateAdmission {
        val provider =
            object : AbstractDataSource() {
                override fun getConnection(): Connection = connect()

                override fun getConnection(
                    username: String,
                    password: String,
                ): Connection = connect()
            }
        return RateAdmission(provider, RateProfiles(ConfigurationSources()), ByteArray(32) { 17 }, agreement)
    }
}
