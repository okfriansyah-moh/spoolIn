package com.spoolin.media.seed

import com.spoolin.media.DatabaseSettings
import com.spoolin.media.features.upload.QuarantineStore
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

fun runSeedArtworkCli(args: Array<String>) {
    val options =
        args.associate { argument ->
            val fields = argument.removePrefix("--").split('=', limit = 2)
            require(fields.size == 2) { "Seed artwork arguments require explicit paths" }
            fields[0] to fields[1]
        }
    require(options.keys.all { it in setOf("seed-artwork-manifest", "seed-artwork-output", "seed-artwork-finalize") })
    val input = SeedArtworkInput(Path.of(requireNotNull(options["seed-artwork-manifest"])))
    val environment = StandardEnvironment()
    ConfigDataEnvironmentPostProcessor.applyTo(environment)
    DatabaseSettings.load(environment, "media", false).pool().use { database ->
        val store = QuarantineStore(Path.of(environment.getProperty("SPOOLIN_MEDIA_ROOT", "/var/spoolin/media")))
        val importer = SeedArtworkImporter(database, store)
        val report =
            options["seed-artwork-finalize"]
                ?.let { importer.finish(input, Path.of(it)) } ?: importer.prepare(input)
        options["seed-artwork-output"]?.let { writeReport(Path.of(it), report) }
        println(SeedJson.mapper.writeValueAsString(report))
    }
}

private fun writeReport(
    path: Path,
    report: Map<String, Any?>,
) {
    val parent = requireNotNull(path.toAbsolutePath().parent)
    Files.createDirectories(parent)
    val temporary = Files.createTempFile(parent, ".seed-", ".json")
    try {
        Files.write(temporary, SeedJson.mapper.writeValueAsBytes(report))
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(temporary)
    }
}
