---
kind: task
id: T005
group: G03
depends_on:
- T002
- T003
readiness: Ready
status: In progress
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T005 — Protect account identities and credential storage

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Encryption: a DB-only read has no readable name/email/password; tampered AAD/tag/key gives a safe failure and leaves valid data unchanged. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-047 | implements |
| ATDD-085 | implements |
| ATDD-086 | implements |
| ATDD-099 | implements |
| ATDD-115 | implements |
| ATDD-120 | implements |
| ATDD-171 | implements |
| ATDD-172 | implements |
| ATDD-175 | implements |
| ATDD-176 | implements |
| ATDD-180 | implements |
| ATDD-181 | both |
| ATDD-228 | implements |
| ATDD-228-T1-R01 | implements |
| ATDD-228-T1-R02 | implements |
| ATDD-228-T1-R03 | implements |
| ATDD-228-T1-R04 | implements |
| ATDD-228-T1-R05 | implements |
| ATDD-228-T1-R06 | implements |
| ATDD-229 | implements |
| ATDD-229-T1-R01 | implements |
| ATDD-229-T1-R02 | implements |
| ATDD-229-T1-R03 | implements |
| ATDD-229-T1-R04 | implements |
| ATDD-229-T1-R05 | implements |
| ATDD-229-T1-R06 | implements |
| ATDD-229-T1-R07 | implements |
| ATDD-229-T1-R08 | implements |
| ATDD-229-T1-R09 | implements |
| ATDD-229-T1-R10 | implements |
| ATDD-230 | implements |
| ATDD-230-T1-R01 | implements |
| ATDD-230-T1-R02 | implements |
| ATDD-230-T1-R03 | implements |
| ATDD-230-T1-R04 | implements |
| ATDD-258 | implements |
| ATDD-277 | both |
| ATDD-296 | both |
| ATDD-297 | both |
| J-08 | implements |
| FR-011 | implements |
| FR-017 | implements |
| NFR-008 | both |
| NFR-015 | implements |
| D05 | implements |
| D07 | implements |
| D12 | implements |
| AC-047 | implements |
| AC-047-C01 | implements |
| AC-048 | implements |
| AC-048-C01 | implements |
| AC-048-C02 | implements |
| AC-111 | implements |
| AC-111-C01 | implements |
| AC-112 | implements |
| AC-112-C01 | implements |
| AC-112-C02 | implements |
| AC-125 | implements |
| AC-125-C01 | implements |
| AC-141 | implements |
| AC-141-C01 | implements |
| AC-141-C02 | implements |
| AC-141-C03 | implements |
| AC-146 | implements |
| AC-146-C01 | implements |
| AC-146-C02 | implements |
| AC-146-C03 | implements |
| AC-146-C04 | implements |
| G-03 | implements |
| G-05 | implements |
| OD-001 | implements |
| US-008 | implements |
| US-020 | implements |
| PRD-POLICY-14 | implements |
| PRD-BOUND-02 | implements |
| PRD-BOUND-08 | implements |
| PRD-BOUND-09 | implements |
| PRD-POLICY-21 | implements |
| PRD-POLICY-22 | implements |
| PRD-HANDOFF-11 | implements |
| PRD-HANDOFF-14 | implements |
| TD-C-031 | implements |
| ADR-04 | both |
| TD-ROW-L4056 | both |
| TD-ROW-L4057 | both |
| TD-ROW-L4058 | both |
| TD-ROW-L4059 | both |
| TD-ROW-L4060 | both |
| TD-ROW-L4061 | both |
| TD-ROW-L4099 | both |
| TD-ROW-L4100 | both |
| TD-ROW-L4101 | both |
| TD-ROW-L4102 | both |
| TD-ROW-L4103 | both |
| TD-ROW-L4104 | both |
| TD-ROW-L4105 | both |
| TD-ROW-L4106 | both |
| TD-ROW-L4107 | both |
| TD-ROW-L4108 | both |
| TD-ROW-L4150 | both |
| TD-ROW-L4151 | both |
| TD-ROW-L4152 | both |
| TD-ROW-L4153 | both |

