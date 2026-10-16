package com.spoolin.core.operations.events

import java.util.UUID

enum class ReceiptAction(
    val subject: String,
) {
    OPERATION_COMPLETED("OPERATION"),
    ACCOUNT_REGISTERED("ACCOUNT"),
    SESSION_SIGNED_IN("SESSION"),
    SESSION_SIGNED_OUT("SESSION"),
}

data class ReceiptAudit(
    val action: ReceiptAction = ReceiptAction.OPERATION_COMPLETED,
    val actorId: UUID? = null,
    val subjectId: UUID? = null,
)
