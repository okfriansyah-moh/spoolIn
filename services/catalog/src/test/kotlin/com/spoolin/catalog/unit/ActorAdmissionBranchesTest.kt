package com.spoolin.catalog.unit

import com.spoolin.catalog.boundary.grpc.CatalogActors
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorRejected
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin

class ActorAdmissionBranchesTest {
    private fun claims() = ActorClaims("catalog", "/spoolin.v1.CatalogService/WriteMovie", unitActor, unitCredential,
        null, "MAINTAINER", 1, 1, 31, "nonce", unitCorrelation, unitOperation)
    private fun current() = Spoolin.Actor.newBuilder().setAccountId(unitActor.toString()).setCredentialId(unitCredential.toString())
        .setAuthEpoch(1).setRole("MAINTAINER").setExpiresAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(unitNow.plusSeconds(60).epochSecond)).build()

    @Test
    fun `live actor admission binds account credential epoch role client and expiry`() {
        val channel = GrpcReplies(current())
        CatalogActors(IdentityServiceGrpc.newBlockingStub(channel), unitClock).check(claims())
        val request = channel.requests.single() as Spoolin.CheckActorRequest
        assertEquals("catalog", request.audience)
        assertEquals(unitActor.toString(), request.accountId)
        val invalid = listOf(current().toBuilder().setAccountId(unitAsset.toString()).build(),
            current().toBuilder().setCredentialId(unitAsset.toString()).build(), current().toBuilder().setAuthEpoch(2).build(),
            current().toBuilder().setRole("VIEWER").build(), current().toBuilder().setClientKind("CLAUDE").build(),
            current().toBuilder().setExpiresAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(unitNow.epochSecond)).build())
        for (actor in invalid) {
            assertThrows(ActorRejected::class.java) {
                CatalogActors(IdentityServiceGrpc.newBlockingStub(GrpcReplies(actor)), unitClock).check(claims())
            }
        }
        val connector = current().toBuilder().setClientKind("CLAUDE").build()
        CatalogActors(IdentityServiceGrpc.newBlockingStub(GrpcReplies(connector)), unitClock).check(claims().copy(clientKind = "CLAUDE"))
    }

    @Test
    fun `viewer can inspect its operation while management mutations remain denied`() {
        val actor = current().toBuilder().setRole("VIEWER").build()
        val denied = GrpcReplies()
        assertThrows(ActorRejected::class.java) {
            CatalogActors(IdentityServiceGrpc.newBlockingStub(denied), unitClock).check(claims().copy(role = "VIEWER"))
        }
        assertEquals(0, denied.requests.size)
        CatalogActors(IdentityServiceGrpc.newBlockingStub(GrpcReplies(actor)), unitClock).check(
            claims().copy(role = "VIEWER", method = "/spoolin.v1.CatalogService/GetOperation"),
        )
    }
}
