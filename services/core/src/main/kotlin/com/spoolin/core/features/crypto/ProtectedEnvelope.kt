package com.spoolin.core.features.crypto

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.security.GeneralSecurityException
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class KeyPurpose { PII, DRAFT, EMAIL_LOOKUP }

enum class KeyState { WRITE, READ, RETIRED }

class ProtectedEnvelope(
    val version: Int,
    val keyId: String,
    nonce: ByteArray,
    cipher: ByteArray,
) {
    private val nonceBytes = nonce.copyOf()
    private val cipherBytes = cipher.copyOf()

    fun nonce(): ByteArray = nonceBytes.copyOf()

    fun cipher(): ByteArray = cipherBytes.copyOf()
}

data class ProtectionBinding(
    val service: String,
    val table: String,
    val rowId: UUID,
    val purpose: KeyPurpose,
)

sealed interface ProtectedRead<out T> {
    class Value<T>(
        val value: T,
    ) : ProtectedRead<T>

    data object Unavailable : ProtectedRead<Nothing>
}

class AeadCipher {
    fun encrypt(
        keyId: String,
        key: ByteArray,
        nonce: ByteArray,
        binding: ProtectionBinding,
        plaintext: ByteArray,
    ): ProtectedEnvelope {
        require(plaintext.size <= MAX_PROTECTED_BYTES) { "Protected value is too large" }
        val engine = engine(Cipher.ENCRYPT_MODE, key, nonce)
        engine.updateAAD(aad(1, keyId, binding))
        return ProtectedEnvelope(1, keyId, nonce, engine.doFinal(plaintext))
    }

    fun decrypt(
        envelope: ProtectedEnvelope,
        key: ByteArray,
        binding: ProtectionBinding,
        failure: (ProtectionFailure) -> Unit = { },
    ): ProtectedRead<ByteArray> =
        try {
            require(envelope.version == 1 && envelope.cipher().size in GCM_TAG_BYTES..MAX_CIPHER_BYTES)
            val engine = engine(Cipher.DECRYPT_MODE, key, envelope.nonce())
            engine.updateAAD(aad(envelope.version, envelope.keyId, binding))
            ProtectedRead.Value(engine.doFinal(envelope.cipher()))
        } catch (_: AEADBadTagException) {
            unavailable(ProtectionFailure.AUTHENTICATION, failure)
        } catch (_: GeneralSecurityException) {
            unavailable(ProtectionFailure.UNAVAILABLE, failure)
        } catch (_: IllegalArgumentException) {
            unavailable(if (key.size == AES_KEY_BYTES) ProtectionFailure.AUTHENTICATION else ProtectionFailure.KEY, failure)
        }

    private fun unavailable(
        code: ProtectionFailure,
        failure: (ProtectionFailure) -> Unit,
    ): ProtectedRead.Unavailable {
        runCatching { failure(code) }
        return ProtectedRead.Unavailable
    }

    private fun engine(
        mode: Int,
        key: ByteArray,
        nonce: ByteArray,
    ): Cipher {
        require(key.size == AES_KEY_BYTES && nonce.size == NONCE_BYTES) { "Invalid protection material" }
        return Cipher.getInstance("AES/GCM/NoPadding").also {
            it.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        }
    }

    private fun aad(
        version: Int,
        keyId: String,
        binding: ProtectionBinding,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(version)
            listOf(binding.service, binding.table, binding.rowId.toString(), binding.purpose.name, keyId).forEach {
                val bytes = it.toByteArray(UTF_8)
                data.writeInt(bytes.size)
                data.write(bytes)
            }
        }
        return output.toByteArray()
    }
}

internal const val AES_KEY_BYTES = 32
internal const val NONCE_BYTES = 12
internal const val GCM_TAG_BITS = 128
internal const val GCM_TAG_BYTES = 16
internal const val MAX_PROTECTED_BYTES = 65536
internal const val MAX_CIPHER_BYTES = 65552
