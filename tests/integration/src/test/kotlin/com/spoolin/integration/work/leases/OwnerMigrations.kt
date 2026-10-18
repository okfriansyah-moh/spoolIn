package com.spoolin.integration.work.leases

import com.spoolin.core.boundary.bootstrap.OwnerFixture
import org.flywaydb.core.Flyway

/** Cross-owner tests select SQL explicitly because their classpath contains several services. */
internal fun migrateOwner(
    fixture: OwnerFixture,
    owner: String,
) {
    require(owner in setOf("core", "catalog", "media"))
    fixture.pool(owner, "migrator").use { source ->
        Flyway
            .configure()
            .dataSource(source)
            .locations("filesystem:${fixture.repository}/services/$owner/src/main/resources/db/migration")
            .schemas("appdata")
            .defaultSchema("appdata")
            .createSchemas(false)
            .failOnMissingLocations(true)
            .validateMigrationNaming(true)
            .cleanDisabled(true)
            .lockRetryCount(0)
            .load()
            .migrate()
    }
}
