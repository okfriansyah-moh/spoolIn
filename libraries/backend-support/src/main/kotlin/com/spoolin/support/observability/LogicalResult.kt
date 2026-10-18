package com.spoolin.support.observability

import com.google.protobuf.Message
import io.grpc.Status

enum class MetricOutcome { SUCCESS, DOMAIN_REJECTED, SYSTEM_FAILED, UNCERTAIN }
enum class MetricCode {
    OK, FIELD_INVALID, INVALID_ARGUMENT, UNAUTHENTICATED, PERMISSION_DENIED, NOT_FOUND, CONFLICT,
    RATE_LIMITED, RESOURCE_EXHAUSTED, UNAVAILABLE, DEADLINE_EXCEEDED, CANCELLED, INTERNAL, UNKNOWN,
    PENDING, RESULT_EXPIRED, FAILED_PRECONDITION, DATA_LOSS, UNIMPLEMENTED,
}

data class LogicalResult private constructor(val outcome: MetricOutcome, val code: MetricCode) {
    companion object {
        fun success(): LogicalResult = LogicalResult(MetricOutcome.SUCCESS, MetricCode.OK)
        fun unknown(): LogicalResult = LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.UNKNOWN)

        private val problemCodes = listOf(
            setOf("FIELD_INVALID", "INVALID", "INVALID_ID") to domain(MetricCode.FIELD_INVALID),
            setOf("UNAUTHENTICATED", "AUTHENTICATION_REQUIRED") to domain(MetricCode.UNAUTHENTICATED),
            setOf("UNAUTHORISED", "FORBIDDEN", "PERMISSION_DENIED", "TV_WRITE_DENIED") to domain(MetricCode.PERMISSION_DENIED),
            setOf("NOT_FOUND", "OPERATION_HIDDEN") to domain(MetricCode.NOT_FOUND),
            setOf("CONFLICT", "VERSION_CONFLICT", "IDEMPOTENCY_CONFLICT", "OPERATION_CONFLICT", "ROLE_EXISTS") to domain(MetricCode.CONFLICT),
            setOf("POLICY_CHANGED", "POLICY_REVISION_MISMATCH", "IMPACT_CHANGED") to domain(MetricCode.FAILED_PRECONDITION),
            setOf("RATE_LIMITED") to domain(MetricCode.RATE_LIMITED),
            setOf("RESOURCE_EXHAUSTED") to domain(MetricCode.RESOURCE_EXHAUSTED),
            setOf("UNAVAILABLE", "DEPENDENCY_UNAVAILABLE") to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.UNAVAILABLE),
            setOf("INTERNAL", "INTERNAL_ERROR", "SYSTEM_FAILED") to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.INTERNAL),
            setOf("DEADLINE_EXCEEDED") to LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.DEADLINE_EXCEEDED),
            setOf("CANCELLED") to LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.CANCELLED),
            setOf("PENDING") to LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.PENDING),
            setOf("RESULT_EXPIRED") to LogicalResult(MetricOutcome.UNCERTAIN, MetricCode.RESULT_EXPIRED),
        ).flatMap { (names, result) -> names.map { it to result } }.toMap()
        private val statusCodes = mapOf(
            Status.Code.INVALID_ARGUMENT to domain(MetricCode.INVALID_ARGUMENT),
            Status.Code.UNAUTHENTICATED to domain(MetricCode.UNAUTHENTICATED),
            Status.Code.PERMISSION_DENIED to domain(MetricCode.PERMISSION_DENIED),
            Status.Code.NOT_FOUND to domain(MetricCode.NOT_FOUND),
            Status.Code.ABORTED to domain(MetricCode.CONFLICT), Status.Code.ALREADY_EXISTS to domain(MetricCode.CONFLICT),
            Status.Code.FAILED_PRECONDITION to domain(MetricCode.FAILED_PRECONDITION),
            Status.Code.OUT_OF_RANGE to domain(MetricCode.FAILED_PRECONDITION),
            Status.Code.RESOURCE_EXHAUSTED to domain(MetricCode.RESOURCE_EXHAUSTED),
            Status.Code.UNAVAILABLE to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.UNAVAILABLE),
            Status.Code.INTERNAL to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.INTERNAL),
            Status.Code.DATA_LOSS to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.DATA_LOSS),
            Status.Code.UNIMPLEMENTED to LogicalResult(MetricOutcome.SYSTEM_FAILED, MetricCode.UNIMPLEMENTED),
        )

        fun fromProblem(code: String?): LogicalResult = problemCodes[code] ?: unknown()

        fun fromStatus(status: Status, mutation: Boolean): LogicalResult = when (status.code) {
            Status.Code.DEADLINE_EXCEEDED -> interruption(MetricCode.DEADLINE_EXCEEDED, mutation)
            Status.Code.CANCELLED -> interruption(MetricCode.CANCELLED, mutation)
            else -> statusCodes[status.code] ?: unknown()
        }

        fun fromGraphql(body: Map<*, *>?, httpStatus: Int, roots: Set<String>): LogicalResult =
            GraphqlResults.classify(body, httpStatus, roots)

        fun fromReceipt(state: String?, code: String?): LogicalResult = when (state) {
            "SUCCEEDED" -> if (code == null) success() else unknown()
            "FAILED" -> fromProblem(code)
            "PENDING" -> fromProblem("PENDING")
            "EXPIRED" -> fromProblem("RESULT_EXPIRED")
            else -> unknown()
        }

        internal fun message(operation: MetricOperation, value: Any?): LogicalResult {
            if (value !is Message || value.descriptorForType.fullName != operation.response) return unknown()
            if (operation.receiptLookup) return lookup(value)
            return when (operation.response) {
                "spoolin.v1.Outcome" -> receipt(value)
                "spoolin.v1.ArtworkCancelOutcome" -> child(value, "operation")?.let(::receipt) ?: unknown()
                else -> problem(value)?.let { fromProblem(it) } ?: knownReply(value)
            }
        }

        private val collectionReplies = setOf("TitlePage", "PersonPage", "CreditPage", "Genres", "Roles", "AuditPage", "Impact")
        private val identityFields = mapOf("Title" to "id", "Person" to "id", "Asset" to "id",
            "Attachment" to "hold_id", "ArtworkGrant" to "asset_id", "ProposalRevision" to "proposal_id")

        private fun knownReply(message: Message): LogicalResult {
            val name = message.descriptorForType.name
            if (name in collectionReplies) return success()
            val identity = identityFields[name]?.let { string(message, it) }
            val known = when (name) {
                in identityFields -> !identity.isNullOrBlank()
                "PolicySnapshot" -> !string(message, "revision").isNullOrBlank()
                "MovieContext" -> child(message, "policy") != null
                "AssetReadChunk" -> message.allFields.isNotEmpty()
                else -> false
            }
            return if (known) success() else unknown()
        }

        private fun lookup(message: Message): LogicalResult {
            val known = !string(message, "operation_id").isNullOrBlank() &&
                string(message, "state") in setOf("SUCCEEDED", "FAILED", "PENDING", "EXPIRED")
            return if (known) success() else unknown()
        }

        private fun receipt(message: Message): LogicalResult = fromReceipt(string(message, "state"), problem(message))
        private fun problem(message: Message): String? = child(message, "problem")?.let { string(it, "code") }
        private fun child(message: Message, name: String): Message? {
            val field = message.descriptorForType.findFieldByName(name) ?: return null
            return if (message.hasField(field)) message.getField(field) as? Message else null
        }
        private fun string(message: Message, name: String): String? =
            message.descriptorForType.findFieldByName(name)?.let { message.getField(it) as? String }
        private fun domain(code: MetricCode) = LogicalResult(MetricOutcome.DOMAIN_REJECTED, code)
        private fun interruption(code: MetricCode, mutation: Boolean) =
            LogicalResult(if (mutation) MetricOutcome.UNCERTAIN else MetricOutcome.SYSTEM_FAILED, code)
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
    private val httpFailures = mapOf(HTTP_BAD_REQUEST to Status.INVALID_ARGUMENT,
        HTTP_UNAUTHENTICATED to Status.UNAUTHENTICATED, HTTP_FORBIDDEN to Status.PERMISSION_DENIED,
        HTTP_TOO_LARGE to Status.RESOURCE_EXHAUSTED, HTTP_RATE_LIMITED to Status.RESOURCE_EXHAUSTED,
        HTTP_UNAVAILABLE to Status.UNAVAILABLE)

    fun classify(body: Map<*, *>?, status: Int, roots: Set<String>): LogicalResult {
        if (status != HTTP_OK) return LogicalResult.fromStatus(httpFailures[status] ?: httpFailure(status), true)
        if (body == null || roots.isEmpty() || roots.any { MetricOperations.graphql(it) == null }) return LogicalResult.unknown()
        val errors = body["errors"]
        if (errors != null && errors !is List<*>) return LogicalResult.unknown()
        val applicable = (errors as? List<*>).orEmpty().filter { applies(it, roots) }
        if (applicable.isNotEmpty()) return worst(applicable.map(::error))
        val data = body["data"] as? Map<*, *> ?: return LogicalResult.unknown()
        return worst(roots.map { field -> root(data, field) })
    }

    private fun httpFailure(status: Int): Status = if (status >= HTTP_SERVER_FAILURE) Status.INTERNAL else Status.UNKNOWN

    private fun applies(value: Any?, roots: Set<String>): Boolean {
        val error = value as? Map<*, *> ?: return true
        val path = error["path"] as? List<*> ?: return true
        val field = path.firstOrNull() as? String ?: return true
        return field in roots
    }

    private fun error(value: Any?): LogicalResult {
        val item = value as? Map<*, *> ?: return LogicalResult.unknown()
        val extensions = item["extensions"] as? Map<*, *> ?: return LogicalResult.unknown()
        val code = extensions["code"] as? String ?: extensions["classification"] as? String
        return if (code == "ValidationError") LogicalResult.fromStatus(Status.INVALID_ARGUMENT, false)
        else LogicalResult.fromProblem(code)
    }

    private fun root(data: Map<*, *>, root: String): LogicalResult {
        if (!data.containsKey(root)) return LogicalResult.unknown()
        val operation = requireNotNull(MetricOperations.graphql(root))
        if (!operation.mutation) return LogicalResult.success()
        val value = data[root] as? Map<*, *> ?: return LogicalResult.unknown()
        val direct = value["problem"] as? Map<*, *>
        if (direct != null) return LogicalResult.fromProblem(direct["code"] as? String)
        val receipt = value["operation"] as? Map<*, *> ?: return LogicalResult.unknown()
        val problem = receipt["problem"] as? Map<*, *>
        return LogicalResult.fromReceipt(receipt["state"] as? String, problem?.get("code") as? String)
    }

    private fun worst(results: List<LogicalResult>): LogicalResult =
        results.maxByOrNull { it.outcome.ordinal } ?: LogicalResult.unknown()
}
