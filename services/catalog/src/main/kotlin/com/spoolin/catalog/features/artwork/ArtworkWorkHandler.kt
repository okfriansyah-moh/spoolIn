package com.spoolin.catalog.features.artwork

import com.spoolin.catalog.operations.work.WorkLeaseGuard
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkClaim
import io.grpc.Status
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class ArtworkWorkHandler(
    private val store: ArtworkWorkStore,
    private val reads: ArtworkReads,
    private val media: MediaServiceGrpc.MediaServiceBlockingStub,
    private val identity: IdentityServiceGrpc.IdentityServiceBlockingStub,
    dataSource: DataSource,
) {
    private val mapper = JsonMapper.builder().build()
    private val guard = WorkLeaseGuard(dataSource)

    fun reconcileExhausted() = store.reconcileExhausted()

    fun handle(claim: WorkClaim) {
        val payload = mapper.readTree(claim.payload)
        val intent = store.read(UUID.fromString(payload["subjectId"].asText()))
        try {
            when (claim.kind) {
                "ARTWORK_ATTACH" -> attach(claim, intent, payload)
                "ARTWORK_CONFIRM" -> {
                    mediaStub().confirm(hold(intent, "media-confirm", claim.correlationId, intent.expectedVersion + 1))
                    guard.complete(claim, AttemptOutcome.SUCCEEDED)
                }
                "ARTWORK_RELEASE" -> {
                    mediaStub().release(hold(intent, "media-release", claim.correlationId, payload["catalogVersion"].asLong()))
                    guard.complete(claim, AttemptOutcome.SUCCEEDED)
                }
                else -> guard.complete(claim, AttemptOutcome.PERMANENT, "UNSUPPORTED_WORK_KIND")
            }
        } catch (failure: io.grpc.StatusRuntimeException) {
            val transient = failure.status.code in setOf(Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED, Status.Code.NOT_FOUND)
            if (claim.kind == "ARTWORK_ATTACH" && (!transient || claim.attemptCount >= claim.maxAttempts)) {
                store.abort(claim, intent, "ARTWORK_UNAVAILABLE")
            } else guard.complete(claim, if (transient) AttemptOutcome.TRANSIENT else AttemptOutcome.PERMANENT, "ARTWORK_DOWNSTREAM_UNAVAILABLE")
        }
    }

    private fun attach(claim: WorkClaim, intent: ArtworkIntent, payload: tools.jackson.databind.JsonNode) {
        if (intent.state != "PREPARING") { guard.complete(claim, AttemptOutcome.SUCCEEDED); return }
        val asset = mediaStub().getAsset(Spoolin.Id.newBuilder().setId(intent.asset.toString()).build())
        if (asset.state == "FAILED" || asset.state == "DELETED") { store.abort(claim, intent, "FILE_INVALID"); return }
        if (asset.state != "READY") { throw Status.UNAVAILABLE.withDescription("ARTWORK_PROCESSING").asRuntimeException() }
        verifyActor(intent, payload)
        mediaStub().reserve(hold(intent, "media-reserve", claim.correlationId, null))
        verifyActor(intent, payload)
        // The hold is durable before Catalog can select its immutable asset.
        store.attach(claim, intent)
    }

    private fun verifyActor(intent: ArtworkIntent, payload: tools.jackson.databind.JsonNode) {
        val actor = identity.withDeadlineAfter(2, TimeUnit.SECONDS).checkActor(Spoolin.CheckActorRequest.newBuilder()
            .setAccountId(intent.actor.toString()).setCredentialId(payload["credentialId"].asText()).setAuthEpoch(payload["authEpoch"].asLong()).setAudience("catalog").build())
        if (actor.accountId != intent.actor.toString() || actor.authEpoch != payload["authEpoch"].asLong() ||
            actor.credentialId != payload["credentialId"].asText() || actor.expiresAt.seconds <= Instant.now().epochSecond || !Permissions.management(actor.role)) {
            throw Status.PERMISSION_DENIED.asRuntimeException()
        }
    }

    private fun hold(intent: ArtworkIntent, step: String, correlation: UUID, version: Long?): Spoolin.Hold =
        Spoolin.Hold.newBuilder().setContext(Spoolin.Context.newBuilder()
            .setOperationId(WorkRequest(step, "ARTWORK", intent.id, Instant.MAX).id(intent.id).toString())
            .setCorrelationId(correlation.toString()).setPolicyRevision(intent.policy))
            .setHoldId(intent.id.toString()).setAssetId(intent.asset.toString()).setTitleId(intent.title.toString())
            .setActorId(intent.actor.toString()).setExpectedVersion(intent.expectedVersion)
            .also { if (version != null) it.catalogVersion = version }.build()

    private fun mediaStub(): MediaServiceGrpc.MediaServiceBlockingStub = media.withDeadlineAfter(5, TimeUnit.SECONDS)
}
