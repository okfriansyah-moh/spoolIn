package com.spoolin.support.observability

import io.grpc.Status

enum class MetricOutcome { SUCCESS, DOMAIN_REJECTED, SYSTEM_FAILED, UNCERTAIN }

enum class MetricCode {
    OK,
    FIELD_INVALID,
    INVALID_ARGUMENT,
    UNAUTHENTICATED,
    PERMISSION_DENIED,
    NOT_FOUND,
    CONFLICT,
    RATE_LIMITED,
    RESOURCE_EXHAUSTED,
    UNAVAILABLE,
    DEADLINE_EXCEEDED,
    CANCELLED,
    INTERNAL,
    UNKNOWN,
    PENDING,
    RESULT_EXPIRED,
    FAILED_PRECONDITION,
    DATA_LOSS,
    UNIMPLEMENTED,
}

data class LogicalResult private constructor(
    val outcome: MetricOutcome,
    val code: MetricCode,
) {
    companion object {
        fun success(): LogicalResult = LogicalResult(MetricOutcome.SUCCESS, MetricCode.OK)

        fun unknown(): LogicalResult = LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.UNKNOWN)

        private val problemCodes =
            listOf(
                setOf("FIELD_INVALID", "INVALID", "INVALID_ID") to domain(MetricCode.FIELD_INVALID),
                setOf("UNAUTHENTICATED", "AUTHENTICATION_REQUIRED") to domain(MetricCode.UNAUTHENTICATED),
                setOf("UNAUTHORISED", "FORBIDDEN", "PERMISSION_DENIED", "TV_WRITE_DENIED") to
                    domain(MetricCode.PERMISSION_DENIED),
                setOf("NOT_FOUND", "OPERATION_HIDDEN") to domain(MetricCode.NOT_FOUND),
                setOf("CONFLICT", "VERSION_CONFLICT", "IDEMPOTENCY_CONFLICT", "OPERATION_CONFLICT", "ROLE_EXISTS") to
                    domain(MetricCode.CONFLICT),
                setOf("POLICY_CHANGED", "POLICY_REVISION_MISMATCH", "IMPACT_CHANGED") to
                    domain(MetricCode.FAILED_PRECONDITION),
                setOf("RATE_LIMITED") to domain(MetricCode.RATE_LIMITED),
                setOf("RESOURCE_EXHAUSTED") to domain(MetricCode.RESOURCE_EXHAUSTED),
                setOf("UNAVAILABLE", "DEPENDENCY_UNAVAILABLE") to
                    LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.UNAVAILABLE),
                setOf("INTERNAL", "INTERNAL_ERROR", "SYSTEM_FAILED") to
                    LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.INTERNAL),
                setOf("DEADLINE_EXCEEDED") to LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.DEADLINE_EXCEEDED),
                setOf("CANCELLED") to LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.CANCELLED),
                setOf("PENDING") to LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.PENDING),
                setOf("RESULT_EXPIRED") to LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.RESULT_EXPIRED),
            ).flatMap { (names, result) -> names.map { it to result } }.toMap()
        private val statusCodes =
            mapOf(
                Status.Code.INVALID_ARGUMENT to domain(MetricCode.INVALID_ARGUMENT),
                Status.Code.UNAUTHENTICATED to domain(MetricCode.UNAUTHENTICATED),
                Status.Code.PERMISSION_DENIED to domain(MetricCode.PERMISSION_DENIED),
                Status.Code.NOT_FOUND to domain(MetricCode.NOT_FOUND),
                Status.Code.ABORTED to domain(MetricCode.CONFLICT),
                Status.Code.ALREADY_EXISTS to domain(MetricCode.CONFLICT),
                Status.Code.FAILED_PRECONDITION to domain(MetricCode.FAILED_PRECONDITION),
                Status.Code.OUT_OF_RANGE to domain(MetricCode.FAILED_PRECONDITION),
                Status.Code.RESOURCE_EXHAUSTED to domain(MetricCode.RESOURCE_EXHAUSTED),
                Status.Code.UNAVAILABLE to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.UNAVAILABLE),
                Status.Code.INTERNAL to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.INTERNAL),
                Status.Code.DATA_LOSS to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.DATA_LOSS),
                Status.Code.UNIMPLEMENTED to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.UNIMPLEMENTED),
            )

        fun fromProblem(code: String?): LogicalResult = problemCodes[code] ?: unknown()

        fun fromStatus(
            status: Status,
            mutation: Boolean,
        ): LogicalResult =
            when (status.code) {
                Status.Code.DEADLINE_EXCEEDED -> interruption(MetricCode.DEADLINE_EXCEEDED, mutation)
                Status.Code.CANCELLED -> interruption(MetricCode.CANCELLED, mutation)
                else -> statusCodes[status.code] ?: unknown()
            }

        fun fromGraphql(
            body: Map<*, *>?,
            httpStatus: Int,
            roots: Set<String>,
        ): LogicalResult = GraphqlResults.classify(body, httpStatus, roots)

        fun fromReceipt(
            state: String?,
            code: String?,
        ): LogicalResult =
            when (state) {
                "SUCCEEDED" -> if (code == null) success() else unknown()
                "FAILED" -> fromProblem(code)
                "PENDING" -> fromProblem("PENDING")
                "EXPIRED" -> fromProblem("RESULT_EXPIRED")
                else -> unknown()
            }

        internal fun message(
            operation: MetricOperation,
            value: Any?,
        ): LogicalResult = LogicalClassification.message(operation, value)

        private fun domain(code: MetricCode) = LogicalResult(MetricOutcome.DOMAIN_REJECTED, code)

        private fun interruption(
            code: MetricCode,
            mutation: Boolean,
        ) = LogicalResult(if (mutation) MetricOutcome.UNCERTAIN else MetricOutcome.SYSTEM_FAILED, code)
    }
}

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHENTICATED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_TOO_LARGE = 413
private const val HTTP_RATE_LIMITED = 429
private const val HTTP_SERVER_FAILURE = 500
private const val HTTP_UNAVAILABLE = 503
private const val HTTP_OK = 200

