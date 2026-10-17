package com.spoolin.core.features.connector

import java.time.Instant
import java.util.UUID

data class ConnectorRecord(
    val id: UUID,
    val accountId: UUID,
    val clientKind: String,
    val authEpoch: Long,
    val scope: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
)
