package com.spoolin.core.boundary.http

import org.springframework.http.ResponseEntity

private const val RATE_LIMITED_STATUS = 429
private const val MILLISECONDS = 1000L

object BrowserResponses {
    fun retrySeconds(milliseconds: Long): String {
        val rounded = (milliseconds + MILLISECONDS - 1) / MILLISECONDS
        return rounded.coerceAtLeast(1).toString()
    }

    fun limited(milliseconds: Long): ResponseEntity<Map<String, Any?>> =
        ResponseEntity
            .status(RATE_LIMITED_STATUS)
            .header("Retry-After", retrySeconds(milliseconds))
            .body(
                mapOf(
                    "data" to null,
                    "errors" to
                        listOf(
                            mapOf(
                                "message" to "The request limit has been reached.",
                                "extensions" to mapOf("code" to "RATE_LIMITED", "retryAfterMs" to milliseconds),
                            ),
                        ),
                ),
            )

    fun failure(
        status: Int,
        code: String,
    ): ResponseEntity<Map<String, Any?>> =
        ResponseEntity.status(status).body(
            mapOf(
                "data" to null,
                "errors" to
                    listOf(
                        mapOf(
                            "message" to "The request could not be completed.",
                            "extensions" to mapOf("code" to code),
                        ),
                    ),
            ),
        )
}
