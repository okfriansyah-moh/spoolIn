package com.spoolin.catalog.boundary.policy

import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
import io.grpc.Status

/** Resolve and compare only inside a fresh owner mutation; receipt replays bypass this check. */
class CatalogWritePolicy(private val provider: () -> PublicPolicy = { environment() }) {
    internal fun <T> bind(input: T, revision: String): PolicyWrite<T> {
        val effective = effective()
        if (revision != effective.revision) throw Status.FAILED_PRECONDITION.withDescription("POLICY_CHANGED").asRuntimeException()
        return PolicyWrite(input, effective)
    }

    internal fun effective(): PublicPolicy = provider()

    companion object {
        fun environment(): PublicPolicy = PolicyEnvironment.resolve(System::getenv)
    }
}

internal data class PolicyWrite<T>(val input: T, val policy: PublicPolicy)