## Prerequisites

- [T002 — Generate and validate the shared wire contracts](../../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.
- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/crypto/ | T005 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/account/persistence/ | T005 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/resources/db/migration/V001__account_protection.sql | T005 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/crypto/ | T005 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/integrationTest/kotlin/com/spoolin/core/recovery/crypto/ | T005 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/account/persistence/ | T005 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [6.2.4 Core tables](../../../04-spoolin-techdoc.md#624-core-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.1 Authenticated encryption and key lifecycle](../../../04-spoolin-techdoc.md#81-authenticated-encryption-and-key-lifecycle): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.3 API security enforcement](../../../04-spoolin-techdoc.md#83-api-security-enforcement): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [12.3 Retention, purge and backup runbooks](../../../04-spoolin-techdoc.md#123-retention-purge-and-backup-runbooks): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-228-T1-R01 · L9122](../../../05-atdd.md#case-atdd-228-enforce-cryptographic-key-registry-storage) | `id` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-228-T1-R02 · L9123](../../../05-atdd.md#case-atdd-228-enforce-cryptographic-key-registry-storage) | `purpose` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (purpose IN ('PII','DRAFT','EMAIL_LOOKUP')); no SQL default; owner supplies value` |
| [ATDD-228-T1-R03 · L9124](../../../05-atdd.md#case-atdd-228-enforce-cryptographic-key-registry-storage) | `state` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (state IN ('WRITE','READ','RETIRED')); no SQL default; owner supplies value` |
| [ATDD-228-T1-R04 · L9125](../../../05-atdd.md#case-atdd-228-enforce-cryptographic-key-registry-storage) | `next_nonce` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 1; explicit NULL fails23502; `NOT NULL DEFAULT 1 CHECK (next_nonce > 0); NOT NULL (explicit or PK)` |
| [ATDD-228-T1-R05 · L9126](../../../05-atdd.md#case-atdd-228-enforce-cryptographic-key-registry-storage) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-228-T1-R06 · L9127](../../../05-atdd.md#case-atdd-228-enforce-cryptographic-key-registry-storage) | `retire_after` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-229-T1-R01 · L9173](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-229-T1-R02 · L9174](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `pii_key_id` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES crypto_key_version(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-229-T1-R03 · L9175](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `pii_nonce` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 12 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(pii_nonce)=12); no SQL default; owner supplies value` |
| [ATDD-229-T1-R04 · L9176](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `pii_cipher` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 16, 65552 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(pii_cipher) BETWEEN 16 AND 65552); no SQL default; owner supplies value` |
| [ATDD-229-T1-R05 · L9177](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `password_hash` (`varchar(512)`); Source-valid technical token; 512 versus 513 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-229-T1-R06 · L9178](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `role` (`varchar(16)`); Source-valid technical token; 16 versus 17 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (role IN ('VIEWER','MAINTAINER')); no SQL default; owner supplies value` |
| [ATDD-229-T1-R07 · L9179](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `auth_epoch` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (auth_epoch >= 0); NOT NULL (explicit or PK)` |
| [ATDD-229-T1-R08 · L9180](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `version` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` |
| [ATDD-229-T1-R09 · L9181](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-229-T1-R10 · L9182](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage) | `updated_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-230-T1-R01 · L9228](../../../05-atdd.md#case-atdd-230-enforce-protected-email-lookup-storage) | `key_id` (`varchar(64)`); Source-valid technical token; 64 versus 65 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES crypto_key_version(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-230-T1-R02 · L9229](../../../05-atdd.md#case-atdd-230-enforce-protected-email-lookup-storage) | `digest` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(digest)=32); no SQL default; owner supplies value` |
| [ATDD-230-T1-R03 · L9230](../../../05-atdd.md#case-atdd-230-enforce-protected-email-lookup-storage) | `account_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-230-T1-R04 · L9231](../../../05-atdd.md#case-atdd-230-enforce-protected-email-lookup-storage) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [J-08 · L91](../../../02-PRD.md#32-application-journeys) | Visitor creates an account \| Name, email, and password available \| Register immediately as a Viewer, then sign in without email verification |
| [FR-011 · L245](../../../02-PRD.md#61-functional-requirements) | Register immediate Viewer accounts, provide demo accounts, and support sign-in/sign-out \| D05 Committed; approved account scope Committed \| AC-047 through AC-049, AC-111, AC-112, AC-141/AC-142, AC-145/AC-146 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-015 · L273](../../../02-PRD.md#62-quality-and-delivery-requirements) | Protect account PII with the selected encryption/rotation constraint; passwords use one-way hashing \| Committed product policy (section 13.1) \| AC-123 through AC-125, AC-127, AC-128; section 10 |
| [D05 · L306](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D07 · L308](../../../02-PRD.md#64-project-decision-register) | Viewer/Maintainer and prepared accounts \| Viewer/Maintainer roles and prepared accounts are adopted as Committed policy in section 13.1; FR-011/FR-012 / AC-049, AC-050, AC-111 |
| [D12 · L313](../../../02-PRD.md#64-project-decision-register) | Authentication mechanism and verified internal identity \| Product access settled; mechanics Open in techdoc. FR-011/FR-012 / AC-047 through AC-052 |
| [AC-047 · L435](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Sign in \| A prepared or registered account exists \| Correct or incorrect credentials are submitted \| Correct credentials permit sign-in with the assigned role; invalid credentials do not grant access and receive non-sensitive feedback. |
| [AC-047-C01 · L435](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Correct credentials permit sign-in with the assigned role; invalid credentials do not grant access and receive non-sensitive feedback. |
| [AC-048 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Expiry/sign-out \| A user signs out or their sign-in expires during work \| They attempt another restricted action \| Require sign-in again; public browsing remains available. Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-048-C01 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Require sign-in again; public browsing remains available. |
| [AC-048-C02 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-111 · L441](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Immediate registration \| A visitor supplies valid name, email, and password for a new account \| Registration succeeds \| Create a Viewer eligible to sign in immediately, without email verification; do not allow registration as Maintainer. |
| [AC-111-C01 · L441](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Create a Viewer eligible to sign in immediately, without email verification; do not allow registration as Maintainer. |
| [AC-112 · L442](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Invalid/repeated registration \| A required field is missing, a registration value violates the effective policy in section 5.5, email is invalid/already used, or submission is repeated \| Registration is attempted \| Show safe corrective feedback, create no duplicate account, and do not expose whether another account contains private details. Never echo/store the password in recovery drafts or logs. |
| [AC-112-C01 · L442](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Show safe corrective feedback, create no duplicate account, and do not expose whether another account contains private details. |
| [AC-112-C02 · L442](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Never echo/store the password in recovery drafts or logs. |
| [AC-125 · L520](../../../02-PRD.md#us-020-protect-private-account-information) | Password protection \| A user has registered or signed in \| Credential storage and outputs are checked by authorized release reviewers \| Passwords are protected by one-way password hashing, cannot be retrieved as an account feature, and never appear in readable storage, audit, logs, or AI payloads. |
| [AC-125-C01 · L520](../../../02-PRD.md#us-020-protect-private-account-information) | Passwords are protected by one-way password hashing, cannot be retrieved as an account feature, and never appear in readable storage, audit, logs, or AI payloads. |
| [AC-141 · L531](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Registration-name boundaries \| Registration-name defaults are configured and remaining registration data are valid \| A visitor submits a name of 2, 100, 1, or 101 code points after trimming \| Accept lengths 2 and 100 and create the Viewer on confirmed success. Reject lengths 1 and 101 without creating an account. Explain the configured length restriction without claiming the person's name is unreal or unauthentic. |
| [AC-141-C01 · L531](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Accept lengths 2 and 100 and create the Viewer on confirmed success. |
| [AC-141-C02 · L531](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject lengths 1 and 101 without creating an account. |
| [AC-141-C03 · L531](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Explain the configured length restriction without claiming the person's name is unreal or unauthentic. |
| [AC-146 · L536](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | New-password bounds and preservation \| Recommended password bounds of 15 to 128 are configured \| A visitor supplies passwords of 14, 15, 128, or 129 code points, including supported Unicode and whitespace \| Reject 14 and 129 for length; 15 and 128 pass the length check subject to the adopted credential policy under OD-001. Evaluate the exact entered password without trimming, transformation, or truncation. No password appears in recovery drafts, audit, or diagnostics. Successful sign-in later accepts that same credential under its account policy. |
| [AC-146-C01 · L536](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject 14 and 129 for length; 15 and 128 pass the length check subject to the adopted credential policy under OD-001. |
| [AC-146-C02 · L536](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Evaluate the exact entered password without trimming, transformation, or truncation. |
| [AC-146-C03 · L536](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | No password appears in recovery drafts, audit, or diagnostics. |
| [AC-146-C04 · L536](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Successful sign-in later accepts that same credential under its account policy. |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-05 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [OD-001 · L1042](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Product access resolved; authentication/session, expiry, account provisioning, credential-strength checks and adoption of Recommended password bounds, credential protection, and verified service/MCP identity remain for techdoc \| FR-011/FR-012/FR-017, NFR-015/NFR-016 \| Technical design owner unassigned; before identity-dependent implementation |
| [US-008 · L429](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | ### US-008: Register, sign in, and enforce access As a visitor, I want an account I can use immediately; as a signed-in user, I want clear permissions. Requirements: FR-011, FR-012, NFR-015. Ratings/reviews remain deferred, so Viewer registration does not unlock a new catalog-writing feature. |
| [US-020 · L512](../../../02-PRD.md#us-020-protect-private-account-information) | ### US-020: Protect private account information As an account holder, I want my private information protected while I can continue to use my account. Requirement: NFR-015. The encryption constraints are stated in section 10; the techdoc will define their implementation. |
| [PRD-POLICY-14 · L144](../../../02-PRD.md#53-accounts-audit-and-ai-boundaries) | Registration collects name, email, and password and immediately creates a Viewer. Prepared Viewer/Maintainer accounts support the demo. Users cannot choose Maintainer during registration. Sign-in and sign-out are included; public reading remains available after sign-out. Invalid registration/sign-in has understandable feedback without exposing credentials or another account's information. |
| [PRD-BOUND-02 · L169](../../../02-PRD.md#name-defaults) | Registration name \| 2 characters \| 100 characters \| Committed selected defaults: a one-character account name is excluded under the default configuration |
| [PRD-BOUND-08 · L184](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Registration email \| Valid, nonempty email \| 254 characters \| Length does not replace format or existing-account checks |
| [PRD-BOUND-09 · L185](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Registration password \| 15 characters \| 128 characters \| Apply to new passwords, not to the validity of an existing account's credential |
| [PRD-POLICY-21 · L188](../../../02-PRD.md#recommended-defaults-for-other-inputs) | The password recommendation follows OWASP authentication guidance for accounts without MFA. Exact email/URL syntax, credential-strength checks, and validation mechanisms remain technical-design work under OD-001/OD-003/OD-019. Other exposed fields, including numerical metadata and collection bounds where applicable, must receive documented limits in technical design; this does not add metadata fields or management screens to the release. |
| [PRD-POLICY-22 · L192](../../../02-PRD.md#validation-safe-handling-and-feedback) | Bounds are inclusive and independently configurable for each applicable field. Required fields remain required, and their effective minimum cannot allow an empty value. Measure text length consistently in Unicode code points. Trim surrounding whitespace from ordinary text before counting and validation while preserving internal spaces, legitimate punctuation, accents, and meaningful synopsis line breaks. Never trim, transform, or silently truncate a password. No field is silently shortened to fit a limit. |
| [PRD-HANDOFF-11 · L844](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | Account data protection requirements; NFR-015 \| Committed \| AES-256-GCM for recoverable account PII; versioned keys, authorized reads during rotation, safe re-encryption and retirement; one-way password hashing. Validate adaptation to this stack, backup/recovery implications, and safe failure behavior. |
| [PRD-HANDOFF-14 · L847](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D07/D08/D09 \| Product choices adopted only where stated \| Viewer/Maintainer/prepared accounts are selected; stale-save protection is selected. Queries/mutations without subscriptions and the specific optimistic-concurrency mechanism remain engineering recommendations. |
| [TD-C-031 · L4044](../../../04-spoolin-techdoc.md#624-core-tables) | Core key/account/alias/session/connector/draft/rate table dictionary and DDL: all types, keys, constraints/indexes/exposure/lifecycles retained; account identity separate from public person. |
| [ADR-04 · L6551](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-04 \| Server sessions and AEAD PII with separate lookup aliases, no browser JSON Web Token (JWT) storage \| FR-009..011/NFR-015; additional identity features require new product decision |
| [TD-ROW-L4056 · L4056](../../../04-spoolin-techdoc.md#crypto_key_version) | `id` \| `varchar(64)` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Operator-supplied immutable key ID, pattern [a-zA-Z0-9_-]{1,64}. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4057 · L4057](../../../04-spoolin-techdoc.md#crypto_key_version) | `purpose` \| `varchar(16)` \| `NOT NULL CHECK (purpose IN ('PII','DRAFT','EMAIL_LOOKUP')); no SQL default; owner supplies value` \| Cryptographic separation. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4058 · L4058](../../../04-spoolin-techdoc.md#crypto_key_version) | `state` \| `varchar(16)` \| `NOT NULL CHECK (state IN ('WRITE','READ','RETIRED')); no SQL default; owner supplies value` \| Only one WRITE key per purpose. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4059 · L4059](../../../04-spoolin-techdoc.md#crypto_key_version) | `next_nonce` \| `bigint` \| `NOT NULL DEFAULT 1 CHECK (next_nonce > 0); NOT NULL (explicit or PK)` \| Next counter; reserved using separately committed row lock; 96-bit encoding with four zero prefix bytes. internal; presence/clearing §5 \| `Long` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4060 · L4060](../../../04-spoolin-techdoc.md#crypto_key_version) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4061 · L4061](../../../04-spoolin-techdoc.md#crypto_key_version) | `retire_after` \| `timestamptz(6)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| Earliest retirement review, not automatic deletion. internal; presence/clearing §5 \| `Instant?` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4099 · L4099](../../../04-spoolin-techdoc.md#user_account) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4100 · L4100](../../../04-spoolin-techdoc.md#user_account) | `pii_key_id` \| `varchar(64)` \| `NOT NULL REFERENCES crypto_key_version(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Encryption key ID, no secret key material. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4101 · L4101](../../../04-spoolin-techdoc.md#user_account) | `pii_nonce` \| `bytea` \| `NOT NULL CHECK (octet_length(pii_nonce)=12); no SQL default; owner supplies value` \| 96-bit unique per-key nonce. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4102 · L4102](../../../04-spoolin-techdoc.md#user_account) | `pii_cipher` \| `bytea` \| `NOT NULL CHECK (octet_length(pii_cipher) BETWEEN 16 AND 65552); no SQL default; owner supplies value` \| UTF8 JSON ciphertext with appended 16-byte GCM tag; max plaintext 65,536 bytes. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4103 · L4103](../../../04-spoolin-techdoc.md#user_account) | `password_hash` \| `varchar(512)` \| `NOT NULL; no SQL default; owner supplies value` \| Argon2id PHC string; salt/parameters embedded, never decryptable. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4104 · L4104](../../../04-spoolin-techdoc.md#user_account) | `role` \| `varchar(16)` \| `NOT NULL CHECK (role IN ('VIEWER','MAINTAINER')); no SQL default; owner supplies value` \| Registration forces VIEWER. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4105 · L4105](../../../04-spoolin-techdoc.md#user_account) | `auth_epoch` \| `bigint` \| `NOT NULL DEFAULT 0 CHECK (auth_epoch >= 0); NOT NULL (explicit or PK)` \| Revocation generation for all sessions/connectors. internal; presence/clearing §5 \| `Long` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4106 · L4106](../../../04-spoolin-techdoc.md#user_account) | `version` \| `bigint` \| `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` \| Optimistic row version; increment on every domain change. API; presence/clearing §5 \| `Long` → Long(decimal string) / int64 |
| [TD-ROW-L4107 · L4107](../../../04-spoolin-techdoc.md#user_account) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4108 · L4108](../../../04-spoolin-techdoc.md#user_account) | `updated_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Owner sets to transaction timestamp on update; not a DB automatic update. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4150 · L4150](../../../04-spoolin-techdoc.md#account_email_lookup) | `key_id` \| `varchar(64)` \| `NOT NULL REFERENCES crypto_key_version(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| EMAIL_LOOKUP key version. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4151 · L4151](../../../04-spoolin-techdoc.md#account_email_lookup) | `digest` \| `bytea` \| `NOT NULL CHECK (octet_length(digest)=32); no SQL default; owner supplies value` \| HMAC-SHA-256 of normalized email. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4152 · L4152](../../../04-spoolin-techdoc.md#account_email_lookup) | `account_id` \| `uuid` \| `NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Private owner. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4153 · L4153](../../../04-spoolin-techdoc.md#account_email_lookup) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |

## Implementation

1. Implement crypto_key_version, protected user_account and account_email_lookup structures with exact types, constraints/indexes and private projections. Account names/emails are distinct from public catalog people.
2. Build purpose-separated AES-256-GCM envelopes/AAD and independently committed nonce allocation. Use the exact source nonce layout and counter bounds; consumed nonces remain consumed after failed domain transactions.
3. Implement normalized email syntax/IDNA policy and keyed lookup aliases, with uniqueness through the active lookup generations. Preserve exact password bytes and selected Argon2id parameters; bound hashing admission/executor resources.
4. Use immutable safe DTOs and fail closed on absent keys/tampered tag/AAD. Never substitute empty protected values, log payloads, or include keys in shared support libraries.
5. Prove parallel allocation/duplicate-email races and actual DB ciphertext/projections. Rotation/backfill and restored-key transitions are T040, so this task closes initial crypto capability only.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T005-A1:** Encryption: a DB-only read has no readable name/email/password; tampered AAD/tag/key gives a safe failure and leaves valid data unchanged.
- **T005-A2:** Nonce: parallel allocations and rollback consume distinct counters with no reuse; counter exhaustion blocks encryption.
- **T005-A3:** Identity: normalized-email duplicates cannot create two accounts; legitimate exact Unicode/whitespace password bytes verify without normalization or truncation.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-181](../../../05-atdd.md#case-atdd-181-consume-nonces-before-fallible-domain-writes) / Integration | F-CRYPTO: independent synthetic PII/DRAFT/EMAIL_LOOKUP epochs old/new, 100+1 eligible rows, distinct canaries, committed nonce counters and isolated backup checkpoint. Never capture raw key material. | Then every used nonce derives from a committed fresh positive63bit counter with four zero prefix bytes and big-endian suffix And restart/retry/rollback never reuse a consumed value for different bytes And uncommitted allocation is unusable and exhaustion disables protected writes until a new key Observation: Allocate nonce counters concurrently in the real separately committed Core transaction, then roll back/crash domain writes. Inspect monotonic unique allocations, unused rolled-back reservations and maximum-counter refusal; retry uses a fresh committed nonce. |
| [ATDD-277](../../../05-atdd.md#case-atdd-277-preserve-exact-password-bytes-independently-of-new-password-policy) / Unit | A permitted 15-code-point password; the same value with leading space; composed/decomposed suffix variants; 14/15/128/129 code points and 512/513 UTF-8 bytes. | Then the exact byte sequences remain distinct without trim, NFC or truncation And new-password input is accepted only within both selected bounds And existing-credential verification eligibility is independent of a tightened new-password length rule Observation: Inspect byte arrays and eligibility results; use a controlled verifier response only for routing. Actual Argon2id credentials/executor and stored-policy compatibility remain integration proof. |
| [ATDD-296](../../../05-atdd.md#case-atdd-296-encode-allocated-nonce-counters-without-claiming-allocation-durability) / Unit | Counters1, 2, 9223372036854775807; zero, negative and overflow. Expected counter1 nonce hex000000000000000000000001. | Then the nonce is four zero bytes plus the eight-byte big-endian counter And invalid/overflow values reject rather than wrap Observation: Compare exact12-byte vectors; no unit assertion claims unique committed allocation, rollback consumption or restore safety. |
| [ATDD-297](../../../05-atdd.md#case-atdd-297-detect-protected-envelope-and-aad-tampering-in-memory) / Unit | Synthetic32-byte purpose key and freshly allocated test nonce; valid plaintext; altered key/version/nonce/tag/ciphertext or AAD service/table/row/purpose/key/version. | Then the valid matching envelope returns exact plaintext And every incompatible/tampered binding fails without empty or plaintext fallback Observation: Use actual cipher authentication and compare fixed plaintext/failure; no filesystem/database/backup evidence is established here. |

Partial implementation contributions: [ATDD-047](../../../05-atdd.md#case-atdd-047-sign-in), [ATDD-085](../../../05-atdd.md#case-atdd-085-immediate-registration), [ATDD-086](../../../05-atdd.md#case-atdd-086-invalidrepeated-registration), [ATDD-099](../../../05-atdd.md#case-atdd-099-password-protection), [ATDD-115](../../../05-atdd.md#case-atdd-115-registration-name-boundaries), [ATDD-120](../../../05-atdd.md#case-atdd-120-new-password-bounds-and-preservation), [ATDD-171](../../../05-atdd.md#case-atdd-171-bound-sessions-and-revoke-verified-credentials), [ATDD-172](../../../05-atdd.md#case-atdd-172-verify-browser-origin-and-session-bound-submission), [ATDD-175](../../../05-atdd.md#case-atdd-175-keep-normalized-email-identity-precise-and-private), [ATDD-176](../../../05-atdd.md#case-atdd-176-verify-the-exact-credential-with-bounded-hashing), [ATDD-180](../../../05-atdd.md#case-atdd-180-reject-tampered-protected-envelopes-without-data-loss), [ATDD-228](../../../05-atdd.md#case-atdd-228-enforce-cryptographic-key-registry-storage), [ATDD-229](../../../05-atdd.md#case-atdd-229-enforce-protected-account-storage), [ATDD-230](../../../05-atdd.md#case-atdd-230-enforce-protected-email-lookup-storage), [ATDD-258](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T005` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T005` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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

Execution update — 15 October 2026: T005 implementation is active in isolated worktree `/private/tmp/spoolin-account-t005`, branch `codex/account-protection`, claim `lane-153c714853114ee6`, base `7cd8807bab0d1d4042e7005e3826da5ad68e39c2`. This uses accepted T002/T003 and status-only coordination commits; the blocked T004 implementation is excluded. Required local/database checks and independent review are pending. T006/T007 remain blocked by their unselected prerequisites.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including self-review, fresh independent review and applicable visual/interaction checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T007 — Connect account and session browser journeys](T007-connect-account-and-session-browser-journeys.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
