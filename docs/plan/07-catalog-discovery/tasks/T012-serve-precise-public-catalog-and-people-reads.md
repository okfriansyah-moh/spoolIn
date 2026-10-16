---
kind: task
id: T012
group: G07
depends_on:
- T002
- T003
- T008
- T009
- T010
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T012 — Serve precise public catalog and people reads

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-001 | implements |
| ATDD-002 | implements |
| ATDD-003 | implements |
| ATDD-004 | implements |
| ATDD-005 | implements |
| ATDD-006 | implements |
| ATDD-007 | implements |
| ATDD-008 | implements |
| ATDD-009 | implements |
| ATDD-010 | implements |
| ATDD-011 | implements |
| ATDD-012 | implements |
| ATDD-013 | implements |
| ATDD-014 | implements |
| ATDD-015 | implements |
| ATDD-030 | implements |
| ATDD-031 | implements |
| ATDD-032 | implements |
| ATDD-133 | implements |
| ATDD-022 | implements |
| ATDD-137 | both |
| ATDD-138 | both |
| ATDD-139 | implements |
| ATDD-236 | implements |
| ATDD-236-T1-R01 | implements |
| ATDD-236-T1-R02 | implements |
| ATDD-236-T1-R03 | implements |
| ATDD-236-T1-R04 | implements |
| ATDD-236-T1-R05 | implements |
| ATDD-236-T1-R06 | implements |
| ATDD-236-T1-R07 | implements |
| ATDD-236-T1-R08 | implements |
| ATDD-236-T1-R09 | implements |
| ATDD-236-T1-R10 | implements |
| ATDD-236-T1-R11 | implements |
| ATDD-236-T1-R12 | implements |
| ATDD-236-T1-R13 | implements |
| ATDD-236-T1-R14 | implements |
| ATDD-236-T1-R15 | implements |
| ATDD-236-T1-R16 | implements |
| ATDD-236-T1-R17 | implements |
| ATDD-236-T1-R18 | implements |
| ATDD-236-T1-R19 | implements |
| ATDD-236-T1-R20 | implements |
| ATDD-236-T1-R21 | implements |
| ATDD-236-T1-R22 | implements |
| ATDD-236-T1-R23 | implements |
| ATDD-238 | implements |
| ATDD-238-T1-R01 | implements |
| ATDD-238-T1-R02 | implements |
| ATDD-238-T1-R03 | implements |
| ATDD-238-T1-R04 | implements |
| ATDD-239 | implements |
| ATDD-239-T1-R01 | implements |
| ATDD-239-T1-R02 | implements |
| ATDD-242 | implements |
| ATDD-242-T1-R01 | implements |
| ATDD-242-T1-R02 | implements |
| ATDD-242-T1-R03 | implements |
| ATDD-242-T1-R04 | implements |
| ATDD-242-T1-R05 | implements |
| ATDD-242-T1-R06 | implements |
| ATDD-242-T1-R07 | implements |
| ATDD-242-T1-R08 | implements |
| ATDD-242-T1-R09 | implements |
| ATDD-242-T1-R10 | implements |
| ATDD-251-T1-R03 | implements |
| ATDD-251-T1-R04 | implements |
| ATDD-251-T1-R05 | implements |
| ATDD-251-T1-R06 | implements |
| ATDD-251-T1-R07 | implements |
| ATDD-251-T1-R08 | implements |
| ATDD-251-T1-R09 | implements |
| ATDD-251-T1-R10 | implements |
| ATDD-253-T1-R01 | implements |
| ATDD-253-T1-R02 | implements |
| ATDD-253-T1-R03 | implements |
| ATDD-253-T1-R04 | implements |
| ATDD-253-T1-R05 | implements |
| ATDD-253-T1-R06 | implements |
| ATDD-253-T1-R07 | implements |
| ATDD-253-T1-R08 | implements |
| ATDD-256 | implements |
| ATDD-257 | implements |
| ATDD-281 | both |
| ATDD-282 | both |
| ATDD-303 | implements |
| ATDD-317 | both |
| J-01 | both |
| FR-001 | implements |
| FR-002 | both |
| FR-003 | implements |
| FR-005 | implements |
| FR-007 | implements |
| FR-017 | implements |
| NFR-003 | both |
| NFR-007 | both |
| NFR-008 | both |
| NFR-016 | both |
| A06 | implements |
| A09 | implements |
| A11 | both |
| A14 | implements |
| D18 | both |
| D25 | implements |
| D26 | implements |
| AC-001 | implements |
| AC-001-C01 | implements |
| AC-001-C02 | implements |
| AC-002 | implements |
| AC-002-C01 | implements |
| AC-003 | implements |
| AC-003-C01 | implements |
| AC-004 | implements |
| AC-004-C01 | implements |
| AC-004-C02 | implements |
| AC-005 | implements |
| AC-005-C01 | implements |
| AC-006 | implements |
| AC-006-C01 | implements |
| AC-006-C02 | implements |
| AC-006-C03 | implements |
| AC-007 | implements |
| AC-007-C01 | implements |
| AC-008 | both |
| AC-008-C01 | both |
| AC-008-C02 | both |
| AC-008-C03 | both |
| AC-009 | implements |
| AC-009-C01 | implements |
| AC-010 | implements |
| AC-010-C01 | implements |
| AC-011 | implements |
| AC-011-C01 | implements |
| AC-012 | implements |
| AC-012-C01 | implements |
| AC-012-C02 | implements |
| AC-013 | implements |
| AC-013-C01 | implements |
| AC-013-C02 | implements |
| AC-014 | implements |
| AC-014-C01 | implements |
| AC-015 | implements |
| AC-015-C01 | implements |
| AC-022 | implements |
| AC-022-C01 | implements |
| AC-022-C02 | implements |
| AC-030 | implements |
| AC-030-C01 | implements |
| AC-031 | implements |
| AC-031-C01 | implements |
| AC-031-C02 | implements |
| AC-032 | implements |
| AC-032-C01 | implements |
| AC-147 | both |
| AC-147-C01 | both |
| AC-147-C02 | both |
| AC-147-C03 | both |
| AC-159 | implements |
| AC-159-C01 | implements |
| AC-159-C02 | implements |
| AC-159-C03 | implements |
| AC-070 | both |
| G-03 | both |
| G-05 | both |
| OD-005 | both |
| OD-018 | implements |
| US-005 | implements |
| PRD-POLICY-04 | implements |
| PRD-BOUND-07 | implements |
| PRD-POLICY-23 | implements |
| TD-C-026 | both |
| TD-C-032 | implements |
| TD-C-039 | both |
| ADR-02 | both |
| TD-ROW-L2726 | both |
| TD-ROW-L2727 | both |
| TD-ROW-L2728 | both |
| TD-ROW-L2729 | both |
| TD-ROW-L2730 | both |
| TD-ROW-L2731 | both |
| TD-ROW-L2732 | both |
| TD-ROW-L2733 | both |
| TD-ROW-L4397 | both |
| TD-ROW-L4398 | both |
| TD-ROW-L4399 | both |
| TD-ROW-L4400 | both |
| TD-ROW-L4401 | both |
| TD-ROW-L4402 | both |
| TD-ROW-L4403 | both |
| TD-ROW-L4404 | both |
| TD-ROW-L4405 | both |
| TD-ROW-L4406 | both |
| TD-ROW-L4407 | both |
| TD-ROW-L4408 | both |
| TD-ROW-L4409 | both |
| TD-ROW-L4410 | both |
| TD-ROW-L4411 | both |
| TD-ROW-L4412 | both |
| TD-ROW-L4413 | both |
| TD-ROW-L4414 | both |
| TD-ROW-L4415 | both |
| TD-ROW-L4416 | both |
| TD-ROW-L4417 | both |
| TD-ROW-L4418 | both |
| TD-ROW-L4419 | both |
| TD-ROW-L4537 | both |
| TD-ROW-L4538 | both |
| TD-ROW-L4539 | both |
| TD-ROW-L4540 | both |
| TD-ROW-L4576 | both |
| TD-ROW-L4577 | both |
| TD-ROW-L4710 | both |
| TD-ROW-L4711 | both |
| TD-ROW-L4712 | both |
| TD-ROW-L4713 | both |
| TD-ROW-L4714 | both |
| TD-ROW-L4715 | both |
| TD-ROW-L4716 | both |
| TD-ROW-L4717 | both |
| TD-ROW-L4718 | both |
| TD-ROW-L4719 | both |
| TD-ROW-L4767 | both |
| TD-ROW-L4768 | both |
| TD-ROW-L4769 | both |
| TD-ROW-L4770 | both |
| TD-ROW-L4771 | both |
| TD-ROW-L4772 | both |
| TD-ROW-L4773 | both |
| BRAIN-A11 | both |
| BRAIN-D18 | both |

