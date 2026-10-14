---
kind: group
id: G06
tasks:
- T010
- T011
integration_owner: T011
---

# 05-reliable-work

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation. Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [6.2.2 Common operational tables](../../04-spoolin-techdoc.md#622-common-operational-tables).
- [6.2.3 Initialization receipt extensions](../../04-spoolin-techdoc.md#623-initialization-receipt-extensions).
- [7.1 Transaction inventory](../../04-spoolin-techdoc.md#71-transaction-inventory).
- [7.3 Idempotency and native worker claims](../../04-spoolin-techdoc.md#73-idempotency-and-native-worker-claims).
- [7.7 Durable work, retries and notifications](../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications).
- [12.3 Retention, purge and backup runbooks](../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks).

## Task list

| Task | Result | Detail |
|---|---|---|
| T010 | Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation. | [T010 — Implement owner receipts and atomic mutation evidence](tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md) |
| T011 | Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication. | [T011 — Run fenced recovery and bounded retries](tasks/T011-run-fenced-recovery-and-bounded-retries.md) |

## Dependencies

External inputs: [T002 — Generate and validate the shared wire contracts](../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md), [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md). Consume their concrete outputs as stated in member prerequisites.

- T010 starts after T002, T003, T009; its task definition owns the specific files/contracts and acceptance boundary.
- T011 starts after T010, T008; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T011 — Run fenced recovery and bounded retries](tasks/T011-run-fenced-recovery-and-bounded-retries.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T010: Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- T010: Crash: response loss after commit resolves the original result from a fresh reader; failure before commit leaves none of its success effects.
- T010: Retention: expired body produces the safe expired outcome and retains no authority to recreate a later-deleted record; initializer receipt is hidden and permanent.
- T011: Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication.
- T011: Budget: crash/restart preserves attempts and jitter/deadline; permanent failures never auto-retry and uncertainty never becomes presumed rollback.
- T011: Recovery: inspected exhaustion shows sanitized owner/operation/attempt/cause, and authorized recovery preserves original evidence and destination identity.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
