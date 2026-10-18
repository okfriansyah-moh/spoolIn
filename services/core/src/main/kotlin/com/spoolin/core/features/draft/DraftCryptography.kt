package com.spoolin.core.features.draft

import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.crypto.ProtectionUnavailable
import java.util.UUID

/**
 * Encrypts and decrypts draft bodies using the DRAFT [KeyPurpose].
 *
 * Nonce allocation is a separately committed operation per the allocator contract (§7.3.1).
 * The caller must reserve the nonce BEFORE acquiring domain locks.
 */
class DraftCryptography(
    private val registry: KeyRegistry,
    private val keys: MountedKeys,
    private val nonces: NonceAllocator,
    private val cipher: AeadCipher,
) {
    /**
     * Reserves a nonce, encrypts [plaintext] under the current DRAFT write key, and returns
     * the resulting [ProtectedEnvelope].  The nonce reservation is committed in a separate
     * transaction by [NonceAllocator]; the envelope is ready to use inside the caller's domain
     * transaction.
     */
    fun encrypt(
        rowId: UUID,
        plaintext: ByteArray,
    ): ProtectedEnvelope {
        val key = registry.writeKey(KeyPurpose.DRAFT)
        val bytes = keys.key(key.id, KeyPurpose.DRAFT) ?: throw ProtectionUnavailable()
        return try {
            val nonce = nonces.reserve(key.id, KeyPurpose.DRAFT)
            cipher.encrypt(key.id, bytes, nonce.bytes(), binding(rowId), plaintext)
        } finally {
            bytes.fill(0)
        }
    }

    fun requireCurrentWrite(connection: java.sql.Connection, envelope: ProtectedEnvelope) {
        registry.requireCurrentWrite(connection, envelope.keyId, KeyPurpose.DRAFT)
    }

    /**
     * Decrypts [envelope] for [rowId].  Returns [ProtectedRead.Unavailable] when the key is
     * unreadable or authentication fails; never falls back to plaintext.
     */
    fun decrypt(
        rowId: UUID,
        envelope: ProtectedEnvelope,
    ): ProtectedRead<ByteArray> {
        val bytes =
            if (registry.readable(envelope.keyId, KeyPurpose.DRAFT)) {
                keys.key(envelope.keyId, KeyPurpose.DRAFT)
            } else {
                null
            }
        return if (bytes == null) {
            ProtectedRead.Unavailable
        } else {
            try {
                cipher.decrypt(envelope, bytes, binding(rowId))
            } finally {
                bytes.fill(0)
            }
        }
    }

    private fun binding(row: UUID): ProtectionBinding = ProtectionBinding("core", "form_draft", row, KeyPurpose.DRAFT)
}
