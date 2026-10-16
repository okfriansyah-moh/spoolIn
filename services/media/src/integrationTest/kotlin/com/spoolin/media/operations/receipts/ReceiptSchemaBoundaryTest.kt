package com.spoolin.media.operations.receipts

import com.spoolin.media.MigrationRunner
import com.spoolin.media.boundary.bootstrap.OwnerFixture
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import java.sql.SQLException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReceiptSchemaBoundaryTest {
    private lateinit var fixture: OwnerFixture

    @BeforeAll
    fun prepare() {
        fixture = OwnerFixture("t010-media-schema.json").start()
        fixture.membership("media", "grant")
        fixture.pool("media", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("media")
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

    @Test
    fun `media rejects every internal initialization variant`() {
        listOf("ACCOUNT_INITIALIZATION", "CATALOG_INITIALIZATION").forEach { type ->
            val values =
                receipt() +
                    mapOf(
                        "receipt_type" to "'$type'",
                        "actor_scope" to "'SERVICE_INITIALIZER'",
                        "result" to "NULL",
                        "state" to "'PENDING'",
                        "expires_at" to "NULL",
                    )
            probe(SqlCase("media internal type $type", "operation_receipt", values, "23514"))
        }
    }

    @Test
    fun `safe projection refuses private fields wrong types and unlisted problem codes`() {
        val safeId = "5d9bcc0c-3235-4684-a196-1360f08b05d9"
        val privateValue = "synthetic-private-canary"
        val invalid =
            listOf(
                "{}",
                "[]",
                "{\"resultId\":1}",
                "{\"resultId\":\"$safeId\",\"version\":1}",
                "{\"resultId\":\"$safeId\",\"discarded\":\"true\"}",
                "{\"resultId\":\"$safeId\",\"name\":\"$privateValue\"}",
            )
        invalid.forEach { body ->
            val failure = assertThrows(IllegalStateException::class.java) { SafeReceiptProjection.read(body) }
            org.junit.jupiter.api.Assertions
                .assertFalse(failure.message.orEmpty().contains(privateValue))
        }
        assertEquals(safeId, SafeReceiptProjection.read("{\"resultId\":\"$safeId\",\"discarded\":false}")?.resultId.toString())
        assertEquals("UNAVAILABLE", SafeReceiptProjection.errorCode("UNAVAILABLE"))
        assertThrows(IllegalStateException::class.java) { SafeReceiptProjection.errorCode(privateValue) }
    }

    private fun receiptCases(): List<SqlCase> {
        val cases = receiptOmissions()
        cases +=
            capacityCases(
                "operation_receipt",
                receipt(),
                listOf("actor_scope" to 96, "kind" to 64, "hash_key_id" to 64, "error_code" to 64),
                false,
            )
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
        cases += capacityCases("operation_receipt", receipt(), listOf("receipt_type" to 24, "state" to 16), true)
        return cases
    }

    private fun eventCases(): List<SqlCase> {
        val cases = eventOmissions()
        cases +=
            capacityCases(
                "operational_event",
                event(),
                listOf("action" to 64, "subject_kind" to 24, "error_code" to 64, "operator_id" to 96),
                false,
            )
        cases += SqlCase("activity cannot claim attempt", "operational_event", event() + ("attempt_no" to "1"), "23514")
        cases += SqlCase("activity metadata wrong shape", "operational_event", event() + ("changes" to "'[]'::jsonb"), "23514")
        cases +=
            capacityCases(
                "operational_event",
                event(),
                listOf(
                    "actor_kind" to 16,
                    "outcome" to 16,
                    "event_type" to 24,
                    "attempt_outcome" to 24,
                ),
                true,
            )
        listOf(65535, 65536, 65537).forEach { size ->
            cases +=
                SqlCase(
                    "changes jsonb text bytes $size",
                    "operational_event",
                    event() +
                        ("changes" to "jsonb_build_object('x',repeat('a',${size - 9}))"),
                    if (size > 65536) "23514" else null,
                )
        }
        cases += phaseCases()
        return cases
    }

    private fun receiptOmissions(): MutableList<SqlCase> {
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
        return cases
    }

    private fun eventOmissions(): MutableList<SqlCase> {
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
        return cases
    }

    private fun capacityCases(
        table: String,
        base: Map<String, String>,
        columns: List<Pair<String, Int>>,
        allowlist: Boolean,
    ): List<SqlCase> =
        columns.flatMap { (column, maximum) ->
            capacitySizes(table, base, column, maximum, allowlist)
        }

    private fun capacitySizes(
        table: String,
        base: Map<String, String>,
        column: String,
        maximum: Int,
        allowlist: Boolean,
    ): List<SqlCase> =
        listOf(maximum, maximum + 1).map { size ->
            val label = if (allowlist) "allowlist capacity" else "capacity"
            SqlCase("$column $label $size", table, base + (column to "repeat('x',$size)"), capacityState(size, maximum, allowlist))
        }

    private fun capacityState(
        size: Int,
        maximum: Int,
        allowlist: Boolean,
    ): String? =
        when {
            size > maximum -> "22001"
            allowlist -> "23514"
            else -> null
        }

    private fun phaseCases(): List<SqlCase> {
        val started = startedEvent()
        val cases = mutableListOf(SqlCase("STARTED valid phase", "operational_event", started, null))
        cases += startedPredicates(started)
        cases += finishedCases(started)
        cases += SqlCase("STARTED no error", "operational_event", started + ("error_code" to "'UNAVAILABLE'"), "23514")
        cases += SqlCase("STARTED no finished outcome", "operational_event", started + ("attempt_outcome" to "'SUCCEEDED'"), "23514")
        cases += recoveryCases(started)
        listOf(
            Triple("generation", "2147483647", null),
            Triple("generation", "2147483648", "22003"),
            Triple("attempt_no", "2147483647", "23514"),
            Triple("attempt_no", "2147483648", "22003"),
            Triple("lease_epoch", "9223372036854775807", null),
            Triple("lease_epoch", "9223372036854775808", "22003"),
        ).forEach { (column, value, state) ->
            cases += SqlCase("$column physical integer range $value", "operational_event", started + (column to value), state)
        }
        return cases
    }

    private fun startedEvent(): Map<String, String> =
        event() +
            mapOf(
                "event_type" to "'ATTEMPT_STARTED'",
                "outcome" to "NULL",
                "work_id" to "'69b45fcf-acad-4e59-9342-156d321b1c48'",
                "generation" to "0",
                "attempt_no" to "1",
                "lease_epoch" to "1",
            )

    private fun startedPredicates(started: Map<String, String>): List<SqlCase> {
        val cases = mutableListOf<SqlCase>()
        listOf("work_id", "generation", "attempt_no", "lease_epoch").forEach { column ->
            cases += SqlCase("STARTED requires $column", "operational_event", started + (column to "NULL"), "23514")
        }
        listOf(
            "generation" to listOf(-1, 0, 1),
            "attempt_no" to listOf(0, 1, 10, 11),
            "lease_epoch" to listOf(-1, 0, 1),
        ).forEach { (column, values) ->
            values.forEach { value ->
                val valid = validPhaseValue(column, value)
                cases +=
                    SqlCase(
                        "STARTED $column predicate $value",
                        "operational_event",
                        started + (column to "$value"),
                        if (valid) null else "23514",
                    )
            }
        }
        return cases
    }

    private fun validPhaseValue(
        column: String,
        value: Int,
    ): Boolean =
        when (column) {
            "generation" -> value >= 0
            "attempt_no" -> value in 1..10
            else -> value > 0
        }

    private fun finishedCases(started: Map<String, String>): List<SqlCase> {
        val cases = mutableListOf<SqlCase>()
        listOf("SUCCEEDED", "TRANSIENT", "PERMANENT", "UNCERTAIN", "CANCELLED").forEach { outcome ->
            val finished =
                started +
                    mapOf(
                        "event_type" to "'ATTEMPT_FINISHED'",
                        "attempt_outcome" to "'$outcome'",
                        "outcome" to if (outcome == "SUCCEEDED") "'SUCCEEDED'" else "'FAILED'",
                    )
            cases += SqlCase("FINISHED $outcome", "operational_event", finished, null)
            cases +=
                SqlCase("FINISHED $outcome mismatched activity outcome", "operational_event", finished + ("outcome" to "'DENIED'"), "23514")
        }
        return cases
    }

    private fun recoveryCases(started: Map<String, String>): List<SqlCase> {
        val cases = mutableListOf<SqlCase>()
        val recovery =
            started +
                mapOf(
                    "event_type" to "'RECOVERY'",
                    "attempt_no" to "NULL",
                    "lease_epoch" to "0",
                    "outcome" to "'FAILED'",
                    "operator_id" to "'fixture-operator'",
                )
        cases += SqlCase("RECOVERY valid phase", "operational_event", recovery, null)
        cases += SqlCase("RECOVERY verified operator required", "operational_event", recovery + ("operator_id" to "NULL"), "23514")
        cases += SqlCase("RECOVERY no attempt", "operational_event", recovery + ("attempt_no" to "1"), "23514")
        cases +=
            SqlCase("operation FK", "operational_event", event() + ("operation_id" to "'01d207d9-ece1-42f3-bb55-5609ef2772bd'"), "23503")
        cases += SqlCase("work FK", "operational_event", started + ("work_id" to "'01d207d9-ece1-42f3-bb55-5609ef2772bd'"), "23503")
        return cases
    }

    private fun probe(item: SqlCase) {
        fixture.connection("media", "migrator").use { connection ->
            connection.autoCommit = false
            try {
                seedWork(connection)
                val sql = "INSERT INTO ${item.table} (${item.values.keys.joinToString()}) VALUES (${item.values.values.joinToString()})"
                executeProbe(connection, sql, item.sqlState)
            } finally {
                connection.rollback()
            }
        }
    }

    private fun seedWork(connection: java.sql.Connection) {
        connection.createStatement().use {
            it.executeUpdate(
                "INSERT INTO durable_work (id,kind,payload,max_attempts,deadline_at,correlation_id," +
                    "base_delay_ms,cap_delay_ms,backoff_multiplier) VALUES " +
                    "('69b45fcf-acad-4e59-9342-156d321b1c48','SYNC','{}',3,CURRENT_TIMESTAMP+interval '1 hour'," +
                    "'b9569801-76f8-49f7-a2f9-358a0e97e2ef',500,5000,2)",
            )
        }
    }

    private fun executeProbe(
        connection: java.sql.Connection,
        sql: String,
        expected: String?,
    ) {
        connection.createStatement().use { statement ->
            if (expected == null) {
                assertEquals(1, statement.executeUpdate(sql))
            } else {
                assertEquals(expected, assertThrows(SQLException::class.java) { statement.executeUpdate(sql) }.sqlState)
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
