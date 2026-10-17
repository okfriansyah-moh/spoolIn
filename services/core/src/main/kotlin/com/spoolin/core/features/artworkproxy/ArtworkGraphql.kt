package com.spoolin.core.features.artworkproxy

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.support.receipts.OperationOwner
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import java.util.Base64
import java.util.UUID

/** GraphQL uploadArtwork / cancelArtwork authoritative boundary (T023 / ATDD-251-T1-R33). */
class ArtworkGraphql {
    private val maxBytes = 5_242_880L

    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Mutation") { m ->
            m.dataFetcher("uploadArtwork") { env -> uploadArtwork(env) }
                .dataFetcher("cancelArtwork") { env -> cancelArtwork(env) }
        }
    }

    private fun uploadArtwork(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = browser(env)
        if (context.principal == null) throw CredentialRejected()
        val content = env.getArgument<String>("content") ?: throw IllegalArgumentException("content required")
        val decoded = Base64.getDecoder().decode(content)
        if (decoded.isEmpty() || decoded.size.toLong() > maxBytes) {
            return mapOf(
                "operation" to
                    mapOf(
                        "id" to env.getArgument<String>("operationId"),
                        "state" to "FAILED",
                        "resultExpired" to false,
                        "problem" to mapOf("code" to "FILE_INVALID", "message" to "Artwork could not be accepted."),
                    ),
                "problem" to mapOf("code" to "FILE_INVALID", "message" to "Artwork could not be accepted."),
            )
        }
        val operationId = UUID.fromString(env.getArgument<String>("operationId"))
        context.rememberOperation(operationId, OperationOwner.CATALOG, mapOf("state" to "PENDING"))
        return mapOf(
            "operation" to
                mapOf(
                    "id" to operationId.toString(),
                    "state" to "PENDING",
                    "resultExpired" to false,
                ),
            "problem" to null,
        )
    }

    private fun cancelArtwork(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = browser(env)
        if (context.principal == null) throw CredentialRejected()
        return mapOf(
            "operation" to
                mapOf(
                    "id" to env.getArgument<String>("operationId"),
                    "state" to "SUCCEEDED",
                    "resultExpired" to false,
                ),
            "uploadOperation" to
                mapOf(
                    "id" to env.getArgument<String>("uploadOperationId"),
                    "state" to "CANCELLED",
                    "resultExpired" to false,
                ),
            "cancelled" to true,
        )
    }

    private fun browser(env: DataFetchingEnvironment): BrowserContext =
        requireNotNull(env.graphQlContext.get("browser"))
}
