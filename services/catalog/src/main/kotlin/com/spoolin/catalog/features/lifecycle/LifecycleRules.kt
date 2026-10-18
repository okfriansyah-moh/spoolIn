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
    val target: java.util.UUID, val expectedVersion: Long, val action: LifecycleRules.Action,
    val confirmed: Boolean, val impactToken: String?, val operation: java.util.UUID,
    val correlation: java.util.UUID, val actor: java.util.UUID, val policy: String,
) {
    fun intent(kind: String) = com.spoolin.support.receipts.OperationIntent(
        operation, "account:$actor", kind, policy,
        listOf("target" to target.toString(), "expectedVersion" to expectedVersion.toString(),
            "action" to action.name, "confirmed" to confirmed.toString(), "impactToken" to impactToken),
    )

    companion object {
        fun parse(request: spoolin.v1.Spoolin.Lifecycle, actor: java.util.UUID): LifecycleCommand = try {
            require(request.target.expectedVersion >= 0)
            LifecycleCommand(id(request.target.id), request.target.expectedVersion,
                LifecycleRules.Action.valueOf(request.action), request.confirmed,
                if (request.hasImpactToken()) request.impactToken else null,
                id(request.target.context.operationId), id(request.target.context.correlationId), actor,
                request.target.context.policyRevision)
        } catch (_: IllegalArgumentException) { throw io.grpc.Status.INVALID_ARGUMENT.asRuntimeException() }

        private fun id(value: String): java.util.UUID = java.util.UUID.fromString(value).also {
            require(it.toString() == value)
        }
    }
}

internal fun lifecycleOutcome(receipt: com.spoolin.support.receipts.ReceiptOutcome): spoolin.v1.Spoolin.Outcome =
    spoolin.v1.Spoolin.Outcome.newBuilder().setOperationId(receipt.id.toString()).setState(receipt.state.name)
        .setResultExpired(receipt.resultExpired).also { builder ->
            receipt.result?.let { result -> builder.resultId = result.resultId.toString()
                result.version?.let { builder.version = it } }
            receipt.errorCode?.let { builder.problem = spoolin.v1.Spoolin.Problem.newBuilder().setCode(it).build() }
        }.build()
