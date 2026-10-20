package com.spoolin.core.boundary.http

import com.google.protobuf.ByteString
import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.ActorTarget
import com.spoolin.core.features.artworkproxy.PreparedPosterUpload
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.identity.ActorRpc
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

@RestController
class MovieProposalReview(
    channels: GrpcChannelFactory,
    private val identities: ObjectProvider<BrowserIdentity>,
    private val boundary: BrowserBoundary,
    private val issuers: ObjectProvider<ActorIssuer>,
    private val policy: PolicyProvider,
    private val uploads: PreparedPosterUpload,
) {
    private val catalog = CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog"))

    @GetMapping("/api/movie-proposals/{id}")
    fun get(@PathVariable id: String, @RequestParam(required = false) revision: Int?, request: HttpServletRequest) =
        json {
            val context = browser(request, false)
            val input = Spoolin.MovieProposalRequest.newBuilder().setProposalId(uuid(id).toString())
            revision?.let { require(it > 0); input.revision = it }
            MovieProposalJson.revision(stub(context, "GetMovieProposal").getMovieProposal(input.build()))
        }

    @GetMapping("/api/movie-posters/{id}")
    fun poster(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArray> {
        return try {
        val context = browser(request, false)
        val result = stub(context, "ReadMoviePoster").readMoviePoster(
            Spoolin.MoviePosterReadRequest.newBuilder().setRefId(uuid(id).toString()).build(),
        )
        ResponseEntity.ok().header("Cache-Control", "no-store")
            .header("X-Content-Type-Options", "nosniff").header("Content-Type", result.poster.mediaType)
            .body(result.imageBytes.toByteArray())
        } catch (_: BrowserRejected) {
            ResponseEntity.status(403).build()
        } catch (_: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (failure: StatusRuntimeException) {
            val status = when (failure.status.code) {
                Status.Code.UNAUTHENTICATED -> 401
                Status.Code.PERMISSION_DENIED -> 403
                Status.Code.NOT_FOUND -> 404
                else -> 503
            }
            ResponseEntity.status(status).build()
        }
    }

    @PostMapping("/api/movie-proposals/{id}/approve")
    fun approve(@PathVariable id: String, @RequestBody body: Decision, request: HttpServletRequest) =
        decide(id, body, request, true)

    @PostMapping("/api/movie-proposals/{id}/reject")
    fun reject(@PathVariable id: String, @RequestBody body: Decision, request: HttpServletRequest) =
        decide(id, body, request, false)

    private fun decide(id: String, body: Decision, request: HttpServletRequest, approve: Boolean) = json {
        val context = browser(request, true)
        require(body.revision > 0 && body.contentHash.isNotBlank() && body.challenge.isNotBlank())
        val input = Spoolin.ApprovalInput.newBuilder().setProposalId(uuid(id).toString())
            .setRevision(body.revision).setContentHash(body.contentHash).setChallenge(body.challenge).build()
        val result = if (approve) stub(context, "ApproveAndCreate").approveAndCreate(input)
            else stub(context, "RejectProposal").rejectProposal(input)
        MovieProposalJson.outcome(result)
    }

    @PostMapping("/api/movie-proposals/{id}/poster")
    fun replace(@PathVariable id: String, @RequestBody body: PosterReplacement, request: HttpServletRequest) = json {
        val context = browser(request, true)
        val current = stub(context, "GetMovieProposal").getMovieProposal(
            Spoolin.MovieProposalRequest.newBuilder().setProposalId(uuid(id).toString()).setRevision(body.revision).build(),
        )
        require(current.revision == body.revision && current.contentHash == body.contentHash)
        require(body.content.length.toLong() <= ((minOf(policy.policy.artwork.maxInputBytes, 5_242_880L) + 2) / 3) * 4)
        val operation = uuid(body.operationId)
        val prepare = Spoolin.PrepareMoviePosterRequest.newBuilder().setContext(operationContext(context, operation))
            .setImageBytes(ByteString.copyFrom(Base64.getDecoder().decode(body.content)))
            .setMediaType(body.mediaType).setSourceUrl(body.sourceUrl).setReuseBasis(body.reuseBasis).build()
        val poster = uploads.prepare(prepare, requireNotNull(context.principal).accountId, { upload, correlation ->
            signer().issue(requireNotNull(context.principal),
                ActorTarget("media", "/spoolin.v1.MediaService/Upload", upload, correlation))
        }) { stub(context, "PrepareMoviePoster", operation).prepareMoviePoster(prepare) }
        if (poster.state != "READY") return@json mapOf("poster" to MovieProposalJson.poster(poster), "state" to poster.state)
        val proposal = Spoolin.Proposal.newBuilder().setProposalId(id).setExpectedRevision(body.revision)
            .setContext(operationContext(context, UUID.nameUUIDFromBytes("poster-revision:$operation".toByteArray())))
            .setValues(current.values).addAllSources(current.sourcesList).setPosterRef(poster.refId).build()
        MovieProposalJson.revision(stub(context, "ProposeMovie", UUID.fromString(proposal.context.operationId)).proposeMovie(proposal))
    }

    private fun browser(request: HttpServletRequest, mutation: Boolean): BrowserContext {
        if (mutation) boundary.check(request)
        val context = identities.ifAvailable?.context(request, mutation, false, bootstrap = false)
            ?: throw Status.UNAVAILABLE.asRuntimeException()
        val principal = context.principal ?: throw Status.UNAUTHENTICATED.asRuntimeException()
        if (principal.role.name != "MAINTAINER") throw Status.PERMISSION_DENIED.asRuntimeException()
        return context
    }

    private fun signer() = issuers.ifAvailable ?: throw Status.UNAVAILABLE.asRuntimeException()

    private fun stub(context: BrowserContext, method: String, operation: UUID? = null): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val token = signer().issue(requireNotNull(context.principal),
            ActorTarget("catalog", "/spoolin.v1.CatalogService/$method", operation, context.correlation))
        val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
        return catalog.withDeadlineAfter(30, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
    }

    private fun operationContext(context: BrowserContext, operation: UUID) = Spoolin.Context.newBuilder()
        .setOperationId(operation.toString()).setCorrelationId(context.correlation.toString())
        .setPolicyRevision(policy.protobuf().revision).build()

    private fun uuid(value: String) = UUID.fromString(value).also { require(it.toString() == value) }

    private fun json(action: () -> Map<String, Any?>): ResponseEntity<Map<String, Any?>> = try {
        ResponseEntity.ok().header("Cache-Control", "no-store").body(action())
    } catch (failure: BrowserRejected) {
        ResponseEntity.status(failure.status).body(mapOf("error" to "BROWSER_REJECTED"))
    } catch (_: IllegalArgumentException) {
        ResponseEntity.badRequest().body(mapOf("error" to "INVALID_INPUT"))
    } catch (failure: StatusRuntimeException) {
        val status = when (failure.status.code) {
            Status.Code.UNAUTHENTICATED -> 401
            Status.Code.PERMISSION_DENIED -> 403
            Status.Code.NOT_FOUND -> 404
            Status.Code.ALREADY_EXISTS, Status.Code.FAILED_PRECONDITION, Status.Code.ABORTED -> 409
            Status.Code.INVALID_ARGUMENT -> 400
            Status.Code.RESOURCE_EXHAUSTED -> 429
            else -> 503
        }
        ResponseEntity.status(status).body(mapOf("error" to failure.status.code.name))
    }

    data class Decision(val revision: Int = 0, val contentHash: String = "", val challenge: String = "")
    data class PosterReplacement(val operationId: String = "", val revision: Int = 0, val contentHash: String = "",
        val content: String = "", val mediaType: String = "", val sourceUrl: String = "", val reuseBasis: String = "")
}
