package com.spoolin.support.notifications

import com.spoolin.support.work.AttemptOutcome
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.net.ssl.SSLContext

/** Fixed sender and recipient; TLS is mandatory before authentication or message content. */
class SmtpTransport(
    private val endpoint: SmtpEndpoint,
    private val credentials: SmtpCredentials,
    private val ssl: SSLContext,
) : NotificationTransport {
    override val mode = "smtp"

    override fun send(
        item: NotificationClaim,
        timeoutMs: Long,
    ): NotificationResult {
        val body =
            try {
                message(item)
            } catch (_: Exception) {
                return NotificationResult(AttemptOutcome.PERMANENT, "NOTIFICATION_INVALID_SUMMARY")
            }
        var dataStarted = false
        return try {
            TimedConnection(endpoint.host, endpoint.port, timeoutMs).use { connection ->
                val session = SmtpSession.open(connection, endpoint.tlsMode, ssl)
                authenticate(session.input, session.output)
                session.envelope(endpoint.from, endpoint.to)
                dataStarted = true
                session.output.write(body)
                session.output.flush()
                session.expect(SmtpSession.OK)
                NotificationResult(
                    AttemptOutcome.SUCCEEDED,
                    reference = "smtp:${item.work.workId}.${item.work.generation}@spoolin.local",
                )
            }
        } catch (failure: SmtpRejection) {
            NotificationResult(
                if (failure.temporary) AttemptOutcome.TRANSIENT else AttemptOutcome.PERMANENT,
                if (failure.temporary) "NOTIFICATION_SMTP_TEMPORARY" else "NOTIFICATION_SMTP_REJECTED",
            )
        } catch (_: Exception) {
            NotificationResult(
                if (dataStarted) AttemptOutcome.UNCERTAIN else AttemptOutcome.TRANSIENT,
                if (dataStarted) "NOTIFICATION_UNCERTAIN" else "NOTIFICATION_CONNECT_FAILED",
            )
        }
    }

    private fun authenticate(
        input: InputStream,
        output: OutputStream,
    ) {
        val username = credentials.username
        val password = credentials.password
        if (username == null || password == null) return
        val encoded = "\u0000$username\u0000$password".toByteArray(Charsets.UTF_8)
        try {
            SmtpSession.command(output, "AUTH PLAIN " + Base64.getEncoder().encodeToString(encoded))
            SmtpSession(input, output).expect(SmtpSession.AUTHENTICATED)
        } finally {
            encoded.fill(0)
        }
    }

    private fun message(item: NotificationClaim): ByteArray {
        val text =
            Base64
                .getMimeEncoder(
                    MIME_LINE_LENGTH,
                    "\r\n".toByteArray(),
                ).encodeToString(notificationText(item).toByteArray(Charsets.UTF_8))
        return (
            "From: <${endpoint.from}>\r\nTo: <${endpoint.to}>\r\nSubject: Spoolin operational notification\r\n" +
                "Date: ${java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(
                    java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC),
                )}\r\n" +
                "Message-ID: <${item.work.workId}.${item.work.generation}@spoolin.local>\r\n" +
                "MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Transfer-Encoding: base64\r\n\r\n" +
                text + "\r\n.\r\n"
        ).toByteArray(StandardCharsets.US_ASCII)
    }

    private companion object {
        const val MIME_LINE_LENGTH = 76
    }
}
