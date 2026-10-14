---
kind: task
id: T008
group: G04
depends_on:
- T003
- T005
- T009
- T010
- T006
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T008 — Enforce scoped service identity and shared admission

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-047 | implements |
| ATDD-050 | implements |
| ATDD-051 | implements |
| ATDD-052 | implements |
| ATDD-085 | implements |
| ATDD-086 | implements |
| ATDD-097 | implements |
| ATDD-099 | implements |
| ATDD-113 | implements |
| ATDD-115 | implements |
| ATDD-116 | implements |
| ATDD-117 | implements |
| ATDD-118 | implements |
| ATDD-119 | implements |
| ATDD-120 | implements |
| ATDD-121 | implements |
| ATDD-122 | implements |
| ATDD-127 | implements |
| ATDD-128 | implements |
| ATDD-129 | implements |
| ATDD-131 | implements |
| ATDD-140 | implements |
| ATDD-171 | implements |
| ATDD-172 | implements |
| ATDD-173 | both |
| ATDD-174 | both |
| ATDD-177 | implements |
| ATDD-184 | implements |
| ATDD-212 | implements |
| ATDD-234 | implements |
| ATDD-234-T1-R01 | implements |
| ATDD-234-T1-R02 | implements |
| ATDD-234-T1-R03 | implements |
| ATDD-234-T1-R04 | implements |
| ATDD-252-T1-R01 | implements |
| ATDD-252-T1-R02 | implements |
| ATDD-258 | implements |
| ATDD-267 | implements |
| ATDD-280 | both |
| ATDD-295 | both |
| ATDD-318 | both |
| ATDD-323 | implements |
| ATDD-324 | implements |
| ATDD-325 | implements |
| ATDD-326 | implements |
| ATDD-327 | implements |
| ATDD-328 | implements |
| ATDD-334 | implements |
| ATDD-336 | implements |
| ATDD-337 | implements |
| ATDD-338 | implements |
| J-08 | implements |
| SC-04 | both |
| FR-002 | implements |
| FR-004 | implements |
| FR-005 | implements |
| FR-006 | implements |
| FR-007 | implements |
| FR-010 | implements |
| FR-011 | implements |
| FR-012 | both |
| FR-016 | implements |
| FR-017 | implements |
| NFR-005 | implements |
| NFR-006 | both |
| NFR-007 | implements |
| NFR-008 | both |
| NFR-012 | implements |
| NFR-013 | implements |
| NFR-015 | implements |
| NFR-016 | implements |
| D05 | both |
| D06 | implements |
| D07 | both |
| D12 | both |
| D21 | implements |
| D23 | both |
| AC-047 | implements |
| AC-047-C01 | implements |
| AC-048 | implements |
| AC-048-C01 | implements |
| AC-048-C02 | implements |
| AC-050 | both |
| AC-050-C01 | both |
| AC-051 | implements |
| AC-051-C01 | implements |
| AC-052 | implements |
| AC-052-C01 | implements |
| AC-111 | implements |
| AC-111-C01 | implements |
| AC-112 | implements |
| AC-112-C01 | implements |
| AC-112-C02 | implements |
| AC-123 | implements |
| AC-123-C01 | implements |
| AC-125 | implements |
| AC-125-C01 | implements |
| AC-139 | implements |
| AC-139-C01 | implements |
| AC-139-C02 | implements |
| AC-139-C03 | implements |
| AC-141 | implements |
| AC-141-C01 | implements |
| AC-141-C02 | implements |
| AC-141-C03 | implements |
| AC-142 | implements |
| AC-142-C01 | implements |
| AC-142-C02 | implements |
| AC-142-C03 | implements |
| AC-143 | implements |
| AC-143-C01 | implements |
| AC-143-C02 | implements |
| AC-143-C03 | implements |
| AC-144 | implements |
| AC-144-C01 | implements |
| AC-144-C02 | implements |
| AC-144-C03 | implements |
| AC-145 | implements |
| AC-145-C01 | implements |
| AC-145-C02 | implements |
| AC-145-C03 | implements |
| AC-145-C04 | implements |
| AC-146 | implements |
| AC-146-C01 | implements |
| AC-146-C02 | implements |
| AC-146-C03 | implements |
| AC-146-C04 | implements |
| AC-147 | implements |
| AC-147-C01 | implements |
| AC-147-C02 | implements |
| AC-147-C03 | implements |
| AC-148 | implements |
| AC-148-C01 | implements |
| AC-148-C02 | implements |
| AC-148-C03 | implements |
| AC-153 | implements |
| AC-153-C01 | implements |
| AC-153-C02 | implements |
| AC-153-C03 | implements |
| AC-153-C04 | implements |
| AC-153-C05 | implements |
| AC-154 | implements |
| AC-154-C01 | implements |
| AC-154-C02 | implements |
| AC-154-C03 | implements |
| AC-154-C04 | implements |
| AC-155 | implements |
| AC-155-C01 | implements |
| AC-155-C02 | implements |
| AC-155-C03 | implements |
| AC-157 | implements |
| AC-157-C01 | implements |
| AC-157-C02 | implements |
| AC-080 | both |
| AC-098 | implements |
| AC-099 | implements |
| G-03 | implements |
| G-04 | implements |
| G-05 | both |
| AC-078 | implements |
| AC-078-C01 | implements |
| OD-002 | both |
| OD-001 | both |
| US-008 | implements |
| US-020 | implements |
| PRD-POLICY-14 | implements |
| PRD-BOUND-02 | implements |
| PRD-POLICY-20 | implements |
| PRD-BOUND-08 | implements |
| PRD-BOUND-09 | implements |
| PRD-POLICY-21 | implements |
| PRD-POLICY-22 | implements |
| PRD-BOUND-11 | implements |
| PRD-FEEDBACK-04 | implements |
| PRD-HANDOFF-10 | implements |
| PRD-HANDOFF-11 | implements |
| PRD-HANDOFF-14 | implements |
| TD-C-018 | both |
| TD-C-019 | both |
| TD-C-051 | both |
| TD-ROW-L2648 | both |
| TD-ROW-L2649 | both |
| TD-ROW-L2650 | both |
| TD-ROW-L2651 | both |
| TD-ROW-L2652 | both |
| TD-ROW-L2653 | both |
| TD-ROW-L2654 | both |
| TD-ROW-L2655 | both |
| TD-ROW-L2656 | both |
| TD-ROW-L2657 | both |
| TD-ROW-L2658 | both |
| TD-ROW-L4353 | both |
| TD-ROW-L4354 | both |
| TD-ROW-L4355 | both |
| TD-ROW-L4356 | both |
| TD-ROW-L5559 | both |
| TD-ROW-L5560 | both |
| TD-ROW-L5561 | both |
| TD-ROW-L5562 | both |
| TD-ROW-L5563 | both |
| TD-ROW-L6032 | both |
| F-RATE | both |
| BRAIN-D05 | both |
| BRAIN-D07 | both |
| BRAIN-D12 | both |
| BRAIN-D23 | both |

