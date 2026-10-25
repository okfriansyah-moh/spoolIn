package com.spoolin.media.boundary.grpc

import com.spoolin.media.fixtures.actorId
import com.spoolin.media.fixtures.instant
import com.spoolin.media.fixtures.operationId
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorSigner
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin.Context
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.UploadChunk
import spoolin.v1.Spoolin.UploadHeader
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.time.Clock
import java.time.ZoneOffset
import java.util.Base64

class MediaActorAdmissionRulesTest {
    @TempDir lateinit var root: Path
    private fun environment(values: Map<String, Any>) = StandardEnvironment().apply {
        propertySources.addFirst(MapPropertySource("unit-actor-admission", values))
    }

    @Test
    fun `catalog reconciliation bypasses actor claims but upload and core protected reads do not`() {
        val interceptor = MediaActors.interceptor(environment(mapOf("spoolin.actor.enabled" to false)))
        for ((method, peer, permitted) in listOf(
            Triple("GetAsset", "catalog", true), Triple("GetOperation", "catalog", true),
            Triple("Upload", "catalog", false), Triple("GetAsset", "core", false), Triple("ReadAsset", "core", true),
        )) {
            val descriptor = MediaServiceGrpc.getUploadMethod().toBuilder().setFullMethodName("spoolin.v1.MediaService/$method").build()
            val call = CallBoundary(descriptor)
            var started = false
            MediaPeer.context(peer).run {
                interceptor.interceptCall(call, Metadata(), ServerCallHandler<UploadChunk, Outcome> { _, _ ->
                    started = true
                    object : ServerCall.Listener<UploadChunk>() {}
                })
            }
            assertEquals(permitted, started, "$peer/$method")
            assertEquals(if (permitted) null else Status.Code.PERMISSION_DENIED, call.closed?.code)
        }
    }

    @Test
    fun `signed upload admission permits maintainers and refuses viewers before business dispatch`() {
        val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val path = root.resolve("actor-public-keys.json")
        Files.writeString(path, "{\"unit\":\"${Base64.getEncoder().encodeToString(keys.public.encoded)}\"}")
        val interceptor = MediaActors.interceptor(environment(mapOf("spoolin.actor.enabled" to true, "ACTOR_VERIFY_KEYS_FILE" to path.toString())), Clock.fixed(instant, ZoneOffset.UTC))
        for (role in listOf("VIEWER", "MAINTAINER")) {
            val descriptor = MediaServiceGrpc.getUploadMethod()
            val call = CallBoundary(descriptor)
            val claims = ActorClaims("media", "/${descriptor.fullMethodName}", actorId, operationId, null, role, 1,
                instant.epochSecond, instant.epochSecond + 20, Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(16) { 5 }), operationId, operationId)
            val headers = Metadata().apply { put(ActorRpc.metadata, ActorSigner("unit", keys.private).sign(claims)) }
            var delivered = false
            val listener = interceptor.interceptCall(call, headers, ServerCallHandler<UploadChunk, Outcome> { _, _ ->
                object : ServerCall.Listener<UploadChunk>() { override fun onMessage(message: UploadChunk) { delivered = true } }
            })
            listener.onMessage(UploadChunk.newBuilder().setHeader(UploadHeader.newBuilder().setActorId(actorId.toString())
                .setContext(Context.newBuilder().setOperationId(operationId.toString()).setCorrelationId(operationId.toString()))).build())
            assertEquals(role == "MAINTAINER", delivered)
            if (role == "VIEWER") assertEquals(Status.Code.PERMISSION_DENIED, call.closed?.code) else assertNull(call.closed)
        }
    }

    private class CallBoundary(private val descriptor: MethodDescriptor<UploadChunk, Outcome>) : ServerCall<UploadChunk, Outcome>() {
        var closed: Status? = null
        override fun request(messages: Int) {}
        override fun sendHeaders(headers: Metadata) {}
        override fun sendMessage(message: Outcome) {}
        override fun close(status: Status, trailers: Metadata) { closed = status }
        override fun isCancelled(): Boolean = false
        override fun getMethodDescriptor(): MethodDescriptor<UploadChunk, Outcome> = descriptor
    }
}
