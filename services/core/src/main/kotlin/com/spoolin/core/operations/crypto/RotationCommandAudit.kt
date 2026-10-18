package com.spoolin.core.operations.crypto

import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import javax.sql.DataSource

internal class RotationCommandAudit(
    private val pool: DataSource,
    private val options: RotationCliOptions,
    private val operator: String,
) {
    private val correlation = UUID.randomUUID()

    fun completed(result: RotationOutcome) {
        record(if (result.ok) "COMPLETED" else "INCOMPLETE", if (result.ok) "SUCCEEDED" else "FAILED", result)
    }

    fun record(
        phase: String,
        outcome: String,
        result: RotationOutcome? = null,
    ) {
        val changes = changes(phase, result)
        pool.committed { connection ->
            check(
                connection.executeBound(
                    "INSERT INTO operational_event(id,actor_kind,action,subject_kind,outcome,changes," +
                        "correlation_id,event_type,operator_id) " +
                        "VALUES(?,'HUMAN','KEY_ROTATION','CRYPTO_KEY',?,?::jsonb,?,'ACTIVITY',?)",
                    UUID.randomUUID(),
                    outcome,
                    changes,
                    correlation,
                    operator,
                ) == 1,
            )
        }
    }

    private fun changes(
        phase: String,
        result: RotationOutcome?,
    ): String =
        JsonMapper.builder().build().writeValueAsString(
            mapOf(
                "purpose" to options.purpose.name,
                "fromKeyId" to options.from,
                "toKeyId" to options.to,
                "phase" to phase,
                "maxBatches" to options.maxBatches,
                "backfilled" to result?.backfilled,
                "remaining" to result?.remaining,
            ),
        )
}
