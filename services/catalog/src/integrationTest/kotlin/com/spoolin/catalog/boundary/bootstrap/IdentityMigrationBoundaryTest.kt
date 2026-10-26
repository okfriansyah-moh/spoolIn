package com.spoolin.catalog.boundary.bootstrap

import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.NamedIdentityResolver
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptState
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.SQLException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/** Real predecessor schemas, offline export, interrupted cutover, and retained owner receipts. */
class IdentityMigrationBoundaryTest {
    private val owners = listOf("core", "catalog", "media")
    private val parent = UUID.fromString("95c65556-47e7-4150-a739-9ad8d065495e")
    private val completed = UUID.fromString("00000000-0000-4000-8000-000000000011")
    private val hashes = IntentHashes(mapOf("retained" to ByteArray(32) { 11 }), "retained")
    private val intent = OperationIntent(parent, "account:owner", "ARTWORK_ATTACH", "v1", listOf("title" to "Movie"))

    @Test
    fun `coordinated upgrade retains pending work tombstones and exact cross owner IDs`() {
        OwnerFixture("sha256-identity-upgrade.json").start().use { fixture ->
            preparePredecessors(fixture)
            val before = owners.associateWith { snapshot(fixture, it) }
            applyExpansion(fixture)
            val index = fixture.root.resolve("legacy.bin")
            assertGuardBlocked(fixture, "catalog", null)
            cutover(fixture, index)
            owners.forEach { owner ->
                identities(fixture, index).use { resolver ->
                    fixture.connection(owner).use(resolver::verify)
                    verifyBindings(resolver)
                }
                assertEquals(before.getValue(owner), snapshot(fixture, owner))
                assertMarkerReadOnly(fixture, owner)
            }
            verifyReceiptReplay(fixture)
            fixture.record("legacy-upgrade", mapOf("owners" to 3, "pendingAndTombstonesRetained" to true))
        }
    }

    private fun preparePredecessors(fixture: OwnerFixture) {
        owners.forEach { owner ->
            fixture.membership(owner, "grant")
            migrate(fixture, owner, mapOf("core" to "7", "catalog" to "25", "media" to "3").getValue(owner))
            fixture.grants(owner)
        }
        insertReceipt(fixture, "catalog", parent, "PENDING", hashes.hash(intent, "retained"), false)
        insertReceipt(fixture, "catalog", completed, "SUCCEEDED", hashes.hash(intent.copy(id = completed), "retained"), true)
        val mediaParent = UUID.fromString("d30cccbe-480a-5054-96a8-ca4cc164f47c")
        insertReceipt(fixture, "media", mediaParent, "PENDING", ByteArray(32), false)
        insertWork(fixture, "catalog", parent, "attach", "ARTWORK_ATTACH")
        insertWork(fixture, "media", mediaParent, "upload-process", "MEDIA_PROCESS")
    }

    private fun migrate(fixture: OwnerFixture, owner: String, target: String) {
        fixture.pool(owner, "migrator").use { pool ->
            Flyway.configure().dataSource(pool).schemas("appdata").defaultSchema("appdata")
                .locations("filesystem:${fixture.repository}/services/$owner/src/main/resources/db/migration")
                .createSchemas(false).cleanDisabled(true).target(target).load().migrate()
        }
    }

    private fun applyExpansion(fixture: OwnerFixture) {
        owners.forEach { owner ->
            migrate(fixture, owner, mapOf("core" to "8", "catalog" to "26", "media" to "4").getValue(owner))
            fixture.grants(owner)
        }
        identities(fixture, null).use { resolver -> fixture.connection("core").use(resolver::verify) }
    }

    private fun insertReceipt(
        fixture: OwnerFixture,
        owner: String,
        id: UUID,
        state: String,
        hash: ByteArray,
        expired: Boolean,
    ) {
        fixture.connection(owner).use { connection ->
            val expiry = if (expired) "CURRENT_TIMESTAMP - INTERVAL '1 day'" else "CURRENT_TIMESTAMP + INTERVAL '7 days'"
            connection.prepareStatement(
                "INSERT INTO operation_receipt(id,actor_scope,kind,request_hash,hash_key_id,state,correlation_id,expires_at) " +
                    "VALUES (?,'account:owner','ARTWORK_ATTACH',?,'retained',?,?,$expiry)",
            ).use { query ->
                query.setObject(1, id)
                query.setBytes(2, hash)
                query.setString(3, state)
                query.setObject(4, id)
                assertEquals(1, query.executeUpdate())
            }
        }
    }

    private fun insertWork(fixture: OwnerFixture, owner: String, operation: UUID, step: String, kind: String) {
        val identifier = legacyVector(operation, step)
        fixture.connection(owner).use { connection ->
            connection.prepareStatement(
                "INSERT INTO durable_work(id,operation_id,kind,payload,max_attempts,deadline_at,correlation_id," +
                    "base_delay_ms,cap_delay_ms,backoff_multiplier,generation,attempt_count) " +
                    "VALUES (?,?,?,?::jsonb,5,CURRENT_TIMESTAMP + INTERVAL '1 day',?,500,5000,2,3,2)",
            ).use { query ->
                query.setObject(1, identifier)
                query.setObject(2, operation)
                query.setString(3, kind)
                query.setString(4, "{\"subjectId\":\"$parent\"}")
                query.setObject(5, operation)
                assertEquals(1, query.executeUpdate())
            }
        }
    }

