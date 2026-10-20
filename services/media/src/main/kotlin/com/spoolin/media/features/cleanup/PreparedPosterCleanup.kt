package com.spoolin.media.features.cleanup

import com.spoolin.media.features.asset.CatalogAttachments
import com.spoolin.media.features.upload.QuarantineStore
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.nio.file.Files
import java.util.UUID
import javax.sql.DataSource

class PreparedPosterCleanup(private val dataSource: DataSource,private val catalog: CatalogAttachments,private val store: QuarantineStore) {
    fun sweep() {
        val now = System.currentTimeMillis()
        val previous = nextSweep.get()
        if (now < previous || !nextSweep.compareAndSet(previous,now+60_000)) return
        val candidates = dataSource.connection.use { conn ->
            conn.queryBound("SELECT id,upload_operation_id FROM asset WHERE provenance->>'preparedPoster'='true' AND state IN ('READY','REJECTED','DELETING') AND updated_at<CURRENT_TIMESTAMP-interval '1 minute' AND processing_work_id IS NOT NULL ORDER BY updated_at LIMIT 20",emptyList()) {
                it.getObject("id",UUID::class.java) to it.getObject("upload_operation_id",UUID::class.java)
            }
        }
        candidates.forEach { (asset,intent) ->
            val remote = try { catalog.find(intent) } catch (_: io.grpc.StatusRuntimeException) { return@forEach }
            if (remote.titleId != intent.toString() || remote.assetId != asset.toString() || remote.state != "RELEASED") return@forEach
            dataSource.committed { conn ->
                val locked = conn.queryBound("SELECT format FROM asset WHERE id=? AND state IN ('READY','REJECTED','DELETING') FOR UPDATE",listOf(asset)) { it.getString("format") }.singleOrNull()
                val refs = conn.queryBound("SELECT 1 FROM asset_reference WHERE asset_id=? AND state IN ('RESERVED','ACTIVE')",listOf(asset)) { 1 }
                if (refs.isNotEmpty()) return@committed
                conn.executeBound("UPDATE asset SET state='DELETING' WHERE id=? AND state IN ('READY','REJECTED')",asset)
                val extension = when (locked) { "JPEG" -> "jpeg"; "PNG" -> "png"; "WEBP" -> "webp"; else -> null }
                if (extension != null) Files.deleteIfExists(store.outputDirectory().resolve("$asset.$extension"))
                Files.deleteIfExists(store.stagingPath(asset))
                conn.executeBound("UPDATE asset SET state='DELETED',updated_at=CURRENT_TIMESTAMP WHERE id=? AND state='DELETING'",asset)
            }
        }
    }
    companion object { private val nextSweep = java.util.concurrent.atomic.AtomicLong(0) }
}
