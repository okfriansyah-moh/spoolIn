package com.spoolin.catalog.unit

import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

internal val unitActor: UUID = UUID.fromString("00000000-0000-4000-8000-000000000001")
internal val unitCredential: UUID = UUID.fromString("00000000-0000-4000-8000-000000000002")
internal val unitCorrelation: UUID = UUID.fromString("00000000-0000-4000-8000-000000000003")
internal val unitOperation: UUID = UUID.fromString("00000000-0000-4000-8000-000000000013")
internal val unitTarget: UUID = UUID.fromString("00000000-0000-4000-8000-000000000011")
internal val unitAsset: UUID = UUID.fromString("00000000-0000-4000-8000-000000000012")
internal val unitNow: Instant = Instant.parse("2026-01-01T12:00:00Z")
internal val unitClock: Clock = Clock.fixed(unitNow, ZoneOffset.UTC)
internal val unitHashes = IntentHashes(mapOf("test" to ByteArray(32) { 1 }), "test")

internal fun receiptRow(
    intent: OperationIntent,
    state: String = "PENDING",
    result: String? = null,
    error: String? = null,
): Map<String, Any?> =
    mapOf(
        "id" to intent.id,
        "receipt_type" to "REQUEST",
        "state" to state,
        "expires_at" to Timestamp.from(unitNow.plusSeconds(86400)),
        "created_at" to Timestamp.from(unitNow),
        "correlation_id" to unitCorrelation,
        "actor_scope" to intent.actorScope,
        "kind" to intent.kind,
        "request_hash" to unitHashes.hash(intent, "test"),
        "hash_key_id" to "test",
        "result" to result,
        "error_code" to error,
    )

internal fun ownerReceipts(db: JdbcRows) = OwnerReceipts(db.source, unitHashes, unitClock)
