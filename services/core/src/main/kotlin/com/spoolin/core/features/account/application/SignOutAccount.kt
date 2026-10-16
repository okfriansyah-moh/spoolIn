package com.spoolin.core.features.account.application

import com.spoolin.core.features.session.SessionCredential
import com.spoolin.core.features.session.SessionStore
import com.spoolin.core.operations.events.ReceiptAction
import com.spoolin.core.operations.events.ReceiptAudit
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.SafeResult
import java.util.UUID

class SignOutAccount(
    private val sessions: SessionStore,
    private val receipts: OwnerReceipts,
    private val admission: AuthenticationAdmission = AuthenticationAdmission.unavailable,
) {
    fun signOut(
        operationId: UUID,
        credential: SessionCredential,
        csrf: String,
        correlation: UUID,
        request: AuthenticationRequest = AuthenticationRequest.unavailable,
    ): ReceiptOutcome {
        request.requireActive()
        admission.check(AuthenticationAction.SIGN_OUT, null, request)
        request.requireActive()
        val target = sessions.logoutIdentity(credential, csrf) ?: throw CredentialRejected()
        val intent =
            OperationIntent(
                operationId,
                "account:${target.accountId}",
                "SignOut",
                "signout-v1",
                listOf("session" to target.sessionId.toString()),
            )
        val verified = sessions.find(credential)
        if (verified == null) return receipts.resolve(intent) ?: throw CredentialRejected()
        val audit = ReceiptAudit(ReceiptAction.SESSION_SIGNED_OUT, target.accountId, target.sessionId)
        return receipts.executeAudited(intent, correlation, audit) { connection ->
            sessions.revoke(connection, target.sessionId, target.accountId)
            request.requireActive()
            SafeResult(operationId)
        }
    }
}
