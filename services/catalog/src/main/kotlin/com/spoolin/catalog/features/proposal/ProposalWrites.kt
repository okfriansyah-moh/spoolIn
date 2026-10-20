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
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.SafeResult
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.Base64
import java.util.UUID
import javax.sql.DataSource

private const val HASH_BYTES = 32
private val APPROVAL_TTL = Duration.ofMinutes(2)
private const val APPROVE_METHOD = "/spoolin.v1.CatalogService/ApproveAndCreate"
private const val REJECT_METHOD = "/spoolin.v1.CatalogService/RejectProposal"

class ProposalWrites(
    private val dataSource: DataSource,
    private val receipts: OwnerReceipts,
    private val movieWrites: MovieWrites,
    private val metrics: ProposalMetrics? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val livePolicyRevision: () -> String = { "default" },
    private val posters: PreparedMoviePosters? = null,
) {
    private val random = SecureRandom()

    fun propose(
        request: Spoolin.Proposal,
        response: StreamObserver<Spoolin.ProposalRevision>,
    ) {
        try {
            val claims = proposalClaims()
            val normalized = ProposalDraft.normalize(request)
            checkPolicy(normalized.context.policyRevision)
            var freshChallenge: String? = null
            val intent = proposalIntent(normalized, claims)
            val correlation = parseId(request.context.correlationId)
            val mutation: (java.sql.Connection) -> SafeResult = { conn ->
                checkPolicy(normalized.context.policyRevision)
                val challenge = randomChallenge().also { freshChallenge = it }
                val poster = if (normalized.hasPosterRef()) requireNotNull(posters).resolve(conn,normalized.posterRef,claims.clientKind == null) else null
                if (poster != null && poster.state != "READY") throw Status.FAILED_PRECONDITION.withDescription("POSTER_NOT_READY").asRuntimeException()
                ProposalRevisions.insert(conn, normalized, claims, challenge, clock.instant(),poster)
            }
            val outcome =
                if (normalized.hasProposalId()) {
                    receipts.execute(intent, correlation, true, mutation = mutation)
                } else {
                    receipts.defer(intent, correlation, true) { mutation(it) }
                }
            val row = proposalRow(normalized, outcome)
            ProposalRevisions.assertOwner(row, claims)
            ProposalRevisions.requireReview(row, clock.instant())
            response.onNext(ProposalResponse.fromRow(row, freshChallenge))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: ActorRejected) {
            response.onError(Status.PERMISSION_DENIED.asRuntimeException())
        } catch (_: IllegalArgumentException) {
            response.onError(Status.INVALID_ARGUMENT.asRuntimeException())
        } catch (_: com.spoolin.support.receipts.ReceiptConflict) {
            response.onError(Status.ALREADY_EXISTS.withDescription("OPERATION_CONFLICT").asRuntimeException())
        } catch (_: com.spoolin.support.receipts.ReceiptHidden) {
            response.onError(Status.PERMISSION_DENIED.asRuntimeException())
        }
    }

    fun preparePoster(request: Spoolin.PrepareMoviePosterRequest,response: StreamObserver<Spoolin.PreparedPoster>) = respond(response) {
        proposalClaims()
        checkPolicy(request.context.policyRevision)
        requireNotNull(posters).prepare(request)
    }

    fun readPoster(request: Spoolin.MoviePosterReadRequest,response: StreamObserver<Spoolin.MoviePosterImage>) = respond(response) {
        if (!request.hasAssetId()) proposalClaims()
        requireNotNull(posters).read(request)
    }

    fun read(request: Spoolin.MovieProposalRequest,response: StreamObserver<Spoolin.ProposalRevision>) = respond(response) {
        val claims = proposalClaims()
        dataSource.committed { conn ->
            val row = if (request.hasRevision()) ProposalStore.requireForUpdate(conn,parseId(request.proposalId),request.revision)
                else ProposalStore.latestForUpdate(conn,parseId(request.proposalId)) ?: throw Status.NOT_FOUND.asRuntimeException()
            ProposalRevisions.assertOwner(row,claims)
            // Only the authenticated browser receives a new review challenge.
            val challenge = if (claims.clientKind == null && row.state == "REVIEW" && clock.instant().isBefore(row.expiresAt)) randomChallenge() else null
            if (challenge != null) conn.executeBound(
                "UPDATE movie_proposal SET challenge_hash=? WHERE id=? AND revision=?",
                java.security.MessageDigest.getInstance("SHA-256").digest(challenge.toByteArray()),row.id,row.revision)
            ProposalResponse.fromRow(row,challenge)
        }
    }

    private fun <T> respond(response: StreamObserver<T>, operation: () -> T) {
        try { response.onNext(operation()); response.onCompleted() }
        catch (e: io.grpc.StatusRuntimeException) { response.onError(e) }
        catch (_: IllegalArgumentException) { response.onError(Status.INVALID_ARGUMENT.asRuntimeException()) }
        catch (_: ActorRejected) { response.onError(Status.PERMISSION_DENIED.asRuntimeException()) }
    }

    private fun proposalRow(
        request: Spoolin.Proposal,
        outcome: com.spoolin.support.receipts.ReceiptOutcome,
    ): ProposalRow =
        dataSource.connection.use { conn ->
            if (!request.hasProposalId()) {
                ProposalStore.initialForOperation(conn, parseId(request.context.operationId))
                    ?: throw Status.NOT_FOUND.asRuntimeException()
            } else {
                val result =
                    outcome.result ?: throw Status.FAILED_PRECONDITION
                        .withDescription("RESULT_EXPIRED")
                        .asRuntimeException()
                ProposalStore.requireForUpdate(conn, result.resultId, Math.toIntExact(requireNotNull(result.version)))
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
            decodeContentHash(request.contentHash)
            val owner = dataSource.connection.use { ProposalStore.requireForUpdate(it,proposalId,revision) }
            ProposalRevisions.assertOwner(owner,claims)
            val bindingClaims = if (claims.clientKind == null) claims.copy(credentialId=owner.connectorId,clientKind=owner.clientKind) else claims
            val binding = ExactApprovalBinding.fromInput(bindingClaims, request, livePolicyRevision(), clock.instant())
            val operationId = approvalOperationId(proposalId, revision, approve)
            val correlationId = UUID.randomUUID()
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
            val original = originalOutcome(claims, proposalId, revision)
            response.onNext(outcomeProto(original, original.id))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: ActorRejected) {
            response.onError(Status.PERMISSION_DENIED.asRuntimeException())
        } catch (_: IllegalArgumentException) {
            response.onError(Status.INVALID_ARGUMENT.asRuntimeException())
        }
    }

    private fun originalOutcome(
        claims: com.spoolin.support.identity.ActorClaims,
        proposalId: UUID,
        revision: Int,
    ): com.spoolin.support.receipts.ReceiptOutcome {
        val row = dataSource.connection.use { ProposalStore.requireForUpdate(it, proposalId, revision) }
        ProposalRevisions.assertOwner(row, claims)
        return receipts.find(row.operationId, "account:${claims.accountId}")
            ?: throw Status.NOT_FOUND.withDescription("ORIGINAL_INTENT_UNAVAILABLE").asRuntimeException()
    }

    private fun assertApprovalClaims(approve: Boolean): com.spoolin.support.identity.ActorClaims {
        val claims = proposalClaims()
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
                listOf(
                    "proposalId" to proposalId.toString(),
                    "revision" to revision.toString(),
                    "connectorId" to claims.credentialId.toString(),
                    "clientKind" to claims.clientKind,
                    "contentHash" to Base64.getEncoder().encodeToString(binding.contentHash),
                ),
            )
        verifyApprovalReplay(binding, proposalId, revision)
        return receipts.execute(intent, correlationId, true) { conn ->
            applyApprovalMutation(conn, claims, binding, proposalId, revision, approve, operationId, correlationId)
        }
    }

    private fun verifyApprovalReplay(
        binding: ExactApprovalBinding,
        proposalId: UUID,
        revision: Int,
    ) {
        val row = dataSource.connection.use { ProposalStore.requireForUpdate(it, proposalId, revision) }
        val problem = binding.identityProblem(row) ?: binding.challengeProblem(row.challengeHash)
        problem?.let { throw it.asRuntimeException() }
        val retained = if (row.state == "REJECTED") row.copy(state = "CREATED") else row
        binding.reviewProblem(retained)?.let { throw it.asRuntimeException() }
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
        val row = ProposalStore.requireForUpdate(conn, proposalId, revision)
        checkPolicy(binding.policyRevision)
        binding.copy(now = clock.instant()).verify(row, row.challengeHash)?.let { throw it.asRuntimeException() }
        if (row.state == "CREATED" && row.movieId != null) {
            return SafeResult(row.movieId, 0)
        }
        ProposalRevisions.requirePendingIntent(conn, row)
        val content = ProposalContent.parseJson(row.contentJson)
        val storedDigest = DeterministicContentSerializer.digest(content)
        if (!storedDigest.contentEquals(row.contentHash)) {
            throw Status.FAILED_PRECONDITION.withDescription("STORED_CONTENT_INVALID").asRuntimeException()
        }
        val result =
            if (approve) {
                publishApprovedProposal(conn, claims, row, content, correlationId)
            } else {
                ProposalStore.markRejected(conn, proposalId, revision)
                receipts.completePending(conn, row.operationId, SafeResult(proposalId, revision.toLong()), "REJECTED")
                SafeResult(proposalId, revision.toLong())
            }
        recordDecision(conn, claims, row, approve, operationId, correlationId)
        return result
    }

    private fun recordDecision(
        conn: java.sql.Connection,
        claims: com.spoolin.support.identity.ActorClaims,
        row: ProposalRow,
        approve: Boolean,
        operationId: UUID,
        correlationId: UUID,
    ) {
        AuditInsert.record(
            conn,
            operationId,
            claims.accountId,
            auditActorKind(claims),
            if (approve) "APPROVE_PROPOSAL" else "REJECT_PROPOSAL",
            "PROPOSAL",
            row.id,
            if (approve) "APPROVED" else "REJECTED",
            "{}",
            correlationId,
            clock.instant(),
        )
    }

    private fun publishApprovedProposal(
        conn: java.sql.Connection,
        claims: com.spoolin.support.identity.ActorClaims,
        row: ProposalRow,
        content: Map<String, Any?>,
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
        ProposalStore.markCreated(conn, row, movieOutcome.resultId, approvedAt, approvalExpires)
        val poster = ProposalContent.poster(content)
        if (poster == null) receipts.completePending(conn, row.operationId, movieOutcome)
        else enqueuePosterAttachment(conn,claims,row,movieOutcome,poster,correlationId)
        return movieOutcome
    }

    private fun enqueuePosterAttachment(conn: java.sql.Connection,claims: com.spoolin.support.identity.ActorClaims,row: ProposalRow,movie: SafeResult,poster: Spoolin.PreparedPoster,correlation: UUID) {
        com.spoolin.catalog.features.artwork.ArtworkAttachmentSql.insertPreparing(conn,row.operationId,movie.resultId,row.actorId,
            movie.version ?: 0,UUID.fromString(poster.assetId),ProposalContent.policyRevision(ProposalContent.parseJson(row.contentJson)),null)
        val work = com.spoolin.support.receipts.WorkRequest("attach","ARTWORK_ATTACH",row.operationId,clock.instant().plusSeconds(300),baseDelayMs=10000,capDelayMs=60000)
        com.spoolin.catalog.operations.receipts.ReceiptWork.enqueue(conn,row.operationId,correlation,work,clock.instant())
        conn.executeBound("UPDATE durable_work SET payload=?::jsonb WHERE id=?",
            "{\"subjectId\":\"${row.operationId}\",\"credentialId\":\"${claims.credentialId}\",\"authEpoch\":${claims.authEpoch}}",work.id(row.operationId))
    }

    private fun auditActorKind(claims: com.spoolin.support.identity.ActorClaims): String =
        when (claims.clientKind) {
            null -> "ANONYMOUS"
            "CLAUDE", "CHATGPT" -> "SERVICE"
            else -> "SERVICE"
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
                .getOrElse {
                    throw Status.INVALID_ARGUMENT.withDescription("CONTENT_HASH_INVALID").asRuntimeException()
                }
        if (decoded.size != HASH_BYTES) {
            throw Status.INVALID_ARGUMENT.withDescription("CONTENT_HASH_INVALID").asRuntimeException()
        }
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
            builder.problem =
                Spoolin.Problem
                    .newBuilder()
                    .setCode(it)
                    .build()
        }
        return builder.build()
    }

    private fun proposalClaims(): com.spoolin.support.identity.ActorClaims {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role) || (claims.clientKind != null && claims.clientKind !in setOf("CLAUDE", "CHATGPT"))) {
            throw ActorRejected()
        }
        return claims
    }

    private fun checkPolicy(revision: String) {
        if (revision != livePolicyRevision()) {
            throw Status.FAILED_PRECONDITION.withDescription("POLICY_CHANGED").asRuntimeException()
        }
    }

    private fun proposalIntent(
        request: Spoolin.Proposal,
        claims: com.spoolin.support.identity.ActorClaims,
    ): OperationIntent =
        OperationIntent(
            parseId(request.context.operationId),
            "account:${claims.accountId}",
            "ProposeMovie",
            request.context.policyRevision,
            listOf(
                "proposalId" to request.proposalId,
                "expectedRevision" to request.expectedRevision.takeIf { request.hasExpectedRevision() }?.toString(),
                "connectorId" to claims.credentialId.toString(),
                "clientKind" to claims.clientKind,
                "content" to DeterministicContentSerializer.json(ProposalContent.fromProposal(request)),
            ),
        )

    private fun randomChallenge(): String {
        val bytes = ByteArray(HASH_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun parseId(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value) }
}
