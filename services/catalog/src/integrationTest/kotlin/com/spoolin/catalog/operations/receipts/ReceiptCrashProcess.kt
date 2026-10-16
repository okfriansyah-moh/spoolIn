package com.spoolin.catalog.operations.receipts

import com.spoolin.catalog.DatabaseSettings
import com.spoolin.support.receipts.ComparisonKeys
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

object ReceiptCrashProcess {
    private fun haltBeforeCommit(source: javax.sql.DataSource): javax.sql.DataSource =
        java.lang.reflect.Proxy.newProxyInstance(
            javax.sql.DataSource::class.java.classLoader,
            arrayOf(javax.sql.DataSource::class.java),
        ) { _, method, arguments ->
            if (method.name == "getConnection") {
                val connection = source.connection
                java.lang.reflect.Proxy.newProxyInstance(
                    java.sql.Connection::class.java.classLoader,
                    arrayOf(java.sql.Connection::class.java),
                ) { _, call, values ->
                    if (call.name == "commit") Runtime.getRuntime().halt(73)
                    call.invoke(connection, *(values ?: emptyArray()))
                }
            } else {
                method.invoke(source, *(arguments ?: emptyArray()))
            }
        } as javax.sql.DataSource

    @JvmStatic
    fun main(arguments: Array<String>) {
        val config = ObjectMapper().readTree(Files.readAllBytes(Path.of(arguments[0])))
        DatabaseSettings(
            config.get("url").asString(),
            config.get("user").asString(),
            Path.of(config.get("passwordFile").asString()),
            false,
        ).pool().use { pool ->
            val active = if (arguments[2].startsWith("inspect")) "v2" else "v1"
            val paths = mapOf("v1" to config.get("v1File").asString(), "v2" to config.get("v2File").asString())
            val source = if (arguments[2] == "before") haltBeforeCommit(pool) else pool
            val receipts = OwnerReceipts(source, ComparisonKeys.load(paths, active, paths.getValue(active)))
            val intent =
                OperationIntent(
                    UUID.fromString(config.get("operationId").asString()),
                    "account:A",
                    "WriteMovie",
                    "p1",
                    emptyList(),
                )
            if (arguments[2].startsWith("inspect")) {
                inspect(pool, receipts, intent, arguments[2], Path.of(arguments[1]))
                return
            }
            val work = WorkRequest("followup", "SYNC", intent.id, Instant.now().plusSeconds(3600))
            receipts.execute(intent, UUID.randomUUID(), true, listOf(work)) { connection ->
                connection.executeBound("INSERT INTO receipt_domain VALUES (1)")
                SafeResult(intent.id, version = 0)
            }
            Runtime.getRuntime().halt(74)
        }
    }

    private fun inspect(
        pool: javax.sql.DataSource,
        receipts: OwnerReceipts,
        intent: OperationIntent,
        mode: String,
        output: Path,
    ) {
        val tables = listOf("receipt_domain", "operation_receipt", "operational_event", "durable_work")
        val counts = tables.map { table -> count(pool, table) }
        if (mode == "inspect-after") {
            receipts.execute(intent, UUID.randomUUID(), true) { error("Committed intent executed again") }
        }
        Files.writeString(output, counts.joinToString(","))
    }

    private fun count(
        pool: javax.sql.DataSource,
        table: String,
    ): Int =
        pool.connection.use { connection ->
            connection.queryBound("SELECT count(*) FROM $table", emptyList()) { it.getInt(1) }.single()
        }
}
