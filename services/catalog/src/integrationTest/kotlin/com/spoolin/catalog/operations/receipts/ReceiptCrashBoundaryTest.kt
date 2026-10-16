package com.spoolin.catalog.operations.receipts

import com.spoolin.catalog.MigrationRunner
import com.spoolin.catalog.boundary.bootstrap.OwnerFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import java.util.concurrent.TimeUnit

class ReceiptCrashBoundaryTest {
    @Test
    fun `actual process death before commit leaves no effects in fresh process reader`() = exercise("before", "0,0,0,0", 73)

    @Test
    fun `actual process response loss after commit preserves original result and all effects`() = exercise("after", "1,1,1,1", 74)

    private fun exercise(
        stage: String,
        expected: String,
        exit: Int,
    ) {
        OwnerFixture("t010-catalog-crash-$stage.json").start().use { fixture ->
            prepare(fixture)
            val config = fixture.root.resolve("receipt-process.json")
            Files.writeString(
                config,
                ObjectMapper().writeValueAsString(
                    mapOf(
                        "url" to fixture.url("catalog"),
                        "user" to "spoolin_catalog_runtime",
                        "passwordFile" to fixture.passwords.getValue("catalog-runtime").toString(),
                        "operationId" to UUID.randomUUID().toString(),
                    ),
                ),
            )
            Files.setPosixFilePermissions(config, PosixFilePermissions.fromString("rw-------"))
            val output = fixture.root.resolve("fresh-reader.txt")
            assertEquals(exit, child(config, output, stage))
            assertEquals(0, child(config, output, "inspect-$stage"))
            assertEquals(expected, Files.readString(output))
            fixture.record("process-$stage", mapOf("childExit" to exit, "freshProcessCounts" to expected))
        }
    }

    private fun prepare(fixture: OwnerFixture) {
        fixture.membership("catalog", "grant")
        fixture.pool("catalog", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("catalog")
        fixture.pool("catalog", "migrator").use { pool ->
            pool.connection.use { connection ->
                connection.createStatement().use {
                    it.execute("CREATE TABLE receipt_domain(value integer PRIMARY KEY)")
                    it.execute("GRANT SELECT,INSERT ON receipt_domain TO spoolin_catalog_runtime")
                }
            }
        }
        fixture.membership("catalog", "revoke")
    }

    private fun child(
        config: Path,
        output: Path,
        mode: String,
    ): Int {
        val executable = Path.of(System.getProperty("java.home"), "bin/java").toString()
        val process =
            ProcessBuilder(
                executable,
                "-cp",
                System.getProperty("receipt.test.classpath"),
                ReceiptCrashProcess::class.java.name,
                config.toString(),
                output.toString(),
                mode,
            ).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS))
            return process.exitValue()
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }
}
