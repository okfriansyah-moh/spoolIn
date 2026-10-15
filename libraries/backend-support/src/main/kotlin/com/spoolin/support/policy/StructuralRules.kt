package com.spoolin.support.policy

import java.util.Locale

private const val MAXIMUM_LANGUAGE_CHARACTERS = 35
private const val MAXIMUM_MANIFEST_VERSION_CHARACTERS = 64

object StructuralRules {
    fun country(value: String): String {
        if (value !in Locale.getISOCountries()) throw InputViolation("countries", "Use an uppercase ISO country code.")
        return value
    }

    fun language(value: String): String {
        if (value.length !in 1..MAXIMUM_LANGUAGE_CHARACTERS ||
            !value.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*"))
        ) {
            throw InputViolation("language", "Use a BCP47 language tag up to 35 characters.")
        }
        try {
            Locale.Builder().setLanguageTag(value).build()
        } catch (_: java.util.IllformedLocaleException) {
            throw InputViolation("language", "Use a valid BCP47 language tag.")
        }
        return value
    }

    fun manifest(
        version: String,
        hash: String,
    ) {
        if (version.isBlank() || version.length > MAXIMUM_MANIFEST_VERSION_CHARACTERS ||
            !hash.matches(Regex("[0-9a-f]{64}"))
        ) {
            throw ConfigurationViolation("manifest", "Supply the exact frozen manifest identity and SHA-256.")
        }
    }
}
