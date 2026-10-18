@file:Suppress("TooManyFunctions")

package com.spoolin.core.features.artworkproxy

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.ActorTarget
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.receipts.OperationOwner
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DEADLINE_SECONDS = 30L
private const val CHILD_DEADLINE_SECONDS = 300L
private const val MAX_BYTES = 5_242_880L

/** GraphQL uploadArtwork / cancelArtwork through Media upload and Catalog attachment (T023). */
class ArtworkGraphql(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val media: MediaServiceGrpc.MediaServiceStub,
    private val issuer: ActorIssuer?,
    private val maximumBytes: Long = MAX_BYTES,
    private val reads: ArtworkReadProxy? = null,
) {
    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Title") { title -> title.dataFetcher("artwork") { env -> artwork(env) } }
        wiring.type("Mutation") { m ->
            m
                .dataFetcher("uploadArtwork") { env -> uploadArtwork(env) }
                .dataFetcher("cancelArtwork") { env -> cancelArtwork(env) }
        }
    }

    private fun artwork(env: DataFetchingEnvironment): Map<String, Any>? {
        val source = env.getSource<Map<String, Any?>>() ?: return null
        val raw = source["_artworkAssetId"] as? String ?: return null
        val asset = UUID.fromString(raw).also { require(it.toString() == raw) }
        val proxy = reads ?: throw RateUnavailable()
        return proxy.metadata(asset, source["_artworkManagement"] == true, browser(env)).fields()
    }

    private fun uploadArtwork(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = browser(env)
        val hold = hold(env, context)
        val operation = UUID.fromString(hold.context.operationId)
        val spool = decodeSpool(env.getArgument<String>("content") ?: "") ?: return fileInvalid(operation)
        return spool.use {
            val principal = requireNotNull(context.principal)
            val token = issueToken(principal, "/spoolin.v1.CatalogService/BeginArtwork", operation, context.correlation)
            val pending = catalogStub(token).beginArtwork(hold)
            validateOutcome(pending, operation)
            context.rememberOperation(operation, OperationOwner.CATALOG, outcomeMap(pending))
            if (pending.state == "PENDING") uploadSpool(context, hold, it)
            mutationResult(pending)
        }
    }

    private fun hold(env: DataFetchingEnvironment, context: BrowserContext): Spoolin.Hold {
        val principal = context.principal ?: throw CredentialRejected()
        val operation = id(env)
        val titleId = requireNotNull(env.getArgument<String>("titleId"))
        validateUuid(titleId, "titleId")
        val policy = requireNotNull(env.getArgument<String>("policyRevision"))
        return Spoolin.Hold.newBuilder().setContext(Spoolin.Context.newBuilder()
            .setOperationId(operation.toString()).setCorrelationId(context.correlation.toString()).setPolicyRevision(policy))
            .setHoldId(operation.toString()).setAssetId(child(operation, "asset").toString()).setTitleId(titleId)
            .setActorId(principal.accountId.toString())
            .setExpectedVersion(requireNotNull(env.getArgument<Long>("expectedVersion"))).build()
    }

    private fun decodeSpool(encoded: String): ArtworkSpool? = try {
        ArtworkSpool.decode(encoded, maximumBytes)
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: java.io.IOException) {
        null
    }

    private fun uploadSpool(context: BrowserContext, hold: Spoolin.Hold, spool: ArtworkSpool) {
        val principal = requireNotNull(context.principal)
        val operation = UUID.fromString(hold.context.operationId)
        val upload = child(operation, "media-upload")
        val token = issueToken(principal, "/spoolin.v1.MediaService/Upload", upload, context.correlation)
        val accepted = ArtworkUpload.upload(ArtworkUploadRequest(
            media, token, principal, upload, context.correlation, hold.context.policyRevision,
            UUID.fromString(hold.assetId), operation, spool,
        ))
        validateOutcome(accepted, upload)
        context.rememberOperation(upload, OperationOwner.MEDIA, outcomeMap(accepted))
    }

    private fun validateOutcome(outcome: Spoolin.Outcome, operation: UUID) {
        val validIdentity = outcome.operationId == operation.toString()
        val validState = outcome.state in setOf("PENDING", "SUCCEEDED", "FAILED", "EXPIRED")
        if (!validIdentity || !validState) throw io.grpc.Status.DATA_LOSS.asRuntimeException()
        validateResult(outcome)
    }

    private fun validateResult(outcome: Spoolin.Outcome) {
        if (outcome.hasVersion() && outcome.version < 0) throw io.grpc.Status.DATA_LOSS.asRuntimeException()
        if (outcome.resultExpired && (outcome.hasVersion() || outcome.hasResultId())) {
            throw io.grpc.Status.DATA_LOSS.asRuntimeException()
        }
        validateResultId(outcome)
    }

    private fun validateResultId(outcome: Spoolin.Outcome) {
        if (outcome.hasResultId()) {
            val valid = isUuid(outcome.resultId)
            if (!valid) throw io.grpc.Status.DATA_LOSS.asRuntimeException()
        }
    }

    private fun child(operation: UUID, step: String): UUID =
        com.spoolin.support.receipts.WorkRequest(
            step, "ARTWORK", operation, java.time.Instant.now().plusSeconds(CHILD_DEADLINE_SECONDS),
        ).id(operation)

    private fun cancelArtwork(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val uploadId = requireNotNull(env.getArgument<String>("uploadOperationId"))
        validateUuid(uploadId, "uploadOperationId")
        val uploadOperationId = UUID.fromString(uploadId)
        val request =
            Spoolin.CancelArtworkRequest
                .newBuilder()
                .setContext(
                    Spoolin.Context
                        .newBuilder()
                        .setOperationId(operationId.toString())
                        .setCorrelationId(context.correlation.toString())
                        .setPolicyRevision("")
                        .build(),
                ).setUploadOperationId(uploadOperationId.toString())
                .build()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/CancelArtwork", operationId, context.correlation)
        val outcome = catalogStub(token).cancelArtwork(request)
        validateOutcome(outcome.operation, operationId)
        validateOutcome(outcome.uploadOperation, uploadOperationId)
        return mapOf(
            "operation" to outcomeMap(outcome.operation),
            "uploadOperation" to outcomeMap(outcome.uploadOperation),
            "cancelled" to outcome.cancelled,
        )
    }

    private fun fileInvalid(operationId: UUID): Map<String, Any?> =
        mapOf(
            "operation" to
                mapOf(
                    "id" to operationId.toString(),
                    "state" to "FAILED",
                    "resultExpired" to false,
                    "problem" to fileInvalidProblem(),
                ),
            "problem" to fileInvalidProblem(),
        )

    private fun fileInvalidProblem(): Map<String, Any?> =
        mapOf(
            "code" to "FILE_INVALID",
            "message" to "Artwork could not be accepted.",
        )

    private fun mutationResult(outcome: Spoolin.Outcome): Map<String, Any?> =
        mapOf(
            "operation" to outcomeMap(outcome),
            "problem" to if (outcome.hasProblem()) problemMap(outcome.problem) else null,
        )

    private fun outcomeMap(outcome: Spoolin.Outcome): Map<String, Any?> =
        mapOf(
            "id" to outcome.operationId,
            "state" to outcome.state,
            "resultId" to if (outcome.hasResultId()) outcome.resultId else null,
            "version" to if (outcome.hasVersion()) outcome.version.toString() else null,
            "resultExpired" to outcome.resultExpired,
            "problem" to if (outcome.hasProblem()) problemMap(outcome.problem) else null,
        )

    private fun problemMap(p: Spoolin.Problem): Map<String, Any?> =
        mapOf(
            "code" to p.code,
            "message" to "The operation could not be completed.",
            "fields" to p.fieldsList.map { mapOf("path" to it.path, "code" to it.code, "message" to it.message) },
        )

    private fun issueToken(
        principal: com.spoolin.core.features.session.SessionPrincipal,
        method: String,
        operationId: UUID,
        correlation: UUID,
    ): String {
        val signer = issuer ?: throw RateUnavailable()
        return signer.issue(principal, ActorTarget(channelFor(method), method, operationId, correlation))
    }

    private fun channelFor(method: String): String = if (method.contains("MediaService")) "media" else "catalog"

    private fun catalogStub(token: String): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val meta = Metadata().apply { put(ActorRpc.metadata, token) }
        return catalog
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(meta))
            .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
    }

    private fun browser(env: DataFetchingEnvironment): BrowserContext =
        requireNotNull(env.graphQlContext.get("browser"))

    private fun id(env: DataFetchingEnvironment): UUID {
        val value = requireNotNull(env.getArgument<String>("operationId"))
        validateUuid(value, "operationId")
        return UUID.fromString(value)
    }

    private fun validateUuid(id: String, path: String) {
        if (!isUuid(id)) throw InputViolation(path, "Supply a lowercase, hyphenated UUID.")
    }

    private fun isUuid(value: String): Boolean = try {
        UUID.fromString(value).toString() == value
    } catch (_: IllegalArgumentException) {
        false
    }
}
