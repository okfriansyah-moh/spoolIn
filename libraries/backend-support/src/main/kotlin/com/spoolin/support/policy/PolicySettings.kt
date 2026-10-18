package com.spoolin.support.policy

private const val REGISTRATION_NAME_MINIMUM = 2
private const val REGISTRATION_NAME_MAXIMUM = 100
private const val PERSON_NAME_MINIMUM = 1
private const val PERSON_NAME_MAXIMUM = 100
private const val TITLE_MINIMUM = 1
private const val TITLE_MAXIMUM = 255
private const val ORIGINAL_TITLE_MINIMUM = 1
private const val ORIGINAL_TITLE_MAXIMUM = 255
private const val SYNOPSIS_MINIMUM = 1
private const val SYNOPSIS_MAXIMUM = 5000
private const val CREDIT_ROLE_MINIMUM = 1
private const val CREDIT_ROLE_MAXIMUM = 100
private const val CHARACTER_NAME_MINIMUM = 1
private const val CHARACTER_NAME_MAXIMUM = 200
private const val BIOGRAPHY_MINIMUM = 1
private const val BIOGRAPHY_MAXIMUM = 5000
private const val EMAIL_MINIMUM = 1
private const val EMAIL_MAXIMUM = 254
private const val PASSWORD_MINIMUM = 15
private const val PASSWORD_MAXIMUM = 128
private const val SEARCH_MINIMUM = 0
private const val SEARCH_MAXIMUM = 200
private const val SOURCE_URL_MINIMUM = 1
private const val SOURCE_URL_MAXIMUM = 2048
private const val REUSE_BASIS_MINIMUM = 1
private const val REUSE_BASIS_MAXIMUM = 4000
private const val METADATA_BYTES_DEFAULT = 131072
private const val UPLOAD_ENVELOPE_BYTES_DEFAULT = 7100000
private const val GRAPHQL_QUERY_BYTES_DEFAULT = 16384
private const val MAX_DEPTH_DEFAULT = 8
private const val MAX_FIELDS_DEFAULT = 200
private const val MAX_ALIASES_DEFAULT = 20
private const val MAX_COST_DEFAULT = 1000
private const val DEFAULT_PAGE_DEFAULT = 24
private const val MAX_PAGE_DEFAULT = 100
private const val PASSWORD_BYTE_CEILING = 512
private const val ESCAPED_SCALAR_BYTES = 6
private const val TWO_ESCAPED_SCALARS_BYTES = 12
private const val ACCOUNT_JSON_OVERHEAD = 50
private const val ACCOUNT_PLAINTEXT_BYTES = 65536
private const val ARTWORK_INPUT_BYTES = 5242880

object PolicySettings {
    private val defaults =
        mapOf(
            "registrationName" to TextBound(REGISTRATION_NAME_MINIMUM, REGISTRATION_NAME_MAXIMUM),
            "personName" to TextBound(PERSON_NAME_MINIMUM, PERSON_NAME_MAXIMUM),
            "title" to TextBound(TITLE_MINIMUM, TITLE_MAXIMUM),
            "originalTitle" to TextBound(ORIGINAL_TITLE_MINIMUM, ORIGINAL_TITLE_MAXIMUM, false),
            "synopsis" to TextBound(SYNOPSIS_MINIMUM, SYNOPSIS_MAXIMUM),
            "creditRole" to TextBound(CREDIT_ROLE_MINIMUM, CREDIT_ROLE_MAXIMUM),
            "characterName" to TextBound(CHARACTER_NAME_MINIMUM, CHARACTER_NAME_MAXIMUM, false),
            "biography" to TextBound(BIOGRAPHY_MINIMUM, BIOGRAPHY_MAXIMUM, false),
            "email" to TextBound(EMAIL_MINIMUM, EMAIL_MAXIMUM),
            "password" to TextBound(PASSWORD_MINIMUM, PASSWORD_MAXIMUM, normalization = "EXACT"),
            "search" to TextBound(SEARCH_MINIMUM, SEARCH_MAXIMUM, false),
            "sourceUrl" to TextBound(SOURCE_URL_MINIMUM, SOURCE_URL_MAXIMUM),
            "reuseBasis" to TextBound(REUSE_BASIS_MINIMUM, REUSE_BASIS_MAXIMUM),
        )
    private val requests =
        mapOf(
            "metadataBytes" to METADATA_BYTES_DEFAULT,
            "uploadEnvelopeBytes" to UPLOAD_ENVELOPE_BYTES_DEFAULT,
            "graphqlQueryBytes" to GRAPHQL_QUERY_BYTES_DEFAULT,
            "maxDepth" to MAX_DEPTH_DEFAULT,
            "maxFields" to MAX_FIELDS_DEFAULT,
            "maxAliases" to MAX_ALIASES_DEFAULT,
            "maxCost" to MAX_COST_DEFAULT,
            "defaultPage" to DEFAULT_PAGE_DEFAULT,
            "maxPage" to MAX_PAGE_DEFAULT,
        )

