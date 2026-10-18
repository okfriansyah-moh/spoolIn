package com.spoolin.core.operations.receipts

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.core.operations.events.ReceiptAction
import com.spoolin.core.operations.events.ReceiptAudit
import com.spoolin.support.observability.LogicalMetrics
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.ReceiptState
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class OwnerReceiptMetricsBoundaryTest {
    private val hashes = IntentHashes(mapOf("test" to ByteArray(32) { 11 }), "test")
    private val owners =
        mapOf(
            "REGISTER" to "register",
            "SignIn" to "signIn",
            "SignOut" to "signOut",
            "DRAFT_SAVE" to "saveDraft",
            "DRAFT_DISCARD" to "discardDraft",
        )

    @Test
    fun `ordinary and audited owner executions expose success only after actual receipt and effect commit`() =
        fixture { pool ->
            SimpleMeterRegistry().useRegistry { registry ->
                val receipts = OwnerReceipts(pool, hashes, Clock.systemUTC(), LogicalMetrics(registry, "core"))
                for ((kind, operation) in owners) {
                    val intent = intent(kind)
                    val mutation: (java.sql.Connection) -> SafeResult = { connection ->
                        assertEquals(
                            0,
                            registry
                                .find("spoolin.operation.results")
                                .tag("operation", operation)
                                .counters()
                                .size,
                        )
                        connection.executeBound("INSERT INTO owner_metric_effect(id) VALUES(?)", intent.id)
                        SafeResult(intent.id)
                    }
                    val result =
                        if (kind == "REGISTER") {
                            receipts.execute(intent, UUID.randomUUID(), true, mutation = mutation)
                        } else {
                            receipts.executeAudited(intent, UUID.randomUUID(), ReceiptAudit(ReceiptAction.OPERATION_COMPLETED), mutation)
                        }
                    assertEquals(ReceiptState.SUCCEEDED, result.state)
                    assertEquals(1.0, counter(registry, operation, "SUCCESS", "OK"))
                }
                assertEquals(5, rows(pool, "owner_metric_effect"))
                assertEquals(5, rows(pool, "operation_receipt"))
                assertEquals(5, rows(pool, "operational_event"))
            }
        }

    @Test
    fun `rolled back effect and receipt never emit success and preserve original typed failure`() =
        fixture { pool ->
            SimpleMeterRegistry().useRegistry { registry ->
                val receipts = OwnerReceipts(pool, hashes, metrics = LogicalMetrics(registry, "core"))
                val intent = intent("DRAFT_SAVE")
                assertThrows(ReceiptConflict::class.java) {
                    receipts.execute(intent, UUID.randomUUID(), true) { connection ->
                        connection.executeBound("INSERT INTO owner_metric_effect(id) VALUES(?)", intent.id)
                        throw ReceiptConflict()
                    }
                }
                assertEquals(0, rows(pool, "owner_metric_effect"))
                assertEquals(0, rows(pool, "operation_receipt"))
                assertEquals(0, rows(pool, "operational_event"))
                assertEquals(1.0, counter(registry, "saveDraft", "DOMAIN_REJECTED", "CONFLICT"))
                assertEquals(
                    0,
                    registry
                        .find("spoolin.operation.results")
                        .tag("outcome", "SUCCESS")
                        .counters()
                        .size,
                )
                assertThrows(IllegalStateException::class.java) {
                    receipts.execute(intent("SignIn"), UUID.randomUUID(), true) { error("Synthetic rollback") }
                }
                assertEquals(1.0, counter(registry, "signIn", "UNCERTAIN", "UNKNOWN"))
            }
        }

    @Test
    fun `resolve and find are excluded while execution replay preserves one effect and one event`() =
        fixture { pool ->
            SimpleMeterRegistry().useRegistry { registry ->
                val receipts = OwnerReceipts(pool, hashes, metrics = LogicalMetrics(registry, "core"))
                val intent = intent("SignOut")
                receipts.execute(intent, UUID.randomUUID(), true) { connection ->
                    connection.executeBound("INSERT INTO owner_metric_effect(id) VALUES(?)", intent.id)
                    SafeResult(intent.id)
                }
                receipts.execute(intent, UUID.randomUUID(), true) { error("Replayed execution changed the effect") }
                assertEquals(2.0, counter(registry, "signOut", "SUCCESS", "OK"))
                repeat(3) {
                    assertEquals(ReceiptState.SUCCEEDED, receipts.resolve(intent)?.state)
                    assertEquals(ReceiptState.SUCCEEDED, receipts.find(intent.id, intent.actorScope)?.state)
                }
                assertEquals(2.0, counter(registry, "signOut", "SUCCESS", "OK"))
                assertEquals(1, rows(pool, "owner_metric_effect"))
                assertEquals(1, rows(pool, "operational_event"))
            }
        }

    @Test
    fun `committed PENDING and FAILED rows retain actual outcome semantics without another mutation`() =
        fixture { pool ->
            SimpleMeterRegistry().useRegistry { registry ->
                val receipts = OwnerReceipts(pool, hashes, metrics = LogicalMetrics(registry, "core"))
                for ((state, code) in listOf("PENDING" to null, "FAILED" to "FIELD_INVALID")) {
                    val intent = intent("DRAFT_SAVE")
                    pending(pool, intent, state, code)
                    assertEquals(
                        ReceiptState.valueOf(state),
                        receipts
                            .execute(intent, UUID.randomUUID(), true) {
                                error("Existing receipt repeated a mutation")
                            }.state,
                    )
                }
                assertEquals(1.0, counter(registry, "saveDraft", "UNCERTAIN", "PENDING"))
                assertEquals(1.0, counter(registry, "saveDraft", "DOMAIN_REJECTED", "FIELD_INVALID"))
                assertEquals(0, rows(pool, "owner_metric_effect"))
            }
        }

    @Test
    fun `actual meter registration failure cannot replace the committed product result`() =
        fixture { pool ->
            SimpleMeterRegistry().useRegistry { registry ->
                Gauge
                    .builder("spoolin.operation.duration") { 1.0 }
                    .tags("service", "core", "operation", "register", "boundary", "owner")
                    .register(registry)
                val receipts = OwnerReceipts(pool, hashes, metrics = LogicalMetrics(registry, "core"))
                val intent = intent("REGISTER")
                val result =
                    receipts.execute(intent, UUID.randomUUID(), true) { connection ->
                        connection.executeBound("INSERT INTO owner_metric_effect(id) VALUES(?)", intent.id)
                        SafeResult(intent.id)
                    }
                assertEquals(ReceiptState.SUCCEEDED, result.state)
                assertEquals(1, rows(pool, "owner_metric_effect"))
                assertEquals(1, rows(pool, "operation_receipt"))
                assertEquals(ReceiptState.SUCCEEDED, receipts.find(intent.id, intent.actorScope)?.state)
            }
        }

    private fun intent(kind: String) = OperationIntent(UUID.randomUUID(), "account:synthetic-owner", kind, "v1", emptyList())

    private fun counter(
        registry: SimpleMeterRegistry,
        operation: String,
        outcome: String,
        code: String,
    ): Double =
        registry
            .get("spoolin.operation.results")
            .tags(
                "service",
                "core",
                "operation",
                operation,
                "boundary",
                "owner",
                "outcome",
                outcome,
                "code",
                code,
            ).counter()
            .count()

    private fun rows(
        pool: DataSource,
        table: String,
    ): Int =
        pool.connection.use {
            it.queryBound("SELECT count(*) FROM $table") { row -> row.getInt(1) }.single()
        }

    private fun pending(
        pool: DataSource,
        intent: OperationIntent,
        state: String,
        code: String?,
    ) {
        pool.connection.use {
            it.executeBound(
                """
                INSERT INTO operation_receipt(id,actor_scope,kind,request_hash,hash_key_id,state,error_code,
                  correlation_id,created_at,expires_at)
                VALUES (?,?,?,?,?,?,?, ?,CURRENT_TIMESTAMP,?)
                """.trimIndent(),
                intent.id,
                intent.actorScope,
                intent.kind,
                hashes.hash(intent, hashes.active),
                hashes.active,
                state,
                code,
                UUID.randomUUID(),
                Timestamp.from(Instant.now().plusSeconds(3600)),
            )
        }
    }

    private fun fixture(action: (DataSource) -> Unit) {
        OwnerFixture("owner-receipt-metrics.json").start().use { fixture ->
            fixture.membership("core", "grant")
            fixture.pool("core", "migrator").use {
                MigrationRunner(it).migrate()
                it.connection.use { connection ->
                    connection.executeBound("CREATE TABLE owner_metric_effect(id uuid PRIMARY KEY)")
                }
            }
            fixture.grants("core")
            // Production grants reset runtime privileges; only this synthetic effect table needs its own grant.
            fixture.sql("core", "GRANT SELECT,INSERT ON appdata.owner_metric_effect TO spoolin_core_runtime")
            fixture.membership("core", "revoke")
            fixture.pool("core").use(action)
        }
    }
}

private inline fun <R> SimpleMeterRegistry.useRegistry(action: (SimpleMeterRegistry) -> R): R =
    try {
        action(this)
    } finally {
        close()
    }
