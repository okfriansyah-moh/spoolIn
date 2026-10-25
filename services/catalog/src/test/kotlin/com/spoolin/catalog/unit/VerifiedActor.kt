package com.spoolin.catalog.unit

import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorVerifier
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import spoolin.v1.CatalogServiceGrpc
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64
import java.util.UUID

/** Real signed assertion admission, without a network server or a database. */
internal fun withMaintainer(
    methodName: String = "ManagedPerson",
    client: String? = null,
    block: () -> Unit,
) {
    val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    val descriptor =
        CatalogServiceGrpc
            .getManagedPersonMethod()
            .toBuilder()
            .setFullMethodName("spoolin.v1.CatalogService/$methodName")
            .build()
    val method = descriptor.fullMethodName
    val issued = Instant.now().epochSecond
    val claims =
        ActorClaims(
            "catalog",
            "/$method",
            UUID.fromString("00000000-0000-4000-8000-000000000001"),
            UUID.fromString("00000000-0000-4000-8000-000000000002"),
            client,
            "MAINTAINER",
            1,
            issued,
            issued + 30,
            Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(16) { 1 }),
            UUID.fromString("00000000-0000-4000-8000-000000000003"),
            null,
        )
    val headers = Metadata().apply { put(ActorRpc.metadata, ActorSigner("test", keys.private).sign(claims)) }
    val call =
        object : ServerCall<spoolin.v1.Spoolin.Id, spoolin.v1.Spoolin.Person>() {
            override fun request(count: Int) = Unit

            override fun sendHeaders(headers: Metadata) = Unit

            override fun sendMessage(message: spoolin.v1.Spoolin.Person) = Unit

            override fun close(
                status: Status,
                trailers: Metadata,
            ) {
                check(status.isOk) { status.toString() }
            }

            override fun isCancelled() = false

            override fun getMethodDescriptor() = descriptor
        }
    var invoked = false
    ActorInterceptor("catalog", setOf(method), ActorVerifier(mapOf("test" to keys.public)), {})
        .interceptCall(
            call,
            headers,
            ServerCallHandler { _, _ ->
                invoked = true
                block()
                object : ServerCall.Listener<spoolin.v1.Spoolin.Id>() {}
            },
        )
    check(invoked) { "Actor assertion was not admitted" }
}