## Prerequisites

- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- [T005 — Protect account identities and credential storage](../../03-account-access/tasks/T005-protect-account-identities-and-credential-storage.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Encryption: a DB-only read has no readable name/email/password; tampered AAD/tag/key gives a safe failure and leaves valid data unchanged.
- [T009 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T010 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T006 — Implement registration and authoritative sessions](../../03-account-access/tasks/T006-implement-registration-and-authoritative-sessions.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | libraries/backend-support/src/main/kotlin/com/spoolin/support/identity/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/ratelimit/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/boundary/grpc/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/boundary/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/main/kotlin/com/spoolin/media/boundary/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/resources/db/migration/V003__rate_windows.sql | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/integrationTest/kotlin/com/spoolin/core/boundary/admission/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/identity/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | libraries/backend-support/src/test/kotlin/com/spoolin/support/identity/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/ratelimit/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/boundary/grpc/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/boundary/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/media/src/test/kotlin/com/spoolin/media/boundary/ | T008 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [5.7 Internal contract enforcement and compatibility](../../../04-spoolin-techdoc.md#57-internal-contract-enforcement-and-compatibility): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.2 Threat model and assurance records](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.4 Configurable rate admission](../../../04-spoolin-techdoc.md#84-configurable-rate-admission): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.5.4 Session and CSRF responsibility map](../../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-234-T1-R01 · L9443](../../../05-atdd.md#case-atdd-234-enforce-shared-rate-window-storage) | `bucket_key` (`bytea`); Genuine owner-valid F-CRYPTO/hash for accepted fixture; synthetic bytes test only physical bounds at 32 and ±1 bytes; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (octet_length(bucket_key)=32); no SQL default; owner supplies value` |
| [ATDD-234-T1-R02 · L9444](../../../05-atdd.md#case-atdd-234-enforce-shared-rate-window-storage) | `window_start` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-234-T1-R03 · L9445](../../../05-atdd.md#case-atdd-234-enforce-shared-rate-window-storage) | `hits` (`integer`); Source-valid integer; physical upper 2147483647 versus 2147483648 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (hits > 0); no SQL default; owner supplies value` |
| [ATDD-234-T1-R04 · L9446](../../../05-atdd.md#case-atdd-234-enforce-shared-rate-window-storage) | `expires_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-252-T1-R01 · L10422](../../../05-atdd.md#case-atdd-252-verify-identityservice-typed-service-and-actor-boundary) | `ResolveActor`; ResolveActor(ResolveActorRequest) → Actor |
| [ATDD-252-T1-R02 · L10423](../../../05-atdd.md#case-atdd-252-verify-identityservice-typed-service-and-actor-boundary) | `CheckActor`; CheckActor(CheckActorRequest) → Actor |
| [J-08 · L93](../../../02-PRD.md#32-application-journeys) | Visitor creates an account \| Name, email, and password available \| Register immediately as a Viewer, then sign in without email verification |
| [SC-04 · L105](../../../02-PRD.md#4-goals-and-success-criteria) | Protect access and accountability \| Public/Viewer/Maintainer permissions hold; private account data stay protected; permitted changes and AI approvals are attributable \| Unverified; Committed |
| [FR-002 · L238](../../../02-PRD.md#61-functional-requirements) | Search movie titles and cast/creator names with the approved matching and visibility rules \| A11/A14 Required; product policy Committed (section 13.1) \| AC-006 through AC-010, AC-022, AC-134, AC-147, AC-159 |
| [FR-004 · L240](../../../02-PRD.md#61-functional-requirements) | Create/edit movies and support archive, restore, and confirmed permanent deletion \| A07/A14 Required; approved lifecycle Committed \| AC-016 through AC-021, AC-108 through AC-110, AC-130, AC-143/AC-144, AC-148, AC-155 |
| [FR-005 · L241](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-006 · L242](../../../02-PRD.md#61-functional-requirements) | Maintain cast/creator credits, starter/custom roles, and multiple roles/character names; respect person lifecycle \| A09/A10 support; D25 Committed; product policy Committed (section 13.1) \| AC-026 through AC-028, AC-132, AC-135, AC-136, AC-143/AC-144 |
| [FR-007 · L243](../../../02-PRD.md#61-functional-requirements) | Preserve usable metadata, explicit unknowns, identity, date precision, and provenance \| D25/D26 Committed \| AC-029 through AC-032, AC-143, AC-145 |
| [FR-010 · L246](../../../02-PRD.md#61-functional-requirements) | Recover failed/uncertain artwork changes while protecting prior and still-used artwork \| D20/D21 Committed; product policy Committed (section 13.1); D11 mechanics Open \| AC-043 through AC-046, AC-152, AC-155, AC-157 |
| [FR-011 · L247](../../../02-PRD.md#61-functional-requirements) | Register immediate Viewer accounts, provide demo accounts, and support sign-in/sign-out \| D05 Committed; approved account scope Committed \| AC-047 through AC-049, AC-111, AC-112, AC-141/AC-142, AC-145/AC-146 |
| [FR-012 · L248](../../../02-PRD.md#61-functional-requirements) | Enforce the public/Viewer/Maintainer capability matrix, including private movie/people archives and connected tools \| D05 Committed; product policy Committed (section 13.1) \| AC-050 through AC-052, AC-113, AC-119, AC-133, AC-134 |
| [FR-016 · L252](../../../02-PRD.md#61-functional-requirements) | Create movies from MCP proposals only after exact-content human approval in ChatGPT/Claude \| Committed product policy (section 13.1) \| AC-116 through AC-122, AC-126, AC-148, AC-156 |
| [FR-017 · L253](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-005 · L265](../../../02-PRD.md#62-quality-and-delivery-requirements) | Setup rejects unusable configuration and protects secrets \| D13/D21; configuration and secret protection Committed \| AC-076 through AC-078, AC-153 through AC-155, AC-157 |
| [NFR-006 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Operations owners can assess readiness, diagnose user failures, and act on alerts \| D23 Committed \| AC-079 through AC-082 |
| [NFR-007 · L267](../../../02-PRD.md#62-quality-and-delivery-requirements) | Responsive, keyboard-usable journeys with accurate feedback and retained recoverable work \| A13/A14 Required; keyboard use Committed \| AC-083 through AC-085, AC-139, AC-147, AC-159 |
| [NFR-008 · L268](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-012 · L272](../../../02-PRD.md#62-quality-and-delivery-requirements) | Risk-informed security consideration with accurate evidence and limitations \| D06 Committed \| AC-098, AC-099 |
| [NFR-013 · L273](../../../02-PRD.md#62-quality-and-delivery-requirements) | Interfaces implement the product's validation, optional data, access, and failure behavior \| A04/A05/A14 Required support; detailed contracts Open \| AC-100 through AC-103, AC-142/AC-143, AC-148 |
| [NFR-015 · L275](../../../02-PRD.md#62-quality-and-delivery-requirements) | Protect account PII with the selected encryption/rotation constraint; passwords use one-way hashing \| Committed product policy (section 13.1) \| AC-123 through AC-125, AC-127, AC-128; section 10 |
| [NFR-016 · L276](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [D05 · L308](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D06 · L309](../../../02-PRD.md#64-project-decision-register) | ISO-informed consideration, no certification/conformity claim \| NFR-012 / AC-098, AC-099 |
| [D07 · L310](../../../02-PRD.md#64-project-decision-register) | Viewer/Maintainer and prepared accounts \| Viewer/Maintainer roles and prepared accounts are adopted as Committed policy in section 13.1; FR-011/FR-012 / AC-049, AC-050, AC-111 |
| [D12 · L315](../../../02-PRD.md#64-project-decision-register) | Authentication mechanism and verified internal identity \| Product access settled; mechanics Open in techdoc. FR-011/FR-012 / AC-047 through AC-052 |
| [D21 · L324](../../../02-PRD.md#64-project-decision-register) | Bounded safe retries and one configured exhaustion channel \| NFR-004/NFR-005, FR-015 / AC-061 through AC-063, AC-072 through AC-078 |
| [D23 · L326](../../../02-PRD.md#64-project-decision-register) | Health/readiness, logs, metrics, dashboards, actionable alerts \| NFR-006 / AC-079 through AC-082 |
| [AC-047 · L437](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Sign in \| A prepared or registered account exists \| Correct or incorrect credentials are submitted \| Correct credentials permit sign-in with the assigned role; invalid credentials do not grant access and receive non-sensitive feedback. |
| [AC-047-C01 · L437](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Correct credentials permit sign-in with the assigned role; invalid credentials do not grant access and receive non-sensitive feedback. |
| [AC-048 · L438](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Expiry/sign-out \| A user signs out or their sign-in expires during work \| They attempt another restricted action \| Require sign-in again; public browsing remains available. Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-048-C01 · L438](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Require sign-in again; public browsing remains available. |
| [AC-048-C02 · L438](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-050 · L440](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Role matrix \| Visitors, Viewers, and Maintainers try each capability \| An action is requested \| Results match section 3.1, including direct attempts to access hidden actions; all Maintainers have the same management permissions. |
| [AC-050-C01 · L440](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Results match section 3.1, including direct attempts to access hidden actions; all Maintainers have the same management permissions. |
| [AC-051 · L441](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Bypass attempt \| A caller supplies fabricated identity/role claims \| They attempt a restricted action \| No privilege or data access is granted from those claims; a connected assistant cannot exceed its user's permissions. |
| [AC-051-C01 · L441](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | No privilege or data access is granted from those claims; a connected assistant cannot exceed its user's permissions. |
| [AC-052 · L442](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Mixed permitted/restricted actions \| A request combines readable data and restricted effects \| It is handled \| Restricted information/effects stay denied; any completed permitted action and failed action are distinguishable, without claiming all actions succeeded. |
| [AC-052-C01 · L442](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Restricted information/effects stay denied; any completed permitted action and failed action are distinguishable, without claiming all actions succeeded. |
| [AC-111 · L443](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Immediate registration \| A visitor supplies valid name, email, and password for a new account \| Registration succeeds \| Create a Viewer eligible to sign in immediately, without email verification; do not allow registration as Maintainer. |
| [AC-111-C01 · L443](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Create a Viewer eligible to sign in immediately, without email verification; do not allow registration as Maintainer. |
| [AC-112 · L444](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Invalid/repeated registration \| A required field is missing, a registration value violates the effective policy in section 5.5, email is invalid/already used, or submission is repeated \| Registration is attempted \| Show safe corrective feedback, create no duplicate account, and do not expose whether another account contains private details. Never echo/store the password in recovery drafts or logs. |
| [AC-112-C01 · L444](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Show safe corrective feedback, create no duplicate account, and do not expose whether another account contains private details. |
| [AC-112-C02 · L444](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Never echo/store the password in recovery drafts or logs. |
| [AC-123 · L520](../../../02-PRD.md#us-020-protect-private-account-information) | Private data boundaries \| Accounts contain names/emails and password credentials \| Public pages, catalog search, other users' access, diagnostics, or MCP proposals are inspected \| Private account information is not exposed outside its authorized purpose; email and private name data receive the selected encrypted-at-rest protection. |
| [AC-123-C01 · L520](../../../02-PRD.md#us-020-protect-private-account-information) | Private account information is not exposed outside its authorized purpose; email and private name data receive the selected encrypted-at-rest protection. |
| [AC-125 · L522](../../../02-PRD.md#us-020-protect-private-account-information) | Password protection \| A user has registered or signed in \| Credential storage and outputs are checked by authorized assessment reviewers \| Passwords are protected by one-way password hashing, cannot be retrieved as an account feature, and never appear in readable storage, audit, logs, or AI payloads. |
| [AC-125-C01 · L522](../../../02-PRD.md#us-020-protect-private-account-information) | Passwords are protected by one-way password hashing, cannot be retrieved as an account feature, and never appear in readable storage, audit, logs, or AI payloads. |
| [AC-139 · L531](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Visible effective policy \| A permitted form, search field, or artwork selection is available \| The user enters information or selects artwork \| Show applicable current bounds and enabled formats before submission. Validation feedback names the affected input and correction, is accessible without color alone, and retains permitted non-secret content. Show processing and saved outcomes separately from validation. |
| [AC-139-C01 · L531](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Show applicable current bounds and enabled formats before submission. |
| [AC-139-C02 · L531](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Validation feedback names the affected input and correction, is accessible without color alone, and retains permitted non-secret content. |
| [AC-139-C03 · L531](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Show processing and saved outcomes separately from validation. |
| [AC-141 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Registration-name boundaries \| Registration-name defaults are configured and remaining registration data are valid \| A visitor submits a name of 2, 100, 1, or 101 code points after trimming \| Accept lengths 2 and 100 and create the Viewer on confirmed success. Reject lengths 1 and 101 without creating an account. Explain the configured length restriction without claiming the person's name is unreal or unauthentic. |
| [AC-141-C01 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Accept lengths 2 and 100 and create the Viewer on confirmed success. |
| [AC-141-C02 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject lengths 1 and 101 without creating an account. |
| [AC-141-C03 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Explain the configured length restriction without claiming the person's name is unreal or unauthentic. |
| [AC-142 · L534](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Unicode, mononyms, and counting \| A permitted name/text value contains Unicode, internal spaces, accents, a hyphen, or an apostrophe \| It is submitted through a form or direct request at a configured boundary \| Apply the same Unicode-code-point count and bounds; preserve legitimate characters and internal spacing without demanding two name parts. Count precomposed é as one code point and its e-plus-combining-accent representation as two; byte length must not determine acceptance. A mononym meeting its field's bound is accepted. |
| [AC-142-C01 · L534](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Apply the same Unicode-code-point count and bounds; preserve legitimate characters and internal spacing without demanding two name parts. |
| [AC-142-C02 · L534](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Count precomposed é as one code point and its e-plus-combining-accent representation as two; byte length must not determine acceptance. |
| [AC-142-C03 · L534](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | A mononym meeting its field's bound is accepted. |
| [AC-143 · L535](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Whitespace, absence, and clearing \| A required ordinary-text field is blank/whitespace-only, or optional original-title/character data are unknown or previously saved \| A create/edit supplies blank required text, explicitly clears optional text, or omits an optional edit value \| Reject blank required text with no partial save. Permit unknown/cleared optional values; omission preserves the stored value. Validate each supplied nonempty optional value, retain meaningful synopsis line breaks, and create no invented metadata. |
| [AC-143-C01 · L535](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject blank required text with no partial save. |
| [AC-143-C02 · L535](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Permit unknown/cleared optional values; omission preserves the stored value. |
| [AC-143-C03 · L535](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Validate each supplied nonempty optional value, retain meaningful synopsis line breaks, and create no invented metadata. |
| [AC-144 · L536](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Other text boundaries \| Recommended bounds are configured: title 1 to 255, synopsis 1 to 5,000, role 1 to 100, and each character name 1 to 200 \| A permitted save supplies each field at its bounds or immediately outside them, with other requirements valid \| Accept the boundary lengths, including a one-character title; reject an empty required value or one above the applicable maximum. Retain the complete permitted draft, commit no partial edit, and silently truncate nothing. Apply character-name limits separately to each supplied name without imposing an invented count limit. |
| [AC-144-C01 · L536](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Accept the boundary lengths, including a one-character title; reject an empty required value or one above the applicable maximum. |
| [AC-144-C02 · L536](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Retain the complete permitted draft, commit no partial edit, and silently truncate nothing. |
| [AC-144-C03 · L536](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Apply character-name limits separately to each supplied name without imposing an invented count limit. |
| [AC-145 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Email and source URL \| Recommended email/URL maxima are configured \| Registration uses a valid email of 254 characters or one of 255; accepted source input uses a valid HTTP/HTTPS URL of 2,048 characters or one of 2,049; or either format is invalid \| Length alone permits the exact maximum and rejects the excess; independently reject invalid syntax and preserve required/optional semantics. Existing-account checks still apply. Accepting a URL authorizes no arbitrary fetch or new source-entry feature. Detailed syntax cases transfer to OD-003/OD-019. |
| [AC-145-C01 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Length alone permits the exact maximum and rejects the excess; independently reject invalid syntax and preserve required/optional semantics. |
| [AC-145-C02 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Existing-account checks still apply. |
| [AC-145-C03 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Accepting a URL authorizes no arbitrary fetch or new source-entry feature. |
| [AC-145-C04 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Detailed syntax cases transfer to OD-003/OD-019. |
| [AC-146 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | New-password bounds and preservation \| Recommended password bounds of 15 to 128 are configured \| A visitor supplies passwords of 14, 15, 128, or 129 code points, including supported Unicode and whitespace \| Reject 14 and 129 for length; 15 and 128 pass the length check subject to the adopted credential policy under OD-001. Evaluate the exact entered password without trimming, transformation, or truncation. No password appears in recovery drafts, audit, or diagnostics. Successful sign-in later accepts that same credential under its account policy. |
| [AC-146-C01 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject 14 and 129 for length; 15 and 128 pass the length check subject to the adopted credential policy under OD-001. |
| [AC-146-C02 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Evaluate the exact entered password without trimming, transformation, or truncation. |
| [AC-146-C03 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | No password appears in recovery drafts, audit, or diagnostics. |
| [AC-146-C04 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Successful sign-in later accepts that same credential under its account policy. |
| [AC-147 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Plain-text safety \| Entered catalog text, names, search text, or changed values contain markup, apostrophes, or instruction-like content within applicable limits \| They appear in forms, catalog/detail pages, search results, previews, or audit history \| Treat the content as text and execute no embedded code or instructions. Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. Private registration data retain their access boundaries. |
| [AC-147-C01 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Treat the content as text and execute no embedded code or instructions. |
| [AC-147-C02 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. |
| [AC-147-C03 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Private registration data retain their access boundaries. |
| [AC-148 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Direct-request validation \| A permitted caller bypasses form checks, or an authorized MCP caller submits an approved movie with invalid changed/new input \| The request reaches the application \| Enforce the effective bounds and content policy, reject invalid input with corrective feedback, and commit no partial catalog/account change or misleading successful audit event. Denied callers remain denied regardless of input validity. Do not repeat permanently invalid requests automatically. |
| [AC-148-C01 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Enforce the effective bounds and content policy, reject invalid input with corrective feedback, and commit no partial catalog/account change or misleading successful audit event. |
| [AC-148-C02 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Denied callers remain denied regardless of input validity. |
| [AC-148-C03 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not repeat permanently invalid requests automatically. |
| [AC-153 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Valid policy override \| A documented deployment configuration changes an applicable text/file minimum or maximum, or enables a nonempty subset of the static formats \| The configuration becomes effective using the techdoc's documented application procedure \| Forms and authoritative validation use the same effective policy. Show changed guidance and enforce inclusive new boundaries through direct requests and MCP as well. Required fields stay required; empty search still clears the text restriction even if the nonempty-query minimum increases. A disabled format is rejected for new uploads. No Maintainer configuration screen is implied. |
| [AC-153-C01 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Forms and authoritative validation use the same effective policy. |
| [AC-153-C02 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Show changed guidance and enforce inclusive new boundaries through direct requests and MCP as well. |
| [AC-153-C03 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Required fields stay required; empty search still clears the text restriction even if the nonempty-query minimum increases. |
| [AC-153-C04 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | A disabled format is rejected for new uploads. |
| [AC-153-C05 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | No Maintainer configuration screen is implied. |
| [AC-154 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Invalid policy configuration \| A text bound is negative/nonintegral/nonfinite, a required minimum permits emptiness, file size is nonpositive, minimum exceeds maximum, a setting is explicitly invalid/empty, or the format set is empty/unsupported \| The policy is loaded or applied \| Affected functionality does not become ready under the invalid policy. Restricted diagnostics identify the setting and correction without secrets; no silent unlimited limit, supported-format expansion, or unsafe fallback occurs. An unset value follows only its documented default. Application/reload behavior remains OD-020. |
| [AC-154-C01 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Affected functionality does not become ready under the invalid policy. |
| [AC-154-C02 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Restricted diagnostics identify the setting and correction without secrets; no silent unlimited limit, supported-format expansion, or unsafe fallback occurs. |
| [AC-154-C03 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | An unset value follows only its documented default. |
| [AC-154-C04 · L546](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Application/reload behavior remains OD-020. |
| [AC-155 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Existing data after tightening \| Existing text/artwork were accepted under older limits, or an account's password was valid before new-password bounds changed \| Limits tighten, the existing record is read, an unchanged stored field is retained during another edit, or the user signs in \| Do not truncate/delete stored text or artwork or invalidate the existing credential solely for the new limits. Keep authorized reads available and permit unchanged fields to remain while validating newly changed input. Invalid new replacement/upload still fails; ordinary reruns do not overwrite preserved data to force conformity. |
| [AC-155-C01 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not truncate/delete stored text or artwork or invalidate the existing credential solely for the new limits. |
| [AC-155-C02 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Keep authorized reads available and permit unchanged fields to remain while validating newly changed input. |
| [AC-155-C03 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Invalid new replacement/upload still fails; ordinary reruns do not overwrite preserved data to force conformity. |
| [AC-157 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Policy changes during a draft/upload \| Input or artwork met earlier limits, and the policy changes before submission/attachment \| The operation is evaluated against the effective policy \| If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-157-C01 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. |
| [AC-157-C02 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-080 · L868](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Preserve sanitized correlation across frontend requests, internal calls, and durable work \| NFR-006; technical design |
| [AC-098 · L876](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Record relevant risks, selected controls, owners, evidence, and limitations \| NFR-012; technical design |
| [AC-099 · L877](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Distinguish intended controls from tested/deployed proof; do not imply ISO/regulatory certification, TLS enforcement, or successful restore without evidence \| NFR-012; technical design |
| [G-03 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-04 · L927](../../../02-PRD.md#122-release-gates-remain-planned) | Upload, replacement, interrupted work, safe retries, exhaustion, and configured notification work \| FR-009/FR-010/FR-014/FR-015/FR-017; NFR-004/NFR-016; AC-149 through AC-152, AC-155/AC-157 \| Planned |
| [G-05 · L928](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [AC-078 · L989](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Secrets \| Setup uses account/provider/key material \| Documentation and diagnostics are inspected \| Real secrets are absent from repository examples, user messages, logs, audit, and notifications. |
| [AC-078-C01 · L989](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Real secrets are absent from repository examples, user messages, logs, audit, and notifications. |
| [OD-002 · L1021](../../../02-PRD.md#131-resolved-product-decisions) | Public discovery; Viewer and Maintainer; same permissions for all Maintainers; no role-management UI \| FR-011/FR-012; AC-049/AC-050/AC-113 |
| [OD-001 · L1044](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Product access resolved; authentication/session, expiry, account provisioning, credential-strength checks and adoption of Recommended password bounds, credential protection, and verified service/MCP identity remain for techdoc \| FR-011/FR-012/FR-017, NFR-015/NFR-016 \| Technical design owner unassigned; before identity-dependent implementation |
| [US-008 · L431](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | ### US-008: Register, sign in, and enforce access As a visitor, I want an account I can use immediately; as a signed-in user, I want clear permissions. Requirements: FR-011, FR-012, NFR-015. Ratings/reviews remain deferred, so Viewer registration does not unlock a new catalog-writing feature. |
| [US-020 · L514](../../../02-PRD.md#us-020-protect-private-account-information) | ### US-020: Protect private account information As an account holder, I want my private information protected while I can continue to use my account. Requirement: NFR-015. The encryption constraints are stated in section 10; the techdoc will define their implementation. |
| [PRD-POLICY-14 · L146](../../../02-PRD.md#53-accounts-audit-and-ai-boundaries) | Registration collects name, email, and password and immediately creates a Viewer. Prepared Viewer/Maintainer accounts support the demo. Users cannot choose Maintainer during registration. Sign-in and sign-out are included; public reading remains available after sign-out. Invalid registration/sign-in has understandable feedback without exposing credentials or another account's information. |
| [PRD-BOUND-02 · L171](../../../02-PRD.md#name-defaults) | Registration name \| 2 characters \| 100 characters \| Committed selected defaults: a one-character account name is excluded under the default configuration |
| [PRD-POLICY-20 · L177](../../../02-PRD.md#recommended-defaults-for-other-inputs) | These Recommended starting values remain unselected and are not external assessment obligations. The techdoc must record adopted values or justified alternatives before implementing the affected inputs. Acceptance criteria referring to these numbers apply when these recommended values are configured; the Committed behavior is enforcement of the effective policy. |
| [PRD-BOUND-08 · L186](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Registration email \| Valid, nonempty email \| 254 characters \| Length does not replace format or existing-account checks |
| [PRD-BOUND-09 · L187](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Registration password \| 15 characters \| 128 characters \| Apply to new passwords, not to the validity of an existing account's credential |
| [PRD-POLICY-21 · L190](../../../02-PRD.md#recommended-defaults-for-other-inputs) | The password recommendation follows OWASP authentication guidance for accounts without MFA. Exact email/URL syntax, credential-strength checks, and validation mechanisms remain technical-design work under OD-001/OD-003/OD-019. Other exposed fields, including numerical metadata and collection bounds where applicable, must receive documented limits in technical design; this does not add metadata fields or management screens to the release. |
| [PRD-POLICY-22 · L194](../../../02-PRD.md#validation-safe-handling-and-feedback) | Bounds are inclusive and independently configurable for each applicable field. Required fields remain required, and their effective minimum cannot allow an empty value. Measure text length consistently in Unicode code points. Trim surrounding whitespace from ordinary text before counting and validation while preserving internal spaces, legitimate punctuation, accents, and meaningful synopsis line breaks. Never trim, transform, or silently truncate a password. No field is silently shortened to fit a limit. |
| [PRD-BOUND-11 · L210](../../../02-PRD.md#artwork-policy) | Supported formats \| Static JPEG, PNG, and WebP \| Committed supported formats; configuration may enable a nonempty subset |
| [PRD-FEEDBACK-04 · L800](../../../02-PRD.md#810-shared-feedback-rules) | Validation / denied access \| Show effective bounds and allowed formats; identify field/content corrections or permission failure without revealing private data. Retain permitted non-secret input without truncation; execute no entered markup. |
| [PRD-HANDOFF-10 · L845](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D06 \| Committed consideration \| Risk-to-control/owner/evidence/limitation review informed by ISO/IEC 27001/27002, without certification or organizational-conformity claims. |
| [PRD-HANDOFF-11 · L846](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | Account data protection requirements; NFR-015 \| Committed \| AES-256-GCM for recoverable account PII; versioned keys, authorized reads during rotation, safe re-encryption and retirement; one-way password hashing. Validate adaptation to this stack, backup/recovery implications, and safe failure behavior. |
| [PRD-HANDOFF-14 · L849](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D07/D08/D09 \| Product choices adopted only where stated \| Viewer/Maintainer/prepared accounts are selected; stale-save protection is selected. Queries/mutations without subscriptions and the specific optimistic-concurrency mechanism remain engineering recommendations. |
| [TD-C-018 · L2610](../../../04-spoolin-techdoc.md#57-internal-contract-enforcement-and-compatibility) | Ed25519 actor assertions bind issuer/receiver/method/account/credential/role/epoch/time/nonce/correlation/operation; &lt;=30s, &lt;=2s skew, &lt;=4KiB, exact encoding/key rotation. Live Core recheck precedes owner locks. |
| [TD-C-019 · L2642](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | Every method×SAN pair follows explicit allowlist and extra actor/effect predicate; IdentityService.ResolveActor denies all selected network callers; worker certificates cannot elevate user payloads. |
| [TD-C-051 · L5549](../../../04-spoolin-techdoc.md#84-configurable-rate-admission) | Configurable fixed-window Core rate profiles, exact positive ranges/identities/HMAC separation, multi-profile atomic admission, durable restart/counter/config transitions, fail closed unavailable state, safe429 timing; MCP local bounded binding counters separate from durable tool quotas. |
| [TD-ROW-L2648 · L2648](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | IdentityService.CheckActor \| catalog \| Exact assertion-bound actor/credential/auth epoch; live lookup before protected Catalog transaction |
| [TD-ROW-L2649 · L2649](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | IdentityService.ResolveActor \| none \| No selected network flow; deny core/catalog/media/mcp. Core resolves its own browser/connector credentials inside its owner boundary |
| [TD-ROW-L2650 · L2650](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | CatalogService.SearchTitles/GetTitle/SearchPeople/GetPerson/PersonCredits/TitleCredits/ListGenres/ListRoles \| core \| Public visibility for public context; no inferred management elevation |
| [TD-ROW-L2651 · L2651](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | CatalogService.CreateRole/ManagedTitles/ManagedPeople/ManagedTitle/ManagedPerson/ManagedCredits/Audit/WriteMovie/WritePerson/WriteCredit/RemoveCredit/MovieLifecycle/PersonLifecycle/DeletionImpact/BeginArtwork/CancelArtwork \| core \| Scoped verified actor, correct permission/version/ownership and live actor recheck where required; direct TV writes denied |
| [TD-ROW-L2652 · L2652](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | CatalogService.GetOperation/ResolveArtworkAccess \| core \| Actor-scoped receipt, or authorized public/current attachment read grant as specified |
| [TD-ROW-L2653 · L2653](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | CatalogService.ProposeMovie/ApproveAndCreate/RejectProposal \| core \| Client-bound live Maintainer; proposal scope; approval/rejection additionally trusted exact UI binding/challenge |
| [TD-ROW-L2654 · L2654](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | CatalogService.GetAttachment \| media \| Exact hold/intent tuple only; PREPARING/ACTIVE/RELEASED projection cannot grant catalog management |
| [TD-ROW-L2655 · L2655](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | MediaService.Upload/ReadAsset \| core \| Verified actor and PREPARING upload intent, or current authorized attachment/grant before first byte |
| [TD-ROW-L2656 · L2656](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | MediaService.GetAsset/GetOperation \| core, catalog \| Core actor/operation scope; Catalog persisted effect/asset/hold reconciliation scope only |
| [TD-ROW-L2657 · L2657](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | MediaService.Reserve/Confirm/Release \| catalog \| Worker persisted asset/title/hold/operation tuple and monotonic version; no Core actor assertion forwarded to another audience |
| [TD-ROW-L2658 · L2658](../../../04-spoolin-techdoc.md#571-internal-method-authorization-map) | McpGatewayService.GetPolicy/GetMovieContext/ProposeMovie/ApproveMovie/RejectMovie/GetMovieOperation \| mcp \| Client-bound connector scope; trusted private UI assertion additionally for approval/rejection |
| [TD-ROW-L4353 · L4353](../../../04-spoolin-techdoc.md#request_rate_window) | `bucket_key` \| `bytea` \| `NOT NULL CHECK (octet_length(bucket_key)=32); no SQL default; owner supplies value` \| HMAC of profile/version/window duration + trusted source/account-identifier/credential dimension (§8.4); no raw identity. internal; presence/clearing §5 \| `ByteArray` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4354 · L4354](../../../04-spoolin-techdoc.md#request_rate_window) | `window_start` \| `timestamptz(6)` \| `NOT NULL; no SQL default; owner supplies value` \| UTC window aligned to the configured duration using owner database time (§8.4). internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4355 · L4355](../../../04-spoolin-techdoc.md#request_rate_window) | `hits` \| `integer` \| `NOT NULL CHECK (hits > 0); no SQL default; owner supplies value` \| Atomic INSERT/UPDATE count bounded by current MAX_REQUESTS; no decrement/refund (§8.4). internal; presence/clearing §5 \| `Int` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4356 · L4356](../../../04-spoolin-techdoc.md#request_rate_window) | `expires_at` \| `timestamptz(6)` \| `NOT NULL; no SQL default; owner supplies value` \| Window expiry. internal; presence/clearing §5 \| `Instant` → not exposed; no GraphQL/proto field |
| [TD-ROW-L5559 · L5559](../../../04-spoolin-techdoc.md#841-rate-profile-configuration) | Sign-in/account \| `RATE_LIMIT_SIGNIN_ACCOUNT_MAX_REQUESTS` / 10 \| `RATE_LIMIT_SIGNIN_ACCOUNT_WINDOW_SECONDS` / 900 \| HMAC of normalized submitted account identifier, even for nonexistent accounts; Core before Argon2id |
| [TD-ROW-L5560 · L5560](../../../04-spoolin-techdoc.md#841-rate-profile-configuration) | Sign-in/source \| `RATE_LIMIT_SIGNIN_SOURCE_MAX_REQUESTS` / 60 \| `RATE_LIMIT_SIGNIN_SOURCE_WINDOW_SECONDS` / 900 \| HMAC of trusted connection source; Core before Argon2id, alongside account profile |
| [TD-ROW-L5561 · L5561](../../../04-spoolin-techdoc.md#841-rate-profile-configuration) | Registration \| `RATE_LIMIT_REGISTER_MAX_REQUESTS` / 5 \| `RATE_LIMIT_REGISTER_WINDOW_SECONDS` / 3600 \| HMAC of trusted connection source; Core before hashing/encryption/account insertion |
| [TD-ROW-L5562 · L5562](../../../04-spoolin-techdoc.md#841-rate-profile-configuration) | Mutation \| `RATE_LIMIT_MUTATION_MAX_REQUESTS` / 60 \| `RATE_LIMIT_MUTATION_WINDOW_SECONDS` / 60 \| HMAC of verified credential identity; all other browser mutations, including draft save/discard, plus MCP propose/approve/reject at Core |
| [TD-ROW-L5563 · L5563](../../../04-spoolin-techdoc.md#841-rate-profile-configuration) | Read \| `RATE_LIMIT_READ_MAX_REQUESTS` / 120 \| `RATE_LIMIT_READ_WINDOW_SECONDS` / 60 \| Trusted source for anonymous/public browser reads and public artwork; verified credential for protected reads/private artwork and all read gateway methods; adapter-local requests use verified connector binding |
| [TD-ROW-L6032 · L6032](../../../04-spoolin-techdoc.md#111-configuration-contract) | Rate admission \| Exact RATE_LIMIT_* keys/defaults/bounds, RATE_LIMIT_KEY_FILE and MCP_RATE_LIMIT_MAX_BINDINGS in §8.4; same effective profile revision; no disable-by-zero \| Core persistent counters; MCP local resource counters only |
| [BRAIN-D05 · L76](../../../01-brainstorming-result.md#4-decision-register) | D05 \| Committed \| Design for authentication, authorization, RBAC, and audit logging from the start; implement core functionality first, then complete security |
| [BRAIN-D07 · L78](../../../01-brainstorming-result.md#4-decision-register) | D07 \| Recommended \| Viewer/Maintainer roles and predefined initial accounts; exact permission policy and account provisioning remain open |
| [BRAIN-D12 · L83](../../../01-brainstorming-result.md#4-decision-register) | D12 \| Open \| Select authentication/session mechanism and verified identity propagation through gRPC |
| [BRAIN-D23 · L94](../../../01-brainstorming-result.md#4-decision-register) | D23 \| Committed \| Health/readiness checks, correlated logs, metrics, dashboards, and actionable alerts; numerical SLOs and any SLA remain open |

## Implementation

1. Implement receiver SAN/method allowlists and Ed25519 actor assertions with exact issuer/audience/method/account/credential/role/epoch/time/nonce/operation binding, ≤30s validity, ≤2s skew and ≤4KiB size. ResolveActor remains denied to all selected network callers.
2. Implement Core live actor checks before Catalog write locks, key rotation/verification-key distribution without exposing signing keys, and trustworthy client/account credential boundaries.
3. Implement request_rate_window with every selected profile/config key/positive bound, trusted source/account/credential dimensions and atomic multi-profile admission. Deduplicate by profile plus trusted scope/key, not profile alone.
4. Preserve counters/window rules through replica/restart/config changes; fail closed when authoritative admission unavailable. Throttle before new business receipt/effect; replay attempts may consume quota without repeating business effect.
5. Keep worker/probe budgets separate and expose safe retry timing. Unit rules plus real concurrent Core replicas prove admitted counts; full operation matrix closure is T042/T043.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T008-A1:** Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- **T008-A2:** Admission: at limit−1/limit/limit+1 under coordinated two-replica calls, admissions never exceed the shared selected quota and denied requests leave no new receipt/mutation.
- **T008-A3:** Recovery: invalid zero/empty profile prevents readiness, limiter outage denies safely, and workers/probes retain their separately bounded authenticated path.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-173](../../../05-atdd.md#case-atdd-173-reject-altered-service-and-actor-assertions) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §5.7 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then service SAN and Core Ed25519 context must both verify and protected calls recheck live identity before locking And assertion lifetime≤30 s, skew≤2 s and metadata≤4 KiB cannot be bypassed And a valid assertion cannot grant another actor/method or survive revocation checked at the documented authorization point Observation: Send genuine and altered Ed25519 assertions over real mTLS for every claim/key/encoding row. Observe receiver failure before protected effects, metadata size and live CheckActor behavior; request payload role cannot override the verified actor. |
| [ATDD-174](../../../05-atdd.md#case-atdd-174-rotate-actor-verification-without-sharing-private-keys) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §5.7 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then permitted in-flight assertions remain verifiable until their bounded expiry And unknown/retired/wrong-format keys are rejected And only Core receives the PKCS8 private signer; receivers have at most four SPKI public key entries and no encryption secret Observation: Observe only Core holding the private key, then distribute new public keys, switch signer and retire old after the 60-second minimum. Exercise valid old/new and expired assertions across actual receivers; no receiver receives signing/encryption private material. |
| [ATDD-280](../../../05-atdd.md#case-atdd-280-decide-permissions-without-trusting-ui-visibility-or-contribution-roles) / Unit | Visitor, Viewer, Maintainer; public read, archive/audit/write, TV direct write; credit role Actor; A/B owned draft and operation IDs. | Then public active reads are allowed and management requires Maintainer And direct TV mutation is denied to every actor; Actor credit grants no management rights And another account’s draft/receipt is denied even to a Maintainer Observation: Assert explicit allow/deny decisions for all rows, including A/B equal catalog powers. This does not establish transport identity or server enforcement. |
| [ATDD-295](../../../05-atdd.md#case-atdd-295-verify-signed-actor-claims-with-real-in-process-cryptography) / Unit | Synthetic Ed25519 key/signature; claims audienceCatalog, methodWriteMovie, actorA, credentialC, epoch5, operationO; altered signature/key/claims, duplicateJSONkey, &gt;4 KiB, invalid duration and skew beyond2 s. | Then only correctly signed allowed-key exact-bound claims within selected lifetime/skew verify And duplicate keys, caller-supplied algorithms, oversized or mismatched claims fail safely Observation: Use actual in-process JCA signature verification for synthetic bytes; never mock a successful cryptographic check. Real mTLS, key distribution and live revocation require integration. |
| [ATDD-318](../../../05-atdd.md#case-atdd-318-validate-configurable-rate-profiles-and-trusted-scope-decisions) / Unit | F-RATE all10keys; defaults and selected min/max±1;0/negative/fraction/NaN/overflow, empty and conflicting sources; spoofed forwarded IP or requested profile. In-memory decoded Core key bytes31/32/33 with explicit required-secret/owner-only facts; MCP binding-capacity0/1/1024/100000/100001. Real file permissions are System proof, not this unit case. | Then valid positive bounded integers resolve by environment/file/default precedence And invalid settings fail with name/correction and no secret value And unverified payload/header cannot change trusted quota dimension or choose an internal exemption And Core requires a32-byte rate key and local binding capacity respects1..100000 with default1024; MCP has no permission to load the Core key Observation: Inspect resolved settings/source and profile/dimension decisions using controlled clock/identity. Real cross-replica admission, persisted windows and limiter outages require integration. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-RATE · L244](../../../05-atdd.md#f-rate-configurable-shared-admission) | The five profiles are SIGNIN_ACCOUNT10/900s, SIGNIN_SOURCE60/900s, REGISTER5/3600s, MUTATION60/60s and READ120/60s. Each has `RATE_LIMIT_<PROFILE>_MAX_REQUESTS` and `RATE_LIMIT_<PROFILE>_WINDOW_SECONDS`. MAX_REQUESTS is1..2147483647 and WINDOW_SECONDS1..86400; both are positive bounded integers. RATE_LIMIT_KEY_FILE is a protected32-byte Core secret. MCP_RATE_LIMIT_MAX_BINDINGS defaults1024 with range1..100000, no live eviction; capture actual resolved values and policy before execution. Test each selected lower/upper bound−1/bound/bound+1 and precedence by presence;0, negative, fraction, overflow, whitespace and malformed values are invalid. Two Core replicas share actual PostgreSQL atomic counters with keyed trusted dimensions and persisted windows. Prepare one remaining slot and synchronized contenders; examine admitted responses and no business effect/receipt for refusal. Public catalog/session/policy/public-artwork reads use trusted source scope even when signed in; protected reads/private artwork and all MCP gateway reads use verified credential scope, while browser/gateway writes use MUTATION credential scope. Sign-in checks both independently keyed account and source profiles in one sorted short transaction; if one denies, available companion attempt capacity is conservatively consumed without hashing or a business receipt; registration uses trusted source. Mixed GraphQL queries satisfy every distinct(profile, trusted scope/key) once and repeated aliases cannot bypass cost/depth/field limits; no caller can choose the profile or internal exemption. RATE_LIMIT_KEY_FILE is stable across ordinary restart and never shared with MCP; private profile revision and nonlogged HMAC-canary agreement prevent independent replica quotas. MAX changes apply to existing same-duration buckets; WINDOW changes start explicitly aligned new-duration buckets with no cross-revision cumulative promise. Fixed-window boundaries may admit on both sides and are not rolling windows. Adapter-local request/concurrency/message budgets complement Core quotas: keep counters across session deletion/reconnect, expire entries only after twice their applicable windows and never evict live bindings at capacity. Process restart may reset only local counters, not the authoritative Core quotas. Verified workers/probes use separate identity/concurrency/deadline controls. Limiter outage is UNAVAILABLE/HTTP503, never unlimited or a successful empty response. Quota gRPC feedback includes spoolin-error-code=RATE_LIMITED and spoolin-retry-after-ms1..86400000 mapped to existing Problem; HTTP429 includes Retry-After, resource JSON-RPC-32000 includes safe data Problem where the selected resource contract applies. The accepted benchmark keeps its warmup/duration/read/write recipe, but an isolated measurement profile must have explicitly recorded higher valid positive quotas so baseline requests measure the selected operations rather than mostly throttles. Do not disable admission, hide429/errors or change production defaults. Report throttles and invalidate an admission-dominated budget run; abuse-control cases use separate quota fixtures. |

Partial implementation contributions: [ATDD-047](../../../05-atdd.md#case-atdd-047-sign-in), [ATDD-050](../../../05-atdd.md#case-atdd-050-role-matrix), [ATDD-051](../../../05-atdd.md#case-atdd-051-bypass-attempt), [ATDD-052](../../../05-atdd.md#case-atdd-052-mixed-permittedrestricted-actions), [ATDD-085](../../../05-atdd.md#case-atdd-085-immediate-registration), [ATDD-086](../../../05-atdd.md#case-atdd-086-invalidrepeated-registration), [ATDD-097](../../../05-atdd.md#case-atdd-097-private-data-boundaries), [ATDD-099](../../../05-atdd.md#case-atdd-099-password-protection), [ATDD-113](../../../05-atdd.md#case-atdd-113-visible-effective-policy), [ATDD-115](../../../05-atdd.md#case-atdd-115-registration-name-boundaries), [ATDD-116](../../../05-atdd.md#case-atdd-116-unicode-mononyms-and-counting), [ATDD-117](../../../05-atdd.md#case-atdd-117-whitespace-absence-and-clearing), [ATDD-118](../../../05-atdd.md#case-atdd-118-other-text-boundaries), [ATDD-119](../../../05-atdd.md#case-atdd-119-email-and-source-url), [ATDD-120](../../../05-atdd.md#case-atdd-120-new-password-bounds-and-preservation), [ATDD-121](../../../05-atdd.md#case-atdd-121-plain-text-safety), [ATDD-122](../../../05-atdd.md#case-atdd-122-direct-request-validation), [ATDD-127](../../../05-atdd.md#case-atdd-127-valid-policy-override), [ATDD-128](../../../05-atdd.md#case-atdd-128-invalid-policy-configuration), [ATDD-129](../../../05-atdd.md#case-atdd-129-existing-data-after-tightening), [ATDD-131](../../../05-atdd.md#case-atdd-131-policy-changes-during-a-draftupload), [ATDD-140](../../../05-atdd.md#case-atdd-140-refresh-affected-views-only-after-confirmed-changes), [ATDD-171](../../../05-atdd.md#case-atdd-171-bound-sessions-and-revoke-verified-credentials), [ATDD-172](../../../05-atdd.md#case-atdd-172-verify-browser-origin-and-session-bound-submission), [ATDD-177](../../../05-atdd.md#case-atdd-177-enforce-shared-fixed-windows-without-identity-leakage), [ATDD-184](../../../05-atdd.md#case-atdd-184-keep-sensitive-and-unbounded-content-out-of-outputs), [ATDD-212](../../../05-atdd.md#case-atdd-212-review-security-evidence-without-certifying-the-application), [ATDD-234](../../../05-atdd.md#case-atdd-234-enforce-shared-rate-window-storage), [ATDD-258](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback), [ATDD-267](../../../05-atdd.md#case-atdd-267-inspect-policies-state-transitions-and-feedback), [ATDD-323](../../../05-atdd.md#case-atdd-323-prove-sql-and-hql-binding-across-actual-input-boundaries), [ATDD-324](../../../05-atdd.md#case-atdd-324-prove-stored-and-reflected-text-cannot-execute-in-browser-or-review-ui), [ATDD-325](../../../05-atdd.md#case-atdd-325-keep-filename-and-metadata-payloads-outside-processor-execution), [ATDD-326](../../../05-atdd.md#case-atdd-326-admit-at-most-the-shared-quota-across-concurrent-core-replicas), [ATDD-327](../../../05-atdd.md#case-atdd-327-apply-rate-configuration-through-validated-restart-with-persistent-windows), [ATDD-328](../../../05-atdd.md#case-atdd-328-fail-closed-when-the-authoritative-limiter-is-unavailable), [ATDD-334](../../../05-atdd.md#case-atdd-334-keep-worker-and-probe-budgets-separate-from-browser-quotas), [ATDD-336](../../../05-atdd.md#case-atdd-336-keep-protected-data-out-of-browser-persistence), [ATDD-337](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies), [ATDD-338](../../../05-atdd.md#case-atdd-338-prevent-private-response-caching-and-stale-history-disclosure). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T008` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T008` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
