package com.spoolin.integration.catalog.reads

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Cross-service catalog reads integration test boundary.
 *
 * Exercises SearchTitles, GetTitle, SearchPeople, GetPerson, TitleCredits,
 * PersonCredits, ListGenres, ListRoles through the real Core→Catalog gRPC boundary.
 *
 * Requires: live PostgreSQL 18.6, Catalog service with V002 migration, Core service.
 * Implementation: pending T013 full integration wiring.
 *
 * ATDDs covered (full integration boundary): ATDD-137, ATDD-138.
 */
@Disabled("Integration boundary pending T013 full wiring")
class CatalogReadsBoundaryTest {

    @Test
    fun `placeholder - real boundary pending T013`() {
        // Full cross-service read boundary tests belong in T013.
        // T012 contributes local unit evidence (DateOrdering, CursorBinding, QueryStructure).
    }
}
