package com.spoolin.support.receipts

import java.time.Instant
import java.util.UUID

enum class OperationOwner { CORE, CATALOG, MEDIA }

enum class ReceiptState { PENDING, SUCCEEDED, FAILED, EXPIRED }

data class SafeResult(
    val resultId: UUID,
    val version: Long? = null,
    val discarded: Boolean? = null,
) {
    init {
        require(version == null || version >= 0)
        require(version == null || discarded == null)
    }

    fun json(): String {
        val fields = mutableListOf("\"resultId\":\"$resultId\"")
        version?.let { fields.add("\"version\":\"$it\"") }
        discarded?.let { fields.add("\"discarded\":$it") }
        return fields.joinToString(",", "{", "}")
    }
}

data class ReceiptOutcome(
    val id: UUID,
    val state: ReceiptState,
    val result: String?,
    val errorCode: String?,
    val correlationId: UUID,
    val createdAt: Instant,
    val resultExpired: Boolean,
)

class ReceiptConflict : IllegalStateException("IDEMPOTENCY_CONFLICT")

class ReceiptHidden : IllegalStateException("Operation is unavailable")
