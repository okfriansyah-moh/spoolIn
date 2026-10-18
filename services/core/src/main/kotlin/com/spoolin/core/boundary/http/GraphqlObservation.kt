package com.spoolin.core.boundary.http

import com.spoolin.support.observability.LogicalMetrics
import com.spoolin.support.observability.LogicalResult
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.async.DeferredResult

/** Observes the actual completed response, including typed failures carried by HTTP 200. */
internal class GraphqlObservation(
    private val metrics: LogicalMetrics?,
    private val started: Long,
) {
    @Volatile var roots: Set<String> = emptySet()

    fun attach(deferred: DeferredResult<ResponseEntity<Map<String, Any?>>>) {
        deferred.onCompletion {
            val response = deferred.result as? ResponseEntity<*>
            val operation = roots.singleOrNull() ?: "GraphQL"
            val result = LogicalResult.fromGraphql(
                response?.body as? Map<*, *>, response?.statusCode?.value() ?: 0, roots,
            )
            metrics?.complete(operation, "ingress", result, started)
        }
    }
}
