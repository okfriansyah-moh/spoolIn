package com.spoolin.support.receipts

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class Comparison { REPLAY, CONFLICT, HIDDEN }

data class OperationIntent(
    val id: UUID,
    val actorScope: String,
    val kind: String,
    val policyRevision: String,
    val fields: List<Pair<String, String?>>,
) {
    init {
        require(actorScope.length in 1..96 && kind.length in 1..64)
        require(fields.map { it.first }.distinct().size == fields.size)
    }

    fun serialized(): ByteArray {
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { output ->
            listOf(actorScope, kind, policyRevision).forEach { output.text(it) }
            output.writeInt(fields.size)
            fields.forEach { (name, value) ->
                output.text(name)
                output.writeBoolean(value != null)
                value?.let { output.text(it) }
            }
        }
        return buffer.toByteArray()
    }

    override fun toString(): String = "OperationIntent(id=$id, content=redacted)"
}

private fun DataOutputStream.text(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeInt(bytes.size)
    write(bytes)
}

class IntentHashes(
    keys: Map<String, ByteArray>,
    val active: String,
) {
    private val material = keys.mapValues { it.value.copyOf() }

    init {
        require(material.containsKey(active)) { "Operation comparison key is unavailable" }
        require(material.all { it.key.matches(Regex("[a-zA-Z0-9_-]{1,64}")) && it.value.size == 32 })
    }

    fun hash(
        intent: OperationIntent,
        keyId: String?,
    ): ByteArray {
        val input = intent.serialized()
        if (keyId == null) return MessageDigest.getInstance("SHA-256").digest(input)
        val key = material[keyId] ?: error("Retained operation comparison key is unavailable")
        return Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(input)
        }
    }

    fun compare(
        intent: OperationIntent,
        actor: String,
        kind: String,
        expected: ByteArray,
        keyId: String?,
    ): Comparison {
        if (actor != intent.actorScope) return Comparison.HIDDEN
        val matches = MessageDigest.isEqual(expected, hash(intent, keyId))
        return if (kind == intent.kind && matches) Comparison.REPLAY else Comparison.CONFLICT
    }
}

fun eligible(
    now: Instant,
    cutoff: Instant,
): Boolean = now.isBefore(cutoff)
