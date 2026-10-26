package com.spoolin.catalog.features.lifecycle

/** Archive-first lifecycle controls (AC-110 / AC-133). */
object LifecycleRules {
    enum class Action { ARCHIVE, RESTORE, DELETE }

    fun allowedActions(status: String): Set<Action> =
        when (status) {
            "ACTIVE" -> setOf(Action.ARCHIVE)
            "ARCHIVED" -> setOf(Action.RESTORE, Action.DELETE)
            else -> emptySet()
        }

    fun requiresConfirmation(action: Action): Boolean = action == Action.DELETE

    const val CANCEL_CHANGES_NOTHING: Boolean = true
}

internal data class LifecycleCommand(
    val target: java.util.UUID,
    val expectedVersion: Long,
    val action: LifecycleRules.Action,
    val confirmed: Boolean,
    val impactToken: String?,
    val operation: java.util.UUID,
    val correlation: java.util.UUID,
    val actor: java.util.UUID,
    val policy: String,
) {
    fun intent(kind: String) =
        com.spoolin.support.receipts.OperationIntent(
            operation,
            "account:$actor",
            kind,
            policy,
            listOf(
                "target" to target.toString(),
                "expectedVersion" to expectedVersion.toString(),
                "action" to action.name,
                "confirmed" to confirmed.toString(),
                "impactToken" to impactToken,
            ),
        )

    companion object {
        fun parse(
            request: spoolin.v1.Spoolin.Lifecycle,
            claims: com.spoolin.support.identity.ActorClaims,
        ): LifecycleCommand =
            try {
                require(request.target.hasExpectedVersion() && request.target.expectedVersion >= 0)
                val command =
                    LifecycleCommand(
                        id(request.target.id),
                        request.target.expectedVersion,
                        LifecycleRules.Action.valueOf(request.action),
                        request.confirmed,
                        if (request.hasImpactToken()) request.impactToken else null,
                        id(request.target.context.operationId),
                        id(request.target.context.correlationId),
                        claims.accountId,
                        request.target.context.policyRevision,
                    )
                if (command.operation != claims.operationId || command.correlation != claims.correlationId) {
                    throw io.grpc.Status.PERMISSION_DENIED
                        .asRuntimeException()
                }
                command
            } catch (_: IllegalArgumentException) {
                throw io.grpc.Status.INVALID_ARGUMENT
                    .asRuntimeException()
            }

        private fun id(value: String): java.util.UUID =
            java.util.UUID.fromString(value).also {
                require(it.toString() == value)
            }
    }
}

internal fun lifecycleOutcome(receipt: com.spoolin.support.receipts.ReceiptOutcome): spoolin.v1.Spoolin.Outcome =
    com.spoolin.catalog.boundary.grpc.CatalogWriteRpc.outcome(receipt)

internal fun lifecycleRejection(
    request: spoolin.v1.Spoolin.Lifecycle,
    failure: io.grpc.StatusRuntimeException,
): spoolin.v1.Spoolin.Outcome? {
    val code =
        when (failure.status.description) {
            "IMPACT_CHANGED", "STALE_VERSION", "POLICY_CHANGED" -> failure.status.description
            "LIFECYCLE_STATE_CHANGED", "CONFIRMATION_REQUIRED" -> "FIELD_INVALID"
            else -> return null
        }
    return spoolin.v1.Spoolin.Outcome
        .newBuilder()
        .setOperationId(request.target.context.operationId)
        .setState("FAILED")
        .setProblem(
            spoolin.v1.Spoolin.Problem
                .newBuilder()
                .setCode(code),
        ).build()
}
