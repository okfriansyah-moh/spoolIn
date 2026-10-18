package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import javax.sql.DataSource

fun interface BackupKeyCustody {
    /** Implementations must inspect actual retained backups and tested retained key access. */
    fun verified(keyId: String, purpose: KeyPurpose): Boolean
}

class RotationRetirement(private val dataSource: DataSource, private val custody: BackupKeyCustody = BackupKeyCustody { _, _ -> false }) {
    fun retire(id: String, purpose: KeyPurpose): RotationOutcome {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        if (!custody.verified(id, purpose)) return RotationOutcome(false, "Retirement blocked: retained backup expiry or tested key custody is unverified")
        return dataSource.committed { connection ->
            val keys = connection.queryBound("SELECT id,state,retire_after FROM appdata.crypto_key_version WHERE purpose=? ORDER BY id FOR UPDATE", listOf(purpose.name)) {
                Triple(it.getString("id"), it.getString("state"), it.getTimestamp("retire_after")?.toInstant())
            }
            val old = keys.singleOrNull { it.first == id } ?: return@committed RotationOutcome(false, "Retirement key is unavailable")
            if (old.second != "READ" || old.third == null || java.time.Instant.now().isBefore(old.third)) {
                return@committed RotationOutcome(false, "Retirement blocked by key state or earliest review time")
            }
            val encrypted = connection.queryBound("SELECT (SELECT count(*) FROM appdata.user_account WHERE pii_key_id=?) + " +
                "(SELECT count(*) FROM appdata.form_draft WHERE body_key_id=?) AS total", listOf(id, id)) { it.getLong("total") }.single()
            if (encrypted != 0L) return@committed RotationOutcome(false, "Retirement blocked by live encrypted references", remaining = encrypted)
            if (purpose == KeyPurpose.EMAIL_LOOKUP) {
                val active = keys.singleOrNull { it.second == "WRITE" } ?: return@committed RotationOutcome(false, "Active lookup key is unavailable")
                val missing = connection.queryBound("SELECT count(*) AS total FROM appdata.user_account a WHERE NOT EXISTS(" +
                    "SELECT 1 FROM appdata.account_email_lookup e WHERE e.account_id=a.id AND e.key_id=?)", listOf(active.first)) { it.getLong("total") }.single()
                if (missing != 0L) return@committed RotationOutcome(false, "Retirement blocked by incomplete lookup coverage", remaining = missing)
                connection.executeBound("DELETE FROM appdata.account_email_lookup WHERE key_id=?", id)
            }
            val referenced = connection.queryBound("SELECT (SELECT count(*) FROM appdata.user_account WHERE pii_key_id=?) + " +
                "(SELECT count(*) FROM appdata.form_draft WHERE body_key_id=?) + (SELECT count(*) FROM appdata.account_email_lookup WHERE key_id=?) AS total",
                listOf(id, id, id)) { it.getLong("total") }.single()
            if (referenced != 0L) return@committed RotationOutcome(false, "Retirement blocked by live protected references", remaining = referenced)
            check(connection.executeBound("UPDATE appdata.crypto_key_version SET state='RETIRED' WHERE id=? AND purpose=? AND state='READ'", id, purpose.name) == 1)
            RotationOutcome(true, "Key retired after live-reference and independently verified backup checks; material custody remains governed by retained backups")
        }
    }
}
