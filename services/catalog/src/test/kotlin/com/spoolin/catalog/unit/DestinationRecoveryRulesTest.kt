package com.spoolin.catalog.unit

import com.spoolin.catalog.operations.work.WorkRecoveryDestination
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.work.WorkRecoveryInspection
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.sql.Timestamp
import java.time.Instant

class DestinationRecoveryRulesTest {
    private val now = Instant.parse("2026-01-01T12:00:00Z")

    private fun work(kind: String) =
        WorkRecoveryInspection(
            unitAsset,
            kind,
            "EXHAUSTED",
            0,
            1,
            1,
            3,
            now.plusSeconds(300),
            "ARTWORK_DOWNSTREAM_UNAVAILABLE",
            unitOperation,
            unitCorrelation,
        )

    private fun replies(
        kind: String,
        version: Long = 8,
    ) = arrayOf(
        listOf(mapOf("last" to Timestamp.from(now.minusSeconds(600)))),
        listOf(mapOf("now" to Timestamp.from(now))),
        listOf(mapOf("payload" to """{"subjectId":"$unitOperation","catalogVersion":8}""")),
        listOf(
            mapOf(
                "asset_id" to unitAsset,
                "expected_version" to 7L,
                "state" to
                    if (kind ==
                        "ARTWORK_ATTACH"
                    ) {
                        "PREPARING"
                    } else {
                        "ATTACHED"
                    },
                "status" to "ACTIVE",
                "version" to version,
                "attachment_id" to if (kind == "ARTWORK_CONFIRM") unitOperation else null,
            ),
        ),
    )

    private fun outcome(
        step: String,
        version: Long? = null,
    ): Spoolin.Outcome {
        val id = WorkRequest(step, "ARTWORK", unitOperation, Instant.MAX).id(unitOperation)
        return Spoolin.Outcome
            .newBuilder()
            .setOperationId(id.toString())
            .setResultId(unitOperation.toString())
            .setState("SUCCEEDED")
            .also { if (version != null) it.version = version }
            .build()
    }

    @Test fun `destination reconciliation accepts matching completed reserve confirm and release receipts`() {
        for ((kind, step) in listOf(
            "ARTWORK_ATTACH" to "media-reserve",
            "ARTWORK_CONFIRM" to "media-confirm",
            "ARTWORK_RELEASE" to "media-release",
        )) {
            val rows = replies(kind, if (kind == "ARTWORK_ATTACH") 7 else 8).toMutableList()
            if (kind == "ARTWORK_ATTACH") rows.add(listOf(mapOf("count" to 1)))
            val db = JdbcRows(*rows.toTypedArray())
            val channel =
                if (kind == "ARTWORK_ATTACH") {
                    GrpcReplies(
                        Spoolin.Asset
                            .newBuilder()
                            .setId(unitAsset.toString())
                            .setState("READY")
                            .build(),
                        outcome(step),
                    )
                } else {
                    GrpcReplies(outcome(step, 8))
                }
            WorkRecoveryDestination(MediaServiceGrpc.newBlockingStub(channel)).use { destination ->
                assertEquals("catalog", destination.owner)
                db.source.connection.use { destination.validate(it, work(kind)) }
            }
            assertTrue(db.writes.isEmpty(), "Recovery inspection only reconciles destination evidence")
        }
    }

    @Test fun `absent destination receipt permits replay while ambiguous unavailable or mismatched results reject`() {
        val db = JdbcRows(*replies("ARTWORK_CONFIRM"))
        db.source.connection.use {
            WorkRecoveryDestination(MediaServiceGrpc.newBlockingStub(GrpcReplies(Status.NOT_FOUND))).validate(it, work("ARTWORK_CONFIRM"))
        }
        for (reply in listOf(
            outcome("media-confirm", 9),
            outcome("media-confirm", 8).toBuilder().setResultExpired(true).build(),
            outcome("media-confirm", 8).toBuilder().setState("PENDING").build(),
            outcome("media-confirm", 8).toBuilder().setResultId(unitTarget.toString()).build(),
        )) {
            val source = JdbcRows(*replies("ARTWORK_CONFIRM"))
            assertThrows(IllegalArgumentException::class.java) {
                source.source.connection.use {
                    WorkRecoveryDestination(MediaServiceGrpc.newBlockingStub(GrpcReplies(reply))).validate(it, work("ARTWORK_CONFIRM"))
                }
            }
        }
        val unavailable = JdbcRows(*replies("ARTWORK_CONFIRM"))
        assertEquals(
            Status.Code.UNAVAILABLE,
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                unavailable.source.connection.use {
                    WorkRecoveryDestination(
                        MediaServiceGrpc.newBlockingStub(GrpcReplies(Status.UNAVAILABLE)),
                    ).validate(it, work("ARTWORK_CONFIRM"))
                }
            }.status.code,
        )
    }

    @Test fun `recovery requires bounded old effect and matching selected catalog target`() {
        val active = JdbcRows(listOf(mapOf("last" to Timestamp.from(now.minusSeconds(1)))), listOf(mapOf("now" to Timestamp.from(now))))
        assertThrows(IllegalArgumentException::class.java) {
            active.source.connection.use {
                WorkRecoveryDestination(MediaServiceGrpc.newBlockingStub(GrpcReplies())).validate(it, work("ARTWORK_CONFIRM"))
            }
        }
        val unselected = JdbcRows(*replies("ARTWORK_RELEASE"))
        assertThrows(IllegalArgumentException::class.java) {
            unselected.source.connection.use {
                WorkRecoveryDestination(MediaServiceGrpc.newBlockingStub(GrpcReplies())).validate(it, work("ARTWORK_CONFIRM"))
            }
        }
        val unknown = JdbcRows()
        assertThrows(IllegalArgumentException::class.java) {
            unknown.source.connection.use {
                WorkRecoveryDestination(MediaServiceGrpc.newBlockingStub(GrpcReplies())).validate(it, work("UNKNOWN"))
            }
        }
    }
}
