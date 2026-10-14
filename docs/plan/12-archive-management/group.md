---
kind: group
id: G11
tasks:
- T023
- T024
integration_owner: T024
---

# 12-archive-management

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing. Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [5.3 Operation, permission and deadline inventory](../../04-spoolin-techdoc.md#53-operation-permission-and-deadline-inventory).
- [6.2.5 Catalog tables](../../04-spoolin-techdoc.md#625-catalog-tables).
- [7.1 Transaction inventory](../../04-spoolin-techdoc.md#71-transaction-inventory).
- [7.5 Additional consequential native operations](../../04-spoolin-techdoc.md#75-additional-consequential-native-operations).
- [7.6 Artwork transfer and reference-safe attachment](../../04-spoolin-techdoc.md#76-artwork-transfer-and-reference-safe-attachment).
- [3.1.1 UI reference and implementation map](../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map).
- [3.4 Forms, drafts and conflict recovery](../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery).
- [3.6 Components, accessibility and responsive behavior](../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T023 | Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing. | [T023 — Implement current-impact archive, restore and deletion](tasks/T023-implement-current-impact-archive-restore-and-deletion.md) |
| T024 | Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content. | [T024 — Connect private archives and irreversible confirmations](tasks/T024-connect-private-archives-and-irreversible-confirmations.md) |

## Dependencies

External inputs: [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T007 — Connect account and session browser journeys](../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md), [T012 — Serve precise public catalog and people reads](../07-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md), [T013 — Connect discovery, details and public people views](../07-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T017 — Implement authoritative movie creation and edits](../09-movie-management/tasks/T017-implement-authoritative-movie-creation-and-edits.md), [T018 — Connect movie editors and confirmed outcomes](../09-movie-management/tasks/T018-connect-movie-editors-and-confirmed-outcomes.md), [T019 — Implement shared person writes and identity](../10-people-and-credits/tasks/T019-implement-shared-person-writes-and-identity.md), [T020 — Connect people management and retained edits](../10-people-and-credits/tasks/T020-connect-people-management-and-retained-edits.md), [T021 — Implement ordered credits and custom contribution roles](../10-people-and-credits/tasks/T021-implement-ordered-credits-and-custom-contribution-roles.md), [T022 — Connect credit editing and verify the people outcome](../10-people-and-credits/tasks/T022-connect-credit-editing-and-verify-the-people-outcome.md), [T026 — Coordinate protective holds, attachment and cleanup](../11-artwork-management/tasks/T026-coordinate-protective-holds-attachment-and-cleanup.md). Consume their concrete outputs as stated in member prerequisites.

- T023 starts after T008, T010, T012, T017, T019, T021, T026; its task definition owns the specific files/contracts and acceptance boundary.
- T024 starts after T004, T007, T013, T018, T020, T022, T023; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T024 — Connect private archives and irreversible confirmations](tasks/T024-connect-private-archives-and-irreversible-confirmations.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T023: Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing.
- T023: Impact: a new/removed credit or changed title after preview invalidates the earlier person deletion confirmation; all TV effects remain represented.
- T023: Survival: restore recreates no removed credit/person/title, permanent deletion preserves unrelated titles/people/referenced artwork and records one attributable result.
- T024: Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content.
- T024: Confirmation: stale impact requires fresh review, cancel preserves all data, and permanent delete leaves no restore action.
- T024: Permutations: archive/restore either side shows only surviving both-active credits, and stale receipt replay never recreates a deleted record. T035 owns the separate seed-rerun preservation proof (ATDD-112/198).

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
