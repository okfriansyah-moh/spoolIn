package com.spoolin.core.boundary.http

import com.spoolin.core.boundary.graphql.GraphqlEnvelope
import com.spoolin.core.boundary.graphql.PolicyGraphql
import com.spoolin.core.features.account.application.AuthenticationDenied
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.policy.PolicyJson
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.parser.Parser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

@RestController
class BrowserGraphql(
    private val policy: PolicyProvider,
    private val graphql: PolicyGraphql,
    private val boundary: BrowserBoundary,
    private val identity: org.springframework.beans.factory.ObjectProvider<BrowserIdentity>,
) {
    @RequestMapping("/graphql")
    fun execute(request: HttpServletRequest): ResponseEntity<Map<String, Any?>> =
        try {
            boundary.check(request)
            val input = body(request)
            val parsed = GraphqlEnvelope(policy.policy).parse(input)
            val document = Parser().parseDocument(parsed.query)
            val operation = document.getDefinitionsOfType(OperationDefinition::class.java).single()
            val mutation = operation.operation == OperationDefinition.Operation.MUTATION
            val roots = operation.selectionSet.selections.filterIsInstance<Field>()
            val logoutOnly = roots.size == 1 && roots.single().name == "signOut"
            val identities = identity.ifAvailable ?: throw BrowserRejected(BrowserBoundary.UNAVAILABLE)
            val context = identities.context(request, mutation, logoutOnly)
            val result = graphql.execute(input, context)
            val headers = HttpHeaders()
            context.cookies().forEach { headers.add(HttpHeaders.SET_COOKIE, it) }
            ResponseEntity.status(status(result)).headers(headers).body(result)
        } catch (failure: BrowserRejected) {
            failure(failure.status)
        } catch (_: AuthenticationDenied) {
            failure(BrowserBoundary.UNAVAILABLE)
        } catch (_: IllegalArgumentException) {
            failure(BrowserBoundary.BAD_REQUEST)
        }

    private fun status(result: Map<String, Any?>): Int {
        val error = (result["errors"] as? List<*>)?.firstOrNull() as? Map<*, *>
        val code = (error?.get("extensions") as? Map<*, *>)?.get("code")
        return when (code) {
            "UNAVAILABLE" -> BrowserBoundary.UNAVAILABLE
            "RESOURCE_EXHAUSTED" -> TOO_MANY_REQUESTS
            else -> HTTP_OK
        }
    }

    private fun body(request: HttpServletRequest): Map<String, Any?> {
        val maximum = policy.policy.requestLimits.getValue("uploadEnvelopeBytes")
        val bytes = request.inputStream.readNBytes(maximum + 1)
        if (bytes.size > maximum) throw BrowserRejected(BrowserBoundary.TOO_LARGE)
        val mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build()
        val input =
            try {
                mapper.readValue(bytes, object : TypeReference<Map<String, Any?>>() {})
            } catch (_: tools.jackson.core.JacksonException) {
                throw BrowserRejected(BrowserBoundary.BAD_REQUEST)
            }
        PolicyJson.encode(input)
        return input
    }

    private fun failure(status: Int): ResponseEntity<Map<String, Any?>> =
        ResponseEntity.status(status).body(
            mapOf("data" to null, "errors" to listOf(mapOf("message" to "The request could not be completed."))),
        )
}

private const val HTTP_OK = 200
private const val TOO_MANY_REQUESTS = 429
