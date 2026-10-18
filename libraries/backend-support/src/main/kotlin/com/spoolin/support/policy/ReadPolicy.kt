package com.spoolin.support.policy

import java.security.MessageDigest

/** Effective policy for reads; empty normalized search clears criteria even with a nonzero text minimum. */
class ReadPolicy(val effective: PublicPolicy) {
    fun pageSize(requested: Int?): Int {
        val first = requested ?: effective.requestLimits.getValue("defaultPage")
        val maximum = effective.requestLimits.getValue("maxPage")
        if (first !in 1..maximum) throw InputViolation("first", "Use 1 to $maximum records per page.")
        return first
    }

    // The approved int32 RPC fields have no presence: zero is the existing omitted-value representation.
    fun rpcPageSize(first: Int): Int = pageSize(first.takeUnless { it == 0 })

    fun search(raw: String?): String? {
        if (raw == null) return null
        TextRules.scalars(raw, "text")
        return if (TextRules.blank(raw)) null else {
            TextRules.ordinary(raw, effective.textBounds.getValue("search"), "text")
        }
    }

    fun cursorScope(surface: String, first: Int, criteria: Map<String, Any?>): String {
        val bytes = PolicyJson.encode(listOf("read-scope-v2", effective.revision, surface, first, criteria))
        return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
