package com.spoolin.core.features.artworkproxy

import io.grpc.Status
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

private const val SCRATCH_CAPACITY = 4

internal object ArtworkScratch {
    private val root = Path.of(System.getProperty("java.io.tmpdir"), "spoolin-core-artwork")
    private val permits = Semaphore(SCRATCH_CAPACITY)
    private val active = ConcurrentHashMap.newKeySet<Path>()
    private val fileMode = PosixFilePermissions.fromString("rw-------")

    init {
        val directoryMode = PosixFilePermissions.fromString("rwx------")
        try {
            Files.createDirectory(root, PosixFilePermissions.asFileAttribute(directoryMode))
        } catch (_: FileAlreadyExistsException) {
            check(Files.isDirectory(root, NOFOLLOW_LINKS)) { "Artwork scratch directory is invalid" }
        }
        check(Files.getPosixFilePermissions(root, NOFOLLOW_LINKS) == directoryMode)
        val probe = Files.createTempFile(root, "owner-", ".tmp", PosixFilePermissions.asFileAttribute(fileMode))
        try {
            check(Files.getOwner(root, NOFOLLOW_LINKS) == Files.getOwner(probe, NOFOLLOW_LINKS))
        } finally {
            Files.deleteIfExists(probe)
        }
        Files.newDirectoryStream(root).use { entries ->
            entries.forEach { path ->
                if (path.fileName.toString().startsWith("asset-") && Files.isRegularFile(path, NOFOLLOW_LINKS)) {
                    Files.delete(path)
                }
            }
        }
    }

    fun create(): Path {
        if (!permits.tryAcquire()) throw Status.RESOURCE_EXHAUSTED.withDescription("ARTWORK_BUSY").asRuntimeException()
        var created = false
        try {
            val path = Files.createTempFile(root, "asset-", ".tmp", PosixFilePermissions.asFileAttribute(fileMode))
            active.add(path)
            created = true
            return path
        } finally {
            if (!created) permits.release()
        }
    }

    fun delete(path: Path) {
        if (active.remove(path)) {
            try { Files.deleteIfExists(path) } finally { permits.release() }
        }
    }
}