private object GraphqlResults {
    private val httpFailures =
        mapOf(
            HTTP_BAD_REQUEST to Status.INVALID_ARGUMENT,
            HTTP_UNAUTHENTICATED to Status.UNAUTHENTICATED,
            HTTP_FORBIDDEN to Status.PERMISSION_DENIED,
            HTTP_TOO_LARGE to Status.RESOURCE_EXHAUSTED,
            HTTP_RATE_LIMITED to Status.RESOURCE_EXHAUSTED,
            HTTP_UNAVAILABLE to Status.UNAVAILABLE,
        )

    fun classify(
        body: Map<*, *>?,
        status: Int,
        roots: Set<String>,
    ): LogicalResult =
        if (status != HTTP_OK) {
            LogicalResult.fromStatus(httpFailures[status] ?: httpFailure(status), true)
        } else {
            body?.takeIf { validRoots(roots) }?.let { classifyBody(it, roots) } ?: LogicalResult.unknown()
        }

    private fun validRoots(roots: Set<String>): Boolean =
        roots.isNotEmpty() && roots.all { MetricOperations.graphql(it) != null }

    private fun classifyBody(
        body: Map<*, *>,
        roots: Set<String>,
    ): LogicalResult {
        val errors = body["errors"]
        return if (errors != null && errors !is List<*>) {
            LogicalResult.unknown()
        } else {
            val applicable = (errors as? List<*>).orEmpty().filter { applies(it, roots) }
            if (applicable.isNotEmpty()) {
                worst(applicable.map(::error))
            } else {
                dataResults(body, roots)
            }
        }
    }

    private fun dataResults(
        body: Map<*, *>,
        roots: Set<String>,
    ): LogicalResult {
        val data = body["data"] as? Map<*, *>
        return data?.let { worst(roots.map { field -> root(it, field) }) } ?: LogicalResult.unknown()
    }

    private fun httpFailure(status: Int): Status =
        if (status >=
            HTTP_SERVER_FAILURE
        ) {
            Status.INTERNAL
        } else {
            Status.UNKNOWN
        }

    private fun applies(
        value: Any?,
        roots: Set<String>,
    ): Boolean {
        val error = value as? Map<*, *>
        val path = error?.get("path") as? List<*>
        val field = path?.firstOrNull() as? String
        return field?.let { it in roots } ?: true
    }

    private fun error(value: Any?): LogicalResult {
        val item = value as? Map<*, *>
        val extensions = item?.get("extensions") as? Map<*, *> ?: return LogicalResult.unknown()
        val code = extensions["code"] as? String ?: extensions["classification"] as? String
        return if (code == "ValidationError") {
            LogicalResult.fromStatus(Status.INVALID_ARGUMENT, false)
        } else {
            LogicalResult.fromProblem(code)
        }
    }

    private fun root(
        data: Map<*, *>,
        root: String,
    ): LogicalResult =
        if (!data.containsKey(root)) {
            LogicalResult.unknown()
        } else {
            val operation = requireNotNull(MetricOperations.graphql(root))
            if (operation.mutation) mutation(data[root]) else LogicalResult.success()
        }

    private fun mutation(value: Any?): LogicalResult {
        val item = value as? Map<*, *> ?: return LogicalResult.unknown()
        val direct = item["problem"] as? Map<*, *>
        return if (direct != null) {
            LogicalResult.fromProblem(direct["code"] as? String)
        } else {
            val receipt = item["operation"] as? Map<*, *>
            val problem = receipt?.get("problem") as? Map<*, *>
            LogicalResult.fromReceipt(receipt?.get("state") as? String, problem?.get("code") as? String)
        }
    }

    private fun worst(results: List<LogicalResult>): LogicalResult =
        results.maxByOrNull { it.outcome.ordinal } ?: LogicalResult.unknown()
}
