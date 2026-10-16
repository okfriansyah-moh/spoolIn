package com.spoolin.core.boundary.http

import org.springframework.http.ResponseEntity

object BrowserResponses {
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
