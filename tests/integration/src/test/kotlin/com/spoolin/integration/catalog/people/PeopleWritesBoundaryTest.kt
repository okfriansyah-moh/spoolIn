package com.spoolin.integration.catalog.people

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Cross-service person write integration test boundary.
 *
 * Exercises createPerson (ATDD-023), updatePerson (ATDD-023), stale-version rejection (ATDD-079),
 * same-named identity independence (ATDD-081), archived-person rejection (ATDD-023-C02),
 * name boundary enforcement (ATDD-114), managed reads (ATDD-251-T1-R12/R15) and
 * storage constraints (ATDD-237) through the real Core→Catalog gRPC boundary.
 *
 * Requires: live PostgreSQL 18.6, Catalog service (V migration), Core service.
 * Implementation: pending T019/T021 full integration wiring and fixture setup.
 *
 * ATDD-023: Create/update person — name 1–100 code points; independent identity.
 * ATDD-026 / ATDD-027: Multiple roles / invalid credit — person lifecycle prerequisite.
 * ATDD-079: Stale save — VERSION_CONFLICT; draft retained for review.
 * ATDD-081: Same-named people remain distinct; no identity merge by text alone.
 * ATDD-114: Person-name boundaries — 1 and 100 pass; 0 and 101 reject.
 * ATDD-237: Storage constraints — id/name/status/impact_version/version/created_at/updated_at rules.
 * ATDD-251-T1-R12: managedPerson — private active/archived projection.
 * ATDD-251-T1-R15: managedPeople — explicit ACTIVE/ARCHIVED state; name/UUID order.
 * ATDD-251-T1-R26: createPerson — required Unicode name; optional biography; independent identity.
 * ATDD-251-T1-R27: updatePerson — active person/current version; changed fields only.
 * ATDD-253-T1-R11: ManagedPeople(PageRequest) → PersonPage.
 * ATDD-253-T1-R13: ManagedPerson(Id) → Person.
 * ATDD-253-T1-R17: WritePerson(PersonWrite) → Outcome.
 * ATDD-261: People state transitions and feedback.
 * ATDD-262: Credits state transitions and feedback.
 */
@Disabled("Integration boundary pending T019/T021 full wiring")
class PeopleWritesBoundaryTest {

    @Test
    fun `placeholder - createPerson boundary pending T019`() {
        // ATDD-023: valid A input creates one active person with matching audit.
        // ATDD-114: one code-point name accepted; zero and 101 rejected with field error.
        // ATDD-081: two Alex records stay distinct; no identity merge.
        // ATDD-251-T1-R26: required name; optional biography; independent identity.
    }

    @Test
    fun `placeholder - updatePerson boundary pending T019`() {
        // ATDD-023-C02: archived person cannot be edited; restoration required first.
        // ATDD-079: stale expectedVersion → VERSION_CONFLICT; draft retained.
        // ATDD-251-T1-R27: active person / current version; changed fields only.
    }

    @Test
    fun `placeholder - managedPerson reads boundary pending T019`() {
        // ATDD-251-T1-R12: managedPerson returns active or archived person for Maintainer.
        // ATDD-253-T1-R13: ManagedPerson(Id) → Person.
    }

    @Test
    fun `placeholder - managedPeople reads boundary pending T019`() {
        // ATDD-251-T1-R15: managedPeople with explicit state; name/UUID order.
        // ATDD-253-T1-R11: ManagedPeople(PageRequest) → PersonPage.
    }

    @Test
    fun `placeholder - person storage constraints boundary pending T018 DB fixture`() {
        // ATDD-237: id/name/status/impact_version/version/created_at/updated_at DDL constraints.
        // Requires PostgreSQL 18.6 and live schema fixture.
    }
}
