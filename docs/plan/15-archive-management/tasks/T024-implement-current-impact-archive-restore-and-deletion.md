---
kind: task
id: T024
group: G15
depends_on:
- T009
- T007
- T012
- T016
- T018
- T020
- T023
readiness: Ready
status: In progress
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T024 — Implement current-impact archive, restore and deletion

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-019 | implements |
| ATDD-020 | implements |
| ATDD-024 | implements |
| ATDD-025 | implements |
| ATDD-028 | implements |
| ATDD-082 | implements |
| ATDD-083 | implements |
| ATDD-084 | implements |
| ATDD-103 | implements |
| ATDD-104 | implements |
| ATDD-105 | implements |
| ATDD-106 | implements |
| ATDD-107 | implements |
| ATDD-108 | implements |
| ATDD-109 | implements |
| ATDD-110 | implements |
| ATDD-111 | implements |
| ATDD-087 | implements |
| ATDD-149 | both |
| ATDD-150 | both |
| ATDD-251-T1-R16 | implements |
| ATDD-251-T1-R25 | implements |
| ATDD-251-T1-R28 | implements |
| ATDD-253-T1-R20 | implements |
| ATDD-253-T1-R21 | implements |
| ATDD-253-T1-R22 | implements |
| ATDD-264 | implements |
| ATDD-311 | both |
| J-03 | both |
| J-09 | both |
| FR-001 | implements |
| FR-002 | implements |
| FR-004 | implements |
| FR-005 | both |
| FR-006 | implements |
| FR-012 | implements |
| FR-013 | implements |
| A07 | implements |
| A09 | implements |
| A10 | both |
| A11 | implements |
| D25 | implements |
| AC-019 | implements |
| AC-019-C01 | implements |
| AC-019-C02 | implements |
| AC-020 | implements |
| AC-020-C01 | implements |
| AC-108 | implements |
| AC-108-C01 | implements |
| AC-108-C02 | implements |
| AC-109 | implements |
| AC-109-C01 | implements |
| AC-109-C02 | implements |
| AC-109-C03 | implements |
| AC-110 | implements |
| AC-110-C01 | implements |
| AC-110-C02 | implements |
| AC-110-C03 | implements |
| AC-130 | implements |
| AC-130-C01 | implements |
| AC-024 | both |
| AC-024-C01 | both |
| AC-024-C02 | both |
| AC-024-C03 | both |
| AC-025 | implements |
| AC-025-C01 | implements |
| AC-025-C02 | implements |
| AC-129 | both |
| AC-129-C01 | both |
| AC-028 | implements |
| AC-028-C01 | implements |
| AC-028-C02 | implements |
| AC-028-C03 | implements |
| AC-131 | implements |
| AC-131-C01 | implements |
| AC-131-C02 | implements |
| AC-132 | implements |
| AC-132-C01 | implements |
| AC-132-C02 | implements |
| AC-132-C03 | implements |
| AC-133 | implements |
| AC-133-C01 | implements |
| AC-133-C02 | implements |
| AC-133-C03 | implements |
| AC-134 | implements |
| AC-134-C01 | implements |
| AC-134-C02 | implements |
| AC-134-C03 | implements |
| AC-135 | implements |
| AC-135-C01 | implements |
| AC-135-C02 | implements |
| AC-135-C03 | implements |
| AC-135-C04 | implements |
| AC-136 | implements |
| AC-136-C01 | implements |
| AC-136-C02 | implements |
| AC-136-C03 | implements |
| AC-137 | implements |
| AC-137-C01 | implements |
| AC-137-C02 | implements |
| AC-137-C03 | implements |
| AC-113 | implements |
| AC-113-C01 | implements |
| AC-113-C02 | implements |
| G-03 | both |
| G-05 | implements |
| OD-002 | implements |
| OD-004 | both |
| OD-009 | implements |
| US-004 | implements |
| PRD-PERM-04 | implements |
| PRD-POLICY-07 | implements |
| PRD-POLICY-08 | implements |
| PRD-POLICY-10 | implements |
| PRD-CONCEPT-06 | implements |
| PRD-CONCEPT-08 | implements |
| TD-C-037 | both |
| TD-C-038 | both |
| ADR-03 | both |
| TD-ROW-L2739 | both |
| TD-ROW-L2748 | both |
| TD-ROW-L2751 | both |
| BRAIN-A10 | both |

## Prerequisites

