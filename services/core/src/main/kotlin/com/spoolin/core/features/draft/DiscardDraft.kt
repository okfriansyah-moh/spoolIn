package com.spoolin.core.features.draft

import com.spoolin.core.operations.events.ReceiptAction
import com.spoolin.core.operations.events.ReceiptAudit
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.SafeResult
import java.util.UUID

private const val DRAFT_DISCARD_KIND = "DRAFT_DISCARD"

/**
 * Application service for [discardDraft] mutations.
 *
 * Implements §7.3.1 receipt-first idempotency.  Absent / other-account drafts return
 * `discarded = false`; a stale version throws [DraftStaleVersion]; a matching owned draft
 * is deleted atomically with the receipt and redacted audit.
 */
class DiscardDraft(
    private val drafts: ProtectedDrafts,
    private val receipts: OwnerReceipts,
) {
    /**
     * Discards a draft identified by [draftId] at [expectedVersion] for [accountId].
     *
     * @throws DraftStaleVersion if the existing owned draft version differs from [expectedVersion].
     */
    fun discard(
        operationId: UUID,
        accountId: UUID,
        draftId: UUID,
        expectedVersion: Long,
        correlationId: UUID,
    ): ReceiptOutcome {
        require(expectedVersion >= 0) { "expectedVersion must be non-negative" }
        val intent =
            OperationIntent(
                id = operationId,
                actorScope = "account:$accountId",
                kind = DRAFT_DISCARD_KIND,
                policyRevision = "discard",
                fields =
                    listOf(
                        "draftId" to draftId.toString(),
                        "expectedVersion" to expectedVersion.toString(),
                    ),
            )
        val audit = ReceiptAudit(ReceiptAction.DRAFT_DISCARDED, actorId = accountId, subjectId = draftId)
        return receipts.executeAudited(intent, correlationId, audit) { connection ->
            val discarded =
                drafts.discardInTransaction(
                    connection = connection,
                    accountId = accountId,
                    draftId = draftId,
                    expectedVersion = expectedVersion,
                )
            SafeResult(resultId = draftId, discarded = discarded)
        }
    }
}
