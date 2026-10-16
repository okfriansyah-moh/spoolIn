package com.spoolin.support.receipts

fun interface ReceiptAuthority {
    fun actorScope(): String?
}

object RegistrationIntent {
    fun create(
        id: java.util.UUID,
        anonymousScope: String,
        policyRevision: String,
        normalizedIdentity: String,
        submissionNonce: java.util.UUID,
    ): OperationIntent =
        OperationIntent(
            id,
            anonymousScope,
            "REGISTER",
            policyRevision,
            listOf("identity" to normalizedIdentity, "submissionNonce" to submissionNonce.toString()),
        )
}
