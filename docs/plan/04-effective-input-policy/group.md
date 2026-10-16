---
kind: group
id: G04
feature: effective-input-policy
tasks:
- T006
integration_owner: T006
---

# 04-effective-input-policy

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.

This stage belongs to **effective-input-policy**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [3.4 Forms, drafts and conflict recovery](../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery).
- [5.4 Projection and presence crosswalk](../../04-spoolin-techdoc.md#54-projection-and-presence-crosswalk).
- [5.11 JSON projections and draft schemas](../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas).
- [11.1 Configuration contract](../../04-spoolin-techdoc.md#111-configuration-contract).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T006 | Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences. | [T006 — Implement effective policy and precise input semantics](tasks/T006-implement-effective-policy-and-precise-input-semantics.md) |

## Dependencies

External inputs: [T002 — Generate and validate the shared wire contracts](../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md), [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md).

- T006 starts after T002, T003, T004.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T006 — Implement effective policy and precise input semantics](tasks/T006-implement-effective-policy-and-precise-input-semantics.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T006: Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- T006: Compatibility: tightening does not truncate data/artwork or invalidate old credentials; a changed over-limit value fails with preserved draft and no partial mutation.
- T006: Presence: absent keeps the stored value, explicit null/trimmed blank optional text clears, and a slash inside a character string is retained. ATDD-278 verifies each omission/null/blank outcome at the isolated rule boundary.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
