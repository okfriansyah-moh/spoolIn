package com.spoolin.core.features.draft

import tools.jackson.databind.ObjectMapper

private const val MAX_PLAINTEXT_BYTES = 65536

/** Keys that must never appear in any draft body regardless of target kind. */
private val REJECTED_KEYS =
    setOf(
        "password",
        "passwordHash",
        "piiCipher",
        "piiNonce",
        "piiKeyId",
        "bodyCipher",
        "bodyNonce",
        "bodyKeyId",
        "sessionToken",
        "csrfToken",
        "authToken",
    )

/** Allowed top-level keys per target kind (TD-ROW-L3245 – L3247). */
private val ALLOWED_KEYS =
    mapOf(
        "MOVIE" to
            setOf(
                "title", "originalTitle", "synopsis", "genreIds", "release",
                "runtimeMinutes", "originalLanguage", "synopsisLanguage",
                "countries", "tvStatus", "tvEnd", "releaseContext",
            ),
        "PERSON" to setOf("name", "biography"),
        "CREDIT" to setOf("titleId", "personId", "roleId", "characters"),
    )

/**
 * Codec for the encrypted draft body.
 *
 * Validates the plaintext shape: JSON object, allowed target-specific keys, no private/credential
 * keys, ≤ 65 536 plaintext bytes.  Draft values are preserved unnormalized (TD-C-023); final-write
 * normalization is applied at Catalog commit time, not here.
 *
 * ATDD-293: incomplete or over-policy non-secret values are accepted within the envelope;
 * unknown/private/credential/file keys and oversize payloads reject.
 */
class DraftPayload {
    private val mapper = ObjectMapper()

    /**
     * Validates [json] against the permitted shape for [targetKind] and returns the UTF-8 encoded
     * bytes ready for encryption.
     *
     * @throws IllegalArgumentException if the JSON is invalid, the kind is unknown, any rejected
     *   key is present, any unknown field is present, or the payload exceeds the size limit.
     */
    fun encode(
        targetKind: String,
        json: String,
    ): ByteArray {
        val allowed = ALLOWED_KEYS[targetKind] ?: throw IllegalArgumentException(
            "Draft targetKind must be MOVIE, PERSON, or CREDIT",
        )
        val bytes = json.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_PLAINTEXT_BYTES) { "Draft payload exceeds the 65 536-byte limit" }
        val node = runCatching { mapper.readTree(json) }.getOrElse {
            throw IllegalArgumentException("Draft values must be a valid JSON object")
        }
        require(node.isObject) { "Draft values must be a JSON object" }
        val keys = node.properties().asSequence().map { it.key }.toSet()
        require(keys.none { it in REJECTED_KEYS }) {
            "Draft values must not contain private or credential fields"
        }
        require(keys.all { it in allowed }) { "Draft values contain fields not permitted for $targetKind" }
        return bytes
    }

    /** Decodes the decrypted plaintext bytes back to a JSON string. */
    fun decode(bytes: ByteArray): String = String(bytes, Charsets.UTF_8)
}
