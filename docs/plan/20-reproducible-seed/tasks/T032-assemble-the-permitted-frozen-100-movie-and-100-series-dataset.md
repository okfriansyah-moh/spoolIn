---
kind: task
id: T032
group: G20
depends_on:
- T001
- T006
readiness: Ready
status: In progress
risk: medium
test_strategy: alongside
visual_review: not-applicable
---

# T032 — Assemble the permitted frozen 100-movie and 100-series dataset

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Dataset: frozen accepted manifest has exactly100/100 unique source identities and complete required values with usable provenance/reuse evidence. This task owns the change surface and local evidence below. Risk is medium: incorrect composition, build assumptions or delivery claims can hide missing behavior. Verify the actual declared boundary. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-029 | implements |
| ATDD-112 | implements |
| ATDD-033 | implements |
| ATDD-034 | implements |
| ATDD-035 | implements |
| ATDD-036 | implements |
| ATDD-037 | implements |
| ATDD-049 | implements |
| ATDD-132 | implements |
| ATDD-196 | implements |
| ATDD-197 | implements |
| ATDD-199 | implements |
| ATDD-301 | both |
| SC-02 | implements |
| FR-005 | implements |
| FR-007 | implements |
| FR-008 | implements |
| FR-011 | implements |
| FR-017 | implements |
| NFR-008 | both |
| NFR-016 | implements |
| A04 | implements |
| D05 | implements |
| D07 | implements |
| D12 | implements |
| D19 | implements |
| D24 | implements |
| D25 | implements |
| D26 | implements |
| AC-138 | implements |
| AC-138-C01 | implements |
| AC-138-C02 | implements |
| AC-029 | implements |
| AC-029-C01 | implements |
| AC-049 | implements |
| AC-049-C01 | implements |
| AC-111 | implements |
| AC-111-C01 | implements |
| AC-158 | implements |
| AC-158-C01 | implements |
| AC-158-C02 | implements |
| AC-158-C03 | implements |
| AC-087 | implements |
| G-03 | implements |
| AC-033 | implements |
| AC-033-C01 | implements |
| AC-034 | implements |
| AC-034-C01 | implements |
| AC-034-C02 | implements |
| AC-035 | implements |
| AC-035-C01 | implements |
| AC-035-C02 | implements |
| AC-036 | implements |
| AC-036-C01 | implements |
| AC-037 | implements |
| AC-037-C01 | implements |
| OD-002 | implements |
| OD-004 | implements |
| OD-018 | implements |
| OD-006 | implements |
| US-006 | implements |
| PRD-POLICY-01 | implements |
| PRD-POLICY-09 | implements |
| PRD-POLICY-11 | implements |
| PRD-POLICY-19 | implements |
| PRD-CONCEPT-02 | implements |
| PRD-CONCEPT-05 | implements |
| PRD-CONCEPT-12 | implements |
| PRD-HANDOFF-09 | implements |
| F-SOURCES | both |
| ATDD-G-04 | implements |

## Prerequisites

