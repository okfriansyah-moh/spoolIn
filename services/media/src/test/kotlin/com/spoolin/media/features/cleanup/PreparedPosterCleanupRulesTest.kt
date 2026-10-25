package com.spoolin.media.features.cleanup

import com.spoolin.media.features.asset.CatalogAttachments
import com.spoolin.media.features.upload.QuarantineStore
import com.spoolin.media.fixtures.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import spoolin.v1.Spoolin.Attachment
import java.nio.file.Files
import java.nio.file.Path

class PreparedPosterCleanupRulesTest {
    @TempDir lateinit var root: Path
    @Test fun `released unused prepared poster removes private and published bytes in a committed cleanup`() {
        val store = QuarantineStore(root)
        val published = Files.write(store.outputDirectory().resolve("$assetId.png"), byteArrayOf(1, 2))
        val staging = Files.write(store.stagingPath(assetId), byteArrayOf(3))
        val db = JdbcBoundary().rows(mapOf("id" to assetId, "upload_operation_id" to operationId))
            .rows(mapOf("format" to "PNG")).rows()
        val remote = Attachment.newBuilder().setTitleId(operationId.toString()).setAssetId(assetId.toString()).setState("RELEASED").build()
        val cleanup = PreparedPosterCleanup(db.source, CatalogAttachments(CatalogBoundary(remote).client), store)
        cleanup.sweep()
        assertFalse(Files.exists(published))
        assertFalse(Files.exists(staging))
        assertEquals(1, db.commits)
        // The same scheduled sweep is bounded to once per minute.
        cleanup.sweep()
        assertEquals(1, db.commits)
    }
}
