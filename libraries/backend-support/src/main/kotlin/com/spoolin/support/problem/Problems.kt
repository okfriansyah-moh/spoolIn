package com.spoolin.support.problem

private const val MAXIMUM_RETRY_AFTER_MS = 86400000
private const val HTTP_OK = 200
private const val HTTP_SUCCESS_MAXIMUM = 299

enum class FailureKind(
    val code: String,
    val rpcStatus: String,
    val correction: String,
) {
    INVALID("FIELD_INVALID", "INVALID_ARGUMENT", "Correct the indicated input."),
    UNAUTHENTICATED("UNAUTHENTICATED", "UNAUTHENTICATED", "Sign in again."),
    DENIED("FORBIDDEN", "PERMISSION_DENIED", "This action is not permitted."),
    NOT_FOUND("NOT_FOUND", "NOT_FOUND", "The requested record is unavailable."),
    STALE("STALE_VERSION", "ABORTED", "Review the current version."),
    IMPACT("IMPACT_CHANGED", "ABORTED", "Review the changed consequences."),
    IDEMPOTENCY("IDEMPOTENCY_CONFLICT", "ALREADY_EXISTS", "Use the original request content."),
    CAPACITY("RESOURCE_EXHAUSTED", "RESOURCE_EXHAUSTED", "Try a smaller request or retry later."),
    DEPENDENCY("UNAVAILABLE", "UNAVAILABLE", "Try again when the service is available."),
    DEADLINE("DEADLINE_EXCEEDED", "DEADLINE_EXCEEDED", "Check the original operation before retrying."),
}

data class FieldIssue(
    val path: String,
    val code: String,
    val message: String,
) {
    fun projection(): Map<String, String> = mapOf("path" to path, "code" to code, "message" to message)
}

class SafeProblem private constructor(
    val code: String,
    val message: String,
    val fields: List<FieldIssue>,
    val retryAfterMs: Int?,
) {
    fun graphql(): Map<String, Any?> =
        mapOf(
            "code" to code,
            "message" to message,
            "fields" to fields.map { it.projection() },
            "retryAfterMs" to retryAfterMs,
        )

    fun protobuf(): Map<String, Any> =
        mapOf("code" to code, "fields" to fields.map { it.projection() }) +
            (retryAfterMs?.let { mapOf("retry_after_ms" to it) } ?: emptyMap())

    fun mcpFailure(): McpFailureProjection = McpFailureProjection.from(this)

    companion object {
        fun from(
            kind: FailureKind,
            field: String? = null,
            retryAfterMs: Int? = null,
        ): SafeProblem {
            require(retryAfterMs == null || retryAfterMs in 1..MAXIMUM_RETRY_AFTER_MS)
            val path = field?.takeIf { it.matches(Regex("[A-Za-z][A-Za-z0-9_.\\[\\]]{0,127}")) }
            val issues = path?.let { listOf(FieldIssue(it, kind.code, kind.correction)) } ?: emptyList()
            return SafeProblem(kind.code, kind.correction, issues, retryAfterMs)
        }
    }
}

object LogicalOutcome {
    fun succeeded(
        httpStatus: Int,
        problem: SafeProblem?,
        state: String? = null,
    ): Boolean = httpStatus in HTTP_OK..HTTP_SUCCESS_MAXIMUM && problem == null && state !in setOf("FAILED", "EXPIRED")
}

/** Internal classified failure; the MCP adapter owns protocol/tool-specific wrapping. */
class McpFailureProjection private constructor(
    private val problem: SafeProblem,
) {
    fun projection(): Map<String, Any?> =
        mapOf(
            "isError" to true,
            "problem" to problem.graphql(),
            "safeText" to problem.message,
        )

    companion object {
        fun from(problem: SafeProblem): McpFailureProjection = McpFailureProjection(problem)
    }
}
