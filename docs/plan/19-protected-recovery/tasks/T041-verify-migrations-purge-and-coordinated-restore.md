---
kind: task
id: T041
group: G19
depends_on:
- T003
- T011
- T023
- T026
- T035
- T039
- T040
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T041 — Verify migrations, purge and coordinated restore

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Migration: drift/partial incompatible schema blocks readiness with diagnostic cause, and rollback does not pretend to undo destructive committed change. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-198 | verifies |
| ATDD-200 | both |
| ATDD-201 | both |
| ATDD-208 | both |
| ATDD-209 | both |
| ATDD-210 | both |
| ATDD-310 | both |
| SC-03 | both |
| FR-005 | both |
| FR-008 | both |
| NFR-003 | both |
| D24 | both |
| AC-138 | both |
| AC-138-C01 | both |
| AC-138-C02 | both |
| AC-071 | both |
| AC-071-C01 | both |
| G-03 | both |
| AC-035 | both |
| AC-035-C01 | both |
| AC-035-C02 | both |
| OD-004 | both |
| TD-C-056 | both |
| TD-C-061 | both |
| TD-ROW-L6330 | both |
| TD-ROW-L6331 | both |
| TD-ROW-L6332 | both |
| TD-ROW-L6333 | both |
| TD-ROW-L6334 | both |
| TD-ROW-L6335 | both |
| TD-ROW-L6336 | both |
| TD-ROW-L6337 | both |
| F-EXPIRY-session-idle | both |
| F-EXPIRY-session-absolute | both |
| F-EXPIRY-connector | both |
| F-EXPIRY-draft | both |
| F-EXPIRY-cursor | both |
| F-EXPIRY-impact | both |
| F-EXPIRY-proposal | both |
| F-EXPIRY-challenge | both |
| F-EXPIRY-approval | both |
| BRAIN-D24 | both |

## Prerequisites

- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- [T011 — Run fenced recovery and bounded retries](../../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lease: a stalled worker cannot renew/finish after replacement, and a late effect is reconciled without duplication.
- [T023 — Implement current-impact archive, restore and deletion](../../12-archive-management/tasks/T023-implement-current-impact-archive-restore-and-deletion.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing.
- [T026 — Coordinate protective holds, attachment and cleanup](../../11-artwork-management/tasks/T026-coordinate-protective-holds-attachment-and-cleanup.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.
- [T035 — Initialize and replay seed and prepared accounts safely](../../15-reproducible-seed/tasks/T035-initialize-and-replay-seed-and-prepared-accounts-safely.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles.
- [T039 — Implement the seven reproducible lifecycle commands](../../18-local-operations/tasks/T039-implement-the-seven-reproducible-lifecycle-commands.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, One command: documented prerequisites plus make start yield usable local seeded app and operational entry points without manual migrate/seed/start steps.
- [T040 — Rotate keys and aliases without losing access](T040-rotate-keys-and-aliases-without-losing-access.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Continuity: existing and new account/draft access works through rotation with correct values and no plaintext leakage.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | ops/bin/backup.sh | T041 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/restore.sh | T041 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/maintenance.sh | T041 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/migration/ | T041 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/operations/restore/ | T041 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [7.5 Additional consequential native operations](../../../04-spoolin-techdoc.md#75-additional-consequential-native-operations): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [10.2 Flyway and schema evolution](../../../04-spoolin-techdoc.md#102-flyway-and-schema-evolution): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.3 Retention, purge and backup runbooks](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [SC-03 · L102](../../../02-PRD.md#4-goals-and-success-criteria) | Preserve user decisions \| Edits, archive state, and permanent deletions survive ordinary seed reruns; saved data/artwork survive restarts \| Unverified; Committed |
| [FR-005 · L239](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-008 · L242](../../../02-PRD.md#61-functional-requirements) | Supply the reproducible 100-movie/100-series initial catalog; preserve later movie/person edits, archive states, permanent deletions, and removed credits on reruns \| D24 Committed; rerun policy Committed (section 13.1) \| AC-033 through AC-037, AC-138, AC-158 |
| [NFR-003 · L261](../../../02-PRD.md#62-quality-and-delivery-requirements) | Saved data/artwork persist through restarts; committed persistence safeguards remain \| D18/D19 Committed \| AC-069 through AC-071 |
| [D24 · L325](../../../02-PRD.md#64-project-decision-register) | 100 distinct movies + 100 distinct TV series, variety, reproducible safe reruns \| FR-008 / AC-033 through AC-037 |
| [AC-138 · L400](../../../02-PRD.md#us-004-maintain-people-and-credits) | Seed rerun after person changes \| Seeded people were edited, archived, or permanently deleted and their credits changed or were removed \| Ordinary seeding repeats or resumes after interruption \| Preserve edits, archive state, permanent deletion, and removed credits; do not reactivate/recreate people or reinstate relationships. Later person/movie restoration still obeys those decisions, with no duplicate people or credits. |
| [AC-138-C01 · L400](../../../02-PRD.md#us-004-maintain-people-and-credits) | Preserve edits, archive state, permanent deletion, and removed credits; do not reactivate/recreate people or reinstate relationships. |
| [AC-138-C02 · L400](../../../02-PRD.md#us-004-maintain-people-and-credits) | Later person/movie restoration still obeys those decisions, with no duplicate people or credits. |
| [AC-071 · L473](../../../02-PRD.md#us-012-preserve-saved-work) | Restart persistence \| Saved records, archives, artwork, audit, and pending/failed work exist \| The stack stops/starts or restarts \| Those outcomes remain available; recovery continues within its retained budget and no normal command silently resets data. |
| [AC-071-C01 · L473](../../../02-PRD.md#us-012-preserve-saved-work) | Those outcomes remain available; recovery continues within its retained budget and no normal command silently resets data. |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [AC-035 · L961](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Preserve user decisions \| Seeded movies or people were edited, archived, or permanently deleted, credits were removed, and user records were added \| Ordinary seeding runs again \| Preserve those decisions and user-created records; do not recreate deleted movies/people, reactivate archived records, reinstate removed credits, or duplicate titles/people/genres/credits. The original 100/100 target is not a later catalog cap or reset instruction. |
| [AC-035-C01 · L961](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | Preserve those decisions and user-created records; do not recreate deleted movies/people, reactivate archived records, reinstate removed credits, or duplicate titles/people/genres/credits. |
| [AC-035-C02 · L961](../../../02-PRD.md#us-006-start-with-a-reproducible-catalog) | The original 100/100 target is not a later catalog cap or reset instruction. |
| [OD-004 · L1020](../../../02-PRD.md#131-resolved-product-decisions) | Movies and people require archiving before separately confirmed permanent deletion. Person archiving retains credits privately; restoration reveals only surviving credits on active titles. Deletion preserves associated titles and movie deletion preserves shared people. \| FR-004/FR-005/FR-006; AC-019/AC-024/AC-108 through AC-110, AC-129 through AC-138 |
| [TD-C-056 · L6000](../../../04-spoolin-techdoc.md#102-flyway-and-schema-evolution) | Serialized owner Flyway immutable versioned migrations, history/role/grant order, checksum drift failure,120s lock bound, no auto repair, real empty/upgrade/nontransactional/backfill/restore rehearsal and schema supported-range readiness. |
| [TD-C-061 · L6316](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks) | Bounded owner retention with unresolved dependencies and permanent ledgers; coordinated quiesced encrypted three-DB/artwork/key backup and new-key isolated restore drill, original-writer stop, truthful RPO/RTO and shutdown/rollback semantics. |
| [TD-ROW-L6330 · L6330](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks) | Startup migration/seed fails \| Read stage/code/hash; compare history and frozen manifest; fix config/input, rerun same identity; no automatic repair/reset \| Correct history, exact initial counts or truthful retained catalog receipt |
| [TD-ROW-L6331 · L6331](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks) | Media unavailable \| Keep metadata serving; inspect queue/disk/processor; restart Media, reconcile reserved holds/receipts \| Old artwork safe; no duplicate attach; pending outcome resolved |
| [TD-ROW-L6332 · L6332](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks) | Mutation uncertain \| Query owner operation using same UUID; if pending inspect durable work, if expired retain no-repeat result \| Terminal receipt or explicit unresolved status; never generate replacement identity blindly |
| [TD-ROW-L6333 · L6333](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks) | Deadlock/slow DB \| Inspect sanitized locks/long transactions and EXPLAIN in isolated copy; respect lock order and bounded retry \| No stale overwrites; latency/plan evidence |
| [TD-ROW-L6334 · L6334](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks) | Key failure \| Stop protected writes, validate mount/keyID/AAD; restore correct read key, never plaintext fallback \| Canary + sampled decrypt + nonce-safe new WRITE key |
| [TD-ROW-L6335 · L6335](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks) | Disk low \| Stop new uploads before critical; inspect protected/orphan volumes and retention; expand/free safe logs only \| DB WAL and protected assets intact, free headroom verified |
| [TD-ROW-L6336 · L6336](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks) | Notification unknown/fails \| Inspect ledger/attempt/provider receipt; repair selected adapter; controlled replay only when safe \| Recorded versus accepted/delivered accurately labeled |
| [TD-ROW-L6337 · L6337](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks) | Tunnel/client unavailable \| Inspect outbound connection/permissions/tunnel identity and client registration; no public-port workaround \| Independent discovery + exact-content approval tests per client |
| [BRAIN-D24 · L93](../../../01-brainstorming-result.md#4-decision-register) | D24 \| Committed \| Seed an initial catalog of 100 distinct movies and 100 distinct TV series, including their metadata; each title type must span multiple release years and months; seeding must be reproducible and safe to rerun |

## Implementation

1. Implement owner-scoped bounded retention respecting unresolved work/holds, permanent initialization/deletion/receipt evidence and current references; expiry denies eligibility independently of later physical purge.
2. Exercise immutable migration checksums, interruption, transactional/nontransactional recovery, supported schema range and expand/backfill/contract rollout/rollback boundaries; never auto-repair drift.
3. Quiesce writers/workers and back up all three DBs, private artwork, manifest and protected key material as one coordinated encrypted recovery set with checksums/custody metadata.
4. Restore only in a distinct isolated namespace with original writers stopped. Revoke restored sessions/connectors and provision fresh WRITE keys/nonce ranges before new encryption; retain old read keys only for needed data.
5. Verify restored relationships/attachments/receipts/pending work and no repeated effects, then record measured restore/recovery results and limits. These tools never operate on normal state during tests.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T041-A1:** Migration: drift/partial incompatible schema blocks readiness with diagnostic cause, and rollback does not pretend to undo destructive committed change.
- **T041-A2:** Purge: expired content is denied before cleanup while protected holds/unresolved history/permanent ledgers remain; cleanup cannot enable replay or remove referenced output.
- **T041-A3:** Restore: independently restored data/artwork/keys agree, old credentials fail, fresh writes use new keys/counters, and source installation remains untouched.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-198](../../../05-atdd.md#case-atdd-198-verify-receipts-without-resetting-user-decisions) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §10.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then ordinary reruns never upsert frozen content over current rows, credentials, keys or artwork And lifetime seed/source tombstones prevent resurrection even when domain rows are absent And optional artwork initialization uses the normal protective hold protocol and reports its phase separately Observation: Record current domain rows/tombstones/accounts/assets before actual identical-manifest rerun. Compare every edited/archived/deleted/removed/manual value afterward and inspect resumed Media hold phases; a completed receipt is not permission to upsert or resurrect. Verify permanent typed initialization receipt and seed_member/provenance_record removal ledgers after seven-day result cleanup and90 day resolved-history cleanup. Neither completed receipt nor absent domain row permits resetting current data; optional Media asset processing/reference phases remain separate. |
| [ATDD-200](../../../05-atdd.md#case-atdd-200-block-readiness-on-migration-drift-or-interruption) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §10.2 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then owner-only Flyway and installation locks serialize within120s and applied checksums remain immutable And transactional failures roll back, incomplete nontransactional indexes require inspection and deliberate repair And startup rejects partial/drifted schema without automatic repair or runtimeDDL Observation: Run actual Flyway with isolated owner migrators from empty/previous schemas, concurrent runners, checksum drift and killed nontransactional index creation. Inspect history/locks/DDL state and readiness exit; repairs require deliberate operator action, never automatic masking. |
| [ATDD-201](../../../05-atdd.md#case-atdd-201-preserve-compatibility-during-backfill-and-rollback) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §10.2 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then compatible readers tolerate additive fields and resumes verify counts/checksums without duplicate/lost values And supported schema min/max gates readiness before contract removal And contraction waits for rollback/backup horizon; destructive rollback requires coordinated restore and acknowledged loss of later writes Observation: Exercise actual old/new schema and generated clients within declared compatibility bounds, interrupt 100-row keyset backfill and inspect counts/checksums. Check binary readiness gates and rollback horizon; unsupported destructive rollback requires coordinated restore evidence. |
| [ATDD-208](../../../05-atdd.md#case-atdd-208-purge-eligible-data-without-erasing-recovery-evidence) / System | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §12.3 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then bounded100-row expiry andchild-before parent purge preserve unresolved work/notifications/key backup references And lifetime operation/seed/provenance/Catalogaudit/approval identity safeguards survive Observation: Run actual bounded purge jobs with controlled owner clock and concurrent locked draft save. Inspect eligible children before parents and preserved unresolved work/notifications/key-backup/lifetime ledgers; redacted proposal content cannot be represented as still readable. REQUEST result-body cleanup excludes ACCOUNT_INITIALIZATION/CATALOG_INITIALIZATION typed columns. Preserve catalog_audit_event, seed/provenance ledgers and movie_proposal approval/consumption identity for installation lifetime. Keep unresolved/exhausted/unknown work and operational_event phases; purge matching STARTED/FINISHED together only when parent eligible. Retain notification_summary90days, clear eligible complete asset processing block before parent-work purge, and preserve output/provenance/asset_reference. Eligible proposal redaction uses content{} plus fresh nonusable challenge digest with original hash/approval evidence intact; no expired consent or erased-content reconstruction follows. |
| [ATDD-209](../../../05-atdd.md#case-atdd-209-capture-all-protected-recovery-components-together) / System | F-CRYPTO: independent synthetic PII/DRAFT/EMAIL_LOOKUP epochs old/new, 100+1 eligible rows, distinct canaries, committed nonce counters and isolated backup checkpoint. Never capture raw key material. | Then the checkpoint reconciles database references with actual artwork and required read keys And checksum/version/permission checks detect missing or changed components Observation: Create an actual isolated coordinated checkpoint and compare dump/artwork/roles/key/version checksum manifest after restore. Exercise missing/tampered components and protected decryption/reference reconciliation. Record physical custody and measured recovery time/data horizon, not assumed RPO/RTO. |
| [ATDD-210](../../../05-atdd.md#case-atdd-210-resume-restored-protection-only-with-fresh-write-keys) / System | F-CRYPTO: independent synthetic PII/DRAFT/EMAIL_LOOKUP epochs old/new, 100+1 eligible rows, distinct canaries, committed nonce counters and isolated backup checkpoint. Never capture raw key material. | Then old counter cannot encrypt underold key andnew writes use fresh purpose key versions And restored sessions/connectors fail after epoch/secret rotation while authorized data/aliases/tombstones/artwork reconcile And missing key makes encrypted data unrecoverable withsafe diagnostics, never invented account values Observation: Restore old counters/keys into an isolated stopped installation, then attempt protected writes before/after fresh key epochs and credential invalidation. Inspect no old-key encryption, authorized READ-only decryption and reconciled tombstones/assets; omitted read keys fail truthfully. |
| [ATDD-310](../../../05-atdd.md#case-atdd-310-deny-expired-access-before-physical-purge) / Integration | F-EXPIRY each sessionidle/absolute/connector/draft/cursor/impact/challenge/approvalT atT−1 microsecond/T/T+1; retain row afterT; actorassertion separate2 s skew. | Then valid matching access is eligible only before all ordinary cutoffs And at/beyond cutoff access/action is denied without protected effects even before purge And expiry does not erase receipt/hold/tombstone safeguards or relax signed-actor verification Observation: Record API/gRPC result and unchanged domain state with actual owner clock/read boundary; inspect row presence to distinguish logical expiry from cleanup. Recheck saved-draft purge against concurrent refreshed expiry. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-EXPIRY-session-idle · L176](../../../05-atdd.md#f-expiry-eligibility-versus-purge) | session-idle uses 30 minutes cutoff T: independently assert eligible T-1 microsecond, denied T and T+1 microsecond with expired row retained. No ordinary skew grace; purge cannot extend access or erase recovery safeguards. |
| [F-EXPIRY-session-absolute · L176](../../../05-atdd.md#f-expiry-eligibility-versus-purge) | session-absolute uses 12 hours cutoff T: independently assert eligible T-1 microsecond, denied T and T+1 microsecond with expired row retained. No ordinary skew grace; purge cannot extend access or erase recovery safeguards. |
| [F-EXPIRY-connector · L176](../../../05-atdd.md#f-expiry-eligibility-versus-purge) | connector uses at most 7 days cutoff T: independently assert eligible T-1 microsecond, denied T and T+1 microsecond with expired row retained. No ordinary skew grace; purge cannot extend access or erase recovery safeguards. |
| [F-EXPIRY-draft · L176](../../../05-atdd.md#f-expiry-eligibility-versus-purge) | draft uses 24 hours cutoff T: independently assert eligible T-1 microsecond, denied T and T+1 microsecond with expired row retained. No ordinary skew grace; purge cannot extend access or erase recovery safeguards. |
| [F-EXPIRY-cursor · L176](../../../05-atdd.md#f-expiry-eligibility-versus-purge) | cursor uses 15 minutes cutoff T: independently assert eligible T-1 microsecond, denied T and T+1 microsecond with expired row retained. No ordinary skew grace; purge cannot extend access or erase recovery safeguards. |
| [F-EXPIRY-impact · L176](../../../05-atdd.md#f-expiry-eligibility-versus-purge) | impact uses 5 minutes cutoff T: independently assert eligible T-1 microsecond, denied T and T+1 microsecond with expired row retained. No ordinary skew grace; purge cannot extend access or erase recovery safeguards. |
| [F-EXPIRY-proposal · L176](../../../05-atdd.md#f-expiry-eligibility-versus-purge) | proposal uses 10 minutes cutoff T: independently assert eligible T-1 microsecond, denied T and T+1 microsecond with expired row retained. No ordinary skew grace; purge cannot extend access or erase recovery safeguards. |
| [F-EXPIRY-challenge · L176](../../../05-atdd.md#f-expiry-eligibility-versus-purge) | challenge uses 10 minutes cutoff T: independently assert eligible T-1 microsecond, denied T and T+1 microsecond with expired row retained. No ordinary skew grace; purge cannot extend access or erase recovery safeguards. |
| [F-EXPIRY-approval · L176](../../../05-atdd.md#f-expiry-eligibility-versus-purge) | approval uses 2 minutes cutoff T: independently assert eligible T-1 microsecond, denied T and T+1 microsecond with expired row retained. No ordinary skew grace; purge cannot extend access or erase recovery safeguards. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| boundary-tests | `bash ops/bin/test-integration.sh --task T041` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

This task’s executable proof is the declared integration/system/browser procedure; it has no separately required unit selector. Earlier rule/component owners supply their local evidence. Verify this task at its actual boundary instead of inventing a unit suite that mirrors it.

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| boundary-tests | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
