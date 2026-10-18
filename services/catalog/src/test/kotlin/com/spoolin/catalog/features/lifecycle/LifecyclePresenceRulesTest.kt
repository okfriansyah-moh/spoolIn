package com.spoolin.catalog.features.lifecycle

import com.spoolin.support.identity.ActorClaims
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.util.UUID

class LifecyclePresenceRulesTest {
    private val operation = UUID.randomUUID()
    private val correlation = UUID.randomUUID()
    private val target = UUID.randomUUID()

    @Test
    fun `missing version is rejected while an explicitly encoded zero round trips`() {
        val missing = versioned()
        assertFalse(Spoolin.VersionedId.parseFrom(missing.toByteArray()).hasExpectedVersion())
        val invalid = assertThrows(StatusRuntimeException::class.java) {
            LifecycleCommand.parse(lifecycle(missing), actor())
        }
        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.status.code)
        val explicit = missing.toBuilder().setExpectedVersion(0).build()
        val encoded = Spoolin.VersionedId.parseFrom(explicit.toByteArray())
        assertTrue(encoded.hasExpectedVersion())
        assertEquals(0L, LifecycleCommand.parse(lifecycle(encoded), actor()).expectedVersion)
    }

    @Test
    fun `credit zero and omitted language retain independent native presence`() {
        val missingCredit = Spoolin.CreditWrite.getDefaultInstance()
        assertFalse(missingCredit.hasExpectedVersion())
        val credit = Spoolin.CreditWrite.parseFrom(missingCredit.toBuilder().setExpectedVersion(0).build().toByteArray())
        assertTrue(credit.hasExpectedVersion())
        val absent = Spoolin.MovieValues.getDefaultInstance()
        assertFalse(absent.hasSynopsisLanguage())
        val empty = Spoolin.MovieValues.parseFrom(absent.toBuilder().setSynopsisLanguage("").build().toByteArray())
        assertTrue(empty.hasSynopsisLanguage())
        assertEquals("", empty.synopsisLanguage)
    }

    private fun versioned(): Spoolin.VersionedId = Spoolin.VersionedId.newBuilder()
        .setContext(Spoolin.Context.newBuilder().setOperationId(operation.toString())
            .setCorrelationId(correlation.toString()).setPolicyRevision("test"))
        .setId(target.toString()).build()

    private fun lifecycle(target: Spoolin.VersionedId): Spoolin.Lifecycle = Spoolin.Lifecycle.newBuilder()
        .setTarget(target).setAction("ARCHIVE").build()

    private fun actor(): ActorClaims = ActorClaims(
        "catalog", "/spoolin.v1.CatalogService/MovieLifecycle", UUID.randomUUID(), UUID.randomUUID(),
        null, "MAINTAINER", 0, 1, 30, "AAAAAAAAAAAAAAAAAAAAAA", correlation, operation,
    )
}
