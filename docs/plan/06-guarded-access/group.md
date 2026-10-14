---
kind: group
id: G04
tasks:
- T008
integration_owner: T008
---

# 06-guarded-access

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [5.7 Internal contract enforcement and compatibility](../../04-spoolin-techdoc.md#57-internal-contract-enforcement-and-compatibility).
- [8.2 Threat model and assurance records](../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records).
- [8.4 Configurable rate admission](../../04-spoolin-techdoc.md#84-configurable-rate-admission).
- [8.5.4 Session and CSRF responsibility map](../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map).

## Task list

| Task | Result | Detail |
|---|---|---|
| T008 | Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority. | [T008 — Enforce scoped service identity and shared admission](tasks/T008-enforce-scoped-service-identity-and-shared-admission.md) |

## Dependencies

External inputs: [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T005 — Protect account identities and credential storage](../03-account-access/tasks/T005-protect-account-identities-and-credential-storage.md), [T006 — Implement registration and authoritative sessions](../03-account-access/tasks/T006-implement-registration-and-authoritative-sessions.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md). Consume their concrete outputs as stated in member prerequisites.

- T008 starts after T003, T005, T009, T010, T006; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T008 — Enforce scoped service identity and shared admission](tasks/T008-enforce-scoped-service-identity-and-shared-admission.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T008: Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- T008: Admission: at limit−1/limit/limit+1 under coordinated two-replica calls, admissions never exceed the shared selected quota and denied requests leave no new receipt/mutation.
- T008: Recovery: invalid zero/empty profile prevents readiness, limiter outage denies safely, and workers/probes retain their separately bounded authenticated path.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