    fun key(field: String): String = field.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

    fun resolve(source: ConfigurationSources): PublicPolicy {
        val limits =
            requests.mapValues { (name, default) ->
                source
                    .integer(
                        "REQUEST_${key(name)}",
                        default,
                        1,
                        if (name.endsWith("Page")) MAX_PAGE_DEFAULT else Int.MAX_VALUE,
                    ).value
            }
        if (limits.getValue("defaultPage") > limits.getValue("maxPage")) {
            throw ConfigurationViolation("REQUEST_DEFAULT_PAGE", "Use a default page no larger than the maximum page.")
        }
        val bounds =
            defaults.mapValues { (name, default) ->
                resolveText(name, default, source, limits.getValue("metadataBytes"))
            }
        accountCapacity(bounds)
        RequestCapacity.validate(bounds, limits)
        return PublicPolicy(bounds, artwork(source), limits)
    }

    private fun resolveText(
        name: String,
        default: TextBound,
        source: ConfigurationSources,
        metadata: Int,
    ): TextBound {
        val prefix = key(name)
        val min =
            if (name in setOf("email", "sourceUrl", "reuseBasis")) {
                default.minCodePoints
            } else {
                source
                    .integer(
                        "${prefix}_MIN_CODE_POINTS",
                        default.minCodePoints,
                        if (name == "search") 0 else 1,
                        Int.MAX_VALUE,
                    ).value
            }
        val ceiling =
            when (name) {
                "password" -> PASSWORD_BYTE_CEILING
                "email" -> EMAIL_MAXIMUM
                "sourceUrl" -> SOURCE_URL_MAXIMUM
                "reuseBasis" -> REUSE_BASIS_MAXIMUM
                else ->
                    (metadata.toLong() / ESCAPED_SCALAR_BYTES)
                        .coerceAtLeast(
                            1,
                        ).toInt()
            }
        val max = source.integer("${prefix}_MAX_CODE_POINTS", default.maxCodePoints, 1, ceiling).value
        if (min >
            max
        ) {
            throw ConfigurationViolation("${prefix}_MIN_CODE_POINTS", "Use a minimum no larger than the maximum.")
        }
        return TextBound(min, max, default.required, default.normalization)
    }

    private fun accountCapacity(bounds: Map<String, TextBound>) {
        val worst =
            bounds.getValue("registrationName").maxCodePoints.toLong() * ESCAPED_SCALAR_BYTES +
                bounds.getValue("email").maxCodePoints.toLong() * TWO_ESCAPED_SCALARS_BYTES + ACCOUNT_JSON_OVERHEAD
        if (worst >
            ACCOUNT_PLAINTEXT_BYTES
        ) {
            throw ConfigurationViolation(
                "REGISTRATION_NAME_MAX_CODE_POINTS",
                "Keep the complete account payload within 65536 UTF-8 bytes.",
            )
        }
    }

    private fun artwork(source: ConfigurationSources): ArtworkBounds {
        val max = source.integer("ARTWORK_MAX_INPUT_BYTES", ARTWORK_INPUT_BYTES, 1, ARTWORK_INPUT_BYTES).value.toLong()
        val selected = requireNotNull(source.value("ARTWORK_FORMATS", "JPEG,PNG,WEBP")).value.split(',')
        if (selected.isEmpty() || selected.distinct().size != selected.size ||
            selected.any { it !in setOf("JPEG", "PNG", "WEBP") }
        ) {
            throw ConfigurationViolation("ARTWORK_FORMATS", "Select a nonempty subset of JPEG, PNG, WEBP.")
        }
        return ArtworkBounds(maxInputBytes = max, formats = selected.sorted())
    }
}
