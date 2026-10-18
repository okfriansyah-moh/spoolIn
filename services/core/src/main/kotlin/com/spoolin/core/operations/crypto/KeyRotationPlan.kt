package com.spoolin.core.operations.crypto

import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import javax.sql.DataSource

internal const val MAX_ROTATION_BATCHES = 10000

/** Activates only verified material; resumable backfill and retirement have distinct outcomes. */
data class KeyRotationPlan(
    val fromAlias: String,
    val toAlias: String,
    val purpose: KeyPurpose,
) {
    init {
        require(fromAlias != toAlias && listOf(fromAlias, toAlias).all { it.matches(Regex("[A-Za-z0-9_-]{1,64}")) })
    }

    fun rotate(
        dataSource: DataSource,
        registry: KeyRegistry,
        dryRun: Boolean,
        keys: MountedKeys? = null,
        maxBatches: Int = 100,
    ): RotationOutcome {
        require(maxBatches in 1..MAX_ROTATION_BATCHES)
        if (keys == null) {
            return RotationOutcome(
                false,
                "Rotation requires validated mounted source and target key material; " +
                    "no state changed",
            )
        }
        return dataSource.connection.use { lock ->
            lock.queryBound("SELECT pg_advisory_lock(734850220,?)", listOf(purpose.ordinal)) { true }
            try {
                rotateLocked(dataSource, registry, dryRun, keys, maxBatches)
            } finally {
                lock.queryBound("SELECT pg_advisory_unlock(734850220,?)", listOf(purpose.ordinal)) { true }
            }
        }
    }

    private fun rotateLocked(
        dataSource: DataSource,
        registry: KeyRegistry,
        dryRun: Boolean,
        keys: MountedKeys,
        maxBatches: Int,
    ): RotationOutcome =
        when {
            !eligible(states(dataSource)) ->
                RotationOutcome(false, "Rotation source/target state is incompatible; no state changed")
            dryRun -> validateDryRun(keys)
            else -> activateAndBackfill(dataSource, registry, keys, maxBatches)
        }

    private fun activateAndBackfill(
        dataSource: DataSource,
        registry: KeyRegistry,
        keys: MountedKeys,
        maxBatches: Int,
    ): RotationOutcome {
        RotationKeys.validateSource(dataSource, keys, fromAlias, purpose)
        RotationKeys.validate(dataSource, keys, toAlias, purpose)
        activate(dataSource)
        return RotationBackfill(dataSource, registry, keys, this).run(maxBatches)
    }

    private fun validateDryRun(keys: MountedKeys): RotationOutcome {
        require(
            keys.key(fromAlias, purpose)?.also { it.fill(0) } != null &&
                keys.key(toAlias, purpose)?.also { it.fill(0) } != null,
        )
        return RotationOutcome(
            true,
            "Dry run validated key availability and registration; no activation, backfill or retirement performed",
        )
    }

    private fun states(dataSource: DataSource): Map<String, String> =
        dataSource.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT id,state FROM appdata.crypto_key_version WHERE id IN (?,?) AND purpose=? ORDER BY id",
                    listOf(fromAlias, toAlias, purpose.name),
                ) { it.getString("id") to it.getString("state") }
                .toMap()
        }

    private fun eligible(states: Map<String, String>): Boolean =
        states.size == 2 &&
            (
                (states[fromAlias] == "WRITE" && states[toAlias] == "READ") ||
                    (states[fromAlias] == "READ" && states[toAlias] == "WRITE")
            )

    private fun activate(dataSource: DataSource) =
        dataSource.committed { connection ->
            val current =
                connection
                    .queryBound(
                        "SELECT id,state FROM appdata.crypto_key_version WHERE purpose=? ORDER BY id FOR UPDATE",
                        listOf(purpose.name),
                    ) {
                        it.getString("id") to it.getString("state")
                    }.toMap()
            check(eligible(current.filterKeys { it in setOf(fromAlias, toAlias) })) { "Rotation state changed" }
            if (current[toAlias] != "WRITE") {
                check(
                    connection.executeBound(
                        "UPDATE appdata.crypto_key_version SET state='READ' WHERE id=? AND purpose=? AND state='WRITE'",
                        fromAlias,
                        purpose.name,
                    ) ==
                        1,
                )
                check(
                    connection.executeBound(
                        "UPDATE appdata.crypto_key_version SET state='WRITE' WHERE id=? AND purpose=? AND state='READ'",
                        toAlias,
                        purpose.name,
                    ) ==
                        1,
                )
            }
        }
}

data class RotationOutcome(
    val ok: Boolean,
    val message: String,
    val backfilled: Int = 0,
    val remaining: Long = 0,
)