    private fun legacyVector(namespace: UUID, name: String): UUID =
        when (namespace.toString() to name) {
            parent.toString() to "attach" -> UUID.fromString("5d231fd7-27ab-59ec-b937-c887840b7a7d")
            "d30cccbe-480a-5054-96a8-ca4cc164f47c" to "upload-process" -> UUID.fromString("01143073-411e-50cd-8523-5f5244c39bcd")
            else -> error("Missing independently computed legacy fixture vector")
        }

    private fun snapshot(fixture: OwnerFixture, owner: String): List<String> =
        fixture.connection(owner).use { connection ->
            listOf("operation_receipt", "durable_work").map { table ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY id),'[]'::jsonb)::text FROM $table t").use { rows ->
                        check(rows.next())
                        rows.getString(1)
                    }
                }
            }
        }

    private fun cutover(fixture: OwnerFixture, index: Path) {
        val wrapper = fixture.root.resolve("interrupt-psql.py")
        Files.writeString(wrapper, """
            #!/usr/bin/env python3
            import subprocess, sys
            sql = sys.stdin.buffer.read()
            if any('dbname=spoolin_catalog' in value for value in sys.argv) and b'UPDATE appdata.identity_hash_migration' in sql:
                sys.exit(91)
            sys.exit(subprocess.run(['psql', *sys.argv[1:]], input=sql).returncode)
        """.trimIndent() + "\n")
        Files.setPosixFilePermissions(wrapper, PosixFilePermissions.fromString("rwx------"))
        assertTrue(runExport(fixture, index, wrapper) != 0)
        assertTrue(Files.isRegularFile(index))
        assertFalse(Files.exists(index.resolveSibling("legacy.bin.ready")))
        assertGuardBlocked(fixture, "core", index)
        assertGuardBlocked(fixture, "catalog", index)
        assertEquals(0, runExport(fixture, index, null))
        assertTrue(Files.isRegularFile(index.resolveSibling("legacy.bin.ready")))
    }

    private fun runExport(fixture: OwnerFixture, index: Path, wrapper: Path?): Int {
        val command = mutableListOf("python3", "ops/identity/migrate.py", "--output", index.toString(), "--apply")
        owners.forEach { owner ->
            val connection = "host=${fixture.postgres.host} port=${fixture.postgres.getMappedPort(5432)} " +
                "dbname=spoolin_$owner user=postgres sslmode=verify-full sslrootcert=${fixture.root.resolve("ca.crt")}"
            command += listOf("--connection", "$owner=$connection")
        }
        if (wrapper != null) command += listOf("--psql", wrapper.toString())
        val output = fixture.root.resolve(if (wrapper == null) "apply.log" else "interrupted.log")
        val process = ProcessBuilder(command).directory(fixture.repository.toFile()).redirectErrorStream(true)
            .redirectOutput(output.toFile())
        process.environment()["PGPASSWORD"] = Files.readString(fixture.passwords.getValue("postgres")).trimEnd('\n', '\r')
        val running = process.start()
        check(running.waitFor(180, java.util.concurrent.TimeUnit.SECONDS)) { "Offline migration fixture timed out" }
        if (wrapper == null && running.exitValue() != 0) error("Migration fixture failed: ${Files.readString(output).takeLast(1500)}")
        return running.exitValue()
    }

    private fun identities(fixture: OwnerFixture, path: Path?): NamedIdentityResolver =
        NamedIdentityResolver(path) {
            Files.newInputStream(fixture.repository.resolve("libraries/backend-support/src/main/resources/identity/frozen-v5.bin"))
        }

    private fun assertGuardBlocked(fixture: OwnerFixture, owner: String, path: Path?) {
        identities(fixture, path).use { resolver ->
            fixture.connection(owner).use { connection ->
                assertThrows(IllegalStateException::class.java) { resolver.verify(connection) }
            }
        }
    }

    private fun verifyBindings(resolver: NamedIdentityResolver) {
        assertEquals(legacyVector(parent, "attach"), resolver.id(parent, "attach".toByteArray()))
        assertEquals(UUID.fromString("d30cccbe-480a-5054-96a8-ca4cc164f47c"), resolver.id(parent, "media-upload".toByteArray()))
        val upload = resolver.id(parent, "media-upload".toByteArray())
        assertEquals(legacyVector(upload, "upload-process"), resolver.id(upload, "upload-process".toByteArray()))
        assertEquals(8, resolver.id(UUID.fromString("00000000-0000-4000-8000-000000000099"), "attach".toByteArray()).version())
    }

    private fun assertMarkerReadOnly(fixture: OwnerFixture, owner: String) {
        fixture.connection(owner).use { connection ->
            val failure = assertThrows(SQLException::class.java) {
                connection.createStatement().use { it.executeUpdate("UPDATE appdata.identity_hash_migration SET registry_sha256=NULL") }
            }
            assertEquals("42501", failure.sqlState)
        }
    }

    private fun verifyReceiptReplay(fixture: OwnerFixture) {
        fixture.pool("catalog").use { pool ->
            val receipts = OwnerReceipts(pool, hashes, Clock.fixed(Instant.now(), ZoneOffset.UTC))
            val pending = receipts.defer(intent, parent, true) { error("Existing pending work must not be enqueued again") }
            assertEquals(ReceiptState.PENDING, pending.state)
            val retained = receipts.execute(intent.copy(id = completed), completed, true) { error("Expired success must not repeat its effect") }
            assertEquals(ReceiptState.SUCCEEDED, retained.state)
            assertTrue(retained.resultExpired)
        }
    }
}
