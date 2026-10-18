package com.spoolin.core.boundary.http

import com.spoolin.core.boundary.graphql.GraphqlEnvelope
import com.spoolin.core.boundary.graphql.GraphqlSizeExceeded
import com.spoolin.core.boundary.graphql.PolicyGraphql
import com.spoolin.core.features.account.application.AuthenticationDenied
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.policy.PolicyJson
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

@RestController
class BrowserGraphql(
    private val policy: PolicyProvider,
    private val graphql: PolicyGraphql,
    private val boundary: BrowserBoundary,
    private val requests: BrowserRequests,
    private val services: BrowserServices,
) {
    @RequestMapping("/graphql")
    fun execute(request: HttpServletRequest): DeferredResult<ResponseEntity<Map<String, Any?>>> {
        val started = System.nanoTime()
        val observation = GraphqlObservation(services.metrics.ifAvailable, started)
        return try {
            submit(request, started, observation)
        } catch (failure: BrowserRejected) {
            BrowserResponses.complete(failure(failure.status))
        } catch (_: GraphqlSizeExceeded) {
            BrowserResponses.complete(failure(BrowserBoundary.TOO_LARGE))
        } catch (_: graphql.parser.InvalidSyntaxException) {
            BrowserResponses.complete(failure(BrowserBoundary.BAD_REQUEST))
        } catch (_: IllegalArgumentException) {
            BrowserResponses.complete(failure(BrowserBoundary.BAD_REQUEST))
        }.also(observation::attach)
    }

    private fun submit(
        request: HttpServletRequest,
        started: Long,
        observation: GraphqlObservation,
    ): DeferredResult<ResponseEntity<Map<String, Any?>>> {
        boundary.check(request)
        val body = body(request)
        val parsed = GraphqlEnvelope(policy.policy).prepare(body.input)
        observation.roots = parsed.roots.toSet()
        val identity = parsed.roots.any { it in setOf("register", "signIn", "signOut", "session") }
        val budget = if (identity) IDENTITY_BUDGET_MS else METADATA_BUDGET_MS
        val elapsed =
            java.util.concurrent.TimeUnit.NANOSECONDS
                .toMillis(System.nanoTime() - started)
        val remaining = budget - elapsed
        return if (remaining <= 0) {
            BrowserResponses.complete(BrowserResponses.failure(BrowserBoundary.UNAVAILABLE, "DEADLINE_EXCEEDED"))
        } else {
            requests.run(remaining) { deadline -> dispatch(request, body, parsed, deadline) }
        }
    }

    private fun dispatch(
        request: HttpServletRequest,
        body: GraphqlBody,
        parsed: com.spoolin.core.boundary.graphql.PreparedGraphql,
        deadline: Long,
    ): ResponseEntity<Map<String, Any?>> =
        try {
            perform(request, body, parsed, deadline)
        } catch (failure: com.spoolin.core.features.ratelimit.RateLimited) {
            BrowserResponses.limited(failure.retryAfterMs)
        } catch (_: com.spoolin.core.features.ratelimit.RateUnavailable) {
            failure(BrowserBoundary.UNAVAILABLE)
        } catch (failure: BrowserRejected) {
            failure(failure.status)
        } catch (_: GraphqlSizeExceeded) {
            failure(BrowserBoundary.TOO_LARGE)
        } catch (_: AuthenticationDenied) {
            failure(BrowserBoundary.UNAVAILABLE)
        } catch (_: IllegalArgumentException) {
            failure(BrowserBoundary.BAD_REQUEST)
        }

    private fun perform(
        request: HttpServletRequest,
        body: GraphqlBody,
        parsed: com.spoolin.core.boundary.graphql.PreparedGraphql,
        deadline: Long,
    ): ResponseEntity<Map<String, Any?>> {
        val upload = parsed.roots.singleOrNull() == "uploadArtwork"
        val limit = policy.policy.requestLimits.getValue(if (upload) "uploadEnvelopeBytes" else "metadataBytes")
        if (body.size > limit) throw BrowserRejected(BrowserBoundary.TOO_LARGE)
        val identities = services.identity.ifAvailable ?: throw BrowserRejected(BrowserBoundary.UNAVAILABLE)
        val context =
            services.admission.ifAvailable?.context(request, parsed, identities, deadline)
                ?: unavailable()
        val result = graphql.execute(body.input, context)
        val headers = HttpHeaders()
        context.cookies().forEach { headers.add(HttpHeaders.SET_COOKIE, it) }
        retryAfter(result)?.let { headers.set("Retry-After", it) }
        return ResponseEntity.status(status(result)).headers(headers).body(result)
    }

    private fun unavailable(): Nothing =
        throw com.spoolin.core.features.ratelimit
            .RateUnavailable()

    private fun retryAfter(result: Map<String, Any?>): String? {
        val error = (result["errors"] as? List<*>)?.firstOrNull() as? Map<*, *>
        val milliseconds = (error?.get("extensions") as? Map<*, *>)?.get("retryAfterMs") as? Long
        return milliseconds?.let(BrowserResponses::retrySeconds)
    }

    private fun status(result: Map<String, Any?>): Int {
        val error = (result["errors"] as? List<*>)?.firstOrNull() as? Map<*, *>
        val code = (error?.get("extensions") as? Map<*, *>)?.get("code")
        return when (code) {
            "UNAVAILABLE", "DEADLINE_EXCEEDED" -> BrowserBoundary.UNAVAILABLE
            "RESOURCE_EXHAUSTED", "RATE_LIMITED" -> TOO_MANY_REQUESTS
            else -> HTTP_OK
        }
    }

    private fun body(request: HttpServletRequest): GraphqlBody {
        val maximum = policy.policy.requestLimits.getValue("uploadEnvelopeBytes")
        val bytes = request.inputStream.readNBytes(maximum + 1)
        if (bytes.size > maximum) throw BrowserRejected(BrowserBoundary.TOO_LARGE)
        val constraints =
            tools.jackson.core.StreamReadConstraints
                .builder()
                .maxNestingDepth(policy.policy.requestLimits.getValue("maxDepth") + JSON_ENVELOPE_DEPTH)
                .build()
        val factory =
            tools.jackson.core.json.JsonFactory
                .builder()
                .streamReadConstraints(constraints)
                .build()
        val mapper = JsonMapper.builder(factory).enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build()
        val input =
            try {
                requireNotNull(mapper.readValue(bytes, object : TypeReference<Map<String, Any?>>() {}))
            } catch (_: tools.jackson.core.JacksonException) {
                invalidBody()
            }
        try {
            PolicyJson.encode(input)
        } catch (_: IllegalStateException) {
            invalidBody()
        }
        return GraphqlBody(input, bytes.size)
    }

    private fun invalidBody(): Nothing = throw BrowserRejected(BrowserBoundary.BAD_REQUEST)

    private fun failure(status: Int): ResponseEntity<Map<String, Any?>> {
        val code =
            when (status) {
                BrowserBoundary.FORBIDDEN -> "FORBIDDEN"
                BrowserBoundary.UNAVAILABLE -> "UNAVAILABLE"
                BrowserBoundary.TOO_LARGE -> "RESOURCE_EXHAUSTED"
                else -> "FIELD_INVALID"
            }
        return BrowserResponses.failure(status, code)
    }
}

private const val HTTP_OK = 200
private const val TOO_MANY_REQUESTS = 429

private class GraphqlBody(
    val input: Map<String, Any?>,
    val size: Int,
)

private const val JSON_ENVELOPE_DEPTH = 4

private const val IDENTITY_BUDGET_MS = 5000L
private const val METADATA_BUDGET_MS = 3000L
