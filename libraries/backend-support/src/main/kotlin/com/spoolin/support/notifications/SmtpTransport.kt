package com.spoolin.support.notifications

import com.spoolin.support.work.AttemptOutcome
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.net.ssl.SSLContext

/** Fixed sender and recipient; TLS is mandatory before authentication or message content. */
class SmtpTransport(private val host: String, private val port: Int, private val tlsMode: String,
    private val from: String, private val to: String, private val username: String?, private val password: String?,
    private val ssl: SSLContext) : NotificationTransport {
    init {
        require(tlsMode in setOf("implicit", "starttls") && port in 1..65535)
        require(host.matches(Regex("[A-Za-z0-9][A-Za-z0-9.-]{0,252}")))
        require(listOf(from, to).all { it.length <= 254 && it.matches(Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+")) })
        require((username == null) == (password == null))
        require(listOfNotNull(username, password).all { it.isNotEmpty() && it.none(Char::isISOControl) })
    }
    override val mode = "smtp"

    override fun send(item: NotificationClaim, timeoutMs: Long): NotificationResult {
        val body = try { message(item) } catch (_: IllegalArgumentException) {
            return NotificationResult(AttemptOutcome.PERMANENT, "NOTIFICATION_INVALID_SUMMARY")
        }
        var dataStarted = false
        return try {
            TimedConnection(host, port, timeoutMs).use { connection ->
                val socket = if (tlsMode == "implicit") connection.tls(ssl) else connection.raw
                var input: InputStream = BufferedInputStream(socket.inputStream)
                var output = socket.outputStream
                expect(input, 220)
                command(output, "EHLO spoolin.local"); expect(input, 250)
                if (tlsMode == "starttls") {
                    command(output, "STARTTLS"); expect(input, 220)
                    val secured = connection.tls(ssl)
                    input = BufferedInputStream(secured.inputStream); output = secured.outputStream
                    command(output, "EHLO spoolin.local"); expect(input, 250)
                }
                authenticate(input, output)
                command(output, "MAIL FROM:<$from>"); expect(input, 250)
                command(output, "RCPT TO:<$to>"); expect(input, 250)
                command(output, "DATA"); expect(input, 354)
                dataStarted = true
                output.write(body); output.flush()
                expect(input, 250)
                NotificationResult(AttemptOutcome.SUCCEEDED, reference = "smtp:${item.work.workId}.${item.work.generation}@spoolin.local")
            }
        } catch (failure: SmtpRejection) {
            NotificationResult(if (failure.temporary) AttemptOutcome.TRANSIENT else AttemptOutcome.PERMANENT,
                if (failure.temporary) "NOTIFICATION_SMTP_TEMPORARY" else "NOTIFICATION_SMTP_REJECTED")
        } catch (_: Exception) {
            NotificationResult(if (dataStarted) AttemptOutcome.UNCERTAIN else AttemptOutcome.TRANSIENT,
                if (dataStarted) "NOTIFICATION_UNCERTAIN" else "NOTIFICATION_CONNECT_FAILED")
        }
    }

    private fun authenticate(input: InputStream, output: OutputStream) {
        if (username == null || password == null) return
        val credentials = "\u0000$username\u0000$password".toByteArray(Charsets.UTF_8)
        try { command(output, "AUTH PLAIN " + Base64.getEncoder().encodeToString(credentials)); expect(input, 235) }
        finally { credentials.fill(0) }
    }

    private fun message(item: NotificationClaim): ByteArray {
        val text = Base64.getMimeEncoder(76, "\r\n".toByteArray()).encodeToString(notificationText(item).toByteArray(Charsets.UTF_8))
        return ("From: <$from>\r\nTo: <$to>\r\nSubject: Spoolin operational notification\r\n" +
            "Message-ID: <${item.work.workId}.${item.work.generation}@spoolin.local>\r\n" +
            "MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n" +
            text + "\r\n.\r\n").toByteArray(StandardCharsets.US_ASCII)
    }

    private fun command(output: OutputStream, value: String) {
        output.write((value + "\r\n").toByteArray(StandardCharsets.US_ASCII)); output.flush()
    }

    private fun expect(input: InputStream, expected: Int) {
        var line = boundedLine(input)
        val code = line.take(3).toIntOrNull() ?: throw SmtpRejection(false)
        var size = line.length
        while (line.getOrNull(3) == '-') {
            line = boundedLine(input); size += line.length
            require(size <= 16384 && line.take(3).toIntOrNull() == code)
        }
        if (code != expected) throw SmtpRejection(code in 400..499)
    }
}

private class SmtpRejection(val temporary: Boolean) : RuntimeException("SMTP request rejected")
