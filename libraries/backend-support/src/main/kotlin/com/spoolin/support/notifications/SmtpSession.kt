package com.spoolin.support.notifications

import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLContext

internal class SmtpSession(
    val input: InputStream,
    val output: OutputStream,
) {
    fun envelope(
        from: String,
        to: String,
    ) {
        command(output, "MAIL FROM:<$from>")
        expect(OK)
        command(output, "RCPT TO:<$to>")
        expect(OK)
        command(output, "DATA")
        expect(DATA_READY)
    }

    fun expect(expected: Int) {
        var line = boundedLine(input)
        val code = line.take(CODE_LENGTH).toIntOrNull() ?: throw SmtpRejection(false)
        var size = line.length
        while (line.getOrNull(CODE_LENGTH) == '-') {
            line = boundedLine(input)
            size += line.length
            require(size <= MAX_REPLY_BYTES && line.take(CODE_LENGTH).toIntOrNull() == code)
        }
        if (code != expected) throw SmtpRejection(code in TEMPORARY_MIN..TEMPORARY_MAX)
    }

    private fun hello() {
        command(output, "EHLO spoolin.local")
        expect(OK)
    }

    companion object {
        const val OK = 250
        const val AUTHENTICATED = 235
        private const val READY = 220
        private const val DATA_READY = 354
        private const val CODE_LENGTH = 3
        private const val MAX_REPLY_BYTES = 16384
        private const val TEMPORARY_MIN = 400
        private const val TEMPORARY_MAX = 499

        fun open(
            connection: TimedConnection,
            tlsMode: String,
            ssl: SSLContext,
        ): SmtpSession {
            val socket = if (tlsMode == "implicit") connection.tls(ssl) else connection.raw
            val initial = SmtpSession(BufferedInputStream(socket.inputStream), socket.outputStream)
            initial.expect(READY)
            initial.hello()
            return if (tlsMode == "starttls") initial.upgrade(connection, ssl) else initial
        }

        fun command(
            output: OutputStream,
            value: String,
        ) {
            output.write((value + "\r\n").toByteArray(StandardCharsets.US_ASCII))
            output.flush()
        }
    }

    private fun upgrade(
        connection: TimedConnection,
        ssl: SSLContext,
    ): SmtpSession {
        command(output, "STARTTLS")
        expect(READY)
        val secured = connection.tls(ssl)
        return SmtpSession(BufferedInputStream(secured.inputStream), secured.outputStream).also { it.hello() }
    }
}

internal class SmtpRejection(
    val temporary: Boolean,
) : RuntimeException("SMTP request rejected")
