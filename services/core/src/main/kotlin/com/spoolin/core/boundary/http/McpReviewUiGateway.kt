package com.spoolin.core.boundary.http

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Challenge redemption over HTTP is closed.
 * The raw challenge is delivered only in MCP Apps `_meta.ui` for the trusted host.
 */
@RestController
class McpReviewUiGateway {
    @GetMapping("/gateway/review_ui_binding")
    fun reviewUiBinding(): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.NOT_FOUND).build()
}
