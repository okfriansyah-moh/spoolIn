package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.crypto.queryBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.time.Instant

class BackupCheckpoint(
    private val fixture: CryptoFixture,
    private val row: String,
) {
    private val directory = fixture.owner.root.resolve("checkpoint-$row")
    private val keyDirectory = fixture.owner.root.resolve("checkpoint-keys-$row")
    private var completedNanos = 0L

    fun prepare() {
        val privateDirectory = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))
        Files.createDirectory(directory, privateDirectory)
        Files.createDirectory(keyDirectory, privateDirectory)
        val startedUtc = Instant.now().toString()
        val before = metadata()
        val accountRows = fixture.scalar("SELECT count(*) FROM appdata.user_account")
        val aliasRows = fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup")
        val path = "/tmp/${fixture.owner.namespace}-$row.dump"
        val argv =
            arrayOf(
                "sh",
                "-c",
                "umask 077; export PGPASSWORD=\"\$(cat /run/spoolin/postgres-password)\"; " +
                    "exec pg_dump --no-password -U postgres -d spoolin_core --format=custom --file=\"\$1\"",
                "checkpoint",
                path,
            )
        val command = fixture.owner.postgres.execInContainer(*argv)
        assertEquals(0, command.exitCode, "Private checkpoint dump failed")
        val version = fixture.owner.postgres.execInContainer("pg_dump", "--version")
        assertEquals(0, version.exitCode)
        assertTrue(version.stdout.trim().startsWith("pg_dump (PostgreSQL) 18.6"))
        val inventory = fixture.owner.postgres.execInContainer("pg_restore", "--list", path)
        assertEquals(0, inventory.exitCode)
        listOf("crypto_key_version", "user_account", "account_email_lookup").forEach {
            assertTrue(inventory.stdout.contains("TABLE DATA appdata $it"))
        }
        val dump = directory.resolve("core.dump")
        fixture.owner.postgres.copyFileFromContainer(path, dump.toString())
        protect(dump)
        val bytes = Files.readAllBytes(dump)
        assertEquals("PGDMP", String(bytes.copyOfRange(0, 5), Charsets.US_ASCII))
        val checksum = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        fixture.mounts.forEach { (id, mount) ->
            val retained = keyDirectory.resolve(id)
            Files.copy(mount.file, retained)
            protect(retained)
            assertEquals(32, Files.size(retained))
            assertTrue(Files.readAllBytes(mount.file).contentEquals(Files.readAllBytes(retained)))
        }
        val after = metadata()
        assertEquals(before, after, "Writers must remain quiescent across this isolated checkpoint")
        assertEquals(accountRows, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
        assertEquals(aliasRows, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup"))
        val manifest =
            mapOf(
                "namespace" to fixture.owner.namespace,
                "database" to "spoolin_core",
                "dumpSha256" to checksum,
                "dumpBytes" to bytes.size,
                "epochs" to before,
                "separateProtectedKeyFiles" to fixture.mounts.size,
                "protectedKeyBytesEach" to 32,
                "protectedKeyCopiesEqual" to true,
                "accountRows" to accountRows,
                "aliasRows" to aliasRows,
                "startedUtc" to startedUtc,
            )
        val manifestFile = directory.resolve("manifest.json")
        Files.writeString(manifestFile, ObjectMapper().writeValueAsString(manifest))
        protect(manifestFile)
        completedNanos = System.nanoTime()
        fixture.observe(
            "atdd181",
            "backup-$row",
            manifest +
                mapOf(
                    "argv" to argv.toList(),
                    "dumpExit" to command.exitCode,
                    "inventoryExit" to inventory.exitCode,
                    "version" to version.stdout.trim(),
                    "mode" to "0600 files, 0700 directories",
                    "quiescentBeforeAfterEqual" to true,
                    "completedUtc" to Instant.now().toString(),
                    "completedMonotonicNanos" to completedNanos,
                    "scope" to "Isolated Core crypto fixture precondition; no installation restore claim",
                ),
        )
    }

    fun actionStarted() {
        val actionNanos = System.nanoTime()
        assertTrue(completedNanos > 0 && completedNanos < actionNanos)
        fixture.observe(
            "atdd181",
            "backup-$row-order",
            mapOf(
                "preparedMonotonicNanos" to completedNanos,
                "actionStartMonotonicNanos" to actionNanos,
                "preparedBeforeAction" to true,
                "actionStartUtc" to Instant.now().toString(),
            ),
        )
    }

    private fun metadata(): List<Map<String, Any>> =
        fixture.owner.connection("core").use { connection ->
            connection.queryBound("SELECT id,purpose,state,next_nonce FROM appdata.crypto_key_version ORDER BY id") {
                mapOf(
                    "id" to it.getString("id"),
                    "purpose" to it.getString("purpose"),
                    "state" to it.getString("state"),
                    "nextNonce" to it.getLong("next_nonce").toString(),
                )
            }
        }

    private fun protect(path: Path) {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(path))
    }

    fun recordCleanup() {
        assertTrue(!Files.exists(directory) && !Files.exists(keyDirectory))
        fixture.observe("atdd181", "backup-$row-cleanup", mapOf("privateDumpRemoved" to true, "privateKeysRemoved" to true))
    }
}
