---
kind: task
id: T007
group: G03
depends_on:
- T004
- T006
- T008
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: required
---

# T007 — Connect account and session browser journeys

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-047 | both |
| ATDD-048 | implements |
| ATDD-085 | both |
| ATDD-086 | both |
| ATDD-099 | both |
| ATDD-115 | both |
| ATDD-120 | both |
| ATDD-171 | verifies |
| ATDD-172 | verifies |
| ATDD-258 | both |
| ATDD-258-T1-R01 | both |
| ATDD-258-T1-R02 | both |
| ATDD-258-T1-R03 | both |
| ATDD-258-T1-R04 | both |
| ATDD-258-T1-R05 | both |
| ATDD-258-T1-R06 | both |
| ATDD-259 | implements |
| ATDD-259-T1-R01 | implements |
| ATDD-259-T1-R02 | implements |
| ATDD-259-T1-R03 | implements |
| ATDD-259-T1-R04 | implements |
| ATDD-259-T1-R05 | implements |
| ATDD-259-T1-R06 | implements |
| ATDD-259-T1-R07 | implements |
| ATDD-259-T1-R08 | implements |
| ATDD-259-T1-R09 | implements |
| ATDD-259-T1-R10 | implements |
| ATDD-336 | implements |
| ATDD-338 | implements |
| J-08 | both |
| FR-011 | both |
| FR-017 | both |
| NFR-015 | both |
| D05 | both |
| D07 | both |
| D12 | both |
| AC-047 | both |
| AC-047-C01 | both |
| AC-048 | both |
| AC-048-C01 | both |
| AC-048-C02 | both |
| AC-111 | both |
| AC-111-C01 | both |
| AC-112 | both |
| AC-112-C01 | both |
| AC-112-C02 | both |
| AC-125 | both |
| AC-125-C01 | both |
| AC-141 | both |
| AC-141-C01 | both |
| AC-141-C02 | both |
| AC-141-C03 | both |
| AC-146 | both |
| AC-146-C01 | both |
| AC-146-C02 | both |
| AC-146-C03 | both |
| AC-146-C04 | both |
| G-03 | both |
| G-05 | both |
| OD-001 | both |
| US-008 | both |
| US-020 | both |
| PRD-POLICY-14 | both |
| PRD-BOUND-02 | both |
| PRD-BOUND-08 | both |
| PRD-BOUND-09 | both |
| PRD-POLICY-21 | both |
| PRD-POLICY-22 | both |
| PRD-HANDOFF-11 | both |
| PRD-HANDOFF-14 | both |
| UI-account-login | both |
| UI-account-register | both |
| UI-account-registered | both |
| UI-account-registration-failure | both |
| UI-account-viewer | both |
| UI-account-maintainer | both |
| DS-POLICY-17 | implements |
| TD-C-004 | both |
| TD-C-006 | both |
| TD-C-010 | both |
| TD-C-052 | both |
| TD-THEME-STORAGE | both |
| TD-BROWSER-STORAGE | both |
| TD-CSRF-STORAGE | both |
| TD-HISTORY-REVALIDATION | both |
| TD-SESSION-PERSISTENCE | both |
| TD-SESSION-COOKIE-LIFECYCLE | both |
| TD-SESSION-DELIVERY | both |
| TD-COOKIE-RESIDUAL | both |
| TD-CWE-539 | both |
| TD-CWE-614 | both |
| TD-ASVS-SESSION-CROSSWALK | both |
| TD-U-008 | both |
| V-AUTH | both |
| TD-ROW-L3557 | both |
| TD-ROW-L3589 | both |
| TD-ROW-L3621 | both |
| TD-ROW-L3623 | both |
| TD-ROW-L5449 | both |
| TD-ROW-L5451 | both |
| TD-ROW-L5504 | both |
| TD-ROW-L5505 | both |
| TD-ROW-L5595 | both |
| TD-ROW-L5596 | both |
| TD-ROW-L5597 | both |
| TD-ROW-L5598 | both |
| TD-ROW-L5632 | both |
| TD-ROW-L5633 | both |
| TD-ROW-L5634 | both |
| TD-ROW-L5648 | both |
| TD-ROW-L5649 | both |
| TD-ROW-L5650 | both |
| TD-ROW-L5651 | both |
| TD-ROW-L5652 | both |
| TD-ROW-L5653 | both |
| TD-BLOCK-L3700 | both |
| F-BROWSER-POLICY | both |
| BRAIN-D05 | both |
| BRAIN-D07 | both |
| BRAIN-D12 | both |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T006 — Implement registration and authoritative sessions](T006-implement-registration-and-authoritative-sessions.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Registration: valid input creates exactly one Viewer eligible to sign in; duplicate/invalid input causes no partial account or misleading successful audit.
- [T008 — Enforce scoped service identity and shared admission](../../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | frontend/src/routes/sign-in/+page.svelte | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/register/+page.svelte | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/manage/+layout.svelte | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/features/session/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/transport/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/session/ | T007 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.1 Routes, screens and API dependencies](../../../04-spoolin-techdoc.md#31-routes-screens-and-api-dependencies): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.4.1 Browser storage and restored-page state](../../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [8.5.4 Session and CSRF responsibility map](../../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-259-T1-R01 · L10775](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | session-check; Authenticated candidate; hold `session` while opening a protected destination; Announce Checking your session and conceal protected fields/draft/actions. Public catalog navigation stays usable; unresolved identity grants no protected submit. |
| [ATDD-259-T1-R02 · L10776](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | auth-unavailable; Reader; fail `session` while Core can still answer public catalog reads; Show Sign-in is temporarily unavailable, no private account details or protected work, and Browse catalog. Failure is distinct from confirmed sign-out. |
| [ATDD-259-T1-R03 · L10777](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | expired; Maintainer A; expire the session during editing; Show session-expired feedback, conceal protected draft and require reauthentication; public browsing remains. Do not describe committed changes as rolled back. |
| [ATDD-259-T1-R04 · L10778](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | denied; Viewer/Visitor; navigate to a Maintainer destination; Show You do not have access and public navigation; render no protected record/draft fields or enabled management actions. |
| [ATDD-259-T1-R05 · L10779](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | recheck; Maintainer A; reauthenticate with an unconfirmed prior save; Show Check before continuing; resolve the original owner operation and current eligibility before restoring/submitting draft values. Do not offer a blind new save UUID. |
| [ATDD-259-T1-R06 · L10780](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | other-account; Account B; sign in after A's editing session ends; Show draft-unavailable-to-this-account feedback with original-owner sign-in/public browse actions. A's draft values, identifiers and password recovery never appear. |
| [ATDD-259-T1-R07 · L10781](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | signout; Signed-in account; choose sign-out from the session menu; Explain that the session ends and protected work becomes unavailable to the next account; offer confirmation/cancel where shown. An uncertain save is not undone by sign-out. |
| [ATDD-259-T1-R08 · L10782](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | signed-out; Signed-in account; receive confirmed successful `signOut`; Show Signed out, remove protected DOM/controller/cache state and offer public browsing. Do not restore protected work on browser Back. |
| [ATDD-259-T1-R09 · L10783](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | signout-failed; Signed-in account; receive confirmed failed `signOut` before revocation; Show Sign-out did not complete and retry; retain the known active-session identity. Do not claim sign-out or transfer its protected work to another account. |
| [ATDD-259-T1-R10 · L10784](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | signout-uncertain; Signed-in account; lose the `signOut` response; Show Sign-out is not confirmed, conceal protected content/submission and recheck the original result/session. Claim neither active authorization nor confirmed revocation until reconciled. |
| [J-08 · L93](../../../02-PRD.md#32-application-journeys) | Visitor creates an account \| Name, email, and password available \| Register immediately as a Viewer, then sign in without email verification |
| [FR-011 · L247](../../../02-PRD.md#61-functional-requirements) | Register immediate Viewer accounts, provide demo accounts, and support sign-in/sign-out \| D05 Committed; approved account scope Committed \| AC-047 through AC-049, AC-111, AC-112, AC-141/AC-142, AC-145/AC-146 |
| [FR-017 · L253](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-015 · L275](../../../02-PRD.md#62-quality-and-delivery-requirements) | Protect account PII with the selected encryption/rotation constraint; passwords use one-way hashing \| Committed product policy (section 13.1) \| AC-123 through AC-125, AC-127, AC-128; section 10 |
| [D05 · L308](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D07 · L310](../../../02-PRD.md#64-project-decision-register) | Viewer/Maintainer and prepared accounts \| Viewer/Maintainer roles and prepared accounts are adopted as Committed policy in section 13.1; FR-011/FR-012 / AC-049, AC-050, AC-111 |
| [D12 · L315](../../../02-PRD.md#64-project-decision-register) | Authentication mechanism and verified internal identity \| Product access settled; mechanics Open in techdoc. FR-011/FR-012 / AC-047 through AC-052 |
| [AC-047 · L437](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Sign in \| A prepared or registered account exists \| Correct or incorrect credentials are submitted \| Correct credentials permit sign-in with the assigned role; invalid credentials do not grant access and receive non-sensitive feedback. |
| [AC-047-C01 · L437](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Correct credentials permit sign-in with the assigned role; invalid credentials do not grant access and receive non-sensitive feedback. |
| [AC-048 · L438](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Expiry/sign-out \| A user signs out or their sign-in expires during work \| They attempt another restricted action \| Require sign-in again; public browsing remains available. Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-048-C01 · L438](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Require sign-in again; public browsing remains available. |
| [AC-048-C02 · L438](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-111 · L443](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Immediate registration \| A visitor supplies valid name, email, and password for a new account \| Registration succeeds \| Create a Viewer eligible to sign in immediately, without email verification; do not allow registration as Maintainer. |
| [AC-111-C01 · L443](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Create a Viewer eligible to sign in immediately, without email verification; do not allow registration as Maintainer. |
| [AC-112 · L444](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Invalid/repeated registration \| A required field is missing, a registration value violates the effective policy in section 5.5, email is invalid/already used, or submission is repeated \| Registration is attempted \| Show safe corrective feedback, create no duplicate account, and do not expose whether another account contains private details. Never echo/store the password in recovery drafts or logs. |
| [AC-112-C01 · L444](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Show safe corrective feedback, create no duplicate account, and do not expose whether another account contains private details. |
| [AC-112-C02 · L444](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Never echo/store the password in recovery drafts or logs. |
| [AC-125 · L522](../../../02-PRD.md#us-020-protect-private-account-information) | Password protection \| A user has registered or signed in \| Credential storage and outputs are checked by authorized assessment reviewers \| Passwords are protected by one-way password hashing, cannot be retrieved as an account feature, and never appear in readable storage, audit, logs, or AI payloads. |
| [AC-125-C01 · L522](../../../02-PRD.md#us-020-protect-private-account-information) | Passwords are protected by one-way password hashing, cannot be retrieved as an account feature, and never appear in readable storage, audit, logs, or AI payloads. |
| [AC-141 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Registration-name boundaries \| Registration-name defaults are configured and remaining registration data are valid \| A visitor submits a name of 2, 100, 1, or 101 code points after trimming \| Accept lengths 2 and 100 and create the Viewer on confirmed success. Reject lengths 1 and 101 without creating an account. Explain the configured length restriction without claiming the person's name is unreal or unauthentic. |
| [AC-141-C01 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Accept lengths 2 and 100 and create the Viewer on confirmed success. |
| [AC-141-C02 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject lengths 1 and 101 without creating an account. |
| [AC-141-C03 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Explain the configured length restriction without claiming the person's name is unreal or unauthentic. |
| [AC-146 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | New-password bounds and preservation \| Recommended password bounds of 15 to 128 are configured \| A visitor supplies passwords of 14, 15, 128, or 129 code points, including supported Unicode and whitespace \| Reject 14 and 129 for length; 15 and 128 pass the length check subject to the adopted credential policy under OD-001. Evaluate the exact entered password without trimming, transformation, or truncation. No password appears in recovery drafts, audit, or diagnostics. Successful sign-in later accepts that same credential under its account policy. |
| [AC-146-C01 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject 14 and 129 for length; 15 and 128 pass the length check subject to the adopted credential policy under OD-001. |
| [AC-146-C02 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Evaluate the exact entered password without trimming, transformation, or truncation. |
| [AC-146-C03 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | No password appears in recovery drafts, audit, or diagnostics. |
| [AC-146-C04 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Successful sign-in later accepts that same credential under its account policy. |
| [G-03 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-05 · L928](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [OD-001 · L1044](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Product access resolved; authentication/session, expiry, account provisioning, credential-strength checks and adoption of Recommended password bounds, credential protection, and verified service/MCP identity remain for techdoc \| FR-011/FR-012/FR-017, NFR-015/NFR-016 \| Technical design owner unassigned; before identity-dependent implementation |
| [US-008 · L431](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | ### US-008: Register, sign in, and enforce access As a visitor, I want an account I can use immediately; as a signed-in user, I want clear permissions. Requirements: FR-011, FR-012, NFR-015. Ratings/reviews remain deferred, so Viewer registration does not unlock a new catalog-writing feature. |
| [US-020 · L514](../../../02-PRD.md#us-020-protect-private-account-information) | ### US-020: Protect private account information As an account holder, I want my private information protected while I can continue to use my account. Requirement: NFR-015. The encryption constraints are stated in section 10; the techdoc will define their implementation. |
| [PRD-POLICY-14 · L146](../../../02-PRD.md#53-accounts-audit-and-ai-boundaries) | Registration collects name, email, and password and immediately creates a Viewer. Prepared Viewer/Maintainer accounts support the demo. Users cannot choose Maintainer during registration. Sign-in and sign-out are included; public reading remains available after sign-out. Invalid registration/sign-in has understandable feedback without exposing credentials or another account's information. |
| [PRD-BOUND-02 · L171](../../../02-PRD.md#name-defaults) | Registration name \| 2 characters \| 100 characters \| Committed selected defaults: a one-character account name is excluded under the default configuration |
| [PRD-BOUND-08 · L186](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Registration email \| Valid, nonempty email \| 254 characters \| Length does not replace format or existing-account checks |
| [PRD-BOUND-09 · L187](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Registration password \| 15 characters \| 128 characters \| Apply to new passwords, not to the validity of an existing account's credential |
| [PRD-POLICY-21 · L190](../../../02-PRD.md#recommended-defaults-for-other-inputs) | The password recommendation follows OWASP authentication guidance for accounts without MFA. Exact email/URL syntax, credential-strength checks, and validation mechanisms remain technical-design work under OD-001/OD-003/OD-019. Other exposed fields, including numerical metadata and collection bounds where applicable, must receive documented limits in technical design; this does not add metadata fields or management screens to the release. |
| [PRD-POLICY-22 · L194](../../../02-PRD.md#validation-safe-handling-and-feedback) | Bounds are inclusive and independently configurable for each applicable field. Required fields remain required, and their effective minimum cannot allow an empty value. Measure text length consistently in Unicode code points. Trim surrounding whitespace from ordinary text before counting and validation while preserving internal spaces, legitimate punctuation, accents, and meaningful synopsis line breaks. Never trim, transform, or silently truncate a password. No field is silently shortened to fit a limit. |
| [PRD-HANDOFF-11 · L846](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | Account data protection requirements; NFR-015 \| Committed \| AES-256-GCM for recoverable account PII; versioned keys, authorized reads during rotation, safe re-encryption and retirement; one-way password hashing. Validate adaptation to this stack, backup/recovery implications, and safe failure behavior. |
| [PRD-HANDOFF-14 · L849](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D07/D08/D09 \| Product choices adopted only where stated \| Viewer/Maintainer/prepared accounts are selected; stale-save protection is selected. Queries/mutations without subscriptions and the specific optimistic-concurrency mechanism remain engineering recommendations. |
| [DS-POLICY-17 · L96](../../../03-design-system.md#visible-interaction-contracts) | Session checking conceals protected work. Expiry or reauthentication preserves permitted draft ownership without implying committed work rolled back. Resume only after same-account, permission, target, and original-operation checks. A different account must not see or inherit the draft. A permitted intended destination returns after sign-in; a disallowed destination falls back to permitted content. Confirmed sign-out conceals drafts; failed sign-out retains the active session; uncertain sign-out conceals private content while checking, without claiming completion. |
| [TD-C-004 · L824](../../../04-spoolin-techdoc.md#31-routes-screens-and-api-dependencies) | Implement each declared browser route, role guard, session-before-protected-render and safe internal intended redirect; identity outage hides protected state while reachable public reads remain independent. |
| [TD-C-006 · L943](../../../04-spoolin-techdoc.md#32-feature-organization-and-generated-requests) | Generate named GraphQL operation types; use typed fetch and real XHR upload events, deadlines/cancellation and same-origin CSRF. Long stays decimal string. Feature controllers own all request/state resources. |
| [TD-C-010 · L985](../../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state) | Theme-only production localStorage key spoolin.theme dark/light; invalid removal/system fallback/current-page persistence failure; no other persistent application storage. pagehide concealment and pageshow current-session verification prevent stale protected restoration. |
| [TD-C-052 · L5587](../../../04-spoolin-techdoc.md#85-browser-storage-and-session-cookie-controls) | Session nonpersistent Secure HttpOnly delivery-only cookie and confirmed deletion, readable anti-forgery exception, lifetime/residual browser restoration checks; real storage/network/DOM instrumentation with CWE539/614 and ASVS versioned references. |
| [TD-THEME-STORAGE · L991](../../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state) | TD-THEME-STORAGE \| Theme preference \| One production localStorage key, `spoolin.theme`, with exactly `dark` or `light`; no account ID or other fields |
| [TD-BROWSER-STORAGE · L992](../../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state) | TD-BROWSER-STORAGE \| Credentials, account projections, drafts, approval challenges and API responses \| No localStorage, sessionStorage, IndexedDB or application-managed cache persistence; permitted temporary feature state remains in memory |
| [TD-CSRF-STORAGE · L993](../../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state) | TD-CSRF-STORAGE \| Browser anti-forgery value \| Readable through the existing session/anonymous CSRF mechanism; frontend retains it in memory for the request header, never as a persisted login credential |
| [TD-HISTORY-REVALIDATION · L995](../../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state) | TD-HISTORY-REVALIDATION \| Logout, expiry, account change and restored pages \| Conceal/clear protected DOM and controller projections, abort requests and revoke object URLs; revalidate the current session before protected state becomes usable again |
| [TD-SESSION-PERSISTENCE · L5595](../../../04-spoolin-techdoc.md#851-cookie-lifecycle-contract) | TD-SESSION-PERSISTENCE \| Core issues/renews `__Host-spoolin_session` without Expires or Max-Age; no remember-me or persistent refresh-cookie alternative \| Actual Set-Cookie headers and browser cookie attributes on every issuing/renewing identity path |
| [TD-SESSION-COOKIE-LIFECYCLE · L5596](../../../04-spoolin-techdoc.md#851-cookie-lifecycle-contract) | TD-SESSION-COOKIE-LIFECYCLE \| Secure, HttpOnly, SameSite=Lax, Path=/ and absent Domain on session issuance/renewal; confirmed logout deletes with Max-Age=0 and identical name/path/security scope \| Core revocation and browser deletion, including a lost logout response; UI cleanup does not fabricate successful server revocation |
| [TD-SESSION-DELIVERY · L5597](../../../04-spoolin-techdoc.md#851-cookie-lifecycle-contract) | TD-SESSION-DELIVERY \| The opaque 32-random-byte session credential is transferred only by Set-Cookie; Core retains its SHA-256 hash \| No raw value in JSON/URLs/storage/receipts/logs; declared csrfToken remains readable anti-forgery data |
| [TD-COOKIE-RESIDUAL · L5598](../../../04-spoolin-techdoc.md#851-cookie-lifecycle-contract) | TD-COOKIE-RESIDUAL \| Browser restoration may restore a session cookie; Core's existing idle/absolute expiry and revocation remain authoritative \| Restored valid sessions follow existing checks; expired/revoked credentials fail even when the browser still has a cookie |
| [TD-CWE-539 · L5632](../../../04-spoolin-techdoc.md#853-cwe-and-asvs-verification-crosswalk) | TD-CWE-539 / sensitive persistent cookies \| Core uses non-persistent opaque session cookies; no sensitive fields or persistent token alternative; server expiry/revocation limits restored cookies \| Historical v4.0.3-3.2.3 allowed secured cookies or sessionStorage. Current v5.0.0-14.3.3 covers browser sensitive-data storage with a session-token exception; non-persistent cookie issuance is SpoolIn's stricter selected policy. Observe headers, stores and restored expired/revoked denial |
| [TD-CWE-614 · L5633](../../../04-spoolin-techdoc.md#853-cwe-and-asvs-verification-crosswalk) | TD-CWE-614 / missing Secure \| Core always sets Secure on session cookies, retaining locally trusted HTTPS and fail-closed prerequisites \| Historical v4.0.3-3.4.1; current v5.0.0-3.3.1. Inspect actual issuance/renewal/deletion attributes and configured transport |
| [TD-ASVS-SESSION-CROSSWALK · L5634](../../../04-spoolin-techdoc.md#853-cwe-and-asvs-verification-crosswalk) | TD-ASVS-SESSION-CROSSWALK / companion controls \| Core/session and frontend jointly enforce cookie delivery, anti-forgery, cleanup and response-cache rules \| v5.0.0-3.3.2 SameSite; -3.3.3 host prefix; -3.3.4 HttpOnly/Set-Cookie-only credentials; -14.3.1 client cleanup; -14.3.2 no-store; -7.3.1/-7.3.2 timeouts; -7.4.1 termination |
| [TD-U-008 · L6542](../../../04-spoolin-techdoc.md#appendix-a1-baseline-decisions-and-requirement-changes) | TD-U-008 \| Theme-only JavaScript storage; non-persistent Secure HttpOnly session cookies; explicit CWE-539/614 coverage \| Production `spoolin.theme` dark/light only; Core owns cookie lifecycle and server expiry. ASVS 4.0.3 references retain historical context; current mappings use 5.0.0. No database/wire schema change |
| [V-AUTH · L6391](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-AUTH \| Prepared/registered Viewer/Maintainer, forged roles, expired/revoked/fixed sessions, bad Origin/CSRF, login policy changes, account-switched drafts and the __Host-spoolin_session cookie; assert owner isolation and generic denial, exact passwords and no secret leakage; draft replay original IDs/versions after later edit/discard/purge without resurrecting content |
| [TD-ROW-L3557 · L3557](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | bootstrap \| Core operation_receipt: version→setup_version, manifest_hash→setup_manifest_hash, viewer_id/maintainer_id→setup_* IDs; created_at retained \| Unique version/checksum, account FKs, permanent evidence and no account reset |
| [TD-ROW-L3589 · L3589](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `user_account` \| Core `account/persistence` \| Permanent identity, role, encrypted PII and password verifier |
| [TD-ROW-L3621 · L3621](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `user_account`, `browser_session` \| Owning Session projection and cookie handling \| Decrypt only permitted display name; no hashes, token, key material or account email in routine output |
| [TD-ROW-L3623 · L3623](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `form_draft` \| Authorized Draft \| Decrypt account-owned permitted body; original Catalog operationId remains separate from Core save/discard receipt |
| [TD-ROW-L5449 · L5449](../../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications) | smtp \| Fixed SMTP endpoint, TLS verification, username/password as needed, fixed from/to \| SMTP acceptance can set ACCEPTED with server response/message ID; this is not proof inbox delivery. DELIVERY_CONFIRMED only if authenticated provider evidence exists. Ambiguous post-DATA timeout→UNKNOWN |
| [TD-ROW-L5451 · L5451](../../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications) | google_chat \| Secret fixed space webhook/credential, verified HTTPS \| Provider success/reference→ACCEPTED; same uncertainty and bounded failure semantics |
| [TD-ROW-L5504 · L5504](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-02 Core→Catalog/Media \| Spoofed actor/service escalates permission \| mTLS SAN + audience assertion + live destructive recheck; access control/API \| Forged role/header/audience, Viewer writes, cross-service credential; in-flight revocation race documented; backend owner |
| [TD-ROW-L5505 · L5505](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-03 DB/backup \| Account PII disclosure or duplicate account \| AEAD, separate HMAC aliases, least privilege; data protection/cryptography \| DB-only exfiltration lacks plaintext; tamper AAD/tag/key, rotate aliases concurrently; host/key compromise remains; security/operator |
| [TD-ROW-L5595 · L5595](../../../04-spoolin-techdoc.md#851-cookie-lifecycle-contract) | TD-SESSION-PERSISTENCE \| Core issues/renews `__Host-spoolin_session` without Expires or Max-Age; no remember-me or persistent refresh-cookie alternative \| Actual Set-Cookie headers and browser cookie attributes on every issuing/renewing identity path |
| [TD-ROW-L5596 · L5596](../../../04-spoolin-techdoc.md#851-cookie-lifecycle-contract) | TD-SESSION-COOKIE-LIFECYCLE \| Secure, HttpOnly, SameSite=Lax, Path=/ and absent Domain on session issuance/renewal; confirmed logout deletes with Max-Age=0 and identical name/path/security scope \| Core revocation and browser deletion, including a lost logout response; UI cleanup does not fabricate successful server revocation |
| [TD-ROW-L5597 · L5597](../../../04-spoolin-techdoc.md#851-cookie-lifecycle-contract) | TD-SESSION-DELIVERY \| The opaque 32-random-byte session credential is transferred only by Set-Cookie; Core retains its SHA-256 hash \| No raw value in JSON/URLs/storage/receipts/logs; declared csrfToken remains readable anti-forgery data |
| [TD-ROW-L5598 · L5598](../../../04-spoolin-techdoc.md#851-cookie-lifecycle-contract) | TD-COOKIE-RESIDUAL \| Browser restoration may restore a session cookie; Core's existing idle/absolute expiry and revocation remain authoritative \| Restored valid sessions follow existing checks; expired/revoked credentials fail even when the browser still has a cookie |
| [TD-ROW-L5632 · L5632](../../../04-spoolin-techdoc.md#853-cwe-and-asvs-verification-crosswalk) | TD-CWE-539 / sensitive persistent cookies \| Core uses non-persistent opaque session cookies; no sensitive fields or persistent token alternative; server expiry/revocation limits restored cookies \| Historical v4.0.3-3.2.3 allowed secured cookies or sessionStorage. Current v5.0.0-14.3.3 covers browser sensitive-data storage with a session-token exception; non-persistent cookie issuance is SpoolIn's stricter selected policy. Observe headers, stores and restored expired/revoked denial |
| [TD-ROW-L5633 · L5633](../../../04-spoolin-techdoc.md#853-cwe-and-asvs-verification-crosswalk) | TD-CWE-614 / missing Secure \| Core always sets Secure on session cookies, retaining locally trusted HTTPS and fail-closed prerequisites \| Historical v4.0.3-3.4.1; current v5.0.0-3.3.1. Inspect actual issuance/renewal/deletion attributes and configured transport |
| [TD-ROW-L5634 · L5634](../../../04-spoolin-techdoc.md#853-cwe-and-asvs-verification-crosswalk) | TD-ASVS-SESSION-CROSSWALK / companion controls \| Core/session and frontend jointly enforce cookie delivery, anti-forgery, cleanup and response-cache rules \| v5.0.0-3.3.2 SameSite; -3.3.3 host prefix; -3.3.4 HttpOnly/Set-Cookie-only credentials; -14.3.1 client cleanup; -14.3.2 no-store; -7.3.1/-7.3.2 timeouts; -7.4.1 termination |
| [TD-ROW-L5648 · L5648](../../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map) | Anonymous `session` \| Same-origin read with Host/Origin/client checks; supplies the existing signed readable anti-forgery cookie/value; no mutation header is needed to bootstrap it \| Keep the CSRF value in memory. ATDD-172 covers bootstrap and mutation rejection |
| [TD-ROW-L5649 · L5649](../../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map) | Browser GraphQL read \| Host/Origin/client checks and the applicable rate profile; private reads additionally verify session and permissions. Reads do not acquire mutation-only CSRF/version/policy arguments \| Rebuild authenticated CSRF state from `session` after reload; never read the HttpOnly credential. Session/storage checks: ATDD-171/336/337 |
| [TD-ROW-L5650 · L5650](../../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map) | Register/sign-in or authenticated mutation \| Require the appropriate anonymous or session-derived `X-CSRF-Token` as well as the existing identity/permission checks. SameSite does not replace this check \| Invalid/missing tokens deny before effects; do not persist either kind of CSRF value in a JavaScript store |
| [TD-ROW-L5651 · L5651](../../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map) | Confirmed sign-out \| Revoke the server session and return the matching deletion header. A cookie deletion alone does not prove revocation \| Clear/lock protected projections immediately; reconcile the original intent if the reply is lost. ATDD-171/337/338 |
| [TD-ROW-L5652 · L5652](../../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map) | Idle/absolute expiry or revocation \| Reject protected work when the existing server deadlines/epoch checks fail, even if a restored browser still sends a cookie \| Conceal protected views and recover only after a valid same-account session and existing draft checks. ATDD-171/179/337/338 |
| [TD-ROW-L5653 · L5653](../../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map) | Delayed reply or history restoration \| Every GraphQL response uses no-store; server authorization remains independent of page cleanup \| Accept a response only for the still-current controller/account/session generation. Revalidate restored pages before revealing protected state; no stale reply can unlock another account's view |
| [TD-BLOCK-L3700 · L3700](../../../04-spoolin-techdoc.md#operation_receipt-ddl) | operation_receipt DDL — complete sql contract, all members, constraints and field numbers |
| [BRAIN-D05 · L76](../../../01-brainstorming-result.md#4-decision-register) | D05 \| Committed \| Design for authentication, authorization, RBAC, and audit logging from the start; implement core functionality first, then complete security |
| [BRAIN-D07 · L78](../../../01-brainstorming-result.md#4-decision-register) | D07 \| Recommended \| Viewer/Maintainer roles and predefined initial accounts; exact permission policy and account provisioning remain open |
| [BRAIN-D12 · L83](../../../01-brainstorming-result.md#4-decision-register) | D12 \| Open \| Select authentication/session mechanism and verified identity propagation through gRPC |

## Implementation

1. Implement sign-in/register/bootstrap/sign-out controllers and operation documents; use autocomplete name/email/current-password/new-password correctly and display current effective policy without applying new-password minimum to login.
2. Bind SessionBoundary to verified identity, safe intended return URL and current generation. While checking, expired or uncertain, conceal protected DOM/controller state and abort obsolete replies; public browsing remains reachable.
3. Handle confirmed sign-out, failed sign-out retaining the active session, and uncertain sign-out concealing private content while reconciling. Do not infer rollback of already committed work.
4. On pagehide conceal and clear private projections/revoke object URLs; on pageshow revalidate identity before restoring permitted views. Different-account recovery is denied; draft controller integration is T015/T016.
5. Run account/session state rows through real Core HTTPS, including keyboard submission and announcement/focus observations, not fixture timers.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T007-A1:** Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- **T007-A2:** Feedback: invalid/repeated registration retains permitted non-secret fields, never password; safe errors reveal no other account details.
- **T007-A3:** History: restored private pages and delayed responses remain concealed until current same-account verification; uncertain logout cannot claim completion.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-047](../../../05-atdd.md#case-atdd-047-sign-in) / End-to-end | CorrectpreparedViewer/Maintainer password; wrong password; unknown email; invalid syntax; generic safe errors and assigned role. | Then Correct credentials permit sign-in with the assigned role; invalid credentials do not grant access and receive non-sensitive feedback. Observation: Inspect real session issuance and assigned Viewer/Maintainer role on correct exact password; wrong/unknown/syntax failures remain generic and unauthenticated. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-085](../../../05-atdd.md#case-atdd-085-immediate-registration) / End-to-end | Visitorregister"Li",new"li@example.test",exact15charpassword; Viewercreated, no auto sign-in, email verification orrole choice; then signin. | Then Create a Viewer eligible to sign in immediately, without email verification; do not allow registration as Maintainer. Observation: Register valid Viewer without email verification/role selection/automatic sign-in; inspect one protected account and subsequent explicit sign-in/public experience. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-086](../../../05-atdd.md#case-atdd-086-invalidrepeated-registration) / End-to-end | Missing/invalid name/email/password, existing normalized email, repeated same UUID andconcurrentdifferentUUIDsameemail; generic failure, no duplicates or password draft. | Then Show safe corrective feedback, create no duplicate account, and do not expose whether another account contains private details. Never echo/store the password in recovery drafts or logs. Observation: Inspect invalid/generic duplicate registration feedback and retained nonsecret fields, excluding password drafts; repeated same intent cannot create a session or extra account. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-099](../../../05-atdd.md#case-atdd-099-password-protection) / End-to-end | ExactpasswordPHChash inspect with authorized reviewer; no decryptable password feature or readable password anywhere; correct signin still works. | Then Passwords are protected by one-way password hashing, cannot be retrieved as an account feature, and never appear in readable storage, audit, logs, or AI payloads. Observation: Inspect exact-password verification and nonplaintext credential storage through actual bounded Argon2id; password/hash never enters draft/audit/model/telemetry. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-115](../../../05-atdd.md#case-atdd-115-registration-name-boundaries) / End-to-end | Repeat("x",1/2/100/101) as registrationname; only2and100 pass; explain configured minimum, notnameauthenticity. | Then Accept lengths 2 and 100 and create the Viewer on confirmed success. Reject lengths 1 and 101 without creating an account. Explain the configured length restriction without claiming the person's name is unreal or unauthentic. Observation: Submit registration-name2..100 boundary rows, distinguish catalog-person minimum1, and inspect valid encrypted registration versus nonsecret retained errors. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-120](../../../05-atdd.md#case-atdd-120-new-password-bounds-and-preservation) / End-to-end | Exactpasswordrepeatx14/15/128/129 code points; whitespace/composed/decomposed/Unicode rows; preserve512UTF8byte ceiling; old credential after tightening. | Then Reject 14 and 129 for length; 15 and 128 pass the length check subject to the adopted credential policy under OD-001. Evaluate the exact entered password without trimming, transformation, or truncation. No password appears in recovery drafts, audit, or diagnostics. Successful sign-in later accepts that same credential under its account policy. Observation: Inspect exact new-password bounds/no-trim behavior and successful existing credential after tighter new-input policy; no password appears in recovery data. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-171](../../../05-atdd.md#case-atdd-171-bound-sessions-and-revoke-verified-credentials) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §8 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. Exact target cookie __Host-spoolin_session; valid token under obsolete cookie name alone does not authenticate a protected request. | Then new sign-in rotates session and CSRF with Secure/HttpOnly/SameSite=Lax/Path=/ and no Domain And only256-bit token hashes persist; five active sessions/account and30 minidle/12 habsolute hold And expired/revoked/fixed tokens deny protected actions; lost reply re-verifies credentials and never retrieves raw cookie from a receipt Observation: Inspect the exact __Host-spoolin_session cookie name and HttpOnly/Secure/SameSite=Lax/Path=/ with absent Domain, Inspect actual Set-Cookie attributes, hashed token storage, rotation, five-session races and account epoch. Apply F-EXPIRY to idle/absolute/credential cutoffs; a present expired row is denied before purge, while public browsing remains available. Selected cookie lifetime: inspect every actual issuing/renewing identity response over the configured trusted HTTPS listener. __Host-spoolin_session retains Secure/HttpOnly/SameSite=Lax/Path=/ and absent Domain; issuance/renewal omit both Expires and Max-Age. Confirmed signOut invalidates Core and deletes with Max-Age=0 using matching scope/security attributes. Response loss remains uncertain until existing result/session reconciliation; no raw credential is recovered from a receipt. Browser closure is not revocation: actual restoration of a still-valid session can resume only after current checks, while independently expired/revoked restored cookies are denied. No remember-me/JWT/refresh-cookie alternative is introduced. Complement ATDD-337 for real header/browser lifecycle evidence. |
| [ATDD-172](../../../05-atdd.md#case-atdd-172-verify-browser-origin-and-session-bound-submission) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §8 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then valid same-origin sessions retain their reconstructible token across reload/tabs/restart And forged or missing browser boundary values deny mutation before domain dispatch And session-csrf-v1 plusNUL/rawtoken HMAC and constant-time matching apply; key rotation invalidates prior sessions Observation: Capture real Origin/Host/content-type/web-header/CSRF matrix for anonymous and authenticated requests. Verify valid reload/parallel continuity and constant-time verification boundary; wrong/missing tokens create no protected mutation and diagnostic output stays safe. Authentication credential is HttpOnly and Set-Cookie-only; anonymous signed double-submit cookie and declared Session.csrfToken intentionally remain readable anti-forgery material. The frontend keeps the header value in memory, not localStorage/sessionStorage/IndexedDB/cache or a login-token substitute. Verify sign-in rotation and reconstruction after reload/tabs/Core restart without persisting the CSRF value; missing/forged token still denies mutation. browser_session.csrf_hash is SHA-256 of the encoded derived HMAC token using the existing purpose string/NUL/decoded raw32byte session input; physical32byte SQL shape and the public schema are unchanged. A blanket HttpOnly/forbid-all-token rule fails the valid anonymous/authenticated controls. Secondary cookie purposes: capture all authentication, signed registration-scope and anonymous anti-forgery Set-Cookie paths independently. Issuance has Secure, Path=/, no Domain, Expires or Max-Age; the credential and non-script signed scope have HttpOnly, while the existing anonymous anti-forgery cookie stays readable and SameSite=Lax. Confirm the signed value/scope is verified before registration effects; reading a CSRF value never authenticates an account. A same-origin session/read does not require the mutation-only X-CSRF-Token header; missing/wrong header still denies register/sign-in and other mutations. Compare reconstructed CSRF across reload/tabs/restart without persistent JS writes. Hold an A-bound session response, change controller/account/session generation, then release it: it must not replace the newer in-memory session/CSRF context or unlock protected work. Confirmed logout and expiry remain the existing owner checks, with batching last_seen updates unable to extend absolute lifetime. |
| [ATDD-258](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) / End-to-end | F-UI; each state row uses F-CATALOG/F-ACTORS and its source-defined stage. Replace local fixture IDs with deterministic actual IDs; no simulated save/upload/client result counts as execution evidence. | Then the visible heading, identity context, feedback and action match the row oracle And each outcome actually applicable to this flow remains distinct from its pending, failed or unavailable alternatives And theme, width and keyboard changes preserve state and show no restricted data or unsupported feature Observation: Exercise `session`, `policy`, `signIn`, `register` in `/sign-in`, `/register`, capturing the current response/operation and each row’s visible heading, identity, controls, retained values and safe next action. Assert the named SessionForm / ErrorSummary through actual visible layout and accessible names/status rather than hidden fixture text. Run each row independently in both themes at declared widths; verify keyboard/focus and permitted navigation without leaking protected data. Source fragments identify design references, not implementation selectors. \| State reference \| Actor / independent trigger \| Visible semantic oracle \| \| --- \| --- \| --- \| \| login \| Visitor; open `/sign-in`, submit valid and invalid credentials independently \| Show labeled email/password fields, existing-credential guidance, registration and public-browse links. Failure is generic; success follows assigned role and a permitted internal destination, without recovering or echoing passwords. \| \| register \| Visitor; open `/register` with current `policy` \| Show name/email/password guidance using effective limits, Create Viewer account and Sign in. Role selection, verification and password recovery are absent; password bytes stay exact. \| \| registered \| Visitor; receive confirmed successful `register` \| Show account-ready feedback and Sign in; Viewer is assigned, no verification email or automatic sign-in is implied, and the password field is cleared. \| \| registration-failure \| Visitor; submit invalid/duplicate data or replay registration \| Show safe field correction/generic account feedback, retained permitted non-secret values and Sign in. Disclose no other account and claim no duplicate creation; password is excluded from recovery. \| \| viewer \| Viewer; receive verified `session` after sign-in \| Show public catalog access and sign-out, no management navigation; restore a permitted intended public destination and reject a restricted workspace/external return target. \| \| maintainer \| Maintainer A; sign in with interrupted own work \| Show permitted management navigation; resume only A's current eligible draft after target/version/policy/permission and original-operation checks. Offer sign-out without granting access from a fixture role label. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-258-T1-R01 · L10728](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | login; Visitor; open `/sign-in`, submit valid and invalid credentials independently; Show labeled email/password fields, existing-credential guidance, registration and public-browse links. Failure is generic; success follows assigned role and a permitted internal destination, without recovering or echoing passwords. |
| [ATDD-258-T1-R02 · L10729](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | register; Visitor; open `/register` with current `policy`; Show name/email/password guidance using effective limits, Create Viewer account and Sign in. Role selection, verification and password recovery are absent; password bytes stay exact. |
| [ATDD-258-T1-R03 · L10730](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | registered; Visitor; receive confirmed successful `register`; Show account-ready feedback and Sign in; Viewer is assigned, no verification email or automatic sign-in is implied, and the password field is cleared. |
| [ATDD-258-T1-R04 · L10731](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | registration-failure; Visitor; submit invalid/duplicate data or replay registration; Show safe field correction/generic account feedback, retained permitted non-secret values and Sign in. Disclose no other account and claim no duplicate creation; password is excluded from recovery. |
| [ATDD-258-T1-R05 · L10732](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | viewer; Viewer; receive verified `session` after sign-in; Show public catalog access and sign-out, no management navigation; restore a permitted intended public destination and reject a restricted workspace/external return target. |
| [ATDD-258-T1-R06 · L10733](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | maintainer; Maintainer A; sign in with interrupted own work; Show permitted management navigation; resume only A's current eligible draft after target/version/policy/permission and original-operation checks. Offer sign-out without granting access from a fixture role label. |
| [F-BROWSER-POLICY · L111](../../../05-atdd.md#f-browser-policy-scoped-future-browser-security-evidence) | Use the production storage/restore policy and cookie lifecycle through a unique disposable production browser origin/profile and real trusted HTTPS/Core. Keep synthetic A/B and storage/cookie canaries isolated; record only redacted attributes, classifications, booleans and permitted IDs. Observe application calls before startup plus intermediate/final browser stores; a clean store alone is no absence proof. Cookie capture is privileged transient test-driver memory, never an application store or raw report/HAR. Test frontend and served review code, keeping real chat-host isolation as its separate required evidence. Controlled API/storage failures are labeled; unit spies cannot satisfy real browser/identity obligations. The one production localStorage entry is spoolin.theme with exact dark/light. Prototype movie-design-theme is only presentation evidence. No protected data, encrypted drafts or API responses are placed in JavaScript-accessible persistence/history; allowed transient CSRF/non-secret form memory remains distinct from authentication. No-store applies to every GraphQL response, with existing artwork policy and static caching intact. Browser-owned restore/password manager/profile files are outside application storage control; do not claim their physical erasure or use browser closure/localhost plaintext/port differences as security oracles. Actual restoration/BFCache support is recorded by browser/version; unavailable branches remain unexecuted rather than simulated passes. |

Partial implementation contributions: [ATDD-048](../../../05-atdd.md#case-atdd-048-expirysign-out), [ATDD-259](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback), [ATDD-336](../../../05-atdd.md#case-atdd-336-keep-protected-data-out-of-browser-persistence), [ATDD-338](../../../05-atdd.md#case-atdd-338-prevent-private-response-caching-and-stale-history-disclosure). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T007` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T007` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| visual-review | Capture every listed state under matched source/actual conditions and compare using the contract below | `/Users/mekari/Developer/personal-project/movie-app` | Required visual differences resolved at current revision; no overflow/missing identity | yes |
| interaction-accessibility | Exercise keyboard/focus/labels/errors/live regions/dialogs, zoom/reduced motion and source browser/host matrix independently of pixels | `/Users/mekari/Developer/personal-project/movie-app` | Meaningful accessible interaction and safe recovery, with unavailable contexts kept unverified | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer self-reviews first. A fresh independent reviewer then receives this task, inherited constraints, exact source clauses, baseline and resulting change, actual tests and sanitized evidence. Record distinct reviewer identities, reviewed revision, each finding as required/advisory, correction and recheck; labels alone do not establish independence. Scrutinize permission/secret boundaries, races, rollback/replay, lifetime evidence and the real owner boundary specified in this task. Implement → check → self-review → independent review → correct → rerun affected checks. Broaden testing only for a new concern. If no fresh reviewer is available, leave In review with exact next action; self-review cannot replace it.

For each state below, load the linked adopted reference and the actual route/panel (technical §3.1.1); use its source actor/fixture/trigger from ATDD. Review dark and light at1440/390/320CSSpx, current browser version/build, controlled time/data and matched state. Retain expected reference plus actual capture, source hash/code revision/environment/trigger, side-by-side comparison and verdict for layout, hierarchy, typography, colors, spacing, complete identity/content, overflow, artwork treatment and feedback. Narrow documented masks cannot hide required behavior.

Check keyboard/focus/labels/errors/announcements,200%zoom,reduced motion,touch and Chromium/Firefox/WebKit plus screen-reader smoke separately. For AI states include actual Claude/ChatGPT host context at T032/T033; standalone resource evidence cannot prove host isolation. Existing mockups are adopted references, not automatic pixel baselines or production test passes.

| Flow/state reference | Actor and fixture condition | Trigger and expected comparison |
|---|---|---|
| [account / login](../../../design-system/mockup/account.html#login) · [ATDD-258-T1-R01 · L10728](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | Visitor; open `/sign-in`, submit valid and invalid credentials independently; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show labeled email/password fields, existing-credential guidance, registration and public-browse links. Failure is generic; success follows assigned role and a permitted internal destination, without recovering or echoing passwords. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [account / register](../../../design-system/mockup/account.html#register) · [ATDD-258-T1-R02 · L10729](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | Visitor; open `/register` with current `policy`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show name/email/password guidance using effective limits, Create Viewer account and Sign in. Role selection, verification and password recovery are absent; password bytes stay exact. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [account / registered](../../../design-system/mockup/account.html#registered) · [ATDD-258-T1-R03 · L10730](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | Visitor; receive confirmed successful `register`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show account-ready feedback and Sign in; Viewer is assigned, no verification email or automatic sign-in is implied, and the password field is cleared. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [account / registration-failure](../../../design-system/mockup/account.html#registration-failure) · [ATDD-258-T1-R04 · L10731](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | Visitor; submit invalid/duplicate data or replay registration; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show safe field correction/generic account feedback, retained permitted non-secret values and Sign in. Disclose no other account and claim no duplicate creation; password is excluded from recovery. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [account / viewer](../../../design-system/mockup/account.html#viewer) · [ATDD-258-T1-R05 · L10732](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | Viewer; receive verified `session` after sign-in; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show public catalog access and sign-out, no management navigation; restore a permitted intended public destination and reject a restricted workspace/external return target. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [account / maintainer](../../../design-system/mockup/account.html#maintainer) · [ATDD-258-T1-R06 · L10733](../../../05-atdd.md#case-atdd-258-inspect-account-state-transitions-and-feedback) | Maintainer A; sign in with interrupted own work; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show permitted management navigation; resume only A's current eligible draft after target/version/policy/permission and original-operation checks. Offer sign-out without granting access from a fixture role label. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / session-check](../../../design-system/mockup/session.html#session-check) · [ATDD-259-T1-R01 · L10775](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Authenticated candidate; hold `session` while opening a protected destination; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Announce Checking your session and conceal protected fields/draft/actions. Public catalog navigation stays usable; unresolved identity grants no protected submit. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / auth-unavailable](../../../design-system/mockup/session.html#auth-unavailable) · [ATDD-259-T1-R02 · L10776](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Reader; fail `session` while Core can still answer public catalog reads; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Sign-in is temporarily unavailable, no private account details or protected work, and Browse catalog. Failure is distinct from confirmed sign-out. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / expired](../../../design-system/mockup/session.html#expired) · [ATDD-259-T1-R03 · L10777](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Maintainer A; expire the session during editing; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show session-expired feedback, conceal protected draft and require reauthentication; public browsing remains. Do not describe committed changes as rolled back. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / denied](../../../design-system/mockup/session.html#denied) · [ATDD-259-T1-R04 · L10778](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Viewer/Visitor; navigate to a Maintainer destination; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show You do not have access and public navigation; render no protected record/draft fields or enabled management actions. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / recheck](../../../design-system/mockup/session.html#recheck) · [ATDD-259-T1-R05 · L10779](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Maintainer A; reauthenticate with an unconfirmed prior save; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Check before continuing; resolve the original owner operation and current eligibility before restoring/submitting draft values. Do not offer a blind new save UUID. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / other-account](../../../design-system/mockup/session.html#other-account) · [ATDD-259-T1-R06 · L10780](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Account B; sign in after A's editing session ends; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show draft-unavailable-to-this-account feedback with original-owner sign-in/public browse actions. A's draft values, identifiers and password recovery never appear. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / signout](../../../design-system/mockup/session.html#signout) · [ATDD-259-T1-R07 · L10781](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Signed-in account; choose sign-out from the session menu; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Explain that the session ends and protected work becomes unavailable to the next account; offer confirmation/cancel where shown. An uncertain save is not undone by sign-out. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / signed-out](../../../design-system/mockup/session.html#signed-out) · [ATDD-259-T1-R08 · L10782](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Signed-in account; receive confirmed successful `signOut`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Signed out, remove protected DOM/controller/cache state and offer public browsing. Do not restore protected work on browser Back. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / signout-failed](../../../design-system/mockup/session.html#signout-failed) · [ATDD-259-T1-R09 · L10783](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Signed-in account; receive confirmed failed `signOut` before revocation; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Sign-out did not complete and retry; retain the known active-session identity. Do not claim sign-out or transfer its protected work to another account. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [session / signout-uncertain](../../../design-system/mockup/session.html#signout-uncertain) · [ATDD-259-T1-R10 · L10784](../../../05-atdd.md#case-atdd-259-inspect-session-state-transitions-and-feedback) | Signed-in account; lose the `signOut` response; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Sign-out is not confirmed, conceal protected content/submission and recheck the original result/session. Claim neither active authorization nor confirmed revocation until reconciled. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| boundary-tests | Not run | — | — | — | — |
| visual-review | Not run | — | — | — | — |
| interaction-accessibility | Not run | — | — | — | — |
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
