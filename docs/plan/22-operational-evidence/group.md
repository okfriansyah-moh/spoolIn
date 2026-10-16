---
kind: group
id: G22
feature: operational-evidence
tasks:
- T035
- T036
integration_owner: T036
---

# 22-operational-evidence

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug. Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard.

This stage belongs to **operational-evidence**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [12.2.1 JSON log contract](../../04-spoolin-techdoc.md#1221-json-log-contract).
- [11.1 Configuration contract](../../04-spoolin-techdoc.md#111-configuration-contract).
- [13.2 Additional boundary verification](../../04-spoolin-techdoc.md#132-additional-boundary-verification).
- [12.2 Metrics, alerts and privacy](../../04-spoolin-techdoc.md#122-metrics-alerts-and-privacy).
- [12.4 Metric contracts and alert expressions](../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions).
- [13.3 Verification locations and command selection](../../04-spoolin-techdoc.md#133-verification-locations-and-command-selection).

## Task list

| Task | Result | Detail |
|---|---|---|
| T035 | Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug. | [T035 — Emit bounded private-safe JSON diagnostics](tasks/T035-emit-bounded-private-safe-json-diagnostics.md) |
| T036 | Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard. | [T036 — Expose logical metrics, dashboards and alert episodes](tasks/T036-expose-logical-metrics-dashboards-and-alert-episodes.md) |

## Dependencies

External inputs: [T002 — Generate and validate the shared wire contracts](../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md), [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T009 — Enforce scoped service identity and shared admission](../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md), [T011 — Run fenced recovery and bounded retries](../09-reliable-work-recovery-and-retries/tasks/T011-run-fenced-recovery-and-bounded-retries.md), [T022 — Accept and process bounded artwork streams](../14-artwork-management-processing-and-attachment/tasks/T022-accept-and-process-bounded-artwork-streams.md), [T023 — Coordinate protective holds, attachment and cleanup](../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md), [T028 — Implement isolated MCP transport and connector identity](../18-assisted-movie-creation-transport-and-review/tasks/T028-implement-isolated-mcp-transport-and-connector-identity.md), [T034 — Record actionable alerts through one selected mode](../21-operational-notifications/tasks/T034-record-actionable-alerts-through-one-selected-mode.md).

- T035 starts after T002, T003, T009, T011, T028, T023.
- T036 starts after T011, T022, T023, T028, T034, T035.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T036 — Expose logical metrics, dashboards and alert episodes](tasks/T036-expose-logical-metrics-dashboards-and-alert-episodes.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T035: Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug.
- T035: Context: concurrent/cancelled requests cannot inherit each other's identity, and restarted work has fresh trace with retained safe operation link.
- T035: Failure: full/unavailable sink remains bounded, exposes truthful drop/failure signals and does not fabricate rollback or corrupt MCP protocol output.
- T036: Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard.
- T036: Alert: one episode routes one sanitized durable event, repeated signals deduplicate, recovery resets appropriately, and receiver outage cannot be labeled delivered.
- T036: Privacy: no secret/account/operation/high-cardinality input label appears and private exporters are not browser/MCP tools.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
