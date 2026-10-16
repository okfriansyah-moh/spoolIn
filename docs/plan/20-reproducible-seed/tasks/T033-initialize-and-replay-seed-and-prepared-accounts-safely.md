---
kind: task
id: T033
group: G20
depends_on:
- T008
- T007
- T012
- T016
- T018
- T020
- T022
- T023
- T032
- T024
- T010
- T013
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T033 — Initialize and replay seed and prepared accounts safely

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-029 | verifies |
| ATDD-112 | both |
| ATDD-033 | both |
| ATDD-034 | both |
| ATDD-035 | both |
| ATDD-036 | both |
| ATDD-037 | both |
| ATDD-049 | both |
| ATDD-132 | both |
| ATDD-196 | verifies |
| ATDD-197 | both |
| ATDD-198 | implements |
| ATDD-199 | both |
| ATDD-235 | implements |
| ATDD-235-T1-R01 | implements |
| ATDD-235-T1-R02 | implements |
| ATDD-235-T1-R03 | implements |
| ATDD-235-T1-R04 | implements |
| ATDD-243 | implements |
| ATDD-243-T1-R01 | implements |
| ATDD-243-T1-R02 | implements |
| ATDD-243-T1-R03 | implements |
| ATDD-243-T1-R04 | implements |
| ATDD-243-T1-R05 | implements |
| ATDD-244 | implements |
| ATDD-244-T1-R01 | implements |
| ATDD-244-T1-R02 | implements |
| ATDD-244-T1-R03 | implements |
| ATDD-244-T1-R04 | implements |
| ATDD-244-T1-R05 | implements |
| ATDD-244-T1-R06 | implements |
| ATDD-244-T1-R07 | implements |
| SC-02 | both |
| FR-005 | both |
| FR-007 | both |
| FR-008 | both |
| FR-011 | both |
| FR-017 | both |
| NFR-008 | both |
| NFR-016 | both |
| A04 | both |
| D05 | both |
| D07 | both |
| D12 | both |
| D19 | both |
| D24 | both |
| D25 | both |
| D26 | both |
| AC-138 | both |
| AC-138-C01 | both |
| AC-138-C02 | both |
| AC-029 | both |
| AC-029-C01 | both |
| AC-049 | both |
| AC-049-C01 | both |
| AC-111 | both |
| AC-111-C01 | both |
| AC-158 | both |
| AC-158-C01 | both |
| AC-158-C02 | both |
| AC-158-C03 | both |
| AC-087 | both |
| G-03 | both |
| AC-033 | both |
| AC-033-C01 | both |
| AC-034 | both |
| AC-034-C01 | both |
| AC-034-C02 | both |
| AC-035 | both |
| AC-035-C01 | both |
| AC-035-C02 | both |
| AC-036 | both |
| AC-036-C01 | both |
| AC-037 | both |
| AC-037-C01 | both |
| OD-002 | both |
| OD-004 | both |
| OD-018 | both |
| OD-006 | both |
| US-006 | both |
| PRD-POLICY-01 | both |
| PRD-POLICY-09 | both |
| PRD-POLICY-11 | both |
| PRD-POLICY-19 | both |
| PRD-CONCEPT-02 | both |
| PRD-CONCEPT-05 | both |
| PRD-CONCEPT-12 | both |
| PRD-HANDOFF-09 | both |
| DS-POLICY-11 | implements |
| TD-C-030 | both |
| TD-C-055 | both |
| TECH-G-04 | both |
| TD-U-006 | both |
| ADR-08 | both |
| V-SEED | both |
| TD-ROW-L3558 | both |
| TD-ROW-L3569 | both |
| TD-ROW-L3586 | both |
| TD-ROW-L3599 | both |
| TD-ROW-L3626 | both |
| TD-ROW-L3954 | both |
| TD-ROW-L3955 | both |
| TD-ROW-L3956 | both |
| TD-ROW-L3957 | both |
| TD-ROW-L3992 | both |
| TD-ROW-L3993 | both |
| TD-ROW-L3994 | both |
| TD-ROW-L3995 | both |
| TD-ROW-L3996 | both |
| TD-ROW-L5187 | both |
| TD-ROW-L5513 | both |
| TD-ROW-L5533 | both |
| TD-BLOCK-L3964 | both |
| TD-BLOCK-L4003 | both |
| TD-BLOCK-L4782 | both |
| ATDD-G-04 | both |
| BRAIN-A04 | both |
| BRAIN-D05 | both |
| BRAIN-D07 | both |
| BRAIN-D12 | both |
| BRAIN-D19 | both |
| BRAIN-D24 | both |
| BRAIN-D25 | both |
| BRAIN-D26 | both |

## Prerequisites

