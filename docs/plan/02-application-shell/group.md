---
kind: group
id: G02
feature: application-shell
tasks:
- T004
integration_owner: T004
---

# 02-application-shell

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.

This stage belongs to **application-shell**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [3.1 Routes, screens and API dependencies](../../04-spoolin-techdoc.md#31-routes-screens-and-api-dependencies).
- [3.4.1 Browser storage and restored-page state](../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state).
- [3.6 Components, accessibility and responsive behavior](../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T004 | Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette. | [T004 — Build the shared themed and accessible shell](tasks/T004-build-the-shared-themed-and-accessible-shell.md) |

## Dependencies

External inputs: [T001 — Lock the build and isolated verification harness](../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md), [T002 — Generate and validate the shared wire contracts](../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md), [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md).

- T004 starts after T001, T002, T003.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T004 — Build the shared themed and accessible shell](tasks/T004-build-the-shared-themed-and-accessible-shell.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T004: Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- T004: Controls: keyboard-only invalid submission points to associated correction without color-only signals or lost content; dialog cancellation changes no business state.
- T004: Presentation: shared components show readable full identity at 320px with 44px targets and approved typography; generated preview imagery is a fixture reference only.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
