---
kind: group
id: G14
feature: artwork-management
tasks:
- T022
- T023
integration_owner: T023
---

# 14-artwork-management-processing-and-attachment

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing. Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.

This stage belongs to **artwork-management**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [4.4 SpoolIn Media service](../../04-spoolin-techdoc.md#44-spoolin-media-service).
- [5.2.5 Media messages](../../04-spoolin-techdoc.md#525-media-messages).
- [6.2.6 Media tables](../../04-spoolin-techdoc.md#626-media-tables).
- [7.6 Artwork transfer and reference-safe attachment](../../04-spoolin-techdoc.md#76-artwork-transfer-and-reference-safe-attachment).
- [8.3.2 Output and executable-input controls](../../04-spoolin-techdoc.md#832-output-and-executable-input-controls).
- [5.5 Browser HTTP and operational endpoints](../../04-spoolin-techdoc.md#55-browser-http-and-operational-endpoints).
- [6.2.5 Catalog tables](../../04-spoolin-techdoc.md#625-catalog-tables).
- [7.7 Durable work, retries and notifications](../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications).
- [3.5 Artwork and operation feedback](../../04-spoolin-techdoc.md#35-artwork-and-operation-feedback).
- [3.1.1 UI reference and implementation map](../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T022 | Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing. | [T022 — Accept and process bounded artwork streams](tasks/T022-accept-and-process-bounded-artwork-streams.md) |
| T023 | Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion. | [T023 — Coordinate protective holds, attachment and cleanup](tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md) |

## Dependencies

External inputs: [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T006 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md), [T007 — Implement owner receipts and atomic mutation evidence](../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md), [T009 — Enforce scoped service identity and shared admission](../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md), [T011 — Run fenced recovery and bounded retries](../09-reliable-work-recovery-and-retries/tasks/T011-run-fenced-recovery-and-bounded-retries.md), [T016 — Implement authoritative movie creation and edits](../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md).

- T022 starts after T003, T009, T006, T007, T011.
- T023 starts after T009, T007, T011, T016, T022.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T023 — Coordinate protective holds, attachment and cleanup](tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T022: Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing.
- T022: Execution: hostile path/option metadata executes no extra command, fetch or write outside quarantine/output boundaries.
- T022: Durability: crash before/after accepted processing leaves truthful resumable state, one work binding and no false READY or lost protected output.
- T023: Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.
- T023: Cancel: either abort wins with no new attachment or attach wins truthfully; a stale/delayed release cannot undo the winner.
- T023: Retrieval: public/Viewer cannot fetch private/archived/unattached assets by guessed ID or stale URL; Media outage leaves unrelated metadata usable.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
