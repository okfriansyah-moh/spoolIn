---
kind: task
id: T020
group: G10
depends_on:
- T004
- T007
- T013
- T015
- T019
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: required
---

# T020 — Connect people management and retained edits

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-023 | both |
| ATDD-025 | implements |
| ATDD-026 | implements |
| ATDD-027 | implements |
| ATDD-079 | implements |
| ATDD-081 | implements |
| ATDD-114 | both |
| ATDD-261 | both |
| ATDD-261-T1-R01 | both |
| ATDD-261-T1-R02 | both |
| ATDD-261-T1-R03 | both |
| ATDD-261-T1-R04 | both |
| ATDD-261-T1-R05 | both |
| ATDD-261-T1-R06 | both |
| ATDD-261-T1-R07 | both |
| ATDD-261-T1-R08 | both |
| ATDD-261-T1-R09 | both |
| ATDD-262 | implements |
| FR-005 | both |
| FR-006 | implements |
| FR-017 | both |
| NFR-014 | implements |
| A09 | implements |
| A10 | both |
| D09 | implements |
| D25 | implements |
| AC-023 | both |
| AC-023-C01 | both |
| AC-023-C02 | both |
| AC-026 | implements |
| AC-026-C01 | implements |
| AC-027 | implements |
| AC-027-C01 | implements |
| AC-027-C02 | implements |
| AC-104 | implements |
| AC-104-C01 | implements |
| AC-107 | implements |
| AC-107-C01 | implements |
| AC-140 | both |
| AC-140-C01 | both |
| AC-140-C02 | both |
| G-03 | both |
| OD-009 | implements |
| US-002 | both |
| PRD-PERM-01 | implements |
| PRD-PERM-05 | implements |
| PRD-POLICY-03 | both |
| PRD-POLICY-05 | implements |
| PRD-POLICY-18 | both |
| PRD-BOUND-01 | both |
| PRD-BOUND-05 | implements |
| PRD-CONCEPT-09 | both |
| UI-people-people | both |
| UI-people-people-loading | both |
| UI-people-people-empty | both |
| UI-people-people-failure | both |
| UI-people-person-unavailable | both |
| UI-people-person | both |
| UI-people-person-edit | both |
| UI-people-person-conflict | both |
| UI-people-person-archived | both |
| DS-POLICY-13 | implements |
| BRAIN-A10 | both |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T007 — Connect account and session browser journeys](../../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T013 — Connect discovery, details and public people views](../../07-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria.
- [T015 — Build account-isolated browser draft controllers](../../08-draft-recovery/tasks/T015-build-account-isolated-browser-draft-controllers.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request.
- [T019 — Implement shared person writes and identity](T019-implement-shared-person-writes-and-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | frontend/src/routes/manage/people/+page.svelte | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/manage/people/new/+page.svelte | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/manage/people/[id]/+page.svelte | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/features/person/write/ | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/people/ | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.1.1 UI reference and implementation map](../../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.4 Forms, drafts and conflict recovery](../../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.6 Components, accessibility and responsive behavior](../../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [FR-005 · L239](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-006 · L240](../../../02-PRD.md#61-functional-requirements) | Maintain cast/creator credits, starter/custom roles, and multiple roles/character names; respect person lifecycle \| A09/A10 support; D25 Committed; product policy Committed (section 13.1) \| AC-026 through AC-028, AC-132, AC-135, AC-136, AC-143/AC-144 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-014 · L272](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preserve stale drafts and prevent unintended duplicate effects without merging distinct works/people \| Approved product policy Committed; D09 mechanism Recommended; D21 safe replay Committed \| AC-104 through AC-107 |
| [A09 · L288](../../../02-PRD.md#63-project-requirements-coverage) | List cast and creators \| FR-001/FR-005/FR-006 / AC-004, AC-022, AC-026, AC-134 |
| [A10 · L289](../../../02-PRD.md#63-project-requirements-coverage) | Add, update, delete cast and creators \| FR-005/FR-006 / AC-023 through AC-028, AC-131 through AC-133 |
| [D09 · L310](../../../02-PRD.md#64-project-decision-register) | Optimistic concurrency; separate duplicate requests/data \| Stale-save rejection selected as product behavior; mechanism stays with techdoc. NFR-014 / AC-104 through AC-107 |
| [D25 · L326](../../../02-PRD.md#64-project-decision-register) | Metadata, type/genres, stable identities, provenance \| FR-006/FR-007 / AC-026 through AC-032 |
| [AC-023 · L386](../../../02-PRD.md#us-004-maintain-people-and-credits) | Create/update \| A Maintainer supplies a person name within the effective limits in section 5.5 and valid available details for a new or active person \| Save succeeds \| Preserve one shared person identity and show corrections through its visible credits. Reject missing/invalid required information with retained input and no partial save; archived people require restoration before editing. |
| [AC-023-C01 · L386](../../../02-PRD.md#us-004-maintain-people-and-credits) | Preserve one shared person identity and show corrections through its visible credits. |
| [AC-023-C02 · L386](../../../02-PRD.md#us-004-maintain-people-and-credits) | Reject missing/invalid required information with retained input and no partial save; archived people require restoration before editing. |
| [AC-026 · L390](../../../02-PRD.md#us-004-maintain-people-and-credits) | Multiple roles \| An active shared person and active movie exist \| A Maintainer assigns credits \| Actor/Director/Writer/Producer are available; custom roles fit Cast or Creator; one person may have multiple roles and multiple acting character names without cloning the person. |
| [AC-026-C01 · L390](../../../02-PRD.md#us-004-maintain-people-and-credits) | Actor/Director/Writer/Producer are available; custom roles fit Cast or Creator; one person may have multiple roles and multiple acting character names without cloning the person. |
| [AC-027 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Invalid/duplicate credit \| A name/category/reference is missing, the selected person is archived, or an identical credit exists \| The Maintainer saves a new credit \| Request correction for invalid input; reject assignment to an archived person until restored, and do not create duplicate identical display entries. Custom labels use consistent category/identity treatment. |
| [AC-027-C01 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Request correction for invalid input; reject assignment to an archived person until restored, and do not create duplicate identical display entries. |
| [AC-027-C02 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Custom labels use consistent category/identity treatment. |
| [AC-104 · L491](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Stale save \| Two Maintainers edit the same movie/person \| One saves before the other \| Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-104-C01 · L491](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-107 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Similar real-world records \| Independent works/people share names \| Records are created or compared \| Preserve distinct identities and do not merge by text alone; explain genuine identity conflicts without silently changing another record. |
| [AC-107-C01 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Preserve distinct identities and do not merge by text alone; explain genuine identity conflicts without silently changing another record. |
| [AC-140 · L530](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Person-name boundaries \| Person-name defaults are configured and the actor is a Maintainer editing an active person or creating one \| They submit otherwise valid person data with a name of 1, 100, 0, or 101 Unicode code points after trimming \| Accept lengths 1 and 100, including a single-character professional name; reject empty and 101 with a field-specific error and no partial change. Preserve the rejected draft for correction, and never merge same-named people. |
| [AC-140-C01 · L530](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Accept lengths 1 and 100, including a single-character professional name; reject empty and 101 with a field-specific error and no partial change. |
| [AC-140-C02 · L530](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Preserve the rejected draft for correction, and never merge same-named people. |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [OD-009 · L1022](../../../02-PRD.md#131-resolved-product-decisions) | Reject stale writes, retain draft, require review; no force overwrite \| NFR-014; AC-020/AC-025/AC-104; concurrency mechanism transfers to techdoc |
| [US-002 · L345](../../../02-PRD.md#us-002-search-and-filter) | ### US-002: Search and filter As a reader, I want search and filters to narrow results together so I can find titles or people. Requirements: FR-002, FR-003, NFR-013. People search concerns catalog people, not account holders. |
| [PRD-PERM-01 · L68](../../../02-PRD.md#31-permission-matrix) | Browse/search/filter active movies and TV series; view metadata and public people/credits \| Yes \| Yes \| Yes |
| [PRD-PERM-05 · L72](../../../02-PRD.md#31-permission-matrix) | Manage shared people and custom credit roles \| No \| No \| Yes |
| [PRD-POLICY-03 · L118](../../../02-PRD.md#51-included-behavior) | Search is case-insensitive partial matching on display/original titles and person names. Empty search text adds no text restriction. Title ordering defaults to newest release context first, with unknown release dates last. Within the precision the source provides, results remain consistently ordered; tie-breakers and navigation mechanics belong to the techdoc. Multiple selected genres match any selected genre; type, genre, year/month, and search combine as narrowing categories. Month requires year; clearing year also clears month. Clearing text preserves other filters. Each visible result must match the currently displayed criteria. |
| [PRD-POLICY-05 · L122](../../../02-PRD.md#51-included-behavior) | Cast and creators share reusable Person records. Start with Actor in Cast and Director, Writer, and Producer in Creator. Maintainers may add custom labels within either category. A person may have several roles on a movie; an acting credit may list several character names. Repeated identical credits do not create duplicated display entries. Names and titles are not globally unique identities; similarly named people and remakes remain distinct. |
| [PRD-POLICY-18 · L164](../../../02-PRD.md#name-defaults) | Catalog person names and registration names use separate, configurable defaults. Both are single name fields; do not require a given-name/family-name split, multiple words, or a surname. Support mononyms, Unicode, spaces, hyphens, and apostrophes. A length check does not prove that a name is authentic. |
| [PRD-BOUND-01 · L168](../../../02-PRD.md#name-defaults) | Catalog person name \| 1 character \| 100 characters \| Committed selected defaults: permit short professional names, including single-character names |
| [PRD-BOUND-05 · L181](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Custom credit role \| 1 character \| 100 characters \| Must belong to Cast or Creator |
| [PRD-CONCEPT-09 · L817](../../../02-PRD.md#9-conceptual-information-and-privacy) | Account \| Private single-field name/email, protected password credentials, and Viewer/Maintainer role. Registration-name defaults differ from catalog person-name defaults in section 5.5. No public account profile or role-selection registration. |
| [DS-POLICY-13 · L88](../../../03-design-system.md#visible-interaction-contracts) | Discovery combines case-insensitive partial display/original-title search, any selected genre, and all other categories. People search is a separate labeled mode. Year is required before month; clearing year also clears month, clearing text preserves other filters. Year-only records do not acquire a month/day. Unknown dates remain available without a date filter, ordered last. Invalid current queries retain text and suppress stale results. Newer requests own the visible results; outdated responses cannot replace them. |
| [BRAIN-A10 · L55](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A10 \| Add, delete, and update cast and creators \| Working person management with defined relationship behavior |

## Implementation

1. Implement managed people list, create and edit using shared list/access/error anatomy plus the people flow references; the absence of a separate managed-list mockup does not add a new design direction.
2. Bind current name/biography policy, typed operations and draft controller. Show same-name identities distinctly and preserve unknown optional context.
3. Handle loading/empty/no-match/error, successful creation/edit, invalid/denied/stale/removed/uncertain states with source-prescribed safe action and draft ownership.
4. Link public people/details correctly while keeping private archived data within managed views; lifecycle actions delegate to T024.
5. Verify real saves, field boundaries and keyboard/navigation/focus at all declared visual contexts.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T020-A1:** Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied.
- **T020-A2:** Validation: one-character professional name is allowed, rejected input is retained intact and no account data appears.
- **T020-A3:** Feedback: stale or unavailable target cannot be silently recreated and lost replies are reconciled before another write.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-023](../../../05-atdd.md#case-atdd-023-createupdate) / End-to-end | Create "V"; edit P-A name to "Alex Rivera" with same UUID. Invalidblank/101 code points and archived-target rows independently. | Then Preserve one shared person identity and show corrections through its visible credits. Reject missing/invalid required information with retained input and no partial save; archived people require restoration before editing. Observation: Compare stored one-character V and edited Alex Rivera under the same UUID; rejected blank/over-bound/archive rows preserve old values and actionable feedback. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-114](../../../05-atdd.md#case-atdd-114-person-name-boundaries) / End-to-end | Repeat("x",0/1/100/101) aftertrim as personname; "V" valid; distinctVrecordsnotmerged. Samebound viaUIandownerrequest. | Then Accept lengths 1 and 100, including a single-character professional name; reject empty and 101 with a field-specific error and no partial change. Preserve the rejected draft for correction, and never merge same-named people. Observation: Submit person-name min/max±1 through form/direct owner and inspect code-point count, exact saved values and safe field feedback without truncation. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-261](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) / End-to-end | F-UI; each state row uses F-CATALOG/F-ACTORS and its source-defined stage. Replace local fixture IDs with deterministic actual IDs; no simulated save/upload/client result counts as execution evidence. | Then the visible heading, identity context, feedback and action match the row oracle And each outcome actually applicable to this flow remains distinct from its pending, failed or unavailable alternatives And theme, width and keyboard changes preserve state and show no restricted data or unsupported feature Observation: Exercise `people`, `person`, `personCredits`, `managedPeople`, `managedPerson`, `createPerson`, `updatePerson`, draft operations in `/people`, `/people/[id]`, `/manage/people` and its editor routes, capturing the current response/operation and each row’s visible heading, identity, controls, retained values and safe next action. Assert the named PeopleList / PersonMetadata / PersonEditor through actual visible layout and accessible names/status rather than hidden fixture text. Run each row independently in both themes at declared widths; verify keyboard/focus and permitted navigation without leaking protected data. Source fragments identify design references, not implementation selectors. \| State reference \| Actor / independent trigger \| Visible semantic oracle \| \| --- \| --- \| --- \| \| people \| Reader; complete `people` with active same-name people and contributions \| Show name search, active catalog people, roles and distinct identity links; show neither account directory nor archived people. Permit independent person navigation. \| \| people-loading \| Reader; hold the current `people` search response \| Show Loading people for current criteria; do not infer emptiness or let a late prior response replace the new search. \| \| people-empty \| Reader; return successful `people` with no active records \| Show No people available as successful empty feedback. Preserve current search and permit returning/changing it; failure remains a separate state. \| \| people-failure \| Reader; fail the current `people` read \| Show People could not load with retry of retained name criteria; an outage is not no-match/empty data. \| \| person-unavailable \| Reader; archive/delete person before an old detail link resolves \| Show This person is unavailable and Return to people; expose no private biography, retained credits or account information. \| \| person \| Reader; complete active `person` and `personCredits` \| Show catalog identity, contribution categories, source context and credits only on active titles with active person. Same-name accounts/people are not substituted. \| \| person-edit \| Maintainer; open new/active person editor and submit permitted values \| Show required professional name, optional biography, effective rules and Save person. Preserve mononyms/Unicode; success updates shared identity across visible credits without merging names. \| \| person-conflict \| Maintainer B; submit stale `updatePerson` after A's change \| Show This person changed, current record and retained draft; require review before a fresh versioned save. No overwrite, duplicate or resurrection action is offered. \| \| person-archived \| Maintainer; open an archived person's managed view \| Show Restore before editing and disable person edits/new assignment. Explain permitted removal of an existing active-movie retained credit, without TV editing or recreating it on restore. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-261-T1-R01 · L10877](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | people; Reader; complete `people` with active same-name people and contributions; Show name search, active catalog people, roles and distinct identity links; show neither account directory nor archived people. Permit independent person navigation. |
| [ATDD-261-T1-R02 · L10878](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | people-loading; Reader; hold the current `people` search response; Show Loading people for current criteria; do not infer emptiness or let a late prior response replace the new search. |
| [ATDD-261-T1-R03 · L10879](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | people-empty; Reader; return successful `people` with no active records; Show No people available as successful empty feedback. Preserve current search and permit returning/changing it; failure remains a separate state. |
| [ATDD-261-T1-R04 · L10880](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | people-failure; Reader; fail the current `people` read; Show People could not load with retry of retained name criteria; an outage is not no-match/empty data. |
| [ATDD-261-T1-R05 · L10881](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | person-unavailable; Reader; archive/delete person before an old detail link resolves; Show This person is unavailable and Return to people; expose no private biography, retained credits or account information. |
| [ATDD-261-T1-R06 · L10882](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | person; Reader; complete active `person` and `personCredits`; Show catalog identity, contribution categories, source context and credits only on active titles with active person. Same-name accounts/people are not substituted. |
| [ATDD-261-T1-R07 · L10883](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | person-edit; Maintainer; open new/active person editor and submit permitted values; Show required professional name, optional biography, effective rules and Save person. Preserve mononyms/Unicode; success updates shared identity across visible credits without merging names. |
| [ATDD-261-T1-R08 · L10884](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | person-conflict; Maintainer B; submit stale `updatePerson` after A's change; Show This person changed, current record and retained draft; require review before a fresh versioned save. No overwrite, duplicate or resurrection action is offered. |
| [ATDD-261-T1-R09 · L10885](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | person-archived; Maintainer; open an archived person's managed view; Show Restore before editing and disable person edits/new assignment. Explain permitted removal of an existing active-movie retained credit, without TV editing or recreating it on restore. |

Partial implementation contributions: [ATDD-025](../../../05-atdd.md#case-atdd-025-missingdeniedcompeting-action), [ATDD-026](../../../05-atdd.md#case-atdd-026-multiple-roles), [ATDD-027](../../../05-atdd.md#case-atdd-027-invalidduplicate-credit), [ATDD-079](../../../05-atdd.md#case-atdd-079-stale-save), [ATDD-081](../../../05-atdd.md#case-atdd-081-similar-real-world-records), [ATDD-262](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T020` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T020` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
| [people / people](../../../design-system/mockup/people.html#people) · [ATDD-261-T1-R01 · L10877](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | Reader; complete `people` with active same-name people and contributions; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show name search, active catalog people, roles and distinct identity links; show neither account directory nor archived people. Permit independent person navigation. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [people / people-loading](../../../design-system/mockup/people.html#people-loading) · [ATDD-261-T1-R02 · L10878](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | Reader; hold the current `people` search response; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Loading people for current criteria; do not infer emptiness or let a late prior response replace the new search. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [people / people-empty](../../../design-system/mockup/people.html#people-empty) · [ATDD-261-T1-R03 · L10879](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | Reader; return successful `people` with no active records; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show No people available as successful empty feedback. Preserve current search and permit returning/changing it; failure remains a separate state. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [people / people-failure](../../../design-system/mockup/people.html#people-failure) · [ATDD-261-T1-R04 · L10880](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | Reader; fail the current `people` read; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show People could not load with retry of retained name criteria; an outage is not no-match/empty data. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [people / person-unavailable](../../../design-system/mockup/people.html#person-unavailable) · [ATDD-261-T1-R05 · L10881](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | Reader; archive/delete person before an old detail link resolves; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show This person is unavailable and Return to people; expose no private biography, retained credits or account information. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [people / person](../../../design-system/mockup/people.html#person) · [ATDD-261-T1-R06 · L10882](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | Reader; complete active `person` and `personCredits`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show catalog identity, contribution categories, source context and credits only on active titles with active person. Same-name accounts/people are not substituted. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [people / person-edit](../../../design-system/mockup/people.html#person-edit) · [ATDD-261-T1-R07 · L10883](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | Maintainer; open new/active person editor and submit permitted values; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show required professional name, optional biography, effective rules and Save person. Preserve mononyms/Unicode; success updates shared identity across visible credits without merging names. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [people / person-conflict](../../../design-system/mockup/people.html#person-conflict) · [ATDD-261-T1-R08 · L10884](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | Maintainer B; submit stale `updatePerson` after A's change; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show This person changed, current record and retained draft; require review before a fresh versioned save. No overwrite, duplicate or resurrection action is offered. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [people / person-archived](../../../design-system/mockup/people.html#person-archived) · [ATDD-261-T1-R09 · L10885](../../../05-atdd.md#case-atdd-261-inspect-people-state-transitions-and-feedback) | Maintainer; open an archived person's managed view; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Restore before editing and disable person edits/new assignment. Explain permitted removal of an existing active-movie retained credit, without TV editing or recreating it on restore. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

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
- Hand the owned outputs to [T022 — Connect credit editing and verify the people outcome](T022-connect-credit-editing-and-verify-the-people-outcome.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
