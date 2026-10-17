package com.spoolin.catalog.seed

import com.spoolin.catalog.DatabaseSettings
import com.spoolin.support.receipts.queryBound
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Path
import java.util.UUID

/** CLI entry for ops/bin/seed.sh --apply (T033). */
fun runSeedCli(args: Array<String>) {
    val manifestArg = args.firstOrNull { it.startsWith("--seed-manifest=") } ?: usage()
    val manifest = Path.of(manifestArg.removePrefix("--seed-manifest="))
    val seedId = System.getenv("SPOOLIN_SEED_ID") ?: "manifest-v1"
    val dataSource = DatabaseSettings.load(StandardEnvironment(), "catalog", false).pool()
    dataSource.use { pool ->
        val loader = CatalogSeedLoader(pool)
        val inserted =
            loader.loadFromManifest(manifest, seedId) { label ->
                pool.connection.use { connection ->
                    connection
                        .queryBound("SELECT id FROM genre WHERE label = ?", listOf(label)) {
                            it.getObject("id", UUID::class.java)
                        }.singleOrNull() ?: error("missing genre label: $label")
                }
            }
        println("CatalogSeedLoader inserted $inserted titles for seed_id=$seedId")
    }
}

private fun usage(): Nothing {
    System.err.println("usage: catalog --seed-manifest=/path/to/manifest.json")
    kotlin.system.exitProcess(2)
}
