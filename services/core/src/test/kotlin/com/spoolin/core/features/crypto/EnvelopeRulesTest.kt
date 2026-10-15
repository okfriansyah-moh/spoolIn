package com.spoolin.core.features.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class EnvelopeRulesTest {
    private val cipher = AeadCipher()
    private val key = ByteArray(32) { it.toByte() }
    private val nonce =
        java.util.HexFormat
            .of()
            .parseHex("000000000000000000000001")
    private val row = UUID.fromString("ebd4fba8-bf29-4dce-8d39-48516ac7f9da")
    private val binding = ProtectionBinding("core", "user_account", row, KeyPurpose.PII)
    private val plain = "synthetic protected value: é".toByteArray()

    @Test
    fun matchingEnvelopeReturnsExactBytesAndCopiesItsArrays() {
        val envelope = cipher.encrypt("pii-1", key, nonce, binding, plain)
        assertArrayEquals(plain, (cipher.decrypt(envelope, key, binding) as ProtectedRead.Value).value)
        assertEquals(plain.size + 16, envelope.cipher().size)
        envelope.nonce().fill(0)
        envelope.cipher().fill(0)
        assertArrayEquals(plain, (cipher.decrypt(envelope, key, binding) as ProtectedRead.Value).value)
    }

    @Test
    fun everyAlteredEnvelopeFieldFailsAuthentication() {
        val valid = cipher.encrypt("pii-1", key, nonce, binding, plain)
        listOf(
            ProtectedEnvelope(2, valid.keyId, valid.nonce(), valid.cipher()),
            ProtectedEnvelope(1, "pii-2", valid.nonce(), valid.cipher()),
            ProtectedEnvelope(1, valid.keyId, flipped(valid.nonce(), 11), valid.cipher()),
            ProtectedEnvelope(1, valid.keyId, valid.nonce(), flipped(valid.cipher(), 0)),
            ProtectedEnvelope(1, valid.keyId, valid.nonce(), flipped(valid.cipher(), valid.cipher().lastIndex)),
            ProtectedEnvelope(1, valid.keyId, ByteArray(11), valid.cipher()),
            ProtectedEnvelope(1, valid.keyId, valid.nonce(), ByteArray(15)),
        ).forEach { assertInstanceOf(ProtectedRead.Unavailable::class.java, cipher.decrypt(it, key, binding)) }
        assertInstanceOf(ProtectedRead.Unavailable::class.java, cipher.decrypt(valid, flipped(key, 0), binding))
        assertInstanceOf(ProtectedRead.Unavailable::class.java, cipher.decrypt(valid, ByteArray(31), binding))
    }

    @Test
    fun everyAlteredAadBindingFailsAuthentication() {
        val valid = cipher.encrypt("pii-1", key, nonce, binding, plain)
        listOf(
            binding.copy(service = "catalog"),
            binding.copy(table = "other"),
            binding.copy(rowId = UUID.fromString("59807635-d933-43f3-a81e-8063b4e112a7")),
            binding.copy(purpose = KeyPurpose.DRAFT),
            binding.copy(purpose = KeyPurpose.EMAIL_LOOKUP),
        ).forEach { assertInstanceOf(ProtectedRead.Unavailable::class.java, cipher.decrypt(valid, key, it)) }
    }

    @Test
    fun plaintextOctetLimitIsInclusive() {
        val max = ByteArray(65536) { 42 }
        val envelope = cipher.encrypt("pii-1", key, nonce, binding, max)
        assertEquals(65552, envelope.cipher().size)
        assertArrayEquals(max, (cipher.decrypt(envelope, key, binding) as ProtectedRead.Value).value)
        assertThrows(IllegalArgumentException::class.java) {
            cipher.encrypt("pii-1", key, nonce, binding, ByteArray(65537))
        }
    }

    private fun flipped(
        bytes: ByteArray,
        index: Int,
    ): ByteArray = bytes.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
}
