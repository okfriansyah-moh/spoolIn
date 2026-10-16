package com.spoolin.core.features.account.application

import com.spoolin.core.features.session.AnonymousSubmission
import com.spoolin.core.features.session.IssuedSession
import com.spoolin.core.features.session.SessionCredential
import com.spoolin.core.features.session.SessionStore
import com.spoolin.core.operations.events.ReceiptAction
import com.spoolin.core.operations.events.ReceiptAudit
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.SafeResult
import java.util.UUID

class LoginInput(
    val operationId: UUID,
    val email: String,
    val password: String,
) {
    override fun toString(): String = "LoginInput(content=redacted)"
}

class LoginResult(
    val outcome: ReceiptOutcome,
    val issued: IssuedSession?,
)

class SignInAccount(
    private val verifier: CredentialVerifier,
    private val sessions: SessionStore,
    private val receipts: OwnerReceipts,
    private val admission: AuthenticationAdmission = AuthenticationAdmission.unavailable,
) {
    fun signIn(
        input: LoginInput,
        submission: AnonymousSubmission,
        previous: SessionCredential?,
        correlation: UUID,
        request: AuthenticationRequest = AuthenticationRequest.unavailable,
    ): LoginResult {
        val identity = verifier.normalizedEmail(input.email)
        request.requireActive()
        admission.check(AuthenticationAction.SIGN_IN, identity, request)
        request.requireActive()
        val account = verifier.verify(identity, input.password)
        val intent =
            OperationIntent(
                input.operationId,
                submission.scope,
                "SignIn",
                "signin-v1",
                listOf("account" to account.id.toString(), "nonce" to submission.nonce.toString()),
            )
        request.requireActive()
        val id = UUID.randomUUID()
        val audit = ReceiptAudit(ReceiptAction.SESSION_SIGNED_IN, account.id, id)
        var issued: IssuedSession? = null
        val outcome =
            receipts.executeAudited(intent, correlation, audit) { connection ->
                issued = sessions.issue(connection, account.id, account.authEpoch, previous, id)
                request.requireActive()
                SafeResult(input.operationId)
            }
        return LoginResult(outcome, issued)
    }
}
