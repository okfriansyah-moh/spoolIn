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
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DEADLINE_SECONDS = 30L
private const val MAX_BYTES = 5_242_880L

/** GraphQL uploadArtwork / cancelArtwork through Media upload and Catalog attachment (T023). */
class ArtworkGraphql(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val media: MediaServiceGrpc.MediaServiceStub,
    private val issuer: ActorIssuer?,
) {
    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Mutation") { m ->
            m
                .dataFetcher("uploadArtwork") { env -> uploadArtwork(env) }
                .dataFetcher("cancelArtwork") { env -> cancelArtwork(env) }
        }
    }

    @Suppress("ReturnCount")
    private fun uploadArtwork(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val uploadOperationId = id(env)
        val titleId = requireNotNull(env.getArgument<String>("titleId"))
        validateUuid(titleId)
        val expectedVersion = requireNotNull(env.getArgument<Long>("expectedVersion"))
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val content = Base64.getDecoder().decode(env.getArgument<String>("content") ?: "")
        val invalid = invalidUploadContent(content, uploadOperationId)
        if (invalid != null) {
            return invalid
        }
        val assetId = UUID.randomUUID()
        val uploadCtx =
            ArtworkUploadContext(
                browser = context,
                principal = principal,
                uploadOperationId = uploadOperationId,
                policyRevision = policyRevision,
                assetId = assetId,
                content = content,
            )
        val uploadOutcome = runMediaUpload(uploadCtx)
        val failed = failedUploadResult(uploadOutcome)
        return failed
            ?: attachUploadedArtwork(
                ArtworkAttachContext(
                    browser = context,
                    principal = principal,
                    uploadOperationId = uploadOperationId,
                    titleId = titleId,
                    expectedVersion = expectedVersion,
                    policyRevision = policyRevision,
                    assetId = assetId,
                    uploadOutcome = uploadOutcome,
                ),
            )
    }

    private fun invalidUploadContent(
        content: ByteArray,
        uploadOperationId: UUID,
    ): Map<String, Any?>? =
        if (content.isEmpty() || content.size.toLong() > MAX_BYTES) {
            fileInvalid(uploadOperationId)
        } else {
            null
        }

    private fun runMediaUpload(ctx: ArtworkUploadContext): Spoolin.Outcome {
        val uploadToken =
            issueToken(
                ctx.principal,
                "/spoolin.v1.MediaService/Upload",
                ctx.uploadOperationId,
                ctx.browser.correlation,
            )
        return ArtworkUpload.upload(
            ArtworkUploadRequest(
                media = media,
                actorToken = uploadToken,
                principal = ctx.principal,
                uploadOperationId = ctx.uploadOperationId,
                correlationId = ctx.browser.correlation,
                policyRevision = ctx.policyRevision,
                assetId = ctx.assetId,
                content = ctx.content,
            ),
        )
    }

    private fun failedUploadResult(uploadOutcome: Spoolin.Outcome): Map<String, Any?>? =
        if (uploadOutcome.state != "FAILED") {
            null
        } else {
            mapOf(
                "operation" to outcomeMap(uploadOutcome),
                "problem" to
                    if (uploadOutcome.hasProblem()) {
                        problemMap(uploadOutcome.problem)
                    } else {
                        fileInvalidProblem()
                    },
            )
        }

    private fun attachUploadedArtwork(ctx: ArtworkAttachContext): Map<String, Any?> {
        val attachOperationId = UUID.randomUUID()
        val hold =
            Spoolin.Hold
                .newBuilder()
                .setContext(
                    Spoolin.Context
                        .newBuilder()
                        .setOperationId(attachOperationId.toString())
                        .setCorrelationId(ctx.browser.correlation.toString())
                        .setPolicyRevision(ctx.policyRevision)
                        .build(),
                ).setHoldId(ctx.assetId.toString())
                .setAssetId(ctx.assetId.toString())
                .setTitleId(ctx.titleId)
                .setExpectedVersion(ctx.expectedVersion)
                .build()
        val catalogToken =
            issueToken(
                ctx.principal,
                "/spoolin.v1.CatalogService/BeginArtwork",
                attachOperationId,
                ctx.browser.correlation,
            )
        val attachOutcome = catalogStub(catalogToken).beginArtwork(hold)
        ctx.browser.rememberOperation(
            ctx.uploadOperationId,
            OperationOwner.MEDIA,
            outcomeMap(ctx.uploadOutcome),
        )
        ctx.browser.rememberOperation(
            attachOperationId,
            OperationOwner.CATALOG,
            outcomeMap(attachOutcome),
        )
        return mutationResult(attachOutcome)
    }

    private fun cancelArtwork(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = browser(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val uploadOperationId = UUID.fromString(env.getArgument<String>("uploadOperationId"))
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

    private fun browser(env: DataFetchingEnvironment): BrowserContext = requireNotNull(env.graphQlContext.get("browser"))

    private fun id(env: DataFetchingEnvironment): UUID = UUID.fromString(env.getArgument<String>("operationId"))

    private fun validateUuid(id: String) {
        runCatching { UUID.fromString(id).also { require(it.toString() == id) } }
            .onFailure { throw InputViolation("titleId", "Supply a lowercase, hyphenated UUID.") }
    }
}
