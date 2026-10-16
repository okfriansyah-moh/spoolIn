package com.spoolin.support.policy

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.EdECKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

private const val MAXIMUM_VERIFIERS = 4

/** Validates supplied key bytes; mounted-file acquisition belongs to the service boundary. */
object ActorKeyRules {
    fun signing(bytes: ByteArray): PrivateKey =
        checked("ACTOR_SIGNING_PRIVATE_KEY_FILE") {
            KeyFactory.getInstance("Ed25519").generatePrivate(PKCS8EncodedKeySpec(bytes)).also(::ed25519)
        }

    fun verifiers(encoded: Map<String, ByteArray>): Map<String, PublicKey> {
        if (encoded.size !in 1..MAXIMUM_VERIFIERS) {
            throw ConfigurationViolation("ACTOR_VERIFY_KEYS_FILE", "Supply one to four Ed25519 verifier entries.")
        }
        return encoded.mapValues { (id, bytes) ->
            CompatibilityFacts.keyId(id, "ACTOR_VERIFY_KEYS_FILE")
            checked("ACTOR_VERIFY_KEYS_FILE") {
                KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(bytes)).also(::ed25519)
            }
        }
    }

    private fun ed25519(key: java.security.Key) {
        require(key is EdECKey && key.params.name == "Ed25519")
    }

    private fun <T> checked(
        setting: String,
        read: () -> T,
    ): T =
        try {
            read()
        } catch (_: GeneralSecurityException) {
            throw ConfigurationViolation(setting, "Supply the selected Ed25519 DER key format.")
        } catch (_: IllegalArgumentException) {
            throw ConfigurationViolation(setting, "Supply the selected Ed25519 DER key format.")
        }
}
