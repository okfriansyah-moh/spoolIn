---
kind: task
id: T017
group: G09
depends_on:
- T008
- T009
- T010
- T012
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T017 — Implement authoritative movie creation and edits

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-016 | implements |
| ATDD-017 | implements |
| ATDD-018 | implements |
| ATDD-021 | implements |
| ATDD-080 | implements |
| ATDD-053 | implements |
| ATDD-054 | implements |
| ATDD-055 | implements |
| ATDD-056 | implements |
| ATDD-088 | implements |
| ATDD-089 | implements |
| ATDD-101 | implements |
| ATDD-147 | both |
| ATDD-148 | both |
| ATDD-151 | verifies |
| ATDD-185 | implements |
| ATDD-224 | implements |
| ATDD-224-T1-R01 | implements |
| ATDD-224-T1-R02 | implements |
| ATDD-224-T1-R03 | implements |
| ATDD-224-T1-R04 | implements |
| ATDD-224-T1-R05 | implements |
| ATDD-224-T1-R06 | implements |
| ATDD-224-T1-R07 | implements |
| ATDD-224-T1-R08 | implements |
| ATDD-224-T1-R09 | implements |
| ATDD-224-T1-R10 | implements |
| ATDD-224-T1-R11 | implements |
| ATDD-251-T1-R11 | implements |
| ATDD-251-T1-R14 | implements |
| ATDD-251-T1-R23 | implements |
| ATDD-251-T1-R24 | implements |
| ATDD-253-T1-R10 | implements |
| ATDD-253-T1-R12 | implements |
| ATDD-253-T1-R16 | implements |
| ATDD-265 | implements |
| ATDD-298 | both |
| ATDD-307 | implements |
| J-02 | both |
| J-11 | implements |
| SC-04 | both |
| FR-004 | both |
| FR-005 | both |
| FR-013 | both |
| FR-014 | both |
| NFR-003 | both |
| NFR-008 | both |
| NFR-013 | both |
| NFR-014 | both |
| NFR-015 | implements |
| A04 | both |
| A05 | both |
| A07 | both |
| A10 | both |
| A14 | implements |
| D03 | both |
| D05 | both |
| D08 | both |
| D09 | both |
| D18 | both |
| D19 | both |
| D20 | both |
| AC-016 | both |
| AC-016-C01 | both |
| AC-017 | implements |
| AC-017-C01 | implements |
| AC-017-C02 | implements |
| AC-018 | implements |
| AC-018-C01 | implements |
| AC-018-C02 | implements |
| AC-021 | implements |
| AC-021-C01 | implements |
| AC-024 | both |
| AC-024-C01 | both |
| AC-024-C02 | both |
| AC-024-C03 | both |
| AC-137 | both |
| AC-137-C01 | both |
| AC-137-C02 | both |
| AC-137-C03 | both |
| AC-053 | both |
| AC-053-C01 | both |
| AC-053-C02 | both |
| AC-054 | implements |
| AC-054-C01 | implements |
| AC-055 | implements |
| AC-055-C01 | implements |
| AC-055-C02 | implements |
| AC-056 | implements |
| AC-056-C01 | implements |
| AC-056-C02 | implements |
| AC-056-C03 | implements |
| AC-114 | implements |
| AC-114-C01 | implements |
| AC-114-C02 | implements |
| AC-115 | implements |
| AC-115-C01 | implements |
| AC-127 | implements |
| AC-127-C01 | implements |
| AC-127-C02 | implements |
| AC-104 | both |
| AC-104-C01 | both |
| AC-105 | both |
| AC-105-C01 | both |
| AC-069 | both |
| AC-070 | both |
| AC-087 | both |
| AC-100 | both |
| AC-106 | both |
| G-02 | both |
| G-03 | both |
| AC-057 | both |
| AC-057-C01 | both |
| OD-004 | both |
| OD-009 | both |
| OD-010 | both |
| OD-011 | implements |
| US-015 | implements |
| PRD-PERM-06 | implements |
| PRD-PERM-07 | implements |
| PRD-BOUND-03 | implements |
| PRD-BOUND-04 | implements |
| PRD-POLICY-24 | implements |
| PRD-POLICY-26 | implements |
| PRD-CONCEPT-04 | implements |
| PRD-CONCEPT-07 | implements |
| PRD-HANDOFF-05 | implements |
| TD-ROW-L2734 | both |
| TD-ROW-L2737 | both |
| TD-ROW-L2746 | both |
| TD-ROW-L2747 | both |
| BRAIN-A04 | both |
| BRAIN-A05 | both |
| BRAIN-A07 | both |
| BRAIN-A10 | both |
| BRAIN-D03 | both |
| BRAIN-D05 | both |
| BRAIN-D08 | both |
| BRAIN-D09 | both |
| BRAIN-D18 | both |
| BRAIN-D19 | both |
| BRAIN-D20 | both |

