---
kind: task
id: T018
group: G09
depends_on:
- T004
- T007
- T013
- T015
- T017
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: required
---

# T018 — Connect movie editors and confirmed outcomes

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Editor: valid create/update displays committed values under the same ID; optional omissions stay unknown and retained invalid fields remain fully correctable. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-016 | both |
| ATDD-017 | both |
| ATDD-018 | both |
| ATDD-020 | implements |
| ATDD-021 | both |
| ATDD-079 | implements |
| ATDD-080 | both |
| ATDD-081 | implements |
| ATDD-064 | implements |
| ATDD-260 | implements |
| ATDD-260-T1-R01 | implements |
| ATDD-260-T1-R02 | implements |
| ATDD-260-T1-R03 | implements |
| ATDD-260-T1-R04 | implements |
| ATDD-260-T1-R05 | implements |
| ATDD-260-T1-R06 | implements |
| ATDD-260-T1-R07 | implements |
| ATDD-260-T1-R08 | implements |
| ATDD-260-T1-R09 | implements |
| ATDD-260-T1-R10 | implements |
| FR-004 | both |
| NFR-014 | both |
| A07 | both |
| A14 | both |
| D09 | both |
| AC-016 | both |
| AC-016-C01 | both |
| AC-017 | both |
| AC-017-C01 | both |
| AC-017-C02 | both |
| AC-018 | both |
| AC-018-C01 | both |
| AC-018-C02 | both |
| AC-021 | both |
| AC-021-C01 | both |
| AC-105 | both |
| AC-105-C01 | both |
| G-03 | both |
| PRD-PERM-07 | both |
| PRD-BOUND-03 | both |
| PRD-BOUND-04 | both |
| PRD-CONCEPT-04 | both |
| UI-management-movies | both |
| UI-management-editor | both |
| UI-management-create | both |
| UI-management-saved | both |
| UI-management-save-failed | both |
| UI-management-save-uncertain | both |
| UI-management-save-resolved | both |
| UI-management-target-changed | both |
| UI-management-media-outage | both |
| UI-management-draft-cancel | both |
| DS-POLICY-06 | implements |
| V-WRITE | both |
| TD-ROW-L3561 | both |
| TD-BLOCK-L4070 | both |
| TD-BLOCK-L4365 | both |
| TD-BLOCK-L4549 | both |
| BRAIN-A07 | both |
| BRAIN-A14 | both |
| BRAIN-D09 | both |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T007 — Connect account and session browser journeys](../../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T013 — Connect discovery, details and public people views](../../07-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria.
- [T015 — Build account-isolated browser draft controllers](../../08-draft-recovery/tasks/T015-build-account-isolated-browser-draft-controllers.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request.
- [T017 — Implement authoritative movie creation and edits](T017-implement-authoritative-movie-creation-and-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | frontend/src/routes/manage/movies/+page.svelte | T018 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/manage/movies/new/+page.svelte | T018 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/manage/movies/[id]/+page.svelte | T018 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/features/movie/ | T018 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/movies/ | T018 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.1 Routes, screens and API dependencies](../../../04-spoolin-techdoc.md#31-routes-screens-and-api-dependencies): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.4 Forms, drafts and conflict recovery](../../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.6 Components, accessibility and responsive behavior](../../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-260-T1-R01 · L10826](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | movies; Maintainer; complete `managedTitles` for active movies; Show ManagementList with Create movie and identity-based Edit/Artwork/Archive actions. TV remains read-only, with no direct TV edit/artwork/credit/lifecycle or episode controls. |
| [ATDD-260-T1-R02 · L10827](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | editor; Maintainer B; submit stale `updateMovie` after A changes M-A; Show current saved values and complete retained draft for comparison, field controls and explicit review/reload. Offer no force overwrite; saving again requires reviewed current version. |
| [ATDD-260-T1-R03 · L10828](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | create; Maintainer; open `/manage/movies/new` with current policy/genres; Show title, synopsis, release year, genre selection and Create movie; required guidance and unknown optional fields are honest. Pending submit cannot appear as a saved record. |
| [ATDD-260-T1-R04 · L10829](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | saved; Maintainer; receive confirmed successful `createMovie`/`updateMovie`; Show Movie saved with saved identity/values and available record/history navigation. Update preserves unaffected credits/metadata; only a confirmed response permits success feedback. |
| [ATDD-260-T1-R05 · L10830](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | save-failed; Maintainer; return confirmed validation/domain failure before movie commit; Show Changes were not saved, linked corrections and complete permitted draft; keep prior saved record. A fresh attempt is offered only after the failed outcome is known. |
| [ATDD-260-T1-R06 · L10831](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | save-uncertain; Maintainer; drop the movie write response after submission; Show Save result not confirmed with retained permitted draft and Check original result. Do not label failure/success or suggest blind new submission. |
| [ATDD-260-T1-R07 · L10832](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | save-resolved; Maintainer; recover original successful Catalog operation after lost reply; Show Original save confirmed from its safe original receipt/identity. Refresh the currently authorized record projection, which may differ after later edits; no historical-value reconstruction, resurrection or expiry refresh. No repeat save is needed. |
| [ATDD-260-T1-R08 · L10833](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | target-changed; Maintainer; archive/delete edited target before its save; Show target's current unavailable/archived state with retained permitted draft for review; saving cannot restore/recreate it. Restore separately where allowed before a fresh edit. |
| [ATDD-260-T1-R09 · L10834](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | media-outage; Maintainer; make Media unavailable while Catalog remains reachable; Keep metadata readable/editable and existing attachment identity; show artwork-unavailable feedback/placeholder. Do not imply placeholder means deletion or block unrelated metadata saves. |
| [ATDD-260-T1-R10 · L10835](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | draft-cancel; Maintainer; choose to leave/discard an unsaved edit; Explain loss of current unsaved values, offer Cancel first and separate discard. Cancel retains input; confirmed discard leaves saved movie unchanged and reconciles any original server-draft operation. |
| [FR-004 · L238](../../../02-PRD.md#61-functional-requirements) | Create/edit movies and support archive, restore, and confirmed permanent deletion \| A07/A14 Required; approved lifecycle Committed \| AC-016 through AC-021, AC-108 through AC-110, AC-130, AC-143/AC-144, AC-148, AC-155 |
| [NFR-014 · L272](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preserve stale drafts and prevent unintended duplicate effects without merging distinct works/people \| Approved product policy Committed; D09 mechanism Recommended; D21 safe replay Committed \| AC-104 through AC-107 |
| [A07 · L286](../../../02-PRD.md#63-project-requirements-coverage) | Add, update, delete movies \| FR-004 / AC-016 through AC-021, AC-108 through AC-110, AC-130 |
| [A14 · L293](../../../02-PRD.md#63-project-requirements-coverage) | Sufficient operation feedback \| NFR-007; FR-001/FR-002/FR-004/FR-009 / AC-002, AC-009, AC-018, AC-038, AC-085 |
| [D09 · L310](../../../02-PRD.md#64-project-decision-register) | Optimistic concurrency; separate duplicate requests/data \| Stale-save rejection selected as product behavior; mechanism stays with techdoc. NFR-014 / AC-104 through AC-107 |
| [AC-016 · L368](../../../02-PRD.md#us-003-maintain-movies) | Create \| A Maintainer supplies title, synopsis, genre(s), and release year \| A valid creation completes \| One active movie appears with its saved information and an attributable audit entry; missing optional values remain unknown. |
| [AC-016-C01 · L368](../../../02-PRD.md#us-003-maintain-movies) | One active movie appears with its saved information and an attributable audit entry; missing optional values remain unknown. |
| [AC-017 · L369](../../../02-PRD.md#us-003-maintain-movies) | Update \| An active movie is open for editing \| Valid edits are saved \| Show the confirmed updated information under the same movie identity, preserving unaffected details and relationships. Clearing an optional value differs from leaving it unchanged. |
| [AC-017-C01 · L369](../../../02-PRD.md#us-003-maintain-movies) | Show the confirmed updated information under the same movie identity, preserving unaffected details and relationships. |
| [AC-017-C02 · L369](../../../02-PRD.md#us-003-maintain-movies) | Clearing an optional value differs from leaving it unchanged. |
| [AC-018 · L370](../../../02-PRD.md#us-003-maintain-movies) | Validation / denied write \| Required data are blank/missing, changed input violates the effective bounds/content policy, or permission is absent \| A save is attempted \| No partial catalog edit occurs; show corrective/permission feedback and retain permitted input. Never retain or echo passwords as form-recovery data. |
| [AC-018-C01 · L370](../../../02-PRD.md#us-003-maintain-movies) | No partial catalog edit occurs; show corrective/permission feedback and retain permitted input. |
| [AC-018-C02 · L370](../../../02-PRD.md#us-003-maintain-movies) | Never retain or echo passwords as form-recovery data. |
| [AC-021 · L373](../../../02-PRD.md#us-003-maintain-movies) | Lost response / artwork outage \| A movie save loses confirmation or artwork handling is unavailable \| The Maintainer checks the outcome or edits only metadata \| Resolve uncertain saves before suggesting resubmission; metadata-only work remains usable independently of artwork availability. |
| [AC-021-C01 · L373](../../../02-PRD.md#us-003-maintain-movies) | Resolve uncertain saves before suggesting resubmission; metadata-only work remains usable independently of artwork availability. |
| [AC-105 · L492](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Repeated submission \| The same intended change is submitted twice or after lost confirmation \| The outcome is resolved \| Show one completed effect or its pending outcome without unintended duplicates; button disabling alone is not the promised behavior. |
| [AC-105-C01 · L492](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Show one completed effect or its pending outcome without unintended duplicates; button disabling alone is not the promised behavior. |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [PRD-PERM-07 · L74](../../../02-PRD.md#31-permission-matrix) | Approve an AI-proposed movie \| No \| No \| Yes, explicitly in chat |
| [PRD-BOUND-03 · L179](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Movie display title / optional original title \| 1 character \| 255 characters \| Display title is required; original title may remain unknown |
| [PRD-BOUND-04 · L180](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Movie synopsis \| 1 character \| 5,000 characters \| Required; length alone does not establish descriptive quality |
| [PRD-CONCEPT-04 · L812](../../../02-PRD.md#9-conceptual-information-and-privacy) | Release context \| Original release for a movie; first-air for a series. Preserve year, year/month, or full date and supplied regional/source context. No invented January 1 or day-of-month. |
| [DS-POLICY-06 · L70](../../../03-design-system.md#typography-layout-and-shape) | Body and controls share Avenir Next → Segoe UI → Arial → sans-serif. Dark display headings use the same family; light display headings use Didot → Times New Roman → serif, retaining C’s editorial character. These are local system stacks, with no downloaded font or licensing claim. Font fallback may change wrapping; content must accommodate it. |
| [V-WRITE · L6387](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-WRITE \| Two real sessions/transactions issue valid/invalid/stale/repeated movie/person/credit writes; assert affected rows, versions, exact stored values, no partial changes, successful audit/work only on committed mutation; real distinct same-name works remain distinct |
| [TD-ROW-L3561 · L3561](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | work_attempt \| operational_event phases: work/generation/attempt/lease preserved; started_at and ended_at→STARTED/FINISHED.created_at; outcome→attempt_outcome; error/operator retained \| Start before effect, one completion, safe interrupted recovery and retained prior generations |
| [TD-BLOCK-L4070 · L4070](../../../04-spoolin-techdoc.md#crypto_key_version-ddl) | crypto_key_version DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4365 · L4365](../../../04-spoolin-techdoc.md#request_rate_window-ddl) | request_rate_window DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4549 · L4549](../../../04-spoolin-techdoc.md#genre-ddl) | genre DDL — complete sql contract, all members, constraints and field numbers |
| [BRAIN-A07 · L52](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A07 \| Add, delete, and update movies \| Working operations with validation and understandable outcomes |
| [BRAIN-A14 · L59](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A14 \| Sufficient feedback for upload, listing, updating, and searching \| Loading, success, failure, validation, and empty states integrated into features |
| [BRAIN-D09 · L78](../../../01-brainstorming-result.md#4-decision-register) | D09 \| Recommended \| Protect stale updates with optimistic concurrency; distinguish duplicate-request handling from duplicate-data rules |

## Implementation

1. Build managed movie list and create/edit routes with current policy guidance, optional unknown/clear controls and generated operations. Reuse shared components; do not copy mockup form logic.
2. Bind permitted draft recovery and base/current/draft comparison. Disable repeated UI submission while retaining the same intent UUID; backend receipts remain the actual duplicate safeguard.
3. Show confirmed save, validation/denied, changed/removed, failed and uncertain results distinctly. Resolve original outcome before any new write; metadata remains editable during image service outage.
4. Invalidate affected lists/details only after confirmed mutation and issue new cursors, preserving current user criteria.
5. Exercise management state rows with real Core/Catalog and keyboard/error summary checks; full draft reauthentication closure belongs to T016.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T018-A1:** Editor: valid create/update displays committed values under the same ID; optional omissions stay unknown and retained invalid fields remain fully correctable.
- **T018-A2:** Conflict: two Maintainers cannot overwrite newer data; stale review shows current and draft without force overwrite.
- **T018-A3:** Uncertainty: no lost response becomes a failure or blind repeat; confirmed metadata-only save works while artwork is unavailable.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-016](../../../05-atdd.md#case-atdd-016-create) / End-to-end | New "Harbor Story", synopsis "A pilot returns home.", genres G-Drama, release 2024-10, optional fields absent; submit once as Maintainer A. | Then One active movie appears with its saved information and an attributable audit entry; missing optional values remain unknown. Observation: Capture original create UUID, confirmed saved new movie ID and exact required/optional fields in a fresh Catalog read; correlate one successful audit/work receipt. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-017](../../../05-atdd.md#case-atdd-017-update) / End-to-end | M-A version 5/originalTitle"Cahaya"; independent patches synopsis"Revised ⟨LF U+000A⟩ second line.", originalTitle:null, and synopsis-only with originalTitle omitted. | Then Show the confirmed updated information under the same movie identity, preserving unaffected details and relationships. Clearing an optional value differs from leaving it unchanged. Observation: Observe actual argument presence and saved originalTitle/synopsis for each patch; compare unchanged, cleared and revised newline-containing values plus version increment. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-018](../../../05-atdd.md#case-atdd-018-validation--denied-write) / End-to-end | Rows: whitespace title; missing synopsis; no genres; overlimit changed title; valid Viewerwrite; expired session. Capture pre/post rows and retained permitted draft. | Then No partial catalog edit occurs; show corrective/permission feedback and retain permitted input. Never retain or echo passwords as form-recovery data. Observation: Record field/role/session error and safe retained draft for each rejected write; compare domain/audit/work before/after to prove no successful partial change. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-021](../../../05-atdd.md#case-atdd-021-lost-response--artwork-outage) / End-to-end | Commit M-A metadata then drop response; lookup same CATALOG operation. Separately stop Media and submit metadata-only edit. | Then Resolve uncertain saves before suggesting resubmission; metadata-only work remains usable independently of artwork availability. Observation: Drop the reply after actual metadata commit and inspect original receipt before showing success; with Media stopped, metadata-only edits still persist without image-dependent success claims. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-080](../../../05-atdd.md#case-atdd-080-repeated-submission) / End-to-end | SameUUID/content from two clients or lost reply after commit; receipt one effect/PENDING; new UUIDsame-title is distinct user intent, not text merge. | Then Show one completed effect or its pending outcome without unintended duplicates; button disabling alone is not the promised behavior. Observation: Double-submit/retry the same intent with matching/mismatched payload and response loss; inspect original receipt and exactly one logical effect. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |

Partial implementation contributions: [ATDD-020](../../../05-atdd.md#case-atdd-020-changedremoved-target), [ATDD-079](../../../05-atdd.md#case-atdd-079-stale-save), [ATDD-081](../../../05-atdd.md#case-atdd-081-similar-real-world-records), [ATDD-064](../../../05-atdd.md#case-atdd-064-artwork-outage), [ATDD-260](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T018` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T018` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| visual-review | Inspect relevant changed states against the adopted references; retain exhaustive matrix coverage with T044 unless required by this feature | `/Users/mekari/Developer/personal-project/movie-app` | Required visual differences resolved at current revision; no overflow/missing identity | yes |
| interaction-accessibility | Exercise keyboard/focus/labels/errors/live regions/dialogs, zoom/reduced motion and source browser/host matrix independently of pixels | `/Users/mekari/Developer/personal-project/movie-app` | Meaningful accessible interaction and safe recovery, with unavailable contexts kept unverified | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Use the linked adopted references and actual feature for relevant changed states. Browser assertions and direct inspection establish ordinary behavior; retain screenshots when visual inspection needs them. Record actual fixtures, environment and outcomes. Exhaustive theme/width/browser matrices below remain release coverage references owned by T044 unless the selected behavior needs that exact observation. Do not delete their case IDs or claim deferred proof passed.

Check changed keyboard/focus/labels/errors/announcements and relevant zoom, motion, touch and browser behavior. Preserve known regressions and actual reader observations required to prove speech; exhaustive compatibility sweeps remain release-owned. For AI states include actual Claude/ChatGPT host context at T032/T033; standalone resource evidence cannot prove host isolation. Existing mockups are adopted references, not automatic pixel baselines or production test passes.

| Flow/state reference | Actor and fixture condition | Trigger and expected comparison |
|---|---|---|
| [management / movies](../../../design-system/mockup/management.html#movies) · [ATDD-260-T1-R01 · L10826](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer; complete `managedTitles` for active movies; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show ManagementList with Create movie and identity-based Edit/Artwork/Archive actions. TV remains read-only, with no direct TV edit/artwork/credit/lifecycle or episode controls. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [management / editor](../../../design-system/mockup/management.html#editor) · [ATDD-260-T1-R02 · L10827](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer B; submit stale `updateMovie` after A changes M-A; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show current saved values and complete retained draft for comparison, field controls and explicit review/reload. Offer no force overwrite; saving again requires reviewed current version. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [management / create](../../../design-system/mockup/management.html#create) · [ATDD-260-T1-R03 · L10828](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer; open `/manage/movies/new` with current policy/genres; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show title, synopsis, release year, genre selection and Create movie; required guidance and unknown optional fields are honest. Pending submit cannot appear as a saved record. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [management / saved](../../../design-system/mockup/management.html#saved) · [ATDD-260-T1-R04 · L10829](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer; receive confirmed successful `createMovie`/`updateMovie`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Movie saved with saved identity/values and available record/history navigation. Update preserves unaffected credits/metadata; only a confirmed response permits success feedback. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [management / save-failed](../../../design-system/mockup/management.html#save-failed) · [ATDD-260-T1-R05 · L10830](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer; return confirmed validation/domain failure before movie commit; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Changes were not saved, linked corrections and complete permitted draft; keep prior saved record. A fresh attempt is offered only after the failed outcome is known. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [management / save-uncertain](../../../design-system/mockup/management.html#save-uncertain) · [ATDD-260-T1-R06 · L10831](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer; drop the movie write response after submission; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Save result not confirmed with retained permitted draft and Check original result. Do not label failure/success or suggest blind new submission. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [management / save-resolved](../../../design-system/mockup/management.html#save-resolved) · [ATDD-260-T1-R07 · L10832](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer; recover original successful Catalog operation after lost reply; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Original save confirmed from its safe original receipt/identity. Refresh the currently authorized record projection, which may differ after later edits; no historical-value reconstruction, resurrection or expiry refresh. No repeat save is needed. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [management / target-changed](../../../design-system/mockup/management.html#target-changed) · [ATDD-260-T1-R08 · L10833](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer; archive/delete edited target before its save; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show target's current unavailable/archived state with retained permitted draft for review; saving cannot restore/recreate it. Restore separately where allowed before a fresh edit. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [management / media-outage](../../../design-system/mockup/management.html#media-outage) · [ATDD-260-T1-R09 · L10834](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer; make Media unavailable while Catalog remains reachable; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Keep metadata readable/editable and existing attachment identity; show artwork-unavailable feedback/placeholder. Do not imply placeholder means deletion or block unrelated metadata saves. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [management / draft-cancel](../../../design-system/mockup/management.html#draft-cancel) · [ATDD-260-T1-R10 · L10835](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback) | Maintainer; choose to leave/discard an unsaved edit; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Explain loss of current unsaved values, offer Cancel first and separate discard. Cancel retains input; confirmed discard leaves saved movie unchanged and reconciles any original server-draft operation. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

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
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
