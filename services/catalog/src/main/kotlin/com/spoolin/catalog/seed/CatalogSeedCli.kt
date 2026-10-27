package com.spoolin.catalog.seed

import com.spoolin.catalog.DatabaseSettings
import com.spoolin.support.logging.applyNormalizedLogLevel
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PolicyEnvironment
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor
import org.springframework.boot.context.config.ConfigDataException
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.StandardEnvironment
import tools.jackson.core.JacksonException
import java.io.IOException
import java.nio.file.Path
import java.sql.SQLException
import kotlin.system.exitProcess

/** CLI entry for ops/bin/seed.sh --apply (T033). */
fun runSeedCli(args: Array<String>) {
    applyNormalizedLogLevel(System.getenv("LOG_LEVEL"), "catalog")
    try {
        seedCommand(args)
    } catch (
        _: ConfigDataException,
    ) {
        failed("SEED_CONFIGURATION_INVALID")
    } catch (
        _: InputViolation,
    ) {
        failed("SEED_POLICY_INVALID")
    } catch (
        _: IllegalArgumentException,
    ) {
        failed("SEED_CONFIGURATION_OR_MANIFEST_INVALID")
    } catch (
        _: IllegalStateException,
    ) {
        failed("SEED_RECEIPT_OR_PROVENANCE_INVALID")
    } catch (
        _: SQLException,
    ) {
        failed("SEED_DATABASE_UNAVAILABLE")
    } catch (
        _: JacksonException,
    ) {
        failed("SEED_MANIFEST_INVALID")
    } catch (_: IOException) {
        failed("SEED_IO_UNAVAILABLE")
    }
}

private fun seedCommand(args: Array<String>) {
    val manifestArg = args.firstOrNull { it.startsWith("--seed-manifest=") } ?: usage()
    val manifest = Path.of(manifestArg.removePrefix("--seed-manifest="))
    require(
        args.all {
            it.startsWith("--seed-manifest=") || it.startsWith("--seed-artwork-input=") ||
                it.startsWith("--seed-artwork-output=") || it == "--validate-only"
        },
    ) { "Unsupported seed argument" }
    val environment = seedEnvironment()
    val policy = { PolicyEnvironment.resolve { environment.getProperty(it) } }
    if ("--validate-only" in args) {
        val effective = policy()
        val prepared = SeedManifest.prepare(manifest, effective)
        println(
            "Seed preparation validated version=${prepared.version} " +
                "policyRevision=${effective.revision}; database not opened",
        )
        return
    }
    val seedVersion =
        System.getenv("SPOOLIN_SEED_ID")
            ?: System.getenv("SPOOLIN_SEED_VERSION")
            ?: "manifest-v1"
    val dataSource = DatabaseSettings.load(environment, "catalog", false).pool()
    dataSource.use { pool ->
        pool.connection.use(com.spoolin.support.identity.NamedIdentities::verify)
        val loader = CatalogSeedLoader(pool, policy)
        val inserted =
            loader.loadFromManifest(
                manifest,
                seedVersion,
                args.firstOrNull { it.startsWith("--seed-artwork-input=") }?.substringAfter("=")?.let(Path::of),
                args.firstOrNull { it.startsWith("--seed-artwork-output=") }?.substringAfter("=")?.let(Path::of),
            )
        println("CatalogSeedLoader inserted $inserted titles for seed_version=$seedVersion")
    }
}

private fun usage(): Nothing {
    System.err.println("usage: catalog --seed-manifest=/path/to/manifest.json")
    kotlin.system.exitProcess(2)
}

private fun failed(code: String): Nothing {
    System.err.println("seed: $code; inspect configured policy, frozen input and owned database prerequisites")
    exitProcess(1)
}

/** Loads the same protected ConfigData imports as Catalog, without starting its server or database. */
internal fun seedEnvironment(environment: ConfigurableEnvironment = StandardEnvironment()): ConfigurableEnvironment {
    ConfigDataEnvironmentPostProcessor.applyTo(environment)
    return environment
}
