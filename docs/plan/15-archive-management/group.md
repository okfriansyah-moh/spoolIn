---
kind: group
id: G15
feature: archive-management
tasks:
- T024
- T025
integration_owner: T025
---

# 15-archive-management

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing. Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content.

This stage belongs to **archive-management**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

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
| T024 | Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing. | [T024 — Implement current-impact archive, restore and deletion](tasks/T024-implement-current-impact-archive-restore-and-deletion.md) |
| T025 | Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content. | [T025 — Connect private archives and irreversible confirmations](tasks/T025-connect-private-archives-and-irreversible-confirmations.md) |

## Dependencies

External inputs: [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T007 — Implement owner receipts and atomic mutation evidence](../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md), [T009 — Enforce scoped service identity and shared admission](../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md), [T010 — Connect account and session browser journeys](../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md), [T012 — Serve precise public catalog and people reads](../10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md), [T013 — Connect discovery, details and public people views](../10-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T016 — Implement authoritative movie creation and edits](../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md), [T017 — Connect movie editors and confirmed outcomes](../12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md), [T018 — Implement shared person writes and identity](../13-people-and-credits/tasks/T018-implement-shared-person-writes-and-identity.md), [T019 — Connect people management and retained edits](../13-people-and-credits/tasks/T019-connect-people-management-and-retained-edits.md), [T020 — Implement ordered credits and custom contribution roles](../13-people-and-credits/tasks/T020-implement-ordered-credits-and-custom-contribution-roles.md), [T021 — Connect credit editing and verify the people outcome](../13-people-and-credits/tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md), [T023 — Coordinate protective holds, attachment and cleanup](../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md).

- T024 starts after T009, T007, T012, T016, T018, T020, T023.
- T025 starts after T004, T010, T013, T017, T019, T021, T024.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T025 — Connect private archives and irreversible confirmations](tasks/T025-connect-private-archives-and-irreversible-confirmations.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T024: Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing.
- T024: Impact: a new/removed credit or changed title after preview invalidates the earlier person deletion confirmation; all TV effects remain represented.
- T024: Survival: restore recreates no removed credit/person/title, permanent deletion preserves unrelated titles/people/referenced artwork and records one attributable result.
- T025: Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content.
- T025: Confirmation: stale impact requires fresh review, cancel preserves all data, and permanent delete leaves no restore action.
- T025: Permutations: archive/restore either side shows only surviving both-active credits, and stale receipt replay never recreates a deleted record. T033 owns the separate seed-rerun preservation proof (ATDD-112/198).

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
