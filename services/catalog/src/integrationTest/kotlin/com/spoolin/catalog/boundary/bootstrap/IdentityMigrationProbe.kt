package com.spoolin.catalog.boundary.bootstrap

import com.spoolin.support.identity.NamedIdentities
import com.spoolin.support.receipts.WorkRequest
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Instant
import java.util.Properties
import java.util.UUID

/** Separate process checks the production environment-configured resolver against a real owner guard. */
object IdentityMigrationProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2)
        val properties =
            Properties().apply {
                setProperty("user", "spoolin_catalog_runtime")
                setProperty("password", Files.readString(Path.of(args[1])).trimEnd('\n', '\r'))
                setProperty("connectTimeout", "2")
            }
        DriverManager.getConnection(args[0], properties).use(NamedIdentities::verify)
        val parent = UUID.fromString("95c65556-47e7-4150-a739-9ad8d065495e")
        check(
            WorkRequest("attach", "ARTWORK_ATTACH", parent, Instant.MAX).id(parent) ==
                UUID.fromString("5d231fd7-27ab-59ec-b937-c887840b7a7d"),
        )
        check(
            WorkRequest("media-upload", "ARTWORK", parent, Instant.MAX).id(parent) ==
                UUID.fromString("d30cccbe-480a-5054-96a8-ca4cc164f47c"),
        )
        val fresh = UUID.fromString("00000000-0000-4000-8000-000000000099")
        check(WorkRequest("attach", "ARTWORK_ATTACH", fresh, Instant.MAX).id(fresh).version() == 8)
        println("Configured identity registry retained legacy work and generated UUIDv8 for a new root")
    }
}