- [T009 — Enforce scoped service identity and shared admission](../../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T007 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T012 — Serve precise public catalog and people reads](../../10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year.
- [T016 — Implement authoritative movie creation and edits](../../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none.
- [T018 — Implement shared person writes and identity](../../13-people-and-credits/tasks/T018-implement-shared-person-writes-and-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact.
- [T020 — Implement ordered credits and custom contribution roles](../../13-people-and-credits/tasks/T020-implement-ordered-credits-and-custom-contribution-roles.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit.
- [T023 — Coordinate protective holds, attachment and cleanup](../../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/lifecycle/ | T024 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/catalogview/lifecycle/ | T024 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/lifecycle/ | T024 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/lifecycle/ | T024 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/catalogview/lifecycle/ | T024 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [5.3 Operation, permission and deadline inventory](../../../04-spoolin-techdoc.md#53-operation-permission-and-deadline-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.1 Transaction inventory](../../../04-spoolin-techdoc.md#71-transaction-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.5 Additional consequential native operations](../../../04-spoolin-techdoc.md#75-additional-consequential-native-operations): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.6 Artwork transfer and reference-safe attachment](../../../04-spoolin-techdoc.md#76-artwork-transfer-and-reference-safe-attachment): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-251-T1-R16 · L10362](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `deletionImpact`; Maintainer / Catalog \| Person current version/impact tuples; signed 5 min token, no mutation; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R25 · L10371](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `movieLifecycle`; Maintainer / Catalog \| Movie/version, explicit final confirmation; archive first, restore archive, delete archive with current effects; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R28 · L10374](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `personLifecycle`; Maintainer / Catalog \| Version/confirmation; deletion requires current impact token and ARCHIVED state; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-253-T1-R20 · L10484](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `MovieLifecycle`; MovieLifecycle(Lifecycle) → Outcome |
| [ATDD-253-T1-R21 · L10485](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `PersonLifecycle`; PersonLifecycle(Lifecycle) → Outcome |
| [ATDD-253-T1-R22 · L10486](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `DeletionImpact`; DeletionImpact(Id) → Impact |
| [J-03 · L89](../../../02-PRD.md#32-application-journeys) | Maintainer manages people/credits \| Signed in; affected records identified \| Reuse active people, manage credit roles, and archive a person before restoration or separately confirmed permanent deletion; preview affected titles |
| [J-09 · L92](../../../02-PRD.md#32-application-journeys) | Maintainer manages movie removal \| Movie active or archived \| Archive first; restore later or separately confirm permanent deletion |
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
| [TD-C-037 · L5166](../../../04-spoolin-techdoc.md#7-transactions-lifecycle-and-durable-work) | Exact owner lock ordering/isolation; Catalog advisory serialization; no remote calls during transactions; CAS/version/impact increments; SQLSTATE mapping and at most one bounded original-ID whole-transaction retry. |
| [TD-C-038 · L5187](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Archive-first movie/person lifecycle, TV denies, fresh five-minute signed person impact tuple hash/reconfirmation, independent credit visibility and same-transaction deletion/tombstones/work; archived-person credit removal allowed only from active movie. |
| [ADR-03 · L6550](../../../04-spoolin-techdoc.md#appendix-a2-architecture-decisions) | ADR-03 \| Local serialized Catalog writes and optimistic versions. Reject distributed transaction for artwork \| Lifecycle/impact requirements; measured write contention may justify finer locks later |
| [TD-ROW-L2739 · L2739](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `deletionImpact` \| Maintainer / Catalog \| Person current version/impact tuples; signed 5 min token, no mutation |
| [TD-ROW-L2748 · L2748](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `movieLifecycle` \| Maintainer / Catalog \| Movie/version, explicit final confirmation; archive first, restore archive, delete archive with current effects |
| [TD-ROW-L2751 · L2751](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `personLifecycle` \| Maintainer / Catalog \| Version/confirmation; deletion requires current impact token and ARCHIVED state |
| [BRAIN-A10 · L55](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A10 \| Add, delete, and update cast and creators \| Working person management with defined relationship behavior |

## Implementation

1. Implement movie/person archive-first transitions and immutable permanent deletion ledgers. Public visibility always requires active title and active person; restore one side never restores the other or recreates removed relationships.
2. Build signed five-minute person impact preview over current versions and all affected active/archived movies and seeded TV credits. Lock/recompute the full source tuple before deletion; any changed impact requires review.
3. Use source advisory/row lock ordering with credit and artwork operations. Reject oversized impact rather than authorizing partial consequences; no SQL cross-owner transaction.
4. Delete eligible movie credits while preserving shared people and assets used elsewhere; delete eligible person and remaining credits while preserving titles. Commit audit/receipt/tombstones and reference-release work together.
5. Exercise same-key replay, separate stale actions, archive/restore ordering and attachment/cleanup races. Uncertain response is reconciled before another transition.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T024-A1:** Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing.
- **T024-A2:** Impact: a new/removed credit or changed title after preview invalidates the earlier person deletion confirmation; all TV effects remain represented.
- **T024-A3:** Survival: restore recreates no removed credit/person/title, permanent deletion preserves unrelated titles/people/referenced artwork and records one attributable result.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-149](../../../05-atdd.md#case-atdd-149-serialize-consequential-catalog-races) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then the Catalog advisory lock precedes fixed record lock order and prevents an unreviewed deletion-impact phantom And read-only previews use one bounded REPEATABLE READ snapshot And no page/user review retains a transaction and no owner waits on remote I/O under a domain lock Observation: Synchronize two real Catalog transactions before advisory and row locks. Capture resulting lifecycle/credit/impact versions and lock ordering, and check the winning complete state without phantoms; no remote invocation may occur under a held domain connection. |
| [ATDD-150](../../../05-atdd.md#case-atdd-150-invalidate-every-changed-deletion-consequence) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.1 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then a changed signed person/impact version or sorted credit/title tuple returns IMPACT_CHANGED with current consequences And an expired five-minute token cannot authorize deletion And only a new explicit confirmation of the current preview can delete P-A and its remaining credits Observation: Capture preview tuples/count/hash and five-minute expiry, change each consequential credit/title/person property, then send the old token under the actual Catalog lock. Inspect IMPACT_CHANGED plus fresh preview and unchanged person/credits until reconfirmation. F-IMPACT specifies oversize/overflow refusal. |
| [ATDD-311](../../../05-atdd.md#case-atdd-311-refuse-oversized-deletion-impact-without-authorizing-partial-consequences) / Integration | F-IMPACT actual person credits/titles whose complete preview crosses metadata/list envelope; ordinary small preview; unit overflow2147483648 companion. | Then the oversized response is RESOURCE_EXHAUSTED without truncated consequences or usable deletion token And person/credits/titles are unchanged; no permanent deletion occurs And the small preview remains complete and supports normal explicit confirmation Observation: Grow deterministic relationship fixtures until measured serialized complete preview is over the existing envelope, then inspect complete stored relationships and refusal. Integer overflow itself is an isolated scalar case, not simulated billions-of-rows integration evidence. |

Partial implementation contributions: [ATDD-019](../../../05-atdd.md#case-atdd-019-permanent-deletion), [ATDD-020](../../../05-atdd.md#case-atdd-020-changedremoved-target), [ATDD-024](../../../05-atdd.md#case-atdd-024-permanent-person-deletion), [ATDD-025](../../../05-atdd.md#case-atdd-025-missingdeniedcompeting-action), [ATDD-028](../../../05-atdd.md#case-atdd-028-remove-credit--changing-references), [ATDD-082](../../../05-atdd.md#case-atdd-082-archive), [ATDD-083](../../../05-atdd.md#case-atdd-083-restore-movie), [ATDD-084](../../../05-atdd.md#case-atdd-084-deletion-controls--cancel), [ATDD-103](../../../05-atdd.md#case-atdd-103-deletion-impact-changes), [ATDD-104](../../../05-atdd.md#case-atdd-104-competing-or-uncertain-lifecycle-action), [ATDD-105](../../../05-atdd.md#case-atdd-105-archive-person), [ATDD-106](../../../05-atdd.md#case-atdd-106-restore-person), [ATDD-107](../../../05-atdd.md#case-atdd-107-archive-first-controls--cancel), [ATDD-108](../../../05-atdd.md#case-atdd-108-public-visibility-and-archive-feedback), [ATDD-109](../../../05-atdd.md#case-atdd-109-independent-titleperson-lifecycle), [ATDD-110](../../../05-atdd.md#case-atdd-110-archived-person-edit-and-credit-boundary), [ATDD-111](../../../05-atdd.md#case-atdd-111-competing-interrupted-or-uncertain-person-lifecycle), [ATDD-087](../../../05-atdd.md#case-atdd-087-private-archive-boundary), [ATDD-264](../../../05-atdd.md#case-atdd-264-inspect-archives-state-transitions-and-feedback). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T024` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T024` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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


## Implementation delivery (17 October 2026)

Implementation for this task is on branch `codex/g04-g16-delivery` ([PR #8](https://github.com/okfriansyah-moh/spoolIn/pull/8)). Local task and browser runners were **not** executed on the authoring machine (host resource limit). Use CI or a better-resourced environment to run `ops/bin/test-unit.sh` / `ops/bin/test-integration.sh` for this task; set `SPOOLIN_TASK_BROWSER=1` for lighter Playwright when running browser integrations. Formal acceptance, combined review, and release proof remain with **G26**.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T025 — Connect private archives and irreversible confirmations](T025-connect-private-archives-and-irreversible-confirmations.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
