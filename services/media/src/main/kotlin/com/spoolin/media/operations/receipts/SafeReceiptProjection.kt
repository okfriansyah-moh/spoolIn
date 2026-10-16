package com.spoolin.media.operations.receipts

import com.spoolin.support.receipts.SafeResult
import tools.jackson.databind.ObjectMapper
import java.util.UUID

object SafeReceiptProjection {
    fun errorCode(value: String?): String? {
        if (value == null) return null
        val allowed =
            com.spoolin.support.problem.FailureKind.entries
                .map { it.code } + "RESULT_EXPIRED"
        check(value in allowed) { "Stored operation problem is unavailable" }
        return value
    }

    fun read(json: String?): SafeResult? {
        if (json == null) return null
        return runCatching {
            val node = ObjectMapper().readTree(json)
            require(node.isObject && node.propertyNames().all { it in setOf("resultId", "version", "discarded") })
            val id = node.get("resultId")
            require(id != null && id.isString)
            val version = node.get("version")
            val discarded = node.get("discarded")
            require(version == null || version.isString)
            require(discarded == null || discarded.isBoolean)
            SafeResult(UUID.fromString(id.asString()), version?.asString()?.toLong(), discarded?.asBoolean())
        }.getOrElse { throw IllegalStateException("Stored operation projection is unavailable") }
    }
}
