package com.spoolin.media.operations.receipts

import com.spoolin.media.DatabaseSettings
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.WorkRequest
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

object ReceiptCrashProcess {
    @JvmStatic
    fun main(arguments: Array<String>) {
        val config = ObjectMapper().readTree(Files.readAllBytes(Path.of(arguments[0])))
        DatabaseSettings(
            config.get("url").asString(),
            config.get("user").asString(),
            Path.of(config.get("passwordFile").asString()),
            false,
        ).pool().use { pool ->
            val receipts = OwnerReceipts(pool, IntentHashes(mapOf("v1" to ByteArray(32) { 21 }), "v1"))
            val intent =
                OperationIntent(
                    UUID.fromString(config.get("operationId").asString()),
                    "account:A",
                    "WriteMovie",
                    "p1",
                    emptyList(),
                )
            if (arguments[2].startsWith("inspect")) {
                val tables = listOf("receipt_domain", "operation_receipt", "operational_event", "durable_work")
                val counts =
                    tables.map { table ->
                        pool.connection.use { connection ->
                            connection.createStatement().use { statement ->
                                statement.executeQuery("SELECT count(*) FROM $table").use { rows ->
                                    rows.next()
                                    rows.getInt(1)
                                }
                            }
                        }
                    }
                if (arguments[2] == "inspect-after") {
                    receipts.execute(intent, UUID.randomUUID(), true) { error("Committed intent executed again") }
                }
                Files.writeString(Path.of(arguments[1]), counts.joinToString(","))
                return
            }
            val work = WorkRequest("followup", "SYNC", intent.id, Instant.now().plusSeconds(3600))
            receipts.execute(intent, UUID.randomUUID(), true, listOf(work)) { connection ->
                connection.createStatement().use { it.executeUpdate("INSERT INTO receipt_domain VALUES (1)") }
                if (arguments[2] == "before") Runtime.getRuntime().halt(73)
                SafeResult(intent.id, version = 0)
            }
            Runtime.getRuntime().halt(74)
        }
    }
}
