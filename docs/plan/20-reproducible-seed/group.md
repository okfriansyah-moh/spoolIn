---
kind: group
id: G20
feature: reproducible-seed
tasks:
- T032
- T033
integration_owner: T033
---

# 20-reproducible-seed

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Dataset: frozen accepted manifest has exactly100/100 unique source identities and complete required values with usable provenance/reuse evidence. Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles.

This stage belongs to **reproducible-seed**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

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
| T032 | Dataset: frozen accepted manifest has exactly100/100 unique source identities and complete required values with usable provenance/reuse evidence. | [T032 — Assemble the permitted frozen 100-movie and 100-series dataset](tasks/T032-assemble-the-permitted-frozen-100-movie-and-100-series-dataset.md) |
| T033 | Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles. | [T033 — Initialize and replay seed and prepared accounts safely](tasks/T033-initialize-and-replay-seed-and-prepared-accounts-safely.md) |

## Dependencies

External inputs: [T001 — Lock the build and isolated verification harness](../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md), [T006 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md), [T007 — Implement owner receipts and atomic mutation evidence](../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md), [T008 — Implement registration and authoritative sessions](../06-account-access-registration-and-sessions/tasks/T008-implement-registration-and-authoritative-sessions.md), [T010 — Connect account and session browser journeys](../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md), [T012 — Serve precise public catalog and people reads](../10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md), [T013 — Connect discovery, details and public people views](../10-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T016 — Implement authoritative movie creation and edits](../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md), [T018 — Implement shared person writes and identity](../13-people-and-credits/tasks/T018-implement-shared-person-writes-and-identity.md), [T020 — Implement ordered credits and custom contribution roles](../13-people-and-credits/tasks/T020-implement-ordered-credits-and-custom-contribution-roles.md), [T022 — Accept and process bounded artwork streams](../14-artwork-management-processing-and-attachment/tasks/T022-accept-and-process-bounded-artwork-streams.md), [T023 — Coordinate protective holds, attachment and cleanup](../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md), [T024 — Implement current-impact archive, restore and deletion](../15-archive-management/tasks/T024-implement-current-impact-archive-restore-and-deletion.md).

- T032 starts after T001, T006.
- T033 starts after T008, T007, T012, T016, T018, T020, T022, T023, T032, T024, T010, T013.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T033 — Initialize and replay seed and prepared accounts safely](tasks/T033-initialize-and-replay-seed-and-prepared-accounts-safely.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T032: Dataset: frozen accepted manifest has exactly100/100 unique source identities and complete required values with usable provenance/reuse evidence.
- T032: Variety: both types span multiple years/known months with actual recorded distributions and no fabricated day precision.
- T032: Integrity: tampered checksum, missing permission, invalid relationship or incompatible required value fails the bundle; same source data produces the same identities regardless of display spelling.
- T033: Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles.
- T033: Rerun: edited/archived/deleted records, removed credits/accounts/keys are preserved; ordinary result purge does not delete successful initialization evidence.
- T033: Failure: a partial/crashed/concurrent run commits all or resumes safely, changed snapshot fails visibly, and prepared account rerun never promotes an existing user.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
