---
kind: task
id: T019
group: G10
depends_on:
- T008
- T009
- T010
- T012
- T017
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T019 — Implement shared person writes and identity

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-023 | implements |
| ATDD-026 | implements |
| ATDD-027 | implements |
| ATDD-079 | implements |
| ATDD-081 | implements |
| ATDD-114 | implements |
| ATDD-237 | implements |
| ATDD-237-T1-R01 | implements |
| ATDD-237-T1-R02 | implements |
| ATDD-237-T1-R03 | implements |
| ATDD-237-T1-R04 | implements |
| ATDD-237-T1-R05 | implements |
| ATDD-237-T1-R06 | implements |
| ATDD-237-T1-R07 | implements |
| ATDD-237-T1-R08 | implements |
| ATDD-251-T1-R12 | implements |
| ATDD-251-T1-R15 | implements |
| ATDD-251-T1-R26 | implements |
| ATDD-251-T1-R27 | implements |
| ATDD-253-T1-R11 | implements |
| ATDD-253-T1-R13 | implements |
| ATDD-253-T1-R17 | implements |
| ATDD-261 | implements |
| ATDD-262 | implements |
| FR-005 | implements |
| FR-006 | implements |
| FR-017 | implements |
| NFR-014 | implements |
| A09 | implements |
| A10 | implements |
| D09 | implements |
| D25 | implements |
| AC-023 | implements |
| AC-023-C01 | implements |
| AC-023-C02 | implements |
| AC-026 | implements |
| AC-026-C01 | implements |
| AC-027 | implements |
| AC-027-C01 | implements |
| AC-027-C02 | implements |
| AC-104 | implements |
| AC-104-C01 | implements |
| AC-107 | implements |
| AC-107-C01 | implements |
| AC-140 | implements |
| AC-140-C01 | implements |
| AC-140-C02 | implements |
| G-03 | implements |
| OD-009 | implements |
| US-002 | implements |
| PRD-PERM-01 | implements |
| PRD-PERM-05 | implements |
| PRD-POLICY-03 | implements |
| PRD-POLICY-05 | implements |
| PRD-POLICY-18 | implements |
| PRD-BOUND-01 | implements |
| PRD-BOUND-05 | implements |
| PRD-CONCEPT-09 | implements |
| TD-ROW-L2735 | both |
| TD-ROW-L2738 | both |
| TD-ROW-L2749 | both |
| TD-ROW-L2750 | both |
| TD-ROW-L4488 | both |
| TD-ROW-L4489 | both |
| TD-ROW-L4490 | both |
| TD-ROW-L4491 | both |
| TD-ROW-L4492 | both |
| TD-ROW-L4493 | both |
| TD-ROW-L4494 | both |
| TD-ROW-L4495 | both |

## Prerequisites

