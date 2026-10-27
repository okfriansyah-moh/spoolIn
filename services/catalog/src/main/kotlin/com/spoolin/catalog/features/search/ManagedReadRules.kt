package com.spoolin.catalog.features.search

import io.grpc.Status

internal object ManagedReadRules {
    fun state(value: String): String {
        if (value !in setOf("ACTIVE", "ARCHIVED")) {
            throw Status.INVALID_ARGUMENT.withDescription("INVALID_STATE").asRuntimeException()
        }
        return value
    }
}