## Prerequisites

- [T002 — Generate and validate the shared wire contracts](../../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.
- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- [T008 — Enforce scoped service identity and shared admission](../../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T009 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T010 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/search/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/taxonomy/read/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/person/read/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/provenance/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/resources/db/migration/V002__catalog_read_model.sql | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/catalogview/read/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/search/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/integrationTest/kotlin/com/spoolin/catalog/persistence/search/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/catalog/reads/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/seed/persistence/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/taxonomy/read/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/person/read/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/provenance/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/catalogview/read/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/seed/persistence/ | T012 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.3 Search, navigation and request state](../../../04-spoolin-techdoc.md#33-search-navigation-and-request-state): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [4.3 SpoolIn Catalog service](../../../04-spoolin-techdoc.md#43-spoolin-catalog-service): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.3 Operation, permission and deadline inventory](../../../04-spoolin-techdoc.md#53-operation-permission-and-deadline-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.2 Search and date semantics](../../../04-spoolin-techdoc.md#72-search-and-date-semantics): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.4 Index and query-plan obligations](../../../04-spoolin-techdoc.md#74-index-and-query-plan-obligations): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-236-T1-R01 · L9541](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-236-T1-R02 · L9542](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `kind` (`varchar(8)`); Source-valid technical token; 8 versus 9 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (kind IN ('MOVIE','TV')); no SQL default; owner supplies value` |
| [ATDD-236-T1-R03 · L9543](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `status` (`varchar(8)`); Source-valid technical token; 8 versus 9 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission supplies 'ACTIVE'; explicit NULL fails23502; `NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ARCHIVED')); NOT NULL (explicit or PK)` |
| [ATDD-236-T1-R04 · L9544](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `display_title` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (length(btrim(display_title))>0); no SQL default; owner supplies value` |
| [ATDD-236-T1-R05 · L9545](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `original_title` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R06 · L9546](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `synopsis` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (length(btrim(synopsis))>0); no SQL default; owner supplies value` |
| [ATDD-236-T1-R07 · L9547](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `synopsis_language` (`varchar(35)`); Source-valid technical token; 35 versus 36 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-236-T1-R08 · L9548](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `original_language` (`varchar(35)`); Source-valid technical token; 35 versus 36 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R09 · L9549](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `countries` (`text[]`); Ordered complete strings, empty/NULL-element/two-dimensional arrays only against declared array predicates; no invented per-entry SQL limit; Omission supplies CAST('{}' AS text[]); explicit NULL fails23502; `NOT NULL DEFAULT '{}'::text[] CHECK (array_position(countries,NULL) IS NULL); NOT NULL (explicit or PK)` |
| [ATDD-236-T1-R10 · L9550](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `release_year` (`smallint`); Source-valid integer; physical upper 32767 versus 32768 tests range separately from row acceptance; declared CHECK probes 0, 1, 9999, 10000; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (release_year BETWEEN 1 AND 9999); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R11 · L9551](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `release_month` (`smallint`); Source-valid integer; physical upper 32767 versus 32768 tests range separately from row acceptance; declared CHECK probes 0, 1, 12, 13; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (release_month BETWEEN 1 AND 12); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R12 · L9552](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `release_day` (`smallint`); Source-valid integer; physical upper 32767 versus 32768 tests range separately from row acceptance; declared CHECK probes 0, 1, 31, 32; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (release_day BETWEEN 1 AND 31); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R13 · L9553](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `release_context` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R14 · L9554](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `runtime_minutes` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes 0, 1, 100000, 100001; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (runtime_minutes BETWEEN 1 AND 100000); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R15 · L9555](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `tv_status` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (tv_status IN ('CONTINUING','ENDED','CANCELLED','UNKNOWN')); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R16 · L9556](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `end_year` (`smallint`); Source-valid integer; physical upper 32767 versus 32768 tests range separately from row acceptance; declared CHECK probes 0, 1, 9999, 10000; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (end_year BETWEEN 1 AND 9999); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R17 · L9557](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `end_month` (`smallint`); Source-valid integer; physical upper 32767 versus 32768 tests range separately from row acceptance; declared CHECK probes 0, 1, 12, 13; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (end_month BETWEEN 1 AND 12); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R18 · L9558](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `end_day` (`smallint`); Source-valid integer; physical upper 32767 versus 32768 tests range separately from row acceptance; declared CHECK probes 0, 1, 31, 32; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `CHECK (end_day BETWEEN 1 AND 31); NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R19 · L9559](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `asset_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R20 · L9560](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `attachment_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-236-T1-R21 · L9561](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `version` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` |
| [ATDD-236-T1-R22 · L9562](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-236-T1-R23 · L9563](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints) | `updated_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-238-T1-R01 · L9662](../../../05-atdd.md#case-atdd-238-enforce-genre-storage-and-normalized-uniqueness) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-238-T1-R02 · L9663](../../../05-atdd.md#case-atdd-238-enforce-genre-storage-and-normalized-uniqueness) | `label` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-238-T1-R03 · L9664](../../../05-atdd.md#case-atdd-238-enforce-genre-storage-and-normalized-uniqueness) | `normalized_label` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission fails23502; explicit NULL fails23502; `NOT NULL UNIQUE; no SQL default; owner supplies value` |
| [ATDD-238-T1-R04 · L9665](../../../05-atdd.md#case-atdd-238-enforce-genre-storage-and-normalized-uniqueness) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-239-T1-R01 · L9711](../../../05-atdd.md#case-atdd-239-enforce-movie-genre-membership-storage) | `title_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES title(id) ON DELETE CASCADE; no SQL default; owner supplies value` |
| [ATDD-239-T1-R02 · L9712](../../../05-atdd.md#case-atdd-239-enforce-movie-genre-membership-storage) | `genre_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES genre(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-242-T1-R01 · L9862](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `source` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-242-T1-R02 · L9863](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `external_id` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-242-T1-R03 · L9864](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `subject_kind` (`varchar(8)`); Source-valid technical token; 8 versus 9 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (subject_kind IN ('TITLE','PERSON')); no SQL default; owner supplies value` |
| [ATDD-242-T1-R04 · L9865](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `subject_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-242-T1-R05 · L9866](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `record_url` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-242-T1-R06 · L9867](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `retrieved_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-242-T1-R07 · L9868](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `reuse_note` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-242-T1-R08 · L9869](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `field_sources` (`jsonb`); Independent source-valid object; wrong container tests actual shape CHECK; measure PostgreSQL jsonb::text bytes at 16384 and ±1; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (jsonb_typeof(field_sources)='object' AND octet_length(field_sources::text)<=16384); no SQL default; owner supplies value` |
| [ATDD-242-T1-R09 · L9870](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `locally_changed` (`text[]`); Ordered complete strings, empty/NULL-element/two-dimensional arrays only against declared array predicates; no invented per-entry SQL limit; Omission supplies CAST('{}' AS text[]); explicit NULL fails23502; `NOT NULL DEFAULT '{}'::text[] CHECK (array_position(locally_changed,NULL) IS NULL); NOT NULL (explicit or PK)` |
| [ATDD-242-T1-R10 · L9871](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger) | `deleted_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-251-T1-R03 · L10349](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `titles`; Public / Catalog \| Search/filter AND, genre OR; active records, stable signed cursor; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R04 · L10350](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `title`; Public / Catalog \| Lowercase, hyphenated UUID; hidden/deleted returns null, no same-name substitute; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R05 · L10351](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `people`; Public / Catalog \| Literal bounded name search; active people, stable name/UUID order; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R06 · L10352](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `person`; Public / Catalog \| Lowercase, hyphenated UUID; active person only; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R07 · L10353](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `titleCredits`; Public / Catalog \| Active title AND person; bounded connection; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R08 · L10354](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `personCredits`; Public / Catalog \| Active person AND title; bounded connection; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R09 · L10355](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `genres`; Public / Catalog \| Seed taxonomy, normalized labels and stable label/UUID ordering; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R10 · L10356](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `creditRoles`; Public / Catalog \| Role category and character eligibility, normalized label/UUID order; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-253-T1-R01 · L10465](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `SearchTitles`; SearchTitles(Search) → TitlePage |
| [ATDD-253-T1-R02 · L10466](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `GetTitle`; GetTitle(Id) → Title |
| [ATDD-253-T1-R03 · L10467](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `SearchPeople`; SearchPeople(Search) → PersonPage |
| [ATDD-253-T1-R04 · L10468](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `GetPerson`; GetPerson(Id) → Person |
| [ATDD-253-T1-R05 · L10469](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `PersonCredits`; PersonCredits(PageRequest) → CreditPage |
| [ATDD-253-T1-R06 · L10470](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `TitleCredits`; TitleCredits(PageRequest) → CreditPage |
| [ATDD-253-T1-R07 · L10471](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ListGenres`; ListGenres(google.protobuf.Empty) → Genres |
| [ATDD-253-T1-R08 · L10472](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ListRoles`; ListRoles(google.protobuf.Empty) → Roles |
| [J-01 · L87](../../../02-PRD.md#32-application-journeys) | Visitor/Viewer finds a title or person \| Public catalog is available \| Search/filter active titles, inspect honest metadata and public credits, and distinguish missing information from a failed load |
| [FR-001 · L235](../../../02-PRD.md#61-functional-requirements) | Browse visible movies/TV titles, artwork, active people, and public credits \| A06/A09/A14 Required; D24/D25 Committed; product policy Committed (section 13.1) \| AC-001 through AC-005, AC-113, AC-134, AC-135 |
| [FR-002 · L236](../../../02-PRD.md#61-functional-requirements) | Search movie titles and cast/creator names with the approved matching and visibility rules \| A11/A14 Required; product policy Committed (section 13.1) \| AC-006 through AC-010, AC-022, AC-134, AC-147, AC-159 |
| [FR-003 · L237](../../../02-PRD.md#61-functional-requirements) | Combine type/genre/text/year/month filters using honest release precision \| D26 Committed; product policy Committed (section 13.1) \| AC-011 through AC-015 |
| [FR-005 · L239](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-007 · L241](../../../02-PRD.md#61-functional-requirements) | Preserve usable metadata, explicit unknowns, identity, date precision, and provenance \| D25/D26 Committed \| AC-029 through AC-032, AC-143, AC-145 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-003 · L261](../../../02-PRD.md#62-quality-and-delivery-requirements) | Saved data/artwork persist through restarts; committed persistence safeguards remain \| D18/D19 Committed \| AC-069 through AC-071 |
| [NFR-007 · L265](../../../02-PRD.md#62-quality-and-delivery-requirements) | Responsive, keyboard-usable journeys with accurate feedback and retained recoverable work \| A13/A14 Required; keyboard use Committed \| AC-083 through AC-085, AC-139, AC-147, AC-159 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-016 · L274](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [A06 · L285](../../../02-PRD.md#63-project-requirements-coverage) | List movies with artwork \| FR-001 / AC-001, AC-003 |
| [A09 · L288](../../../02-PRD.md#63-project-requirements-coverage) | List cast and creators \| FR-001/FR-005/FR-006 / AC-004, AC-022, AC-026, AC-134 |
| [A11 · L290](../../../02-PRD.md#63-project-requirements-coverage) | Search movies, cast, creators \| FR-002 / AC-006 through AC-010, AC-022, AC-134 |
| [A14 · L293](../../../02-PRD.md#63-project-requirements-coverage) | Sufficient operation feedback \| NFR-007; FR-001/FR-002/FR-004/FR-009 / AC-002, AC-009, AC-018, AC-038, AC-085 |
| [D18 · L319](../../../02-PRD.md#64-project-decision-register) | JPA/Hibernate and justified parameterized native SQL \| NFR-003 / AC-069, AC-070; techdoc |
| [D25 · L326](../../../02-PRD.md#64-project-decision-register) | Metadata, type/genres, stable identities, provenance \| FR-006/FR-007 / AC-026 through AC-032 |
| [D26 · L327](../../../02-PRD.md#64-project-decision-register) | Composable year/month filters with source date precision \| FR-003/FR-007 / AC-011 through AC-015, AC-029, AC-032 |
| [AC-001 · L339](../../../02-PRD.md#us-001-browse-titles-and-people) | List and details \| Active movies/TV series exist \| A reader opens the catalog and a title \| The correct title/type, available artwork, metadata, and visible credits appear. No episode-tracking controls appear. |
| [AC-001-C01 · L339](../../../02-PRD.md#us-001-browse-titles-and-people) | The correct title/type, available artwork, metadata, and visible credits appear. |
| [AC-001-C02 · L339](../../../02-PRD.md#us-001-browse-titles-and-people) | No episode-tracking controls appear. |
| [AC-002 · L340](../../../02-PRD.md#us-001-browse-titles-and-people) | Loading, empty, failure \| A catalog request is pending, empty, or failed \| The reader waits for it \| Show loading, a successful empty state, or a failure with retry respectively; an outage never appears as an empty catalog. |
| [AC-002-C01 · L340](../../../02-PRD.md#us-001-browse-titles-and-people) | Show loading, a successful empty state, or a failure with retry respectively; an outage never appears as an empty catalog. |
| [AC-003 · L341](../../../02-PRD.md#us-001-browse-titles-and-people) | Missing artwork \| Artwork is absent or fails to load \| A title is displayed \| A placeholder preserves readable title information and navigation; missing artwork does not make the title disappear. |
| [AC-003-C01 · L341](../../../02-PRD.md#us-001-browse-titles-and-people) | A placeholder preserves readable title information and navigation; missing artwork does not make the title disappear. |
| [AC-004 · L342](../../../02-PRD.md#us-001-browse-titles-and-people) | Shared and absent credits \| An active person has cast/creator credits, another person or title is archived, or credits are unknown \| The reader opens public credits \| Show applicable credits only where both title and person are active, without cloning the person; unknown credits remain explicit. No archived person/title information or retained credit relationship appears publicly. |
| [AC-004-C01 · L342](../../../02-PRD.md#us-001-browse-titles-and-people) | Show applicable credits only where both title and person are active, without cloning the person; unknown credits remain explicit. |
| [AC-004-C02 · L342](../../../02-PRD.md#us-001-browse-titles-and-people) | No archived person/title information or retained credit relationship appears publicly. |
| [AC-005 · L343](../../../02-PRD.md#us-001-browse-titles-and-people) | Unavailable title \| A movie was archived or deleted after listing \| A visitor/Viewer opens its old detail link \| Report that it is unavailable and provide catalog navigation; do not expose archived information or substitute a same-named title. |
| [AC-005-C01 · L343](../../../02-PRD.md#us-001-browse-titles-and-people) | Report that it is unavailable and provide catalog navigation; do not expose archived information or substitute a same-named title. |
| [AC-006 · L351](../../../02-PRD.md#us-002-search-and-filter) | Matching \| Visible titles have display/original titles and active cast/creator people have names \| The reader searches titles or people with a differently cased substring \| Return matching visible titles for title search and matching active cast/creator people for name search. Archived people are not returned, and their retained credits do not provide a public title-search match; independent matching title text still may. Account data never appear. |
| [AC-006-C01 · L351](../../../02-PRD.md#us-002-search-and-filter) | Return matching visible titles for title search and matching active cast/creator people for name search. |
| [AC-006-C02 · L351](../../../02-PRD.md#us-002-search-and-filter) | Archived people are not returned, and their retained credits do not provide a public title-search match; independent matching title text still may. |
| [AC-006-C03 · L351](../../../02-PRD.md#us-002-search-and-filter) | Account data never appear. |
| [AC-007 · L352](../../../02-PRD.md#us-002-search-and-filter) | No match / clear \| Text has no matches with current filters \| The reader searches, then clears text \| Show no matches first; clearing removes only the text restriction and preserves other filters. |
| [AC-007-C01 · L352](../../../02-PRD.md#us-002-search-and-filter) | Show no matches first; clearing removes only the text restriction and preserves other filters. |
| [AC-008 · L353](../../../02-PRD.md#us-002-search-and-filter) | Navigation and order \| Results include ties, partial dates, and unknown dates \| The reader browses results repeatedly \| Titles default to newest release context first, unknown dates last; equal input/data have consistent ordering without navigation duplication. Show correction for input outside supported limits. Search length follows section 5.5; navigation and other query limits are techdoc work. |
| [AC-008-C01 · L353](../../../02-PRD.md#us-002-search-and-filter) | Titles default to newest release context first, unknown dates last; equal input/data have consistent ordering without navigation duplication. |
| [AC-008-C02 · L353](../../../02-PRD.md#us-002-search-and-filter) | Show correction for input outside supported limits. |
| [AC-008-C03 · L353](../../../02-PRD.md#us-002-search-and-filter) | Search length follows section 5.5; navigation and other query limits are techdoc work. |
| [AC-009 · L354](../../../02-PRD.md#us-002-search-and-filter) | Rapid edits / failure \| The reader changes a pending search \| Results arrive out of order or the latest request fails \| Only current criteria control the displayed result; retain those criteria and show their loading/failure state with retry. |
| [AC-009-C01 · L354](../../../02-PRD.md#us-002-search-and-filter) | Only current criteria control the displayed result; retain those criteria and show their loading/failure state with retry. |
| [AC-010 · L355](../../../02-PRD.md#us-002-search-and-filter) | Permission / interruption \| A restricted read is attempted or navigation interrupts a search \| The request ends \| No restricted data are shown; old results cannot replace the reader's current view. |
| [AC-010-C01 · L355](../../../02-PRD.md#us-002-search-and-filter) | No restricted data are shown; old results cannot replace the reader's current view. |
| [AC-011 · L356](../../../02-PRD.md#us-002-search-and-filter) | Combined filters \| Titles vary in genre, type, year/month, and text \| The reader combines filters \| Results match at least one selected genre and every other active filter category; resetting one category preserves the others. |
| [AC-011-C01 · L356](../../../02-PRD.md#us-002-search-and-filter) | Results match at least one selected genre and every other active filter category; resetting one category preserves the others. |
| [AC-012 · L357](../../../02-PRD.md#us-002-search-and-filter) | Partial dates \| Titles are known as 2024, October 2024, and 9 October 2024 \| The reader selects 2024, then October \| All match the year; only the latter two match October. No missing month/day is fabricated. |
| [AC-012-C01 · L357](../../../02-PRD.md#us-002-search-and-filter) | All match the year; only the latter two match October. |
| [AC-012-C02 · L357](../../../02-PRD.md#us-002-search-and-filter) | No missing month/day is fabricated. |
| [AC-013 · L358](../../../02-PRD.md#us-002-search-and-filter) | Unknown year \| A title's release year is unknown \| A year filter is applied or removed \| Exclude it while a year is selected; allow it without a date filter. Never filter on the date the record was added. |
| [AC-013-C01 · L358](../../../02-PRD.md#us-002-search-and-filter) | Exclude it while a year is selected; allow it without a date filter. |
| [AC-013-C02 · L358](../../../02-PRD.md#us-002-search-and-filter) | Never filter on the date the record was added. |
| [AC-014 · L359](../../../02-PRD.md#us-002-search-and-filter) | Month validation \| No year is selected, or the month is invalid \| The reader attempts a month filter or clears year \| Month requires a year; invalid months receive correction; clearing year clears month so no hidden date restriction remains. |
| [AC-014-C01 · L359](../../../02-PRD.md#us-002-search-and-filter) | Month requires a year; invalid months receive correction; clearing year clears month so no hidden date restriction remains. |
| [AC-015 · L360](../../../02-PRD.md#us-002-search-and-filter) | Empty combination \| Valid combined filters exclude all titles \| Search completes \| Show no matches and retain editable filters; do not silently broaden them. |
| [AC-015-C01 · L360](../../../02-PRD.md#us-002-search-and-filter) | Show no matches and retain editable filters; do not silently broaden them. |
| [AC-022 · L385](../../../02-PRD.md#us-004-maintain-people-and-credits) | List/search people \| Active people exist or the public list is empty; archived people may also exist \| A reader lists/searches people, or a Maintainer opens the people archive \| Public results show active catalog people and permitted roles; the Maintainer-only archive identifies archived people and retained credits. Show distinct loading/empty/no-match/failure states; same names do not collapse identities. |
| [AC-022-C01 · L385](../../../02-PRD.md#us-004-maintain-people-and-credits) | Public results show active catalog people and permitted roles; the Maintainer-only archive identifies archived people and retained credits. |
| [AC-022-C02 · L385](../../../02-PRD.md#us-004-maintain-people-and-credits) | Show distinct loading/empty/no-match/failure states; same names do not collapse identities. |
| [AC-030 · L409](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Unknown optional data \| Runtime, country, credits, end date, external ID, or artwork is unknown \| The record is displayed or edited \| Unknown is explicit; it does not become zero runtime, invented credits, a fabricated date, or guessed artwork. |
| [AC-030-C01 · L409](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Unknown is explicit; it does not become zero runtime, invented credits, a fabricated date, or guessed artwork. |
| [AC-031 · L410](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Identity/provenance \| Remakes or people share names, or sources use similar identifiers \| Records are inspected or seeded again \| Distinct works/people retain identities; source-scoped IDs and source record/link, snapshot/retrieval date, and reuse notes remain available. Text alone never merges them. |
| [AC-031-C01 · L410](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Distinct works/people retain identities; source-scoped IDs and source record/link, snapshot/retrieval date, and reuse notes remain available. |
| [AC-031-C02 · L410](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Text alone never merges them. |
| [AC-032 · L411](../../../02-PRD.md#us-005-read-trustworthy-metadata) | TV/date labels \| A series has first-air context, status, and optional end context/runtime \| Its details are viewed \| Label series status and first-air/end information honestly; label TV duration as episode runtime and retain year/year-month/full-date precision. |
| [AC-032-C01 · L411](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Label series status and first-air/end information honestly; label TV duration as episode runtime and retain year/year-month/full-date precision. |
| [AC-147 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Plain-text safety \| Entered catalog text, names, search text, or changed values contain markup, apostrophes, or instruction-like content within applicable limits \| They appear in forms, catalog/detail pages, search results, previews, or audit history \| Treat the content as text and execute no embedded code or instructions. Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. Private registration data retain their access boundaries. |
| [AC-147-C01 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Treat the content as text and execute no embedded code or instructions. |
| [AC-147-C02 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. |
| [AC-147-C03 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Private registration data retain their access boundaries. |
| [AC-159 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Search-length boundary and correction \| Recommended search bounds of 0 to 200 are configured and other filters are selected \| A reader clears text, submits a 200-code-point query, or enters 201 code points \| Clearing text preserves other filters; 200 passes the length check. For 201, retain the full text and filters, show correction, and do not issue a truncated query or present old results as matching the invalid current query. No-match, loading, and failure remain distinct. |
| [AC-159-C01 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Clearing text preserves other filters; 200 passes the length check. |
| [AC-159-C02 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | For 201, retain the full text and filters, show correction, and do not issue a truncated query or present old results as matching the invalid current query. |
| [AC-159-C03 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | No-match, loading, and failure remain distinct. |
| [AC-070 · L860](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify applicable native SQL parameterization, joins/order/pagination, mapping, plans, and write safeguards against the chosen engine \| NFR-003; technical design |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-05 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [OD-005 · L1021](../../../02-PRD.md#131-resolved-product-decisions) | Case-insensitive partial title/person search; newest releases first, unknown last; any selected genre and all other filter categories; month requires year \| FR-002/FR-003; AC-006 through AC-015, AC-159; search-length policy in section 5.5, remaining navigation/query limits transfer to OD-019 |
| [OD-018 · L1023](../../../02-PRD.md#131-resolved-product-decisions) | Seeded TV read/search/filter/metadata only; no direct TV management or episode tracking \| FR-001/FR-007/FR-008; AC-001/AC-029/AC-032/AC-033 |
| [US-005 · L402](../../../02-PRD.md#us-005-read-trustworthy-metadata) | ### US-005: Read trustworthy metadata As a reader, I want useful metadata with source context so I can distinguish works without guessed details. Requirements: FR-007, FR-003. The complete conceptual baseline is in section 9. |
| [PRD-POLICY-04 · L120](../../../02-PRD.md#51-included-behavior) | Movies filter by their source original release context; TV series use first-air context. Year-only dates match a year and no month. Year/month dates match that month without inventing a day. Unknown years remain available without a date filter and are excluded when a year is selected. Database-added dates do not substitute for release dates. |
| [PRD-BOUND-07 · L183](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Catalog search \| 0 characters \| 200 characters \| Empty search removes only the text restriction |
| [PRD-POLICY-23 · L194](../../../02-PRD.md#validation-safe-handling-and-feedback) | A blank optional text value may explicitly clear that value; an omitted edit value leaves it unchanged. Empty search always clears the text restriction, including when a higher minimum is configured for nonempty queries. Validate a supplied nonempty optional value against its bounds. Do not force unknown optional data to become zero, an invented date, or placeholder text. Wire representations of omission and clearing belong to the techdoc. |
| [TD-C-026 · L3634](../../../04-spoolin-techdoc.md#621-date-validation-helper) | Catalog immutable partial-date helper and checks preserve precision, invalid calendar rejection and endpoint required year; pg_trgm belongs to privileged bootstrap while helper belongs to Catalog Flyway. |
| [TD-C-032 · L4385](../../../04-spoolin-techdoc.md#625-catalog-tables) | Catalog title/person/taxonomy/credit/provenance/seed membership DDL and ownership, independent visibility/IDs, partial dates, unique contribution role relation, seed/provenance removal tombstones. |
| [TD-C-039 · L5194](../../../04-spoolin-techdoc.md#72-search-and-date-semantics) | PostgreSQL pg_unicode_fast literal caseless matching and wildcard escaping; normalized role/genre identity only, honest date sorting, signed scoped 15min cursor &lt;=1024 bytes, live pages, owner cross-row genre/date/character rules. |
| [ADR-02 · L6549](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-02 \| JPA for aggregates, native SQL for search/claim/CAS; no generic repository framework \| AC-069/070; preserve DB constraints and real-engine tests |
| [TD-ROW-L2726 · L2726](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `titles` \| Public / Catalog \| Search/filter AND, genre OR; active records, stable signed cursor |
| [TD-ROW-L2727 · L2727](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `title` \| Public / Catalog \| Lowercase, hyphenated UUID; hidden/deleted returns null, no same-name substitute |
| [TD-ROW-L2728 · L2728](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `people` \| Public / Catalog \| Literal bounded name search; active people, stable name/UUID order |
| [TD-ROW-L2729 · L2729](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `person` \| Public / Catalog \| Lowercase, hyphenated UUID; active person only |
| [TD-ROW-L2730 · L2730](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `titleCredits` \| Public / Catalog \| Active title AND person; bounded connection |
| [TD-ROW-L2731 · L2731](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `personCredits` \| Public / Catalog \| Active person AND title; bounded connection |
| [TD-ROW-L2732 · L2732](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `genres` \| Public / Catalog \| Seed taxonomy, normalized labels and stable label/UUID ordering |
| [TD-ROW-L2733 · L2733](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `creditRoles` \| Public / Catalog \| Role category and character eligibility, normalized label/UUID order |
| [TD-ROW-L4397 · L4397](../../../04-spoolin-techdoc.md#title) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4398 · L4398](../../../04-spoolin-techdoc.md#title) | `kind` \| `varchar(8)` \| `NOT NULL CHECK (kind IN ('MOVIE','TV')); no SQL default; owner supplies value` \| Immutable type. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4399 · L4399](../../../04-spoolin-techdoc.md#title) | `status` \| `varchar(8)` \| `NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ARCHIVED')); NOT NULL (explicit or PK)` \| Only movie lifecycle can change; TV remains active. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4400 · L4400](../../../04-spoolin-techdoc.md#title) | `display_title` \| `text` \| `NOT NULL CHECK (length(btrim(display_title))>0); no SQL default; owner supplies value` \| Default new/changed 1–255 code points. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4401 · L4401](../../../04-spoolin-techdoc.md#title) | `original_title` \| `text` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Optional plain text; default 1–255 when nonempty. API; presence/clearing §5 \| `String?` → String/declared enum / optional string |
| [TD-ROW-L4402 · L4402](../../../04-spoolin-techdoc.md#title) | `synopsis` \| `text` \| `NOT NULL CHECK (length(btrim(synopsis))>0); no SQL default; owner supplies value` \| Required source-grounded prose; default 1–5000. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4403 · L4403](../../../04-spoolin-techdoc.md#title) | `synopsis_language` \| `varchar(35)` \| `NOT NULL; no SQL default; owner supplies value` \| BCP47 tag or und when genuinely unknown. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4404 · L4404](../../../04-spoolin-techdoc.md#title) | `original_language` \| `varchar(35)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| BCP47; unknown NULL. API; presence/clearing §5 \| `String?` → String/declared enum / optional string |
| [TD-ROW-L4405 · L4405](../../../04-spoolin-techdoc.md#title) | `countries` \| `text[]` \| `NOT NULL DEFAULT '{}'::text[] CHECK (array_position(countries,NULL) IS NULL); NOT NULL (explicit or PK)` \| Unique ISO3166-1 alpha2 codes; empty means unknown. API; presence/clearing §5 \| `List<String>` → [String!] / repeated string |
| [TD-ROW-L4406 · L4406](../../../04-spoolin-techdoc.md#title) | `release_year` \| `smallint` \| `CHECK (release_year BETWEEN 1 AND 9999); NULL allowed; no SQL default; owner supplies value` \| New manual/MCP movies required; import may be unknown. API; presence/clearing §5 \| `Short?` → Int / optional int32 |
| [TD-ROW-L4407 · L4407](../../../04-spoolin-techdoc.md#title) | `release_month` \| `smallint` \| `CHECK (release_month BETWEEN 1 AND 12); NULL allowed; no SQL default; owner supplies value` \| Only if year known. API; presence/clearing §5 \| `Short?` → Int / optional int32 |
| [TD-ROW-L4408 · L4408](../../../04-spoolin-techdoc.md#title) | `release_day` \| `smallint` \| `CHECK (release_day BETWEEN 1 AND 31); NULL allowed; no SQL default; owner supplies value` \| Only if month known, calendar-valid. API; presence/clearing §5 \| `Short?` → Int / optional int32 |
| [TD-ROW-L4409 · L4409](../../../04-spoolin-techdoc.md#title) | `release_context` \| `text` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Original release/first-air region/source context, default max 1000. API; presence/clearing §5 \| `String?` → String/declared enum / optional string |
| [TD-ROW-L4410 · L4410](../../../04-spoolin-techdoc.md#title) | `runtime_minutes` \| `integer` \| `CHECK (runtime_minutes BETWEEN 1 AND 100000); NULL allowed; no SQL default; owner supplies value` \| Movie duration or explicitly TV episode duration; NULL unknown. API; presence/clearing §5 \| `Int?` → Int / optional int32 |
| [TD-ROW-L4411 · L4411](../../../04-spoolin-techdoc.md#title) | `tv_status` \| `varchar(16)` \| `CHECK (tv_status IN ('CONTINUING','ENDED','CANCELLED','UNKNOWN')); NULL allowed; no SQL default; owner supplies value` \| Required for TV, NULL for MOVIE. API; presence/clearing §5 \| `String?` → String/declared enum / optional string |
| [TD-ROW-L4412 · L4412](../../../04-spoolin-techdoc.md#title) | `end_year` \| `smallint` \| `CHECK (end_year BETWEEN 1 AND 9999); NULL allowed; no SQL default; owner supplies value` \| TV end context; NULL unknown. API; presence/clearing §5 \| `Short?` → Int / optional int32 |
| [TD-ROW-L4413 · L4413](../../../04-spoolin-techdoc.md#title) | `end_month` \| `smallint` \| `CHECK (end_month BETWEEN 1 AND 12); NULL allowed; no SQL default; owner supplies value` \| Only with end year. API; presence/clearing §5 \| `Short?` → Int / optional int32 |
| [TD-ROW-L4414 · L4414](../../../04-spoolin-techdoc.md#title) | `end_day` \| `smallint` \| `CHECK (end_day BETWEEN 1 AND 31); NULL allowed; no SQL default; owner supplies value` \| Only with end month, calendar-valid. API; presence/clearing §5 \| `Short?` → Int / optional int32 |
| [TD-ROW-L4415 · L4415](../../../04-spoolin-techdoc.md#title) | `asset_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Logical current Media asset; no cross-DB FK. API; presence/clearing §5 \| `UUID?` → UUID / optional string |
| [TD-ROW-L4416 · L4416](../../../04-spoolin-techdoc.md#title) | `attachment_id` \| `uuid` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Current immutable Media hold ID; null iff no asset. internal; presence/clearing §5 \| `UUID?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4417 · L4417](../../../04-spoolin-techdoc.md#title) | `version` \| `bigint` \| `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` \| Optimistic row version; increment on every domain change. API; presence/clearing §5 \| `Long` → Long(decimal string) / int64 |
| [TD-ROW-L4418 · L4418](../../../04-spoolin-techdoc.md#title) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4419 · L4419](../../../04-spoolin-techdoc.md#title) | `updated_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Owner sets to transaction timestamp on update; not a DB automatic update. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4537 · L4537](../../../04-spoolin-techdoc.md#genre) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4538 · L4538](../../../04-spoolin-techdoc.md#genre) | `label` \| `text` \| `NOT NULL; no SQL default; owner supplies value` \| Display label max 100 by policy. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4539 · L4539](../../../04-spoolin-techdoc.md#genre) | `normalized_label` \| `text` \| `NOT NULL UNIQUE; no SQL default; owner supplies value` \| Trim + PostgreSQL casefold(pg_unicode_fast); NFC lookup only, not display rewrite. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4540 · L4540](../../../04-spoolin-techdoc.md#genre) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4576 · L4576](../../../04-spoolin-techdoc.md#title_genre) | `title_id` \| `uuid` \| `NOT NULL REFERENCES title(id) ON DELETE CASCADE; no SQL default; owner supplies value` \| Title. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4577 · L4577](../../../04-spoolin-techdoc.md#title_genre) | `genre_id` \| `uuid` \| `NOT NULL REFERENCES genre(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Genre. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4710 · L4710](../../../04-spoolin-techdoc.md#provenance_record) | `source` \| `varchar(64)` \| `NOT NULL; no SQL default; owner supplies value` \| Provider namespace, not title text. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4711 · L4711](../../../04-spoolin-techdoc.md#provenance_record) | `external_id` \| `text` \| `NOT NULL; no SQL default; owner supplies value` \| Provider ID max 255 policy. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4712 · L4712](../../../04-spoolin-techdoc.md#provenance_record) | `subject_kind` \| `varchar(8)` \| `NOT NULL CHECK (subject_kind IN ('TITLE','PERSON')); no SQL default; owner supplies value` \| Logical table class. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4713 · L4713](../../../04-spoolin-techdoc.md#provenance_record) | `subject_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Stable logical target, may be permanently deleted; deliberately no FK. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4714 · L4714](../../../04-spoolin-techdoc.md#provenance_record) | `record_url` \| `text` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| HTTP/HTTPS reference max 2048; never arbitrary fetch. API; presence/clearing §5 \| `String?` → String/declared enum / optional string |
| [TD-ROW-L4715 · L4715](../../../04-spoolin-techdoc.md#provenance_record) | `retrieved_at` \| `timestamptz(6)` \| `NOT NULL; no SQL default; owner supplies value` \| Snapshot/retrieval instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4716 · L4716](../../../04-spoolin-techdoc.md#provenance_record) | `reuse_note` \| `text` \| `NOT NULL; no SQL default; owner supplies value` \| License/facts/original synopsis reasoning max 4000. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4717 · L4717](../../../04-spoolin-techdoc.md#provenance_record) | `field_sources` \| `jsonb` \| `NOT NULL CHECK (jsonb_typeof(field_sources)='object' AND octet_length(field_sources::text)<=16384); no SQL default; owner supplies value` \| Field→source URL/retrieval/precision mapping; trusted schema, not executable. API; presence/clearing §5 \| `TypedJsonDTO` → typed projection/JSON / typed message |
| [TD-ROW-L4718 · L4718](../../../04-spoolin-techdoc.md#provenance_record) | `locally_changed` \| `text[]` \| `NOT NULL DEFAULT '{}'::text[] CHECK (array_position(locally_changed,NULL) IS NULL); NOT NULL (explicit or PK)` \| Fields changed after import; do not misrepresent edits as original source. API; presence/clearing §5 \| `List<String>` → [String!] / repeated string |
| [TD-ROW-L4719 · L4719](../../../04-spoolin-techdoc.md#provenance_record) | `deleted_at` \| `timestamptz(6)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Permanent deletion tombstone. internal; presence/clearing §5 \| `Instant?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4767 · L4767](../../../04-spoolin-techdoc.md#seed_member) | `seed_id` \| `varchar(64)` \| `NOT NULL REFERENCES operation_receipt(seed_version) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Manifest owner. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4768 · L4768](../../../04-spoolin-techdoc.md#seed_member) | `member_key` \| `varchar(255)` \| `NOT NULL; no SQL default; owner supplies value` \| Stable manifest key, not name. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4769 · L4769](../../../04-spoolin-techdoc.md#seed_member) | `subject_kind` \| `varchar(16)` \| `NOT NULL CHECK (subject_kind IN ('TITLE','PERSON','GENRE','ROLE','CREDIT')); no SQL default; owner supplies value` \| Seeded class. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4770 · L4770](../../../04-spoolin-techdoc.md#seed_member) | `subject_id` \| `uuid` \| `NOT NULL; no SQL default; owner supplies value` \| Stable logical ID; no polymorphic FK by design. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4771 · L4771](../../../04-spoolin-techdoc.md#seed_member) | `content_hash` \| `bytea` \| `NOT NULL CHECK (octet_length(content_hash)=32); no SQL default; owner supplies value` \| Original member content digest. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4772 · L4772](../../../04-spoolin-techdoc.md#seed_member) | `applied_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Applied once. internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4773 · L4773](../../../04-spoolin-techdoc.md#seed_member) | `removed_at` \| `timestamptz(6)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Explicit physical deletion/removal tombstone. internal; presence/clearing §5 \| `Instant?` → not exposed; no GraphQL/proto field |
| [BRAIN-A11 · L56](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A11 \| Search movies, cast, and creators \| Defined searchable fields and matching behavior, including no-results/error states |
| [BRAIN-D18 · L87](../../../01-brainstorming-result.md#4-decision-register) | D18 \| Committed \| Spring Data JPA/Hibernate for ordinary persistence; parameterized native SQL for complex joins when it improves clarity or query control |

## Implementation

1. Create title/person/genre/title_genre/credit_role/credit/provenance_record read-model DDL and explicit JPA/DTO mappings from §6.2.5. Implement Catalog's partial-date function/checks and nullable unknown fields without invented values; feature write invariants remain with their command owners.
2. Create the Catalog seed_member schema and owner tombstone helper together with the initial read-model migration. T021/T023 use that helper during removals; T035 later populates/replays membership. This schema/primitive ownership does not initialize production seed data.
3. Implement public title/person/credit/genre/role queries through Core→Catalog. Apply both-active visibility predicates at Catalog, source-scoped IDs and no account PII. Core optional artwork enrichment degrades to metadata+placeholder on Media failure.
4. Implement pg_unicode_fast case-insensitive literal substring search with parameter binding and escaped wildcard semantics, OR selected genres and AND other categories, honest year/month precision and stable date/UUID ordering.
5. Implement signed, reader/query-bound 15-minute cursors ≤1024 bytes, page/resource limits and live-page behavior. Reject invalid/expired/mismatched cursor and month-without-year; read calls add no receipt/audit mutation.
6. Verify real PostgreSQL native projections/indexes and TCP gRPC/GraphQL results against hand-derived F-CATALOG/F-ORDER sets, including same names, punctuation, archived relationships and known/unknown dates.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T012-A1:** Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year.
- **T012-A2:** Visibility: archived person/title and retained credit do not leak through search, cursor or detail, while another independently matching title remains visible.
- **T012-A3:** Identity: same-named records retain distinct IDs; invalid input is rejected without altered/truncated search and without read-side mutation.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-137](../../../05-atdd.md#case-atdd-137-search-unicode-and-punctuation-literally) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.2 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then the exact hand-checked matching IDs use PostgreSQL casefold and literal substring semantics And punctuation cannot broaden the predicate or change another row And accent/normalization distinctions and original display spelling remain intact Observation: Execute actual parameterized queries on PostgreSQL 18.6 with pg_unicode_fast for accented capitals, sigma, sharp-s and literal %, _ and backslash. Compare exact returned IDs and unchanged stored display strings; no language-runtime lowercase substitute proves this rule. |
| [ATDD-138](../../../05-atdd.md#case-atdd-138-bind-live-pagination-to-its-current-query-and-reader) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.2 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then unchanged rows follow year/month/day descending NULLS LAST and UUID ascending without unstable ties And malformed, oversized, expired or differently scoped cursors are rejected rather than producing an empty page And a refresh starts at a fresh cursor when concurrent edits move records Observation: Capture every cursor’s query/scope tuple and resulting ordered UUIDs through Core and Catalog. Repeat unchanged pages for exact equality; alter filters, actor/visibility, signature and expiry independently and observe invalid-cursor feedback. Use F-EXPIRY for equality; live edits may move rows and refresh must restart. |
| [ATDD-281](../../../05-atdd.md#case-atdd-281-order-partial-date-tuples-with-stable-uuid-ties) / Unit | Rows 2024-10-09 UUID a, 2024-10 unknown day UUID b, 2024 unknown month UUID c, 2023 UUID d, unknown UUID e; equal known date UUID f/g. | Then the first five rows order a,b,c,d,e and equal-date f precedes g And ordering does not manufacture chronological precision Observation: Compare the literal expected sequence; actual PostgreSQL ORDER BY/keyset behavior is proved by integration. |
| [ATDD-282](../../../05-atdd.md#case-atdd-282-validate-cursor-binding-and-expiry-with-a-controlled-clock) / Unit | Signed synthetic cursor bound to query Light/public scope/A, first tuple and expiresAt=T; clock T−1 microsecond/T/T+1 microsecond; changed filter/visibility/actor/signature. | Then only authentic matching scope before T is eligible And expiry equality, changed binding and altered signature return invalid cursor rather than an empty result Observation: Use actual in-process MAC verification on synthetic bytes and compare eligibility; no page/database/network is involved. |
| [ATDD-317](../../../05-atdd.md#case-atdd-317-keep-user-values-out-of-query-structure) / Unit | Literal strings apostrophe/OR1=1/comment/UNION and %/_/backslash; legal sort/filter enums versus unknown structural token. | Then user text remains a bound value rather than an identifier or SQL/HQL fragment And only fixed selected structural choices are eligible And literal wildcard escaping preserves search semantics without being the SQL injection defense Observation: Compare fixed template/parameter decisions with independent input examples. A mock query builder cannot establish real driver binding, so the complementary PostgreSQL case is required. |

Partial implementation contributions: [ATDD-001](../../../05-atdd.md#case-atdd-001-list-and-details), [ATDD-002](../../../05-atdd.md#case-atdd-002-loading-empty-failure), [ATDD-003](../../../05-atdd.md#case-atdd-003-missing-artwork), [ATDD-004](../../../05-atdd.md#case-atdd-004-shared-and-absent-credits), [ATDD-005](../../../05-atdd.md#case-atdd-005-unavailable-title), [ATDD-006](../../../05-atdd.md#case-atdd-006-matching), [ATDD-007](../../../05-atdd.md#case-atdd-007-no-match--clear), [ATDD-008](../../../05-atdd.md#case-atdd-008-navigation-and-order), [ATDD-009](../../../05-atdd.md#case-atdd-009-rapid-edits--failure), [ATDD-010](../../../05-atdd.md#case-atdd-010-permission--interruption), [ATDD-011](../../../05-atdd.md#case-atdd-011-combined-filters), [ATDD-012](../../../05-atdd.md#case-atdd-012-partial-dates), [ATDD-013](../../../05-atdd.md#case-atdd-013-unknown-year), [ATDD-014](../../../05-atdd.md#case-atdd-014-month-validation), [ATDD-015](../../../05-atdd.md#case-atdd-015-empty-combination), [ATDD-030](../../../05-atdd.md#case-atdd-030-unknown-optional-data), [ATDD-031](../../../05-atdd.md#case-atdd-031-identityprovenance), [ATDD-032](../../../05-atdd.md#case-atdd-032-tvdate-labels), [ATDD-133](../../../05-atdd.md#case-atdd-133-search-length-boundary-and-correction), [ATDD-022](../../../05-atdd.md#case-atdd-022-listsearch-people), [ATDD-139](../../../05-atdd.md#case-atdd-139-restore-complete-criteria-without-accepting-old-results), [ATDD-236](../../../05-atdd.md#case-atdd-236-enforce-title-storage-and-lifecycle-constraints), [ATDD-238](../../../05-atdd.md#case-atdd-238-enforce-genre-storage-and-normalized-uniqueness), [ATDD-239](../../../05-atdd.md#case-atdd-239-enforce-movie-genre-membership-storage), [ATDD-242](../../../05-atdd.md#case-atdd-242-enforce-bounded-provenance-storage-and-deletion-ledger), [ATDD-256](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback), [ATDD-257](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback), [ATDD-303](../../../05-atdd.md#case-atdd-303-keep-only-the-latest-search-generation-visible). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T012` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T012` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| boundary-tests | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T013 — Connect discovery, details and public people views](T013-connect-discovery-details-and-public-people-views.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
