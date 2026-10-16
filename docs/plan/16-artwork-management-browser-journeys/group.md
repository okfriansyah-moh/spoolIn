---
kind: group
id: G16
feature: artwork-management
tasks:
- T026
integration_owner: T026
---

# 16-artwork-management-browser-journeys

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Progress: observed byte events drive transfer only; processing remains visible until actual attachment and current movie image agree.

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
| T026 | Progress: observed byte events drive transfer only; processing remains visible until actual attachment and current movie image agree. | [T026 — Connect measured progress and recover attachment outcomes](tasks/T026-connect-measured-progress-and-recover-attachment-outcomes.md) |

## Dependencies

External inputs: [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T010 — Connect account and session browser journeys](../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md), [T017 — Connect movie editors and confirmed outcomes](../12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md), [T022 — Accept and process bounded artwork streams](../14-artwork-management-processing-and-attachment/tasks/T022-accept-and-process-bounded-artwork-streams.md), [T023 — Coordinate protective holds, attachment and cleanup](../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md), [T024 — Implement current-impact archive, restore and deletion](../15-archive-management/tasks/T024-implement-current-impact-archive-restore-and-deletion.md).

- T026 starts after T004, T010, T017, T022, T023, T024.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T026 — Connect measured progress and recover attachment outcomes](tasks/T026-connect-measured-progress-and-recover-attachment-outcomes.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T026: Progress: observed byte events drive transfer only; processing remains visible until actual attachment and current movie image agree.
- T026: Recovery: invalid/rejected/new failed replacement leaves previous image and metadata; unknown result preserves uncertainty and checks original UUID.
- T026: Safety: cancelled or expired-user upload cannot gain unauthorized attachment and unavailable original image still has readable fallback.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
