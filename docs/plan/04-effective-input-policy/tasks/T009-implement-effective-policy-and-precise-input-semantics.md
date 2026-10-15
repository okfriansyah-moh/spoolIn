---
kind: task
id: T009
group: G05
depends_on:
- T002
- T003
- T004
readiness: Ready
status: In progress
risk: high
test_strategy: test-first
visual_review: required
---

# T009 — Implement effective policy and precise input semantics

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-050 | implements |
| ATDD-051 | implements |
| ATDD-052 | implements |
| ATDD-097 | implements |
| ATDD-113 | implements |
| ATDD-116 | implements |
| ATDD-117 | implements |
| ATDD-118 | implements |
| ATDD-119 | implements |
| ATDD-121 | implements |
| ATDD-122 | implements |
| ATDD-127 | implements |
| ATDD-128 | implements |
| ATDD-129 | implements |
| ATDD-131 | implements |
| ATDD-140 | implements |
| ATDD-177 | implements |
| ATDD-184 | implements |
| ATDD-212 | implements |
| ATDD-251-T1-R02 | implements |
| ATDD-255-T1-R01 | implements |
| ATDD-267 | implements |
| ATDD-276 | both |
| ATDD-278 | both |
| ATDD-279 | both |
| ATDD-289 | both |
| ATDD-290 | both |
| ATDD-291 | both |
| ATDD-292 | both |
| ATDD-323 | implements |
| ATDD-324 | implements |
| ATDD-325 | implements |
| ATDD-326 | implements |
| ATDD-328 | implements |
| ATDD-334 | implements |
| ATDD-336 | implements |
| ATDD-337 | implements |
| ATDD-338 | implements |
| SC-04 | implements |
| FR-002 | implements |
| FR-004 | implements |
| FR-005 | implements |
| FR-006 | implements |
| FR-007 | implements |
| FR-010 | implements |
| FR-011 | implements |
| FR-012 | implements |
| FR-016 | implements |
| FR-017 | implements |
| NFR-005 | implements |
| NFR-007 | implements |
| NFR-008 | both |
| NFR-012 | implements |
| NFR-013 | implements |
| NFR-015 | implements |
| NFR-016 | implements |
| D05 | implements |
| D06 | implements |
| D07 | implements |
| D12 | implements |
| D21 | implements |
| AC-048 | implements |
| AC-050 | implements |
| AC-050-C01 | implements |
| AC-051 | implements |
| AC-051-C01 | implements |
| AC-052 | implements |
| AC-052-C01 | implements |
| AC-123 | implements |
| AC-123-C01 | implements |
| AC-139 | implements |
| AC-139-C01 | implements |
| AC-139-C02 | implements |
| AC-139-C03 | implements |
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
| AC-098 | implements |
| AC-099 | implements |
| AC-102 | both |
| G-03 | implements |
| G-04 | implements |
| G-05 | implements |
| AC-078 | implements |
| AC-078-C01 | implements |
| OD-002 | implements |
| PRD-POLICY-20 | implements |
| PRD-BOUND-11 | implements |
| PRD-FEEDBACK-04 | implements |
| PRD-HANDOFF-10 | implements |
| TD-C-008 | both |
| TD-C-057 | both |
| ADR-09 | both |
| TD-ROW-L2725 | both |
| TD-ROW-L6020 | both |
| TD-ROW-L6021 | both |
| TD-ROW-L6022 | both |
| TD-ROW-L6023 | both |
| TD-ROW-L6024 | both |
| TD-ROW-L6025 | both |
| TD-ROW-L6026 | both |
| TD-ROW-L6027 | both |
| TD-ROW-L6028 | both |
| TD-ROW-L6029 | both |
| TD-ROW-L6030 | both |
| TD-ROW-L6031 | both |
| TD-ROW-L6034 | both |
| F-CONFIG | both |

## Prerequisites

