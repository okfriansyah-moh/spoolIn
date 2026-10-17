package com.spoolin.support.work

import java.util.UUID

private const val MAX_OPERATOR_ID_LENGTH = 96

/** Request DTO for an authorized operator-initiated work recovery. */
data class WorkRecoveryRequest(
    val workId: UUID,
    val operatorId: String,
    val reason: String,
    val correlationId: UUID = UUID.randomUUID(),
) {
    init {
        require(operatorId.isNotBlank() && operatorId.length <= MAX_OPERATOR_ID_LENGTH) {
            "operatorId must be non-blank and at most 96 characters"
        }
        require(reason.isNotBlank()) { "Recovery reason is required" }
    }
}
