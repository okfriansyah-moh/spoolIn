---
kind: group
id: G07
tasks:
- T012
- T013
integration_owner: T013
---

# 07-catalog-discovery

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year. Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [3.3 Search, navigation and request state](../../04-spoolin-techdoc.md#33-search-navigation-and-request-state).
- [4.3 SpoolIn Catalog service](../../04-spoolin-techdoc.md#43-spoolin-catalog-service).
- [5.3 Operation, permission and deadline inventory](../../04-spoolin-techdoc.md#53-operation-permission-and-deadline-inventory).
- [6.2.5 Catalog tables](../../04-spoolin-techdoc.md#625-catalog-tables).
- [7.2 Search and date semantics](../../04-spoolin-techdoc.md#72-search-and-date-semantics).
- [7.4 Index and query-plan obligations](../../04-spoolin-techdoc.md#74-index-and-query-plan-obligations).
- [3.1 Routes, screens and API dependencies](../../04-spoolin-techdoc.md#31-routes-screens-and-api-dependencies).
- [3.1.1 UI reference and implementation map](../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map).
- [3.6 Components, accessibility and responsive behavior](../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T012 | Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year. | [T012 — Serve precise public catalog and people reads](tasks/T012-serve-precise-public-catalog-and-people-reads.md) |
| T013 | Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria. | [T013 — Connect discovery, details and public people views](tasks/T013-connect-discovery-details-and-public-people-views.md) |

## Dependencies

External inputs: [T002 — Generate and validate the shared wire contracts](../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md), [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T007 — Connect account and session browser journeys](../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md). Consume their concrete outputs as stated in member prerequisites.

- T012 starts after T002, T003, T008, T009, T010; its task definition owns the specific files/contracts and acceptance boundary.
- T013 starts after T004, T007, T012; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T013 — Connect discovery, details and public people views](tasks/T013-connect-discovery-details-and-public-people-views.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T012: Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year.
- T012: Visibility: archived person/title and retained credit do not leak through search, cursor or detail, while another independently matching title remains visible.
- T012: Identity: same-named records retain distinct IDs; invalid input is rejected without altered/truncated search and without read-side mutation.
- T013: Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria.
- T013: Details: selected UUID/type/metadata/provenance match the clicked record, same names never redirect identity, missing art preserves navigation and unavailable target leaks no archive data.
- T013: UI: both themes preserve visible content/actions across declared viewports; failure has retry, successful zero rows has an empty/no-match explanation.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
