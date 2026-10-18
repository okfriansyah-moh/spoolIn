package com.spoolin.support.notifications

data class SmtpEndpoint(
    val host: String,
    val port: Int,
    val tlsMode: String,
    val from: String,
    val to: String,
) {
    init {
        require(tlsMode in setOf("implicit", "starttls") && port in 1..MAX_PORT)
        require(host.matches(Regex("[A-Za-z0-9][A-Za-z0-9.-]{0,252}")))
        require(listOf(from, to).all {
            it.length <= MAX_MAILBOX_LENGTH && it.matches(Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+"))
        })
    }

    override fun toString(): String = "SmtpEndpoint(redacted)"

    private companion object {
        const val MAX_PORT = 65535
        const val MAX_MAILBOX_LENGTH = 254
    }
}

class SmtpCredentials(val username: String?, val password: String?) {
    init {
        require((username == null) == (password == null))
        require(listOfNotNull(username, password).all { it.isNotEmpty() && it.none(Char::isISOControl) })
    }

    override fun toString(): String = "SmtpCredentials(redacted)"
}
