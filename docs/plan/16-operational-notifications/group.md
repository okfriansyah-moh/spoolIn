---
kind: group
id: G16
tasks:
- T036
integration_owner: T036
---

# 16-operational-notifications

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Default: exhausted eligible work yields one inspectable sanitized recording and zero sends; database mode needs no external provider credential. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [6.2.2 Common operational tables](../../04-spoolin-techdoc.md#622-common-operational-tables).
- [7.7 Durable work, retries and notifications](../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications).
- [11.1 Configuration contract](../../04-spoolin-techdoc.md#111-configuration-contract).
- [12.4 Metric contracts and alert expressions](../../04-spoolin-techdoc.md#124-metric-contracts-and-alert-expressions).

## Task list

| Task | Result | Detail |
|---|---|---|
| T036 | Default: exhausted eligible work yields one inspectable sanitized recording and zero sends; database mode needs no external provider credential. | [T036 — Record actionable alerts through one selected mode](tasks/T036-record-actionable-alerts-through-one-selected-mode.md) |

## Dependencies

External inputs: [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md), [T011 — Run fenced recovery and bounded retries](../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md). Consume their concrete outputs as stated in member prerequisites.

- T036 starts after T003, T009, T010, T011; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T036 — Record actionable alerts through one selected mode](tasks/T036-record-actionable-alerts-through-one-selected-mode.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T036: Default: exhausted eligible work yields one inspectable sanitized recording and zero sends; database mode needs no external provider credential.
- T036: Failure: provider/receiver outage remains visible without rolling back successful business action or recursively notifying itself.
- T036: Ambiguity: lost provider response never becomes confirmed delivery or automatic duplicate send; only genuine selected-provider receipt supports external acceptance claims.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
