package com.spoolin.support.identity

import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.OperationalSettings
import com.spoolin.support.policy.SettingValue
import java.security.MessageDigest

enum class RateProfile { SIGNIN_ACCOUNT, SIGNIN_SOURCE, REGISTER, MUTATION, READ }

enum class RateScope { SOURCE, ACCOUNT_IDENTIFIER, CREDENTIAL }

data class RateRule(
    val maximum: Int,
    val seconds: Int,
)

class RateProfiles(
    source: ConfigurationSources,
) {
    val settings: Map<String, SettingValue<Int>> =
        OperationalSettings.rate.associate { setting ->
            setting.name to source.integer(setting.name, setting.default, setting.min, setting.max)
        }
    val revision: String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(
                settings
                    .toSortedMap()
                    .entries
                    .joinToString("\n") { "${it.key}=${it.value.value}" }
                    .toByteArray(),
            ).joinToString("") { "%02x".format(it) }

    fun rule(profile: RateProfile): RateRule =
        RateRule(
            settings.getValue("RATE_LIMIT_${profile.name}_MAX_REQUESTS").value,
            settings.getValue("RATE_LIMIT_${profile.name}_WINDOW_SECONDS").value,
        )

    override fun toString(): String = "RateProfiles(redacted)"
}
