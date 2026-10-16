package com.spoolin.core.features.account.application

import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.session.AnonymousSubmission
import com.spoolin.core.operations.events.ReceiptAction
import com.spoolin.core.operations.events.ReceiptAudit
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.RegistrationIntent
import com.spoolin.support.receipts.SafeResult
import java.util.UUID

class RegistrationInput(
    val operationId: UUID,
    val name: String,
    val email: String,
    val password: String,
) {
    override fun toString(): String = "RegistrationInput(content=redacted)"
}

class RegisterAccount(
    private val accounts: ProtectedAccounts,
    private val hasher: PasswordHasher,
    private val rules: IdentityRules,
    private val receipts: OwnerReceipts,
    private val admission: AuthenticationAdmission = AuthenticationAdmission.unavailable,
) {
    fun register(
        input: RegistrationInput,
        submission: AnonymousSubmission,
        correlation: UUID,
        request: AuthenticationRequest = AuthenticationRequest.unavailable,
    ): ReceiptOutcome {
        request.requireActive()
        admission.check(AuthenticationAction.REGISTER, null, request)
        request.requireActive()
        val email = rules.normalizedEmail(input.email)
        val intent =
            RegistrationIntent.create(
                input.operationId,
                submission.scope,
                "registration-v1",
                email,
                submission.nonce,
            )
        val existing = receipts.resolve(intent)
        if (existing != null) return existing
        val identity = rules.identity(input.name, input.email)
        val hash = hasher.hashNew(input.password)
        request.requireActive()
        val id = UUID.randomUUID()
        request.requireActive()
        val prepared = accounts.prepare(id, identity, hash, AccountRole.VIEWER)
        val audit = ReceiptAudit(ReceiptAction.ACCOUNT_REGISTERED, subjectId = id)
        return receipts.executeAudited(intent, correlation, audit) {
            accounts.insert(it, prepared)
            request.requireActive()
            SafeResult(input.operationId)
        }
    }
}
