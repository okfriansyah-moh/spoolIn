package com.spoolin.core.features.draft

import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.operations.events.ReceiptAction
import com.spoolin.core.operations.events.ReceiptAudit
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.logging.SafeJsonDiagnostics
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.SafeResult
import org.slf4j.LoggerFactory
import java.util.UUID

private const val DRAFT_SAVE_KIND = "DRAFT_SAVE"

/**
 * Application service for [saveDraft] mutations.
 *
 * Implements §7.3.1 receipt-first idempotency: the operation fingerprint is checked before any
 * business mutation.  Nonce reservation is committed in its own separate transaction before
 * domain locks are acquired.
 */
class SaveDraft(
    private val drafts: ProtectedDrafts,
    private val crypto: DraftCryptography,
    private val payload: DraftPayload,
    private val receipts: OwnerReceipts,
) {
    private val logger = LoggerFactory.getLogger(SaveDraft::class.java)
    /**
     * Saves a draft owned by [accountId].
     *
     * - Null [expectedVersion] = create (new draft, version 0).
     * - Non-null [expectedVersion] = update with CAS.
     *
     * @throws CredentialRejected if not authenticated.
     * @throws DraftStaleVersion if the expected version does not match.
     * @throws DraftQuotaExceeded if the account already has 20 live drafts.
     */
    fun save(command: DraftSaveCommand): ReceiptOutcome {
        require(command.expectedVersion == null || command.expectedVersion >= 0) {
            "expectedVersion must be non-negative"
        }
        val plaintext = payload.encode(command.input.targetKind, command.input.values)
        // Reserve nonce in a separately committed transaction BEFORE domain locks (§7.3.1).
        val envelope =
            try {
                try {
                    crypto.encrypt(command.draftId, plaintext)
                } catch (failure: Exception) {
                    logger.warn(
                        SafeJsonDiagnostics.emit(
                            mapOf(
                                "event" to "draft_encrypt_failed",
                                "draftId" to command.draftId.toString(),
                                "targetKind" to command.input.targetKind,
                            ),
                        ),
                    )
                    throw failure
                }
            } finally {
                plaintext.fill(0)
            }
        val intent =
            OperationIntent(
                id = command.operationId,
                actorScope = "account:${command.accountId}",
                kind = DRAFT_SAVE_KIND,
                policyRevision = command.input.policyRevision,
                fields =
                    buildList {
                        add("draftId" to command.draftId.toString())
                        add("targetKind" to command.input.targetKind)
                        add("targetId" to command.input.targetId?.toString())
                        add("baseVersion" to command.input.baseVersion?.toString())
                        add("operationId" to command.input.operationId?.toString())
                        add("expectedVersion" to command.expectedVersion?.toString())
                        add("values" to command.input.values)
                    },
            )
        val audit =
            ReceiptAudit(
                ReceiptAction.DRAFT_SAVED,
                actorId = command.accountId,
                subjectId = command.draftId,
            )
        return receipts.executeAudited(intent, command.correlationId, audit) { connection ->
            val newVersion =
                drafts.saveInTransaction(
                    connection,
                    DraftPersistCommand(
                        accountId = command.accountId,
                        draftId = command.draftId,
                        expectedVersion = command.expectedVersion,
                        input = command.input,
                        envelope = envelope,
                    ),
                )
            SafeResult(resultId = command.draftId, version = newVersion)
        }
    }
}
