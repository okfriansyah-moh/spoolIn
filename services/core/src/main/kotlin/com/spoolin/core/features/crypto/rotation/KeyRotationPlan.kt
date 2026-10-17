package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.support.receipts.executeBound
import javax.sql.DataSource

/** Coordinated alias rotation plan (T040). */
data class KeyRotationPlan(
    val fromAlias: String,
    val toAlias: String,
    val purpose: KeyPurpose,
) {
    fun rotate(
        dataSource: DataSource,
        registry: KeyRegistry,
        dryRun: Boolean,
    ): RotationOutcome {
        val current = registry.writeKey(purpose)
        if (current.id != fromAlias) {
            return RotationOutcome(false, "active write key is ${current.id}, expected $fromAlias")
        }
        if (dryRun) {
            return RotationOutcome(true, "dry-run: would promote $toAlias to WRITE and retire $fromAlias")
        }
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            connection.executeBound(
                "UPDATE crypto_key_version SET state = 'READ' WHERE id = ? AND purpose = ? AND state = 'WRITE'",
                fromAlias,
                purpose.name,
            )
            connection.executeBound(
                "UPDATE crypto_key_version SET state = 'WRITE' WHERE id = ? AND purpose = ? AND state = 'READ'",
                toAlias,
                purpose.name,
            )
            connection.commit()
        }
        return RotationOutcome(true, "rotated ${purpose.name} write key to $toAlias")
    }
}

data class RotationOutcome(
    val ok: Boolean,
    val message: String,
)
