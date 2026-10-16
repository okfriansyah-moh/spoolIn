package com.spoolin.core.features.account.application

import com.spoolin.core.boundary.graphql.AccountGraphql
import com.spoolin.core.boundary.http.BrowserIdentity
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.session.AnonymousCsrf
import com.spoolin.core.features.session.SessionCsrf
import com.spoolin.core.features.session.SessionStore
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.core.requireProtectedFile
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.nio.file.Files
import java.nio.file.Path
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.account-protection.enabled"], havingValue = "true")
class AuthenticationConfiguration {
    @Bean
    @ConditionalOnMissingBean(AuthenticationAdmission::class)
    @ConditionalOnProperty(name = ["spoolin.rate-limit.enabled"], havingValue = "false", matchIfMissing = true)
    fun authenticationAdmission(): AuthenticationAdmission = AuthenticationAdmission.unavailable

    @Bean
    fun sessionCsrf(environment: Environment): SessionCsrf = SessionCsrf(key(environment))

    @Bean
    fun anonymousCsrf(environment: Environment): AnonymousCsrf = AnonymousCsrf(key(environment))

    @Bean
    fun sessionStore(
        dataSource: DataSource,
        csrf: SessionCsrf,
    ): SessionStore = SessionStore(dataSource, csrf)

    @Bean
    fun browserIdentity(
        store: SessionStore,
        anonymous: AnonymousCsrf,
        csrf: SessionCsrf,
    ): BrowserIdentity = BrowserIdentity(store, anonymous, csrf)

    @Bean
    fun credentialVerifier(
        accounts: ProtectedAccounts,
        hasher: PasswordHasher,
        rules: IdentityRules,
    ): CredentialVerifier = CredentialVerifier(accounts, hasher, rules)

    @Bean
    fun registerAccount(
        accounts: ProtectedAccounts,
        hasher: PasswordHasher,
        rules: IdentityRules,
        receipts: OwnerReceipts,
        admission: AuthenticationAdmission,
    ): RegisterAccount = RegisterAccount(accounts, hasher, rules, receipts, admission)

    @Bean
    fun signInAccount(
        verifier: CredentialVerifier,
        store: SessionStore,
        receipts: OwnerReceipts,
        admission: AuthenticationAdmission,
    ): SignInAccount = SignInAccount(verifier, store, receipts, admission)

    @Bean
    fun signOutAccount(
        store: SessionStore,
        receipts: OwnerReceipts,
        admission: AuthenticationAdmission,
    ): SignOutAccount = SignOutAccount(store, receipts, admission)

    @Bean
    fun accountGraphql(
        accounts: ProtectedAccounts,
        register: RegisterAccount,
        signIn: SignInAccount,
        signOut: SignOutAccount,
    ): AccountGraphql = AccountGraphql(accounts, register, signIn, signOut)
}

private const val CSRF_KEY_BYTES = 32L

private fun key(environment: Environment): ByteArray {
    val path = Path.of(environment.getRequiredProperty("CSRF_KEY_FILE"))
    requireProtectedFile(path, CSRF_KEY_BYTES)
    return Files.readAllBytes(path).also { require(it.size.toLong() == CSRF_KEY_BYTES) }
}
