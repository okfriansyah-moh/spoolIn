package com.spoolin.support.receipts

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val MAX_ACTOR_SCOPE = 96
private const val MAX_KIND = 64
private const val COMPARISON_KEY_BYTES = 32

enum class Comparison { REPLAY, CONFLICT, HIDDEN }

data class OperationIntent(
    val id: UUID,
    val actorScope: String,
    val kind: String,
    val policyRevision: String,
    val fields: List<Pair<String, String?>>,
) {
    init {
        require(actorScope.length in 1..MAX_ACTOR_SCOPE && kind.length in 1..MAX_KIND)
        require(fields.map { it.first }.distinct().size == fields.size)
    }

    fun serialized(): ByteArray {
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { output ->
            listOf(actorScope, kind, policyRevision).forEach { output.text(it) }
            output.writeInt(fields.size)
            fields.forEach { (name, value) ->
                output.field(name, value)
            }
        }
        return buffer.toByteArray()
    }

    override fun toString(): String = "OperationIntent(id=$id, content=redacted)"
}

private fun DataOutputStream.field(
    name: String,
    value: String?,
) {
    text(name)
    writeBoolean(value != null)
    if (value != null) text(value)
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
        require(material.all { it.key.matches(Regex("[a-zA-Z0-9_-]{1,64}")) && it.value.size == COMPARISON_KEY_BYTES })
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
