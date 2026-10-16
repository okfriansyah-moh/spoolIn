package com.spoolin.support.policy

private const val ESCAPED_CODE_POINT_BYTES = 6L

/** Counts complete envelopes without allocating strings at configured maxima. */
internal object RequestCapacity {
    fun validate(
        bounds: Map<String, TextBound>,
        limits: Map<String, Int>,
    ) {
        bounds.keys.forEach { field ->
            val envelope = RequestEnvelopes.forField(field)
            val query = envelope["query"] as? String
            if (query != null && query.toByteArray(Charsets.UTF_8).size > limits.getValue("graphqlQueryBytes")) {
                throw ConfigurationViolation("REQUEST_GRAPHQL_QUERY_BYTES", "Allow the complete typed request query.")
            }
            if (bytes(envelope, field, bounds) > limits.getValue("metadataBytes")) {
                throw ConfigurationViolation(
                    "${PolicySettings.key(field)}_MAX_CODE_POINTS",
                    "Keep the complete request, including required sibling fields, within REQUEST_METADATA_BYTES.",
                )
            }
        }
    }

    private fun bytes(
        value: Any?,
        candidate: String,
        bounds: Map<String, TextBound>,
    ): Long =
        when (value) {
            is CapacityText -> {
                val bound = bounds.getValue(value.field)
                val count = if (value.field == candidate) bound.maxCodePoints else bound.minCodePoints
                2L + ESCAPED_CODE_POINT_BYTES * count
            }
            is Map<*, *> ->
                2L +
                    value.entries.sumOf {
                        PolicyJson.encode(it.key).toByteArray(Charsets.UTF_8).size + 1L +
                            bytes(it.value, candidate, bounds)
                    } + (value.size - 1).coerceAtLeast(0)
            is List<*> -> 2L + value.sumOf { bytes(it, candidate, bounds) } + (value.size - 1).coerceAtLeast(0)
            else ->
                PolicyJson
                    .encode(value)
                    .toByteArray(Charsets.UTF_8)
                    .size
                    .toLong()
        }
}
