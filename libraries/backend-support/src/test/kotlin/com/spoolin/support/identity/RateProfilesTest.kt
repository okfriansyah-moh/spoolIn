package com.spoolin.support.identity

import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.ConfigurationViolation
import com.spoolin.support.policy.OperationalSettings
import com.spoolin.support.policy.ValueSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class RateProfilesTest {
    @TestFactory
    fun `every configured profile rejects invalid bounded integers`() =
        OperationalSettings.rate.flatMap { setting ->
            listOf("", "0", "-1", "1.5", "NaN", " 1", "1 ", "2147483648", "${setting.max.toLong() + 1}")
                .distinct()
                .map { invalid ->
                    dynamicTest("${setting.name} rejects $invalid") {
                        assertThrows(ConfigurationViolation::class.java) {
                            RateProfiles(ConfigurationSources(mapOf(setting.name to invalid)))
                        }
                    }
                }
        }

    @TestFactory
    fun `each bound and precedence is resolved by presence`() =
        OperationalSettings.rate.flatMap { setting ->
            listOf(setting.min, setting.max).distinct().map { bound ->
                dynamicTest("${setting.name} accepts $bound") {
                    val profiles = RateProfiles(ConfigurationSources(mapOf(setting.name to "$bound")))
                    assertEquals(bound, profiles.settings.getValue(setting.name).value)
                    assertEquals(ValueSource.ENVIRONMENT, profiles.settings.getValue(setting.name).source)
                }
            }
        }

    @Test
    fun `defaults stable revision and changed operator value have independent outcomes`() {
        val defaults = RateProfiles(ConfigurationSources())
        assertEquals(RateRule(10, 900), defaults.rule(RateProfile.SIGNIN_ACCOUNT))
        assertEquals(RateRule(60, 900), defaults.rule(RateProfile.SIGNIN_SOURCE))
        assertEquals(RateRule(5, 3600), defaults.rule(RateProfile.REGISTER))
        assertEquals(RateRule(60, 60), defaults.rule(RateProfile.MUTATION))
        assertEquals(RateRule(2400, 60), defaults.rule(RateProfile.READ))
        assertEquals(defaults.revision, RateProfiles(ConfigurationSources()).revision)
        assertNotEquals(
            defaults.revision,
            RateProfiles(
                ConfigurationSources(
                    mapOf("RATE_LIMIT_READ_MAX_REQUESTS" to "121"),
                ),
            ).revision,
        )
    }
}
