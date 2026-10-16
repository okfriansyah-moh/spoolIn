package com.spoolin.core.boundary.graphql

import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.account.application.LoginInput
import com.spoolin.core.features.account.application.RegisterAccount
import com.spoolin.core.features.account.application.RegistrationInput
import com.spoolin.core.features.account.application.SignInAccount
import com.spoolin.core.features.account.application.SignOutAccount
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionUnavailable
import com.spoolin.core.features.session.SessionCredential
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.ReceiptState
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import java.util.UUID

class AccountGraphql(
    private val accounts: ProtectedAccounts,
    private val registration: RegisterAccount,
    private val login: SignInAccount,
    private val logout: SignOutAccount,
) {
    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { it.dataFetcher("session") { env -> session(context(env)) } }
        wiring.type("Mutation") { mutation ->
            mutation
                .dataFetcher("register", ::register)
                .dataFetcher("signIn", ::signIn)
                .dataFetcher("signOut", ::signOut)
        }
    }

    private fun register(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        if (context.principal != null) throw CredentialRejected()
        val input = requireNotNull(env.getArgument<Map<String, Any?>>("input"))
        val command = RegistrationInput(id(env), text(input, "name"), text(input, "email"), text(input, "password"))
        val submission = context.anonymous ?: throw CredentialRejected()
        return mutation(registration.register(command, submission, context.correlation, context.request))
    }

    private fun signIn(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val submission = context.anonymous ?: throw CredentialRejected()
        val email = requireNotNull(env.getArgument<String>("email"))
        val password = requireNotNull(env.getArgument<String>("password"))
        val input = LoginInput(id(env), email, password)
        val result = login.signIn(input, submission, context.credential, context.correlation, context.request)
        result.issued?.let { context.issue(it.cookie()) }
        return mutation(result.outcome)
    }

    private fun signOut(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val credential = context.credential ?: throw CredentialRejected()
        val csrf = context.csrf ?: throw CredentialRejected()
        val result = logout.signOut(id(env), credential, csrf, context.correlation, context.request)
        if (result.state == ReceiptState.SUCCEEDED) context.issue(SessionCredential.deletion())
        return mutation(result)
    }

    private fun session(context: BrowserContext): Map<String, Any?> {
        val principal = context.principal
        if (principal == null) {
            return mapOf("authenticated" to false, "csrfToken" to requireNotNull(context.anonymous).csrfToken)
        }
        val protected = accounts.identity(principal.accountId)
        val identity = (protected as? ProtectedRead.Value)?.value ?: throw ProtectionUnavailable()
        return mapOf(
            "authenticated" to true,
            "role" to principal.role.name,
            "accountId" to principal.accountId.toString(),
            "displayName" to identity.name,
            "expiresAt" to principal.expiresAt.toString(),
            "csrfToken" to principal.csrfToken,
        )
    }

    private fun mutation(outcome: ReceiptOutcome): Map<String, Any?> =
        mapOf(
            "operation" to
                mapOf(
                    "id" to outcome.id.toString(),
                    "state" to outcome.state.name,
                    "resultId" to outcome.result?.resultId?.toString(),
                    "resultExpired" to outcome.resultExpired,
                ),
            "problem" to null,
        )

    private fun context(env: DataFetchingEnvironment): BrowserContext {
        val browser = env.graphQlContext.get<BrowserContext>("browser")
        return requireNotNull(browser)
    }

    private fun id(env: DataFetchingEnvironment): UUID = UUID.fromString(env.getArgument<String>("operationId"))

    private fun text(
        input: Map<String, Any?>,
        name: String,
    ): String = requireNotNull(input[name] as? String)
}
