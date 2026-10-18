package com.spoolin.core.features.crypto

import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun interface ProtectionAlert {
    fun unavailable(rowId: UUID)

    fun failed(code: ProtectionFailure) = Unit
}

class AccountCryptography(
    private val registry: KeyRegistry,
    private val keys: MountedKeys,
    private val nonces: NonceAllocator,
    private val cipher: AeadCipher,
    private val alert: ProtectionAlert,
) {
    fun encrypt(
        rowId: UUID,
        plaintext: ByteArray,
    ): ProtectedEnvelope {
        val key = observeProtection(alert, ProtectionFailure.KEY) { registry.writeKey(KeyPurpose.PII) }
        val bytes =
            observeProtection(alert, ProtectionFailure.KEY) {
                keys.key(key.id, key.purpose) ?: throw ProtectionUnavailable()
            }
        return try {
            val nonce = observeProtection(alert, ProtectionFailure.UNAVAILABLE) { nonces.reserve(key.id, key.purpose) }
            observeProtection(alert, ProtectionFailure.UNAVAILABLE) {
                cipher.encrypt(key.id, bytes, nonce.bytes(), binding(rowId), plaintext)
            }
        } finally {
            bytes.fill(0)
        }
    }

    fun decrypt(
        rowId: UUID,
        envelope: ProtectedEnvelope,
    ): ProtectedRead<ByteArray> {
        val bytes =
            observeProtection(alert, ProtectionFailure.KEY) {
                if (registry.readable(envelope.keyId, KeyPurpose.PII)) {
                    keys.key(envelope.keyId, KeyPurpose.PII)
                } else {
                    null
                }
            }
        val result =
            decryptProtected(alert, bytes) { key ->
                cipher.decrypt(envelope, key, binding(rowId)) { alert.recordFailure(it) }
            }
        if (result == ProtectedRead.Unavailable) alert.unavailable(rowId)
        return result
    }

    fun lookupDigest(
        key: RegisteredKey,
        normalizedEmail: String,
    ): ByteArray {
        require(key.purpose == KeyPurpose.EMAIL_LOOKUP)
        val bytes =
            observeProtection(alert, ProtectionFailure.KEY) {
                keys.key(key.id, KeyPurpose.EMAIL_LOOKUP) ?: throw ProtectionUnavailable()
            }
        return try {
            observeProtection(alert, ProtectionFailure.UNAVAILABLE) {
                Mac
                    .getInstance("HmacSHA256")
                    .also { it.init(SecretKeySpec(bytes, "HmacSHA256")) }
                    .doFinal(("email-lookup-v1\u0000" + normalizedEmail).toByteArray(Charsets.UTF_8))
            }
        } finally {
            bytes.fill(0)
        }
    }

    private fun binding(row: UUID): ProtectionBinding = ProtectionBinding("core", "user_account", row, KeyPurpose.PII)
}
