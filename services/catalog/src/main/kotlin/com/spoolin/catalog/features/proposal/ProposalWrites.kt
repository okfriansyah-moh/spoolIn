@file:Suppress("TooManyFunctions", "LongParameterList")

package com.spoolin.catalog.features.proposal

import com.spoolin.catalog.features.movie.MovieWrites
import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.catalog.operations.metrics.ProposalMetrics
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.ActorRejected
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.util.Base64
import java.util.UUID
import javax.sql.DataSource

private const val PROPOSAL_TTL = Duration.ofMinutes(10)
private const val APPROVAL_TTL = Duration.ofMinutes(2)
private const val MAX_PENDING = 20
private const val APPROVE_METHOD = "/spoolin.v1.CatalogService/ApproveAndCreate"
private const val REJECT_METHOD = "/spoolin.v1.CatalogService/RejectProposal"

class ProposalWrites(
    private val dataSource: DataSource,
    private val receipts: OwnerReceipts,
    private val movieWrites: MovieWrites,
    private val metrics: ProposalMetrics? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val livePolicyRevision: () -> String = { "default" },
) {
    private val random = SecureRandom()

    fun propose(
        request: Spoolin.Proposal,
        response: StreamObserver<Spoolin.ProposalRevision>,
    ) {
        try {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!Permissions.management(claims.role)) throw ActorRejected()
            require(claims.clientKind != null) { "Connector required" }
            val operationId = parseId(request.context.operationId)
            val correlationId = parseId(request.context.correlationId)
            val actorScope = "account:${claims.accountId}"
            val intent =
                OperationIntent(
                    operationId,
                    actorScope,
                    "ProposeMovie",
                    request.context.policyRevision,
                    mapOf("proposal" to "v1"),
                )
            val challenge = randomChallenge()
            val content = ProposalContent.fromProposal(request)
            val contentHash = DeterministicContentSerializer.digest(content)
            val outcome =
                receipts.execute(intent, correlationId, true) { conn ->
                    enforcePendingQuota(conn, claims.accountId)
                    val proposalId =
                        request.proposalId.takeIf { it.isNotBlank() }?.let(::parseId) ?: UUID.randomUUID()
                    val revision = if (request.hasExpectedRevision()) request.expectedRevision + 1 else 1
                    supersedeReview(conn, proposalId)
                    val challengeHash = sha256(challenge.toByteArray(Charsets.UTF_8))
                    val expiresAt = clock.instant().plus(PROPOSAL_TTL)
                    conn.executeBound(
                        "INSERT INTO movie_proposal " +
                            "(id, revision, actor_id, connector_id, client_kind, content, content_hash, " +
                            "challenge_hash, state, operation_id, expires_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, 'REVIEW', ?, ?)",
                        proposalId,
                        revision,
                        claims.accountId,
                        claims.credentialId,
                        claims.clientKind,
                        DeterministicContentSerializer.json(content),
                        contentHash,
                        challengeHash,
                        operationId,
                        Timestamp.from(expiresAt),
                    )
                    SafeResult(proposalId, revision.toLong())
                }
            val stored = outcome.result ?: throw Status.INTERNAL.asRuntimeException()
            response.onNext(
                Spoolin.ProposalRevision
                    .newBuilder()
                    .setProposalId(stored.resultId.toString())
                    .setRevision(stored.version?.toInt() ?: 1)
                    .setValues(request.values)
                    .setContentHash(Base64.getEncoder().encodeToString(contentHash))
                    .setExpiresAt(protoTime(clock.instant().plus(PROPOSAL_TTL)))
                    .setUiChallenge(challenge.toByteArray())
                    .build(),
            )
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: ActorRejected) {
            response.onError(Status.PERMISSION_DENIED.asRuntimeException())
        }
    }

    fun approve(
        request: Spoolin.ApprovalInput,
        response: StreamObserver<Spoolin.Outcome>,
    ) {
        handleApproval(request, response, approve = true)
    }

    fun reject(
        request: Spoolin.ApprovalInput,
        response: StreamObserver<Spoolin.Outcome>,
    ) {
        handleApproval(request, response, approve = false)
    }

    private fun handleApproval(
        request: Spoolin.ApprovalInput,
        response: StreamObserver<Spoolin.Outcome>,
        approve: Boolean,
    ) {
        try {
            val claims = assertApprovalClaims(approve)
            val proposalId = parseId(request.proposalId)
            val revision = request.revision
            val wireHash = decodeContentHash(request.contentHash)
            val binding =
                ExactApprovalBinding.fromInput(
                    claims,
                    proposalId,
                    revision,
                    wireHash,
                    request.challenge,
                    livePolicyRevision(),
                    clock.instant(),
                )
            val operationId = approvalOperationId(proposalId, revision, approve)
            val correlationId = UUID.randomUUID()
            val outcome =
                executeApprovalReceipt(
                    claims,
                    binding,
                    proposalId,
                    revision,
                    approve,
                    operationId,
                    correlationId,
                )
            if (approve) {
                metrics?.markApproved()
            } else {
                metrics?.markRejected()
            }
            response.onNext(outcomeProto(outcome, operationId))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: ActorRejected) {
            response.onError(Status.PERMISSION_DENIED.asRuntimeException())
        } catch (_: IllegalArgumentException) {
            response.onError(Status.INVALID_ARGUMENT.asRuntimeException())
        }
    }

    private fun assertApprovalClaims(approve: Boolean): com.spoolin.support.identity.ActorClaims {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw ActorRejected()
        if (claims.clientKind == null) {
            throw Status.PERMISSION_DENIED.withDescription("CONNECTOR_REQUIRED").asRuntimeException()
        }
        val expectedMethod = if (approve) APPROVE_METHOD else REJECT_METHOD
        if (claims.method != expectedMethod) {
            throw Status.PERMISSION_DENIED.withDescription("MODEL_CONSENT_DENIED").asRuntimeException()
        }
        return claims
    }

    private fun executeApprovalReceipt(
        claims: com.spoolin.support.identity.ActorClaims,
        binding: ExactApprovalBinding,
        proposalId: UUID,
        revision: Int,
        approve: Boolean,
        operationId: UUID,
        correlationId: UUID,
    ): com.spoolin.support.receipts.ReceiptOutcome {
        val actorScope = "account:${claims.accountId}"
        val kind = if (approve) "ApproveAndCreate" else "RejectProposal"
        val intent =
            OperationIntent(
                operationId,
                actorScope,
                kind,
                binding.policyRevision,
                mapOf("proposalId" to proposalId.toString(), "revision" to revision.toString()),
            )
        return receipts.execute(intent, correlationId, true) { conn ->
            applyApprovalMutation(conn, claims, binding, proposalId, revision, approve, operationId, correlationId)
        }
    }

    private fun applyApprovalMutation(
        conn: java.sql.Connection,
        claims: com.spoolin.support.identity.ActorClaims,
        binding: ExactApprovalBinding,
        proposalId: UUID,
        revision: Int,
        approve: Boolean,
        operationId: UUID,
        correlationId: UUID,
    ): SafeResult {
        val row =
            ProposalStore.loadForUpdate(conn, proposalId, revision)
                ?: throw Status.NOT_FOUND.withDescription("PROPOSAL_NOT_FOUND").asRuntimeException()
        if (row.state == "CREATED" && row.movieId != null) {
            return SafeResult(row.movieId, 0)
        }
        binding.verify(row, row.challengeHash)?.let { throw it.asRuntimeException() }
        val content = ProposalContent.parseJson(row.contentJson)
        val storedDigest = DeterministicContentSerializer.digest(content)
        if (!storedDigest.contentEquals(row.contentHash)) {
            throw Status.FAILED_PRECONDITION.withDescription("STORED_CONTENT_INVALID").asRuntimeException()
        }
        val actorKind = claims.clientKind ?: "CONNECTOR"
        if (approve) {
            recordProposalAudit(
                conn,
                "APPROVE_PROPOSAL",
                proposalId,
                claims.accountId,
                operationId,
                correlationId,
                actorKind,
            )
            return publishApprovedProposal(conn, claims, row, content, proposalId, revision, correlationId)
        }
        recordProposalAudit(
            conn,
            "REJECT_PROPOSAL",
            proposalId,
            claims.accountId,
            operationId,
            correlationId,
            actorKind,
        )
        ProposalStore.markRejected(conn, proposalId, revision)
        return SafeResult(proposalId, revision.toLong())
    }

    private fun publishApprovedProposal(
        conn: java.sql.Connection,
        claims: com.spoolin.support.identity.ActorClaims,
        row: ProposalRow,
        content: Map<String, Any?>,
        proposalId: UUID,
        revision: Int,
        correlationId: UUID,
    ): SafeResult {
        val values = ProposalContent.movieValues(content)
        val approvedAt = clock.instant()
        val approvalExpires = minOf(approvedAt.plus(APPROVAL_TTL), row.expiresAt)
        val movieOutcome =
            movieWrites.publishFromProposal(
                conn,
                values,
                claims.accountId,
                row.operationId,
                correlationId,
            )
        ProposalStore.markCreated(
            conn,
            proposalId,
            revision,
            movieOutcome.resultId,
            approvedAt,
            approvalExpires,
            claims.accountId,
            claims.credentialId,
            row.contentHash,
            clock.instant(),
        )
        return movieOutcome
    }

    private fun recordProposalAudit(
        conn: java.sql.Connection,
        action: String,
        proposalId: UUID,
        actorId: UUID,
        operationId: UUID,
        correlationId: UUID,
        actorKind: String,
    ) {
        AuditInsert.record(
            conn,
            operationId,
            actorId,
            actorKind,
            action,
            "PROPOSAL",
            proposalId,
            "SUCCEEDED",
            "{}",
            correlationId,
            clock.instant(),
        )
    }

    private fun approvalOperationId(
        proposalId: UUID,
        revision: Int,
        approve: Boolean,
    ): UUID {
        val label = if (approve) "approve" else "reject"
        return UUID.nameUUIDFromBytes("$label:$proposalId:$revision".toByteArray(Charsets.UTF_8))
    }

    private fun decodeContentHash(value: String): ByteArray {
        val decoded =
            runCatching { Base64.getDecoder().decode(value) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("CONTENT_HASH_INVALID").asRuntimeException() }
        if (decoded.size != 32) throw Status.INVALID_ARGUMENT.withDescription("CONTENT_HASH_INVALID").asRuntimeException()
        return decoded
    }

    private fun outcomeProto(
        outcome: com.spoolin.support.receipts.ReceiptOutcome,
        operationId: UUID,
    ): Spoolin.Outcome {
        val builder =
            Spoolin.Outcome
                .newBuilder()
                .setOperationId(operationId.toString())
                .setState(outcome.state.name)
                .setResultExpired(outcome.resultExpired)
        outcome.result?.let { result ->
            builder.resultId = result.resultId.toString()
            result.version?.let { builder.version = it }
        }
        outcome.errorCode?.let {
            builder.problem = Spoolin.Problem.newBuilder().setCode(it).build()
        }
        return builder.build()
    }

    private fun enforcePendingQuota(
        conn: java.sql.Connection,
        actorId: UUID,
    ) {
        val count =
            conn.queryBound(
                "SELECT count(*) FROM movie_proposal WHERE actor_id = ? AND state = 'REVIEW'",
                listOf(actorId),
            ) { it.getInt(1) }.single()
        if (count >= MAX_PENDING) throw Status.RESOURCE_EXHAUSTED.asRuntimeException()
    }

    private fun supersedeReview(
        conn: java.sql.Connection,
        proposalId: UUID,
    ) {
        conn.executeBound(
            "UPDATE movie_proposal SET state = 'SUPERSEDED' WHERE id = ? AND state = 'REVIEW'",
            proposalId,
        )
    }

    private fun randomChallenge(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(value: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(value)

    private fun protoTime(instant: java.time.Instant): com.google.protobuf.Timestamp =
        com.google.protobuf.Timestamp
            .newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()

    private fun parseId(value: String): UUID =
        UUID.fromString(value).also { require(it.toString() == value) }
}
