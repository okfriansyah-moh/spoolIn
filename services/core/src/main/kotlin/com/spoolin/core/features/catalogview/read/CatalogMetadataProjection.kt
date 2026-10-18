package com.spoolin.core.features.catalogview.read

import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import io.grpc.Status
import spoolin.v1.Spoolin
import java.time.Instant

/** Resolves declared metadata from actual owner responses, once per GraphQL request. */
class CatalogMetadataProjection(
    private val loadGenres: () -> Spoolin.Genres,
) {
    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Genre") { type -> type.dataFetcher("label") { env -> label(env) } }
    }

    private fun label(env: DataFetchingEnvironment): String {
        val source = env.getSource<Map<String, Any?>>()
        val supplied = source?.get("label") as? String
        if (!supplied.isNullOrBlank()) return supplied
        val id = source?.get("id") as? String
        val labels =
            synchronized(env.graphQlContext) {
                env.graphQlContext.get<Map<String, String>>(GENRE_CACHE) ?: loadGenres()
                    .nodesList
                    .associate { it.id to it.label }
                    .also { env.graphQlContext.put(GENRE_CACHE, it) }
            }
        return labels[id]?.takeIf { it.isNotBlank() } ?: throw Status.DATA_LOSS.asRuntimeException()
    }

    companion object {
        private const val GENRE_CACHE = "spoolin.catalog.genreLabels"

        fun fieldSources(provenance: Spoolin.Provenance): Map<String, Any?> =
            provenance.fieldSourcesList.associate { source ->
                source.field to
                    mapOf(
                        "url" to source.url,
                        "reuseBasis" to source.reuseBasis,
                        "retrievedAt" to
                            Instant
                                .ofEpochSecond(
                                    source.retrievedAt.seconds,
                                    source.retrievedAt.nanos.toLong(),
                                ).toString(),
                        "precision" to source.precision.takeIf { source.hasPrecision() },
                    )
            }
    }
}
