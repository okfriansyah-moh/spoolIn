---
kind: task
id: T006
group: G03
depends_on:
- T005
- T009
- T010
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T006 — Implement registration and authoritative sessions

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-047 | implements |
| ATDD-085 | implements |
| ATDD-086 | implements |
| ATDD-099 | implements |
| ATDD-115 | implements |
| ATDD-120 | implements |
| ATDD-153 | verifies |
| ATDD-171 | implements |
| ATDD-172 | implements |
| ATDD-175 | verifies |
| ATDD-176 | verifies |
| ATDD-231 | implements |
| ATDD-231-T1-R01 | implements |
| ATDD-231-T1-R02 | implements |
| ATDD-231-T1-R03 | implements |
| ATDD-231-T1-R04 | implements |
| ATDD-231-T1-R05 | implements |
| ATDD-231-T1-R06 | implements |
| ATDD-231-T1-R07 | implements |
| ATDD-231-T1-R08 | implements |
| ATDD-231-T1-R09 | implements |
| ATDD-251-T1-R01 | implements |
| ATDD-251-T1-R20 | implements |
| ATDD-251-T1-R21 | implements |
| ATDD-251-T1-R22 | implements |
| ATDD-258 | implements |
| ATDD-337 | implements |
| ATDD-337-T1-R01 | implements |
| ATDD-337-T1-R02 | implements |
| ATDD-337-T1-R03 | implements |
| ATDD-337-T1-R04 | implements |
| ATDD-337-T1-R05 | implements |
| ATDD-337-T1-R06 | implements |
| ATDD-337-T1-R07 | implements |
| ATDD-337-T1-R08 | implements |
| J-08 | both |
| FR-004 | both |
| FR-010 | both |
| FR-011 | both |
| FR-017 | both |
| NFR-005 | both |
| NFR-013 | both |
| NFR-015 | both |
| NFR-016 | both |
| D05 | implements |
| D07 | both |
| D12 | implements |
| AC-047 | implements |
| AC-047-C01 | implements |
| AC-048 | implements |
| AC-048-C01 | implements |
| AC-048-C02 | implements |
| AC-111 | both |
| AC-111-C01 | both |
| AC-112 | both |
| AC-112-C01 | both |
| AC-112-C02 | both |
| AC-125 | both |
| AC-125-C01 | both |
| AC-141 | implements |
| AC-141-C01 | implements |
| AC-141-C02 | implements |
| AC-141-C03 | implements |
| AC-146 | both |
| AC-146-C01 | both |
| AC-146-C02 | both |
| AC-146-C03 | both |
| AC-146-C04 | both |
| AC-155 | both |
| AC-155-C01 | both |
| AC-155-C02 | both |
| AC-155-C03 | both |
| AC-102 | both |
| G-02 | both |
| G-03 | both |
| G-04 | both |
| G-05 | both |
| OD-001 | both |
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
| TD-C-047 | both |
| TD-ROW-L2724 | both |
| TD-ROW-L2743 | both |
| TD-ROW-L2744 | both |
| TD-ROW-L2745 | both |
| TD-ROW-L4193 | both |
| TD-ROW-L4194 | both |
| TD-ROW-L4195 | both |
| TD-ROW-L4196 | both |
| TD-ROW-L4197 | both |
| TD-ROW-L4198 | both |
| TD-ROW-L4199 | both |
| TD-ROW-L4200 | both |
| TD-ROW-L4201 | both |
| F-ACTORS | both |
| BRAIN-D07 | both |

## Prerequisites

