package com.spoolin.catalog.unit

import com.spoolin.catalog.features.lifecycle.DeletionImpact
import com.spoolin.catalog.features.lifecycle.ImpactSnapshot
import com.spoolin.catalog.features.lifecycle.ImpactToken
import com.spoolin.catalog.features.lifecycle.LifecycleCommand
import com.spoolin.catalog.features.lifecycle.LifecycleRules
import com.spoolin.catalog.features.lifecycle.LifecycleStore
import io.grpc.Context
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class DeletionImpactRulesTest {
    private fun subject() = listOf(mapOf("status" to "ARCHIVED", "version" to 7L, "impact_version" to 3L))

    private fun credit() =
        listOf(
            mapOf(
                "id" to unitAsset,
                "version" to 1L,
                "title_id" to unitTarget,
                "title_version" to 2L,
                "display_title" to "River",
                "kind" to "MOVIE",
                "status" to "ACTIVE",
            ),
        )

    private fun signer() = ImpactToken("test", ByteArray(32) { 1 }, unitClock)

    @Test fun `deletion preview signs full consequence set including retained title label`() =
        withMaintainer {
            val db = JdbcRows(subject(), credit())
            val result = RpcResult<Spoolin.Impact>()
            DeletionImpact(db.source, signer()).preview(
                Spoolin.Id
                    .newBuilder()
                    .setId(unitActor.toString())
                    .build(),
                result,
            )
            val impact = result.value()
            assertEquals(1, impact.affectedCreditCount)
            assertEquals(listOf("River"), impact.affectedTitlesList)
            assertEquals(7L, impact.version)
            assertEquals(unitActor.toString(), impact.targetId)
            assertFalse(impact.token.isBlank())
            assertEquals(unitNow.plusSeconds(300).epochSecond, impact.expiresAt.seconds)
        }

    @Test fun `deletion preview denies absent actor malformed id absent subject and missing signer`() {
        val denied = RpcResult<Spoolin.Impact>()
        DeletionImpact(JdbcRows().source, signer()).preview(Spoolin.Id.getDefaultInstance(), denied)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(denied.failure).code)
        withMaintainer {
            val invalid = RpcResult<Spoolin.Impact>()
            DeletionImpact(JdbcRows().source, signer()).preview(
                Spoolin.Id
                    .newBuilder()
                    .setId("bad")
                    .build(),
                invalid,
            )
            assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(invalid.failure).code)
            val absent = RpcResult<Spoolin.Impact>()
            DeletionImpact(
                JdbcRows(emptyList()).source,
                signer(),
            ).preview(
                Spoolin.Id
                    .newBuilder()
                    .setId(unitActor.toString())
                    .build(),
                absent,
            )
            assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(absent.failure).code)
        }
        val snapshot = ImpactSnapshot.fromCredits(unitActor, 7, 3, "ARCHIVED", emptyList())
        assertEquals(
            "IMPACT_SIGNER_UNAVAILABLE",
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                DeletionImpact(JdbcRows().source).project(snapshot)
            }.status.description,
        )
    }

    @Test fun `deletion verify accepts current full set and rejects changed fingerprint`() {
        val original = ImpactSnapshot.fromCredits(unitActor, 7, 3, "ARCHIVED", emptyList())
        val token = signer().issue(original).token
        val same = JdbcRows(subject(), emptyList())
        same.source.connection.use { assertEquals(0L, DeletionImpact(same.source, signer()).verify(it, unitActor, token).creditCount) }
        val changed = JdbcRows(subject(), credit())
        assertEquals(
            "IMPACT_CHANGED",
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                changed.source.connection.use { DeletionImpact(changed.source, signer()).verify(it, unitActor, token) }
            }.status.description,
        )
    }

    @Test fun `cancelled consequence read stops before returning incomplete snapshot`() {
        val db = JdbcRows(subject(), credit())
        val cancelled = Context.current().withCancellation()
        cancelled.cancel(null)
        cancelled.run {
            assertEquals(
                Status.Code.CANCELLED,
                assertThrows(io.grpc.StatusRuntimeException::class.java) {
                    db.source.connection.use { ImpactSnapshot.read(it, unitActor) }
                }.status.code,
            )
        }
    }

    @Test fun `confirmed person deletion verifies consequence token before removing relationships`() {
        val snapshot = ImpactSnapshot.fromCredits(unitTarget, 7, 3, "ARCHIVED", emptyList())
        val db = JdbcRows(emptyList(), subject(), subject(), emptyList())
        val command =
            LifecycleCommand(
                unitTarget,
                7,
                LifecycleRules.Action.DELETE,
                true,
                signer().issue(snapshot).token,
                unitOperation,
                unitCorrelation,
                unitActor,
                "policy",
            )
        val result = db.source.connection.use { LifecycleStore(unitClock).person(it, command, DeletionImpact(db.source, signer())) }
        assertEquals(8L, result.version)
        assertTrue(db.writes.any { "DELETED" in it && unitTarget in it })
        assertTrue(
            db.writes
                .flatten()
                .filterIsInstance<String>()
                .any { it.contains("DELETED") && it.contains("ARCHIVED") },
        )
    }
}
