package com.spoolin.core.boundary.grpc

import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.core.features.connector.ConnectorInsert
import com.spoolin.core.features.session.SessionCsrf
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.core.features.session.SessionStore
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class IdentityRpcBehaviorTest {
    private val account = UUID.randomUUID()
    private val credential = UUID.randomUUID()
    private val now = Instant.parse("2026-10-25T10:00:00Z")
    private val request = Spoolin.CheckActorRequest.newBuilder().setAudience("catalog").setAccountId(account.toString()).setCredentialId(credential.toString()).setAuthEpoch(3).build()

    @Test
    fun `session authority maps exact account credential role epoch and expiry`() {
        val db = SessionJdbcFixture(listOf(listOf(mapOf("role" to "MAINTAINER", "auth_epoch" to 3L, "expires" to Timestamp.from(now.plusSeconds(30))))))
        val sessions = SessionStore(db, SessionCsrf(ByteArray(32) { 1 }), Clock.fixed(now, ZoneOffset.UTC))
        val result = observe(IdentityRpc({ sessions }, { null }), request)
        assertNull(result.failure)
        assertEquals(account.toString(), result.value!!.accountId)
        assertEquals(credential.toString(), result.value!!.credentialId)
        assertEquals("MAINTAINER", result.value!!.role)
        assertEquals(3L, result.value!!.authEpoch)
        assertEquals(now.plusSeconds(30).epochSecond, result.value!!.expiresAt.seconds)
        assertTrue(result.completed)
    }

    @Test
    fun `connector fallback requires live credential and maps authoritative account role`() {
        val db = SessionJdbcFixture(listOf(listOf(row()), listOf(mapOf("role" to "MAINTAINER"))))
        val connector = ConnectorStore(db, Clock.fixed(now, ZoneOffset.UTC))
        val result = observe(IdentityRpc({ null }, { connector }), request)
        assertNull(result.failure)
        assertEquals("CHATGPT", result.value!!.clientKind)
        assertEquals("MAINTAINER", result.value!!.role)
        for (invalid in listOf(row() + ("revoked_at" to Timestamp.from(now)), row() + ("expires_at" to Timestamp.from(now)))) {
            db.enqueue(invalid)
            assertEquals(Status.Code.PERMISSION_DENIED, observe(IdentityRpc({ null }, { connector }), request).failure)
        }
        db.enqueue()
        assertEquals(Status.Code.PERMISSION_DENIED, observe(IdentityRpc({ null }, { connector }), request).failure)
    }

    @Test
    fun `malformed identity wrong audience negative epoch and unavailable stores fail safely`() {
        val rpc = IdentityRpc({ null }, { null })
        for (invalid in listOf(request.toBuilder().setAudience("media").build(), request.toBuilder().setAuthEpoch(-1).build(), request.toBuilder().setAccountId("invalid").build(), request.toBuilder().setCredentialId("UPPERCASE").build())) {
            val result = observe(rpc, invalid)
            assertEquals(Status.Code.PERMISSION_DENIED, result.failure)
            assertNull(result.value)
            assertFalse(result.completed)
        }
        assertEquals(Status.Code.UNAVAILABLE, observe(rpc, request).failure)
        assertEquals(Status.Code.UNAVAILABLE, observe(IdentityRpc({ error("database error") }, { null }), request).failure)
    }

    @Test
    fun `connector token lookup insert and liveness retain strict token bounds`() {
        val db = SessionJdbcFixture(listOf(listOf(row())))
        val store = ConnectorStore(db, Clock.fixed(now, ZoneOffset.UTC))
        val record = store.findByTokenHash(ByteArray(32))!!
        assertEquals(account, record.accountId)
        assertTrue(store.live(record))
        assertThrows(IllegalArgumentException::class.java) { store.findByTokenHash(ByteArray(31)) }
        val insertion = ConnectorInsert(credential, ByteArray(32), account, "CHATGPT", 3, now.plusSeconds(3600))
        store.insert(insertion)
        assertEquals(1, db.updates)
        assertThrows(IllegalArgumentException::class.java) { store.insert(insertion.copy(clientKind = "UNKNOWN")) }
        assertThrows(IllegalArgumentException::class.java) { store.insert(insertion.copy(tokenHash = ByteArray(33))) }
        db.enqueue()
        assertNull(store.accountRole(account))
    }

    private fun row(): Map<String, Any?> = mapOf("id" to credential, "account_id" to account, "client_kind" to "CHATGPT", "auth_epoch" to 3L, "scope" to "movie:propose movie:approve", "expires_at" to Timestamp.from(now.plusSeconds(3600)), "revoked_at" to null)
    private fun observe(rpc: IdentityRpc, input: Spoolin.CheckActorRequest): Result {
        val result = Result()
        rpc.checkActor(input, object : StreamObserver<Spoolin.Actor> {
            override fun onNext(value: Spoolin.Actor) { result.value = value }
            override fun onError(failure: Throwable) { result.failure = Status.fromThrowable(failure).code }
            override fun onCompleted() { result.completed = true }
        })
        return result
    }
    private class Result(var value: Spoolin.Actor? = null, var failure: Status.Code? = null, var completed: Boolean = false)
}
