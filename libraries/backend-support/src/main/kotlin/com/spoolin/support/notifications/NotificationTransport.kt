package com.spoolin.support.notifications

import com.spoolin.support.work.AttemptOutcome
import tools.jackson.databind.json.JsonMapper
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLContext

/** One socket and one send. No redirects, authentication challenges or automatic connection retries. */
class WebhookTransport(
    override val mode: String,
    private val endpoint: URI,
    allowedHosts: Set<String>,
    private val ssl: SSLContext,
) : NotificationTransport {
    init {
        require(mode in setOf("slack", "google_chat"))
        require(
            endpoint.scheme == "https" && endpoint.host in allowedHosts && endpoint.userInfo == null &&
                endpoint.fragment == null,
        )
    }

    private val mapper = JsonMapper.builder().build()

    override fun send(
        item: NotificationClaim,
        timeoutMs: Long,
    ): NotificationResult {
        val body =
            try {
                mapper.writeValueAsBytes(mapOf("text" to notificationText(item)))
            } catch (
                _: Exception,
            ) {
                return NotificationResult(AttemptOutcome.PERMANENT, "NOTIFICATION_INVALID_SUMMARY")
            }
        var attempted = false
        val port = if (endpoint.port < 0) HTTPS_PORT else endpoint.port
        return try {
            TimedConnection(endpoint.host, port, timeoutMs).use { connection ->
                val socket = connection.tls(ssl)
                val header = requestHeader(body.size)
                attempted = true
                socket.outputStream.write(header.toByteArray(StandardCharsets.US_ASCII))
                socket.outputStream.write(body)
                socket.outputStream.flush()
                WebhookResponse.read(BufferedInputStream(socket.inputStream), item, mode)
            }
        } catch (_: Exception) {
            NotificationResult(
                if (attempted) AttemptOutcome.UNCERTAIN else AttemptOutcome.TRANSIENT,
                if (attempted) "NOTIFICATION_UNCERTAIN" else "NOTIFICATION_CONNECT_FAILED",
            )
        }
    }

    private fun requestHeader(bodySize: Int): String {
        val path = endpoint.rawPath + (endpoint.rawQuery?.let { "?$it" } ?: "")
        val host = endpoint.host + if (endpoint.port < 0 || endpoint.port == HTTPS_PORT) "" else ":${endpoint.port}"
        return "POST $path HTTP/1.1\r\nHost: $host\r\nContent-Type: application/json\r\n" +
            "Content-Length: $bodySize\r\nConnection: close\r\n\r\n"
    }

    private companion object {
        const val HTTPS_PORT = 443
    }
}

internal fun notificationText(item: NotificationClaim): String {
    val node = JsonMapper.builder().build().readTree(item.summary)
    require(node.isObject)
    val keys =
        setOf(
            "environment",
            "service",
            "operationId",
            "workId",
            "class",
            "attemptSummary",
            "runbook",
            "severity",
            "alertname",
            "summary",
            "description",
            "status",
            "startsAt",
            "endsAt",
        )
    val values =
        node.propertyNames().filter { it in keys }.sorted().map { key ->
            val value = node.get(key)
            require(
                value.isString && value.asString().length <= MAX_SUMMARY_FIELD_LENGTH &&
                    value.asString().none(Char::isISOControl),
            )
            "$key: ${value.asString()}"
        }
    val context =
        listOfNotNull(
            "notificationWorkId: ${item.work.workId}",
            item.work.operationId?.let {
                "operationId: $it"
            },
            "attemptSummary: ${item.work.attemptCount} of ${item.work.maxAttempts}",
        )
    return (context + values).joinToString("\n")
}

internal fun boundedLine(input: InputStream): String {
    val output = java.io.ByteArrayOutputStream()
    while (output.size() <= MAX_LINE_BYTES) {
        val byte = input.read()
        require(byte >= 0)
        if (byte == LINE_FEED) {
            val bytes = output.toByteArray()
            require(bytes.isNotEmpty() && bytes.last() == CARRIAGE_RETURN.toByte())
            return String(bytes, 0, bytes.size - 1, StandardCharsets.US_ASCII)
        }
        output.write(byte)
    }
    throw IllegalArgumentException("Provider response exceeded bounds")
}

private const val MAX_SUMMARY_FIELD_LENGTH = 1024
private const val MAX_LINE_BYTES = 8192
private const val LINE_FEED = 10
private const val CARRIAGE_RETURN = 13
