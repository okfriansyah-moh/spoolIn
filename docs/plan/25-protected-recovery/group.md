---
kind: group
id: G25
feature: protected-recovery
tasks:
- T040
- T041
integration_owner: T041
---

# 25-protected-recovery

[Overall plan](../../06-PLAN.md) · [Numbering history](../../plan-numbering-v1.md)

## Outcome and scope

Continuity: existing and new account/draft access works through rotation with correct values and no plaintext leakage. Migration: drift/partial incompatible schema blocks readiness with diagnostic cause, and rollback does not pretend to undo destructive committed change.

This stage belongs to **protected-recovery**. It owns only the member tasks below. Other stages of this feature retain their separate prerequisites and proof.

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

External inputs: [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T005 — Protect account identities and credential storage](../03-account-access-credential-storage/tasks/T005-protect-account-identities-and-credential-storage.md), [T008 — Implement registration and authoritative sessions](../06-account-access-registration-and-sessions/tasks/T008-implement-registration-and-authoritative-sessions.md), [T011 — Run fenced recovery and bounded retries](../09-reliable-work-recovery-and-retries/tasks/T011-run-fenced-recovery-and-bounded-retries.md), [T014 — Persist encrypted drafts with durable save and discard receipts](../11-draft-recovery-storage-and-controllers/tasks/T014-persist-encrypted-drafts-with-durable-save-and-discard-receipts.md), [T023 — Coordinate protective holds, attachment and cleanup](../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md), [T024 — Implement current-impact archive, restore and deletion](../15-archive-management/tasks/T024-implement-current-impact-archive-restore-and-deletion.md), [T033 — Initialize and replay seed and prepared accounts safely](../20-reproducible-seed/tasks/T033-initialize-and-replay-seed-and-prepared-accounts-safely.md), [T037 — Implement the seven reproducible lifecycle commands](../23-local-operations/tasks/T037-implement-the-seven-reproducible-lifecycle-commands.md).

- T040 starts after T005, T008, T014, T033.
- T041 starts after T003, T011, T024, T023, T033, T037, T040.

Complete earlier stages first. Explicit task dependencies retain their original obligations; numbering does not waive checks or authorize concurrent writers.

## Integrated acceptance

[T041 — Verify migrations, purge and coordinated restore](tasks/T041-verify-migrations-purge-and-coordinated-restore.md) owns this stage's combined verdict. Require all members Done with their assigned current checks and independent review. Later feature and release proof remains with its mapped owner; it does not reopen accepted contributor work.

- T040: Continuity: existing and new account/draft access works through rotation with correct values and no plaintext leakage.
- T040: Safety: interrupted backfill resumes without duplicate normalized identities or overwritten concurrent edits; old key cannot retire while needed.
- T040: Failure: missing/tampered protection yields safe failure without blank-value overwrite, alias corruption or new nonce reuse.
- T041: Migration: drift/partial incompatible schema blocks readiness with diagnostic cause, and rollback does not pretend to undo destructive committed change.
- T041: Purge: expired content is denied before cleanup while protected holds/unresolved history/permanent ledgers remain; cleanup cannot enable replay or remove referenced output.
- T041: Restore: independently restored data/artwork/keys agree, old credentials fail, fresh writes use new keys/counters, and source installation remains untouched.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
