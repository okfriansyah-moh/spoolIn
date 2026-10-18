package com.spoolin.catalog.boundary.grpc

import com.google.protobuf.Timestamp
import io.grpc.Status
import spoolin.v1.Spoolin.FieldSource
import tools.jackson.core.StreamReadConstraints
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.time.Instant

/** Projects only declared public source facts, never arbitrary provenance-ledger properties. */
internal object FieldSourceProjection {
    private const val MAXIMUM_BYTES = 16_384
    private const val MAXIMUM_FIELDS = 64
    private const val MAXIMUM_DEPTH = 4
    private const val MAXIMUM_URL_LENGTH = 2048
    private const val MAXIMUM_TIMESTAMP_LENGTH = 64
    private const val MAXIMUM_REUSE_BASIS_LENGTH = 4000
    private const val MAXIMUM_PRECISION_LENGTH = 16
    private val mapper =
        JsonMapper
            .builder(
                JsonFactory
                    .builder()
                    .streamReadConstraints(
                        StreamReadConstraints
                            .builder()
                            .maxNestingDepth(MAXIMUM_DEPTH)
                            .maxStringLength(MAXIMUM_BYTES)
                            .build(),
                    ).build(),
            ).enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build()

    fun parse(json: String): List<FieldSource> =
        try {
            require(json.toByteArray(Charsets.UTF_8).size <= MAXIMUM_BYTES)
            val fields = mapper.readTree(json)
            require(fields.isObject && fields.size() <= MAXIMUM_FIELDS)
            fields.propertyNames().sorted().map { field -> source(field, fields[field]) }
        } catch (_: Exception) {
            throw Status.DATA_LOSS.asRuntimeException()
        }

    private fun source(
        field: String,
        node: JsonNode,
    ): FieldSource {
        require(field in PUBLIC_FIELDS && node.isObject)
        val url = text(node, "url", MAXIMUM_URL_LENGTH)
        val uri = URI(url)
        require(uri.scheme == "https" && uri.host != null && uri.userInfo == null)
        val at = Instant.parse(text(node, "retrievedAt", MAXIMUM_TIMESTAMP_LENGTH))
        val basis = if (node.has("permissionBasis")) "permissionBasis" else "reuseBasis"
        val builder =
            FieldSource
                .newBuilder()
                .setField(field)
                .setUrl(url)
                .setReuseBasis(text(node, basis, MAXIMUM_REUSE_BASIS_LENGTH))
                .setRetrievedAt(Timestamp.newBuilder().setSeconds(at.epochSecond).setNanos(at.nano))
        if (node.has("precision")) {
            val precision = text(node, "precision", MAXIMUM_PRECISION_LENGTH)
            require(precision in setOf("YEAR", "MONTH", "DAY", "UNKNOWN"))
            builder.precision = precision
        }
        return builder.build()
    }

    private fun text(
        node: JsonNode,
        key: String,
        maximum: Int,
    ): String {
        val value = node.path(key)
        require(value.isString)
        return value.asText().also { text ->
            require(text.isNotBlank() && text.length <= maximum && text.none { it.isISOControl() })
        }
    }

    private val PUBLIC_FIELDS =
        setOf(
            "title",
            "artwork",
            "originalTitle",
            "synopsis",
            "genres",
            "release",
            "runtimeMinutes",
            "originalLanguage",
            "synopsisLanguage",
            "countries",
            "tvStatus",
            "tvEnd",
            "end",
            "releaseContext",
            "name",
            "biography",
            "category",
            "allowsCharacters",
            "personId",
            "titleId",
            "roleId",
            "characters",
        )
}
