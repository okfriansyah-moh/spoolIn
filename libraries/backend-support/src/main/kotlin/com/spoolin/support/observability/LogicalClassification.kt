package com.spoolin.support.observability

import com.google.protobuf.Message

/** Typed protobuf response projection; malformed and absent outcomes stay uncertain. */
internal object LogicalClassification {
    fun message(
        operation: MetricOperation,
        value: Any?,
    ): LogicalResult {
        return if (value is Message && value.descriptorForType.fullName == operation.response) {
            if (operation.receiptLookup) lookup(value) else reply(operation, value)
        } else {
            LogicalResult.unknown()
        }
    }

    private fun reply(
        operation: MetricOperation,
        value: Message,
    ): LogicalResult =
        when (operation.response) {
            "spoolin.v1.Outcome" -> receipt(value)
            "spoolin.v1.ArtworkCancelOutcome" -> child(value, "operation")?.let(::receipt) ?: LogicalResult.unknown()
            else -> problem(value)?.let { LogicalResult.fromProblem(it) } ?: knownReply(value)
        }

    private val collectionReplies =
        setOf("TitlePage", "PersonPage", "CreditPage", "Genres", "Roles", "AuditPage", "Impact")
    private val identityFields =
        mapOf(
            "Title" to "id",
            "Person" to "id",
            "Asset" to "id",
            "Attachment" to "hold_id",
            "ArtworkGrant" to "asset_id",
            "ProposalRevision" to "proposal_id",
        )

    private fun knownReply(message: Message): LogicalResult {
        val name = message.descriptorForType.name
        if (name in collectionReplies) return LogicalResult.success()
        val identity = identityFields[name]?.let { string(message, it) }
        val known =
            when (name) {
                in identityFields -> !identity.isNullOrBlank()
                "PolicySnapshot" -> !string(message, "revision").isNullOrBlank()
                "MovieContext" -> child(message, "policy") != null
                "AssetReadChunk" -> message.allFields.isNotEmpty()
                else -> false
            }
        return if (known) LogicalResult.success() else LogicalResult.unknown()
    }

    private fun lookup(message: Message): LogicalResult {
        val known =
            !string(message, "operation_id").isNullOrBlank() &&
                string(message, "state") in setOf("SUCCEEDED", "FAILED", "PENDING", "EXPIRED")
        return if (known) LogicalResult.success() else LogicalResult.unknown()
    }

    private fun receipt(message: Message): LogicalResult =
        LogicalResult.fromReceipt(string(message, "state"), problem(message))

    private fun problem(message: Message): String? = child(message, "problem")?.let { string(it, "code") }

    private fun child(
        message: Message,
        name: String,
    ): Message? {
        val field = message.descriptorForType.findFieldByName(name) ?: return null
        return if (message.hasField(field)) message.getField(field) as? Message else null
    }

    private fun string(
        message: Message,
        name: String,
    ): String? = message.descriptorForType.findFieldByName(name)?.let { message.getField(it) as? String }
}
