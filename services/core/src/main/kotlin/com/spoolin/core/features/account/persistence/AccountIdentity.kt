package com.spoolin.core.features.account.persistence

import com.spoolin.support.policy.TextBound
import com.spoolin.support.policy.TextRules
import java.util.Locale

class AccountIdentity internal constructor(
    val name: String,
    val emailDisplay: String,
    val emailNormalized: String,
)

class IdentityRules(
    private val minimumName: Int = MIN_ACCOUNT_NAME,
    private val maximumName: Int = MAX_ACCOUNT_NAME,
) {
    private val idna = Idna2008()
    private val localSyntax = Regex("[a-zA-Z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#\$%&'*+/=?^_`{|}~-]+)*")

    fun identity(
        name: String,
        email: String,
    ): AccountIdentity {
        val trimmed = TextRules.ordinary(name, TextBound(minimumName, maximumName), "name")
        require(trimmed.none { it.isISOControl() }) { "Account name is invalid" }
        return AccountIdentity(trimmed, email.trim(), normalizedEmail(email))
    }

    fun normalizedEmail(value: String): String {
        require(value.none { it.isISOControl() }) { "Email is invalid" }
        val email = value.trim()
        require(
            email.codePointCount(0, email.length) in MIN_EMAIL_CHARACTERS..MAX_EMAIL_CHARACTERS && email.count {
                it == '@'
            } == 1,
        ) { "Email is invalid" }
        val local = email.substringBefore('@')
        require(local.length in 1..MAX_LOCAL_OCTETS && localSyntax.matches(local)) { "Email is invalid" }
        val ascii = idna.ascii(email.substringAfter('@'))
        val result = local.lowercase(Locale.ROOT) + "@" + ascii
        require(result.length <= MAX_EMAIL_CHARACTERS) { "Email is invalid" }
        return result
    }
}

private const val MIN_ACCOUNT_NAME = 2
private const val MAX_ACCOUNT_NAME = 100
private const val MIN_EMAIL_CHARACTERS = 3
private const val MAX_EMAIL_CHARACTERS = 254
private const val MAX_LOCAL_OCTETS = 64
