package com.spoolin.core.boundary.http

import com.spoolin.core.features.mcpgateway.ProposalUiChallengeStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Trusted review UI binding (challenge only; not model tool output). */
@RestController
class McpReviewUiGateway(
    private val uiChallenges: ProposalUiChallengeStore,
) {
    @GetMapping("/gateway/review_ui_binding")
    fun reviewUiBinding(
        @RequestParam proposalId: String,
        @RequestParam revision: Int,
        @RequestParam contentHash: String,
        @RequestParam bindingToken: String,
    ): ResponseEntity<Map<String, String>> {
        if (proposalId.isBlank() || revision < 1 || contentHash.isBlank() || bindingToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
        val challenge =
            uiChallenges.getChallenge(proposalId, revision, contentHash, bindingToken)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(mapOf("challenge" to challenge))
    }
}
