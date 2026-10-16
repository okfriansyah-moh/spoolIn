package com.spoolin.core.recovery.crypto

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.ProtectionAlert
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.UUID

class CryptoFixture(
    evidenceName: String = "t005-owner-observations.json",
) : AutoCloseable {
    val owner = OwnerFixture(evidenceName).start()
    val pool by lazy { owner.pool("core") }
    val alerts = mutableListOf<UUID>()
    val alert = ProtectionAlert { alerts.add(it) }
    val mounts = mutableMapOf<String, KeyMount>()
    val keys: MountedKeys
    val registry: KeyRegistry
    val allocator: NonceAllocator
    val crypto: AccountCryptography
    val accounts: ProtectedAccounts
    private val checkpoints = mutableListOf<BackupCheckpoint>()

    init {
        owner.membership("core", "grant")
        owner.pool("core", "migrator").use { MigrationRunner(it).migrate() }
        owner.grants("core")
        owner.membership("core", "revoke")
        listOf("pii-old", "pii-write", "draft-old", "draft-write", "email-old", "email-write").forEach { id ->
            val purpose =
                when {
                    id.startsWith("pii") -> KeyPurpose.PII
                    id.startsWith("draft") -> KeyPurpose.DRAFT
                    else -> KeyPurpose.EMAIL_LOOKUP
                }
            val file = owner.root.resolve(id)
            Files.write(file, ByteArray(32).also { SecureRandom().nextBytes(it) })
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
            mounts[id] = KeyMount(purpose, file)
            register(id, purpose, if (id.endsWith("old")) "READ" else "WRITE")
        }
        keys = MountedKeys(mounts)
        registry = KeyRegistry(pool)
        allocator = NonceAllocator(pool)
        crypto = AccountCryptography(registry, keys, allocator, AeadCipher(), alert)
        accounts = ProtectedAccounts(pool, registry, crypto, AccountPayload(), alert)
    }

    fun backupCheckpoint(row: String): BackupCheckpoint {
        val checkpoint = BackupCheckpoint(this, row)
        checkpoint.prepare()
        checkpoints.add(checkpoint)
        return checkpoint
    }

    fun register(
        id: String,
        purpose: KeyPurpose,
        state: String,
    ) {
        pool.connection.use { connection ->
            connection.prepareStatement("INSERT INTO appdata.crypto_key_version(id,purpose,state) VALUES (?,?,?)").use {
                it.setString(1, id)
                it.setString(2, purpose.name)
                it.setString(3, state)
                it.executeUpdate()
            }
        }
    }

    fun scalar(sql: String): Long =
        owner.connection("core").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { rows ->
                    check(rows.next())
                    rows.getLong(1)
                }
            }
        }

    fun observe(
        case: String,
        row: String,
        values: Map<String, Any>,
    ) {
        val path = owner.repository.resolve(".ai/evidence/numbering-v1/t005-$case-$row.json")
        Files.createDirectories(path.parent)
        Files.writeString(
            path,
            ObjectMapper().writeValueAsString(
                mapOf(
                    "case" to case,
                    "row" to row,
                    "namespace" to owner.namespace,
                    "postgres" to "18.6",
                    "freshReader" to true,
                    "observed" to values,
                ),
            ),
        )
    }

    override fun close() {
        keys.close()
        pool.close()
        owner.close()
        checkpoints.forEach { it.recordCleanup() }
    }
}