- [T001 — Lock the build and isolated verification harness](../../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted.
- [T006 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Source/reuse evidence and exact manifest do not exist yet and must be assembled in this task. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | data/seed/manifest.json | T032 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | data/seed/sources/ | T032 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | data/seed/artwork/ | T032 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | build-tools/validate-seed/ | T032 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [10.1 Frozen seed contract](../../../04-spoolin-techdoc.md#101-frozen-seed-contract): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [14. Technical dependencies and release criteria](../../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
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
| [ATDD-G-04 · L14188](../../../05-atdd.md#required-execution-and-release-prerequisites) | G-04 licensed frozen seed \| Actual100/100 reusable facts/manifest/source checksums and optional separately permitted artwork; documentation contains no completed runtime seed |

## Implementation

1. Select openly reusable facts with no mandatory provider account and record accessible reuse basis per source and separately for optional artwork. Research actual sources when implementing; supplied design posters do not satisfy production data rights.
2. Freeze exactly100 distinct movies and100 distinct TV titles, required metadata and honest source date precision, stable UUIDv5 source-qualified identity, synopsis language/originality and per-field provenance.
3. Record source checksums, snapshot/retrieval dates, attribution, chosen year range and per-type year/known-month counts spanning multiple years/months. Do not fabricate optional facts, truncate names or impose unapproved equal quotas.
4. Validate complete record/relationship references and effective policy before accepting the manifest; a rejected record cannot be silently dropped while claiming100/100.
5. Build deterministic validation/manifest identity tooling and reset-free fixture exports. Runtime safe initialization belongs to T033.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T032-A1:** Dataset: frozen accepted manifest has exactly100/100 unique source identities and complete required values with usable provenance/reuse evidence.
- **T032-A2:** Variety: both types span multiple years/known months with actual recorded distributions and no fabricated day precision.
- **T032-A3:** Integrity: tampered checksum, missing permission, invalid relationship or incompatible required value fails the bundle; same source data produces the same identities regardless of display spelling.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Use checks alongside implementation because build/visual/runtime composition must be exercised as its interfaces appear; acceptance still governs before completion. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-301](../../../05-atdd.md#case-atdd-301-derive-stable-seed-identities-independently-of-spelling) / Unit | F-SEED namespace; source/kind/externalID tuple; changed display title; ambiguous concatenations (ab, c) versus(a, bc); changed source/kind; timestamp-only change. | Then identical source identity keeps UUID despite display spelling changes And distinct length-prefixed identities cannot collapse by concatenation And nondeterministic retrieval/run timestamps do not alter the defined record checksum Observation: Use independently computed fixed UUIDv5/deterministic-content vectors; real seed transaction/rerun/tombstone preservation remains integration. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-SOURCES · L210](../../../05-atdd.md#f-sources-and-f-impact-independent-capacity-boundaries) | Source evidence uses supported field/url/reuseBasis and exact reviewed normalized values. Each source record’s fieldSources object has at most one item per field; different source records may support the same field. Group evidence by its source identity without merging unrelated provider IDs. Prepare fixtures measured by actual PostgreSQL `octet_length(field_sources::text)` at 16383/16384/16385 bytes, separately from ordinary request and MCP result/resource131071/131072/131073 bytes. Field/schema invalidity maps INVALID_ARGUMENT; exhausted envelopes/representation capacity map RESOURCE_EXHAUSTED. Reject before committing partial proposal/publication/provenance; no silent evidence omission, URL fetch or truncated review. If actual DB bound checking raises23514, rollback and map invalid input safely rather than leaking SQL. Impact count is exact and cannot wrap GraphQL Int. Test2147483647/2147483648 at the scalar projector. In real integration, grow actual deterministic credits/title tuples until the complete serialized preview exceeds its existing response bound; return RESOURCE_EXHAUSTED and no usable deletion token, with all records unchanged. Do not invent a feature cap, silently paginate a signed partial deletion consequence, or claim a simulated huge count proves a real database dataset. |

Partial implementation contributions: [ATDD-029](../../../05-atdd.md#case-atdd-029-seed-metadata), [ATDD-112](../../../05-atdd.md#case-atdd-112-seed-rerun-after-person-changes), [ATDD-033](../../../05-atdd.md#case-atdd-033-fresh-initial-seed), [ATDD-034](../../../05-atdd.md#case-atdd-034-variety), [ATDD-035](../../../05-atdd.md#case-atdd-035-preserve-user-decisions), [ATDD-036](../../../05-atdd.md#case-atdd-036-interruptedconcurrent-seed), [ATDD-037](../../../05-atdd.md#case-atdd-037-repeatableinvalid-input), [ATDD-049](../../../05-atdd.md#case-atdd-049-demo-access), [ATDD-132](../../../05-atdd.md#case-atdd-132-seed-compatibility-with-limits), [ATDD-196](../../../05-atdd.md#case-atdd-196-validate-the-reusable-frozen-catalog-before-initial-use), [ATDD-197](../../../05-atdd.md#case-atdd-197-commit-or-replay-the-complete-initial-snapshot), [ATDD-199](../../../05-atdd.md#case-atdd-199-initialize-prepared-accounts-once-without-promotion). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T032` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

Only isolated build/unit/component validation is required here. The source case/row owners named in Traceability retain complete real-service/client proof; no empty integration selection is required for local completion. Graphical captures below still inspect a rendered component/reference under the declared contexts.

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T033 — Initialize and replay seed and prepared accounts safely](T033-initialize-and-replay-seed-and-prepared-accounts-safely.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
