---
kind: task
id: T040
group: G19
depends_on:
- T005
- T006
- T014
- T035
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T040 — Rotate keys and aliases without losing access

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Continuity: existing and new account/draft access works through rotation with correct values and no plaintext leakage. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-098 | both |
| ATDD-102 | both |
| ATDD-182 | both |
| ATDD-183 | both |
| FR-011 | both |
| NFR-015 | both |
| AC-112 | both |
| AC-112-C01 | both |
| AC-112-C02 | both |
| AC-124 | both |
| AC-124-C01 | both |
| AC-128 | both |
| AC-128-C01 | both |
| TD-C-048 | both |
| V-CRYPTO | both |

## Prerequisites

- [T005 — Protect account identities and credential storage](../../03-account-access/tasks/T005-protect-account-identities-and-credential-storage.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Encryption: a DB-only read has no readable name/email/password; tampered AAD/tag/key gives a safe failure and leaves valid data unchanged.
- [T006 — Implement registration and authoritative sessions](../../03-account-access/tasks/T006-implement-registration-and-authoritative-sessions.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.
- [T014 — Persist encrypted drafts with durable save and discard receipts](../../08-draft-recovery/tasks/T014-persist-encrypted-drafts-with-durable-save-and-discard-receipts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation.
- [T035 — Initialize and replay seed and prepared accounts safely](../../15-reproducible-seed/tasks/T035-initialize-and-replay-seed-and-prepared-accounts-safely.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Fresh: two clean isolated installations have equivalent accepted identities/metadata and exact initial100/100 with usable prepared roles.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/crypto/rotation/ | T040 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | ops/bin/rotate-keys.sh | T040 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/integrationTest/kotlin/com/spoolin/core/recovery/rotation/ | T040 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/crypto/ | T040 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/crypto/rotation/ | T040 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [8.1 Authenticated encryption and key lifecycle](../../../04-spoolin-techdoc.md#81-authenticated-encryption-and-key-lifecycle): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [10.2 Flyway and schema evolution](../../../04-spoolin-techdoc.md#102-flyway-and-schema-evolution): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.3 Retention, purge and backup runbooks](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [FR-011 · L247](../../../02-PRD.md#61-functional-requirements) | Register immediate Viewer accounts, provide demo accounts, and support sign-in/sign-out \| D05 Committed; approved account scope Committed \| AC-047 through AC-049, AC-111, AC-112, AC-141/AC-142, AC-145/AC-146 |
| [NFR-015 · L275](../../../02-PRD.md#62-quality-and-delivery-requirements) | Protect account PII with the selected encryption/rotation constraint; passwords use one-way hashing \| Committed product policy (section 13.1) \| AC-123 through AC-125, AC-127, AC-128; section 10 |
| [AC-112 · L444](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Invalid/repeated registration \| A required field is missing, a registration value violates the effective policy in section 5.5, email is invalid/already used, or submission is repeated \| Registration is attempted \| Show safe corrective feedback, create no duplicate account, and do not expose whether another account contains private details. Never echo/store the password in recovery drafts or logs. |
| [AC-112-C01 · L444](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Show safe corrective feedback, create no duplicate account, and do not expose whether another account contains private details. |
| [AC-112-C02 · L444](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Never echo/store the password in recovery drafts or logs. |
| [AC-124 · L521](../../../02-PRD.md#us-020-protect-private-account-information) | Key rotation continuity \| Existing accounts contain protected data and rotation occurs \| Existing and new users use permitted account operations \| Authorized use continues with correct data; rotation does not expose plaintext, lose account information, or make old accounts inaccessible. |
| [AC-124-C01 · L521](../../../02-PRD.md#us-020-protect-private-account-information) | Authorized use continues with correct data; rotation does not expose plaintext, lose account information, or make old accounts inaccessible. |
| [AC-128 · L523](../../../02-PRD.md#us-020-protect-private-account-information) | Protection failure \| Encryption/decryption or required key access fails \| A protected operation is attempted \| Fail with non-sensitive feedback and operational evidence; never fall back to plaintext, invent missing account values, or overwrite valid protected data with an empty value. |
| [AC-128-C01 · L523](../../../02-PRD.md#us-020-protect-private-account-information) | Fail with non-sensitive feedback and operational evidence; never fall back to plaintext, invent missing account values, or overwrite valid protected data with an empty value. |
| [TD-C-048 · L5475](../../../04-spoolin-techdoc.md#81-authenticated-encryption-and-key-lifecycle) | AES256-GCM separate purpose keys/AAD/committed unique counters, tamper fail-closed, HMAC alias dual-write rotation, batches100/CAS, retained keys/backups; restore only new WRITE keys and revoked restored credentials. |
| [V-CRYPTO · L6400](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-CRYPTO \| Query DB/backup raw values, duplicate normalized email races, dual-alias rotation/backfill interruption, AEAD tamper/AAD/key loss, nonce allocation rollback/exhaustion, restore old counters with new WRITE keys, credential invalidation; assert no plaintext/duplicates/nonce reuse |

## Implementation

1. Implement purpose-key WRITE/READ/retirement transitions and dual-generation HMAC lookup alias lifecycle under source locks/CAS. Only Core owns protected material; operator files/backups remain outside the checkout.
2. Process re-encryption/backfill in bounded batches100 with resumable progress and current-row CAS; writes target current key while authorized reads use retained versions.
3. Coordinate concurrent registration/email uniqueness with dual aliases and restarts. Verify every eligible row/backed-up requirement before retirement; missing/tampered data blocks retirement rather than deleting evidence.
4. Use immutable audit/operational evidence and restricted rotate-keys command with redacted diagnostics. Never recycle nonce ranges or let failed batches corrupt protected rows.
5. Exercise real DB interruptions, alias races, failed decrypt, key removal and restoration-read scenarios; full coordinated restore/new-write keys belongs to T041.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T040-A1:** Continuity: existing and new account/draft access works through rotation with correct values and no plaintext leakage.
- **T040-A2:** Safety: interrupted backfill resumes without duplicate normalized identities or overwritten concurrent edits; old key cannot retire while needed.
- **T040-A3:** Failure: missing/tampered protection yields safe failure without blank-value overwrite, alias corruption or new nonce reuse.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-098](../../../05-atdd.md#case-atdd-098-key-rotation-continuity) / End-to-end | ExistingaccountoldPIIkey; newWRITEkey, concurrent reads/register andinterruptedbatchreencryption; accesscontinuous and old retirement blocked until safe. | Then Authorized use continues with correct data; rotation does not expose plaintext, lose account information, or make old accounts inaccessible. Observation: Interrupt real key/lookup backfill and resume with authorized old/new reads; inspect no lost values/duplicate account and retirement only after live/backup checks. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-102](../../../05-atdd.md#case-atdd-102-protection-failure) / End-to-end | Missingkey/wrongtag/wrongAAD/unknown version/decrypt failure; protected operation unavailable, sanitized operational code, existingcipher untouched/no empty fallback. | Then Fail with non-sensitive feedback and operational evidence; never fall back to plaintext, invent missing account values, or overwrite valid protected data with an empty value. Observation: Corrupt/miss actual encryption material and inspect safe unavailable result while original ciphertext stays intact; no plaintext fallback or invented empty profile. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-182](../../../05-atdd.md#case-atdd-182-resume-re-encryption-before-retiring-keys) / Integration | F-CRYPTO: independent synthetic PII/DRAFT/EMAIL_LOOKUP epochs old/new, 100+1 eligible rows, distinct canaries, committed nonce counters and isolated backup checkpoint. Never capture raw key material. | Then old and new authorized reads continue while CAS commits each re-encrypted row And restart resumes old-key rows without losing values or marking unfinished work complete And retirement waits for zero live references plus expired backups or tested retained key access Observation: Re-encrypt 101 real protected rows through READ→WRITE switch and 100-row CAS batches, interrupt midway and race an edit. Inspect remaining old-key rows, preserved plaintext after authorized decryption and key references; retirement requires live and backup checks. |
| [ATDD-183](../../../05-atdd.md#case-atdd-183-preserve-email-uniqueness-during-alias-backfill) / Integration | F-CRYPTO: independent synthetic PII/DRAFT/EMAIL_LOOKUP epochs old/new, 100+1 eligible rows, distinct canaries, committed nonce counters and isolated backup checkpoint. Never capture raw key material. | Then both compatible aliases are queried and inserted atomically until full coverage And old-account case variants cannot register a duplicate during the overlap And old lookup retirement waits for coverage and backup custody independently of PII encryption rotation Observation: Run alias backfill and concurrent registration against actual unique aliases while old/new coverage is partial. Inspect both inserted/query aliases and exactly one account for case variants; completion/retirement requires full coverage and retained backup keys. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T040` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T040` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer self-reviews first. A fresh independent reviewer then receives this task, inherited constraints, exact source clauses, baseline and resulting change, actual tests and sanitized evidence. Record distinct reviewer identities, reviewed revision, each finding as required/advisory, correction and recheck; labels alone do not establish independence. Scrutinize permission/secret boundaries, races, rollback/replay, lifetime evidence and the real owner boundary specified in this task. Implement → check → self-review → independent review → correct → rerun affected checks. Broaden testing only for a new concern. If no fresh reviewer is available, leave In review with exact next action; self-review cannot replace it.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| boundary-tests | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including self-review, fresh independent review and applicable visual/interaction checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T041 — Verify migrations, purge and coordinated restore](T041-verify-migrations-purge-and-coordinated-restore.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