## Prerequisites

- [T008 — Enforce scoped service identity and shared admission](../../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T009 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T010 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T012 — Serve precise public catalog and people reads](../../07-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/movie/ | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/taxonomy/write/ | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/operations/audit/ | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/resources/db/migration/V003__catalog_audit.sql | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/catalogview/movie/ | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/movie/ | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/catalog/writes/ | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/taxonomy/write/ | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/operations/audit/ | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/catalogview/movie/ | T017 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [4.3 SpoolIn Catalog service](../../../04-spoolin-techdoc.md#43-spoolin-catalog-service): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.1.6 Browser mutations](../../../04-spoolin-techdoc.md#516-browser-mutations): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.3 Operation, permission and deadline inventory](../../../04-spoolin-techdoc.md#53-operation-permission-and-deadline-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.1 Transaction inventory](../../../04-spoolin-techdoc.md#71-transaction-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-224-T1-R01 · L8880](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-224-T1-R02 · L8881](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `operation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `REFERENCES operation_receipt(id) ON DELETE RESTRICT; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-224-T1-R03 · L8882](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `actor_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-224-T1-R04 · L8883](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `actor_kind` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (actor_kind IN ('HUMAN','SERVICE','ANONYMOUS')); no SQL default; owner supplies value` |
| [ATDD-224-T1-R05 · L8884](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `action` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-224-T1-R06 · L8885](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `subject_kind` (`varchar(24)`); Source-valid technical token; 24 versus 25 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-224-T1-R07 · L8886](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `subject_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-224-T1-R08 · L8887](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `outcome` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (outcome IN ('SUCCEEDED','DENIED','FAILED','APPROVED','REJECTED')); no SQL default; owner supplies value` |
| [ATDD-224-T1-R09 · L8888](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `changes` (`jsonb`); Independent source-valid object; wrong container tests actual shape CHECK; measure PostgreSQL jsonb::text bytes at 65536 and ±1; Omission supplies CAST('{}' AS jsonb); explicit NULL fails23502; `NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(changes)='object' AND octet_length(changes::text)<=65536); NOT NULL (explicit or PK)` |
| [ATDD-224-T1-R10 · L8889](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `correlation_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-224-T1-R11 · L8890](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-251-T1-R11 · L10357](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `managedTitle`; Maintainer / Catalog \| Private active/archived surviving movie or TV projection; TV remains read-only; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R14 · L10360](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `managedTitles`; Maintainer / Catalog \| Explicit ACTIVE/ARCHIVED state; stable page order; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R23 · L10369](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `createMovie`; Maintainer / Catalog \| Current policy, required metadata/genres/partial date; duplicate name allowed, source identity separate; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R24 · L10370](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `updateMovie`; Maintainer / Catalog \| Active MOVIE, expected movie version, changed-field mask/current policy; no TV edits; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-253-T1-R10 · L10474](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ManagedTitles`; ManagedTitles(PageRequest) → TitlePage |
| [ATDD-253-T1-R12 · L10476](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ManagedTitle`; ManagedTitle(Id) → Title |
| [ATDD-253-T1-R16 · L10480](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `WriteMovie`; WriteMovie(MovieWrite) → Outcome |
| [J-02 · L90](../../../02-PRD.md#32-application-journeys) | Maintainer corrects movie information \| Signed in with movie-management permission \| Create/edit valid core metadata; see saved information or a retained draft with corrective feedback |
| [J-11 · L96](../../../02-PRD.md#32-application-journeys) | Maintainer reviews accountability \| Signed in \| Inspect read-only catalog history and AI approval/creation outcomes |
| [SC-04 · L105](../../../02-PRD.md#4-goals-and-success-criteria) | Protect access and accountability \| Public/Viewer/Maintainer permissions hold; private account data stay protected; permitted changes and AI approvals are attributable \| Unverified; Committed |
| [FR-004 · L240](../../../02-PRD.md#61-functional-requirements) | Create/edit movies and support archive, restore, and confirmed permanent deletion \| A07/A14 Required; approved lifecycle Committed \| AC-016 through AC-021, AC-108 through AC-110, AC-130, AC-143/AC-144, AC-148, AC-155 |
| [FR-005 · L241](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-013 · L249](../../../02-PRD.md#61-functional-requirements) | Keep attributable change/security records and expose read-only Maintainer history for movie/person lifecycle, credits, artwork, and AI approval/creation \| D05/D20 and audit UI Committed \| AC-053 through AC-056, AC-114, AC-115, AC-127, AC-131 through AC-133, AC-137 |
| [FR-014 · L250](../../../02-PRD.md#61-functional-requirements) | Preserve required follow-up through interruptions and make exhausted work inspectable/recoverable \| D20/D21 Committed \| AC-057 through AC-060 |
| [NFR-003 · L263](../../../02-PRD.md#62-quality-and-delivery-requirements) | Saved data/artwork persist through restarts; committed persistence safeguards remain \| D18/D19 Committed \| AC-069 through AC-071 |
| [NFR-008 · L268](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-013 · L273](../../../02-PRD.md#62-quality-and-delivery-requirements) | Interfaces implement the product's validation, optional data, access, and failure behavior \| A04/A05/A14 Required support; detailed contracts Open \| AC-100 through AC-103, AC-142/AC-143, AC-148 |
| [NFR-014 · L274](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preserve stale drafts and prevent unintended duplicate effects without merging distinct works/people \| Approved product policy Committed; D09 mechanism Recommended; D21 safe replay Committed \| AC-104 through AC-107 |
| [NFR-015 · L275](../../../02-PRD.md#62-quality-and-delivery-requirements) | Protect account PII with the selected encryption/rotation constraint; passwords use one-way hashing \| Committed product policy (section 13.1) \| AC-123 through AC-125, AC-127, AC-128; section 10 |
| [A04 · L285](../../../02-PRD.md#63-assessment-coverage) | Real service-to-service gRPC \| NFR-001/NFR-008 / AC-065, AC-087 |
| [A05 · L286](../../../02-PRD.md#63-assessment-coverage) | Frontend application communication through GraphQL \| NFR-001/NFR-013 / AC-066, AC-100 |
| [A07 · L288](../../../02-PRD.md#63-assessment-coverage) | Add, update, delete movies \| FR-004 / AC-016 through AC-021, AC-108 through AC-110, AC-130 |
| [A10 · L291](../../../02-PRD.md#63-assessment-coverage) | Add, update, delete cast and creators \| FR-005/FR-006 / AC-023 through AC-028, AC-131 through AC-133 |
| [A14 · L295](../../../02-PRD.md#63-assessment-coverage) | Sufficient operation feedback \| NFR-007; FR-001/FR-002/FR-004/FR-009 / AC-002, AC-009, AC-018, AC-038, AC-085 |
| [D03 · L306](../../../02-PRD.md#64-project-decision-register) | GraphQL frontend and gRPC backend boundaries \| NFR-001/NFR-013 / AC-065, AC-066, AC-100 |
| [D05 · L308](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D08 · L311](../../../02-PRD.md#64-project-decision-register) | Queries/mutations without subscriptions \| Remains an engineering recommendation; NFR-013 / AC-100 |
| [D09 · L312](../../../02-PRD.md#64-project-decision-register) | Optimistic concurrency; separate duplicate requests/data \| Stale-save rejection selected as product behavior; mechanism stays with techdoc. NFR-014 / AC-104 through AC-107 |
| [D18 · L321](../../../02-PRD.md#64-project-decision-register) | JPA/Hibernate and justified parameterized native SQL \| NFR-003 / AC-069, AC-070; techdoc |
| [D19 · L322](../../../02-PRD.md#64-project-decision-register) | Unit/integration, real gRPC and selected database checks \| NFR-008 / AC-086 through AC-088 |
| [D20 · L323](../../../02-PRD.md#64-project-decision-register) | Atomic successful audit, durable follow-up, saga only when needed \| FR-013/FR-014 / AC-053, AC-054, AC-057 through AC-060; techdoc mechanics |
| [AC-016 · L370](../../../02-PRD.md#us-003-maintain-movies) | Create \| A Maintainer supplies title, synopsis, genre(s), and release year \| A valid creation completes \| One active movie appears with its saved information and an attributable audit entry; missing optional values remain unknown. |
| [AC-016-C01 · L370](../../../02-PRD.md#us-003-maintain-movies) | One active movie appears with its saved information and an attributable audit entry; missing optional values remain unknown. |
| [AC-017 · L371](../../../02-PRD.md#us-003-maintain-movies) | Update \| An active movie is open for editing \| Valid edits are saved \| Show the confirmed updated information under the same movie identity, preserving unaffected details and relationships. Clearing an optional value differs from leaving it unchanged. |
| [AC-017-C01 · L371](../../../02-PRD.md#us-003-maintain-movies) | Show the confirmed updated information under the same movie identity, preserving unaffected details and relationships. |
| [AC-017-C02 · L371](../../../02-PRD.md#us-003-maintain-movies) | Clearing an optional value differs from leaving it unchanged. |
| [AC-018 · L372](../../../02-PRD.md#us-003-maintain-movies) | Validation / denied write \| Required data are blank/missing, changed input violates the effective bounds/content policy, or permission is absent \| A save is attempted \| No partial catalog edit occurs; show corrective/permission feedback and retain permitted input. Never retain or echo passwords as form-recovery data. |
| [AC-018-C01 · L372](../../../02-PRD.md#us-003-maintain-movies) | No partial catalog edit occurs; show corrective/permission feedback and retain permitted input. |
| [AC-018-C02 · L372](../../../02-PRD.md#us-003-maintain-movies) | Never retain or echo passwords as form-recovery data. |
| [AC-021 · L375](../../../02-PRD.md#us-003-maintain-movies) | Lost response / artwork outage \| A movie save loses confirmation or artwork handling is unavailable \| The Maintainer checks the outcome or edits only metadata \| Resolve uncertain saves before suggesting resubmission; metadata-only work remains usable independently of artwork availability. |
| [AC-021-C01 · L375](../../../02-PRD.md#us-003-maintain-movies) | Resolve uncertain saves before suggesting resubmission; metadata-only work remains usable independently of artwork availability. |
| [AC-024 · L389](../../../02-PRD.md#us-004-maintain-people-and-credits) | Permanent person deletion \| A person is archived and a Maintainer reviews the current affected active movies, archived movies, and TV titles \| They separately confirm permanent deletion \| Remove the person and all remaining retained credits while preserving titles; retain an attributable deletion record and provide no restore. Cancellation removes nothing. Restoring a title cannot recreate the person or credits. |
| [AC-024-C01 · L389](../../../02-PRD.md#us-004-maintain-people-and-credits) | Remove the person and all remaining retained credits while preserving titles; retain an attributable deletion record and provide no restore. |
| [AC-024-C02 · L389](../../../02-PRD.md#us-004-maintain-people-and-credits) | Cancellation removes nothing. |
| [AC-024-C03 · L389](../../../02-PRD.md#us-004-maintain-people-and-credits) | Restoring a title cannot recreate the person or credits. |
| [AC-137 · L401](../../../02-PRD.md#us-004-maintain-people-and-credits) | Competing, interrupted, or uncertain person lifecycle \| Archive, restore, or permanent deletion competes with a change, loses confirmation, or is interrupted \| The Maintainer reviews or repeats the action, including after a restart \| Preserve prior state on a confirmed failed change; retain committed state and audit evidence across restart. Require review of conflicts/current deletion impact, reconcile uncertain outcomes, and avoid repeated effects or unconfirmed success. Restoration never recreates a permanently deleted person. |
| [AC-137-C01 · L401](../../../02-PRD.md#us-004-maintain-people-and-credits) | Preserve prior state on a confirmed failed change; retain committed state and audit evidence across restart. |
| [AC-137-C02 · L401](../../../02-PRD.md#us-004-maintain-people-and-credits) | Require review of conflicts/current deletion impact, reconcile uncertain outcomes, and avoid repeated effects or unconfirmed success. |
| [AC-137-C03 · L401](../../../02-PRD.md#us-004-maintain-people-and-credits) | Restoration never recreates a permanently deleted person. |
| [AC-053 · L453](../../../02-PRD.md#us-009-review-accountable-history) | Confirmed change \| A permitted catalog change succeeds \| History is inspected \| A matching attributable successful-change record exists; a success claim cannot stand without its associated evidence. Technical atomicity remains a handoff commitment. |
| [AC-053-C01 · L453](../../../02-PRD.md#us-009-review-accountable-history) | A matching attributable successful-change record exists; a success claim cannot stand without its associated evidence. |
| [AC-053-C02 · L453](../../../02-PRD.md#us-009-review-accountable-history) | Technical atomicity remains a handoff commitment. |
| [AC-054 · L454](../../../02-PRD.md#us-009-review-accountable-history) | Failed change \| An attempted change fails before completion \| State/history are inspected \| No partial catalog change or misleading successful-change event appears; failure evidence is distinguishable from success. |
| [AC-054-C01 · L454](../../../02-PRD.md#us-009-review-accountable-history) | No partial catalog change or misleading successful-change event appears; failure evidence is distinguishable from success. |
| [AC-055 · L455](../../../02-PRD.md#us-009-review-accountable-history) | Security/media outcomes \| Authentication is denied or artwork lifecycle actions occur \| Authorized inspection occurs \| Appropriate sanitized outcomes remain attributable and available, even when the business action failed. Security events need not appear in the Maintainer catalog-history screen. |
| [AC-055-C01 · L455](../../../02-PRD.md#us-009-review-accountable-history) | Appropriate sanitized outcomes remain attributable and available, even when the business action failed. |
| [AC-055-C02 · L455](../../../02-PRD.md#us-009-review-accountable-history) | Security events need not appear in the Maintainer catalog-history screen. |
| [AC-056 · L456](../../../02-PRD.md#us-009-review-accountable-history) | Audit privacy/access \| Audit contains permitted change details \| Someone views it \| Only permitted readers see it; credentials, tokens, raw files, and account emails never appear. History is read-only. Retention/protection mechanics belong to the techdoc. |
| [AC-056-C01 · L456](../../../02-PRD.md#us-009-review-accountable-history) | Only permitted readers see it; credentials, tokens, raw files, and account emails never appear. |
| [AC-056-C02 · L456](../../../02-PRD.md#us-009-review-accountable-history) | History is read-only. |
| [AC-056-C03 · L456](../../../02-PRD.md#us-009-review-accountable-history) | Retention/protection mechanics belong to the techdoc. |
| [AC-114 · L457](../../../02-PRD.md#us-009-review-accountable-history) | Audit screen \| A Maintainer has catalog history to inspect \| They open history and an entry \| Show actor, action, affected record, time, outcome, and relevant changes, explicitly covering movie/person archive, restoration, permanent deletion, credits, artwork, and AI approval/creation. Approval is distinguishable from publication. |
| [AC-114-C01 · L457](../../../02-PRD.md#us-009-review-accountable-history) | Show actor, action, affected record, time, outcome, and relevant changes, explicitly covering movie/person archive, restoration, permanent deletion, credits, artwork, and AI approval/creation. |
| [AC-114-C02 · L457](../../../02-PRD.md#us-009-review-accountable-history) | Approval is distinguishable from publication. |
| [AC-115 · L458](../../../02-PRD.md#us-009-review-accountable-history) | Audit feedback/denial \| History is empty, loading, unavailable, or requested without permission \| The screen/request resolves \| Show the appropriate loading/empty/failure/denied state; permit safe retry for failure, and provide no editing or arbitrary restore control. |
| [AC-115-C01 · L458](../../../02-PRD.md#us-009-review-accountable-history) | Show the appropriate loading/empty/failure/denied state; permit safe retry for failure, and provide no editing or arbitrary restore control. |
| [AC-127 · L459](../../../02-PRD.md#us-009-review-accountable-history) | Actor attribution without account exposure \| An audit event identifies its responsible actor \| A Maintainer views it \| Show the actor identity needed for accountability without exposing email, credentials, unrelated account details, or a public profile link. Public/catalog/MCP outputs do not reveal private account information. |
| [AC-127-C01 · L459](../../../02-PRD.md#us-009-review-accountable-history) | Show the actor identity needed for accountability without exposing email, credentials, unrelated account details, or a public profile link. |
| [AC-127-C02 · L459](../../../02-PRD.md#us-009-review-accountable-history) | Public/catalog/MCP outputs do not reveal private account information. |
| [AC-104 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Stale save \| Two Maintainers edit the same movie/person \| One saves before the other \| Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-104-C01 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-105 · L494](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Repeated submission \| The same intended change is submitted twice or after lost confirmation \| The outcome is resolved \| Show one completed effect or its pending outcome without unintended duplicates; button disabling alone is not the promised behavior. |
| [AC-105-C01 · L494](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Show one completed effect or its pending outcome without unintended duplicates; button disabling alone is not the promised behavior. |
| [AC-069 · L861](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify JPA/Hibernate mappings, transactions, rollback, audit, and required-work consistency \| NFR-003; technical design |
| [AC-070 · L862](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify applicable native SQL parameterization, joins/order/pagination, mapping, plans, and write safeguards against the chosen engine \| NFR-003; technical design |
| [AC-087 · L872](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise GraphQL contracts, real gRPC, selected database, security/audit, native SQL where used, seed, artwork, recovery, and notification integration \| NFR-008; technical design |
| [AC-100 · L878](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Define/verify GraphQL validation against section 5.5, authorization, omission versus clearing, and allowed partial results without partial invalid saves \| NFR-013/FR-017/NFR-016; technical design |
| [AC-106 · L882](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Define/verify operation identity scope, incompatible reuse, in-progress/expiry/retention behavior, and safe replay \| NFR-014; technical design |
| [G-02 · L925](../../../02-PRD.md#122-release-gates-remain-planned) | Mandated stack/protocols and chosen integration approach work \| NFR-001/NFR-002/NFR-013; AC-064 through AC-067, AC-100 through AC-103 \| Planned in techdoc |
| [G-03 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [AC-057 · L973](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | Required follow-up \| A confirmed change requires later work \| The application stops before follow-up completes \| The obligation remains visible and recoverable; an uncommitted change does not leave a false successful follow-up obligation. |
| [AC-057-C01 · L973](../../../02-PRD.md#us-010-recover-follow-up-and-notify-failures) | The obligation remains visible and recoverable; an uncommitted change does not leave a false successful follow-up obligation. |
| [OD-004 · L1022](../../../02-PRD.md#131-resolved-product-decisions) | Movies and people require archiving before separately confirmed permanent deletion. Person archiving retains credits privately; restoration reveals only surviving credits on active titles. Deletion preserves associated titles and movie deletion preserves shared people. \| FR-004/FR-005/FR-006; AC-019/AC-024/AC-108 through AC-110, AC-129 through AC-138 |
| [OD-009 · L1024](../../../02-PRD.md#131-resolved-product-decisions) | Reject stale writes, retain draft, require review; no force overwrite \| NFR-014; AC-020/AC-025/AC-104; concurrency mechanism transfers to techdoc |
| [OD-010 · L1049](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | No unintended duplicate effects or text-only identity merges; replay identity, mismatch/expiry, claims, retention, and uncertain-outcome mechanics remain for techdoc \| NFR-014, FR-016 \| Technical design; before retryable writes |
| [OD-011 · L1050](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Audit screen/access/content fixed, including movie/person lifecycle; event schema, retention, retrieval, integrity/protection, and technical security-event inspection remain for techdoc \| FR-013 \| Technical design; before audit implementation |
| [US-015 · L477](../../../02-PRD.md#us-015-complete-tasks-across-devices) | ### US-015: Complete tasks across devices As a Visitor, Viewer, or Maintainer, I want readable controls and accurate feedback on desktop and mobile. Requirement: NFR-007. Apply these criteria to registration, archive, audit, and catalog flows. |
| [PRD-PERM-06 · L75](../../../02-PRD.md#31-permission-matrix) | View application audit history \| No \| No \| Yes, read-only |
| [PRD-PERM-07 · L76](../../../02-PRD.md#31-permission-matrix) | Approve an AI-proposed movie \| No \| No \| Yes, explicitly in chat |
| [PRD-BOUND-03 · L181](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Movie display title / optional original title \| 1 character \| 255 characters \| Display title is required; original title may remain unknown |
| [PRD-BOUND-04 · L182](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Movie synopsis \| 1 character \| 5,000 characters \| Required; length alone does not establish descriptive quality |
| [PRD-POLICY-24 · L198](../../../02-PRD.md#validation-safe-handling-and-feedback) | Show effective limits in field guidance before submission and identify the affected field and correction when validation fails. Preserve permitted non-secret draft content, including an over-limit value so the user can correct it. Passwords never enter recovery drafts, logs, or audit. Invalid catalog or account submission commits no partial record change. Invalid search retains its entered text and other filters with correction feedback; do not run a truncated or silently altered search. |
| [PRD-POLICY-26 · L202](../../../02-PRD.md#validation-safe-handling-and-feedback) | Catalog text is plain text. Entered markup or instructions must not execute in forms, catalog pages, search results, previews, or audit views. Preserve legitimate names and punctuation during safe handling. OWASP input-validation guidance separates input validation from defenses against injection and unsafe output. Sanitization is not a promise that every vulnerability has been prevented; the techdoc must establish the required safe display and data-handling mechanisms. |
| [PRD-CONCEPT-04 · L814](../../../02-PRD.md#9-conceptual-information-and-privacy) | Release context \| Original release for a movie; first-air for a series. Preserve year, year/month, or full date and supplied regional/source context. No invented January 1 or day-of-month. |
| [PRD-CONCEPT-07 · L817](../../../02-PRD.md#9-conceptual-information-and-privacy) | Movie lifecycle \| Active, archived, or permanently deleted. Archive preserves remaining content; restore respects person state and removed credits. Permanent deletion offers no product restoration. Audit evidence follows the eventual retention policy. |
| [PRD-HANDOFF-05 · L840](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D05/D20 \| Committed \| Successful database change and audit share a local transaction; required cross-service follow-up uses an outbox or equivalent durable work. Concurrent delivery/restart/uncertain outcomes must be safe; saga coordination only if needed. |
| [TD-ROW-L2734 · L2734](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `managedTitle` \| Maintainer / Catalog \| Private active/archived surviving movie or TV projection; TV remains read-only |
| [TD-ROW-L2737 · L2737](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `managedTitles` \| Maintainer / Catalog \| Explicit ACTIVE/ARCHIVED state; stable page order |
| [TD-ROW-L2746 · L2746](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `createMovie` \| Maintainer / Catalog \| Current policy, required metadata/genres/partial date; duplicate name allowed, source identity separate |
| [TD-ROW-L2747 · L2747](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `updateMovie` \| Maintainer / Catalog \| Active MOVIE, expected movie version, changed-field mask/current policy; no TV edits |
| [BRAIN-A04 · L51](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A04 \| Service-to-service communication via gRPC \| Real RPC calls supporting application features; protobuf contracts and integration checks |
| [BRAIN-A05 · L52](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A05 \| Frontend-to-backend communication via GraphQL \| Queries/mutations implement application operations; file/auth transport details documented |
| [BRAIN-A07 · L54](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A07 \| Add, delete, and update movies \| Working operations with validation and understandable outcomes |
| [BRAIN-A10 · L57](../../../01-brainstorming-result.md#3-assessment-requirements-and-acceptance-baseline) | A10 \| Add, delete, and update cast and creators \| Working person management with defined relationship behavior |
| [BRAIN-D03 · L74](../../../01-brainstorming-result.md#4-decision-register) | D03 \| Required \| GraphQL for frontend application operations; gRPC between backend services |
| [BRAIN-D05 · L76](../../../01-brainstorming-result.md#4-decision-register) | D05 \| Committed \| Design for authentication, authorization, RBAC, and audit logging from the start; implement core functionality first, then complete security |
| [BRAIN-D08 · L79](../../../01-brainstorming-result.md#4-decision-register) | D08 \| Recommended \| Use queries/mutations without GraphQL subscriptions for the initial release |
| [BRAIN-D09 · L80](../../../01-brainstorming-result.md#4-decision-register) | D09 \| Recommended \| Protect stale updates with optimistic concurrency; distinguish duplicate-request handling from duplicate-data rules |
| [BRAIN-D18 · L89](../../../01-brainstorming-result.md#4-decision-register) | D18 \| Committed \| Spring Data JPA/Hibernate for ordinary persistence; parameterized native SQL for complex joins when it improves clarity or query control |
| [BRAIN-D19 · L90](../../../01-brainstorming-result.md#4-decision-register) | D19 \| Committed \| Unit and integration tests, including real gRPC communication and persistence against the selected database engine |
| [BRAIN-D20 · L91](../../../01-brainstorming-result.md#4-decision-register) | D20 \| Committed \| Durable outbox or equivalent work table for reliable cross-service follow-up; saga coordination only when the finalized workflow needs it |

## Implementation

1. Implement createMovie/updateMovie/managed projections with Catalog authorization, current policy, required title/synopsis/genre/year and optional unknowns; direct TV writes remain denied.
2. Use explicit patches and expected version, Catalog write lock/row ordering and JPA version/native equivalent. Do not merge by title text, silently fill unknown data or force an overwrite.
3. Create immutable business audit storage and insertion helper with all AuditOutcome and subject fields, safe actor reference and bounded differences. Commit movie/genres/provenance/receipt/audit/required work together.
4. Expose typed GraphQL/gRPC result mappings and original owner receipt reconciliation. Keep metadata-only writes independent of Media availability.
5. Test rollback at each transaction boundary, same-key concurrency, stale separate edits, missing/archived target and equivalent native-write safeguards against actual PostgreSQL.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T017-A1:** Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none.
- **T017-A2:** Edit: unchanged fields survive, explicit optional clear works, stale expected version fails without lost data, and same-name independent works remain distinct.
- **T017-A3:** Failure: response loss resolves original outcome, rollback leaves no success audit/work, and metadata save succeeds during unrelated Media outage.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-147](../../../05-atdd.md#case-atdd-147-roll-back-the-complete-local-mutation-boundary) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.1/7.7 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then each precommit fault rolls back domain change, successful audit, receipt and required work together And committed success retains all four across restart And denied/security failure evidence is recorded separately without inventing successful business history or holding a transaction over remote I/O Observation: Place barriers after domain write, audit insert and work insert inside actual owner transactions. Roll back each fault, then reread all involved tables from a separate connection and verify no successful mutation/audit/work/receipt escaped; verify the success row commits them together. |
| [ATDD-148](../../../05-atdd.md#case-atdd-148-retain-version-and-audit-safeguards-in-native-writes) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.1/7.5 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then affected-row checks reject stale writes, versions advance exactly once and refreshed ORM state cannot overwrite native changes And 23505/23503/23514 return their safe domain conditions And 40001/40P01 retry only the whole transaction at most once under the original ID/deadline before known commit Observation: Observe native prepared parameters, returned affected-row count, version increments and fresh JPA reads. Cause each specified SQLSTATE and inspect whole-transaction rollback/retry count within the original deadline/UUID; a retry of only the final statement must fail this case. |
| [ATDD-151](../../../05-atdd.md#case-atdd-151-distinguish-repeated-intent-from-similar-data) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.3 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then compatible repeats resolve one logical effect or its pending result And different actor cannot inspect the receipt and changed content/kind/version/policy/clearing/ordered characters conflicts And sorted genre-set equivalence follows deterministic set identity while independent same-name creates remain distinct Observation: Submit the same UUID with matching and changed actor/kind/mask/array/version/policy payloads, including concurrent requests and lost replies. Inspect original result ID and one committed logical effect; distinct UUIDs with identical title text remain separate records. Use F-DETERMINISTIC-CONTENT equivalence/non-equivalence vectors. |
| [ATDD-298](../../../05-atdd.md#case-atdd-298-summarize-oversized-audit-diffs-without-losing-domain-values) / Unit | Public before/after diff around65536 bytes with multibyte text; selected changed paths; synthetic private field canary. | Then within-bound allowlisted changes preserve their values And oversize changes retain paths, exact byte lengths, digests and valueOmitted=true And forbidden private fields never appear; source domain inputs remain unchanged Observation: Compare independently measured UTF-8 byte counts and digest vectors; actual jsonb::text bound, transaction and displayed omission require integration. |

Partial implementation contributions: [ATDD-016](../../../05-atdd.md#case-atdd-016-create), [ATDD-017](../../../05-atdd.md#case-atdd-017-update), [ATDD-018](../../../05-atdd.md#case-atdd-018-validation--denied-write), [ATDD-021](../../../05-atdd.md#case-atdd-021-lost-response--artwork-outage), [ATDD-080](../../../05-atdd.md#case-atdd-080-repeated-submission), [ATDD-053](../../../05-atdd.md#case-atdd-053-confirmed-change), [ATDD-054](../../../05-atdd.md#case-atdd-054-failed-change), [ATDD-055](../../../05-atdd.md#case-atdd-055-securitymedia-outcomes), [ATDD-056](../../../05-atdd.md#case-atdd-056-audit-privacyaccess), [ATDD-088](../../../05-atdd.md#case-atdd-088-audit-screen), [ATDD-089](../../../05-atdd.md#case-atdd-089-audit-feedbackdenial), [ATDD-101](../../../05-atdd.md#case-atdd-101-actor-attribution-without-account-exposure), [ATDD-185](../../../05-atdd.md#case-atdd-185-record-large-changes-without-inventing-their-contents), [ATDD-224](../../../05-atdd.md#case-atdd-224-enforce-catalog-business-audit-storage-and-private-event-separation), [ATDD-265](../../../05-atdd.md#case-atdd-265-inspect-audit-state-transitions-and-feedback), [ATDD-307](../../../05-atdd.md#case-atdd-307-expose-audit-outcome-and-affected-record-through-corrected-projections). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T017` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T017` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- Hand the owned outputs to [T018 — Connect movie editors and confirmed outcomes](T018-connect-movie-editors-and-confirmed-outcomes.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
