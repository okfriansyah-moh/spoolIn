---
kind: group
id: G17
feature: draft-recovery
tasks:
- T027
integration_owner: T027
---

# 17-draft-recovery-editor-integration

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Real recovery: after committed save loses response, resume reveals the original outcome and current record without creating another movie/person/credit.

This stage belongs to **draft-recovery**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [3.4 Forms, drafts and conflict recovery](../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery).
- [5.6.5 Code example: save and reconcile a draft](../../04-spoolin-techdoc.md#565-code-example-save-and-reconcile-a-draft).
- [5.11 JSON projections and draft schemas](../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas).
- [6.2.4 Core tables](../../04-spoolin-techdoc.md#624-core-tables).
- [7.3.1 Durable draft receipts](../../04-spoolin-techdoc.md#731-durable-draft-receipts).
- [3.4.1 Browser storage and restored-page state](../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state).
- [8.5.4 Session and CSRF responsibility map](../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T027 | Real recovery: after committed save loses response, resume reveals the original outcome and current record without creating another movie/person/credit. | [T027 — Prove recovery through real movie, person and credit editors](tasks/T027-prove-recovery-through-real-movie-person-and-credit-editors.md) |

## Dependencies

External inputs: [T015 — Build account-isolated browser draft controllers](../11-draft-recovery-storage-and-controllers/tasks/T015-build-account-isolated-browser-draft-controllers.md), [T017 — Connect movie editors and confirmed outcomes](../12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md), [T019 — Connect people management and retained edits](../13-people-and-credits/tasks/T019-connect-people-management-and-retained-edits.md), [T021 — Connect credit editing and verify the people outcome](../13-people-and-credits/tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md), [T025 — Connect private archives and irreversible confirmations](../15-archive-management/tasks/T025-connect-private-archives-and-irreversible-confirmations.md), [T026 — Connect measured progress and recover attachment outcomes](../16-artwork-management-browser-journeys/tasks/T026-connect-measured-progress-and-recover-attachment-outcomes.md).

- T027 starts after T015, T017, T019, T021, T025, T026.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T027 — Prove recovery through real movie, person and credit editors](tasks/T027-prove-recovery-through-real-movie-person-and-credit-editors.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T027: Real recovery: after committed save loses response, resume reveals the original outcome and current record without creating another movie/person/credit.
- T027: Ownership: B cannot inherit A's recovery data through storage/history/delayed replies; A's expired/revoked session cannot expose it before revalidation.
- T027: Mutation recovery: stale or removed targets require review and retain only permitted work; no force overwrite, resurrection or secret persistence.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
