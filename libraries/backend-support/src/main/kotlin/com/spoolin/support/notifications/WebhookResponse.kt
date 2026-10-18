package com.spoolin.support.notifications

import com.spoolin.support.work.AttemptOutcome
import tools.jackson.databind.json.JsonMapper
import java.io.InputStream

internal object WebhookResponse {
    private val mapper = JsonMapper.builder().build()
    private const val RATE_LIMITED = 429
    private const val SUCCESS = 200
    private const val REJECTED_MIN = 300
    private const val REJECTED_MAX = 499
    private const val MAX_RETRY_SECONDS = 3600L
    private const val MILLIS_PER_SECOND = 1000
    private const val MAX_REFERENCE_LENGTH = 255
    private const val MAX_HEADERS_BYTES = 16384
    private const val MAX_BODY_BYTES = 8192
    private const val CHUNK_SIZE_RADIX = 16

    fun read(input: InputStream, item: NotificationClaim, mode: String): NotificationResult {
        val line = boundedLine(input)
        val status = line.split(' ').getOrNull(1)?.toIntOrNull() ?: return unknown()
        val headers = headers(input, line.length)
        return when {
            status == RATE_LIMITED -> rateLimited(headers)
            status in REJECTED_MIN..REJECTED_MAX ->
                NotificationResult(AttemptOutcome.PERMANENT, "NOTIFICATION_REJECTED")
            status == SUCCESS -> accepted(responseBody(input, headers), item, mode)
            else -> unknown()
        }
    }

    private fun rateLimited(headers: Map<String, String>): NotificationResult = NotificationResult(
        AttemptOutcome.TRANSIENT,
        "NOTIFICATION_RATE_LIMITED",
        retryAfterMs = (headers["retry-after"]?.toLongOrNull() ?: 1)
            .coerceIn(1, MAX_RETRY_SECONDS) * MILLIS_PER_SECOND,
    )

    private fun accepted(body: ByteArray, item: NotificationClaim, mode: String): NotificationResult {
        val reference = if (mode == "slack") slackReference(body, item) else googleReference(body)
        return if (reference == null) unknown() else NotificationResult(AttemptOutcome.SUCCEEDED, reference = reference)
    }

    private fun slackReference(body: ByteArray, item: NotificationClaim): String? =
        if (body.toString(Charsets.UTF_8).trim() == "ok") "slack:http-200:${item.work.workId}" else null

    private fun googleReference(body: ByteArray): String? =
        mapper.readTree(body).path("name").asString().takeIf {
            it.length <= MAX_REFERENCE_LENGTH && it.matches(Regex("spaces/[A-Za-z0-9_-]+/messages/[A-Za-z0-9_.~-]+"))
        }

    private fun headers(
        input: InputStream,
        firstLineSize: Int,
    ): Map<String, String> {
        var size = firstLineSize
        val headers = mutableMapOf<String, String>()
        while (true) {
            val next = boundedLine(input)
            size += next.length + 2
            require(size <= MAX_HEADERS_BYTES)
            if (next.isEmpty()) return headers
            val pair = next.split(':', limit = 2)
            require(pair.size == 2)
            val key = pair[0].lowercase()
            require(key !in headers)
            headers[key] = pair[1].trim()
        }
    }

    private fun responseBody(
        input: InputStream,
        headers: Map<String, String>,
    ): ByteArray {
        if (headers["transfer-encoding"]?.lowercase() == "chunked") return chunks(input)
        require(headers["transfer-encoding"] == null)
        val length = headers["content-length"]?.toInt()?.also { require(it in 0..MAX_BODY_BYTES) }
        val bytes = input.readNBytes(length ?: MAX_BODY_BYTES + 1)
        require(bytes.size <= MAX_BODY_BYTES && (length == null || bytes.size == length))
        return bytes
    }

    private fun chunks(input: InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        while (true) {
            val size = boundedLine(input).substringBefore(';').toInt(CHUNK_SIZE_RADIX)
            require(size >= 0 && size <= MAX_BODY_BYTES - output.size())
            if (size == 0) {
                require(boundedLine(input).isEmpty())
                return output.toByteArray()
            }
            val bytes = input.readNBytes(size)
            require(bytes.size == size)
            output.write(bytes)
            require(boundedLine(input).isEmpty())
        }
    }

    private fun unknown() = NotificationResult(AttemptOutcome.UNCERTAIN, "NOTIFICATION_UNCERTAIN")
}
