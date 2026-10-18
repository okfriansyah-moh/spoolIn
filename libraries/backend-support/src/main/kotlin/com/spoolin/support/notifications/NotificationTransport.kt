package com.spoolin.support.notifications

import com.spoolin.support.work.AttemptOutcome
import tools.jackson.databind.json.JsonMapper
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/** One socket and one send. No redirects, authentication challenges or automatic connection retries. */
class WebhookTransport(override val mode: String, private val endpoint: URI, allowedHosts: Set<String>,
    private val ssl: SSLContext) : NotificationTransport {
    init {
        require(mode in setOf("slack", "google_chat"))
        require(endpoint.scheme == "https" && endpoint.host in allowedHosts && endpoint.userInfo == null && endpoint.fragment == null)
    }
    private val mapper = JsonMapper.builder().build()

    override fun send(item: NotificationClaim, timeoutMs: Long): NotificationResult {
        val body = try { mapper.writeValueAsBytes(mapOf("text" to notificationText(item))) }
            catch (_: Exception) { return NotificationResult(AttemptOutcome.PERMANENT, "NOTIFICATION_INVALID_SUMMARY") }
        var attempted = false
        return try {
            TimedConnection(endpoint.host, if (endpoint.port < 0) 443 else endpoint.port, timeoutMs).use { connection ->
                val socket = connection.tls(ssl)
                val path = endpoint.rawPath + (endpoint.rawQuery?.let { "?$it" } ?: "")
                val host = endpoint.host + if (endpoint.port < 0 || endpoint.port == 443) "" else ":${endpoint.port}"
                val header = "POST $path HTTP/1.1\r\nHost: $host\r\nContent-Type: application/json\r\n" +
                    "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                attempted = true
                socket.outputStream.write(header.toByteArray(StandardCharsets.US_ASCII))
                socket.outputStream.write(body); socket.outputStream.flush()
                response(BufferedInputStream(socket.inputStream), item)
            }
        } catch (_: Exception) {
            NotificationResult(if (attempted) AttemptOutcome.UNCERTAIN else AttemptOutcome.TRANSIENT,
                if (attempted) "NOTIFICATION_UNCERTAIN" else "NOTIFICATION_CONNECT_FAILED")
        }
    }

    private fun response(input: InputStream, item: NotificationClaim): NotificationResult {
        val line = boundedLine(input)
        val status = line.split(' ').getOrNull(1)?.toIntOrNull() ?: return unknown()
        val headers = headers(input, line.length)
        if (status == 429) return NotificationResult(AttemptOutcome.TRANSIENT, "NOTIFICATION_RATE_LIMITED",
            retryAfterMs = (headers["retry-after"]?.toLongOrNull() ?: 1).coerceIn(1, 3600) * 1000)
        if (status in 400..499 || status in 300..399) return NotificationResult(AttemptOutcome.PERMANENT, "NOTIFICATION_REJECTED")
        if (status != 200) return unknown()
        val body = responseBody(input, headers)
        val reference = if (mode == "slack") {
            if (body.toString(Charsets.UTF_8).trim() != "ok") return unknown()
            "slack:http-200:${item.work.workId}"
        } else mapper.readTree(body).path("name").asString().takeIf {
            it.length <= 255 && it.matches(Regex("spaces/[A-Za-z0-9_-]+/messages/[A-Za-z0-9_.~-]+"))
        } ?: return unknown()
        return NotificationResult(AttemptOutcome.SUCCEEDED, reference = reference)
    }

    private fun headers(input: InputStream, firstLineSize: Int): Map<String, String> {
        var size = firstLineSize
        val headers = mutableMapOf<String, String>()
        while (true) {
            val next = boundedLine(input); size += next.length + 2
            require(size <= 16384)
            if (next.isEmpty()) return headers
            val pair = next.split(':', limit = 2); require(pair.size == 2)
            val key = pair[0].lowercase(); require(key !in headers)
            headers[key] = pair[1].trim()
        }
    }

    private fun responseBody(input: InputStream, headers: Map<String, String>): ByteArray {
        if (headers["transfer-encoding"]?.lowercase() == "chunked") return chunks(input)
        require(headers["transfer-encoding"] == null)
        val length = headers["content-length"]?.toInt()?.also { require(it in 0..8192) }
        val bytes = input.readNBytes(length ?: 8193)
        require(bytes.size <= 8192 && (length == null || bytes.size == length))
        return bytes
    }
    private fun chunks(input: InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        while (true) {
            val size = boundedLine(input).substringBefore(';').toInt(16)
            require(size >= 0 && size <= 8192 - output.size())
            if (size == 0) { require(boundedLine(input).isEmpty()); return output.toByteArray() }
            val bytes = input.readNBytes(size); require(bytes.size == size)
            output.write(bytes); require(boundedLine(input).isEmpty())
        }
    }
    private fun unknown() = NotificationResult(AttemptOutcome.UNCERTAIN, "NOTIFICATION_UNCERTAIN")
}

