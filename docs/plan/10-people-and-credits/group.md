---
kind: group
id: G10
tasks:
- T019
- T020
- T021
- T022
integration_owner: T022
---

# 10-people-and-credits

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact. Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied. Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit. Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

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
| T019 | Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact. | [T019 — Implement shared person writes and identity](tasks/T019-implement-shared-person-writes-and-identity.md) |
| T020 | Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied. | [T020 — Connect people management and retained edits](tasks/T020-connect-people-management-and-retained-edits.md) |
| T021 | Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit. | [T021 — Implement ordered credits and custom contribution roles](tasks/T021-implement-ordered-credits-and-custom-contribution-roles.md) |
| T022 | Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning. | [T022 — Connect credit editing and verify the people outcome](tasks/T022-connect-credit-editing-and-verify-the-people-outcome.md) |

## Dependencies

External inputs: [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T007 — Connect account and session browser journeys](../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md), [T012 — Serve precise public catalog and people reads](../07-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md), [T013 — Connect discovery, details and public people views](../07-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T015 — Build account-isolated browser draft controllers](../08-draft-recovery/tasks/T015-build-account-isolated-browser-draft-controllers.md), [T017 — Implement authoritative movie creation and edits](../09-movie-management/tasks/T017-implement-authoritative-movie-creation-and-edits.md), [T018 — Connect movie editors and confirmed outcomes](../09-movie-management/tasks/T018-connect-movie-editors-and-confirmed-outcomes.md). Consume their concrete outputs as stated in member prerequisites.

- T019 starts after T008, T009, T010, T012, T017; its task definition owns the specific files/contracts and acceptance boundary.
- T020 starts after T004, T007, T013, T015, T019; its task definition owns the specific files/contracts and acceptance boundary.
- T021 starts after T008, T009, T010, T012, T017, T019; its task definition owns the specific files/contracts and acceptance boundary.
- T022 starts after T004, T007, T015, T018, T020, T021; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T022 — Connect credit editing and verify the people outcome](tasks/T022-connect-credit-editing-and-verify-the-people-outcome.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T019: Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact.
- T019: Identity: two Alex records stay distinct and a legitimate correction appears through that person's permitted credits.
- T019: State: archived/missing/unauthorized person cannot be edited or recreated by stale retry; receipt/audit reflects only confirmed effects.
- T020: Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied.
- T020: Validation: one-character professional name is allowed, rejected input is retained intact and no account data appears.
- T020: Feedback: stale or unavailable target cannot be silently recreated and lost replies are reconciled before another write.
- T021: Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit.
- T021: Concurrency: duplicate role/credit races yield one allowed identity/effect; changed references require review with no dangling relation.
- T021: Lifecycle: retained credit removal from an active movie survives person restoration; direct TV management remains denied.
- T022: Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning.
- T022: Negative: Viewer, archived person assignment, direct TV edit, stale reference and repeated identical request never create unintended relationships.
- T022: UI: each credit state shows full context, accessible correction and safe next action under both themes and narrow layouts.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
