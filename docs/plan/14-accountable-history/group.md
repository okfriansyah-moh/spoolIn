---
kind: group
id: G13
tasks:
- T028
integration_owner: T028
---

# 14-accountable-history

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Attribution: history contains actor/action/affected record/time/outcome/relevant changes for each included event; no absent fields are inferred from scalar compatibility. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [3.1.1 UI reference and implementation map](../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map).
- [4.3 SpoolIn Catalog service](../../04-spoolin-techdoc.md#43-spoolin-catalog-service).
- [5.4 Projection and presence crosswalk](../../04-spoolin-techdoc.md#54-projection-and-presence-crosswalk).
- [6.2.5 Catalog tables](../../04-spoolin-techdoc.md#625-catalog-tables).
- [8.3 API security enforcement](../../04-spoolin-techdoc.md#83-api-security-enforcement).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T028 | Attribution: history contains actor/action/affected record/time/outcome/relevant changes for each included event; no absent fields are inferred from scalar compatibility. | [T028 — Expose complete read-only audit outcomes](tasks/T028-expose-complete-read-only-audit-outcomes.md) |

## Dependencies

External inputs: [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T007 — Connect account and session browser journeys](../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md), [T017 — Implement authoritative movie creation and edits](../09-movie-management/tasks/T017-implement-authoritative-movie-creation-and-edits.md), [T020 — Connect people management and retained edits](../10-people-and-credits/tasks/T020-connect-people-management-and-retained-edits.md), [T022 — Connect credit editing and verify the people outcome](../10-people-and-credits/tasks/T022-connect-credit-editing-and-verify-the-people-outcome.md), [T024 — Connect private archives and irreversible confirmations](../12-archive-management/tasks/T024-connect-private-archives-and-irreversible-confirmations.md), [T026 — Coordinate protective holds, attachment and cleanup](../11-artwork-management/tasks/T026-coordinate-protective-holds-attachment-and-cleanup.md), [T030 — Commit immutable proposals and exact approval once](../13-assisted-movie-creation/tasks/T030-commit-immutable-proposals-and-exact-approval-once.md). Consume their concrete outputs as stated in member prerequisites.

- T028 starts after T004, T007, T017, T020, T022, T024, T026, T030; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T028 — Expose complete read-only audit outcomes](tasks/T028-expose-complete-read-only-audit-outcomes.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T028: Attribution: history contains actor/action/affected record/time/outcome/relevant changes for each included event; no absent fields are inferred from scalar compatibility.
- T028: Privacy: Viewer/public denied reads expose no content, technical events never appear in Maintainer history, and all text is inert.
- T028: Truth: failed transaction has no success audit, approval remains distinct from created movie, and large diff explicitly says what was omitted without corrupting stored data.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
