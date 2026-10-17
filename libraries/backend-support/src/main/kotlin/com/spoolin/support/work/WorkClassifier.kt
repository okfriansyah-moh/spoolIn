package com.spoolin.support.work

/**
 * Outcome of a single durable-work attempt written to operational_event.attempt_outcome.
 *
 * Values must match the operational_event CHECK constraint:
 *   attempt_outcome IN ('SUCCEEDED','TRANSIENT','PERMANENT','UNCERTAIN','CANCELLED')
 */
enum class AttemptOutcome(
    val code: String,
) {
    SUCCEEDED("SUCCEEDED"),
    TRANSIENT("TRANSIENT"),
    PERMANENT("PERMANENT"),
    UNCERTAIN("UNCERTAIN"),
    CANCELLED("CANCELLED"),
}

/**
 * Recognised worker failure kinds.
 *
 * PERMANENT failures stop auto-retry and leave the work EXHAUSTED pending operator review.
 * TRANSIENT failures schedule a retry within the saved budget.
 * UNCERTAIN failures (possible post-send commit) require destination reconciliation before replay.
 */
enum class WorkError(
    val errorCode: String,
) {
    INVALID_ARGUMENT("INVALID_ARGUMENT"),
    PERMISSION_DENIED("PERMISSION_DENIED"),
    NOT_FOUND("NOT_FOUND"),
    STALE_VERSION("STALE_VERSION"),
    UNAVAILABLE_BEFORE_SEND("UNAVAILABLE"),
    RATE_LIMITED("RATE_LIMITED"),
    TIMEOUT_AFTER_SEND("TIMEOUT_AFTER_SEND"),
    UNKNOWN_PROVIDER("UNKNOWN_PROVIDER"),
}

/**
 * Pure failure classifier for work attempt outcomes.
 *
 * Permanent failures (invalid input, authorization, staleness) never enter automatic retry.
 * Safe transient failures (unavailable before send, rate-limited) may be rescheduled within the
 * persisted budget and deadline. Uncertain failures (timeout after send, unqueryable provider)
 * require destination-receipt reconciliation before another send.
 */
object WorkClassifier {
    fun classify(error: WorkError): AttemptOutcome =
        when (error) {
            WorkError.INVALID_ARGUMENT,
            WorkError.PERMISSION_DENIED,
            WorkError.NOT_FOUND,
            WorkError.STALE_VERSION,
            -> AttemptOutcome.PERMANENT

            WorkError.UNAVAILABLE_BEFORE_SEND,
            WorkError.RATE_LIMITED,
            -> AttemptOutcome.TRANSIENT

            WorkError.TIMEOUT_AFTER_SEND,
            WorkError.UNKNOWN_PROVIDER,
            -> AttemptOutcome.UNCERTAIN
        }

    fun isPermanent(error: WorkError): Boolean = classify(error) == AttemptOutcome.PERMANENT

    fun isSafeRetry(error: WorkError): Boolean = classify(error) == AttemptOutcome.TRANSIENT

    fun isUncertain(error: WorkError): Boolean = classify(error) == AttemptOutcome.UNCERTAIN
}
