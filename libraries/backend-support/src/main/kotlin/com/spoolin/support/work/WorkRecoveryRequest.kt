package com.spoolin.support.work

import java.util.UUID

/** Request DTO for an authorized operator-initiated work recovery. */
data class WorkRecoveryRequest(
    val workId: UUID,
    val operatorId: String,
    val reason: String,
    val correlationId: UUID = UUID.randomUUID(),
) {
    init {
        require(workId != null) { "workId is required" }
        require(operatorId.isNotBlank() && operatorId.length <= 96) {
            "operatorId must be non-blank and at most 96 characters"
        }
        require(reason.isNotBlank()) { "Recovery reason is required" }
    }
}
