package com.spoolin.core.boundary.http

import com.spoolin.core.features.connector.ConnectorTokenDigest
import com.spoolin.core.features.mcpgateway.ConnectorRpcContext
import com.spoolin.core.features.mcpgateway.McpGatewayRpc
import com.spoolin.core.features.mcpgateway.ProposalUiChallengeStore
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import spoolin.v1.Spoolin

/** Trusted review UI approval/rejection (connector bearer + bound UI challenge). */
@RestController
class McpReviewApprovalGateway(
    private val gateway: McpGatewayRpc,
    private val uiChallenges: ProposalUiChallengeStore,
) {
    @PostMapping("/gateway/approve_movie")
    fun approveMovie(
        @RequestHeader(name = "Authorization", required = false) authorization: String?,
        @RequestBody body: ApprovalBody,
    ): ResponseEntity<Map<String, Any?>> = submit(authorization, body, approve = true)

    @PostMapping("/gateway/reject_movie")
    fun rejectMovie(
        @RequestHeader(name = "Authorization", required = false) authorization: String?,
        @RequestBody body: ApprovalBody,
    ): ResponseEntity<Map<String, Any?>> = submit(authorization, body, approve = false)

    private fun submit(
        authorization: String?,
        body: ApprovalBody,
        approve: Boolean,
    ): ResponseEntity<Map<String, Any?>> {
        val token = bearerToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (body.proposalId.isBlank() || body.revision < 1 || body.contentHash.isBlank() || body.challenge.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
        if (
            !uiChallenges.matchesBoundChallenge(
                body.proposalId,
                body.revision,
                body.contentHash,
                body.challenge,
            )
        ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val hash = ConnectorTokenDigest.sha256(token.toByteArray(Charsets.UTF_8))
        return ConnectorRpcContext.context(hash).call {
            try {
                val input =
                    Spoolin.ApprovalInput
                        .newBuilder()
                        .setProposalId(body.proposalId)
                        .setRevision(body.revision)
                        .setContentHash(body.contentHash)
                        .setChallenge(body.challenge)
                        .build()
                val outcome =
                    if (approve) {
                        gateway.approveMovieBlocking(input)
                    } else {
                        gateway.rejectMovieBlocking(input)
                    }
                ResponseEntity.ok(outcomeJson(outcome))
            } catch (failure: StatusRuntimeException) {
                when (failure.status.code) {
                    Status.Code.PERMISSION_DENIED -> ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                    Status.Code.INVALID_ARGUMENT -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
                    else -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
                }
            }
        }
    }

    private fun outcomeJson(outcome: Spoolin.Outcome): Map<String, Any?> =
        mapOf(
            "operationId" to outcome.operationId,
            "state" to outcome.state,
            "resultExpired" to outcome.resultExpired,
            "resultId" to outcome.resultId.takeIf { it.isNotBlank() },
            "version" to outcome.version.takeIf { outcome.hasVersion() },
            "problem" to
                if (outcome.hasProblem()) {
                    mapOf(
                        "code" to outcome.problem.code,
                        "message" to "The request could not be completed.",
                        "fields" to emptyList<Any>(),
                        "retryAfterMs" to null,
                    )
                } else {
                    null
                },
        )

    private fun bearerToken(value: String?): String? {
        if (value == null) return null
        val trimmed = value.trim()
        if (!trimmed.startsWith("Bearer ", ignoreCase = true)) return null
        val raw = trimmed.substring(7).trim()
        return raw.ifEmpty { null }
    }

    data class ApprovalBody(
        val proposalId: String = "",
        val revision: Int = 0,
        val contentHash: String = "",
        val challenge: String = "",
    )
}
