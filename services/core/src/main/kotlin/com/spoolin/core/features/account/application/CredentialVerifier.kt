package com.spoolin.core.features.account.application

import com.spoolin.core.features.account.persistence.AccountCredential
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.StoredPasswordHash

class CredentialRejected : IllegalStateException("Sign-in could not be completed")

class CredentialVerifier(
    private val accounts: ProtectedAccounts,
    private val hasher: PasswordHasher,
    private val rules: IdentityRules,
) {
    fun verify(
        email: String,
        password: String,
    ): AccountCredential {
        val identity = rules.normalizedEmail(email)
        val found = accounts.credential(identity)
        val credential = (found as? ProtectedRead.Value)?.value
        val result = hasher.verify(password, credential?.password ?: dummyHash)
        if (credential == null || !result.matched) throw CredentialRejected()
        result.replacement?.let { accounts.replacePassword(credential.id, credential.password, it) }
        return credential
    }

    companion object {
        private val dummyHash =
            StoredPasswordHash(
                "\$argon2id\$v=19\$m=65536,t=3,p=1\$MDEyMzQ1Njc4OWFiY2RlZg\$" +
                    "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY",
            )
    }
}
