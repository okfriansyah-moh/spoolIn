package com.spoolin.core.operations.receipts

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.core.features.crypto.executeBound
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
        OwnerFixture("t010-core-crash-$stage.json").start().use { fixture ->
            prepare(fixture)
            listOf("v1", "v2").forEachIndexed { index, name ->
                val key = fixture.root.resolve(name)
                Files.write(key, ByteArray(32) { (index + 21).toByte() })
                Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"))
            }
            val config = fixture.root.resolve("receipt-process.json")
            Files.writeString(
                config,
                ObjectMapper().writeValueAsString(
                    mapOf(
                        "url" to fixture.url("core"),
                        "user" to "spoolin_core_runtime",
                        "passwordFile" to fixture.passwords.getValue("core-runtime").toString(),
                        "operationId" to UUID.randomUUID().toString(),
                        "v1File" to fixture.root.resolve("v1").toString(),
                        "v2File" to fixture.root.resolve("v2").toString(),
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
        fixture.membership("core", "grant")
        fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("core")
        fixture.pool("core", "migrator").use { pool ->
            pool.connection.use { connection ->
                connection.executeBound("CREATE TABLE receipt_domain(value integer PRIMARY KEY)")
                connection.executeBound("GRANT SELECT,INSERT ON receipt_domain TO spoolin_core_runtime")
            }
        }
        fixture.membership("core", "revoke")
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
