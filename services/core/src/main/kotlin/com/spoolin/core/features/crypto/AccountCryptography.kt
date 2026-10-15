package com.spoolin.core.features.crypto

import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun interface ProtectionAlert {
    fun unavailable(rowId: UUID)
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
        val key = registry.writeKey(KeyPurpose.PII)
        val bytes = keys.key(key.id, key.purpose) ?: throw ProtectionUnavailable()
        return try {
            val nonce = nonces.reserve(key.id, key.purpose)
            cipher.encrypt(key.id, bytes, nonce.bytes(), binding(rowId), plaintext)
        } finally {
            bytes.fill(0)
        }
    }

    fun decrypt(
        rowId: UUID,
        envelope: ProtectedEnvelope,
    ): ProtectedRead<ByteArray> {
        val bytes =
            if (registry.readable(
                    envelope.keyId,
                    KeyPurpose.PII,
                )
            ) {
                keys.key(envelope.keyId, KeyPurpose.PII)
            } else {
                null
            }
        val result =
            if (bytes == null) {
                ProtectedRead.Unavailable
            } else {
                try {
                    cipher.decrypt(envelope, bytes, binding(rowId))
                } finally {
                    bytes.fill(0)
                }
            }
        if (result == ProtectedRead.Unavailable) alert.unavailable(rowId)
        return result
    }

    fun lookupDigest(
        key: RegisteredKey,
        normalizedEmail: String,
    ): ByteArray {
        require(key.purpose == KeyPurpose.EMAIL_LOOKUP)
        val bytes = keys.key(key.id, KeyPurpose.EMAIL_LOOKUP) ?: throw ProtectionUnavailable()
        return try {
            Mac
                .getInstance("HmacSHA256")
                .also { it.init(SecretKeySpec(bytes, "HmacSHA256")) }
                .doFinal(("email-lookup-v1\u0000" + normalizedEmail).toByteArray(Charsets.UTF_8))
        } finally {
            bytes.fill(0)
        }
    }

    private fun binding(row: UUID): ProtectionBinding = ProtectionBinding("core", "user_account", row, KeyPurpose.PII)
}
