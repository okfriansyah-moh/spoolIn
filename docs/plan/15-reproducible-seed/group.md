---
kind: group
id: G15
tasks:
- T034
- T035
integration_owner: T035
---

# 15-reproducible-seed

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Dataset: frozen accepted manifest has exactly100/100 unique source identities and complete required values with usable provenance/reuse evidence. Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [10.1 Frozen seed contract](../../04-spoolin-techdoc.md#101-frozen-seed-contract).
- [6.2.5 Catalog tables](../../04-spoolin-techdoc.md#625-catalog-tables).
- [14. Technical dependencies and release criteria](../../04-spoolin-techdoc.md#14-technical-dependencies-and-release-criteria).
- [6.2.3 Initialization receipt extensions](../../04-spoolin-techdoc.md#623-initialization-receipt-extensions).
- [7.3.2 Attempt events and initialization replay](../../04-spoolin-techdoc.md#732-attempt-events-and-initialization-replay).
- [11.2 Startup DAG and readiness](../../04-spoolin-techdoc.md#112-startup-dag-and-readiness).

## Task list

| Task | Result | Detail |
|---|---|---|
| T034 | Dataset: frozen accepted manifest has exactly100/100 unique source identities and complete required values with usable provenance/reuse evidence. | [T034 — Assemble the permitted frozen 100-movie and 100-series dataset](tasks/T034-assemble-the-permitted-frozen-100-movie-and-100-series-dataset.md) |
| T035 | Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles. | [T035 — Initialize and replay seed and prepared accounts safely](tasks/T035-initialize-and-replay-seed-and-prepared-accounts-safely.md) |

## Dependencies

External inputs: [T001 — Lock the build and isolated verification harness](../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md), [T006 — Implement registration and authoritative sessions](../03-account-access/tasks/T006-implement-registration-and-authoritative-sessions.md), [T007 — Connect account and session browser journeys](../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md), [T012 — Serve precise public catalog and people reads](../07-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md), [T013 — Connect discovery, details and public people views](../07-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T017 — Implement authoritative movie creation and edits](../09-movie-management/tasks/T017-implement-authoritative-movie-creation-and-edits.md), [T019 — Implement shared person writes and identity](../10-people-and-credits/tasks/T019-implement-shared-person-writes-and-identity.md), [T021 — Implement ordered credits and custom contribution roles](../10-people-and-credits/tasks/T021-implement-ordered-credits-and-custom-contribution-roles.md), [T023 — Implement current-impact archive, restore and deletion](../12-archive-management/tasks/T023-implement-current-impact-archive-restore-and-deletion.md), [T025 — Accept and process bounded artwork streams](../11-artwork-management/tasks/T025-accept-and-process-bounded-artwork-streams.md), [T026 — Coordinate protective holds, attachment and cleanup](../11-artwork-management/tasks/T026-coordinate-protective-holds-attachment-and-cleanup.md). Consume their concrete outputs as stated in member prerequisites.

- T034 starts after T001, T009; its task definition owns the specific files/contracts and acceptance boundary.
- T035 starts after T006, T010, T012, T017, T019, T021, T025, T026, T034, T023, T007, T013; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T035 — Initialize and replay seed and prepared accounts safely](tasks/T035-initialize-and-replay-seed-and-prepared-accounts-safely.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T034: Dataset: frozen accepted manifest has exactly100/100 unique source identities and complete required values with usable provenance/reuse evidence.
- T034: Variety: both types span multiple years/known months with actual recorded distributions and no fabricated day precision.
- T034: Integrity: tampered checksum, missing permission, invalid relationship or incompatible required value fails the bundle; same source data produces the same identities regardless of display spelling.
- T035: Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles.
- T035: Rerun: edited/archived/deleted records, removed credits/accounts/keys are preserved; ordinary result purge does not delete successful initialization evidence.
- T035: Failure: a partial/crashed/concurrent run commits all or resumes safely, changed snapshot fails visibly, and prepared account rerun never promotes an existing user.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
