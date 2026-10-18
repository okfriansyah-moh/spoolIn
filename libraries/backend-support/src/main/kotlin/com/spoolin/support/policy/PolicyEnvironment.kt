package com.spoolin.support.policy

object PolicyEnvironment {
    private val names =
        listOf(
            "REGISTRATION_NAME",
            "PERSON_NAME",
            "TITLE",
            "ORIGINAL_TITLE",
            "SYNOPSIS",
            "CREDIT_ROLE",
            "CHARACTER_NAME",
            "BIOGRAPHY",
            "PASSWORD",
        ).flatMap { listOf("${it}_MIN_CODE_POINTS", "${it}_MAX_CODE_POINTS") } +
            listOf(
                "EMAIL_MAX_CODE_POINTS",
                "SEARCH_MAX_CODE_POINTS",
                "SOURCE_URL_MAX_CODE_POINTS",
                "REUSE_BASIS_MAX_CODE_POINTS",
                "ARTWORK_MAX_INPUT_BYTES",
                "ARTWORK_FORMATS",
            ) +
            listOf(
                "METADATA_BYTES",
                "UPLOAD_ENVELOPE_BYTES",
                "GRAPHQL_QUERY_BYTES",
                "MAX_DEPTH",
                "MAX_FIELDS",
                "MAX_ALIASES",
                "MAX_COST",
                "DEFAULT_PAGE",
                "MAX_PAGE",
            ).map { "REQUEST_$it" }

    fun resolve(read: (String) -> String?): PublicPolicy {
        val values = names.mapNotNull { name -> read(name)?.let { name to it } }.toMap()
        val explicit = names.mapNotNull { name -> System.getenv(name)?.let { name to it } }.toMap()
        return PolicySettings.resolve(ConfigurationSources(explicit, values))
    }
}
