---
kind: group
id: G17
tasks:
- T037
- T038
integration_owner: T038
---

# 17-operational-evidence

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug. Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

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
| T037 | Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug. | [T037 — Emit bounded private-safe JSON diagnostics](tasks/T037-emit-bounded-private-safe-json-diagnostics.md) |
| T038 | Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard. | [T038 — Expose logical metrics, dashboards and alert episodes](tasks/T038-expose-logical-metrics-dashboards-and-alert-episodes.md) |

## Dependencies

External inputs: [T002 — Generate and validate the shared wire contracts](../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md), [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T011 — Run fenced recovery and bounded retries](../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md), [T025 — Accept and process bounded artwork streams](../11-artwork-management/tasks/T025-accept-and-process-bounded-artwork-streams.md), [T026 — Coordinate protective holds, attachment and cleanup](../11-artwork-management/tasks/T026-coordinate-protective-holds-attachment-and-cleanup.md), [T029 — Implement isolated MCP transport and connector identity](../13-assisted-movie-creation/tasks/T029-implement-isolated-mcp-transport-and-connector-identity.md), [T036 — Record actionable alerts through one selected mode](../16-operational-notifications/tasks/T036-record-actionable-alerts-through-one-selected-mode.md). Consume their concrete outputs as stated in member prerequisites.

- T037 starts after T002, T003, T008, T011, T029, T026; its task definition owns the specific files/contracts and acceptance boundary.
- T038 starts after T011, T025, T026, T029, T036, T037; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T038 — Expose logical metrics, dashboards and alert episodes](tasks/T038-expose-logical-metrics-dashboards-and-alert-episodes.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T037: Format: every selected application-owned physical line parses independently with exact bounded schema; no banner/plain exception/private canary escapes at info or debug.
- T037: Context: concurrent/cancelled requests cannot inherit each other's identity, and restarted work has fresh trace with retained safe operation link.
- T037: Failure: full/unavailable sink remains bounded, exposes truthful drop/failure signals and does not fabricate rollback or corrupt MCP protocol output.
- T038: Observability: a failed GraphQL domain action under HTTP200 increments its actual outcome/error family and appears meaningfully on the dashboard.
- T038: Alert: one episode routes one sanitized durable event, repeated signals deduplicate, recovery resets appropriately, and receiver outage cannot be labeled delivered.
- T038: Privacy: no secret/account/operation/high-cardinality input label appears and private exporters are not browser/MCP tools.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
