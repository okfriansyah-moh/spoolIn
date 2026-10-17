---
kind: task
id: T025
group: G15
depends_on:
- T004
- T010
- T013
- T017
- T019
- T021
- T024
readiness: Ready
status: Implementation complete
risk: high
test_strategy: test-first
visual_review: required
---

# T025 — Connect private archives and irreversible confirmations

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-019 | both |
| ATDD-020 | verifies |
| ATDD-024 | both |
| ATDD-025 | verifies |
| ATDD-028 | verifies |
| ATDD-082 | both |
| ATDD-083 | both |
| ATDD-084 | both |
| ATDD-103 | both |
| ATDD-104 | both |
| ATDD-105 | both |
| ATDD-106 | both |
| ATDD-107 | both |
| ATDD-108 | both |
| ATDD-109 | both |
| ATDD-110 | both |
| ATDD-111 | both |
| ATDD-087 | both |
| ATDD-264 | both |
| ATDD-264-T1-R01 | both |
| ATDD-264-T1-R02 | both |
| ATDD-264-T1-R03 | both |
| ATDD-264-T1-R04 | both |
| ATDD-264-T1-R05 | both |
| ATDD-264-T1-R06 | both |
| ATDD-264-T1-R07 | both |
| ATDD-264-T1-R08 | both |
| ATDD-264-T1-R09 | both |
| ATDD-264-T1-R10 | both |
| ATDD-264-T1-R11 | both |
| ATDD-264-T1-R12 | both |
| ATDD-264-T1-R13 | both |
| ATDD-264-T1-R14 | both |
| ATDD-264-T1-R15 | both |
| ATDD-264-T1-R16 | both |
| FR-001 | both |
| FR-002 | both |
| FR-004 | both |
| FR-005 | both |
| FR-006 | both |
| FR-012 | both |
| FR-013 | both |
| A07 | both |
| A09 | both |
| A10 | both |
| A11 | both |
| D25 | both |
| AC-019 | both |
| AC-019-C01 | both |
| AC-019-C02 | both |
| AC-020 | both |
| AC-020-C01 | both |
| AC-108 | both |
| AC-108-C01 | both |
| AC-108-C02 | both |
| AC-109 | both |
| AC-109-C01 | both |
| AC-109-C02 | both |
| AC-109-C03 | both |
| AC-110 | both |
| AC-110-C01 | both |
| AC-110-C02 | both |
| AC-110-C03 | both |
| AC-130 | both |
| AC-130-C01 | both |
| AC-024 | both |
| AC-024-C01 | both |
| AC-024-C02 | both |
| AC-024-C03 | both |
| AC-025 | both |
| AC-025-C01 | both |
| AC-025-C02 | both |
| AC-129 | both |
| AC-129-C01 | both |
| AC-028 | both |
| AC-028-C01 | both |
| AC-028-C02 | both |
| AC-028-C03 | both |
| AC-131 | both |
| AC-131-C01 | both |
| AC-131-C02 | both |
| AC-132 | both |
| AC-132-C01 | both |
| AC-132-C02 | both |
| AC-132-C03 | both |
| AC-133 | both |
| AC-133-C01 | both |
| AC-133-C02 | both |
| AC-133-C03 | both |
| AC-134 | both |
| AC-134-C01 | both |
| AC-134-C02 | both |
| AC-134-C03 | both |
| AC-135 | both |
| AC-135-C01 | both |
| AC-135-C02 | both |
| AC-135-C03 | both |
| AC-135-C04 | both |
| AC-136 | both |
| AC-136-C01 | both |
| AC-136-C02 | both |
| AC-136-C03 | both |
| AC-137 | both |
| AC-137-C01 | both |
| AC-137-C02 | both |
| AC-137-C03 | both |
| AC-113 | both |
| AC-113-C01 | both |
| AC-113-C02 | both |
| G-03 | both |
| G-05 | both |
| OD-002 | both |
| OD-004 | both |
| OD-009 | both |
| US-004 | both |
| PRD-PERM-04 | both |
| PRD-POLICY-07 | both |
| PRD-POLICY-08 | both |
| PRD-POLICY-10 | both |
| PRD-CONCEPT-06 | both |
| PRD-CONCEPT-08 | both |
| UI-archives-movie-archive | both |
| UI-archives-movie-impact | both |
| UI-archives-movie-restore | both |
| UI-archives-movie-delete | both |
| UI-archives-person-archive | both |
| UI-archives-person-impact | both |
| UI-archives-person-restore | both |
| UI-archives-person-delete | both |
| UI-archives-impact-changed | both |
| UI-archives-deleted | both |
| UI-archives-archive-loading | both |
| UI-archives-archive-empty | both |
| UI-archives-archive-failed | both |
| UI-archives-archive-uncertain | both |
| UI-archives-archive-denied | both |
| UI-archives-impact-current | both |
| DS-POLICY-12 | implements |
| DS-POLICY-19 | implements |
| V-LIFE | both |
| TD-ROW-L3565 | both |
| TD-ROW-L3590 | both |
| TD-ROW-L3598 | both |
| TD-ROW-L3625 | both |
| TD-ROW-L5182 | both |
| TD-ROW-L5511 | both |
| TD-BLOCK-L3776 | both |
| TD-BLOCK-L3884 | both |
| TD-BLOCK-L4117 | both |
| TD-BLOCK-L4162 | both |
| TD-BLOCK-L4210 | both |
| TD-BLOCK-L4313 | both |
| TD-BLOCK-L4428 | both |
| TD-BLOCK-L4504 | both |
| TD-BLOCK-L4586 | both |
| TD-BLOCK-L4674 | both |
| TD-BLOCK-L4728 | both |
| TD-BLOCK-L5111 | both |
| BRAIN-A07 | both |
| BRAIN-A09 | both |
| BRAIN-A10 | both |
| BRAIN-A11 | both |
| BRAIN-D25 | both |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T010 — Connect account and session browser journeys](../../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T013 — Connect discovery, details and public people views](../../10-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria.
- [T017 — Connect movie editors and confirmed outcomes](../../12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Editor: valid create/update displays committed values under the same ID; optional omissions stay unknown and retained invalid fields remain fully correctable.
- [T019 — Connect people management and retained edits](../../13-people-and-credits/tasks/T019-connect-people-management-and-retained-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Management: permitted A creates/edits a shared person and sees credit projections update; Viewer direct management remains denied.
- [T021 — Connect credit editing and verify the people outcome](../../13-people-and-credits/tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Journey: A creates person and role, assigns two different roles, edits characters then removes one credit; public detail shows exact surviving relationship without cloning.
- [T024 — Implement current-impact archive, restore and deletion](T024-implement-current-impact-archive-restore-and-deletion.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | frontend/src/routes/manage/archives/+page.svelte | T025 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/features/archive/ | T025 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/archives/ | T025 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

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
| [FR-001 · L235](../../../02-PRD.md#61-functional-requirements) | Browse visible movies/TV titles, artwork, active people, and public credits \| A06/A09/A14 Required; D24/D25 Committed; product policy Committed (section 13.1) \| AC-001 through AC-005, AC-113, AC-134, AC-135 |
| [FR-002 · L236](../../../02-PRD.md#61-functional-requirements) | Search movie titles and cast/creator names with the approved matching and visibility rules \| A11/A14 Required; product policy Committed (section 13.1) \| AC-006 through AC-010, AC-022, AC-134, AC-147, AC-159 |
| [FR-004 · L238](../../../02-PRD.md#61-functional-requirements) | Create/edit movies and support archive, restore, and confirmed permanent deletion \| A07/A14 Required; approved lifecycle Committed \| AC-016 through AC-021, AC-108 through AC-110, AC-130, AC-143/AC-144, AC-148, AC-155 |
| [FR-005 · L239](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-006 · L240](../../../02-PRD.md#61-functional-requirements) | Maintain cast/creator credits, starter/custom roles, and multiple roles/character names; respect person lifecycle \| A09/A10 support; D25 Committed; product policy Committed (section 13.1) \| AC-026 through AC-028, AC-132, AC-135, AC-136, AC-143/AC-144 |
| [FR-012 · L246](../../../02-PRD.md#61-functional-requirements) | Enforce the public/Viewer/Maintainer capability matrix, including private movie/people archives and connected tools \| D05 Committed; product policy Committed (section 13.1) \| AC-050 through AC-052, AC-113, AC-119, AC-133, AC-134 |
| [FR-013 · L247](../../../02-PRD.md#61-functional-requirements) | Keep attributable change/security records and expose read-only Maintainer history for movie/person lifecycle, credits, artwork, and AI approval/creation \| D05/D20 and audit UI Committed \| AC-053 through AC-056, AC-114, AC-115, AC-127, AC-131 through AC-133, AC-137 |
| [A07 · L286](../../../02-PRD.md#63-project-requirements-coverage) | Add, update, delete movies \| FR-004 / AC-016 through AC-021, AC-108 through AC-110, AC-130 |
| [A09 · L288](../../../02-PRD.md#63-project-requirements-coverage) | List cast and creators \| FR-001/FR-005/FR-006 / AC-004, AC-022, AC-026, AC-134 |
| [A10 · L289](../../../02-PRD.md#63-project-requirements-coverage) | Add, update, delete cast and creators \| FR-005/FR-006 / AC-023 through AC-028, AC-131 through AC-133 |
| [A11 · L290](../../../02-PRD.md#63-project-requirements-coverage) | Search movies, cast, creators \| FR-002 / AC-006 through AC-010, AC-022, AC-134 |
| [D25 · L326](../../../02-PRD.md#64-project-decision-register) | Metadata, type/genres, stable identities, provenance \| FR-006/FR-007 / AC-026 through AC-032 |
| [AC-019 · L371](../../../02-PRD.md#us-003-maintain-movies) | Permanent deletion \| A movie is archived and a Maintainer sees its deletion consequences \| They separately confirm permanent deletion \| Remove it from the archive and catalog permanently, remove its credits, preserve shared people and other titles' artwork, and retain an attributable deletion record. No restore action remains. |
| [AC-019-C01 · L371](../../../02-PRD.md#us-003-maintain-movies) | Remove it from the archive and catalog permanently, remove its credits, preserve shared people and other titles' artwork, and retain an attributable deletion record. |
| [AC-019-C02 · L371](../../../02-PRD.md#us-003-maintain-movies) | No restore action remains. |
| [AC-020 · L372](../../../02-PRD.md#us-003-maintain-movies) | Changed/removed target \| Another action changed, archived, or deleted an edited movie \| A stale edit is submitted \| Do not overwrite or resurrect it; explain its current state and retain the permitted draft for review. |
| [AC-020-C01 · L372](../../../02-PRD.md#us-003-maintain-movies) | Do not overwrite or resurrect it; explain its current state and retain the permitted draft for review. |
| [AC-108 · L374](../../../02-PRD.md#us-003-maintain-movies) | Archive \| An active movie has artwork and credits \| A Maintainer archives it \| It disappears from public/Viewer discovery and direct access, appears in the private archive, and retains its information, artwork, and credits. Record the action. |
| [AC-108-C01 · L374](../../../02-PRD.md#us-003-maintain-movies) | It disappears from public/Viewer discovery and direct access, appears in the private archive, and retains its information, artwork, and credits. |
| [AC-108-C02 · L374](../../../02-PRD.md#us-003-maintain-movies) | Record the action. |
| [AC-109 · L375](../../../02-PRD.md#us-003-maintain-movies) | Restore movie \| An archived movie retains information; some credited people may be archived or permanently deleted \| A Maintainer restores the movie \| Return it to active discovery with remaining artwork. Show only credits whose people are active; retain archived-person credits privately without restoring the person, and do not recreate deleted people or removed credits. Record restoration. |
| [AC-109-C01 · L375](../../../02-PRD.md#us-003-maintain-movies) | Return it to active discovery with remaining artwork. |
| [AC-109-C02 · L375](../../../02-PRD.md#us-003-maintain-movies) | Show only credits whose people are active; retain archived-person credits privately without restoring the person, and do not recreate deleted people or removed credits. |
| [AC-109-C03 · L375](../../../02-PRD.md#us-003-maintain-movies) | Record restoration. |
| [AC-110 · L376](../../../02-PRD.md#us-003-maintain-movies) | Deletion controls / cancel \| A movie is active or archived \| A Maintainer considers permanent deletion or cancels confirmation \| Active movies offer archive, not direct permanent deletion. Archived movies offer restore and separately confirmed permanent deletion. Cancel changes nothing. |
| [AC-110-C01 · L376](../../../02-PRD.md#us-003-maintain-movies) | Active movies offer archive, not direct permanent deletion. |
| [AC-110-C02 · L376](../../../02-PRD.md#us-003-maintain-movies) | Archived movies offer restore and separately confirmed permanent deletion. |
| [AC-110-C03 · L376](../../../02-PRD.md#us-003-maintain-movies) | Cancel changes nothing. |
| [AC-130 · L377](../../../02-PRD.md#us-003-maintain-movies) | Competing or uncertain lifecycle action \| Archive, restore, or permanent deletion competes with another change, fails, or loses confirmation \| The Maintainer checks the result or repeats the action \| Show the current known state without unintended repeated effects; preserve the prior state on a confirmed failed change, require review of competing changes, and resolve uncertainty before claiming success or offering a repeat. |
| [AC-130-C01 · L377](../../../02-PRD.md#us-003-maintain-movies) | Show the current known state without unintended repeated effects; preserve the prior state on a confirmed failed change, require review of competing changes, and resolve uncertainty before claiming success or offering a repeat. |
| [AC-024 · L387](../../../02-PRD.md#us-004-maintain-people-and-credits) | Permanent person deletion \| A person is archived and a Maintainer reviews the current affected active movies, archived movies, and TV titles \| They separately confirm permanent deletion \| Remove the person and all remaining retained credits while preserving titles; retain an attributable deletion record and provide no restore. Cancellation removes nothing. Restoring a title cannot recreate the person or credits. |
| [AC-024-C01 · L387](../../../02-PRD.md#us-004-maintain-people-and-credits) | Remove the person and all remaining retained credits while preserving titles; retain an attributable deletion record and provide no restore. |
| [AC-024-C02 · L387](../../../02-PRD.md#us-004-maintain-people-and-credits) | Cancellation removes nothing. |
| [AC-024-C03 · L387](../../../02-PRD.md#us-004-maintain-people-and-credits) | Restoring a title cannot recreate the person or credits. |
| [AC-025 · L388](../../../02-PRD.md#us-004-maintain-people-and-credits) | Missing/denied/competing action \| The person disappeared, changed, was archived, or the actor lacks permission \| A write is attempted \| Explain the unavailable/denied/conflict result, preserve permitted draft work, and avoid duplicate or unintended edits. Archiving does not authorize a stale active-person save or recreation. |
| [AC-025-C01 · L388](../../../02-PRD.md#us-004-maintain-people-and-credits) | Explain the unavailable/denied/conflict result, preserve permitted draft work, and avoid duplicate or unintended edits. |
| [AC-025-C02 · L388](../../../02-PRD.md#us-004-maintain-people-and-credits) | Archiving does not authorize a stale active-person save or recreation. |
| [AC-129 · L389](../../../02-PRD.md#us-004-maintain-people-and-credits) | Deletion impact changes \| A Maintainer reviewed an archived person's affected titles and another change alters that impact \| They confirm the earlier permanent-deletion preview \| Require review and confirmation of the current affected titles before deleting; do not silently remove retained credits beyond the reviewed impact. |
| [AC-129-C01 · L389](../../../02-PRD.md#us-004-maintain-people-and-credits) | Require review and confirmation of the current affected titles before deleting; do not silently remove retained credits beyond the reviewed impact. |
| [AC-028 · L392](../../../02-PRD.md#us-004-maintain-people-and-credits) | Remove credit / changing references \| An active movie credit exists; its person may be archived or its references may change \| A Maintainer removes or updates the credit \| A valid permitted change is attributable without deleting the shared person. Removing a retained credit for an archived person is allowed on an active movie and is not reversed by person restoration. Changed/missing references require review and leave no dangling visible credit. |
| [AC-028-C01 · L392](../../../02-PRD.md#us-004-maintain-people-and-credits) | A valid permitted change is attributable without deleting the shared person. |
| [AC-028-C02 · L392](../../../02-PRD.md#us-004-maintain-people-and-credits) | Removing a retained credit for an archived person is allowed on an active movie and is not reversed by person restoration. |
| [AC-028-C03 · L392](../../../02-PRD.md#us-004-maintain-people-and-credits) | Changed/missing references require review and leave no dangling visible credit. |
| [AC-131 · L393](../../../02-PRD.md#us-004-maintain-people-and-credits) | Archive person \| An active person has credits on active/archived movies or seeded TV titles, or has no credits \| A Maintainer reviews affected titles, or sees that none are affected, and archives the person \| Hide the person and their credits publicly, retain information and relationships privately, and leave titles in their existing states. Show the person and retained credits in the Maintainer-only people archive and record the archive action. |
| [AC-131-C01 · L393](../../../02-PRD.md#us-004-maintain-people-and-credits) | Hide the person and their credits publicly, retain information and relationships privately, and leave titles in their existing states. |
| [AC-131-C02 · L393](../../../02-PRD.md#us-004-maintain-people-and-credits) | Show the person and retained credits in the Maintainer-only people archive and record the archive action. |
| [AC-132 · L394](../../../02-PRD.md#us-004-maintain-people-and-credits) | Restore person \| An archived person retains credits; some titles or credits may since have been deleted or removed \| A Maintainer restores the person \| Restore the same person identity and reveal remaining credits only on active titles. Leave archived titles archived; recreate no deleted title or removed credit. Make person editing/new-credit assignment available again and record restoration. |
| [AC-132-C01 · L394](../../../02-PRD.md#us-004-maintain-people-and-credits) | Restore the same person identity and reveal remaining credits only on active titles. |
| [AC-132-C02 · L394](../../../02-PRD.md#us-004-maintain-people-and-credits) | Leave archived titles archived; recreate no deleted title or removed credit. |
| [AC-132-C03 · L394](../../../02-PRD.md#us-004-maintain-people-and-credits) | Make person editing/new-credit assignment available again and record restoration. |
| [AC-133 · L395](../../../02-PRD.md#us-004-maintain-people-and-credits) | Archive-first controls / cancel \| A person is active or archived \| A Maintainer considers permanent deletion or cancels its separate confirmation \| Active people offer archive, never direct permanent deletion, including through direct requests. Archived people offer restore or separately confirmed permanent deletion with affected titles and irreversibility shown. Cancel preserves the person and credits; unauthorized lifecycle requests are denied. |
| [AC-133-C01 · L395](../../../02-PRD.md#us-004-maintain-people-and-credits) | Active people offer archive, never direct permanent deletion, including through direct requests. |
| [AC-133-C02 · L395](../../../02-PRD.md#us-004-maintain-people-and-credits) | Archived people offer restore or separately confirmed permanent deletion with affected titles and irreversibility shown. |
| [AC-133-C03 · L395](../../../02-PRD.md#us-004-maintain-people-and-credits) | Cancel preserves the person and credits; unauthorized lifecycle requests are denied. |
| [AC-134 · L396](../../../02-PRD.md#us-004-maintain-people-and-credits) | Public visibility and archive feedback \| A person is archived while their titles may remain active \| A Visitor/Viewer lists, searches, follows an old person link, or reads title credits; or a Maintainer opens the people archive \| Public views reveal no archived person or retained credit information and do not match titles solely through that hidden credit. Explain unavailable direct access without exposing private details. The restricted archive shows distinct loading, empty, available, denied, and failure-with-retry states. |
| [AC-134-C01 · L396](../../../02-PRD.md#us-004-maintain-people-and-credits) | Public views reveal no archived person or retained credit information and do not match titles solely through that hidden credit. |
| [AC-134-C02 · L396](../../../02-PRD.md#us-004-maintain-people-and-credits) | Explain unavailable direct access without exposing private details. |
| [AC-134-C03 · L396](../../../02-PRD.md#us-004-maintain-people-and-credits) | The restricted archive shows distinct loading, empty, available, denied, and failure-with-retry states. |
| [AC-135 · L397](../../../02-PRD.md#us-004-maintain-people-and-credits) | Independent title/person lifecycle \| Alex has a credit on active Movie A and Movie B; either Movie B or Alex may be archived \| A Maintainer archives/restores the movie or person in either order \| Archiving Movie B leaves Alex visible on Movie A. Archiving Alex hides Alex's credits on both; restoring Alex alone reveals Movie A's remaining credit while Movie B stays archived. Restoring Movie B alone leaves archived Alex hidden. Both must be active for the retained credit to be public. |
| [AC-135-C01 · L397](../../../02-PRD.md#us-004-maintain-people-and-credits) | Archiving Movie B leaves Alex visible on Movie A. |
| [AC-135-C02 · L397](../../../02-PRD.md#us-004-maintain-people-and-credits) | Archiving Alex hides Alex's credits on both; restoring Alex alone reveals Movie A's remaining credit while Movie B stays archived. |
| [AC-135-C03 · L397](../../../02-PRD.md#us-004-maintain-people-and-credits) | Restoring Movie B alone leaves archived Alex hidden. |
| [AC-135-C04 · L397](../../../02-PRD.md#us-004-maintain-people-and-credits) | Both must be active for the retained credit to be public. |
| [AC-136 · L398](../../../02-PRD.md#us-004-maintain-people-and-credits) | Archived-person edit and credit boundary \| A person is archived with a retained credit on an active movie \| A Maintainer edits the person, assigns a new credit, or removes that retained movie credit \| Require restoration before person editing or new assignment. Allow removal of the existing active-movie credit without deleting the person; person restoration does not recreate that credit. Offer no direct TV credit-editing capability. |
| [AC-136-C01 · L398](../../../02-PRD.md#us-004-maintain-people-and-credits) | Require restoration before person editing or new assignment. |
| [AC-136-C02 · L398](../../../02-PRD.md#us-004-maintain-people-and-credits) | Allow removal of the existing active-movie credit without deleting the person; person restoration does not recreate that credit. |
| [AC-136-C03 · L398](../../../02-PRD.md#us-004-maintain-people-and-credits) | Offer no direct TV credit-editing capability. |
| [AC-137 · L399](../../../02-PRD.md#us-004-maintain-people-and-credits) | Competing, interrupted, or uncertain person lifecycle \| Archive, restore, or permanent deletion competes with a change, loses confirmation, or is interrupted \| The Maintainer reviews or repeats the action, including after a restart \| Preserve prior state on a confirmed failed change; retain committed state and audit evidence across restart. Require review of conflicts/current deletion impact, reconcile uncertain outcomes, and avoid repeated effects or unconfirmed success. Restoration never recreates a permanently deleted person. |
| [AC-137-C01 · L399](../../../02-PRD.md#us-004-maintain-people-and-credits) | Preserve prior state on a confirmed failed change; retain committed state and audit evidence across restart. |
| [AC-137-C02 · L399](../../../02-PRD.md#us-004-maintain-people-and-credits) | Require review of conflicts/current deletion impact, reconcile uncertain outcomes, and avoid repeated effects or unconfirmed success. |
| [AC-137-C03 · L399](../../../02-PRD.md#us-004-maintain-people-and-credits) | Restoration never recreates a permanently deleted person. |
| [AC-113 · L443](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Private archive boundary \| An archived movie or person exists \| A Visitor/Viewer browses, searches, follows its link, or inspects credits \| Do not expose the archived record or its private credit relationships. A Maintainer can inspect it in the appropriate movie or people archive; person archiving does not hide the title itself. |
| [AC-113-C01 · L443](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | Do not expose the archived record or its private credit relationships. |
| [AC-113-C02 · L443](../../../02-PRD.md#us-008-register-sign-in-and-enforce-access) | A Maintainer can inspect it in the appropriate movie or people archive; person archiving does not hide the title itself. |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [G-05 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [OD-002 · L1019](../../../02-PRD.md#131-resolved-product-decisions) | Public discovery; Viewer and Maintainer; same permissions for all Maintainers; no role-management UI \| FR-011/FR-012; AC-049/AC-050/AC-113 |
| [OD-004 · L1020](../../../02-PRD.md#131-resolved-product-decisions) | Movies and people require archiving before separately confirmed permanent deletion. Person archiving retains credits privately; restoration reveals only surviving credits on active titles. Deletion preserves associated titles and movie deletion preserves shared people. \| FR-004/FR-005/FR-006; AC-019/AC-024/AC-108 through AC-110, AC-129 through AC-138 |
| [OD-009 · L1022](../../../02-PRD.md#131-resolved-product-decisions) | Reject stale writes, retain draft, require review; no force overwrite \| NFR-014; AC-020/AC-025/AC-104; concurrency mechanism transfers to techdoc |
| [US-004 · L379](../../../02-PRD.md#us-004-maintain-people-and-credits) | ### US-004: Maintain people and credits As a Maintainer, I want shared people, clear credit roles, and recoverable archiving so I can manage contributions across titles before deciding on permanent removal. Requirements: FR-005, FR-006, FR-013. Direct credit editing is for active movies. Shared-person lifecycle actions affect movie and TV credits as explained in the impact preview, without enabling direct TV management. |
| [PRD-PERM-04 · L71](../../../02-PRD.md#31-permission-matrix) | View movie/people archives; archive/restore/permanently delete movies and people \| No \| No \| Yes |
| [PRD-POLICY-07 · L128](../../../02-PRD.md#52-removal-and-editing-rules) | People follow the same lifecycle: Active → Archived → Restore or permanently delete. Before archiving a person, a Maintainer reviews affected active movies, archived movies, and TV-series credits. Archiving hides the person from public/Viewer people lists, search, direct details, and displayed credits across titles. It retains their information and credit relationships privately while leaving the titles themselves in their current states. Maintainers can inspect the archived person and retained credits in a Maintainer-only people archive; management views identify retained credits as belonging to an archived person. |
| [PRD-POLICY-08 · L130](../../../02-PRD.md#52-removal-and-editing-rules) | Restoring a person makes their remaining credits visible again on active titles. Public credit visibility requires both an active title and an active person. Restoring a movie does not restore an archived person; restoring a person does not restore an archived movie. Neither restoration recreates permanently deleted records or explicitly removed credits. For example, archiving Movie B leaves Alex visible on active Movie A; archiving Alex hides Alex's credits on both. Restoring Alex reveals the credit on Movie A while Movie B stays archived. |
| [PRD-POLICY-10 · L134](../../../02-PRD.md#52-removal-and-editing-rules) | Restore an archived person before editing their information or assigning new credits. A Maintainer may still remove a retained credit from an active movie; this is an edit to that movie's relationship, not deletion of the Person record. The removed credit does not return when the person is restored. Direct TV credit editing remains excluded. Shared people survive movie deletion, and assets used by surviving titles must remain available. |
| [PRD-CONCEPT-06 · L814](../../../02-PRD.md#9-conceptual-information-and-privacy) | Person / credit \| Shared catalog person and a title-specific Cast/Creator relationship with starter/custom role and applicable character names. Credits of archived people remain private; public visibility requires an active person and active title. Account identities are separate. |
| [PRD-CONCEPT-08 · L816](../../../02-PRD.md#9-conceptual-information-and-privacy) | Person lifecycle \| Active, archived, or permanently deleted. Archive preserves person information and retained credits privately. Restore reveals surviving credits on active titles; permanent deletion removes the person and retained relationships while preserving titles. |
| [DS-POLICY-12 · L84](../../../03-design-system.md#artwork-icons-and-motion) | Icons are simple local SVG/line shapes. Use labeled controls and avoid ambiguous icon-only deletion. Motion is brief feedback, 140ms by default; no autoplay cinema effects. `prefers-reduced-motion: reduce` disables smooth scrolling and transitions. Loading uses stable placeholders and text rather than motion as the only signal. |
| [DS-POLICY-19 · L100](../../../03-design-system.md#visible-interaction-contracts) | Movies and people archive before permanent deletion. Public views disclose no archived data or retained credits. Restore first to edit. Person impact includes active movies, archived movies, and TV. Permanent deletion has a current impact preview followed by a separate irreversible confirmation; changed impact invalidates earlier confirmation. Restoring one side does not restore the other side or recreate deleted/removed relationships. Remove a retained person credit only from an active movie; never direct-edit TV credits. |
| [V-LIFE · L6388](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-LIFE \| Archive/restore/delete both linked records under concurrency, stale impact token and cancellation; assert remaining public/private credits, latest impact review, tombstones, old artwork protection and no deleted relationship resurrection |
| [TD-ROW-L3565 · L3565](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | asset_hold \| asset_reference; columns and relationships retained \| Multiple protected references, fenced release, no time-only deletion of unresolved reservations |
| [TD-ROW-L3590 · L3590](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `crypto_key_version`, `account_email_lookup` \| Core `crypto/persistence` \| Key/nonce lifecycle differs from account lifecycle; rotation needs multiple lookup aliases |
| [TD-ROW-L3598 · L3598](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `provenance_record` \| Catalog `provenance/persistence` \| Source-scoped identity and reuse evidence survive record deletion |
| [TD-ROW-L3625 · L3625](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `provenance_record` \| Existing Provenance \| Source evidence only; omit internal deletion/reconciliation fields |
| [TD-ROW-L5182 · L5182](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Archive/restore/delete \| Same boundary; enforce current state and title type. Delete person only against current impact token \| READ COMMITTED + catalog lock; no cascading cross-service action |
| [TD-ROW-L5511 · L5511](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-09 visibility \| Archived/private metadata or PII leaks \| Both-endpoint active predicates, own-draft checks, no direct asset URL, cache rules \| Guess IDs, old cursor/asset URL, account switch, private credits; local operator retains DB power; Catalog/Core |
| [TD-BLOCK-L3776 · L3776](../../../04-spoolin-techdoc.md#durable_work-ddl) | durable_work DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L3884 · L3884](../../../04-spoolin-techdoc.md#operational_event-ddl) | operational_event DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4117 · L4117](../../../04-spoolin-techdoc.md#user_account-ddl) | user_account DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4162 · L4162](../../../04-spoolin-techdoc.md#account_email_lookup-ddl) | account_email_lookup DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4210 · L4210](../../../04-spoolin-techdoc.md#browser_session-ddl) | browser_session DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4313 · L4313](../../../04-spoolin-techdoc.md#form_draft-ddl) | form_draft DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4428 · L4428](../../../04-spoolin-techdoc.md#title-ddl) | title DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4504 · L4504](../../../04-spoolin-techdoc.md#person-ddl) | person DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4586 · L4586](../../../04-spoolin-techdoc.md#title_genre-ddl) | title_genre DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4674 · L4674](../../../04-spoolin-techdoc.md#credit-ddl) | credit DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L4728 · L4728](../../../04-spoolin-techdoc.md#provenance_record-ddl) | provenance_record DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L5111 · L5111](../../../04-spoolin-techdoc.md#asset_reference-ddl) | asset_reference DDL — complete sql contract, all members, constraints and field numbers |
| [BRAIN-A07 · L52](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A07 \| Add, delete, and update movies \| Working operations with validation and understandable outcomes |
| [BRAIN-A09 · L54](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A09 \| List cast and creators \| Accessible people/credit views |
| [BRAIN-A10 · L55](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A10 \| Add, delete, and update cast and creators \| Working person management with defined relationship behavior |
| [BRAIN-A11 · L56](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A11 \| Search movies, cast, and creators \| Defined searchable fields and matching behavior, including no-results/error states |
| [BRAIN-D25 · L94](../../../01-brainstorming-result.md#4-decision-register) | D25 \| Committed \| Categorize movies and TV series by title type and genres, include synopses and useful descriptive metadata, and retain stable identities and source provenance to support future review aggregation; detailed baseline in section 8.2 |

## Implementation

1. Implement separate movie/people archive contexts with retained private details/credits, current selection, loading/empty/denied/failure and safe retry.
2. Show current target and all deletion impact categories before a separate irreversible confirmation. Changed impact replaces the preview and invalidates old confirmation; Cancel is initially focused and changes nothing.
3. Render archive/restore/delete pending/confirmed/failed/conflict/uncertain outcomes; reconcile original operation and current target before repeat. Restoring first is required for editing.
4. Invalidate public/managed view cursors only on confirmed changes and verify old direct links disclose no archive data.
5. Run independent title/person lifecycle permutations, retained-credit removals and real deletion/asset preservation across services; capture all archive states and separate keyboard/dialog proof.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T025-A1:** Privacy: Visitor/Viewer cannot read archives through UI, old URL, direct API or related credits; Maintainer can inspect retained content.
- **T025-A2:** Confirmation: stale impact requires fresh review, cancel preserves all data, and permanent delete leaves no restore action.
- **T025-A3:** Permutations: archive/restore either side shows only surviving both-active credits, and stale receipt replay never recreates a deleted record. T033 owns the separate seed-rerun preservation proof (ATDD-112/198).

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-019](../../../05-atdd.md#case-atdd-019-permanent-deletion) / End-to-end | M-A archived with P-A credits and OLD artwork also held by M-B; confirm irreversible targetM-A, then query old UUID/public/archive and audit. | Then Remove it from the archive and catalog permanently, remove its credits, preserve shared people and other titles' artwork, and retain an attributable deletion record. No restore action remains. Observation: Inspect archived-before-delete confirmation target, permanent absence of M-A/owned links, retained identity/audit/tombstones and surviving shared OLD hold for M-B. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-020](../../../05-atdd.md#case-atdd-020-changedremoved-target) / End-to-end | Open M-A version 5; other session updates→6, archives, or deletes before stale save. Each row independent; draft"Retained proposal". | Then Do not overwrite or resurrect it; explain its current state and retain the permitted draft for review. Observation: Compare stale version/current lifecycle with returned conflict/not-found, unchanged winner state and retained proposal draft; no stale overwrite or invented success. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-024](../../../05-atdd.md#case-atdd-024-permanent-person-deletion) / End-to-end | Archived P-A credits on activeM-A/archivedM-B/TV-C; previewthenCancel, and previewthenConfirm in independent installs. | Then Remove the person and all remaining retained credits while preserving titles; retain an attributable deletion record and provide no restore. Cancellation removes nothing. Restoring a title cannot recreate the person or credits. Observation: Inspect current full person-deletion preview and confirm/cancel result; after confirmed delete credits disappear but affected active/archived/TV titles remain. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-025](../../../05-atdd.md#case-atdd-025-missingdeniedcompeting-action) / End-to-end | P-A version 5; other session modifies/archives/deletes it; Viewer or expiredMaintainer attempts write; preserve safe draft. | Then Explain the unavailable/denied/conflict result, preserve permitted draft work, and avoid duplicate or unintended edits. Archiving does not authorize a stale active-person save or recreation. Observation: Record missing/archived/stale/denied person response and safe draft; inspect unchanged winning record/relationships and no unauthorized successful event. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-028](../../../05-atdd.md#case-atdd-028-remove-credit--changing-references) / End-to-end | Remove existing retained(M-A, P-B) while P-B archived; restore P-B and check absence. Other row changes/removes referenced endpoint before credit update. | Then A valid permitted change is attributable without deleting the shared person. Removing a retained credit for an archived person is allowed on an active movie and is not reversed by person restoration. Changed/missing references require review and leave no dangling visible credit. Observation: Inspect removal receipt and absence of the archived-person credit after restore; changed references/stale versions must not remove an unintended relationship. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-082](../../../05-atdd.md#case-atdd-082-archive) / End-to-end | ArchiveM-A active withOLDandcredits; query public title/list/search/credit and private archive plusaudit. | Then It disappears from public/Viewer discovery and direct access, appears in the private archive, and retains its information, artwork, and credits. Record the action. Observation: Inspect active→archived movie state, private archive access and retained credits/artwork with public absence; same-name active records remain distinct. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-083](../../../05-atdd.md#case-atdd-083-restore-movie) / End-to-end | RestoreM-A archived with P-B archived, P-D deleted and seed credit removed; only active person surviving credits become public. | Then Return it to active discovery with remaining artwork. Show only credits whose people are active; retain archived-person credits privately without restoring the person, and do not recreate deleted people or removed credits. Record restoration. Observation: Restore archived movie and reread active public values/surviving active-person credits/artwork; archived people and removed credits are not resurrected. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-084](../../../05-atdd.md#case-atdd-084-deletion-controls--cancel) / End-to-end | ActiveM-A directDELETE rejects; archivedM-A review separate irreversible confirmation; Escape/Cancel has no effect. | Then Active movies offer archive, not direct permanent deletion. Archived movies offer restore and separately confirmed permanent deletion. Cancel changes nothing. Observation: Observe archive-first irreversible controls, current confirmation target and Cancel/default-focus behavior; cancellation leaves all records and artwork unchanged. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-087](../../../05-atdd.md#case-atdd-087-private-archive-boundary) / End-to-end | Oldmovie/personURLs, directAPIcredits, cursors andassetURLs withVisitor/V; archivedprivateonlytoMaintainer, titles not implicitly hidden by archived person. | Then Do not expose the archived record or its private credit relationships. A Maintainer can inspect it in the appropriate movie or people archive; person archiving does not hide the title itself. Observation: Request private archives directly asVisitor/Viewer and read asMaintainer; inspect denied body/no leaks and source-selected archive visibility. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-103](../../../05-atdd.md#case-atdd-103-deletion-impact-changes) / End-to-end | ArchivedP-A preview three credits; anothertransactionaddscreditonM-D orrenames/archives linked title; oldtoken rejectedIMPACT_CHANGED; freshfour-title confirmation. | Then Require review and confirmation of the current affected titles before deleting; do not silently remove retained credits beyond the reviewed impact. Observation: Change every relevant person/credit/title consequence after preview and inspect refreshed impact/IMPACT_CHANGED with no deletion until new confirmation. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-104](../../../05-atdd.md#case-atdd-104-competing-or-uncertain-lifecycle-action) / End-to-end | Independentmoviearchive/restore/delete: competing version, fail before commit, lost reply after commit, repeat and restart; original receipt/current state match. | Then Show the current known state without unintended repeated effects; preserve the prior state on a confirmed failed change, require review of competing changes, and resolve uncertainty before claiming success or offering a repeat. Observation: Race movie lifecycle and lose reply after commit; inspect winner state/current version/original receipt and preserved relationships/artwork rather than stale success. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-105](../../../05-atdd.md#case-atdd-105-archive-person) / End-to-end | P-A withactiveM-A, archivedM-B, TV-C and row with no credits; preview/archive keeps title states, private credits andaudit. | Then Hide the person and their credits publicly, retain information and relationships privately, and leave titles in their existing states. Show the person and retained credits in the Maintainer-only people archive and record the archive action. Observation: Archive shared person and inspect private retained profile/credits with public absence on active movie/TV; title states remain unchanged. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-106](../../../05-atdd.md#case-atdd-106-restore-person) / End-to-end | RestoreP-A; M-B remains archive and removedC-1/deletedM-D absent; same person identity editable again. | Then Restore the same person identity and reveal remaining credits only on active titles. Leave archived titles archived; recreate no deleted title or removed credit. Make person editing/new-credit assignment available again and record restoration. Observation: Restore person and inspect only surviving credits on active titles; archived titles and removed/permanently deleted relationships remain absent. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-107](../../../05-atdd.md#case-atdd-107-archive-first-controls--cancel) / End-to-end | Activepersondirectdelete/archived person edit denied; private preview/current confirm; cancel unchanged; Viewer lifecycle denied. | Then Active people offer archive, never direct permanent deletion, including through direct requests. Archived people offer restore or separately confirmed permanent deletion with affected titles and irreversibility shown. Cancel preserves the person and credits; unauthorized lifecycle requests are denied. Observation: Inspect person archive-first delete/confirm/cancel controls and actual unaffected title/credit state after cancel; direct bypass cannot delete an active person. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-108](../../../05-atdd.md#case-atdd-108-public-visibility-and-archive-feedback) / End-to-end | ArchiveP-A; public name/search/oldURL/credit no hiddeninfo; M-A still visible; archive loading/empty/available/failure/denied independent. | Then Public views reveal no archived person or retained credit information and do not match titles solely through that hidden credit. Explain unavailable direct access without exposing private details. The restricted archive shows distinct loading, empty, available, denied, and failure-with-retry states. Observation: Inspect public person/credit visibility and private archive loading/empty/error/denial after each lifecycle transition, using actual current owner predicates. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-109](../../../05-atdd.md#case-atdd-109-independent-titleperson-lifecycle) / End-to-end | F-LIFECYCLE all four title/person state combinations in both archive/restore orders; retained credit visible iff bothACTIVE. | Then Archiving Movie B leaves Alex visible on Movie A. Archiving Alex hides Alex's credits on both; restoring Alex alone reveals Movie A's remaining credit while Movie B stays archived. Restoring Movie B alone leaves archived Alex hidden. Both must be active for the retained credit to be public. Observation: Apply independent movie/person archive/restore combinations and compare per-credit visibility; restoring one endpoint does not restore the other. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-110](../../../05-atdd.md#case-atdd-110-archived-person-edit-and-credit-boundary) / End-to-end | ArchivedP-B edit/newcredit/updatecredit denied; removeexistingM-A credit permitted; restore does not recreate; TVdirectremove denied. | Then Require restoration before person editing or new assignment. Allow removal of the existing active-movie credit without deleting the person; person restoration does not recreate that credit. Offer no direct TV credit-editing capability. Observation: Attempt archived-person edit/add/change credit and permitted removal from an active movie; inspect safe denials and exact surviving/removed relationships. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-111](../../../05-atdd.md#case-atdd-111-competing-interrupted-or-uncertain-person-lifecycle) / End-to-end | Personarchive/restore/delete before commit failure/after commit lost reply/competing impact/forced restart; current state/audit preserved, no resurrection. | Then Preserve prior state on a confirmed failed change; retain committed state and audit evidence across restart. Require review of conflicts/current deletion impact, reconcile uncertain outcomes, and avoid repeated effects or unconfirmed success. Restoration never recreates a permanently deleted person. Observation: Synchronize stale/revoked/missing/uncertain person lifecycle actions; inspect winner/original receipt and safe recovery without unconfirmed deletion or resurrection. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-264](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) / End-to-end | F-UI; each state row uses F-CATALOG/F-ACTORS and its source-defined stage. Replace local fixture IDs with deterministic actual IDs; no simulated save/upload/client result counts as execution evidence. | Then the visible heading, identity context, feedback and action match the row oracle And each outcome actually applicable to this flow remains distinct from its pending, failed or unavailable alternatives And theme, width and keyboard changes preserve state and show no restricted data or unsupported feature Observation: Exercise `managedTitles`, `managedPeople`, `deletionImpact`, `movieLifecycle`, `personLifecycle`, `operation` in `/manage/archives`, capturing the current response/operation and each row’s visible heading, identity, controls, retained values and safe next action. Assert the named ArchiveTabs / ImpactDialog through actual visible layout and accessible names/status rather than hidden fixture text. Run each row independently in both themes at declared widths; verify keyboard/focus and permitted navigation without leaking protected data. Source fragments identify design references, not implementation selectors. \| State reference \| Actor / independent trigger \| Visible semantic oracle \| \| --- \| --- \| --- \| \| movie-archive \| Maintainer; complete archived `managedTitles` \| Show private archived movies with Restore and separate Review permanent deletion; no active edit. Retain metadata/remaining relationships privately without promising resurrection of deleted data. \| \| movie-impact \| Maintainer; choose archive for active M-A \| Show identity and complete retained artwork/credit consequences: public hiding, private retention, shared people preserved. Offer Confirm archive and Cancel; Cancel changes nothing. \| \| movie-restore \| Maintainer; receive confirmed successful movie restore \| Show restored identity and active-record navigation; display only remaining active-person credits. Archived/deleted people and explicitly removed credits are not recreated. \| \| movie-delete \| Maintainer; review permanent deletion of archived M-A \| Show identity, irreversibility and removal of movie/remaining credits while shared people/other-title artwork survive. Separate confirmation and Cancel are visible; active movies have no direct delete. \| \| person-archive \| Maintainer; complete archived `managedPeople` \| Show private people archive with retained relationships, Restore and separate deletion review. Editing/new assignment require restore; public accounts cannot see this list. \| \| person-impact \| Maintainer; review archiving active P-A with credits \| Show all affected active movies, archived movies and TV, or explicit zero impact. Explain public hiding/private credit retention and unchanged title states; Confirm archive and Cancel remain distinct. \| \| person-restore \| Maintainer; receive confirmed successful person restore \| Show original identity active again; remaining credits on active titles return, archived titles stay private and removed/deleted relationships remain absent. Offer permitted editing/navigation. \| \| person-delete \| Maintainer; receive complete current `deletionImpact` for archived P-A \| Show exact count and full affected active/archived movie/TV consequences, irreversibility and surviving titles. Require separate current confirmation; oversized/nonrepresentable previews show safe exhaustion and grant no delete authority. \| \| impact-changed \| Maintainer; mutate a consequence after preview then submit old token \| Show previous confirmation is no longer current, the changed complete impact and review action. Delete stays unconfirmed until fresh preview and explicit confirmation; no silent expansion occurs. \| \| deleted \| Maintainer; reconcile confirmed permanent deletion \| Show deletion confirmed, no restore/edit action for removed identity, surviving records and history navigation. Do not substitute a similarly named record or claim deleted relationships can return. \| \| archive-loading \| Maintainer candidate; hold access check/list response \| Show Loading private archive; conceal private records until authorization/list resolves. Do not display cached private rows or empty-state conclusions. \| \| archive-empty \| Maintainer; receive successful empty private archive list \| Show No archived records as a successful response; preserve selected archive tab and distinguish read outage or denial. \| \| archive-failed \| Maintainer; fail a lifecycle action before commit \| Show Archive action failed with known prior state preserved and review/fresh-attempt guidance. Independently fail archive-list read: retain tab, show load failure/retry, never infer empty archive or failed mutation from it. \| \| archive-uncertain \| Maintainer; lose archive/restore/delete response \| Show Lifecycle result not confirmed and query original operation before repeat. Restart cannot imply rollback; expose current committed state only after reconciliation. \| \| archive-denied \| Visitor/Viewer; request private archive \| Show Archive access denied with safe public navigation; display no private records or retained relationships and no lifecycle controls. \| \| impact-current \| Maintainer; refresh impact after adding another retained credit/title \| Show complete current affected set and exact count with title states/credit consequences. Only the new current preview can authorize separate confirmation; sample four-title text is replaced by real count. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-264-T1-R01 · L11022](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | movie-archive; Maintainer; complete archived `managedTitles`; Show private archived movies with Restore and separate Review permanent deletion; no active edit. Retain metadata/remaining relationships privately without promising resurrection of deleted data. |
| [ATDD-264-T1-R02 · L11023](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | movie-impact; Maintainer; choose archive for active M-A; Show identity and complete retained artwork/credit consequences: public hiding, private retention, shared people preserved. Offer Confirm archive and Cancel; Cancel changes nothing. |
| [ATDD-264-T1-R03 · L11024](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | movie-restore; Maintainer; receive confirmed successful movie restore; Show restored identity and active-record navigation; display only remaining active-person credits. Archived/deleted people and explicitly removed credits are not recreated. |
| [ATDD-264-T1-R04 · L11025](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | movie-delete; Maintainer; review permanent deletion of archived M-A; Show identity, irreversibility and removal of movie/remaining credits while shared people/other-title artwork survive. Separate confirmation and Cancel are visible; active movies have no direct delete. |
| [ATDD-264-T1-R05 · L11026](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | person-archive; Maintainer; complete archived `managedPeople`; Show private people archive with retained relationships, Restore and separate deletion review. Editing/new assignment require restore; public accounts cannot see this list. |
| [ATDD-264-T1-R06 · L11027](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | person-impact; Maintainer; review archiving active P-A with credits; Show all affected active movies, archived movies and TV, or explicit zero impact. Explain public hiding/private credit retention and unchanged title states; Confirm archive and Cancel remain distinct. |
| [ATDD-264-T1-R07 · L11028](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | person-restore; Maintainer; receive confirmed successful person restore; Show original identity active again; remaining credits on active titles return, archived titles stay private and removed/deleted relationships remain absent. Offer permitted editing/navigation. |
| [ATDD-264-T1-R08 · L11029](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | person-delete; Maintainer; receive complete current `deletionImpact` for archived P-A; Show exact count and full affected active/archived movie/TV consequences, irreversibility and surviving titles. Require separate current confirmation; oversized/nonrepresentable previews show safe exhaustion and grant no delete authority. |
| [ATDD-264-T1-R09 · L11030](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | impact-changed; Maintainer; mutate a consequence after preview then submit old token; Show previous confirmation is no longer current, the changed complete impact and review action. Delete stays unconfirmed until fresh preview and explicit confirmation; no silent expansion occurs. |
| [ATDD-264-T1-R10 · L11031](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | deleted; Maintainer; reconcile confirmed permanent deletion; Show deletion confirmed, no restore/edit action for removed identity, surviving records and history navigation. Do not substitute a similarly named record or claim deleted relationships can return. |
| [ATDD-264-T1-R11 · L11032](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | archive-loading; Maintainer candidate; hold access check/list response; Show Loading private archive; conceal private records until authorization/list resolves. Do not display cached private rows or empty-state conclusions. |
| [ATDD-264-T1-R12 · L11033](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | archive-empty; Maintainer; receive successful empty private archive list; Show No archived records as a successful response; preserve selected archive tab and distinguish read outage or denial. |
| [ATDD-264-T1-R13 · L11034](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | archive-failed; Maintainer; fail a lifecycle action before commit; Show Archive action failed with known prior state preserved and review/fresh-attempt guidance. Independently fail archive-list read: retain tab, show load failure/retry, never infer empty archive or failed mutation from it. |
| [ATDD-264-T1-R14 · L11035](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | archive-uncertain; Maintainer; lose archive/restore/delete response; Show Lifecycle result not confirmed and query original operation before repeat. Restart cannot imply rollback; expose current committed state only after reconciliation. |
| [ATDD-264-T1-R15 · L11036](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | archive-denied; Visitor/Viewer; request private archive; Show Archive access denied with safe public navigation; display no private records or retained relationships and no lifecycle controls. |
| [ATDD-264-T1-R16 · L11037](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | impact-current; Maintainer; refresh impact after adding another retained credit/title; Show complete current affected set and exact count with title states/credit consequences. Only the new current preview can authorize separate confirmation; sample four-title text is replaced by real count. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T025` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T025` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| visual-review | Inspect relevant changed states against the adopted references; retain exhaustive matrix coverage with T044 unless required by this feature | `/Users/mekari/Developer/personal-project/movie-app` | Required visual differences resolved at current revision; no overflow/missing identity | yes |
| interaction-accessibility | Exercise keyboard/focus/labels/errors/live regions/dialogs, zoom/reduced motion and source browser/host matrix independently of pixels | `/Users/mekari/Developer/personal-project/movie-app` | Meaningful accessible interaction and safe recovery, with unavailable contexts kept unverified | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Use the linked adopted references and actual feature for relevant changed states. Browser assertions and direct inspection establish ordinary behavior; retain screenshots when visual inspection needs them. Record actual fixtures, environment and outcomes. Exhaustive theme/width/browser matrices below remain release coverage references owned by T044 unless the selected behavior needs that exact observation. Do not delete their case IDs or claim deferred proof passed.

Check changed keyboard/focus/labels/errors/announcements and relevant zoom, motion, touch and browser behavior. Preserve known regressions and actual reader observations required to prove speech; exhaustive compatibility sweeps remain release-owned. For AI states include actual Claude/ChatGPT host context at T038/T039; standalone resource evidence cannot prove host isolation. Existing mockups are adopted references, not automatic pixel baselines or production test passes.

| Flow/state reference | Actor and fixture condition | Trigger and expected comparison |
|---|---|---|
| [archives / movie-archive](../../../design-system/mockup/archives.html#movie-archive) · [ATDD-264-T1-R01 · L11022](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; complete archived `managedTitles`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show private archived movies with Restore and separate Review permanent deletion; no active edit. Retain metadata/remaining relationships privately without promising resurrection of deleted data. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / movie-impact](../../../design-system/mockup/archives.html#movie-impact) · [ATDD-264-T1-R02 · L11023](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; choose archive for active M-A; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show identity and complete retained artwork/credit consequences: public hiding, private retention, shared people preserved. Offer Confirm archive and Cancel; Cancel changes nothing. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / movie-restore](../../../design-system/mockup/archives.html#movie-restore) · [ATDD-264-T1-R03 · L11024](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; receive confirmed successful movie restore; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show restored identity and active-record navigation; display only remaining active-person credits. Archived/deleted people and explicitly removed credits are not recreated. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / movie-delete](../../../design-system/mockup/archives.html#movie-delete) · [ATDD-264-T1-R04 · L11025](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; review permanent deletion of archived M-A; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show identity, irreversibility and removal of movie/remaining credits while shared people/other-title artwork survive. Separate confirmation and Cancel are visible; active movies have no direct delete. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / person-archive](../../../design-system/mockup/archives.html#person-archive) · [ATDD-264-T1-R05 · L11026](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; complete archived `managedPeople`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show private people archive with retained relationships, Restore and separate deletion review. Editing/new assignment require restore; public accounts cannot see this list. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / person-impact](../../../design-system/mockup/archives.html#person-impact) · [ATDD-264-T1-R06 · L11027](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; review archiving active P-A with credits; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show all affected active movies, archived movies and TV, or explicit zero impact. Explain public hiding/private credit retention and unchanged title states; Confirm archive and Cancel remain distinct. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / person-restore](../../../design-system/mockup/archives.html#person-restore) · [ATDD-264-T1-R07 · L11028](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; receive confirmed successful person restore; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show original identity active again; remaining credits on active titles return, archived titles stay private and removed/deleted relationships remain absent. Offer permitted editing/navigation. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / person-delete](../../../design-system/mockup/archives.html#person-delete) · [ATDD-264-T1-R08 · L11029](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; receive complete current `deletionImpact` for archived P-A; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show exact count and full affected active/archived movie/TV consequences, irreversibility and surviving titles. Require separate current confirmation; oversized/nonrepresentable previews show safe exhaustion and grant no delete authority. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / impact-changed](../../../design-system/mockup/archives.html#impact-changed) · [ATDD-264-T1-R09 · L11030](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; mutate a consequence after preview then submit old token; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show previous confirmation is no longer current, the changed complete impact and review action. Delete stays unconfirmed until fresh preview and explicit confirmation; no silent expansion occurs. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / deleted](../../../design-system/mockup/archives.html#deleted) · [ATDD-264-T1-R10 · L11031](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; reconcile confirmed permanent deletion; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show deletion confirmed, no restore/edit action for removed identity, surviving records and history navigation. Do not substitute a similarly named record or claim deleted relationships can return. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / archive-loading](../../../design-system/mockup/archives.html#archive-loading) · [ATDD-264-T1-R11 · L11032](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer candidate; hold access check/list response; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Loading private archive; conceal private records until authorization/list resolves. Do not display cached private rows or empty-state conclusions. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / archive-empty](../../../design-system/mockup/archives.html#archive-empty) · [ATDD-264-T1-R12 · L11033](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; receive successful empty private archive list; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show No archived records as a successful response; preserve selected archive tab and distinguish read outage or denial. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / archive-failed](../../../design-system/mockup/archives.html#archive-failed) · [ATDD-264-T1-R13 · L11034](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; fail a lifecycle action before commit; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Archive action failed with known prior state preserved and review/fresh-attempt guidance. Independently fail archive-list read: retain tab, show load failure/retry, never infer empty archive or failed mutation from it. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / archive-uncertain](../../../design-system/mockup/archives.html#archive-uncertain) · [ATDD-264-T1-R14 · L11035](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; lose archive/restore/delete response; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Lifecycle result not confirmed and query original operation before repeat. Restart cannot imply rollback; expose current committed state only after reconciliation. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / archive-denied](../../../design-system/mockup/archives.html#archive-denied) · [ATDD-264-T1-R15 · L11036](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Visitor/Viewer; request private archive; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Archive access denied with safe public navigation; display no private records or retained relationships and no lifecycle controls. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [archives / impact-current](../../../design-system/mockup/archives.html#impact-current) · [ATDD-264-T1-R16 · L11037](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback) | Maintainer; refresh impact after adding another retained credit/title; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show complete current affected set and exact count with title states/credit consequences. Only the new current preview can authorize separate confirmation; sample four-title text is replaced by real count. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

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


## Implementation delivery (17 October 2026)

Implementation for this task is on branch `codex/g04-g16-delivery` ([PR #8](https://github.com/okfriansyah-moh/spoolIn/pull/8)). Local task and browser runners were **not** executed on the authoring machine (host resource limit). Use CI or a better-resourced environment to run `ops/bin/test-unit.sh` / `ops/bin/test-integration.sh` for this task; set `SPOOLIN_TASK_BROWSER=1` for lighter Playwright when running browser integrations. Formal acceptance, combined review, and release proof remain with **G26**.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
