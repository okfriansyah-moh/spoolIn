package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import java.sql.Connection
import java.time.Instant
import javax.sql.DataSource

fun interface BackupKeyCustody {
    /** Implementations must inspect actual retained backups and tested retained key access. */
    fun verified(keyId: String, purpose: KeyPurpose): Boolean
}

class RotationRetirement(
    private val dataSource: DataSource,
    private val custody: BackupKeyCustody = BackupKeyCustody { _, _ -> false },
) {
    fun retire(id: String, purpose: KeyPurpose): RotationOutcome {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        if (!custody.verified(id, purpose)) {
            return RotationOutcome(false,
                "Retirement blocked: retained backup expiry or tested key custody is unverified")
        }
        return dataSource.committed { connection -> retireLocked(connection, id, purpose) }
    }

    private fun retireLocked(connection: Connection, id: String, purpose: KeyPurpose): RotationOutcome {
        val keys = lockKeys(connection, purpose)
        val old = keys.singleOrNull { it.id == id } ?: return RotationOutcome(false, "Retirement key is unavailable")
        return if (old.state != "READ" || old.review == null || Instant.now().isBefore(old.review)) {
            RotationOutcome(false, "Retirement blocked by key state or earliest review time")
        } else {
            retireUnreferenced(connection, id, purpose, keys)
        }
    }

    private fun retireUnreferenced(
        connection: Connection,
        id: String,
        purpose: KeyPurpose,
        keys: List<Key>,
    ): RotationOutcome {
        val encrypted = encryptedReferences(connection, id)
        if (encrypted != 0L) {
            return RotationOutcome(false, "Retirement blocked by live encrypted references", remaining = encrypted)
        }
        val lookupFailure = if (purpose == KeyPurpose.EMAIL_LOOKUP) retireLookupAliases(connection, id, keys) else null
        return lookupFailure ?: retireProtected(connection, id, purpose)
    }

    private fun retireProtected(connection: Connection, id: String, purpose: KeyPurpose): RotationOutcome {
        val referenced = protectedReferences(connection, id)
        return if (referenced != 0L) {
            RotationOutcome(false, "Retirement blocked by live protected references", remaining = referenced)
        } else {
            retireKey(connection, id, purpose)
        }
    }

    private fun retireKey(connection: Connection, id: String, purpose: KeyPurpose): RotationOutcome {
        check(connection.executeBound(
            "UPDATE appdata.crypto_key_version SET state='RETIRED' WHERE id=? AND purpose=? AND state='READ'",
            id, purpose.name,
        ) == 1)
        return RotationOutcome(true,
            "Key retired after live-reference and independently verified backup checks; " +
                "material custody remains governed by retained backups")
    }

    private fun lockKeys(connection: Connection, purpose: KeyPurpose): List<Key> = connection.queryBound(
        "SELECT id,state,retire_after FROM appdata.crypto_key_version WHERE purpose=? ORDER BY id FOR UPDATE",
        listOf(purpose.name),
    ) { Key(it.getString("id"), it.getString("state"), it.getTimestamp("retire_after")?.toInstant()) }

    private fun encryptedReferences(connection: Connection, id: String): Long = connection.queryBound(
        "SELECT (SELECT count(*) FROM appdata.user_account WHERE pii_key_id=?) + " +
            "(SELECT count(*) FROM appdata.form_draft WHERE body_key_id=?) AS total",
        listOf(id, id),
    ) { it.getLong("total") }.single()

    private fun retireLookupAliases(connection: Connection, id: String, keys: List<Key>): RotationOutcome? {
        val active = keys.singleOrNull { it.state == "WRITE" }
            ?: return RotationOutcome(false, "Active lookup key is unavailable")
        val missing = connection.queryBound(
            "SELECT count(*) AS total FROM appdata.user_account a WHERE NOT EXISTS(" +
                "SELECT 1 FROM appdata.account_email_lookup e WHERE e.account_id=a.id AND e.key_id=?)",
            listOf(active.id),
        ) { it.getLong("total") }.single()
        return if (missing != 0L) {
            RotationOutcome(false, "Retirement blocked by incomplete lookup coverage", remaining = missing)
        } else {
            connection.executeBound("DELETE FROM appdata.account_email_lookup WHERE key_id=?", id)
            null
        }
    }

    private fun protectedReferences(connection: Connection, id: String): Long = connection.queryBound(
        "SELECT (SELECT count(*) FROM appdata.user_account WHERE pii_key_id=?) + " +
            "(SELECT count(*) FROM appdata.form_draft WHERE body_key_id=?) + " +
            "(SELECT count(*) FROM appdata.account_email_lookup WHERE key_id=?) AS total",
        listOf(id, id, id),
    ) { it.getLong("total") }.single()

    private data class Key(val id: String, val state: String, val review: Instant?)
}
