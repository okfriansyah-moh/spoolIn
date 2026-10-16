package com.spoolin.catalog.operations.receipts

import com.spoolin.catalog.MigrationRunner
import com.spoolin.catalog.boundary.bootstrap.OwnerFixture
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import java.sql.SQLException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReceiptSchemaBoundaryTest {
    private lateinit var fixture: OwnerFixture

    @BeforeAll
    fun prepare() {
        fixture = OwnerFixture("t010-catalog-schema.json").start()
        fixture.membership("catalog", "grant")
        fixture.pool("catalog", "migrator").use { MigrationRunner(it).migrate() }
    }

    @AfterAll
    fun cleanup() = fixture.close()

    @TestFactory
    fun `receipt source rows exercise omission explicit null and declared physical bounds`() =
        receiptCases().map { item ->
            DynamicTest.dynamicTest(item.name) { probe(item) }
        }

    @TestFactory
    fun `event source rows exercise omission explicit null and activity shape`() =
        eventCases().map { item ->
            DynamicTest.dynamicTest(item.name) { probe(item) }
        }

    private fun receiptCases(): List<SqlCase> {
        val cases = mutableListOf<SqlCase>()
        val required = setOf("id", "actor_scope", "kind", "request_hash", "state", "correlation_id")
        receipt().keys.forEachIndexed { index, column ->
            val row = "ATDD-223-T1-R" + (index + 1).toString().padStart(2, '0')
            val omission =
                if (column in required) {
                    "23502"
                } else if (column == "expires_at") {
                    "23514"
                } else {
                    null
                }
            val nullState = if (column in required || column in setOf("receipt_type", "created_at")) "23502" else omission
            cases += SqlCase("$row omitted", "operation_receipt", receipt().minus(column), omission)
            cases += SqlCase("$row SQL NULL", "operation_receipt", receipt() + (column to "NULL"), nullState)
        }
        listOf("actor_scope" to 96, "kind" to 64, "hash_key_id" to 64, "error_code" to 64).forEach { (column, max) ->
            listOf(max, max + 1).forEach { size ->
                cases +=
                    SqlCase(
                        "$column capacity $size",
                        "operation_receipt",
                        receipt() + (column to "repeat('x',$size)"),
                        if (size >
                            max
                        ) {
                            "22001"
                        } else {
                            null
                        },
                    )
            }
        }
        listOf(31, 32, 33).forEach { size ->
            cases +=
                SqlCase(
                    "request_hash bytes $size",
                    "operation_receipt",
                    receipt() + ("request_hash" to "decode(repeat('ab',$size),'hex')"),
                    if (size ==
                        32
                    ) {
                        null
                    } else {
                        "23514"
                    },
                )
        }
        listOf(16383, 16384, 16385).forEach { size ->
            cases +=
                SqlCase(
                    "result jsonb text bytes $size",
                    "operation_receipt",
                    receipt() + ("result" to "jsonb_build_object('x',repeat('a',${size - 9}))"),
                    if (size > 16384) "23514" else null,
                )
        }
        listOf("[]", "true", "1", "null").forEach { json ->
            cases += SqlCase("result rejects $json", "operation_receipt", receipt() + ("result" to "'$json'::jsonb"), "23514")
        }
        cases += SqlCase("state unknown", "operation_receipt", receipt() + ("state" to "'UNKNOWN'"), "23514")
        cases += SqlCase("malformed UUID conversion", "operation_receipt", receipt() + ("id" to "'malformed'"), "22P02")
        return cases
    }

    private fun eventCases(): List<SqlCase> {
        val cases = mutableListOf<SqlCase>()
        val required = setOf("id", "actor_kind", "action", "subject_kind", "correlation_id", "event_type")
        event().keys.forEachIndexed { index, column ->
            val row = "ATDD-226-T1-R" + (index + 1).toString().padStart(2, '0')
            val omitted =
                if (column in required) {
                    "23502"
                } else if (column == "outcome") {
                    "23514"
                } else {
                    null
                }
            val nullState = if (column in required || column in setOf("changes", "created_at")) "23502" else omitted
            cases += SqlCase("$row omitted", "operational_event", event().minus(column), omitted)
            cases += SqlCase("$row SQL NULL", "operational_event", event() + (column to "NULL"), nullState)
        }
        listOf("action" to 64, "subject_kind" to 24, "error_code" to 64, "operator_id" to 96).forEach { (column, max) ->
            listOf(max, max + 1).forEach { size ->
                cases +=
                    SqlCase(
                        "$column capacity $size",
                        "operational_event",
                        event() + (column to "repeat('x',$size)"),
                        if (size >
                            max
                        ) {
                            "22001"
                        } else {
                            null
                        },
                    )
            }
        }
        cases += SqlCase("activity cannot claim attempt", "operational_event", event() + ("attempt_no" to "1"), "23514")
        cases += SqlCase("activity metadata wrong shape", "operational_event", event() + ("changes" to "'[]'::jsonb"), "23514")
        return cases
    }

    private fun probe(item: SqlCase) {
        fixture.connection("catalog", "migrator").use { connection ->
            connection.autoCommit = false
            try {
                val sql = "INSERT INTO ${item.table} (${item.values.keys.joinToString()}) VALUES (${item.values.values.joinToString()})"
                if (item.sqlState == null) {
                    connection.createStatement().use { assertEquals(1, it.executeUpdate(sql)) }
                } else {
                    val error = assertThrows(SQLException::class.java) { connection.createStatement().use { it.executeUpdate(sql) } }
                    assertEquals(item.sqlState, error.sqlState)
                }
            } finally {
                connection.rollback()
            }
        }
    }

    private fun receipt(): Map<String, String> =
        linkedMapOf(
            "id" to "'5e277cf5-2fc2-4b0a-9df9-c0309c3c8fc4'",
            "actor_scope" to "'account:A'",
            "kind" to "'WriteMovie'",
            "receipt_type" to "'REQUEST'",
            "request_hash" to "decode(repeat('ab',32),'hex')",
            "hash_key_id" to "NULL",
            "state" to "'SUCCEEDED'",
            "result" to "'{}'::jsonb",
            "error_code" to "NULL",
            "correlation_id" to "'b9569801-76f8-49f7-a2f9-358a0e97e2ef'",
            "created_at" to "CURRENT_TIMESTAMP",
            "expires_at" to "CURRENT_TIMESTAMP + interval '7 days'",
        )

    private fun event(): Map<String, String> =
        linkedMapOf(
            "id" to "'4bbc2aa2-3463-4428-9d1c-4ea182f953ed'",
            "operation_id" to "NULL",
            "actor_id" to "NULL",
            "actor_kind" to "'SERVICE'",
            "action" to "'OPERATION_COMPLETED'",
            "subject_kind" to "'OPERATION'",
            "subject_id" to "NULL",
            "outcome" to "'SUCCEEDED'",
            "changes" to "'{}'::jsonb",
            "correlation_id" to "'b9569801-76f8-49f7-a2f9-358a0e97e2ef'",
            "created_at" to "CURRENT_TIMESTAMP",
            "event_type" to "'ACTIVITY'",
            "work_id" to "NULL",
            "generation" to "NULL",
            "attempt_no" to "NULL",
            "lease_epoch" to "NULL",
            "attempt_outcome" to "NULL",
            "error_code" to "NULL",
            "operator_id" to "NULL",
        )
}

private data class SqlCase(
    val name: String,
    val table: String,
    val values: Map<String, String>,
    val sqlState: String?,
)
