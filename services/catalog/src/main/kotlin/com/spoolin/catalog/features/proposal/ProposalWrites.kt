@file:Suppress("TooManyFunctions", "LongParameterList")

package com.spoolin.catalog.features.proposal

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

class ProposalWrites(
    private val dataSource: DataSource,
    private val receipts: OwnerReceipts,
    private val clock: Clock = Clock.systemUTC(),
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
            val content = proposalContent(request)
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
        response.onError(
            Status.FAILED_PRECONDITION
                .withDescription("Trusted UI approval route required")
                .asRuntimeException(),
        )
    }

    fun reject(
        request: Spoolin.ApprovalInput,
        response: StreamObserver<Spoolin.Outcome>,
    ) {
        response.onError(
            Status.FAILED_PRECONDITION
                .withDescription("Trusted UI rejection route required")
                .asRuntimeException(),
        )
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

    private fun proposalContent(request: Spoolin.Proposal): Map<String, Any?> =
        mapOf(
            "values" to request.values,
            "sources" to request.sourcesList,
        )

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