internal fun notificationText(item: NotificationClaim): String {
    val node = JsonMapper.builder().build().readTree(item.summary)
    require(node.isObject)
    val keys = setOf("environment", "service", "operationId", "workId", "class", "attemptSummary", "runbook",
        "severity", "alertname", "summary", "description", "status", "startsAt", "endsAt")
    val values = node.propertyNames().filter { it in keys }.sorted().map { key ->
        val value = node.get(key); require(value.isString && value.asString().length <= 1024 && value.asString().none(Char::isISOControl))
        "$key: ${value.asString()}"
    }
    val context = listOfNotNull("notificationWorkId: ${item.work.workId}",
        item.work.operationId?.let { "operationId: $it" }, "attemptSummary: ${item.work.attemptCount} of ${item.work.maxAttempts}")
    return (context + values).joinToString("\n")
}

internal fun boundedLine(input: InputStream): String {
    val output = java.io.ByteArrayOutputStream()
    while (output.size() <= 8192) {
        val byte = input.read(); require(byte >= 0)
        if (byte == 10) {
            val bytes = output.toByteArray(); require(bytes.isNotEmpty() && bytes.last() == 13.toByte())
            return String(bytes, 0, bytes.size - 1, StandardCharsets.US_ASCII)
        }
        output.write(byte)
    }
    throw IllegalArgumentException("Provider response exceeded bounds")
}

internal class TimedConnection(host: String, port: Int, timeoutMs: Long) : AutoCloseable {
    val raw = Socket()
    private val watchdog = timer.schedule({ try { raw.close() } catch (_: Exception) { } }, timeoutMs, TimeUnit.MILLISECONDS)
    private val hostName = host
    init {
        try { val address = resolver.submit<java.net.InetAddress> { java.net.InetAddress.getAllByName(host).first() }
            val resolved = try { address.get(timeoutMs.coerceIn(1, 3000), TimeUnit.MILLISECONDS) }
                finally { address.cancel(true) }
            raw.connect(InetSocketAddress(resolved, port), timeoutMs.coerceAtMost(3000).coerceAtLeast(1).toInt())
            raw.soTimeout = timeoutMs.coerceIn(1, 14000).toInt()
        } catch (failure: Exception) { close(); throw failure }
    }
    fun tls(context: SSLContext): SSLSocket = (context.socketFactory.createSocket(raw, hostName, raw.port, true) as SSLSocket).also {
        it.sslParameters = it.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
        it.startHandshake()
    }
    override fun close() { watchdog.cancel(false); raw.close() }
    companion object {
        private val resolver = java.util.concurrent.ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
            java.util.concurrent.ArrayBlockingQueue(2), java.util.concurrent.ThreadFactory { task ->
                Thread(task, "notification-dns").apply { isDaemon = true }
            }, java.util.concurrent.ThreadPoolExecutor.AbortPolicy())
        private val timer = ScheduledThreadPoolExecutor(1) { task -> Thread(task, "notification-timeout").apply { isDaemon = true } }
            .apply { removeOnCancelPolicy = true }
    }
}
