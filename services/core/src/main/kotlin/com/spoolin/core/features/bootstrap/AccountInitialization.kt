package com.spoolin.core.features.bootstrap

import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.PreparedAccount
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.requireProtectedFile
import java.nio.file.Files
import java.security.MessageDigest
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

private const val PASSWORD_FILE_LIMIT = 4096L

class AccountInitialization(
    private val dataSource: DataSource,
    private val accounts: ProtectedAccounts,
    private val hasher: PasswordHasher,
) {
    fun initialize(config: InitializationConfiguration): Boolean =
        dataSource.connection.use { connection ->
            initializeLocked(connection, config)
        }

    private fun initializeLocked(
        connection: Connection,
        config: InitializationConfiguration,
    ): Boolean {
        try {
            connection.queryBound("SELECT pg_advisory_lock(734850219)") { true }
            if (replay(connection, config)) return false
            initializeKeys(connection, config)
            return persist(connection, config)
        } finally {
            if (!connection.autoCommit) connection.rollback()
            connection.autoCommit = true
            connection.queryBound("SELECT pg_advisory_unlock(734850219)") { true }
        }
    }

    private fun persist(
        connection: Connection,
        config: InitializationConfiguration,
    ): Boolean {
        var committed = false
        try {
            val prepared = config.accounts.map { prepare(it) }
            connection.autoCommit = false
            prepared.forEach { accounts.insert(connection, it) }
            receipt(connection, config, "SUCCEEDED")
            event(connection, config, "SUCCEEDED")
            connection.commit()
            committed = true
            return true
        } finally {
            if (!committed) recordFailure(connection, config)
        }
    }

    private fun recordFailure(
        connection: Connection,
        config: InitializationConfiguration,
    ) {
        if (!connection.autoCommit) connection.rollback()
        connection.autoCommit = false
        receipt(connection, config, "FAILED")
        event(connection, config, "FAILED")
        connection.commit()
    }

    private fun prepare(account: InitializationAccount): PreparedAccount {
        requireProtectedFile(account.passwordFile, PASSWORD_FILE_LIMIT)
        val password = Files.readString(account.passwordFile)
        return accounts.prepare(account.id, account.identity, hasher.hashNew(password), account.role)
    }

    private fun initializeKeys(
        connection: Connection,
        config: InitializationConfiguration,
    ) {
        connection.autoCommit = false
        try {
            config.keys.forEach { (id, mount) -> initializeKey(connection, id, mount.purpose.name) }
            connection.commit()
        } finally {
            connection.rollback()
            connection.autoCommit = true
        }
    }

    private fun replay(
        connection: Connection,
        config: InitializationConfiguration,
    ): Boolean {
        val rows =
            connection.queryBound(
                "SELECT setup_version,setup_manifest_hash,state,setup_viewer_id,setup_maintainer_id " +
                    "FROM appdata.operation_receipt WHERE receipt_type='ACCOUNT_INITIALIZATION'",
            ) { row ->
                require(row.getString("setup_version") == config.version)
                require(MessageDigest.isEqual(row.getBytes("setup_manifest_hash"), config.checksum()))
                if (row.getString("state") == "SUCCEEDED") {
                    validateStored(
                        connection,
                        config,
                        row.getObject("setup_viewer_id", UUID::class.java),
                        AccountRole.VIEWER,
                    )
                    validateStored(
                        connection,
                        config,
                        row.getObject("setup_maintainer_id", UUID::class.java),
                        AccountRole.MAINTAINER,
                    )
                    true
                } else {
                    false
                }
            }
        require(rows.size <= 1) { "Initialization history conflicts" }
        return rows.singleOrNull() == true
    }

    private fun validateStored(
        connection: Connection,
        config: InitializationConfiguration,
        id: UUID,
        role: AccountRole,
    ) {
        val expected = config.accounts.single { it.role == role }
        require(expected.id == id)
        val roles =
            connection.queryBound(
                "SELECT role FROM appdata.user_account WHERE id=?",
                listOf(id),
            ) { it.getString("role") }
        require(roles.singleOrNull() == role.name) { "Prepared account role conflicts" }
        val identity = accounts.identity(id)
        require(identity is ProtectedRead.Value && identity.value.emailNormalized == expected.identity.emailNormalized)
        require(
            identity.value.name == expected.identity.name &&
                identity.value.emailDisplay == expected.identity.emailDisplay,
        )
    }

    private fun receipt(
        connection: Connection,
        config: InitializationConfiguration,
        state: String,
    ) {
        val viewer =
            config.accounts
                .single { it.role == AccountRole.VIEWER }
                .id
                .takeIf { state == "SUCCEEDED" }
        val maintainer =
            config.accounts
                .single { it.role == AccountRole.MAINTAINER }
                .id
                .takeIf { state == "SUCCEEDED" }
        connection
            .executeBound(
                "INSERT INTO appdata.operation_receipt(id,actor_scope,kind,receipt_type,request_hash,state," +
                    "correlation_id," +
                    "setup_version,setup_manifest_hash,setup_viewer_id,setup_maintainer_id) " +
                    "VALUES (?,'SERVICE_INITIALIZER','INITIALIZE_ACCOUNTS','ACCOUNT_INITIALIZATION',?,?,?,?,?,?,?) " +
                    "ON CONFLICT (setup_version) DO UPDATE SET state=EXCLUDED.state," +
                    "setup_viewer_id=EXCLUDED.setup_viewer_id,setup_maintainer_id=EXCLUDED.setup_maintainer_id " +
                    "WHERE operation_receipt.state='FAILED' " +
                    "AND operation_receipt.setup_manifest_hash=EXCLUDED.setup_manifest_hash",
                receiptId(config),
                config.checksum(),
                state,
                receiptId(config),
                config.version,
                config.checksum(),
                viewer,
                maintainer,
            ).also { require(it == 1) { "Initialization receipt conflicts" } }
    }

    private fun event(
        connection: Connection,
        config: InitializationConfiguration,
        outcome: String,
    ) {
        connection.executeBound(
            "INSERT INTO appdata.operational_event(" +
                "id,operation_id,actor_kind,action,subject_kind,outcome,correlation_id,event_type) " +
                "VALUES (?,?,'SERVICE','INITIALIZE_ACCOUNTS','ACCOUNT',?,?,'ACTIVITY')",
            UUID.randomUUID(),
            receiptId(config),
            outcome,
            receiptId(config),
        )
    }
}

private fun receiptId(config: InitializationConfiguration): UUID {
    val namespace = UUID.fromString("9da61864-80f5-5c70-bc79-3914da94079e")
    val name = "initialization/core/ACCOUNT_INITIALIZATION/${config.version}".toByteArray(Charsets.UTF_8)
    return com.spoolin.support.identity.NamedIdentities
        .id(namespace, name)
}

private fun initializeKey(
    connection: Connection,
    id: String,
    purpose: String,
) {
    val current =
        connection.queryBound(
            "SELECT id,state FROM appdata.crypto_key_version WHERE purpose=?",
            listOf(purpose),
        ) { it.getString("id") to it.getString("state") }
    if (current.isEmpty()) {
        connection.executeBound(
            "INSERT INTO appdata.crypto_key_version(id,purpose,state) VALUES (?,?,'WRITE')",
            id,
            purpose,
        )
    } else {
        require(current.any { it.first == id && it.second == "WRITE" }) {
            "Initialization key configuration conflicts"
        }
    }
}
