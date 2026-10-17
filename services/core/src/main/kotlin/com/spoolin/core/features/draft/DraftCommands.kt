package com.spoolin.core.features.draft

import com.spoolin.core.features.crypto.ProtectedEnvelope
import java.sql.Timestamp
import java.util.UUID

/** Receipt-scoped draft save request. */
data class DraftSaveCommand(
    val operationId: UUID,
    val accountId: UUID,
    val draftId: UUID,
    val expectedVersion: Long?,
    val input: DraftInput,
    val correlationId: UUID,
)

/** Timestamps applied on draft insert/update. */
internal data class DraftWriteTimestamps(
    val now: Timestamp,
    val expiresAt: Timestamp,
)

/** In-transaction draft persistence payload. */
data class DraftPersistCommand(
    val accountId: UUID,
    val draftId: UUID,
    val expectedVersion: Long?,
    val input: DraftInput,
    val envelope: ProtectedEnvelope,
)