- [T005 — Protect account identities and credential storage](T005-protect-account-identities-and-credential-storage.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Encryption: a DB-only read has no readable name/email/password; tampered AAD/tag/key gives a safe failure and leaves valid data unchanged.
- [T009 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T010 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/account/api/ | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/account/application/ | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/session/ | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/boundary/http/ | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/resources/db/migration/V002__browser_sessions.sql | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/session/ | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/integrationTest/kotlin/com/spoolin/core/boundary/session/ | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/account/api/ | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/account/application/ | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/boundary/http/ | T006 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [4.2 SpoolIn Core service](../../../04-spoolin-techdoc.md#42-spoolin-core-service): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.5 Browser HTTP and operational endpoints](../../../04-spoolin-techdoc.md#55-browser-http-and-operational-endpoints): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.1 Authenticated encryption and key lifecycle](../../../04-spoolin-techdoc.md#81-authenticated-encryption-and-key-lifecycle): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.5 Browser storage and session-cookie controls](../../../04-spoolin-techdoc.md#85-browser-storage-and-session-cookie-controls): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-231-T1-R01 · L9277](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage) | `token_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY CHECK (octet_length(token_hash)=32); no SQL default; owner supplies value` |
| [ATDD-231-T1-R02 · L9278](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL UNIQUE; no SQL default; owner supplies value` |
| [ATDD-231-T1-R03 · L9279](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage) | `account_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-231-T1-R04 · L9280](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage) | `auth_epoch` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (auth_epoch >= 0); no SQL default; owner supplies value` |
| [ATDD-231-T1-R05 · L9281](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage) | `csrf_hash` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(csrf_hash)=32); no SQL default; owner supplies value` |
| [ATDD-231-T1-R06 · L9282](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-231-T1-R07 · L9283](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage) | `last_seen_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-231-T1-R08 · L9284](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage) | `expires_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-231-T1-R09 · L9285](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage) | `revoked_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission yields SQL NULL subject to table/variant predicates; explicit NULL is physically nullable, subject to table/variant predicates (rejection is23514); `no SQL constraint; NULL allowed; no SQL default; owner supplies value` |
| [ATDD-251-T1-R01 · L10347](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `session`; Any browser / Core \| Session + same-origin CSRF bootstrap; anonymous/authenticated projections allow only declared fields and no raw session token/password/key; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R20 · L10366](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `register`; Anonymous signed registration scope / Core \| Name/email/current password validation; protected lookup uniqueness; creates Viewer, no auto sign-in; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R21 · L10367](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `signIn`; Browser credential submission / Core \| Exact password and generic failure; fresh bounded session/cookies after verification; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R22 · L10368](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `signOut`; Current session / Core \| Revoke bound session and acknowledge original operation; uncertain revocation reconciled; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-337-T1-R01 · L13894](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies) | Sign-in / rotation / any designated issuing path; __Host-spoolin_session; Secure; HttpOnly; SameSite=Lax; Path=/; no Domain, Expires or Max-Age. Compare rotated credential privately; old fixed/replaced credential does not authenticate |
| [ATDD-337-T1-R02 · L13895](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies) | Actual renewal or reissue; Inspect every path that actually emits the session cookie: same scope/flags, no Expires/Max-Age. A server last_seen refresh need not emit a new cookie; no extra renewal endpoint is invented |
| [ATDD-337-T1-R03 · L13896](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies) | Confirmed logout; Core invalidates original session and emits matching deletion with Max-Age=0; browser removes cookie and old credential replay fails |
| [ATDD-337-T1-R04 · L13897](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies) | Lost logout response; No confirmed-revocation claim from UI deletion alone; protected state locks and existing result/session lookup resolves outcome without retrieving raw credential from receipt |
| [ATDD-337-T1-R05 · L13898](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies) | Valid browser restoration; A retained non-persistent cookie may remain valid; current Core verification/eligibility gates continuation rather than treating browser closure as revocation |
| [ATDD-337-T1-R06 · L13899](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies) | Independently expired or revoked restoration; Still-present cookie fails Core eligibility at expiry/revocation, before purge; no private response/submission authority returns |
| [ATDD-337-T1-R07 · L13900](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies) | Delivery / script visibility; Cookie value absent from JSON, URLs, receipts, diagnostics and application stores; document.cookie cannot read authentication credential. Declared readable CSRF remains functional |
| [ATDD-337-T1-R08 · L13901](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies) | Local listener / credential scope; Inspect trusted configured HTTPS and Secure attribute; do not use localhost plaintext suppression or port differences as an oracle. A browser cookie alone cannot authenticate MCP/other listeners or enter their logs |
| [J-08 · L91](../../../02-PRD.md#32-application-journeys) | Visitor creates an account \| Name, email, and password available \| Register immediately as a Viewer, then sign in without email verification |
| [FR-004 · L238](../../../02-PRD.md#61-functional-requirements) | Create/edit movies and support archive, restore, and confirmed permanent deletion \| A07/A14 Required; approved lifecycle Committed \| AC-016 through AC-021, AC-108 through AC-110, AC-130, AC-143/AC-144, AC-148, AC-155 |
| [FR-010 · L244](../../../02-PRD.md#61-functional-requirements) | Recover failed/uncertain artwork changes while protecting prior and still-used artwork \| D20/D21 Committed; product policy Committed (section 13.1); D11 mechanics Open \| AC-043 through AC-046, AC-152, AC-155, AC-157 |
| [FR-011 · L245](../../../02-PRD.md#61-functional-requirements) | Register immediate Viewer accounts, provide demo accounts, and support sign-in/sign-out \| D05 Committed; approved account scope Committed \| AC-047 through AC-049, AC-111, AC-112, AC-141/AC-142, AC-145/AC-146 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-005 · L263](../../../02-PRD.md#62-quality-and-delivery-requirements) | Setup rejects unusable configuration and protects secrets \| D13/D21; configuration and secret protection Committed \| AC-076 through AC-078, AC-153 through AC-155, AC-157 |
| [NFR-013 · L271](../../../02-PRD.md#62-quality-and-delivery-requirements) | Interfaces implement the product's validation, optional data, access, and failure behavior \| A04/A05/A14 Required support; detailed contracts Open \| AC-100 through AC-103, AC-142/AC-143, AC-148 |
| [NFR-015 · L273](../../../02-PRD.md#62-quality-and-delivery-requirements) | Protect account PII with the selected encryption/rotation constraint; passwords use one-way hashing \| Committed product policy (section 13.1) \| AC-123 through AC-125, AC-127, AC-128; section 10 |
| [NFR-016 · L274](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
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
| [AC-155 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Existing data after tightening \| Existing text/artwork were accepted under older limits, or an account's password was valid before new-password bounds changed \| Limits tighten, the existing record is read, an unchanged stored field is retained during another edit, or the user signs in \| Do not truncate/delete stored text or artwork or invalidate the existing credential solely for the new limits. Keep authorized reads available and permit unchanged fields to remain while validating newly changed input. Invalid new replacement/upload still fails; ordinary reruns do not overwrite preserved data to force conformity. |
| [AC-155-C01 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not truncate/delete stored text or artwork or invalidate the existing credential solely for the new limits. |
| [AC-155-C02 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Keep authorized reads available and permit unchanged fields to remain while validating newly changed input. |
| [AC-155-C03 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Invalid new replacement/upload still fails; ordinary reruns do not overwrite preserved data to force conformity. |
| [AC-102 · L878](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify identity, units, presence, precision, Unicode-code-point counting, and boundary compatibility against section 5.5 \| NFR-013/NFR-016; technical design |
| [G-02 · L923](../../../02-PRD.md#122-release-gates-remain-planned) | Mandated stack/protocols and chosen integration approach work \| NFR-001/NFR-002/NFR-013; AC-064 through AC-067, AC-100 through AC-103 \| Planned in techdoc |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-04 · L925](../../../02-PRD.md#122-release-gates-remain-planned) | Upload, replacement, interrupted work, safe retries, exhaustion, and configured notification work \| FR-009/FR-010/FR-014/FR-015/FR-017; NFR-004/NFR-016; AC-149 through AC-152, AC-155/AC-157 \| Planned |
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
| [TD-C-047 · L5459](../../../04-spoolin-techdoc.md#8-identity-security-and-privacy) | Exact session token/cookie/expiry/CSRF/Origin/Host rules, generic identity failures, normalized ASCII dot-atom+IDNA2008 email, exact Argon2id password bytes/parameters, two-slot hashing executor and five-session bound. |
| [TD-ROW-L2724 · L2724](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `session` \| Any browser / Core \| Session + CSRF bootstrap; anonymous and authenticated projections; no secret output |
| [TD-ROW-L2743 · L2743](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `register` \| Anonymous signed registration scope / Core \| Name/email/current password validation; protected lookup uniqueness; creates Viewer, no auto sign-in |
| [TD-ROW-L2744 · L2744](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `signIn` \| Browser credential submission / Core \| Exact password and generic failure; fresh bounded session/cookies after verification |
| [TD-ROW-L2745 · L2745](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `signOut` \| Current session / Core \| Revoke bound session and acknowledge original operation; uncertain revocation reconciled |
| [TD-ROW-L4193 · L4193](../../../04-spoolin-techdoc.md#browser_session) | `token_hash` \| `bytea` \| `PRIMARY KEY CHECK (octet_length(token_hash)=32); no SQL default; owner supplies value` \| SHA-256 of 256-bit random cookie; plaintext never stored. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4194 · L4194](../../../04-spoolin-techdoc.md#browser_session) | `id` \| `uuid` \| `NOT NULL UNIQUE; no SQL default; owner supplies value` \| Nonsecret session ID for internal assertions. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4195 · L4195](../../../04-spoolin-techdoc.md#browser_session) | `account_id` \| `uuid` \| `NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Owner. internal; presence/clearing §5 \| `UUID` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4196 · L4196](../../../04-spoolin-techdoc.md#browser_session) | `auth_epoch` \| `bigint` \| `NOT NULL CHECK (auth_epoch >= 0); no SQL default; owner supplies value` \| Must match current account epoch. internal; presence/clearing §5 \| `Long` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4197 · L4197](../../../04-spoolin-techdoc.md#browser_session) | `csrf_hash` \| `bytea` \| `NOT NULL CHECK (octet_length(csrf_hash)=32); no SQL default; owner supplies value` \| Digest of the encoded session-csrf-v1 HMAC-derived anti-forgery token specified in §8; not a separate random session credential. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4198 · L4198](../../../04-spoolin-techdoc.md#browser_session) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Session start. internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4199 · L4199](../../../04-spoolin-techdoc.md#browser_session) | `last_seen_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Batched update at most once/min. internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4200 · L4200](../../../04-spoolin-techdoc.md#browser_session) | `expires_at` \| `timestamptz(6)` \| `NOT NULL; no SQL default; owner supplies value` \| Absolute end. internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4201 · L4201](../../../04-spoolin-techdoc.md#browser_session) | `revoked_at` \| `timestamptz(6)` \| `no SQL constraint; NULL allowed; no SQL default; owner supplies value` \| NULL means not explicitly revoked. internal; presence/clearing §5 \| `Instant?` → not exposed; no GraphQL/proto field |
| [BRAIN-D07 · L76](../../../01-brainstorming-result.md#4-decision-register) | D07 \| Recommended \| Viewer/Maintainer roles and predefined initial accounts; exact permission policy and account provisioning remain open |

## Implementation

1. Implement register/signIn/session/signOut through Core with immediate Viewer registration, generic safe duplicate/credential feedback and current owner-scoped receipts. No caller-selected Maintainer role or public account directory.
2. Issue the opaque 32-random-byte session credential only through __Host-spoolin_session Set-Cookie. Store its hash, rotate as specified, enforce five-session bound, 30-minute idle and 12-hour absolute eligibility and auth-epoch revocation.
3. Require locally trusted HTTPS, Secure/HttpOnly/SameSite=Lax/Path=/ and absent Domain. Issuance/renewal omit Expires/Max-Age; confirmed deletion uses matching scope and Max-Age=0. Browser closure never substitutes for server expiry.
4. Implement session bootstrap's deliberately readable CSRF channel and current-session mutation check, exact Host/Origin rules, no-store on every GraphQL outcome including early errors, and safe transport/domain failure distinction.
5. Exercise secure listener headers, real browser restoration, revoked credentials, lost sign-out response, hashing overload and forbidden response channels.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T006-A1:** Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.
- **T006-A2:** Session: expired/revoked credentials fail even if restored by the browser; missing/foreign CSRF/Origin denies mutations while bootstrap can obtain anti-forgery state.
- **T006-A3:** Privacy: raw credential appears only in Set-Cookie, no private data enters logs/receipts/browser storage, and confirmed logout revokes before clearing its cookie.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-153](../../../05-atdd.md#case-atdd-153-keep-credentials-out-of-replay-identities) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.3 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then private-input fingerprints use keyed protection and registration uses normalized identity plus ephemeral submission nonce And no reusable password-derived hash, raw name/email/password or cookie is stored in the receipt And completed registration replay gives generic success rather than a session or account directory Observation: Inspect persisted operation fingerprints/receipts for private-input fixtures and changed ephemeral registration nonce. Verify keyed fingerprints and generic completed registration response; scan raw storage/outputs for passwords and reusable password-derived hashes. |
| [ATDD-175](../../../05-atdd.md#case-atdd-175-keep-normalized-email-identity-precise-and-private) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §8/8.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then case variants resolve one account under authoritative alias uniqueness And plus tags/provider dots are not silently merged and original display form remains encrypted And controls, CRLF, display-name and unsupported Unicode local-part reject without directory exposure Observation: Exercise actual Core normalization/encryption/lookup with ASCII case variants, IDNA domains, plus/dot distinctions and invalid syntax. Inspect encrypted display form, alias identity and duplicate constraint under concurrency; no public account-directory output is permitted. |
| [ATDD-176](../../../05-atdd.md#case-atdd-176-verify-the-exact-credential-with-bounded-hashing) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §8 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then valid exact UTF8 credentials verify without trim, transformation or truncation within15..128 code points/512 bytes And Argon2idv19 uses64 MiB, t3, p1, salt16 bytes/output32-bytes on the bounded two-slot executor And old valid credentials continue and safe successful rehash never exposes passwords or hashes Observation: Use actual Argon2id verifier with independently supplied exact UTF-8 fixtures. Inspect PHC v19 parameters, random salt lengths and bounded hashing executor; whitespace/normal-form changes do not match, and tightening new-password policy preserves a valid existing credential. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-ACTORS · L79](../../../05-atdd.md#f-actors-independent-identities) | Provision Viewer V and Maintainers A/B with independent UUIDs, sessions and auth epochs. Registration fixtures use synthetic `a@example.test`, `b@example.test` identities and permitted names. Supply invented exact passwords through protected test input, never this document’s execution artifacts or recovery drafts. A/B have equal catalog powers; B cannot retrieve A’s draft, private operation or upload cancellation. Create distinct service certificates for SAN `core`, `catalog`, `media`, `mcp`, and separate CLAUDE/CHATGPT connector grants bound to A. Invalid/expired/revoked/wrong-role identities are independently configured rows. |

Partial implementation contributions: [ATDD-047](../../../05-atdd.md#case-atdd-047-sign-in), [ATDD-085](../../../05-atdd.md#case-atdd-085-immediate-registration), [ATDD-086](../../../05-atdd.md#case-atdd-086-invalidrepeated-registration), [ATDD-099](../../../05-atdd.md#case-atdd-099-password-protection), [ATDD-115](../../../05-atdd.md#case-atdd-115-registration-name-boundaries), [ATDD-120](../../../05-atdd.md#case-atdd-120-new-password-bounds-and-preservation), [ATDD-171](../../../05-atdd.md#case-atdd-171-bound-sessions-and-revoke-verified-credentials), [ATDD-172](../../../05-atdd.md#case-atdd-172-verify-browser-origin-and-session-bound-submission), [ATDD-231](../../../05-atdd.md#case-atdd-231-enforce-browser-session-storage), [ATDD-258](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback), [ATDD-337](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T006` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T006` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| boundary-tests | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

Execution update — 15 October 2026: Selected-only dependency check: unselected T009 (effective policy) and T010 (owner receipts/atomic evidence) are Not started. T005 is selected but not yet complete. No registration/session implementation or acceptance run was dispatched. Resume after T005, T009 and T010 are Done.

Resume — 15 October 2026: The user requested every selected shell/account-access implementation delivered. Required T009/T010 are now included in this run; their accepted implementations are pending. T005 is accepted in draft PR4. The earlier selected-only exclusion is superseded by this explicit completion request.

Execution scope update — 16 October 2026: T005 and T004 are accepted. T009 and T010 are included in the authorized sequential completion and are normal unfinished execution prerequisites. T006 waits for their accepted outputs; the earlier selected-only exclusion is historical and is no longer an external blocker.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T007 — Connect account and session browser journeys](T007-connect-account-and-session-browser-journeys.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