- [T002 — Generate and validate the shared wire contracts](../../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.
- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.
- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. ATDD-278 revision1.7 agrees with the PRD/techdoc: omission preserves optional text; explicit null or trimmed blank clears it. Source readiness is settled; case execution remains Not run until the rule and its checks exist. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | libraries/backend-support/src/main/kotlin/com/spoolin/support/policy/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | libraries/backend-support/src/main/kotlin/com/spoolin/support/problem/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/policy/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/boundary/graphql/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/policy/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | config/local.example.env | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/contracts/policy/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/policy/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | libraries/backend-support/src/test/kotlin/com/spoolin/support/policy/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | libraries/backend-support/src/test/kotlin/com/spoolin/support/problem/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/policy/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/boundary/graphql/ | T009 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.4 Forms, drafts and conflict recovery](../../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.4 Projection and presence crosswalk](../../../04-spoolin-techdoc.md#54-projection-and-presence-crosswalk): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.11 JSON projections and draft schemas](../../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [11.1 Configuration contract](../../../04-spoolin-techdoc.md#111-configuration-contract): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-251-T1-R02 · L10348](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `policy`; Public / Core \| Effective policy snapshot/revision; protected actions fail readiness on divergent policy; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-255-T1-R01 · L10584](../../../05-atdd.md#case-atdd-255-verify-mcpgatewayservice-typed-service-and-actor-boundary) | `GetPolicy`; GetPolicy(google.protobuf.Empty) → PolicySnapshot |
| [SC-04 · L103](../../../02-PRD.md#4-goals-and-success-criteria) | Protect access and accountability \| Public/Viewer/Maintainer permissions hold; private account data stay protected; permitted changes and AI approvals are attributable \| Unverified; Committed |
| [FR-002 · L236](../../../02-PRD.md#61-functional-requirements) | Search movie titles and cast/creator names with the approved matching and visibility rules \| A11/A14 Required; product policy Committed (section 13.1) \| AC-006 through AC-010, AC-022, AC-134, AC-147, AC-159 |
| [FR-004 · L238](../../../02-PRD.md#61-functional-requirements) | Create/edit movies and support archive, restore, and confirmed permanent deletion \| A07/A14 Required; approved lifecycle Committed \| AC-016 through AC-021, AC-108 through AC-110, AC-130, AC-143/AC-144, AC-148, AC-155 |
| [FR-005 · L239](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-006 · L240](../../../02-PRD.md#61-functional-requirements) | Maintain cast/creator credits, starter/custom roles, and multiple roles/character names; respect person lifecycle \| A09/A10 support; D25 Committed; product policy Committed (section 13.1) \| AC-026 through AC-028, AC-132, AC-135, AC-136, AC-143/AC-144 |
| [FR-007 · L241](../../../02-PRD.md#61-functional-requirements) | Preserve usable metadata, explicit unknowns, identity, date precision, and provenance \| D25/D26 Committed \| AC-029 through AC-032, AC-143, AC-145 |
| [FR-010 · L244](../../../02-PRD.md#61-functional-requirements) | Recover failed/uncertain artwork changes while protecting prior and still-used artwork \| D20/D21 Committed; product policy Committed (section 13.1); D11 mechanics Open \| AC-043 through AC-046, AC-152, AC-155, AC-157 |
| [FR-011 · L245](../../../02-PRD.md#61-functional-requirements) | Register immediate Viewer accounts, provide demo accounts, and support sign-in/sign-out \| D05 Committed; approved account scope Committed \| AC-047 through AC-049, AC-111, AC-112, AC-141/AC-142, AC-145/AC-146 |
| [FR-012 · L246](../../../02-PRD.md#61-functional-requirements) | Enforce the public/Viewer/Maintainer capability matrix, including private movie/people archives and connected tools \| D05 Committed; product policy Committed (section 13.1) \| AC-050 through AC-052, AC-113, AC-119, AC-133, AC-134 |
| [FR-016 · L250](../../../02-PRD.md#61-functional-requirements) | Create movies from MCP proposals only after exact-content human approval in ChatGPT/Claude \| Committed product policy (section 13.1) \| AC-116 through AC-122, AC-126, AC-148, AC-156 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-005 · L263](../../../02-PRD.md#62-quality-and-delivery-requirements) | Setup rejects unusable configuration and protects secrets \| D13/D21; configuration and secret protection Committed \| AC-076 through AC-078, AC-153 through AC-155, AC-157 |
| [NFR-007 · L265](../../../02-PRD.md#62-quality-and-delivery-requirements) | Responsive, keyboard-usable journeys with accurate feedback and retained recoverable work \| A13/A14 Required; keyboard use Committed \| AC-083 through AC-085, AC-139, AC-147, AC-159 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-012 · L270](../../../02-PRD.md#62-quality-and-delivery-requirements) | Risk-informed security consideration with accurate evidence and limitations \| D06 Committed \| AC-098, AC-099 |
| [NFR-013 · L271](../../../02-PRD.md#62-quality-and-delivery-requirements) | Interfaces implement the product's validation, optional data, access, and failure behavior \| A04/A05/A14 Required support; detailed contracts Open \| AC-100 through AC-103, AC-142/AC-143, AC-148 |
| [NFR-015 · L273](../../../02-PRD.md#62-quality-and-delivery-requirements) | Protect account PII with the selected encryption/rotation constraint; passwords use one-way hashing \| Committed product policy (section 13.1) \| AC-123 through AC-125, AC-127, AC-128; section 10 |
| [NFR-016 · L274](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [D05 · L306](../../../02-PRD.md#64-project-decision-register) | Authentication, authorization, RBAC, audit completed for release \| FR-011 through FR-013 / AC-047 through AC-056 |
| [D06 · L307](../../../02-PRD.md#64-project-decision-register) | ISO-informed consideration, no certification/conformity claim \| NFR-012 / AC-098, AC-099 |
| [D07 · L308](../../../02-PRD.md#64-project-decision-register) | Viewer/Maintainer and prepared accounts \| Viewer/Maintainer roles and prepared accounts are adopted as Committed policy in section 13.1; FR-011/FR-012 / AC-049, AC-050, AC-111 |
| [D12 · L313](../../../02-PRD.md#64-project-decision-register) | Authentication mechanism and verified internal identity \| Product access settled; mechanics Open in techdoc. FR-011/FR-012 / AC-047 through AC-052 |
| [D21 · L322](../../../02-PRD.md#64-project-decision-register) | Bounded safe retries and one configured exhaustion channel \| NFR-004/NFR-005, FR-015 / AC-061 through AC-063, AC-072 through AC-078 |
| [AC-048 · L436](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Expiry/sign-out \| A user signs out or their sign-in expires during work \| They attempt another restricted action \| Require sign-in again; public browsing remains available. Do not claim already committed work rolled back or expose protected drafts to a later different user. |
| [AC-050 · L438](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Role matrix \| Visitors, Viewers, and Maintainers try each capability \| An action is requested \| Results match section 3.1, including direct attempts to access hidden actions; all Maintainers have the same management permissions. |
| [AC-050-C01 · L438](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Results match section 3.1, including direct attempts to access hidden actions; all Maintainers have the same management permissions. |
| [AC-051 · L439](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Bypass attempt \| A caller supplies fabricated identity/role claims \| They attempt a restricted action \| No privilege or data access is granted from those claims; a connected assistant cannot exceed its user's permissions. |
| [AC-051-C01 · L439](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | No privilege or data access is granted from those claims; a connected assistant cannot exceed its user's permissions. |
| [AC-052 · L440](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Mixed permitted/restricted actions \| A request combines readable data and restricted effects \| It is handled \| Restricted information/effects stay denied; any completed permitted action and failed action are distinguishable, without claiming all actions succeeded. |
| [AC-052-C01 · L440](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Restricted information/effects stay denied; any completed permitted action and failed action are distinguishable, without claiming all actions succeeded. |
| [AC-123 · L518](../../../02-PRD.md#us-020-protect-private-account-information) | Private data boundaries \| Accounts contain names/emails and password credentials \| Public pages, catalog search, other users' access, diagnostics, or MCP proposals are inspected \| Private account information is not exposed outside its authorized purpose; email and private name data receive the selected encrypted-at-rest protection. |
| [AC-123-C01 · L518](../../../02-PRD.md#us-020-protect-private-account-information) | Private account information is not exposed outside its authorized purpose; email and private name data receive the selected encrypted-at-rest protection. |
| [AC-139 · L529](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Visible effective policy \| A permitted form, search field, or artwork selection is available \| The user enters information or selects artwork \| Show applicable current bounds and enabled formats before submission. Validation feedback names the affected input and correction, is accessible without color alone, and retains permitted non-secret content. Show processing and saved outcomes separately from validation. |
| [AC-139-C01 · L529](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Show applicable current bounds and enabled formats before submission. |
| [AC-139-C02 · L529](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Validation feedback names the affected input and correction, is accessible without color alone, and retains permitted non-secret content. |
| [AC-139-C03 · L529](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Show processing and saved outcomes separately from validation. |
| [AC-142 · L532](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Unicode, mononyms, and counting \| A permitted name/text value contains Unicode, internal spaces, accents, a hyphen, or an apostrophe \| It is submitted through a form or direct request at a configured boundary \| Apply the same Unicode-code-point count and bounds; preserve legitimate characters and internal spacing without demanding two name parts. Count precomposed é as one code point and its e-plus-combining-accent representation as two; byte length must not determine acceptance. A mononym meeting its field's bound is accepted. |
| [AC-142-C01 · L532](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Apply the same Unicode-code-point count and bounds; preserve legitimate characters and internal spacing without demanding two name parts. |
| [AC-142-C02 · L532](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Count precomposed é as one code point and its e-plus-combining-accent representation as two; byte length must not determine acceptance. |
| [AC-142-C03 · L532](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | A mononym meeting its field's bound is accepted. |
| [AC-143 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Whitespace, absence, and clearing \| A required ordinary-text field is blank/whitespace-only, or optional original-title/character data are unknown or previously saved \| A create/edit supplies blank required text, explicitly clears optional text, or omits an optional edit value \| Reject blank required text with no partial save. Permit unknown/cleared optional values; omission preserves the stored value. Validate each supplied nonempty optional value, retain meaningful synopsis line breaks, and create no invented metadata. |
| [AC-143-C01 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject blank required text with no partial save. |
| [AC-143-C02 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Permit unknown/cleared optional values; omission preserves the stored value. |
| [AC-143-C03 · L533](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Validate each supplied nonempty optional value, retain meaningful synopsis line breaks, and create no invented metadata. |
| [AC-144 · L534](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Other text boundaries \| Recommended bounds are configured: title 1 to 255, synopsis 1 to 5,000, role 1 to 100, and each character name 1 to 200 \| A permitted save supplies each field at its bounds or immediately outside them, with other requirements valid \| Accept the boundary lengths, including a one-character title; reject an empty required value or one above the applicable maximum. Retain the complete permitted draft, commit no partial edit, and silently truncate nothing. Apply character-name limits separately to each supplied name without imposing an invented count limit. |
| [AC-144-C01 · L534](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Accept the boundary lengths, including a one-character title; reject an empty required value or one above the applicable maximum. |
| [AC-144-C02 · L534](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Retain the complete permitted draft, commit no partial edit, and silently truncate nothing. |
| [AC-144-C03 · L534](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Apply character-name limits separately to each supplied name without imposing an invented count limit. |
| [AC-145 · L535](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Email and source URL \| Recommended email/URL maxima are configured \| Registration uses a valid email of 254 characters or one of 255; accepted source input uses a valid HTTP/HTTPS URL of 2,048 characters or one of 2,049; or either format is invalid \| Length alone permits the exact maximum and rejects the excess; independently reject invalid syntax and preserve required/optional semantics. Existing-account checks still apply. Accepting a URL authorizes no arbitrary fetch or new source-entry feature. Detailed syntax cases transfer to OD-003/OD-019. |
| [AC-145-C01 · L535](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Length alone permits the exact maximum and rejects the excess; independently reject invalid syntax and preserve required/optional semantics. |
| [AC-145-C02 · L535](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Existing-account checks still apply. |
| [AC-145-C03 · L535](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Accepting a URL authorizes no arbitrary fetch or new source-entry feature. |
| [AC-145-C04 · L535](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Detailed syntax cases transfer to OD-003/OD-019. |
| [AC-147 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Plain-text safety \| Entered catalog text, names, search text, or changed values contain markup, apostrophes, or instruction-like content within applicable limits \| They appear in forms, catalog/detail pages, search results, previews, or audit history \| Treat the content as text and execute no embedded code or instructions. Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. Private registration data retain their access boundaries. |
| [AC-147-C01 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Treat the content as text and execute no embedded code or instructions. |
| [AC-147-C02 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. |
| [AC-147-C03 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Private registration data retain their access boundaries. |
| [AC-148 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Direct-request validation \| A permitted caller bypasses form checks, or an authorized MCP caller submits an approved movie with invalid changed/new input \| The request reaches the application \| Enforce the effective bounds and content policy, reject invalid input with corrective feedback, and commit no partial catalog/account change or misleading successful audit event. Denied callers remain denied regardless of input validity. Do not repeat permanently invalid requests automatically. |
| [AC-148-C01 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Enforce the effective bounds and content policy, reject invalid input with corrective feedback, and commit no partial catalog/account change or misleading successful audit event. |
| [AC-148-C02 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Denied callers remain denied regardless of input validity. |
| [AC-148-C03 · L538](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not repeat permanently invalid requests automatically. |
| [AC-153 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Valid policy override \| A documented deployment configuration changes an applicable text/file minimum or maximum, or enables a nonempty subset of the static formats \| The configuration becomes effective using the techdoc's documented application procedure \| Forms and authoritative validation use the same effective policy. Show changed guidance and enforce inclusive new boundaries through direct requests and MCP as well. Required fields stay required; empty search still clears the text restriction even if the nonempty-query minimum increases. A disabled format is rejected for new uploads. No Maintainer configuration screen is implied. |
| [AC-153-C01 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Forms and authoritative validation use the same effective policy. |
| [AC-153-C02 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Show changed guidance and enforce inclusive new boundaries through direct requests and MCP as well. |
| [AC-153-C03 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Required fields stay required; empty search still clears the text restriction even if the nonempty-query minimum increases. |
| [AC-153-C04 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | A disabled format is rejected for new uploads. |
| [AC-153-C05 · L543](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | No Maintainer configuration screen is implied. |
| [AC-154 · L544](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Invalid policy configuration \| A text bound is negative/nonintegral/nonfinite, a required minimum permits emptiness, file size is nonpositive, minimum exceeds maximum, a setting is explicitly invalid/empty, or the format set is empty/unsupported \| The policy is loaded or applied \| Affected functionality does not become ready under the invalid policy. Restricted diagnostics identify the setting and correction without secrets; no silent unlimited limit, supported-format expansion, or unsafe fallback occurs. An unset value follows only its documented default. Application/reload behavior remains OD-020. |
| [AC-154-C01 · L544](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Affected functionality does not become ready under the invalid policy. |
| [AC-154-C02 · L544](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Restricted diagnostics identify the setting and correction without secrets; no silent unlimited limit, supported-format expansion, or unsafe fallback occurs. |
| [AC-154-C03 · L544](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | An unset value follows only its documented default. |
| [AC-154-C04 · L544](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Application/reload behavior remains OD-020. |
| [AC-155 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Existing data after tightening \| Existing text/artwork were accepted under older limits, or an account's password was valid before new-password bounds changed \| Limits tighten, the existing record is read, an unchanged stored field is retained during another edit, or the user signs in \| Do not truncate/delete stored text or artwork or invalidate the existing credential solely for the new limits. Keep authorized reads available and permit unchanged fields to remain while validating newly changed input. Invalid new replacement/upload still fails; ordinary reruns do not overwrite preserved data to force conformity. |
| [AC-155-C01 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not truncate/delete stored text or artwork or invalidate the existing credential solely for the new limits. |
| [AC-155-C02 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Keep authorized reads available and permit unchanged fields to remain while validating newly changed input. |
| [AC-155-C03 · L545](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Invalid new replacement/upload still fails; ordinary reruns do not overwrite preserved data to force conformity. |
| [AC-157 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Policy changes during a draft/upload \| Input or artwork met earlier limits, and the policy changes before submission/attachment \| The operation is evaluated against the effective policy \| If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-157-C01 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. |
| [AC-157-C02 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-098 · L874](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Record relevant risks, selected controls, owners, evidence, and limitations \| NFR-012; technical design |
| [AC-099 · L875](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Distinguish intended controls from tested/deployed proof; do not imply ISO/regulatory certification, TLS enforcement, or successful restore without evidence \| NFR-012; technical design |
| [AC-102 · L878](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify identity, units, presence, precision, Unicode-code-point counting, and boundary compatibility against section 5.5 \| NFR-013/NFR-016; technical design |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-04 · L925](../../../02-PRD.md#122-release-gates-remain-planned) | Upload, replacement, interrupted work, safe retries, exhaustion, and configured notification work \| FR-009/FR-010/FR-014/FR-015/FR-017; NFR-004/NFR-016; AC-149 through AC-152, AC-155/AC-157 \| Planned |
| [G-05 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [AC-078 · L987](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Secrets \| Setup uses account/provider/key material \| Documentation and diagnostics are inspected \| Real secrets are absent from repository examples, user messages, logs, audit, and notifications. |
| [AC-078-C01 · L987](../../../02-PRD.md#us-013-bound-recovery-and-protect-setup) | Real secrets are absent from repository examples, user messages, logs, audit, and notifications. |
| [OD-002 · L1019](../../../02-PRD.md#131-resolved-product-decisions) | Public discovery; Viewer and Maintainer; same permissions for all Maintainers; no role-management UI \| FR-011/FR-012; AC-049/AC-050/AC-113 |
| [PRD-POLICY-20 · L175](../../../02-PRD.md#recommended-defaults-for-other-inputs) | These Recommended starting values remain unselected and are not baseline requirements. The techdoc must record adopted values or justified alternatives before implementing the affected inputs. Acceptance criteria referring to these numbers apply when these recommended values are configured; the Committed behavior is enforcement of the effective policy. |
| [PRD-BOUND-11 · L208](../../../02-PRD.md#artwork-policy) | Supported formats \| Static JPEG, PNG, and WebP \| Committed supported formats; configuration may enable a nonempty subset |
| [PRD-FEEDBACK-04 · L798](../../../02-PRD.md#810-shared-feedback-rules) | Validation / denied access \| Show effective bounds and allowed formats; identify field/content corrections or permission failure without revealing private data. Retain permitted non-secret input without truncation; execute no entered markup. |
| [PRD-HANDOFF-10 · L843](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D06 \| Committed consideration \| Risk-to-control/owner/evidence/limitation review informed by ISO/IEC 27001/27002, without certification or organizational-conformity claims. |
| [TD-C-008 · L965](../../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery) | Changed-field current policy, Unicode White_Space trimming without NFC for ordinary text, exact passwords, code points, NUL/surrogate rejection, omit/null/empty presence and explicit stale comparison. Dispatch one UUID; uncertain writes reconcile owner receipts. |
| [TD-C-057 · L6010](../../../04-spoolin-techdoc.md#11-configuration-startup-and-command-contracts) | Environment→nonsecret file→documented defaults; required file secrets and all malformed bounds checked before readiness, shared policy/rate revisions, changed-field legacy preservation and gated MCP publication false by default. |
| [ADR-09 · L6556](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-09 \| Text storage plus configurable code-point validation. JSONB only for bounded evidence/payload snapshots \| FR-017/NFR-016; normalized relations for identity/credits/genres |
| [TD-ROW-L2725 · L2725](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `policy` \| Public / Core \| Effective policy snapshot/revision; protected actions fail readiness on divergent policy |
| [TD-ROW-L6020 · L6020](../../../04-spoolin-techdoc.md#111-configuration-contract) | DB \| Distinct service URLs/users/secret files; pool max 8,minIdle1; connect2s; statement3s; lock2s; idle-in-transaction5s; migration statement120s \| Each service; no superuser runtime |
| [TD-ROW-L6021 · L6021](../../../04-spoolin-techdoc.md#111-configuration-contract) | Network \| HTTPS localhost8443, exact Origin; MCP loopback separate port; mTLS cert/key/CA files and service SANs `core`, `catalog`, `media`, `mcp` required for application components \| Core/MCP/internal transport |
| [TD-ROW-L6022 · L6022](../../../04-spoolin-techdoc.md#111-configuration-contract) | Crypto \| Independent purpose keys, active key IDs, lookup/signing/rate/cursor and CSRF_KEY_FILE secrets required; 32-byte AES keys, key files with mode 0600 \| Core; keys never default |
| [TD-ROW-L6023 · L6023](../../../04-spoolin-techdoc.md#111-configuration-contract) | Actor signatures \| ACTOR_SIGNING_PRIVATE_KEY_FILE and ACTOR_SIGNING_KEY_ID on Core; ACTOR_VERIFY_KEYS_FILE on Catalog/Media; Ed25519 formats and rotation in §5.7 \| Core signs; receivers hold public keys only |
| [TD-ROW-L6024 · L6024](../../../04-spoolin-techdoc.md#111-configuration-contract) | Bootstrap \| Prepared Viewer/Maintainer identity and password secret files; frozen manifest identity/hash \| Core/Catalog |
| [TD-ROW-L6025 · L6025](../../../04-spoolin-techdoc.md#111-configuration-contract) | Notification \| mode database by default, enum of four; selected adapter required settings, fixed allowed endpoint/recipients \| Core and owning workers |
| [TD-ROW-L6026 · L6026](../../../04-spoolin-techdoc.md#111-configuration-contract) | Recovery \| retries 2 [0..9], base 500 ms [1..60000], cap 5000 ms&gt;=base, deadline 60000 ms [1000..3600000], multiplier 2 [1..10], full jitter \| Persist effective values per work |
| [TD-ROW-L6027 · L6027](../../../04-spoolin-techdoc.md#111-configuration-contract) | Text \| Person 1..100; registration name 2..100; title/original 1..255 when present; synopsis 1..5000; role 1..100; character 1..200; biography optional 1..5000 \| Shared policy, owner validates |
| [TD-ROW-L6028 · L6028](../../../04-spoolin-techdoc.md#111-configuration-contract) | Credentials/search \| email max 254; new password 15..128; search 0..200; URLmax2048 \| Core/Catalog; no arbitrary URL-fetch feature |
| [TD-ROW-L6029 · L6029](../../../04-spoolin-techdoc.md#111-configuration-contract) | Files \|1..5242880 bytes, enabled nonempty subset JPEG/PNG/WEBP; processing bounds §7.6 \| Media and upload form |
| [TD-ROW-L6030 · L6030](../../../04-spoolin-techdoc.md#111-configuration-contract) | Structural bounds \| Genres&lt;=20; languages BCP47&lt;=35chars, countries uppercase ISO3166-1 alpha2; runtime 1..100000 min; years 1..9999 \| Storage/contract envelope, not UI assumptions |
| [TD-ROW-L6031 · L6031](../../../04-spoolin-techdoc.md#111-configuration-contract) | Request/page \| §5 limits; finite positive maxima; enforce response and upload bounds separately \| Core/MCP/gRPC |
| [TD-ROW-L6034 · L6034](../../../04-spoolin-techdoc.md#111-configuration-contract) | Tunnel \| enabled=true for configured ChatGPT verification; tunnelID/runtime-keyfile/runnerpath required if enabled \| Operator prerequisites; no auto account creation |

## Implementation

1. Implement source-selected configuration precedence by presence: environment then nonsecret file then documented defaults; secret values use the declared protected file settings. Reject explicit empty/invalid/nonfinite/reversed/out-of-capacity values before readiness.
2. Publish the versioned effective policy and shared Unicode-code-point validation rules. Trim ordinary Unicode White_Space without NFC, preserve meaningful internal text, reject NUL/unpaired surrogates, and never transform passwords.
3. Implement omission versus clear adapters and ordered collection replacement. Changed/new values use current bounds; unchanged stored fields remain valid after tightening, empty search removes only text, and explicit optional clearing is allowed.
4. Add typed safe errors/scalars/envelope validation and accessible shared guidance that preserves full rejected non-secret input. Bound policy inputs and output projection independently of storage capacity.
5. Implement and verify ATDD-278 revision1.7 with distinct omitted, explicit-null, empty-string and whitespace-only patches. Omission preserves the stored optional text without a field change; null/trimmed blank requests clearing without a validation error. Retain rejection of an empty required genre set and exact ordered character names, including a literal slash. Record actual unit evidence only after execution; transport and committed-value proof remains assigned to the complementary cases.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T009-A1:** Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- **T009-A2:** Compatibility: tightening does not truncate data/artwork or invalidate old credentials; a changed over-limit value fails with preserved draft and no partial mutation.
- **T009-A3:** Presence: absent keeps the stored value, explicit null/trimmed blank optional text clears, and a slash inside a character string is retained. ATDD-278 verifies each omission/null/blank outcome at the isolated rule boundary.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-276](../../../05-atdd.md#case-atdd-276-count-unicode-code-points-and-preserve-post-trim-text) / Unit | Person default 1..100: empty, NBSP + V + NBSP, 100/101 copies of 😀, composed é and decomposed e + U+0301; isolated unpaired surrogate. | Then NBSP + V + NBSP becomes V; 100 emoji pass and 101 fail without truncation And é and e + U+0301 remain distinct exact strings; the latter counts two code points And blank and unpaired-surrogate input fail safely Observation: Compare exact returned strings, scalar count and rejection field path with the independently listed values. This detects UTF-16 length counting, accidental NFC and silent truncation. |
| [ATDD-278](../../../05-atdd.md#case-atdd-278-distinguish-omitted-fields-from-clearing-and-ordered-replacements) / Unit | Existing originalTitle=Cahaya; patches omit originalTitle, set null, set empty string, or set whitespace-only text; genre IDs [b, a] or []; character names [Maya, Maya / Echo] and reversed order. | Then omission retains Cahaya and explicit null, empty string, or whitespace-only text clears the optional field And an empty required genre set is invalid And character arrays preserve element order and the literal slash inside one name Observation: Compare returned patch intent/mask: omission preserves Cahaya without a field change, while explicit null, empty string, and whitespace-only text each request clearing without a validation error. Verify required-genre errors and ordered character replacements separately; real GraphQL/protobuf presence and committed values require the complementary case. |
| [ATDD-279](../../../05-atdd.md#case-atdd-279-validate-partial-dates-without-inventing-missing-precision) / Unit | unknown; 2024; 2024-02; 2024-02-29; 2023-02-29; month without year; day without month; years 1/9999/0/10000; overlapping partial TV start/end ranges. | Then valid partial tuples retain absent month/day; invalid tuples and impossible days reject And unknown year is eligible for imported read but not manual/MCP movie creation And determinate reversed TV ranges reject while overlapping imprecise ranges remain unknown Observation: Compare tuple/precision and errors against hand-checked leap/year examples; SQL CHECKs and endpoint validation remain separate integration proof. |
| [ATDD-289](../../../05-atdd.md#case-atdd-289-resolve-configuration-by-presence-rather-than-truthiness) / Unit | Environment/file/default: retries 0/file2/default2; publicationfalse/filetrue; omitted env/file; explicit empty or whitespace; valid decimal and invalid NaN/1.5. | Then explicit zero/false overrides lower sources where meaningful And omission falls through, while explicit invalid empty/whitespace/malformed values fail And a required secret has no demo/default fallback Observation: Inspect resolved value/source or redacted setting-name correction; actual readiness and shared policy agreement require integration. |
| [ATDD-290](../../../05-atdd.md#case-atdd-290-validate-selected-policy-bounds-and-mode-dependencies) / Unit | Every F-CONFIG numeric min/max boundary±1; min&gt;max, negative, unsupported/empty formats, cap&lt;base, disabled optional adapter empty, enabled external adapter missing secret file reference. | Then only source-valid finite values and consistent selected groups resolve And active-mode required settings cannot be omitted; deliberately disabled optional fields may be empty And validation reports setting name/correction without secret content Observation: Compare each named setting’s explicit valid/invalid result against F-CONFIG and current source table; do not infer that every fixed operational number is configurable. |
| [ATDD-291](../../../05-atdd.md#case-atdd-291-map-failures-to-safe-boundary-projections) / Unit | Named invalid/unauthenticated/denied/not-found/conflict/capacity/dependency/deadline domain errors plus synthetic SQL/provider/password canaries. | Then codes and safe field paths follow the failure table And internal messages and sensitive canaries are absent And an HTTP200 domain rejection is classified as failure rather than success Observation: Assert exact projected keys/code/classification and canary absence using fixed inputs; actual transport status propagation remains integration. |
| [ATDD-292](../../../05-atdd.md#case-atdd-292-apply-recipient-specific-output-allowlists) / Unit | Public catalog, own-session name, Maintainer audit and model proposal recipients; object contains account email, raw challenge, token, work/envelope/key internals and permitted public title. | Then only each recipient’s declared schema fields are emitted And own-session name never becomes a public directory field And audit exposes F-AUDIT-CORRECTION outcome/subject identity while model output omits UI challenge Observation: Compare complete expected key sets and typed values, not only absence of a few keys; real authorization/protocol projections require integration. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [F-CONFIG · L206](../../../05-atdd.md#f-config-selected-settings-and-concrete-evidence) | For every techdoc §11.1 group (DB, network, crypto/signatures, bootstrap, notification, recovery, text, credentials/search, files, structural, request/page, tunnel and MCP UI assertions), independently select each listed setting and test its valid baseline, bound−1/bound/bound+1, omission, explicit empty/whitespace, false/zero where meaningful, malformed type and conflicting precedence. Use the current source’s literal values and units; do not turn fixed engine/compatibility ceilings into arbitrary tunables. Explicit keys including RETRY_MAX_RETRIES, ACTOR_SIGNING_PRIVATE_KEY_FILE/KEY_ID, ACTOR_VERIFY_KEYS_FILE, CSRF_KEY_FILE and MCP_UI_ASSERTION_KEY_FILE/KEY_ID/PUBLICATION_ENABLED retain their names. At future execution the generated effective config artifact supplies concrete names for remaining semantic settings; record name/value-source/units/bound/error correction before running each row, with secrets represented by synthetic file references only. Missing selected values fail readiness; disagreement on policy revision blocks protected writes. Naming an artifact is not an additional product decision. |

Partial implementation contributions: [ATDD-050](../../../05-atdd.md#case-atdd-050-role-matrix), [ATDD-051](../../../05-atdd.md#case-atdd-051-bypass-attempt), [ATDD-052](../../../05-atdd.md#case-atdd-052-mixed-permittedrestricted-actions), [ATDD-097](../../../05-atdd.md#case-atdd-097-private-data-boundaries), [ATDD-113](../../../05-atdd.md#case-atdd-113-visible-effective-policy), [ATDD-116](../../../05-atdd.md#case-atdd-116-unicode-mononyms-and-counting), [ATDD-117](../../../05-atdd.md#case-atdd-117-whitespace-absence-and-clearing), [ATDD-118](../../../05-atdd.md#case-atdd-118-other-text-boundaries), [ATDD-119](../../../05-atdd.md#case-atdd-119-email-and-source-url), [ATDD-121](../../../05-atdd.md#case-atdd-121-plain-text-safety), [ATDD-122](../../../05-atdd.md#case-atdd-122-direct-request-validation), [ATDD-127](../../../05-atdd.md#case-atdd-127-valid-policy-override), [ATDD-128](../../../05-atdd.md#case-atdd-128-invalid-policy-configuration), [ATDD-129](../../../05-atdd.md#case-atdd-129-existing-data-after-tightening), [ATDD-131](../../../05-atdd.md#case-atdd-131-policy-changes-during-a-draftupload), [ATDD-140](../../../05-atdd.md#case-atdd-140-refresh-affected-views-only-after-confirmed-changes), [ATDD-177](../../../05-atdd.md#case-atdd-177-enforce-shared-fixed-windows-without-identity-leakage), [ATDD-184](../../../05-atdd.md#case-atdd-184-keep-sensitive-and-unbounded-content-out-of-outputs), [ATDD-212](../../../05-atdd.md#case-atdd-212-review-security-evidence-without-certifying-the-application), [ATDD-267](../../../05-atdd.md#case-atdd-267-inspect-policies-state-transitions-and-feedback), [ATDD-323](../../../05-atdd.md#case-atdd-323-prove-sql-and-hql-binding-across-actual-input-boundaries), [ATDD-324](../../../05-atdd.md#case-atdd-324-prove-stored-and-reflected-text-cannot-execute-in-browser-or-review-ui), [ATDD-325](../../../05-atdd.md#case-atdd-325-keep-filename-and-metadata-payloads-outside-processor-execution), [ATDD-326](../../../05-atdd.md#case-atdd-326-admit-at-most-the-shared-quota-across-concurrent-core-replicas), [ATDD-328](../../../05-atdd.md#case-atdd-328-fail-closed-when-the-authoritative-limiter-is-unavailable), [ATDD-334](../../../05-atdd.md#case-atdd-334-keep-worker-and-probe-budgets-separate-from-browser-quotas), [ATDD-336](../../../05-atdd.md#case-atdd-336-keep-protected-data-out-of-browser-persistence), [ATDD-337](../../../05-atdd.md#case-atdd-337-enforce-non-persistent-secure-session-cookies), [ATDD-338](../../../05-atdd.md#case-atdd-338-prevent-private-response-caching-and-stale-history-disclosure). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T009` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| visual-review | Capture every listed state under matched source/actual conditions and compare using the contract below | `/Users/mekari/Developer/personal-project/movie-app` | Required visual differences resolved at current revision; no overflow/missing identity | yes |
| interaction-accessibility | Exercise keyboard/focus/labels/errors/live regions/dialogs, zoom/reduced motion and source browser/host matrix independently of pixels | `/Users/mekari/Developer/personal-project/movie-app` | Meaningful accessible interaction and safe recovery, with unavailable contexts kept unverified | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

Only isolated build/unit/component validation is required here. The source case/row owners named in Traceability retain complete real-service/client proof; no empty integration selection is required for local completion. Graphical captures below still inspect a rendered component/reference under the declared contexts.

## Review and evidence

The implementer self-reviews first. A fresh independent reviewer then receives this task, inherited constraints, exact source clauses, baseline and resulting change, actual tests and sanitized evidence. Record distinct reviewer identities, reviewed revision, each finding as required/advisory, correction and recheck; labels alone do not establish independence. Scrutinize permission/secret boundaries, races, rollback/replay, lifetime evidence and the real owner boundary specified in this task. Implement → check → self-review → independent review → correct → rerun affected checks. Broaden testing only for a new concern. If no fresh reviewer is available, leave In review with exact next action; self-review cannot replace it.

For each state below, load the linked adopted reference and the actual route/panel (technical §3.1.1); use its source actor/fixture/trigger from ATDD. Review dark and light at1440/390/320CSSpx, current browser version/build, controlled time/data and matched state. Retain expected reference plus actual capture, source hash/code revision/environment/trigger, side-by-side comparison and verdict for layout, hierarchy, typography, colors, spacing, complete identity/content, overflow, artwork treatment and feedback. Narrow documented masks cannot hide required behavior.

Check keyboard/focus/labels/errors/announcements,200%zoom,reduced motion,touch and Chromium/Firefox/WebKit plus screen-reader smoke separately. For AI states include actual Claude/ChatGPT host context at T032/T033; standalone resource evidence cannot prove host isolation. Existing mockups are adopted references, not automatic pixel baselines or production test passes.

| Flow/state reference | Actor and fixture condition | Trigger and expected comparison |
|---|---|---|
| [policies / text-boundaries](../../../design-system/mockup/policies.html#text-boundaries) · [ATDD-267-T1-R01 · L11179](../../../05-atdd.md#case-atdd-267-inspect-policies-state-transitions-and-feedback) | Permitted reader/form user; enter controlled Unicode/length/plain-text fixtures; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show effective code-point rules and linked correction, preserving mononyms, punctuation, line breaks and exact valid text. Person/registration minima differ; markup-looking text renders inertly, without byte-count validation or silent truncation. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [policies / optional-clearing](../../../design-system/mockup/policies.html#optional-clearing) · [ATDD-267-T1-R02 · L11180](../../../05-atdd.md#case-atdd-267-inspect-policies-state-transitions-and-feedback) | Maintainer; change/clear/leave optional editor fields independently; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show unknown optional values honestly. Explicit optional clearing differs from unchanged/omitted input; required whitespace-only text receives correction. Character arrays preserve complete entries/slashes; no zero runtime or guessed date/credit. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [policies / policy-changed](../../../design-system/mockup/policies.html#policy-changed) · [ATDD-267-T1-R03 · L11181](../../../05-atdd.md#case-atdd-267-inspect-policies-state-transitions-and-feedback) | Maintainer; tighten policy before a changed/new draft is saved; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show new rule and full retained permitted draft with correction; unchanged stored values remain readable/retainable. No partial save/truncation or settings page is introduced; uncertain original effects still require lookup. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [policies / policy-unavailable](../../../design-system/mockup/policies.html#policy-unavailable) · [ATDD-267-T1-R04 · L11182](../../../05-atdd.md#case-atdd-267-inspect-policies-state-transitions-and-feedback) | Permitted user; fail current `policy` load or start under invalid configuration; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Editing is temporarily unavailable, block unsupported input submissions and retain authorized reads where possible. Do not remove limits or reveal restricted configuration diagnostics; retry/read navigation is safe. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

These captures inspect this task’s controlled policy/draft/review component with source fixtures. They do not require later domain editors or real host integration to exist: T016/T043/T044 and T032/T033 respectively retain the full production/host state proof. Record that boundary explicitly with each capture.

## Evidence

The first table retains historical results at `3990e165`; the latest runtime-phase results follow its execution history below.

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Passed: 2,268 selected/executed, zero failures | [Actual report](/private/tmp/spoolin-policy-t009-complete/.ai/evidence/t009-unit-results.json) | `3990e165` / `176cc6d9` | Node24/JDK21; isolated unit/component rules | `/root/reviewer` |
| visual-review | Not run | — | — | — | — |
| interaction-accessibility | Not run | — | — | — | — |
| self-review | Source corrections complete; overall verification blocked | [Self-review](/private/tmp/spoolin-policy-t009-complete/.ai/evidence/t009-policy/self-review-current.json) | `3990e165` / `176cc6d9` | Frozen implementation and retained actual results | `account-policy-implementer` |
| independent-review | Blocked: required runtime gate failed; stop after two rounds | [Independent stop review](/private/tmp/spoolin-policy-t009-complete/.ai/evidence/t009-independent-stop-review.json) | `3990e165` / `176cc6d9` | Actual receipt, raw reports and fixture logs | `/root/reviewer` |
| quality-gates | Failed: 53 passed, seven not applicable, one required failure | [Exact receipt](/private/tmp/spoolin-policy-t009-complete/.ai/evidence/run-7c49c139b35e4c75ab4a7133d4c44328/receipt.json) | `3990e165` / `176cc6d9` | Required delivery gates | `/root/reviewer` |
| browser-automation | Passed: 32 shell and 40 policy cases; final visual/reader/zoom acceptance remains incomplete | [Actual gate result](/private/tmp/spoolin-policy-t009-complete/.ai/reports/1792092790141198000-47666/result.json) | `3990e165` / `176cc6d9` | Chromium/Firefox/WebKit controlled browser cases | `/root/reviewer` |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.

Execution started — 16 October 2026: Accepted T004/G02 is Done and the refreshed task index reports T009 execution eligible. Isolated branch `codex/effective-policy` in `/private/tmp/spoolin-policy-t009`, claim `lane-696b347b93c64d8e`, owner `account-policy-implementer`, starts from `306e6edaf610d9424ba1d22e170d14f09f1e7fff`. [Active brief](/private/tmp/spoolin-policy-t009/.ai/state/t009-brief.json) records exact scope, test-first strategy, selected cases, required commands, dedicated browser gate and separate visual/reader/zoom proof. Checks remain Not run until captured; this dispatch is not acceptance.

Execution stopped — 16 October 2026: The corrected policy source is retained at `3990e165d284f1f7ffb884383fb1dbcf1b92e599` on `codex/effective-policy-complete`, fingerprint `176cc6d93894bc7932ad347833cd63bf185f9d36dfab63715686108a00dbd269`. [Stopped execution record](/private/tmp/spoolin-shell-account-complete/.ai/evidence/t009-stopped-execution.json) binds the actual receipt and independent review. The local selector passed 2,268 tests; shell and policy browser suites passed 32 and 40 tests. Required gates finished with 53 passed, seven not applicable and one failed: `core-account-boundaries` failed during disposable PostgreSQL startup before the recipe assertions. The database reported ready, while Testcontainers observed no mapped ports; the underlying runtime cause remains unknown.

Independent review resolved source findings C01–C07 but rejected overall acceptance. The two failed correction rounds require this work group to stop. Fresh reader, actual browser 200% zoom and final visual acceptance remain incomplete. Preserve the frozen lane, both failed receipts, original reports/media and correction history. T010, T006, T008 and T007 remain unimplemented pending accepted prerequisites. The exact next action is a revised work-group decision followed by evidence-based runtime diagnosis and a bounded remedy before another required run; a presumed transient or a timeout increase alone is insufficient. No T009 source has been integrated or delivered.

Execution resumed — 16 October 2026: The user authorized a revised runtime investigation, a bounded remedy and subsequent verification/delivery. Both failed receipts and the two historical failed correction rounds remain retained. [Runtime brief](/private/tmp/spoolin-policy-t009-runtime-resume/.ai/state/t009-runtime-resume-brief.json) records the new claim and unchanged imported source fingerprint. Investigation must establish the observed port-mapping cause and an independently reviewed remedy before one newly authorized complete required verification run. This resumption grants no pass, skipped check, timeout weakening or acceptance.

Runtime phase stopped — 16 October 2026: Investigation confirmed a Docker Desktop host-forward bind failure on the original container; the historical port holder remains unknown. The bounded two-file test-fixture remedy is frozen at `97aac669b605276cc2dc85e19d270e452848ff2f`, fingerprint `79e0b37316529e882daa349955101cb6e490b0c6ee5e636aacecc7f240cb9cbb`, and passed independent review plus all 35 affected checks. Its one authorized complete capture finished with two required failures. The [current stop record](/private/tmp/spoolin-shell-account-runtime-resume/.ai/evidence/t009-runtime-stopped-execution.json) binds the [actual receipt](/private/tmp/spoolin-policy-t009-runtime-resume/.ai/evidence/run-6884aa8161024bd5a6a0248bfde0512b/receipt.json), preserved original artifacts and the [independent E02/E03 ledger](/private/tmp/spoolin-policy-t009-runtime-resume/.ai/evidence/t009-runtime-final-independent-stop-review.json).

- E02: the direct local selector passed 2,258 backend assertions, then Vitest startup exited before producing component results. A later frontend gate passed 103 tests, including ten policy cases, after its existing setup hook; this does not replace the failed selected contract.
- E03: policy browser cases passed 39 of 40; WebKit at 390px/dark produced no correction alert after the initial field → Tab → Enter journey. The trace does not identify the post-Tab target or prove submit dispatch; the browser cause remains unknown. Shell browser cases passed 32 of 32, the dedicated Core account boundary passed 26 of 26, and gates finished with 53 passed, seven not applicable and one failed.

Preserve both historical failed rounds and this revised capture without resetting history. The reviewed next action is explicit selector setup followed by unchanged fresh-checkout proof, plus observing the intended keyboard focus/submit boundary before any cause-supported browser correction. A revised work group decision is pending. No retry, click substitution, timeout weakening, manual reader/zoom capture, dependent implementation, T009 integration or T009 publication occurred. Task status remains In progress with unresolved internal findings; task and feature acceptance remain false. The temporary Firefox runtime was stopped once and independently confirmed removed; application test ports are idle. T010, T006, T008 and T007 still await accepted prerequisites. The user-approved T007 contribution verifies safe session handoff and defers positive draft recovery to T015/T016 without accepting the complete recovery case.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including self-review, fresh independent review and applicable visual/interaction checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
