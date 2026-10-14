---
kind: group
id: G18
tasks:
- T039
integration_owner: T039
---

# 18-local-operations

[Overall plan](../../06-PLAN.md)

## Outcome and scope

One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [11.1 Configuration contract](../../04-spoolin-techdoc.md#111-configuration-contract).
- [11.2 Startup DAG and readiness](../../04-spoolin-techdoc.md#112-startup-dag-and-readiness).
- [11.3 All seven Make command contracts](../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts).
- [12.1 Database access and resource budgets](../../04-spoolin-techdoc.md#121-database-access-and-resource-budgets).

## Task list

| Task | Result | Detail |
|---|---|---|
| T039 | One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps. | [T039 — Implement the seven reproducible lifecycle commands](tasks/T039-implement-the-seven-reproducible-lifecycle-commands.md) |

## Dependencies

External inputs: [T001 — Lock the build and isolated verification harness](../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md), [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T007 — Connect account and session browser journeys](../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md), [T011 — Run fenced recovery and bounded retries](../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md), [T013 — Connect discovery, details and public people views](../07-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T018 — Connect movie editors and confirmed outcomes](../09-movie-management/tasks/T018-connect-movie-editors-and-confirmed-outcomes.md), [T020 — Connect people management and retained edits](../10-people-and-credits/tasks/T020-connect-people-management-and-retained-edits.md), [T022 — Connect credit editing and verify the people outcome](../10-people-and-credits/tasks/T022-connect-credit-editing-and-verify-the-people-outcome.md), [T024 — Connect private archives and irreversible confirmations](../12-archive-management/tasks/T024-connect-private-archives-and-irreversible-confirmations.md), [T027 — Connect measured progress and recover attachment outcomes](../11-artwork-management/tasks/T027-connect-measured-progress-and-recover-attachment-outcomes.md), [T028 — Expose complete read-only audit outcomes](../14-accountable-history/tasks/T028-expose-complete-read-only-audit-outcomes.md), [T029 — Implement isolated MCP transport and connector identity](../13-assisted-movie-creation/tasks/T029-implement-isolated-mcp-transport-and-connector-identity.md), [T031 — Build the trusted in-chat review resource](../13-assisted-movie-creation/tasks/T031-build-the-trusted-in-chat-review-resource.md), [T035 — Initialize and replay seed and prepared accounts safely](../15-reproducible-seed/tasks/T035-initialize-and-replay-seed-and-prepared-accounts-safely.md), [T036 — Record actionable alerts through one selected mode](../16-operational-notifications/tasks/T036-record-actionable-alerts-through-one-selected-mode.md), [T038 — Expose logical metrics, dashboards and alert episodes](../17-operational-evidence/tasks/T038-expose-logical-metrics-dashboards-and-alert-episodes.md). Consume their concrete outputs as stated in member prerequisites.

- T039 starts after T001, T003, T007, T011, T013, T018, T020, T022, T024, T027, T028, T029, T031, T035, T036, T038; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T039 — Implement the seven reproducible lifecycle commands](tasks/T039-implement-the-seven-reproducible-lifecycle-commands.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T039: One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps.
- T039: Persistence: stop/start/restart/forced termination preserve edited records, archives, keys, artwork, receipts and retained retry budget.
- T039: Truthful exits: failure at each stage returns its specified nonzero result; health/status never asserts actual client/provider approval or delivery.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
