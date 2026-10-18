package com.spoolin.support.notifications

import java.net.URI
import javax.net.ssl.SSLContext

/** Protected endpoints are process configuration, never selected from queued payloads. */
object NotificationSettings {
    fun transport(read: (String) -> String?): NotificationTransport? =
        when (read("NOTIFICATION_MODE") ?: "database") {
            "database" -> null
            "slack" -> webhook("slack", secret(read, "NOTIFICATION_SLACK_WEBHOOK_FILE"))
            "google_chat" -> webhook("google_chat", secret(read, "NOTIFICATION_GOOGLE_CHAT_WEBHOOK_FILE"))
            "smtp" -> smtp(read)
            else -> invalid()
        }

    private fun webhook(
        mode: String,
        value: String,
    ): NotificationTransport {
        val uri =
            try {
                URI(value)
            } catch (_: Exception) {
                invalid()
            }
        val host = if (mode == "slack") "hooks.slack.com" else "chat.googleapis.com"
        requireSafe(validAuthority(uri, host))
        requireSafe(if (mode == "slack") validSlackPath(uri) else validGooglePath(uri))
        return WebhookTransport(mode, uri, setOf(host), SSLContext.getDefault())
    }

    private fun validAuthority(
        uri: URI,
        host: String,
    ): Boolean =
        uri.scheme == "https" && uri.host == host && uri.port in setOf(-1, HTTPS_PORT) &&
            uri.userInfo == null && uri.fragment == null

    private fun validSlackPath(uri: URI): Boolean =
        uri.rawQuery == null &&
            uri.path.matches(Regex("/services/[A-Za-z0-9_-]+/[A-Za-z0-9_-]+/[A-Za-z0-9_-]+"))

    private fun validGooglePath(uri: URI): Boolean =
        uri.path.matches(Regex("/v1/spaces/[A-Za-z0-9_-]+/messages")) &&
            uri.rawQuery
                ?.split('&')
                ?.map { it.substringBefore('=') }
                ?.sorted() == listOf("key", "token")

    private fun smtp(read: (String) -> String?): NotificationTransport {
        val host = read("NOTIFICATION_SMTP_HOST") ?: invalid()
        requireSafe(host.matches(Regex("[A-Za-z0-9][A-Za-z0-9.-]{0,252}")))
        val port = read("NOTIFICATION_SMTP_PORT")?.toIntOrNull() ?: SMTP_TLS_PORT
        requireSafe(port in 1..MAX_PORT)
        val tls = read("NOTIFICATION_SMTP_TLS") ?: "implicit"
        requireSafe(tls in setOf("implicit", "starttls"))
        val from = mailbox(read("NOTIFICATION_SMTP_FROM") ?: invalid())
        val to = mailbox(read("NOTIFICATION_SMTP_TO") ?: invalid())
        val user = read("NOTIFICATION_SMTP_USERNAME_FILE")?.let { secret(read, "NOTIFICATION_SMTP_USERNAME_FILE") }
        val password = read("NOTIFICATION_SMTP_PASSWORD_FILE")?.let { secret(read, "NOTIFICATION_SMTP_PASSWORD_FILE") }
        requireSafe((user == null) == (password == null))
        return SmtpTransport(
            SmtpEndpoint(host, port, tls, from, to),
            SmtpCredentials(user, password),
            SSLContext.getDefault(),
        )
    }

    private fun mailbox(value: String): String {
        requireSafe(
            value.length <= MAX_MAILBOX_LENGTH &&
                value.matches(Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+")),
        )
        return value
    }

    private fun secret(
        read: (String) -> String?,
        name: String,
    ): String = ProtectedNotificationSecret.read(read(name) ?: invalid())

    private fun requireSafe(valid: Boolean) {
        if (!valid) invalid()
    }

    private const val HTTPS_PORT = 443
    private const val SMTP_TLS_PORT = 465
    private const val MAX_PORT = 65535
    private const val MAX_MAILBOX_LENGTH = 254

    private fun invalid(): Nothing = throw IllegalArgumentException("Selected notification configuration is invalid")
}
