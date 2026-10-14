---
kind: group
id: G19
tasks:
- T040
- T041
integration_owner: T041
---

# 19-protected-recovery

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Continuity: existing and new account/draft access works through rotation with correct values and no plaintext leakage. Migration: drift/partial incompatible schema blocks readiness with diagnostic cause, and rollback does not pretend to undo destructive committed change. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [8.1 Authenticated encryption and key lifecycle](../../04-spoolin-techdoc.md#81-authenticated-encryption-and-key-lifecycle).
- [10.2 Flyway and schema evolution](../../04-spoolin-techdoc.md#102-flyway-and-schema-evolution).
- [12.3 Retention, purge and backup runbooks](../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks).
- [7.5 Additional consequential native operations](../../04-spoolin-techdoc.md#75-additional-consequential-native-operations).

## Task list

| Task | Result | Detail |
|---|---|---|
| T040 | Continuity: existing and new account/draft access works through rotation with correct values and no plaintext leakage. | [T040 — Rotate keys and aliases without losing access](tasks/T040-rotate-keys-and-aliases-without-losing-access.md) |
| T041 | Migration: drift/partial incompatible schema blocks readiness with diagnostic cause, and rollback does not pretend to undo destructive committed change. | [T041 — Verify migrations, purge and coordinated restore](tasks/T041-verify-migrations-purge-and-coordinated-restore.md) |

## Dependencies

External inputs: [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T005 — Protect account identities and credential storage](../03-account-access/tasks/T005-protect-account-identities-and-credential-storage.md), [T006 — Implement registration and authoritative sessions](../03-account-access/tasks/T006-implement-registration-and-authoritative-sessions.md), [T011 — Run fenced recovery and bounded retries](../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md), [T014 — Persist encrypted drafts with durable save and discard receipts](../08-draft-recovery/tasks/T014-persist-encrypted-drafts-with-durable-save-and-discard-receipts.md), [T023 — Implement current-impact archive, restore and deletion](../12-archive-management/tasks/T023-implement-current-impact-archive-restore-and-deletion.md), [T026 — Coordinate protective holds, attachment and cleanup](../11-artwork-management/tasks/T026-coordinate-protective-holds-attachment-and-cleanup.md), [T035 — Initialize and replay seed and prepared accounts safely](../15-reproducible-seed/tasks/T035-initialize-and-replay-seed-and-prepared-accounts-safely.md), [T039 — Implement the seven reproducible lifecycle commands](../18-local-operations/tasks/T039-implement-the-seven-reproducible-lifecycle-commands.md). Consume their concrete outputs as stated in member prerequisites.

- T040 starts after T005, T006, T014, T035; its task definition owns the specific files/contracts and acceptance boundary.
- T041 starts after T003, T011, T023, T026, T035, T039, T040; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T041 — Verify migrations, purge and coordinated restore](tasks/T041-verify-migrations-purge-and-coordinated-restore.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T040: Continuity: existing and new account/draft access works through rotation with correct values and no plaintext leakage.
- T040: Safety: interrupted backfill resumes without duplicate normalized identities or overwritten concurrent edits; old key cannot retire while needed.
- T040: Failure: missing/tampered protection yields safe failure without blank-value overwrite, alias corruption or new nonce reuse.
- T041: Migration: drift/partial incompatible schema blocks readiness with diagnostic cause, and rollback does not pretend to undo destructive committed change.
- T041: Purge: expired content is denied before cleanup while protected holds/unresolved history/permanent ledgers remain; cleanup cannot enable replay or remove referenced output.
- T041: Restore: independently restored data/artwork/keys agree, old credentials fail, fresh writes use new keys/counters, and source installation remains untouched.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
