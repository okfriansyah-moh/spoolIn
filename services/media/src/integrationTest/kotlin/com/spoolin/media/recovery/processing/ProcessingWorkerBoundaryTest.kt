package com.spoolin.media.recovery.processing

import com.spoolin.media.MigrationRunner
import com.spoolin.media.boundary.bootstrap.OwnerFixture
import com.spoolin.media.features.asset.AssetSql
import com.spoolin.media.features.asset.AssetState
import com.spoolin.media.features.processing.MediaProcessingHandler
import com.spoolin.media.features.upload.QuarantineStore
import com.spoolin.media.operations.receipts.ReceiptWork
import com.spoolin.media.operations.work.MediaWorkWatchdog
import com.spoolin.media.operations.work.WorkClaimer
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.imageio.ImageIO
import javax.sql.DataSource

class ProcessingWorkerBoundaryTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `real codec and PostgreSQL processing publish immutable metadata receipt and paired attempt`() {
        database { pool ->
            val store = QuarantineStore(directory)
            val asset = accepted(pool, store)
            val claim = WorkClaimer(pool).claim(1).single()
            MediaProcessingHandler(pool, store).handle(claim)
            assertEquals(AssetState.READY, AssetSql.find(pool, asset)!!.state)
            assertTrue(Files.isRegularFile(directory.resolve("output/$asset.png")))
            assertTrue(!Files.exists(store.stagingPath(asset)))
            pool.connection.use { connection ->
                val state =
                    connection
                        .queryBound(
                            "SELECT w.state AS work,r.state AS receipt,count(e.id) AS events FROM durable_work w " +
                                "JOIN operation_receipt r ON r.id=w.operation_id " +
                                "JOIN operational_event e ON e.work_id=w.id WHERE w.id=? GROUP BY w.state,r.state",
                            listOf(claim.workId),
                        ) { Triple(it.getString("work"), it.getString("receipt"), it.getInt("events")) }
                        .single()
                assertEquals(Triple("DONE", "SUCCEEDED", 2), state)
            }
        }
    }

    @Test
    fun `expired worker cannot commit asset or receipt and watchdog preserves attempt history`() {
        database { pool ->
            val store = QuarantineStore(directory)
            val asset = accepted(pool, store)
            val claim = WorkClaimer(pool).claim(1).single()
            pool.committed { connection ->
                connection.executeBound(
                    "UPDATE durable_work SET lease_until=CURRENT_TIMESTAMP-interval '1 second' WHERE id=?",
                    claim.workId,
                )
            }
            MediaProcessingHandler(pool, store).handle(claim)
            assertEquals(AssetState.PROCESSING, AssetSql.find(pool, asset)!!.state)
            assertTrue(Files.exists(store.stagingPath(asset)))
            MediaWorkWatchdog(pool).reconcile()
            val replacement = WorkClaimer(pool).claim(1).single()
            MediaProcessingHandler(pool, store).handle(replacement)
            assertEquals(AssetState.READY, AssetSql.find(pool, asset)!!.state)
            pool.connection.use { connection ->
                assertEquals(
                    4,
                    connection
                        .queryBound(
                            "SELECT count(*) AS n FROM operational_event WHERE work_id=?",
                            listOf(claim.workId),
                        ) { it.getInt("n") }
                        .single(),
                )
            }
        }
    }

    private fun accepted(
        pool: DataSource,
        store: QuarantineStore,
    ): UUID {
        val id = UUID.randomUUID()
        val operation = UUID.randomUUID()
        val source = store.stagingPath(id)
        ImageIO.write(BufferedImage(10, 8, BufferedImage.TYPE_INT_RGB), "png", source.toFile())
        val bytes = Files.readAllBytes(source)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        val request = WorkRequest("upload-process", "MEDIA_PROCESS", id, Instant.now().plusSeconds(300))
        pool.committed { connection ->
            connection.executeBound(
                "INSERT INTO operation_receipt " +
                    "(id,actor_scope,kind,request_hash,state,correlation_id,expires_at) " +
                    "VALUES (?,'account:test','UPLOAD',?,'PENDING',?,CURRENT_TIMESTAMP+interval '7 days')",
                operation,
                hash,
                operation,
            )
            AssetSql.insertReceiving(connection, id, operation, UUID.randomUUID(), bytes.size.toLong(), "p1")
            ReceiptWork.enqueue(connection, operation, operation, request, Instant.now())
            assertEquals(
                1,
                AssetSql.acceptProcessing(
                    connection,
                    id,
                    "quarantine/$id",
                    hash,
                    request.id(operation),
                    Instant.now(),
                ),
            )
        }
        return id
    }

    private fun database(action: (DataSource) -> Unit) {
        OwnerFixture("t022-media-worker-processing.json").start().use { fixture ->
            fixture.membership("media", "grant")
            fixture.pool("media", "migrator").use { MigrationRunner(it).migrate() }
            fixture.grants("media")
            fixture.membership("media", "revoke")
            fixture.pool("media").use(action)
        }
    }
}
