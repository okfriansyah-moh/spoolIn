package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.queryBound
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.sql.DataSource

private const val CANARY_NONCE_BYTES = 12
private const val HMAC_DIGEST_BYTES = 32

/** Validates target material and consumes a committed fresh nonce before READ-key promotion. */
internal class RotationTargetCanary(
    private val dataSource: DataSource,
    private val keys: MountedKeys,
    private val id: String,
    private val purpose: KeyPurpose,
) {
    fun verify() {
        val material = keys.key(id, purpose) ?: error("Required protected rotation key is unavailable")
        try {
            if (purpose == KeyPurpose.EMAIL_LOOKUP) lookup(material) else encryption(material)
        } finally {
            material.fill(0)
        }
    }

    private fun lookup(material: ByteArray) {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(material, "HmacSHA256"))
        check(mac.doFinal("rotation-canary-v1".toByteArray()).size == HMAC_DIGEST_BYTES)
    }

    private fun counter(): Long =
        dataSource.committed { connection ->
            connection
                .queryBound(
                    "UPDATE appdata.crypto_key_version SET next_nonce=next_nonce+1 WHERE id=? AND purpose=? " +
                        "AND state IN ('WRITE','READ') AND next_nonce<9223372036854775807 " +
                            "RETURNING next_nonce-1 AS nonce",
                    listOf(id, purpose.name),
                ) { it.getLong("nonce") }
                .single()
        }

    private fun encryption(material: ByteArray) {
        val nonce =
            ByteBuffer
                .allocate(CANARY_NONCE_BYTES)
                .putInt(0)
                .putLong(counter())
                .array()
        val binding =
            ProtectionBinding(
                "core",
                if (purpose == KeyPurpose.PII) "user_account" else "form_draft",
                UUID.randomUUID(),
                purpose,
            )
        val clear = "rotation-canary-v1".toByteArray()
        try {
            roundTrip(material, nonce, binding, clear)
        } finally {
            clear.fill(0)
        }
    }

    private fun roundTrip(
        material: ByteArray,
        nonce: ByteArray,
        binding: ProtectionBinding,
        clear: ByteArray,
    ) {
        val envelope = AeadCipher().encrypt(id, material, nonce, binding, clear)
        val read = AeadCipher().decrypt(envelope, material, binding)
        check(read is ProtectedRead.Value && MessageDigest.isEqual(clear, read.value))
        if (read is ProtectedRead.Value) read.value.fill(0)
    }
}
