package com.spoolin.media.boundary.grpc

import io.grpc.Context

/** Set only after the service certificate SAN passed the method allowlist. */
object MediaPeer {
    private val identity: Context.Key<String> = Context.key("media-verified-service")

    fun current(): String? = identity.get()

    fun context(service: String): Context = Context.current().withValue(identity, service)
}
