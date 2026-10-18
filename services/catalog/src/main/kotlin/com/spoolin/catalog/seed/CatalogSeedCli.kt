package com.spoolin.catalog.seed

import com.spoolin.catalog.DatabaseSettings
import com.spoolin.support.logging.applyNormalizedLogLevel
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Path

/** CLI entry for ops/bin/seed.sh --apply (T033). */
fun runSeedCli(args: Array<String>) {
    applyNormalizedLogLevel(System.getenv("LOG_LEVEL"), "catalog")
    val manifestArg = args.firstOrNull { it.startsWith("--seed-manifest=") } ?: usage()
    val manifest = Path.of(manifestArg.removePrefix("--seed-manifest="))
    val seedVersion =
        System.getenv("SPOOLIN_SEED_ID")
            ?: System.getenv("SPOOLIN_SEED_VERSION")
            ?: "manifest-v1"
    val dataSource = DatabaseSettings.load(StandardEnvironment(), "catalog", false).pool()
    dataSource.use { pool ->
        val loader = CatalogSeedLoader(pool)
        val inserted = loader.loadFromManifest(manifest, seedVersion)
        println("CatalogSeedLoader inserted $inserted titles for seed_version=$seedVersion")
    }
}

private fun usage(): Nothing {
    System.err.println("usage: catalog --seed-manifest=/path/to/manifest.json")
    kotlin.system.exitProcess(2)
}
