---
kind: group
id: G13
feature: people-and-credits
tasks:
- T018
- T019
- T020
- T021
integration_owner: T021
---

# 13-people-and-credits

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact. Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied. Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit. Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning.

This stage belongs to **people-and-credits**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [4.3 SpoolIn Catalog service](../../04-spoolin-techdoc.md#43-spoolin-catalog-service).
- [5.3 Operation, permission and deadline inventory](../../04-spoolin-techdoc.md#53-operation-permission-and-deadline-inventory).
- [6.2.5 Catalog tables](../../04-spoolin-techdoc.md#625-catalog-tables).
- [7.1 Transaction inventory](../../04-spoolin-techdoc.md#71-transaction-inventory).
- [3.1.1 UI reference and implementation map](../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map).
- [3.4 Forms, drafts and conflict recovery](../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery).
- [3.6 Components, accessibility and responsive behavior](../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior).
- [5.4 Projection and presence crosswalk](../../04-spoolin-techdoc.md#54-projection-and-presence-crosswalk).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T018 | Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact. | [T018 — Implement shared person writes and identity](tasks/T018-implement-shared-person-writes-and-identity.md) |
| T019 | Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied. | [T019 — Connect people management and retained edits](tasks/T019-connect-people-management-and-retained-edits.md) |
| T020 | Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit. | [T020 — Implement ordered credits and custom contribution roles](tasks/T020-implement-ordered-credits-and-custom-contribution-roles.md) |
| T021 | Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning. | [T021 — Connect credit editing and verify the people outcome](tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md) |

## Dependencies

External inputs: [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T006 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md), [T007 — Implement owner receipts and atomic mutation evidence](../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md), [T009 — Enforce scoped service identity and shared admission](../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md), [T010 — Connect account and session browser journeys](../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md), [T012 — Serve precise public catalog and people reads](../10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md), [T013 — Connect discovery, details and public people views](../10-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T015 — Build account-isolated browser draft controllers](../11-draft-recovery-storage-and-controllers/tasks/T015-build-account-isolated-browser-draft-controllers.md), [T016 — Implement authoritative movie creation and edits](../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md), [T017 — Connect movie editors and confirmed outcomes](../12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md).

- T018 starts after T009, T006, T007, T012, T016.
- T019 starts after T004, T010, T013, T015, T018.
- T020 starts after T009, T006, T007, T012, T016, T018.
- T021 starts after T004, T010, T015, T017, T019, T020.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T021 — Connect credit editing and verify the people outcome](tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T018: Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact.
- T018: Identity: two Alex records stay distinct and a legitimate correction appears through that person's permitted credits.
- T018: State: archived/missing/unauthorized person cannot be edited or recreated by stale retry; receipt/audit reflects only confirmed effects.
- T019: Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied.
- T019: Validation: one-character professional name is allowed, rejected input is retained intact and no account data appears.
- T019: Feedback: stale or unavailable target cannot be silently recreated and lost replies are reconciled before another write.
- T020: Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit.
- T020: Concurrency: duplicate role/credit races yield one allowed identity/effect; changed references require review with no dangling relation.
- T020: Lifecycle: retained credit removal from an active movie survives person restoration; direct TV management remains denied.
- T021: Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning.
- T021: Negative: Viewer, archived person assignment, direct TV edit, stale reference and repeated identical request never create unintended relationships.
- T021: UI: each credit state shows full context, accessible correction and safe next action under both themes and narrow layouts.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
