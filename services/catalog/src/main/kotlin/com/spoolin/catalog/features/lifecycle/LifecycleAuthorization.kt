package com.spoolin.catalog.features.lifecycle

import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import io.grpc.Status

internal object LifecycleAuthorization {
    fun management(): ActorClaims {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw Status.PERMISSION_DENIED.asRuntimeException()
        return claims
    }

    fun currentPolicy(
        requested: String,
        current: String,
    ) {
        if (requested != current) throw Status.ABORTED.withDescription("POLICY_CHANGED").asRuntimeException()
    }
}
