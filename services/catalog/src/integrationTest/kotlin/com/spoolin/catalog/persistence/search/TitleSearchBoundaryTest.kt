package com.spoolin.catalog.persistence.search

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * PostgreSQL search boundary for catalog title/person search.
 *
 * Validates pg_unicode_fast casefold matching, wildcard escaping, keyset ordering,
 * and date filter semantics against a real PostgreSQL 18.6 instance.
 *
 * ATDD-137: Unicode/punctuation search literals.
 * ATDD-138: Live pagination binding to current query.
 *
 * Requires: live PostgreSQL 18.6 with pg_unicode_fast extension + V002 migration.
 * Implementation: pending T012 integration fixture setup (T013 contributes full
 * F-CATALOG fixtures with seeded titles/people).
 */
@Disabled("PostgreSQL boundary pending integration fixture setup")
class TitleSearchBoundaryTest {

    @Test
    fun `placeholder - PostgreSQL search boundary pending integration fixtures`() {
        // Real pg_unicode_fast / keyset search tests require F-CATALOG fixtures.
        // These will be enabled in T013 once seed data is available.
    }
}
