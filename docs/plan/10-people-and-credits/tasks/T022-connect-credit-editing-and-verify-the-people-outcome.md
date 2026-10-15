---
kind: task
id: T022
group: G10
depends_on:
- T004
- T007
- T015
- T018
- T020
- T021
readiness: Ready
status: Not started
risk: high
test_strategy: test-first
visual_review: required
---

# T022 — Connect credit editing and verify the people outcome

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-026 | both |
| ATDD-027 | both |
| ATDD-028 | implements |
| ATDD-079 | verifies |
| ATDD-081 | verifies |
| ATDD-262 | both |
| ATDD-262-T1-R01 | both |
| ATDD-262-T1-R02 | both |
| ATDD-262-T1-R03 | both |
| FR-006 | both |
| NFR-014 | both |
| A09 | both |
| A10 | both |
| D09 | both |
| D25 | both |
| AC-026 | both |
| AC-026-C01 | both |
| AC-027 | both |
| AC-027-C01 | both |
| AC-027-C02 | both |
| AC-104 | both |
| AC-104-C01 | both |
| AC-107 | both |
| AC-107-C01 | both |
| G-03 | both |
| OD-009 | both |
| PRD-PERM-01 | both |
| PRD-PERM-05 | both |
| PRD-POLICY-05 | both |
| PRD-BOUND-05 | both |
| UI-credits-credits | both |
| UI-credits-credit-duplicate | both |
| UI-credits-credit-retained | both |
| TD-ROW-L3596 | both |
| TD-ROW-L3597 | both |
| TD-ROW-L3624 | both |
| TD-BLOCK-L4628 | both |
| BRAIN-A09 | both |
| BRAIN-A10 | both |
| BRAIN-D09 | both |
| BRAIN-D25 | both |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T007 — Connect account and session browser journeys](../../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T015 — Build account-isolated browser draft controllers](../../08-draft-recovery/tasks/T015-build-account-isolated-browser-draft-controllers.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request.
- [T018 — Connect movie editors and confirmed outcomes](../../09-movie-management/tasks/T018-connect-movie-editors-and-confirmed-outcomes.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Editor: valid create/update displays committed values under the same ID; optional omissions stay unknown and retained invalid fields remain fully correctable.
- [T020 — Connect people management and retained edits](T020-connect-people-management-and-retained-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied.
- [T021 — Implement ordered credits and custom contribution roles](T021-implement-ordered-credits-and-custom-contribution-roles.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | frontend/src/lib/features/credit/ | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/credits/ | T022 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

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
| [FR-006 · L240](../../../02-PRD.md#61-functional-requirements) | Maintain cast/creator credits, starter/custom roles, and multiple roles/character names; respect person lifecycle \| A09/A10 support; D25 Committed; product policy Committed (section 13.1) \| AC-026 through AC-028, AC-132, AC-135, AC-136, AC-143/AC-144 |
| [NFR-014 · L272](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preserve stale drafts and prevent unintended duplicate effects without merging distinct works/people \| Approved product policy Committed; D09 mechanism Recommended; D21 safe replay Committed \| AC-104 through AC-107 |
| [A09 · L288](../../../02-PRD.md#63-project-requirements-coverage) | List cast and creators \| FR-001/FR-005/FR-006 / AC-004, AC-022, AC-026, AC-134 |
| [A10 · L289](../../../02-PRD.md#63-project-requirements-coverage) | Add, update, delete cast and creators \| FR-005/FR-006 / AC-023 through AC-028, AC-131 through AC-133 |
| [D09 · L310](../../../02-PRD.md#64-project-decision-register) | Optimistic concurrency; separate duplicate requests/data \| Stale-save rejection selected as product behavior; mechanism stays with techdoc. NFR-014 / AC-104 through AC-107 |
| [D25 · L326](../../../02-PRD.md#64-project-decision-register) | Metadata, type/genres, stable identities, provenance \| FR-006/FR-007 / AC-026 through AC-032 |
| [AC-026 · L390](../../../02-PRD.md#us-004-maintain-people-and-credits) | Multiple roles \| An active shared person and active movie exist \| A Maintainer assigns credits \| Actor/Director/Writer/Producer are available; custom roles fit Cast or Creator; one person may have multiple roles and multiple acting character names without cloning the person. |
| [AC-026-C01 · L390](../../../02-PRD.md#us-004-maintain-people-and-credits) | Actor/Director/Writer/Producer are available; custom roles fit Cast or Creator; one person may have multiple roles and multiple acting character names without cloning the person. |
| [AC-027 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Invalid/duplicate credit \| A name/category/reference is missing, the selected person is archived, or an identical credit exists \| The Maintainer saves a new credit \| Request correction for invalid input; reject assignment to an archived person until restored, and do not create duplicate identical display entries. Custom labels use consistent category/identity treatment. |
| [AC-027-C01 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Request correction for invalid input; reject assignment to an archived person until restored, and do not create duplicate identical display entries. |
| [AC-027-C02 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Custom labels use consistent category/identity treatment. |
| [AC-104 · L491](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Stale save \| Two Maintainers edit the same movie/person \| One saves before the other \| Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-104-C01 · L491](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-107 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Similar real-world records \| Independent works/people share names \| Records are created or compared \| Preserve distinct identities and do not merge by text alone; explain genuine identity conflicts without silently changing another record. |
| [AC-107-C01 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Preserve distinct identities and do not merge by text alone; explain genuine identity conflicts without silently changing another record. |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [OD-009 · L1022](../../../02-PRD.md#131-resolved-product-decisions) | Reject stale writes, retain draft, require review; no force overwrite \| NFR-014; AC-020/AC-025/AC-104; concurrency mechanism transfers to techdoc |
| [PRD-PERM-01 · L68](../../../02-PRD.md#31-permission-matrix) | Browse/search/filter active movies and TV series; view metadata and public people/credits \| Yes \| Yes \| Yes |
| [PRD-PERM-05 · L72](../../../02-PRD.md#31-permission-matrix) | Manage shared people and custom credit roles \| No \| No \| Yes |
| [PRD-POLICY-05 · L122](../../../02-PRD.md#51-included-behavior) | Cast and creators share reusable Person records. Start with Actor in Cast and Director, Writer, and Producer in Creator. Maintainers may add custom labels within either category. A person may have several roles on a movie; an acting credit may list several character names. Repeated identical credits do not create duplicated display entries. Names and titles are not globally unique identities; similarly named people and remakes remain distinct. |
| [PRD-BOUND-05 · L181](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Custom credit role \| 1 character \| 100 characters \| Must belong to Cast or Creator |
| [TD-ROW-L3596 · L3596](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `person`, `credit` \| Catalog `person/persistence` and `credit/persistence` \| Public people are independent of accounts; contributions have their own uniqueness/version |
| [TD-ROW-L3597 · L3597](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `genre`, `credit_role` \| Catalog `taxonomy/persistence` \| Reusable definitions with distinct genre versus contribution-role rules |
| [TD-ROW-L3624 · L3624](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `title`, `person`, `genre`, `title_genre`, `credit_role`, `credit` \| Existing Title/Person/Genre/CreditRole/Credit and corresponding protobuf projections \| Apply independent visibility, presence and precision; derive names through permitted joins |
| [TD-BLOCK-L4628 · L4628](../../../04-spoolin-techdoc.md#credit_role-ddl) | credit_role DDL — complete sql contract, all members, constraints and field numbers |
| [BRAIN-A09 · L54](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A09 \| List cast and creators \| Accessible people/credit views |
| [BRAIN-A10 · L55](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A10 \| Add, delete, and update cast and creators \| Working person management with defined relationship behavior |
| [BRAIN-D09 · L78](../../../01-brainstorming-result.md#4-decision-register) | D09 \| Recommended \| Protect stale updates with optimistic concurrency; distinguish duplicate-request handling from duplicate-data rules |
| [BRAIN-D25 · L94](../../../01-brainstorming-result.md#4-decision-register) | D25 \| Committed \| Categorize movies and TV series by title type and genres, include synopses and useful descriptive metadata, and retain stable identities and source provenance to support future review aggregation; detailed baseline in section 8.2 |

## Implementation

1. Compose the movie credit panel with active-person picker, Cast/Creator grouping, starter/custom roles and ordered character inputs. Role creation stays contextual, not a separate administration product.
2. Bind typed operations, policy guidance, drafts, version conflicts and original operation reconciliation. Retained archived-person credits have explicit private context and permitted removal only.
3. Handle role/credit creation, duplicate, invalid references, denied/stale/failed/uncertain and empty states; do not collapse names into identities or split slash-containing names.
4. Run real people create/edit→credit add/update/remove→public detail flow and verify cross-record visibility, stable IDs and audit/receipt side effects.
5. Collect T019/T020/T021 evidence and close integrated people/credits only after all member tasks and this full boundary pass; archive interactions remain T024/T043.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T022-A1:** Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning.
- **T022-A2:** Negative: Viewer, archived person assignment, direct TV edit, stale reference and repeated identical request never create unintended relationships.
- **T022-A3:** UI: each credit state shows full context, accessible correction and safe next action under both themes and narrow layouts.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-026](../../../05-atdd.md#case-atdd-026-multiple-roles) / End-to-end | P-A gets Actor with ["Maya","Young Maya","Maya / Echo"], Director and Writer on M-A; custom CAST "Stunt" and CREATOR "Editor" without characters. | Then Actor/Director/Writer/Producer are available; custom roles fit Cast or Creator; one person may have multiple roles and multiple acting character names without cloning the person. Observation: Inspect one person UUID with Actor/Director/Writer credits, exact ordered character array including Maya / Echo and new role identity without granting account permission. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-027](../../../05-atdd.md#case-atdd-027-invalidduplicate-credit) / End-to-end | Rows missing person/role/category, archivedP-B, existing(M-A, P-A, Actor), duplicate normalized custom role, Creatorwithcharacters. | Then Request correction for invalid input; reject assignment to an archived person until restored, and do not create duplicate identical display entries. Custom labels use consistent category/identity treatment. Observation: Compare duplicate/invalid relationship and role field codes with unchanged title/person/credit rows; creator/disabled-character roles cannot accept character payload. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-079](../../../05-atdd.md#case-atdd-079-stale-save) / End-to-end | AandBopenmovie/person version 5; A saves6; B submits5; retain base/current/draft and explicit review before fresh UUID/version 6. | Then Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. Observation: Race actual versioned save and reread winner/current version plus conflict feedback and retained input; rejected stale save changes no domain value. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-081](../../../05-atdd.md#case-atdd-081-similar-real-world-records) / End-to-end | Create two works"The Last Light" and two people"Alex" underindependentUUIDs; separately same(source, externalID, kind) conflict safely. | Then Preserve distinct identities and do not merge by text alone; explain genuine identity conflicts without silently changing another record. Observation: Create distinct same-spelling works/people with independent IDs and source identity, then read both; idempotency must not merge real duplicate-looking data. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-262](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback) / End-to-end | F-UI; each state row uses F-CATALOG/F-ACTORS and its source-defined stage. Replace local fixture IDs with deterministic actual IDs; no simulated save/upload/client result counts as execution evidence. | Then the visible heading, identity context, feedback and action match the row oracle And each outcome actually applicable to this flow remains distinct from its pending, failed or unavailable alternatives And theme, width and keyboard changes preserve state and show no restricted data or unsupported feature Observation: Exercise `managedCredits`, `people`, `creditRoles`, `createRole`, `addCredit`, `updateCredit`, `removeCredit` in Panel in the managed movie editor, capturing the current response/operation and each row’s visible heading, identity, controls, retained values and safe next action. Assert the named CreditEditor / RolePicker / CharacterFields through actual visible layout and accessible names/status rather than hidden fixture text. Run each row independently in both themes at declared widths; verify keyboard/focus and permitted navigation without leaking protected data. Source fragments identify design references, not implementation selectors. \| State reference \| Actor / independent trigger \| Visible semantic oracle \| \| --- \| --- \| --- \| \| credits \| Maintainer; load `managedCredits`, active `people` and `creditRoles` in movie editor \| Show shared person picker, Cast/Creator, starter/custom role and optional complete character-name entries. Multiple roles reuse one person; a slash inside a name is data, not a splitter. \| \| credit-duplicate \| Maintainer; submit identical `addCredit` \| Show Review the credit/no duplicate added with retained correction values; display the existing contribution once. A genuinely distinct role can be selected without cloning the person. \| \| credit-retained \| Maintainer; inspect/remove archived P-B's retained credit on an active movie \| Label the relationship private and hidden publicly; offer permitted removal with current references. Preserve the person and explain that later restore cannot recreate the removed credit; no TV edit appears. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-262-T1-R01 · L10927](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback) | credits; Maintainer; load `managedCredits`, active `people` and `creditRoles` in movie editor; Show shared person picker, Cast/Creator, starter/custom role and optional complete character-name entries. Multiple roles reuse one person; a slash inside a name is data, not a splitter. |
| [ATDD-262-T1-R02 · L10928](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback) | credit-duplicate; Maintainer; submit identical `addCredit`; Show Review the credit/no duplicate added with retained correction values; display the existing contribution once. A genuinely distinct role can be selected without cloning the person. |
| [ATDD-262-T1-R03 · L10929](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback) | credit-retained; Maintainer; inspect/remove archived P-B's retained credit on an active movie; Label the relationship private and hidden publicly; offer permitted removal with current references. Preserve the person and explain that later restore cannot recreate the removed credit; no TV edit appears. |

Partial implementation contributions: [ATDD-028](../../../05-atdd.md#case-atdd-028-remove-credit--changing-references). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T022` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T022` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
| [credits / credits](../../../design-system/mockup/credits.html#credits) · [ATDD-262-T1-R01 · L10927](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback) | Maintainer; load `managedCredits`, active `people` and `creditRoles` in movie editor; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show shared person picker, Cast/Creator, starter/custom role and optional complete character-name entries. Multiple roles reuse one person; a slash inside a name is data, not a splitter. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [credits / credit-duplicate](../../../design-system/mockup/credits.html#credit-duplicate) · [ATDD-262-T1-R02 · L10928](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback) | Maintainer; submit identical `addCredit`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Review the credit/no duplicate added with retained correction values; display the existing contribution once. A genuinely distinct role can be selected without cloning the person. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [credits / credit-retained](../../../design-system/mockup/credits.html#credit-retained) · [ATDD-262-T1-R03 · L10929](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback) | Maintainer; inspect/remove archived P-B's retained credit on an active movie; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Label the relationship private and hidden publicly; offer permitted removal with current references. Preserve the person and explain that later restore cannot recreate the removed credit; no TV edit appears. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

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