- [T008 — Enforce scoped service identity and shared admission](../../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T009 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T010 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T012 — Serve precise public catalog and people reads](../../07-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year.
- [T017 — Implement authoritative movie creation and edits](../../09-movie-management/tasks/T017-implement-authoritative-movie-creation-and-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/person/write/ | T019 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/catalogview/person/ | T019 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/person/ | T019 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/catalog/people/ | T019 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/person/write/ | T019 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/catalogview/person/ | T019 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [4.3 SpoolIn Catalog service](../../../04-spoolin-techdoc.md#43-spoolin-catalog-service): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.3 Operation, permission and deadline inventory](../../../04-spoolin-techdoc.md#53-operation-permission-and-deadline-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.1 Transaction inventory](../../../04-spoolin-techdoc.md#71-transaction-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-237-T1-R01 · L9609](../../../05-atdd.md#case-atdd-237-enforce-shared-person-storage-and-lifecycle-constraints) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-237-T1-R02 · L9610](../../../05-atdd.md#case-atdd-237-enforce-shared-person-storage-and-lifecycle-constraints) | `name` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (length(btrim(name))>0); no SQL default; owner supplies value` |
| [ATDD-237-T1-R03 · L9611](../../../05-atdd.md#case-atdd-237-enforce-shared-person-storage-and-lifecycle-constraints) | `biography` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-237-T1-R04 · L9612](../../../05-atdd.md#case-atdd-237-enforce-shared-person-storage-and-lifecycle-constraints) | `status` (`varchar(8)`); Source-valid technical token; 8 versus 9 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission supplies 'ACTIVE'; explicit NULL fails23502; `NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ARCHIVED')); NOT NULL (explicit or PK)` |
| [ATDD-237-T1-R05 · L9613](../../../05-atdd.md#case-atdd-237-enforce-shared-person-storage-and-lifecycle-constraints) | `impact_version` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (impact_version >= 0); NOT NULL (explicit or PK)` |
| [ATDD-237-T1-R06 · L9614](../../../05-atdd.md#case-atdd-237-enforce-shared-person-storage-and-lifecycle-constraints) | `version` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` |
| [ATDD-237-T1-R07 · L9615](../../../05-atdd.md#case-atdd-237-enforce-shared-person-storage-and-lifecycle-constraints) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-237-T1-R08 · L9616](../../../05-atdd.md#case-atdd-237-enforce-shared-person-storage-and-lifecycle-constraints) | `updated_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-251-T1-R12 · L10358](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `managedPerson`; Maintainer / Catalog \| Private active/archived person projection; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R15 · L10361](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `managedPeople`; Maintainer / Catalog \| Explicit ACTIVE/ARCHIVED state; name/UUID order; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R26 · L10372](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `createPerson`; Maintainer / Catalog \| Required Unicode name; optional biography; independent identity; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R27 · L10373](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `updatePerson`; Maintainer / Catalog \| Active person/current version; changed fields only; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-253-T1-R11 · L10475](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ManagedPeople`; ManagedPeople(PageRequest) → PersonPage |
| [ATDD-253-T1-R13 · L10477](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ManagedPerson`; ManagedPerson(Id) → Person |
| [ATDD-253-T1-R17 · L10481](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `WritePerson`; WritePerson(PersonWrite) → Outcome |
| [FR-005 · L239](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-006 · L240](../../../02-PRD.md#61-functional-requirements) | Maintain cast/creator credits, starter/custom roles, and multiple roles/character names; respect person lifecycle \| A09/A10 support; D25 Committed; product policy Committed (section 13.1) \| AC-026 through AC-028, AC-132, AC-135, AC-136, AC-143/AC-144 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-014 · L272](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preserve stale drafts and prevent unintended duplicate effects without merging distinct works/people \| Approved product policy Committed; D09 mechanism Recommended; D21 safe replay Committed \| AC-104 through AC-107 |
| [A09 · L288](../../../02-PRD.md#63-project-requirements-coverage) | List cast and creators \| FR-001/FR-005/FR-006 / AC-004, AC-022, AC-026, AC-134 |
| [A10 · L289](../../../02-PRD.md#63-project-requirements-coverage) | Add, update, delete cast and creators \| FR-005/FR-006 / AC-023 through AC-028, AC-131 through AC-133 |
| [D09 · L310](../../../02-PRD.md#64-project-decision-register) | Optimistic concurrency; separate duplicate requests/data \| Stale-save rejection selected as product behavior; mechanism stays with techdoc. NFR-014 / AC-104 through AC-107 |
| [D25 · L326](../../../02-PRD.md#64-project-decision-register) | Metadata, type/genres, stable identities, provenance \| FR-006/FR-007 / AC-026 through AC-032 |
| [AC-023 · L386](../../../02-PRD.md#us-004-maintain-people-and-credits) | Create/update \| A Maintainer supplies a person name within the effective limits in section 5.5 and valid available details for a new or active person \| Save succeeds \| Preserve one shared person identity and show corrections through its visible credits. Reject missing/invalid required information with retained input and no partial save; archived people require restoration before editing. |
| [AC-023-C01 · L386](../../../02-PRD.md#us-004-maintain-people-and-credits) | Preserve one shared person identity and show corrections through its visible credits. |
| [AC-023-C02 · L386](../../../02-PRD.md#us-004-maintain-people-and-credits) | Reject missing/invalid required information with retained input and no partial save; archived people require restoration before editing. |
| [AC-026 · L390](../../../02-PRD.md#us-004-maintain-people-and-credits) | Multiple roles \| An active shared person and active movie exist \| A Maintainer assigns credits \| Actor/Director/Writer/Producer are available; custom roles fit Cast or Creator; one person may have multiple roles and multiple acting character names without cloning the person. |
| [AC-026-C01 · L390](../../../02-PRD.md#us-004-maintain-people-and-credits) | Actor/Director/Writer/Producer are available; custom roles fit Cast or Creator; one person may have multiple roles and multiple acting character names without cloning the person. |
| [AC-027 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Invalid/duplicate credit \| A name/category/reference is missing, the selected person is archived, or an identical credit exists \| The Maintainer saves a new credit \| Request correction for invalid input; reject assignment to an archived person until restored, and do not create duplicate identical display entries. Custom labels use consistent category/identity treatment. |
| [AC-027-C01 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Request correction for invalid input; reject assignment to an archived person until restored, and do not create duplicate identical display entries. |
| [AC-027-C02 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Custom labels use consistent category/identity treatment. |
| [AC-104 · L491](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Stale save \| Two Maintainers edit the same movie/person \| One saves before the other \| Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-104-C01 · L491](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-107 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Similar real-world records \| Independent works/people share names \| Records are created or compared \| Preserve distinct identities and do not merge by text alone; explain genuine identity conflicts without silently changing another record. |
| [AC-107-C01 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Preserve distinct identities and do not merge by text alone; explain genuine identity conflicts without silently changing another record. |
| [AC-140 · L530](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Person-name boundaries \| Person-name defaults are configured and the actor is a Maintainer editing an active person or creating one \| They submit otherwise valid person data with a name of 1, 100, 0, or 101 Unicode code points after trimming \| Accept lengths 1 and 100, including a single-character professional name; reject empty and 101 with a field-specific error and no partial change. Preserve the rejected draft for correction, and never merge same-named people. |
| [AC-140-C01 · L530](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Accept lengths 1 and 100, including a single-character professional name; reject empty and 101 with a field-specific error and no partial change. |
| [AC-140-C02 · L530](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Preserve the rejected draft for correction, and never merge same-named people. |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [OD-009 · L1022](../../../02-PRD.md#131-resolved-product-decisions) | Reject stale writes, retain draft, require review; no force overwrite \| NFR-014; AC-020/AC-025/AC-104; concurrency mechanism transfers to techdoc |
| [US-002 · L345](../../../02-PRD.md#us-002-search-and-filter) | ### US-002: Search and filter As a reader, I want search and filters to narrow results together so I can find titles or people. Requirements: FR-002, FR-003, NFR-013. People search concerns catalog people, not account holders. |
| [PRD-PERM-01 · L68](../../../02-PRD.md#31-permission-matrix) | Browse/search/filter active movies and TV series; view metadata and public people/credits \| Yes \| Yes \| Yes |
| [PRD-PERM-05 · L72](../../../02-PRD.md#31-permission-matrix) | Manage shared people and custom credit roles \| No \| No \| Yes |
| [PRD-POLICY-03 · L118](../../../02-PRD.md#51-included-behavior) | Search is case-insensitive partial matching on display/original titles and person names. Empty search text adds no text restriction. Title ordering defaults to newest release context first, with unknown release dates last. Within the precision the source provides, results remain consistently ordered; tie-breakers and navigation mechanics belong to the techdoc. Multiple selected genres match any selected genre; type, genre, year/month, and search combine as narrowing categories. Month requires year; clearing year also clears month. Clearing text preserves other filters. Each visible result must match the currently displayed criteria. |
| [PRD-POLICY-05 · L122](../../../02-PRD.md#51-included-behavior) | Cast and creators share reusable Person records. Start with Actor in Cast and Director, Writer, and Producer in Creator. Maintainers may add custom labels within either category. A person may have several roles on a movie; an acting credit may list several character names. Repeated identical credits do not create duplicated display entries. Names and titles are not globally unique identities; similarly named people and remakes remain distinct. |
| [PRD-POLICY-18 · L164](../../../02-PRD.md#name-defaults) | Catalog person names and registration names use separate, configurable defaults. Both are single name fields; do not require a given-name/family-name split, multiple words, or a surname. Support mononyms, Unicode, spaces, hyphens, and apostrophes. A length check does not prove that a name is authentic. |
| [PRD-BOUND-01 · L168](../../../02-PRD.md#name-defaults) | Catalog person name \| 1 character \| 100 characters \| Committed selected defaults: permit short professional names, including single-character names |
| [PRD-BOUND-05 · L181](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Custom credit role \| 1 character \| 100 characters \| Must belong to Cast or Creator |
| [PRD-CONCEPT-09 · L817](../../../02-PRD.md#9-conceptual-information-and-privacy) | Account \| Private single-field name/email, protected password credentials, and Viewer/Maintainer role. Registration-name defaults differ from catalog person-name defaults in section 5.5. No public account profile or role-selection registration. |
| [TD-ROW-L2735 · L2735](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `managedPerson` \| Maintainer / Catalog \| Private active/archived person projection |
| [TD-ROW-L2738 · L2738](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `managedPeople` \| Maintainer / Catalog \| Explicit ACTIVE/ARCHIVED state; name/UUID order |
| [TD-ROW-L2749 · L2749](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `createPerson` \| Maintainer / Catalog \| Required Unicode name; optional biography; independent identity |
| [TD-ROW-L2750 · L2750](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `updatePerson` \| Maintainer / Catalog \| Active person/current version; changed fields only |
| [TD-ROW-L4488 · L4488](../../../04-spoolin-techdoc.md#person) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4489 · L4489](../../../04-spoolin-techdoc.md#person) | `name` \| `text` \| `NOT NULL CHECK (length(btrim(name))>0); no SQL default; owner supplies value` \| Default 1–100 code points, not unique. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4490 · L4490](../../../04-spoolin-techdoc.md#person) | `biography` \| `text` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Optional plain text, default max 5000. API; presence/clearing §5 \| `String?` → String/declared enum / optional string |
| [TD-ROW-L4491 · L4491](../../../04-spoolin-techdoc.md#person) | `status` \| `varchar(8)` \| `NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ARCHIVED')); NOT NULL (explicit or PK)` \| No person edits/new assignments while archived. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4492 · L4492](../../../04-spoolin-techdoc.md#person) | `impact_version` \| `bigint` \| `NOT NULL DEFAULT 0 CHECK (impact_version >= 0); NOT NULL (explicit or PK)` \| Increment on any affected-credit/title display or lifecycle change. API; presence/clearing §5 \| `Long` → Long(decimal string) / int64 |
| [TD-ROW-L4493 · L4493](../../../04-spoolin-techdoc.md#person) | `version` \| `bigint` \| `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` \| Optimistic row version; increment on every domain change. API; presence/clearing §5 \| `Long` → Long(decimal string) / int64 |
| [TD-ROW-L4494 · L4494](../../../04-spoolin-techdoc.md#person) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4495 · L4495](../../../04-spoolin-techdoc.md#person) | `updated_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Owner sets to transaction timestamp on update; not a DB automatic update. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |

## Implementation

1. Implement createPerson/updatePerson and managed person projections with active-record editing, version/policy/actor checks and exact biography/optional-field rules.
2. Preserve single-field Unicode names and separate identities for same names. Person default 1–100 differs from account default 2–100; no fabricated surname or implicit identity merge.
3. Commit person/provenance/receipt/audit atomically and update projections through surviving credits without copying people. Archived targets require restoration before edits/new assignments.
4. Expose managed list/detail state through current permission and safe DTOs; no account profile/search linkage.
5. Test duplicate operations vs duplicate real-world names, stale/removed/archived targets and policy tightening with unchanged fields.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T019-A1:** Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact.
- **T019-A2:** Identity: two Alex records stay distinct and a legitimate correction appears through that person's permitted credits.
- **T019-A3:** State: archived/missing/unauthorized person cannot be edited or recreated by stale retry; receipt/audit reflects only confirmed effects.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| Local contract contribution | Owned source clauses and selected section fixtures | Verify generated/owner behavior locally; full assigned case proof remains with the downstream owner identified in the root register |

Partial implementation contributions: [ATDD-023](../../../05-atdd.md#case-atdd-023-createupdate), [ATDD-026](../../../05-atdd.md#case-atdd-026-multiple-roles), [ATDD-027](../../../05-atdd.md#case-atdd-027-invalidduplicate-credit), [ATDD-079](../../../05-atdd.md#case-atdd-079-stale-save), [ATDD-081](../../../05-atdd.md#case-atdd-081-similar-real-world-records), [ATDD-114](../../../05-atdd.md#case-atdd-114-person-name-boundaries), [ATDD-237](../../../05-atdd.md#case-atdd-237-enforce-shared-person-storage-and-lifecycle-constraints), [ATDD-261](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback), [ATDD-262](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T019` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T019` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer self-reviews first. A fresh independent reviewer then receives this task, inherited constraints, exact source clauses, baseline and resulting change, actual tests and sanitized evidence. Record distinct reviewer identities, reviewed revision, each finding as required/advisory, correction and recheck; labels alone do not establish independence. Scrutinize permission/secret boundaries, races, rollback/replay, lifetime evidence and the real owner boundary specified in this task. Implement → check → self-review → independent review → correct → rerun affected checks. Broaden testing only for a new concern. If no fresh reviewer is available, leave In review with exact next action; self-review cannot replace it.

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
- Reach Ready instructions and Passed evidence for every required check, including self-review, fresh independent review and applicable visual/interaction checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T022 — Connect credit editing and verify the people outcome](T022-connect-credit-editing-and-verify-the-people-outcome.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
