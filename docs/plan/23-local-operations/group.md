---
kind: group
id: G23
feature: local-operations
tasks:
- T037
integration_owner: T037
---

# 23-local-operations

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps.

This stage belongs to **local-operations**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [11.1 Configuration contract](../../04-spoolin-techdoc.md#111-configuration-contract).
- [11.2 Startup DAG and readiness](../../04-spoolin-techdoc.md#112-startup-dag-and-readiness).
- [11.3 All seven Make command contracts](../../04-spoolin-techdoc.md#113-all-seven-make-command-contracts).
- [12.1 Database access and resource budgets](../../04-spoolin-techdoc.md#121-database-access-and-resource-budgets).

## Task list

| Task | Result | Detail |
|---|---|---|
| T037 | One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps. | [T037 — Implement the seven reproducible lifecycle commands](tasks/T037-implement-the-seven-reproducible-lifecycle-commands.md) |

## Dependencies

External inputs: [T001 — Lock the build and isolated verification harness](../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md), [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T010 — Connect account and session browser journeys](../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md), [T011 — Run fenced recovery and bounded retries](../09-reliable-work-recovery-and-retries/tasks/T011-run-fenced-recovery-and-bounded-retries.md), [T013 — Connect discovery, details and public people views](../10-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md), [T017 — Connect movie editors and confirmed outcomes](../12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md), [T019 — Connect people management and retained edits](../13-people-and-credits/tasks/T019-connect-people-management-and-retained-edits.md), [T021 — Connect credit editing and verify the people outcome](../13-people-and-credits/tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md), [T025 — Connect private archives and irreversible confirmations](../15-archive-management/tasks/T025-connect-private-archives-and-irreversible-confirmations.md), [T026 — Connect measured progress and recover attachment outcomes](../16-artwork-management-browser-journeys/tasks/T026-connect-measured-progress-and-recover-attachment-outcomes.md), [T028 — Implement isolated MCP transport and connector identity](../18-assisted-movie-creation-transport-and-review/tasks/T028-implement-isolated-mcp-transport-and-connector-identity.md), [T030 — Build the trusted in-chat review resource](../18-assisted-movie-creation-transport-and-review/tasks/T030-build-the-trusted-in-chat-review-resource.md), [T031 — Expose complete read-only audit outcomes](../19-accountable-history/tasks/T031-expose-complete-read-only-audit-outcomes.md), [T033 — Initialize and replay seed and prepared accounts safely](../20-reproducible-seed/tasks/T033-initialize-and-replay-seed-and-prepared-accounts-safely.md), [T034 — Record actionable alerts through one selected mode](../21-operational-notifications/tasks/T034-record-actionable-alerts-through-one-selected-mode.md), [T036 — Expose logical metrics, dashboards and alert episodes](../22-operational-evidence/tasks/T036-expose-logical-metrics-dashboards-and-alert-episodes.md).

- T037 starts after T001, T003, T010, T011, T013, T017, T019, T021, T025, T026, T031, T028, T030, T033, T034, T036.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T037 — Implement the seven reproducible lifecycle commands](tasks/T037-implement-the-seven-reproducible-lifecycle-commands.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T037: One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps.
- T037: Persistence: stop/start/restart/forced termination preserve edited records, archives, keys, artwork, receipts and retained retry budget.
- T037: Truthful exits: failure at each stage returns its specified nonzero result; health/status never asserts actual client/provider approval or delivery.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