- [T008 — Implement registration and authoritative sessions](../../06-account-access-registration-and-sessions/tasks/T008-implement-registration-and-authoritative-sessions.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.
- [T007 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T012 — Serve precise public catalog and people reads](../../10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year.
- [T016 — Implement authoritative movie creation and edits](../../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none.
- [T018 — Implement shared person writes and identity](../../13-people-and-credits/tasks/T018-implement-shared-person-writes-and-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact.
- [T020 — Implement ordered credits and custom contribution roles](../../13-people-and-credits/tasks/T020-implement-ordered-credits-and-custom-contribution-roles.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit.
- [T022 — Accept and process bounded artwork streams](../../14-artwork-management-processing-and-attachment/tasks/T022-accept-and-process-bounded-artwork-streams.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing.
- [T023 — Coordinate protective holds, attachment and cleanup](../../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.
- [T032 — Assemble the permitted frozen 100-movie and 100-series dataset](T032-assemble-the-permitted-frozen-100-movie-and-100-series-dataset.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Dataset: frozen accepted manifest has exactly100/100 unique source identities and complete required values with usable provenance/reuse evidence.
- [T024 — Implement current-impact archive, restore and deletion](../../15-archive-management/tasks/T024-implement-current-impact-archive-restore-and-deletion.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing.
- [T010 — Connect account and session browser journeys](../../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T013 — Connect discovery, details and public people views](../../10-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/seed/ | T033 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/bootstrap/ | T033 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/resources/db/migration/V007__initial_account_receipts.sql | T033 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | config/accounts.example.yaml | T033 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/seed.sh | T033 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/seed/ | T033 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/seed/ | T033 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/bootstrap/ | T033 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [6.2.3 Initialization receipt extensions](../../../04-spoolin-techdoc.md#623-initialization-receipt-extensions): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.3.2 Attempt events and initialization replay](../../../04-spoolin-techdoc.md#732-attempt-events-and-initialization-replay): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [10.1 Frozen seed contract](../../../04-spoolin-techdoc.md#101-frozen-seed-contract): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [11.2 Startup DAG and readiness](../../../04-spoolin-techdoc.md#112-startup-dag-and-readiness): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-235-T1-R01 · L9492](../../../05-atdd.md#case-atdd-235-preserve-permanent-core-initialization-receipt-evidence) | `setup_version` (`varchar(32)`); Source-valid technical token; 32 versus 33 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `UNIQUE; required only on ACCOUNT_INITIALIZATION; no default` |
| [ATDD-235-T1-R02 · L9493](../../../05-atdd.md#case-atdd-235-preserve-permanent-core-initialization-receipt-evidence) | `setup_manifest_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `Exactly 32 bytes; required only on ACCOUNT_INITIALIZATION` |
| [ATDD-235-T1-R03 · L9494](../../../05-atdd.md#case-atdd-235-preserve-permanent-core-initialization-receipt-evidence) | `setup_viewer_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `FK user_account(id), DELETE RESTRICT; required on successful setup` |
| [ATDD-235-T1-R04 · L9495](../../../05-atdd.md#case-atdd-235-preserve-permanent-core-initialization-receipt-evidence) | `setup_maintainer_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `FK user_account(id), DELETE RESTRICT; required on successful setup` |
| [ATDD-243-T1-R01 · L9917](../../../05-atdd.md#case-atdd-243-preserve-permanent-catalog-initialization-and-seed-membership-evidence) | `seed_version` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `UNIQUE; required only on CATALOG_INITIALIZATION; no default` |
| [ATDD-243-T1-R02 · L9918](../../../05-atdd.md#case-atdd-243-preserve-permanent-catalog-initialization-and-seed-membership-evidence) | `seed_snapshot_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `Exactly 32 bytes; required only on seed initialization` |
| [ATDD-243-T1-R03 · L9919](../../../05-atdd.md#case-atdd-243-preserve-permanent-catalog-initialization-and-seed-membership-evidence) | `seed_movie_count` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 100, 101; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `0..100; successful initialization requires 100` |
| [ATDD-243-T1-R04 · L9920](../../../05-atdd.md#case-atdd-243-preserve-permanent-catalog-initialization-and-seed-membership-evidence) | `seed_tv_count` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 100, 101; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `0..100; successful initialization requires 100` |
| [ATDD-243-T1-R05 · L9921](../../../05-atdd.md#case-atdd-243-preserve-permanent-catalog-initialization-and-seed-membership-evidence) | `seed_summary` (`jsonb`); Independent source-valid object; wrong container tests actual shape CHECK; measure PostgreSQL jsonb::text bytes at 16384 and ±1; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `Object <=16384 bytes; required for seed initialization` |
| [ATDD-244-T1-R01 · L9967](../../../05-atdd.md#case-atdd-244-enforce-lifetime-seed-membership-and-removal-preservation) | `seed_id` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES operation_receipt(seed_version) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-244-T1-R02 · L9968](../../../05-atdd.md#case-atdd-244-enforce-lifetime-seed-membership-and-removal-preservation) | `member_key` (`varchar(255)`); Source-valid technical token; 255 versus 256 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-244-T1-R03 · L9969](../../../05-atdd.md#case-atdd-244-enforce-lifetime-seed-membership-and-removal-preservation) | `subject_kind` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (subject_kind IN ('TITLE','PERSON','GENRE','ROLE','CREDIT')); no SQL default; owner supplies value` |
| [ATDD-244-T1-R04 · L9970](../../../05-atdd.md#case-atdd-244-enforce-lifetime-seed-membership-and-removal-preservation) | `subject_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-244-T1-R05 · L9971](../../../05-atdd.md#case-atdd-244-enforce-lifetime-seed-membership-and-removal-preservation) | `content_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(content_hash)=32); no SQL default; owner supplies value` |
| [ATDD-244-T1-R06 · L9972](../../../05-atdd.md#case-atdd-244-enforce-lifetime-seed-membership-and-removal-preservation) | `applied_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-244-T1-R07 · L9973](../../../05-atdd.md#case-atdd-244-enforce-lifetime-seed-membership-and-removal-preservation) | `removed_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [SC-02 · L101](../../../02-PRD.md#4-goals-and-success-criteria) | Provide a useful starting catalog \| Fresh initial dataset contains exactly 100 distinct movies and 100 distinct TV series with usable metadata, provenance, and multiple years/known months per type \| Unverified; Committed |
| [FR-005 · L239](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-007 · L241](../../../02-PRD.md#61-functional-requirements) | Preserve usable metadata, explicit unknowns, identity, date precision, and provenance \| D25/D26 Committed \| AC-029 through AC-032, AC-143, AC-145 |
| [FR-008 · L242](../../../02-PRD.md#61-functional-requirements) | Supply the reproducible 100-movie/100-series initial catalog; preserve later movie/person edits, archive states, permanent deletions, and removed credits on reruns \| D24 Committed; rerun policy Committed (section 13.1) \| AC-033 through AC-037, AC-138, AC-158 |
| [FR-011 · L245](../../../02-PRD.md#61-functional-requirements) | Register immediate Viewer accounts, provide demo accounts, and support sign-in/sign-out \| D05 Committed; approved account scope Committed \| AC-047 through AC-049, AC-111, AC-112, AC-141/AC-142, AC-145/AC-146 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-016 · L274](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [A04 · L283](../../../02-PRD.md#63-project-requirements-coverage) | Real service-to-service gRPC \| NFR-001/NFR-008 / AC-065, AC-087 |
| [D05 · L306](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D07 · L308](../../../02-PRD.md#64-project-decision-register) | Viewer/Maintainer and prepared accounts \| Viewer/Maintainer roles and prepared accounts are adopted as Committed policy in section 13.1; FR-011/FR-012 / AC-049, AC-050, AC-111 |
| [D12 · L313](../../../02-PRD.md#64-project-decision-register) | Authentication mechanism and verified internal identity \| Product access settled; mechanics Open in techdoc. FR-011/FR-012 / AC-047 through AC-052 |
| [D19 · L320](../../../02-PRD.md#64-project-decision-register) | Unit/integration, real gRPC and selected database checks \| NFR-008 / AC-086 through AC-088 |
| [D24 · L325](../../../02-PRD.md#64-project-decision-register) | 100 distinct movies + 100 distinct TV series, variety, reproducible safe reruns \| FR-008 / AC-033 through AC-037 |
| [D25 · L326](../../../02-PRD.md#64-project-decision-register) | Metadata, type/genres, stable identities, provenance \| FR-006/FR-007 / AC-026 through AC-032 |
| [D26 · L327](../../../02-PRD.md#64-project-decision-register) | Composable year/month filters with source date precision \| FR-003/FR-007 / AC-011 through AC-015, AC-029, AC-032 |
| [AC-138 · L400](../../../02-PRD.md#us-004-maintain-people-and-credits) | Seed rerun after person changes \| Seeded people were edited, archived, or permanently deleted and their credits changed or were removed \| Ordinary seeding repeats or resumes after interruption \| Preserve edits, archive state, permanent deletion, and removed credits; do not reactivate/recreate people or reinstate relationships. Later person/movie restoration still obeys those decisions, with no duplicate people or credits. |
| [AC-138-C01 · L400](../../../02-PRD.md#us-004-maintain-people-and-credits) | Preserve edits, archive state, permanent deletion, and removed credits; do not reactivate/recreate people or reinstate relationships. |
| [AC-138-C02 · L400](../../../02-PRD.md#us-004-maintain-people-and-credits) | Later person/movie restoration still obeys those decisions, with no duplicate people or credits. |
| [AC-029 · L408](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Seed metadata \| A seeded title is opened \| The reader inspects its details \| Show usable title/type, synopsis, genres, release/first-air context, known optional metadata, and source attribution; preserve synopsis language. |
| [AC-029-C01 · L408](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Show usable title/type, synopsis, genres, release/first-air context, known optional metadata, and source attribution; preserve synopsis language. |
| [AC-049 · L437](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Demo access \| A release reviewer follows documented account setup \| They sign in with prepared Viewer/Maintainer access \| Each account demonstrates its assigned capabilities; no unrestricted development bypass substitutes for role enforcement. |
| [AC-049-C01 · L437](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Each account demonstrates its assigned capabilities; no unrestricted development bypass substitutes for role enforcement. |
| [AC-111 · L441](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Immediate registration \| A visitor supplies valid name, email, and password for a new account \| Registration succeeds \| Create a Viewer eligible to sign in immediately, without email verification; do not allow registration as Maintainer. |
| [AC-111-C01 · L441](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Create a Viewer eligible to sign in immediately, without email verification; do not allow registration as Maintainer. |
| [AC-158 · L548](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Seed compatibility with limits \| A fresh snapshot is evaluated against the adopted policy, or a rerun encounters retained data accepted under earlier limits \| Initial seeding or an ordinary rerun is verified \| Select source-permitted records meeting required seed data and adopted bounds without silently shortening names/metadata or fabricating values. Report incompatible required input rather than claiming a completed 100/100 dataset. Ordinary reruns preserve edits, archives, deletions, and removed credits under AC-035/AC-138 and do not rewrite retained data merely to satisfy tighter limits. |
| [AC-158-C01 · L548](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Select source-permitted records meeting required seed data and adopted bounds without silently shortening names/metadata or fabricating values. |
| [AC-158-C02 · L548](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Report incompatible required input rather than claiming a completed 100/100 dataset. |
| [AC-158-C03 · L548](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Ordinary reruns preserve edits, archives, deletions, and removed credits under AC-035/AC-138 and do not rewrite retained data merely to satisfy tighter limits. |
| [AC-087 · L870](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise GraphQL contracts, real gRPC, selected database, security/audit, native SQL where used, seed, artwork, recovery, and notification integration \| NFR-008; technical design |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [AC-033 · L959](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Fresh initial seed \| The application has an empty initialized catalog \| The documented initial seed completes \| Exactly 100 distinct movies and 100 distinct TV series with required seed metadata exist; shared people/genres/credits are not duplicated. |
| [AC-033-C01 · L959](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Exactly 100 distinct movies and 100 distinct TV series with required seed metadata exist; shared people/genres/credits are not duplicated. |
| [AC-034 · L960](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Variety \| The initial dataset and manifest exist \| Counts and release context are inspected \| Each type spans multiple years, older/newer titles, and multiple known months; documented ranges/counts reconcile with the dataset. Equal quotas and every-month coverage are unnecessary. |
| [AC-034-C01 · L960](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Each type spans multiple years, older/newer titles, and multiple known months; documented ranges/counts reconcile with the dataset. |
| [AC-034-C02 · L960](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Equal quotas and every-month coverage are unnecessary. |
| [AC-035 · L961](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Preserve user decisions \| Seeded movies or people were edited, archived, or permanently deleted, credits were removed, and user records were added \| Ordinary seeding runs again \| Preserve those decisions and user-created records; do not recreate deleted movies/people, reactivate archived records, reinstate removed credits, or duplicate titles/people/genres/credits. The original 100/100 target is not a later catalog cap or reset instruction. |
| [AC-035-C01 · L961](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Preserve those decisions and user-created records; do not recreate deleted movies/people, reactivate archived records, reinstate removed credits, or duplicate titles/people/genres/credits. |
| [AC-035-C02 · L961](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | The original 100/100 target is not a later catalog cap or reset instruction. |
| [AC-036 · L962](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Interrupted/concurrent seed \| Seed execution is interrupted, repeated, or overlaps \| It resumes or completes \| Report a truthful partial/failure/completed outcome without duplicate records or lost user decisions; a changed snapshot is not silently treated as the same input. |
| [AC-036-C01 · L962](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Report a truthful partial/failure/completed outcome without duplicate records or lost user decisions; a changed snapshot is not silently treated as the same input. |
| [AC-037 · L963](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Repeatable/invalid input \| Two fresh setups use the same snapshot, or required seed data are invalid \| Seeding runs \| Valid setups produce equivalent identities/metadata/counts; invalid records cannot be silently omitted while reporting a successful 100/100 dataset. |
| [AC-037-C01 · L963](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Valid setups produce equivalent identities/metadata/counts; invalid records cannot be silently omitted while reporting a successful 100/100 dataset. |
| [OD-002 · L1019](../../../02-PRD.md#131-resolved-product-decisions) | Public discovery; Viewer and Maintainer; same permissions for all Maintainers; no role-management UI \| FR-011/FR-012; AC-049/AC-050/AC-113 |
| [OD-004 · L1020](../../../02-PRD.md#131-resolved-product-decisions) | Movies and people require archiving before separately confirmed permanent deletion. Person archiving retains credits privately; restoration reveals only surviving credits on active titles. Deletion preserves associated titles and movie deletion preserves shared people. \| FR-004/FR-005/FR-006; AC-019/AC-024/AC-108 through AC-110, AC-129 through AC-138 |
| [OD-018 · L1023](../../../02-PRD.md#131-resolved-product-decisions) | Seeded TV read/search/filter/metadata only; no direct TV management or episode tracking \| FR-001/FR-007/FR-008; AC-001/AC-029/AC-032/AC-033 |
| [OD-006 · L1044](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Counts/metadata/precision and non-destructive movie/person reruns fixed; provider/reuse terms, exact records/range, snapshot/manifest, invocation, merge/provenance and restart strategy remain for techdoc \| FR-007/FR-008 \| Technical design; before seed implementation |
| [US-006 · L953](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | #### US-006: Start with a reproducible catalog As a release reviewer or operations owner, I want a consistent starting dataset that respects later user decisions. Requirements: FR-008, FR-007, NFR-010. Seed access and invocation are engineering design matters; the observable results are fixed. |
| [PRD-POLICY-01 · L114](../../../02-PRD.md#51-included-behavior) | Movies support listing, details, search, creation, editing, artwork management, archive, restoration, and permanent deletion. TV-series entries support seeded metadata, listing, details, search, and filters. Maintainers cannot directly create, edit, archive, delete, or manage artwork/credits on TV entries in this release. Shared-person corrections, archiving, restoration, or permanent deletion can still affect the people and credits shown on seeded TV titles. Person lifecycle impact previews include those titles; this does not enable direct TV management. |
| [PRD-POLICY-09 · L132](../../../02-PRD.md#52-removal-and-editing-rules) | Permanent person deletion is available only from the people archive. It requires a current preview of all affected titles and a separate irreversible-deletion confirmation. Confirmation deletes the person and their remaining credits, including retained credits on archived movies and seeded TV titles, while preserving the titles. Cancel leaves everything unchanged. If the previewed impact changes, review and confirm the current impact before deletion. A permanently deleted person cannot be restored. |
| [PRD-POLICY-11 · L136](../../../02-PRD.md#52-removal-and-editing-rules) | Ordinary seed reruns preserve movie/person edits, their archive states, permanent deletions, and explicitly removed credits. They must not reactivate archived records, recreate deleted people, or reinstate removed relationships. |
| [PRD-POLICY-19 · L171](../../../02-PRD.md#name-defaults) | The LGA workforce data standard, page 20 provides a documented 100-character full-name precedent. BIGHIT's official profile includes the professional name V. These examples support the policy; they do not establish a universal naming standard or prove coverage of every name. Actual user and seed-name distributions have not been measured. GOV.UK naming guidance recommends checking available population or user data when selecting field lengths. Technical design must review the selected seed names and record any conflict without abbreviating or replacing a person's name silently. |
| [PRD-CONCEPT-02 · L810](../../../02-PRD.md#9-conceptual-information-and-privacy) | Seed metadata \| Usable title, synopsis with its language, one or more consistent genres, release/first-air context, stable identity, and provenance. Include supported cast/creator credits without inventing missing people. |
| [PRD-CONCEPT-05 · L813](../../../02-PRD.md#9-conceptual-information-and-privacy) | Genre \| Shared category independent of movie/TV type. One title can belong to several genres; spelling variants must not create duplicate categories on reseed. |
| [PRD-CONCEPT-12 · L820](../../../02-PRD.md#9-conceptual-information-and-privacy) | Provenance \| Metadata source, source record ID or URL, retrieval/snapshot date, and attribution/reuse notes. A user's later correction must not silently masquerade as an unchanged source value; the techdoc defines how to retain that distinction. |
| [PRD-HANDOFF-09 · L842](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D24 through D26 \| Committed \| Versioned source-permitted seed/snapshot and manifest, exactly 100/100 initially, source precision, identity/provenance, and reruns that preserve movie/person edits, archive states, permanent deletions, and removed credits. |
| [DS-POLICY-11 · L82](../../../03-design-system.md#artwork-icons-and-motion) | The four local raster assets were generated for fictional films and TV fixtures using imagegen; provenance and prompts record their origin. They do not claim rights to existing films, provider seed data, or recognizable performers. User artwork remains untrusted: format/byte checks are not a substitute for authoritative decoding and bounded processing. |
| [TD-C-030 · L3944](../../../04-spoolin-techdoc.md#623-initialization-receipt-extensions) | Typed Core ACCOUNT_INITIALIZATION and Catalog CATALOG_INITIALIZATION receipt fields/constraints permanent; failed same-hash resume, complete immutable bindings/counts, changed checksum fail; Media request-only restriction. |
| [TD-C-055 · L5982](../../../04-spoolin-techdoc.md#10-seed-data-initialization-and-migrations) | Exact licensed frozen100 movie/100TV seed, UUIDv5/source checksum/field provenance/reuse, distribution and original synopsis evidence; full atomic initial snapshot and typed receipt, ordinary rerun preserves edits/deletions/credits/accounts; optional artwork normal hold path. |
| [TECH-G-04 · L6520](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria) | G-04 licensed frozen dataset \| Assemble exact 100 movie/100 TV snapshot, stable identities, distributions, field-level sources, checksums/reuse evidence and permitted artwork \| Initial seed packaging; catalog owner unassigned |
| [TD-U-006 · L6539](../../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes) | TD-U-006 \| Seed from openly reusable internet facts, no mandatory data-provider account \| FR-007/008, OD-006, AC-033–037/158; facts may have multiple sources, with field provenance and separately permitted artwork |
| [ADR-08 · L6555](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-08 \| Frozen reusable seed with retained tombstones. No startup live scraping or destructive reseed \| TD-U-006, AC-033..037/138/158; new dataset is a reviewed versioned migration |
| [V-SEED · L6389](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-SEED \| Fresh100/100, multi-year/month counts, invalid input/license/hash conflict, kill before/after commit, two simultaneous starts, edits/archives/deletions/removed credits/account/artwork then rerun; assert reproducible identities and every preservation invariant |
| [TD-ROW-L3558 · L3558](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | seed_run \| Catalog operation_receipt: id→seed_version, snapshot_hash→seed_snapshot_hash, counts/summary→seed_*; created_at retained, COMPLETE→SUCCEEDED \| Full snapshot atomicity, permanent evidence and matching-checksum failure recovery |
| [TD-ROW-L3569 · L3569](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | seed_member and catalog domain tables \| Retained; seed_member.seed_id now references operation_receipt.seed_version \| Existing membership/relationship identities, constraints and deletion preservation |
| [TD-ROW-L3586 · L3586](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `operation_receipt` \| Each service: `operations/receipt`; Core `bootstrap` and Catalog `seed` use typed initialization projections \| One durable operation identity; permanently retained startup evidence uses explicit owner columns |
| [TD-ROW-L3599 · L3599](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `seed_member` \| Catalog `seed/persistence` \| Per-version record/relationship tombstones prevent resurrection; parent receipt stores seed-run evidence |
| [TD-ROW-L3626 · L3626](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `seed_member` and initialization receipt columns \| Restricted initializer/operator evidence \| No new GraphQL/MCP/RPC or production UI |
| [TD-ROW-L3954 · L3954](../../../04-spoolin-techdoc.md#core-account-initialization-fields) | `setup_version` \| `varchar(32)` \| UNIQUE; required only on ACCOUNT_INITIALIZATION; no default \| Original bootstrap version; permanent identity \| String? → internal only |
| [TD-ROW-L3955 · L3955](../../../04-spoolin-techdoc.md#core-account-initialization-fields) | `setup_manifest_hash` \| `bytea` \| Exactly 32 bytes; required only on ACCOUNT_INITIALIZATION \| Nonsecret original manifest checksum; equals request_hash \| ByteArray? → internal only; no audit/telemetry |
| [TD-ROW-L3956 · L3956](../../../04-spoolin-techdoc.md#core-account-initialization-fields) | `setup_viewer_id` \| `uuid` \| FK user_account(id), DELETE RESTRICT; required on successful setup \| Prepared Viewer identity; never arbitrary account adoption \| UUID? → internal only |
| [TD-ROW-L3957 · L3957](../../../04-spoolin-techdoc.md#core-account-initialization-fields) | `setup_maintainer_id` \| `uuid` \| FK user_account(id), DELETE RESTRICT; required on successful setup \| Prepared Maintainer identity \| UUID? → internal only |
| [TD-ROW-L3992 · L3992](../../../04-spoolin-techdoc.md#catalog-seed-initialization-fields) | `seed_version` \| `varchar(64)` \| UNIQUE; required only on CATALOG_INITIALIZATION; no default \| Original seed_run.id; seed_member.seed_id references this key \| String? → internal only |
| [TD-ROW-L3993 · L3993](../../../04-spoolin-techdoc.md#catalog-seed-initialization-fields) | `seed_snapshot_hash` \| `bytea` \| Exactly 32 bytes; required only on seed initialization \| Frozen snapshot checksum; equals request_hash \| ByteArray? → internal only; no audit/telemetry |
| [TD-ROW-L3994 · L3994](../../../04-spoolin-techdoc.md#catalog-seed-initialization-fields) | `seed_movie_count` \| `integer` \| 0..100; successful initialization requires 100 \| Original manifest count, never current catalog count \| Int? → internal only |
| [TD-ROW-L3995 · L3995](../../../04-spoolin-techdoc.md#catalog-seed-initialization-fields) | `seed_tv_count` \| `integer` \| 0..100; successful initialization requires 100 \| Original manifest count \| Int? → internal only |
| [TD-ROW-L3996 · L3996](../../../04-spoolin-techdoc.md#catalog-seed-initialization-fields) | `seed_summary` \| `jsonb` \| Object &lt;=16384 bytes; required for seed initialization \| Original distribution/source counts/validation revision; permanent typed evidence \| SeedSummary? → internal only |
| [TD-ROW-L5187 · L5187](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Seed \| One serialized catalog initialization transaction for the complete accepted snapshot \| READ COMMITTED + seed advisory lock then catalog lock; failed transaction leaves no partial catalog |
| [TD-ROW-L5513 · L5513](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-11 seed/supply chain \| Unauthorized reuse, tampered dependencies \| Field provenance/licenses/checksums, immutable image/package locks \| Changed snapshot rejected; missing license blocks bundle; manual evidence needed; data/build owner |
| [TD-ROW-L5533 · L5533](../../../04-spoolin-techdoc.md#83-api-security-enforcement) | Seed/admin tools / owning service and operator \| Verified configuration/manifest/checksums, protected credentials; same invariants or explicit repair verification \| Structured arguments, parameterized own-DB operations and no runtime schema privileges \| Hostile source text/paths/configuration cannot become executable SQL/process input; unsafe bootstrap configuration fails before readiness |
| [TD-BLOCK-L3964 · L3964](../../../04-spoolin-techdoc.md#core-account-initialization-fields) | Core account initialization fields — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4003 · L4003](../../../04-spoolin-techdoc.md#catalog-seed-initialization-fields) | Catalog seed initialization fields — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4782 · L4782](../../../04-spoolin-techdoc.md#seed_member-ddl) | seed_member DDL — complete sql contract, all members, constraints and field numbers |
| [BRAIN-A04 · L49](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A04 \| Service-to-service communication via gRPC \| Real RPC calls supporting application features; protobuf contracts and integration checks |
| [BRAIN-D05 · L74](../../../01-brainstorming-result.md#4-decision-register) | D05 \| Committed \| Design for authentication, authorization, RBAC, and audit logging from the start; implement core functionality first, then complete security |
| [BRAIN-D07 · L76](../../../01-brainstorming-result.md#4-decision-register) | D07 \| Recommended \| Viewer/Maintainer roles and predefined initial accounts; exact permission policy and account provisioning remain open |
| [BRAIN-D12 · L81](../../../01-brainstorming-result.md#4-decision-register) | D12 \| Open \| Select authentication/session mechanism and verified identity propagation through gRPC |
| [BRAIN-D19 · L88](../../../01-brainstorming-result.md#4-decision-register) | D19 \| Committed \| Unit and integration tests, including real gRPC communication and persistence against the selected database engine |
| [BRAIN-D24 · L93](../../../01-brainstorming-result.md#4-decision-register) | D24 \| Committed \| Seed an initial catalog of 100 distinct movies and 100 distinct TV series, including their metadata; each title type must span multiple release years and months; seeding must be reproducible and safe to rerun |
| [BRAIN-D25 · L94](../../../01-brainstorming-result.md#4-decision-register) | D25 \| Committed \| Categorize movies and TV series by title type and genres, include synopses and useful descriptive metadata, and retain stable identities and source provenance to support future review aggregation; detailed baseline in section 8.2 |
| [BRAIN-D26 · L95](../../../01-brainstorming-result.md#4-decision-register) | D26 \| Committed \| Filter catalog titles by release year and optionally by month within that year, composable with title type, genre, and text search; preserve source date precision |

## Implementation

1. Consume seed_member and its tombstone helper from T012; do not introduce a second table/migration. Coordinate population/replay with T020/T024 deletion transactions.
2. Implement permanent typed Core ACCOUNT_INITIALIZATION and Catalog CATALOG_INITIALIZATION receipts, source manifest fingerprints and lifetime seed_member/provenance deletion ledgers; Media remains REQUEST-only.
3. Apply the whole initial Catalog snapshot atomically with exact counts/relationships/receipt under source locking; optional permitted artwork follows normal processing/protective-hold attachment.
4. Prepare Viewer/Maintainer accounts from protected operator files once, without silently changing role/password/name/key state on rerun. Never bake credentials into config examples.
5. Resume failed same-hash initialization; reject changed hash/version misuse and concurrent conflicts. A completed ordinary rerun verifies permanent evidence rather than repairing deleted/changed user content.
6. Verify edits, archived/deleted movie/person, removed credit, new user record, failed initialization and concurrent start across restart. Setup tooling calls owner applications with owner credentials, never cross-DB SQL.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T033-A1:** Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles.
- **T033-A2:** Rerun: edited/archived/deleted records, removed credits/accounts/keys are preserved; ordinary result purge does not delete successful initialization evidence.
- **T033-A3:** Failure: a partial/crashed/concurrent run commits all or resumes safely, changed snapshot fails visibly, and prepared account rerun never promotes an existing user.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-029](../../../05-atdd.md#case-atdd-029-seed-metadata) / End-to-end | Inspect one movie and oneTV from frozen manifest; verify synopsislanguage, genreIDs, dates, source/retrievedAt/reuse and optional fields exactly. | Then Show usable title/type, synopsis, genres, release/first-air context, known optional metadata, and source attribution; preserve synopsis language. Observation: Compare source-backed movie/TV fields, language, genres/date precision and provenance with frozen manifest entries; separate permitted optional artwork from required metadata. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-033](../../../05-atdd.md#case-atdd-033-fresh-initial-seed) / System | F-SEED licensed frozen200-title snapshot; empty databases; compareexact100MOVIE/100 TV and normalized shared people/genres/roles/credits. | Then Exactly 100 distinct movies and 100 distinct TV series with required seed metadata exist; shared people/genres/credits are not duplicated. Observation: Count actual fresh MOVIE/TV rows independently, reconcile exactly100/100 and shared taxonomy/people/credits to the frozen manifest; incomplete phases cannot report complete. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-034](../../../05-atdd.md#case-atdd-034-variety) / System | Calculate independent per-type year and known month histograms from frozen manifest; reconcile rows and documented ranges; require &gt;1year and &gt;1knownmonth/type. | Then Each type spans multiple years, older/newer titles, and multiple known months; documented ranges/counts reconcile with the dataset. Equal quotas and every-month coverage are unnecessary. Observation: Compute per-type year/month histograms from frozen source and initialized rows independently; each spans more than one known year/month without fabricated precision. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-035](../../../05-atdd.md#case-atdd-035-preserve-user-decisions) / System | F-SEED changed installation: edited title/person; archived movie/person; deleted movie/person; removed credit; new manual title; protected accounts/artwork. Rerun identical manifest. | Then Preserve those decisions and user-created records; do not recreate deleted movies/people, reactivate archived records, reinstate removed credits, or duplicate titles/people/genres/credits. The original 100/100 target is not a later catalog cap or reset instruction. Observation: Snapshot edited/archived/deleted/removed/new domain members and protected state before identical rerun; inspect every preserved value and no resurrection. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-036](../../../05-atdd.md#case-atdd-036-interruptedconcurrent-seed) / System | Kill seed just before commit, justaftercommitbeforeack, and run twostarts behind a barrier. Changehash with same version in another row. | Then Report a truthful partial/failure/completed outcome without duplicate records or lost user decisions; a changed snapshot is not silently treated as the same input. Observation: Use real seed commit barriers and simultaneous starts; inspect all-or-none domain counts, one accepted manifest receipt and fatal changed-hash conflict. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-037](../../../05-atdd.md#case-atdd-037-repeatableinvalid-input) / System | Two fresh isolated namespaces use same manifest; invalid missing required field/unlicensed record/invalid reference/current policy violation each fail whole initialization. | Then Valid setups produce equivalent identities/metadata/counts; invalid records cannot be silently omitted while reporting a successful 100/100 dataset. Observation: Compare stable identities/checksums across two fresh installations; each singly invalid manifest fails atomically without partial seed success. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-049](../../../05-atdd.md#case-atdd-049-demo-access) / End-to-end | PreparedV/Aaccounts via protected bootstrap files; reviewer follows documented locations andsignin; no bypass identity. | Then Each account demonstrates its assigned capabilities; no unrestricted development bypass substitutes for role enforcement. Observation: Follow protected bootstrap file-location instructions with actual prepared accounts; verify assigned role sign-in and diagnostics excluding password contents. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-112](../../../05-atdd.md#case-atdd-112-seed-rerun-after-person-changes) / End-to-end | Seededpersonedited/archived/deleted and seed credit removed; normal/restarted/concurrent seed rerun; tombstones and current values preserved. | Then Preserve edits, archive state, permanent deletion, and removed credits; do not reactivate/recreate people or reinstate relationships. Later person/movie restoration still obeys those decisions, with no duplicate people or credits. Observation: Rerun the actual same seed after person edit/archive/delete/credit removal and inspect unchanged current rows/tombstones; frozen source is never upserted over decisions. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-132](../../../05-atdd.md#case-atdd-132-seed-compatibility-with-limits) / End-to-end | Freshmanifestpersonname101underm ax100 or unlicensed core fails whole initialization; rerun previously accepted edited values after tightening preserved. | Then Select source-permitted records meeting required seed data and adopted bounds without silently shortening names/metadata or fabricating values. Report incompatible required input rather than claiming a completed 100/100 dataset. Ordinary reruns preserve edits, archives, deletions, and removed credits under AC-035/AC-138 and do not rewrite retained data merely to satisfy tighter limits. Observation: Initialize a frozen manifest against current selected policy and reject a singly invalid member atomically; seed never overrides limits or fixes facts by truncation. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-196](../../../05-atdd.md#case-atdd-196-validate-the-reusable-frozen-catalog-before-initial-use) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §10.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then exact100 movie/100 TV, shared identities, required metadata and honest date/language/provenance hold And deterministic UUIDv5 length-prefixed source/kind/external IDs and checksums of deterministically serialized records reproduce across installs And original/licensed synopses and separately permitted optional art are required; no startup provider account or live scraping substitutes for a frozen snapshot Observation: Independently compare every frozen manifest member, source identity/license, checksum and year/month histogram with actual initialized records. Reject one-fact invalid copies atomically. No provider account/live scrape or reference artwork is accepted as required seed proof. |
| [ATDD-197](../../../05-atdd.md#case-atdd-197-commit-or-replay-the-complete-initial-snapshot) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §10.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then precommit interruption commits no partial catalog while post commit lostreply resolves completed initialization with internal SUCCEEDED And two starts share the serialized seed/catalog decision and same accepted hash And changed hash rejects fatally; a matching retry retains failure evidence and never reports incomplete counts as100/100 Observation: Kill actual initialization at precommit/postcommit barriers and overlap two starts under seed/catalog locks. Inspect complete counts and receipt hash, replay matching FAILED attempts and reject changed hash/version identity without partial records. Inspect CATALOG_INITIALIZATION operation_receipt with seed_version/snapshot hash/typed counts/summary outside result JSON. Internal SUCCEEDED denotes completed seed; PENDING/FAILED cannot claim partial100/100. Same-checksum failure resumes under initializer lock with prior operational failure preserved; changed checksum rejects without writes. |
| [ATDD-199](../../../05-atdd.md#case-atdd-199-initialize-prepared-accounts-once-without-promotion) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §10.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then the first accounts/aliases/hashes/receipt commit atomically with passwords in protected files And matching reruns preserve current passwords/roles/sessions/key state And conflicts fail without arbitraryViewerpromotion or printing secret contents; only file locations are shown Observation: Run actual Core bootstrap with protected files twice/concurrently and with conflicting manifest/identity. Inspect encrypted accounts/aliases/receipt and preservation of current passwords/roles/sessions; diagnostics may name paths but must not contain secret contents. Core ACCOUNT_INITIALIZATION receipt has permanent setup_version/hash and two distinct real account references, outside expiring result JSON. Partial/failed setup contains no successful account bindings. Matching retry may finish atomically while preserving failure history; completed setup never recreates/reset accounts, passwords or roles. |

Partial implementation contributions: [ATDD-198](../../../05-atdd.md#case-atdd-198-verify-receipts-without-resetting-user-decisions), [ATDD-235](../../../05-atdd.md#case-atdd-235-preserve-permanent-core-initialization-receipt-evidence), [ATDD-243](../../../05-atdd.md#case-atdd-243-preserve-permanent-catalog-initialization-and-seed-membership-evidence), [ATDD-244](../../../05-atdd.md#case-atdd-244-enforce-lifetime-seed-membership-and-removal-preservation). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T033` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T033` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
