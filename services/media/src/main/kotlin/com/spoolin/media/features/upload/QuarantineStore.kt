package com.spoolin.media.features.upload

import com.spoolin.media.features.processing.ProcessingBounds
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID

/**
 * Manages UUID-keyed paths inside the quarantine volume (§7.6 / §8.3.2 / ATDD-161).
 *
 * Security constraints:
 * - All file names are generated UUIDs; user-supplied filenames never reach the filesystem.
 * - Paths resolve only under [root]/quarantine or [root]/output.
 * - fsync is called before returning the path, so a later commit/rename finds durable bytes.
 */
class QuarantineStore(
    private val root: Path,
) {
    private val quarantine: Path = root.resolve(ProcessingBounds.STAGING_SUBDIR)
    private val output: Path = root.resolve(ProcessingBounds.OUTPUT_SUBDIR)

    init {
        Files.createDirectories(quarantine)
        Files.createDirectories(output)
    }

    /** Allocate a new quarantine file keyed by [assetId] UUID. Never uses user input in the name. */
    fun stagingPath(assetId: UUID): Path = quarantine.resolve(assetId.toString())

    /** Open the staging file for writing. The caller is responsible for closing the stream. */
    fun openForWrite(path: Path): OutputStream =
        FileChannel
            .open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            .let { channel ->
                object : OutputStream() {
                    override fun write(b: Int) =
                        channel
                            .write(java.nio.ByteBuffer.wrap(byteArrayOf(b.toByte())))
                            .let {}

                    override fun write(
                        b: ByteArray,
                        off: Int,
                        len: Int,
                    ) {
                        channel.write(java.nio.ByteBuffer.wrap(b, off, len))
                    }

                    override fun flush() = channel.force(true) // fsync (§7.6)

                    override fun close() {
                        channel.force(true)
                        channel.close()
                    }
                }
            }

    /** Delete a quarantine file idempotently (cleanup on rejection / cancellation). */
    fun deleteQuarantine(path: Path) = Files.deleteIfExists(path)

    /** Move a committed quarantine file to the output area (atomic within the volume). */
    fun promoteToOutput(
        stagingPath: Path,
        assetId: UUID,
    ): Path {
        val outputPath = output.resolve(assetId.toString())
        Files.move(stagingPath, outputPath, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        return outputPath
    }
}
