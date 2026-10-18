package com.spoolin.catalog.seed

import java.util.UUID

/** Source roles bind to migration-owned starter identities independently of display labels. */
internal object SeedRoleIdentity {
    private val starters =
        mapOf(
            "P161" to "b0000001-0000-4000-8000-000000000001",
            "P57" to "b0000001-0000-4000-8000-000000000002",
            "P58" to "b0000001-0000-4000-8000-000000000003",
            "P162" to "b0000001-0000-4000-8000-000000000004",
        )

    fun resolve(
        source: String,
        externalId: String,
        sourceIdentity: String,
    ): UUID {
        val starter = if (source == "wikidata") starters[externalId] else null
        return UUID.fromString(starter ?: sourceIdentity)
    }
}
