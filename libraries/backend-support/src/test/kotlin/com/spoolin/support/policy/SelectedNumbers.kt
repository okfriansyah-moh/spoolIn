package com.spoolin.support.policy

internal data class SelectedNumber(
    val name: String,
    val default: Int,
    val minimum: Int,
    val maximum: Int,
    val mode: String,
    val field: String = "",
    val isMinimum: Boolean = false,
) {
    val alternate: Int get() = if (default > minimum) default - 1 else default + 1

    fun read(source: ConfigurationSources): SettingValue<Int> {
        val actual =
            when (mode) {
                "operational" -> OperationalSettings.resolve(source).getValue(name).value
                "request" -> PolicySettings.resolve(source).requestLimits.getValue(field)
                "artwork" ->
                    PolicySettings
                        .resolve(source)
                        .artwork.maxInputBytes
                        .toInt()
                "smtp" -> {
                    NotificationSettings.resolve(source)
                    source.integer(name, default, minimum, maximum).value
                }
                else -> {
                    val bound = PolicySettings.resolve(source).textBounds.getValue(field)
                    if (isMinimum) bound.minCodePoints else bound.maxCodePoints
                }
            }
        return SettingValue(actual, requireNotNull(source.value(name, default.toString())).source)
    }
}

/** Expected literals are selected from TECH11.1 and the fixed complete-envelope byte vectors. */
internal object SelectedNumbers {
    val operational =
        listOf(
            SelectedNumber("RETRY_MAX_RETRIES", 2, 0, 9, "operational"),
            SelectedNumber("RETRY_BASE_DELAY_MS", 500, 1, 60000, "operational"),
            SelectedNumber("RETRY_MAX_DELAY_MS", 5000, 1, 3600000, "operational"),
            SelectedNumber("RETRY_DEADLINE_MS", 60000, 1000, 3600000, "operational"),
            SelectedNumber("RETRY_MULTIPLIER", 2, 1, 10, "operational"),
            SelectedNumber("MCP_RATE_LIMIT_MAX_BINDINGS", 1024, 1, 100000, "operational"),
            SelectedNumber("RATE_LIMIT_SIGNIN_ACCOUNT_MAX_REQUESTS", 10, 1, 2147483647, "operational"),
            SelectedNumber("RATE_LIMIT_SIGNIN_ACCOUNT_WINDOW_SECONDS", 900, 1, 86400, "operational"),
            SelectedNumber("RATE_LIMIT_SIGNIN_SOURCE_MAX_REQUESTS", 60, 1, 2147483647, "operational"),
            SelectedNumber("RATE_LIMIT_SIGNIN_SOURCE_WINDOW_SECONDS", 900, 1, 86400, "operational"),
            SelectedNumber("RATE_LIMIT_REGISTER_MAX_REQUESTS", 5, 1, 2147483647, "operational"),
            SelectedNumber("RATE_LIMIT_REGISTER_WINDOW_SECONDS", 3600, 1, 86400, "operational"),
            SelectedNumber("RATE_LIMIT_MUTATION_MAX_REQUESTS", 60, 1, 2147483647, "operational"),
            SelectedNumber("RATE_LIMIT_MUTATION_WINDOW_SECONDS", 60, 1, 86400, "operational"),
            SelectedNumber("RATE_LIMIT_READ_MAX_REQUESTS", 120, 1, 2147483647, "operational"),
            SelectedNumber("RATE_LIMIT_READ_WINDOW_SECONDS", 60, 1, 86400, "operational"),
            SelectedNumber("SMTP_PORT", 587, 1, 65535, "smtp"),
            SelectedNumber("ARTWORK_MAX_INPUT_BYTES", 5242880, 1, 5242880, "artwork"),
        )
    val requests =
        listOf(
            SelectedNumber("REQUEST_METADATA_BYTES", 131072, 1, 2147483647, "request", "metadataBytes"),
            SelectedNumber("REQUEST_UPLOAD_ENVELOPE_BYTES", 7100000, 1, 2147483647, "request", "uploadEnvelopeBytes"),
            SelectedNumber("REQUEST_GRAPHQL_QUERY_BYTES", 16384, 1, 2147483647, "request", "graphqlQueryBytes"),
            SelectedNumber("REQUEST_MAX_DEPTH", 8, 1, 2147483647, "request", "maxDepth"),
            SelectedNumber("REQUEST_MAX_FIELDS", 200, 1, 2147483647, "request", "maxFields"),
            SelectedNumber("REQUEST_MAX_ALIASES", 20, 1, 2147483647, "request", "maxAliases"),
            SelectedNumber("REQUEST_MAX_COST", 1000, 1, 2147483647, "request", "maxCost"),
            SelectedNumber("REQUEST_DEFAULT_PAGE", 24, 1, 100, "request", "defaultPage"),
            SelectedNumber("REQUEST_MAX_PAGE", 100, 1, 100, "request", "maxPage"),
        )
    val text =
        listOf(
            SelectedNumber("REGISTRATION_NAME_MIN_CODE_POINTS", 2, 1, 2147483647, "text", "registrationName", true),
            SelectedNumber("REGISTRATION_NAME_MAX_CODE_POINTS", 100, 1, 10406, "text", "registrationName"),
            SelectedNumber("PERSON_NAME_MIN_CODE_POINTS", 1, 1, 2147483647, "text", "personName", true),
            SelectedNumber("PERSON_NAME_MAX_CODE_POINTS", 100, 1, 21797, "text", "personName"),
            SelectedNumber("TITLE_MIN_CODE_POINTS", 1, 1, 2147483647, "text", "title", true),
            SelectedNumber("TITLE_MAX_CODE_POINTS", 255, 1, 21781, "text", "title"),
            SelectedNumber("ORIGINAL_TITLE_MIN_CODE_POINTS", 1, 1, 2147483647, "text", "originalTitle", true),
            SelectedNumber("ORIGINAL_TITLE_MAX_CODE_POINTS", 255, 1, 21777, "text", "originalTitle"),
            SelectedNumber("SYNOPSIS_MIN_CODE_POINTS", 1, 1, 2147483647, "text", "synopsis", true),
            SelectedNumber("SYNOPSIS_MAX_CODE_POINTS", 5000, 1, 21781, "text", "synopsis"),
            SelectedNumber("CREDIT_ROLE_MIN_CODE_POINTS", 1, 1, 2147483647, "text", "creditRole", true),
            SelectedNumber("CREDIT_ROLE_MAX_CODE_POINTS", 100, 1, 21797, "text", "creditRole"),
            SelectedNumber("CHARACTER_NAME_MIN_CODE_POINTS", 1, 1, 2147483647, "text", "characterName", true),
            SelectedNumber("CHARACTER_NAME_MAX_CODE_POINTS", 200, 1, 21772, "text", "characterName"),
            SelectedNumber("BIOGRAPHY_MIN_CODE_POINTS", 1, 1, 2147483647, "text", "biography", true),
            SelectedNumber("BIOGRAPHY_MAX_CODE_POINTS", 5000, 1, 21793, "text", "biography"),
            SelectedNumber("EMAIL_MAX_CODE_POINTS", 254, 1, 254, "text", "email"),
            SelectedNumber("PASSWORD_MIN_CODE_POINTS", 15, 1, 2147483647, "text", "password", true),
            SelectedNumber("PASSWORD_MAX_CODE_POINTS", 128, 1, 512, "text", "password"),
            SelectedNumber("SEARCH_MIN_CODE_POINTS", 0, 0, 2147483647, "text", "search", true),
            SelectedNumber("SEARCH_MAX_CODE_POINTS", 200, 1, 21827, "text", "search"),
            SelectedNumber("SOURCE_URL_MAX_CODE_POINTS", 2048, 1, 2048, "text", "sourceUrl"),
            SelectedNumber("REUSE_BASIS_MAX_CODE_POINTS", 4000, 1, 4000, "text", "reuseBasis"),
        )
    val all = operational + requests + text
}
