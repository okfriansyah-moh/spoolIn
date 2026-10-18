package com.spoolin.support.notifications

import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import javax.net.ssl.SSLContext

/** Protected endpoints are process configuration, never selected from queued payloads. */
object NotificationSettings {
    fun transport(read: (String) -> String?): NotificationTransport? = when (read("NOTIFICATION_MODE") ?: "database") {
        "database" -> null
        "slack" -> webhook("slack", secret(read, "NOTIFICATION_SLACK_WEBHOOK_FILE"))
        "google_chat" -> webhook("google_chat", secret(read, "NOTIFICATION_GOOGLE_CHAT_WEBHOOK_FILE"))
        "smtp" -> smtp(read)
        else -> invalid()
    }

    private fun webhook(mode: String, value: String): NotificationTransport {
        val uri = try { URI(value) } catch (_: Exception) { invalid() }
        val host = if (mode == "slack") "hooks.slack.com" else "chat.googleapis.com"
        requireSafe(uri.scheme == "https" && uri.host == host && uri.port in setOf(-1, 443) &&
            uri.userInfo == null && uri.fragment == null)
        if (mode == "slack") requireSafe(uri.rawQuery == null &&
            uri.path.matches(Regex("/services/[A-Za-z0-9_-]+/[A-Za-z0-9_-]+/[A-Za-z0-9_-]+")))
        else requireSafe(uri.path.matches(Regex("/v1/spaces/[A-Za-z0-9_-]+/messages")) &&
            uri.rawQuery?.split('&')?.map { it.substringBefore('=') }?.sorted() == listOf("key", "token"))
        return WebhookTransport(mode, uri, setOf(host), SSLContext.getDefault())
    }

    private fun smtp(read: (String) -> String?): NotificationTransport {
        val host = read("NOTIFICATION_SMTP_HOST") ?: invalid()
        requireSafe(host.matches(Regex("[A-Za-z0-9][A-Za-z0-9.-]{0,252}")))
        val port = read("NOTIFICATION_SMTP_PORT")?.toIntOrNull() ?: 465
        requireSafe(port in 1..65535)
        val tls = read("NOTIFICATION_SMTP_TLS") ?: "implicit"
        requireSafe(tls in setOf("implicit", "starttls"))
        val from = mailbox(read("NOTIFICATION_SMTP_FROM") ?: invalid())
        val to = mailbox(read("NOTIFICATION_SMTP_TO") ?: invalid())
        val user = read("NOTIFICATION_SMTP_USERNAME_FILE")?.let { secret(read, "NOTIFICATION_SMTP_USERNAME_FILE") }
        val password = read("NOTIFICATION_SMTP_PASSWORD_FILE")?.let { secret(read, "NOTIFICATION_SMTP_PASSWORD_FILE") }
        requireSafe((user == null) == (password == null))
        return SmtpTransport(host, port, tls, from, to, user, password, SSLContext.getDefault())
    }

    private fun mailbox(value: String): String {
        requireSafe(value.length <= 254 && value.matches(Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+")))
        return value
    }

    private fun secret(read: (String) -> String?, name: String): String = try {
        val path = Path.of(read(name) ?: invalid())
        requireSafe(Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.size(path) in 1..4096 &&
            Files.getPosixFilePermissions(path, NOFOLLOW_LINKS) == PosixFilePermissions.fromString("rw-------"))
        Files.newByteChannel(path, setOf<java.nio.file.OpenOption>(java.nio.file.StandardOpenOption.READ, NOFOLLOW_LINKS)).use { channel ->
            val buffer = java.nio.ByteBuffer.allocate(4097)
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) { /* bounded protected file */ }
            requireSafe(buffer.position() in 1..4096)
            buffer.flip()
            val value = Charsets.UTF_8.newDecoder().decode(buffer).toString().trimEnd('\n', '\r')
            requireSafe(value.isNotEmpty() && value.none(Char::isISOControl))
            value
        }
    } catch (_: Exception) { invalid() }

    private fun requireSafe(valid: Boolean) { if (!valid) invalid() }
    private fun invalid(): Nothing = throw IllegalArgumentException("Selected notification configuration is invalid")
}
