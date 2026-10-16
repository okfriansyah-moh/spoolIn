package com.spoolin.support.policy

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

private const val FIRST_PRINTABLE_CODE_POINT = 32

data class ArtworkBounds(
    val maxInputBytes: Long = 5242880,
    val formats: List<String> = listOf("JPEG", "PNG", "WEBP"),
    val maxDimensionPx: Int = 8192,
    val maxPixels: Long = 40000000,
    val maxDecodedBytes: Long = 160000000,
    val maxOutputBytes: Long = 8388608,
) {
    fun projection(): Map<String, Any> =
        mapOf(
            "maxInputBytes" to maxInputBytes.toString(),
            "formats" to formats,
            "maxDimensionPx" to maxDimensionPx,
            "maxPixels" to maxPixels.toString(),
            "maxDecodedBytes" to maxDecodedBytes.toString(),
            "maxOutputBytes" to maxOutputBytes.toString(),
        )
}

class PublicPolicy(
    textBounds: Map<String, TextBound>,
    artwork: ArtworkBounds,
    requestLimits: Map<String, Int>,
) {
    val artwork: ArtworkBounds =
        artwork.copy(
            formats = java.util.Collections.unmodifiableList(artwork.formats.toList()),
        )
    val textBounds: Map<String, TextBound> = java.util.Collections.unmodifiableMap(textBounds.toMap())
    val requestLimits: Map<String, Int> = java.util.Collections.unmodifiableMap(requestLimits.toMap())
    val revision: String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(PolicyJson.encode(body()).toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun projection(): Map<String, Any> = body() + ("revision" to revision)

    private fun body(): Map<String, Any> =
        mapOf(
            "textBounds" to
                textBounds.mapValues { (_, bound) ->
                    mapOf(
                        "minCodePoints" to bound.minCodePoints,
                        "maxCodePoints" to bound.maxCodePoints,
                        "required" to bound.required,
                        "normalization" to bound.normalization,
                    )
                },
            "artwork" to artwork.projection(),
            "requestLimits" to requestLimits,
        )

    fun agrees(revisions: Collection<String>): Boolean = revisions.isNotEmpty() && revisions.all { it == revision }
}

object PolicyJson {
    fun encode(value: Any?): String =
        when (value) {
            null -> "null"
            is String -> quote(value)
            is Boolean, is Int, is Long -> value.toString()
            is List<*> -> value.joinToString(",", "[", "]") { encode(it) }
            is Map<*, *> ->
                value.entries.sortedBy { it.key.toString() }.joinToString(",", "{", "}") {
                    require(it.key is String)
                    quote(it.key.toString()) + ":" + encode(it.value)
                }
            else -> error("Unsupported policy value type")
        }

    private fun quote(value: String): String {
        TextRules.scalars(value, "policy")
        return value
            .map { char ->
                when {
                    char == '"' -> "\\\""
                    char == '\\' -> "\\\\"
                    char.code < FIRST_PRINTABLE_CODE_POINT -> "\\u%04x".format(char.code)
                    else -> char.toString()
                }
            }.joinToString("", "\"", "\"")
    }
}
