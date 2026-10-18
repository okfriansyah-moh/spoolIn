---
kind: task
id: T013
group: G10
depends_on:
- T004
- T010
- T012
readiness: Ready
status: In progress
risk: medium
test_strategy: test-first
visual_review: required
---

# T013 — Connect discovery, details and public people views

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria. This task owns the change surface and local evidence below. Risk is medium: incorrect composition, build assumptions or delivery claims can hide missing behavior. Verify the actual declared boundary. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-001 | both |
| ATDD-002 | both |
| ATDD-003 | both |
| ATDD-004 | both |
| ATDD-005 | both |
| ATDD-006 | both |
| ATDD-007 | both |
| ATDD-008 | both |
| ATDD-009 | both |
| ATDD-010 | both |
| ATDD-011 | both |
| ATDD-012 | both |
| ATDD-013 | both |
| ATDD-014 | both |
| ATDD-015 | both |
| ATDD-029 | implements |
| ATDD-030 | both |
| ATDD-031 | both |
| ATDD-032 | both |
| ATDD-133 | both |
| ATDD-022 | both |
| ATDD-139 | both |
| ATDD-256 | both |
| ATDD-256-T1-R01 | both |
| ATDD-256-T1-R02 | both |
| ATDD-256-T1-R03 | both |
| ATDD-256-T1-R04 | both |
| ATDD-256-T1-R05 | both |
| ATDD-256-T1-R06 | both |
| ATDD-256-T1-R07 | both |
| ATDD-257 | both |
| ATDD-257-T1-R01 | both |
| ATDD-257-T1-R02 | both |
| ATDD-257-T1-R03 | both |
| ATDD-257-T1-R04 | both |
| ATDD-303 | both |
| FR-001 | both |
| FR-002 | both |
| FR-003 | both |
| FR-005 | both |
| FR-007 | both |
| FR-017 | both |
| NFR-007 | both |
| NFR-008 | both |
| A06 | both |
| A09 | both |
| A11 | both |
| A14 | both |
| D25 | both |
| D26 | both |
| AC-001 | both |
| AC-001-C01 | both |
| AC-001-C02 | both |
| AC-002 | both |
| AC-002-C01 | both |
| AC-003 | both |
| AC-003-C01 | both |
| AC-004 | both |
| AC-004-C01 | both |
| AC-004-C02 | both |
| AC-005 | both |
| AC-005-C01 | both |
| AC-006 | both |
| AC-006-C01 | both |
| AC-006-C02 | both |
| AC-006-C03 | both |
| AC-007 | both |
| AC-007-C01 | both |
| AC-008 | both |
| AC-008-C01 | both |
| AC-008-C02 | both |
| AC-008-C03 | both |
| AC-009 | both |
| AC-009-C01 | both |
| AC-010 | both |
| AC-010-C01 | both |
| AC-011 | both |
| AC-011-C01 | both |
| AC-012 | both |
| AC-012-C01 | both |
| AC-012-C02 | both |
| AC-013 | both |
| AC-013-C01 | both |
| AC-013-C02 | both |
| AC-014 | both |
| AC-014-C01 | both |
| AC-015 | both |
| AC-015-C01 | both |
| AC-022 | both |
| AC-022-C01 | both |
| AC-022-C02 | both |
| AC-030 | both |
| AC-030-C01 | both |
| AC-031 | both |
| AC-031-C01 | both |
| AC-031-C02 | both |
| AC-032 | both |
| AC-032-C01 | both |
| AC-159 | both |
| AC-159-C01 | both |
| AC-159-C02 | both |
| AC-159-C03 | both |
| G-03 | both |
| OD-005 | both |
| OD-018 | both |
| US-005 | both |
| PRD-POLICY-04 | both |
| PRD-BOUND-07 | both |
| PRD-POLICY-23 | both |
| UI-discovery-catalog | both |
| UI-discovery-catalog-loading | both |
| UI-discovery-catalog-empty | both |
| UI-discovery-catalog-failure | both |
| UI-discovery-catalog-outdated | both |
| UI-discovery-catalog-invalid | both |
| UI-discovery-catalog-no-match | both |
| UI-details-movie | both |
| UI-details-tv | both |
| UI-details-unavailable | both |
| UI-details-identities | both |
| DS-POLICY-02 | implements |
| DS-POLICY-04 | implements |
| TD-C-007 | both |
| V-READ | both |
| TD-ROW-L3591 | both |
| TD-ROW-L3595 | both |
| TD-ROW-L5183 | both |
| TD-ROW-L5503 | both |
| TD-ROW-L5524 | both |
| BRAIN-A06 | both |
| BRAIN-A09 | both |
| BRAIN-A11 | both |
| BRAIN-A14 | both |
| BRAIN-D25 | both |
| BRAIN-D26 | both |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T010 — Connect account and session browser journeys](../../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T012 — Serve precise public catalog and people reads](T012-serve-precise-public-catalog-and-people-reads.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | frontend/src/routes/+page.svelte | T013 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/titles/[id]/+page.svelte | T013 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/people/+page.svelte | T013 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/people/[id]/+page.svelte | T013 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/features/catalog/ | T013 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/features/person/read/ | T013 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/discovery/ | T013 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.1 Routes, screens and API dependencies](../../../04-spoolin-techdoc.md#31-routes-screens-and-api-dependencies): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.1.1 UI reference and implementation map](../../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.3 Search, navigation and request state](../../../04-spoolin-techdoc.md#33-search-navigation-and-request-state): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.6 Components, accessibility and responsive behavior](../../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [FR-001 · L235](../../../02-PRD.md#61-functional-requirements) | Browse visible movies/TV titles, artwork, active people, and public credits \| A06/A09/A14 Required; D24/D25 Committed; product policy Committed (section 13.1) \| AC-001 through AC-005, AC-113, AC-134, AC-135 |
| [FR-002 · L236](../../../02-PRD.md#61-functional-requirements) | Search movie titles and cast/creator names with the approved matching and visibility rules \| A11/A14 Required; product policy Committed (section 13.1) \| AC-006 through AC-010, AC-022, AC-134, AC-147, AC-159 |
| [FR-003 · L237](../../../02-PRD.md#61-functional-requirements) | Combine type/genre/text/year/month filters using honest release precision \| D26 Committed; product policy Committed (section 13.1) \| AC-011 through AC-015 |
| [FR-005 · L239](../../../02-PRD.md#61-functional-requirements) | Manage active people; archive before restore or confirmed permanent deletion, retaining remaining credits privately \| A09/A10/A11 Required; person lifecycle Committed (section 13.1) \| AC-022 through AC-025, AC-129, AC-131 through AC-138, AC-140/AC-142, AC-148 |
| [FR-007 · L241](../../../02-PRD.md#61-functional-requirements) | Preserve usable metadata, explicit unknowns, identity, date precision, and provenance \| D25/D26 Committed \| AC-029 through AC-032, AC-143, AC-145 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-007 · L265](../../../02-PRD.md#62-quality-and-delivery-requirements) | Responsive, keyboard-usable journeys with accurate feedback and retained recoverable work \| A13/A14 Required; keyboard use Committed \| AC-083 through AC-085, AC-139, AC-147, AC-159 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [A06 · L285](../../../02-PRD.md#63-project-requirements-coverage) | List movies with artwork \| FR-001 / AC-001, AC-003 |
| [A09 · L288](../../../02-PRD.md#63-project-requirements-coverage) | List cast and creators \| FR-001/FR-005/FR-006 / AC-004, AC-022, AC-026, AC-134 |
| [A11 · L290](../../../02-PRD.md#63-project-requirements-coverage) | Search movies, cast, creators \| FR-002 / AC-006 through AC-010, AC-022, AC-134 |
| [A14 · L293](../../../02-PRD.md#63-project-requirements-coverage) | Sufficient operation feedback \| NFR-007; FR-001/FR-002/FR-004/FR-009 / AC-002, AC-009, AC-018, AC-038, AC-085 |
| [D25 · L326](../../../02-PRD.md#64-project-decision-register) | Metadata, type/genres, stable identities, provenance \| FR-006/FR-007 / AC-026 through AC-032 |
| [D26 · L327](../../../02-PRD.md#64-project-decision-register) | Composable year/month filters with source date precision \| FR-003/FR-007 / AC-011 through AC-015, AC-029, AC-032 |
| [AC-001 · L339](../../../02-PRD.md#us-001-browse-titles-and-people) | List and details \| Active movies/TV series exist \| A reader opens the catalog and a title \| The correct title/type, available artwork, metadata, and visible credits appear. No episode-tracking controls appear. |
| [AC-001-C01 · L339](../../../02-PRD.md#us-001-browse-titles-and-people) | The correct title/type, available artwork, metadata, and visible credits appear. |
| [AC-001-C02 · L339](../../../02-PRD.md#us-001-browse-titles-and-people) | No episode-tracking controls appear. |
| [AC-002 · L340](../../../02-PRD.md#us-001-browse-titles-and-people) | Loading, empty, failure \| A catalog request is pending, empty, or failed \| The reader waits for it \| Show loading, a successful empty state, or a failure with retry respectively; an outage never appears as an empty catalog. |
| [AC-002-C01 · L340](../../../02-PRD.md#us-001-browse-titles-and-people) | Show loading, a successful empty state, or a failure with retry respectively; an outage never appears as an empty catalog. |
| [AC-003 · L341](../../../02-PRD.md#us-001-browse-titles-and-people) | Missing artwork \| Artwork is absent or fails to load \| A title is displayed \| A placeholder preserves readable title information and navigation; missing artwork does not make the title disappear. |
| [AC-003-C01 · L341](../../../02-PRD.md#us-001-browse-titles-and-people) | A placeholder preserves readable title information and navigation; missing artwork does not make the title disappear. |
| [AC-004 · L342](../../../02-PRD.md#us-001-browse-titles-and-people) | Shared and absent credits \| An active person has cast/creator credits, another person or title is archived, or credits are unknown \| The reader opens public credits \| Show applicable credits only where both title and person are active, without cloning the person; unknown credits remain explicit. No archived person/title information or retained credit relationship appears publicly. |
| [AC-004-C01 · L342](../../../02-PRD.md#us-001-browse-titles-and-people) | Show applicable credits only where both title and person are active, without cloning the person; unknown credits remain explicit. |
| [AC-004-C02 · L342](../../../02-PRD.md#us-001-browse-titles-and-people) | No archived person/title information or retained credit relationship appears publicly. |
| [AC-005 · L343](../../../02-PRD.md#us-001-browse-titles-and-people) | Unavailable title \| A movie was archived or deleted after listing \| A visitor/Viewer opens its old detail link \| Report that it is unavailable and provide catalog navigation; do not expose archived information or substitute a same-named title. |
| [AC-005-C01 · L343](../../../02-PRD.md#us-001-browse-titles-and-people) | Report that it is unavailable and provide catalog navigation; do not expose archived information or substitute a same-named title. |
| [AC-006 · L351](../../../02-PRD.md#us-002-search-and-filter) | Matching \| Visible titles have display/original titles and active cast/creator people have names \| The reader searches titles or people with a differently cased substring \| Return matching visible titles for title search and matching active cast/creator people for name search. Archived people are not returned, and their retained credits do not provide a public title-search match; independent matching title text still may. Account data never appear. |
| [AC-006-C01 · L351](../../../02-PRD.md#us-002-search-and-filter) | Return matching visible titles for title search and matching active cast/creator people for name search. |
| [AC-006-C02 · L351](../../../02-PRD.md#us-002-search-and-filter) | Archived people are not returned, and their retained credits do not provide a public title-search match; independent matching title text still may. |
| [AC-006-C03 · L351](../../../02-PRD.md#us-002-search-and-filter) | Account data never appear. |
| [AC-007 · L352](../../../02-PRD.md#us-002-search-and-filter) | No match / clear \| Text has no matches with current filters \| The reader searches, then clears text \| Show no matches first; clearing removes only the text restriction and preserves other filters. |
| [AC-007-C01 · L352](../../../02-PRD.md#us-002-search-and-filter) | Show no matches first; clearing removes only the text restriction and preserves other filters. |
| [AC-008 · L353](../../../02-PRD.md#us-002-search-and-filter) | Navigation and order \| Results include ties, partial dates, and unknown dates \| The reader browses results repeatedly \| Titles default to newest release context first, unknown dates last; equal input/data have consistent ordering without navigation duplication. Show correction for input outside supported limits. Search length follows section 5.5; navigation and other query limits are techdoc work. |
| [AC-008-C01 · L353](../../../02-PRD.md#us-002-search-and-filter) | Titles default to newest release context first, unknown dates last; equal input/data have consistent ordering without navigation duplication. |
| [AC-008-C02 · L353](../../../02-PRD.md#us-002-search-and-filter) | Show correction for input outside supported limits. |
| [AC-008-C03 · L353](../../../02-PRD.md#us-002-search-and-filter) | Search length follows section 5.5; navigation and other query limits are techdoc work. |
| [AC-009 · L354](../../../02-PRD.md#us-002-search-and-filter) | Rapid edits / failure \| The reader changes a pending search \| Results arrive out of order or the latest request fails \| Only current criteria control the displayed result; retain those criteria and show their loading/failure state with retry. |
| [AC-009-C01 · L354](../../../02-PRD.md#us-002-search-and-filter) | Only current criteria control the displayed result; retain those criteria and show their loading/failure state with retry. |
| [AC-010 · L355](../../../02-PRD.md#us-002-search-and-filter) | Permission / interruption \| A restricted read is attempted or navigation interrupts a search \| The request ends \| No restricted data are shown; old results cannot replace the reader's current view. |
| [AC-010-C01 · L355](../../../02-PRD.md#us-002-search-and-filter) | No restricted data are shown; old results cannot replace the reader's current view. |
| [AC-011 · L356](../../../02-PRD.md#us-002-search-and-filter) | Combined filters \| Titles vary in genre, type, year/month, and text \| The reader combines filters \| Results match at least one selected genre and every other active filter category; resetting one category preserves the others. |
| [AC-011-C01 · L356](../../../02-PRD.md#us-002-search-and-filter) | Results match at least one selected genre and every other active filter category; resetting one category preserves the others. |
| [AC-012 · L357](../../../02-PRD.md#us-002-search-and-filter) | Partial dates \| Titles are known as 2024, October 2024, and 9 October 2024 \| The reader selects 2024, then October \| All match the year; only the latter two match October. No missing month/day is fabricated. |
| [AC-012-C01 · L357](../../../02-PRD.md#us-002-search-and-filter) | All match the year; only the latter two match October. |
| [AC-012-C02 · L357](../../../02-PRD.md#us-002-search-and-filter) | No missing month/day is fabricated. |
| [AC-013 · L358](../../../02-PRD.md#us-002-search-and-filter) | Unknown year \| A title's release year is unknown \| A year filter is applied or removed \| Exclude it while a year is selected; allow it without a date filter. Never filter on the date the record was added. |
| [AC-013-C01 · L358](../../../02-PRD.md#us-002-search-and-filter) | Exclude it while a year is selected; allow it without a date filter. |
| [AC-013-C02 · L358](../../../02-PRD.md#us-002-search-and-filter) | Never filter on the date the record was added. |
| [AC-014 · L359](../../../02-PRD.md#us-002-search-and-filter) | Month validation \| No year is selected, or the month is invalid \| The reader attempts a month filter or clears year \| Month requires a year; invalid months receive correction; clearing year clears month so no hidden date restriction remains. |
| [AC-014-C01 · L359](../../../02-PRD.md#us-002-search-and-filter) | Month requires a year; invalid months receive correction; clearing year clears month so no hidden date restriction remains. |
| [AC-015 · L360](../../../02-PRD.md#us-002-search-and-filter) | Empty combination \| Valid combined filters exclude all titles \| Search completes \| Show no matches and retain editable filters; do not silently broaden them. |
| [AC-015-C01 · L360](../../../02-PRD.md#us-002-search-and-filter) | Show no matches and retain editable filters; do not silently broaden them. |
| [AC-022 · L385](../../../02-PRD.md#us-004-maintain-people-and-credits) | List/search people \| Active people exist or the public list is empty; archived people may also exist \| A reader lists/searches people, or a Maintainer opens the people archive \| Public results show active catalog people and permitted roles; the Maintainer-only archive identifies archived people and retained credits. Show distinct loading/empty/no-match/failure states; same names do not collapse identities. |
| [AC-022-C01 · L385](../../../02-PRD.md#us-004-maintain-people-and-credits) | Public results show active catalog people and permitted roles; the Maintainer-only archive identifies archived people and retained credits. |
| [AC-022-C02 · L385](../../../02-PRD.md#us-004-maintain-people-and-credits) | Show distinct loading/empty/no-match/failure states; same names do not collapse identities. |
| [AC-030 · L409](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Unknown optional data \| Runtime, country, credits, end date, external ID, or artwork is unknown \| The record is displayed or edited \| Unknown is explicit; it does not become zero runtime, invented credits, a fabricated date, or guessed artwork. |
| [AC-030-C01 · L409](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Unknown is explicit; it does not become zero runtime, invented credits, a fabricated date, or guessed artwork. |
| [AC-031 · L410](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Identity/provenance \| Remakes or people share names, or sources use similar identifiers \| Records are inspected or seeded again \| Distinct works/people retain identities; source-scoped IDs and source record/link, snapshot/retrieval date, and reuse notes remain available. Text alone never merges them. |
| [AC-031-C01 · L410](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Distinct works/people retain identities; source-scoped IDs and source record/link, snapshot/retrieval date, and reuse notes remain available. |
| [AC-031-C02 · L410](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Text alone never merges them. |
| [AC-032 · L411](../../../02-PRD.md#us-005-read-trustworthy-metadata) | TV/date labels \| A series has first-air context, status, and optional end context/runtime \| Its details are viewed \| Label series status and first-air/end information honestly; label TV duration as episode runtime and retain year/year-month/full-date precision. |
| [AC-032-C01 · L411](../../../02-PRD.md#us-005-read-trustworthy-metadata) | Label series status and first-air/end information honestly; label TV duration as episode runtime and retain year/year-month/full-date precision. |
| [AC-159 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Search-length boundary and correction \| Recommended search bounds of 0 to 200 are configured and other filters are selected \| A reader clears text, submits a 200-code-point query, or enters 201 code points \| Clearing text preserves other filters; 200 passes the length check. For 201, retain the full text and filters, show correction, and do not issue a truncated query or present old results as matching the invalid current query. No-match, loading, and failure remain distinct. |
| [AC-159-C01 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Clearing text preserves other filters; 200 passes the length check. |
| [AC-159-C02 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | For 201, retain the full text and filters, show correction, and do not issue a truncated query or present old results as matching the invalid current query. |
| [AC-159-C03 · L549](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | No-match, loading, and failure remain distinct. |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [OD-005 · L1021](../../../02-PRD.md#131-resolved-product-decisions) | Case-insensitive partial title/person search; newest releases first, unknown last; any selected genre and all other filter categories; month requires year \| FR-002/FR-003; AC-006 through AC-015, AC-159; search-length policy in section 5.5, remaining navigation/query limits transfer to OD-019 |
| [OD-018 · L1023](../../../02-PRD.md#131-resolved-product-decisions) | Seeded TV read/search/filter/metadata only; no direct TV management or episode tracking \| FR-001/FR-007/FR-008; AC-001/AC-029/AC-032/AC-033 |
| [US-005 · L402](../../../02-PRD.md#us-005-read-trustworthy-metadata) | ### US-005: Read trustworthy metadata As a reader, I want useful metadata with source context so I can distinguish works without guessed details. Requirements: FR-007, FR-003. The complete conceptual baseline is in section 9. |
| [PRD-POLICY-04 · L120](../../../02-PRD.md#51-included-behavior) | Movies filter by their source original release context; TV series use first-air context. Year-only dates match a year and no month. Year/month dates match that month without inventing a day. Unknown years remain available without a date filter and are excluded when a year is selected. Database-added dates do not substitute for release dates. |
| [PRD-BOUND-07 · L183](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Catalog search \| 0 characters \| 200 characters \| Empty search removes only the text restriction |
| [PRD-POLICY-23 · L194](../../../02-PRD.md#validation-safe-handling-and-feedback) | A blank optional text value may explicitly clear that value; an omitted edit value leaves it unchanged. Empty search always clears the text restriction, including when a higher minimum is configured for nonempty queries. Validate a supplied nonempty optional value against its bounds. Do not force unknown optional data to become zero, an invented date, or placeholder text. Wire representations of omission and clearing belong to the techdoc. |
| [DS-POLICY-02 · L39](../../../03-design-system.md#identity-and-voice) | Use “SpoolIn” as the wordmark and an original paired-frame mark at small sizes. Preserve its silhouette; never substitute a play button that implies streaming. Icon and component identity specimen show browser-icon sizes of 16, 24, and 48px. The compact icon uses the stable dark brand tile across both themes; surrounding UI changes theme. |
| [DS-POLICY-04 · L47](../../../03-design-system.md#one-token-authority-two-themes) | All JSON dimensions use explicit CSS strings (`px`, `ms`) to avoid implicit-unit conversion. Theme switching changes color and the display-family alias, while preserving the selected state, focus, filters, and typed text. It neither submits nor resets a form. Body/control type, sizing, spacing and component geometry stay shared. The initial preference follows the system when no review preference exists; only the non-sensitive theme choice is persisted in the prototype. Theme persistence is unrelated to authentication or draft recovery. |
| [TD-C-007 · L953](../../../04-spoolin-techdoc.md#33-search-navigation-and-request-state) | URL q/kind/repeated genre/year/month with invalid-state correction, 250ms debounce, generation-bound late reply suppression, criteria-preserving retries, pagination history and fresh cursor invalidation after confirmed mutations. |
| [V-READ · L6386](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-READ \| Seed real PostgreSQL with active/archived movie/TV/person ties, Unicode names, partial/unknown dates; exercise GraphQL and real Catalog gRPC; assert exact visible IDs/metadata, stable cursors, literal substring/filter AND/genre OR, no hidden leakage and loading/empty/failure separation |
| [TD-ROW-L3591 · L3591](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `browser_session` \| Core `session/persistence` \| Browser expiry, revocation and CSRF binding |
| [TD-ROW-L3595 · L3595](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `title`, `title_genre` \| Catalog `movie/persistence`; TV read projections through `search` \| Title identity plus normalized many-to-many genre membership |
| [TD-ROW-L5183 · L5183](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Browse/detail/impact preview \| One consistent read of rows, relationships and count/impact version \| REPEATABLE READ, read-only, &lt;=3 s; never keep transaction between pages or while user reviews |
| [TD-ROW-L5503 · L5503](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-01 browser→Core \| Session theft/fixation/CSRF grants writes \| Secure HttpOnly session, rotate, Origin/CSRF, narrow host; session/authentication \| Reuse revoked token, foreign Origin, login CSRF and expiry; compromised browser still powerful; Core owner |
| [TD-ROW-L5524 · L5524](../../../04-spoolin-techdoc.md#83-api-security-enforcement) | Browser GraphQL / Core \| Exact Host/Origin, session/CSRF, cookie/storage rules, typed presence/schema/size/cost, object permissions, §8.4 quota \| Feature commands only; Catalog/Core transactions own changes; no client SQL or arbitrary resolver dispatch \| Forged role/owner, SQL-looking text, unknown keys/masks, deep/aliased requests, concurrent replay/stale save produce no unintended effect |
| [BRAIN-A06 · L51](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A06 \| List movies with artwork \| Movie listing displays available artwork and handles missing/broken artwork |
| [BRAIN-A09 · L54](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A09 \| List cast and creators \| Accessible people/credit views |
| [BRAIN-A11 · L56](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A11 \| Search movies, cast, and creators \| Defined searchable fields and matching behavior, including no-results/error states |
| [BRAIN-A14 · L59](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A14 \| Sufficient feedback for upload, listing, updating, and searching \| Loading, success, failure, validation, and empty states integrated into features |
| [BRAIN-D25 · L94](../../../01-brainstorming-result.md#4-decision-register) | D25 \| Committed \| Categorize movies and TV series by title type and genres, include synopses and useful descriptive metadata, and retain stable identities and source provenance to support future review aggregation; detailed baseline in section 8.2 |
| [BRAIN-D26 · L95](../../../01-brainstorming-result.md#4-decision-register) | D26 \| Committed \| Filter catalog titles by release year and optionally by month within that year, composable with title type, genre, and text search; preserve source date precision |

## Implementation

1. Implement discovery grid, title and public-person routes using shared components and typed operation documents; retain fixed SpoolIn headline/supporting copy and explicit date/type/unknown labels.
2. Make URL q/kind/repeated genre/year/month the restorable criteria. Debounce 250ms, identify each request generation, abort obsolete requests and prevent old responses after navigation/history from replacing current state.
3. Clear only the selected criterion; clearing year clears month, and clearing text preserves other filters. Invalid search keeps full text and hides stale matching claims; distinguish loading/empty/no-match/failure/outdated states.
4. Render full metadata, source provenance and permitted credits with missing/broken artwork fallback. No episode controls, direct TV management or account information appears.
5. Integrate against real read services and capture all discovery/detail states. Public people reading is also owned here; people-management composition is T019. Invalidation after actual writes is proved by T043 once producers exist.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T013-A1:** Search: a delayed old reply cannot replace current criteria; invalid 201-code-point input stays correctable without a truncated request; browser Back restores complete criteria.
- **T013-A2:** Details: selected UUID/type/metadata/provenance match the clicked record, same names never redirect identity, missing art preserves navigation and unavailable target leaks no archive data.
- **T013-A3:** UI: both themes preserve visible content/actions across declared viewports; failure has retry, successful zero rows has an empty/no-match explanation.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-001](../../../05-atdd.md#case-atdd-001-list-and-details) / End-to-end | Browse M-A, M-B, TV-C and U-D as Visitor, Viewer V and Maintainer A. M-A has image OLD and two roles for P-A; U-D lacks artwork. | Then The correct title/type, available artwork, metadata, and visible credits appear. No episode-tracking controls appear. Observation: Inspect list/detail response UUID/type/metadata, selected artwork and per-role credit DOM for M-A/M-B/TV-C/U-D; no episode controls. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-002](../../../05-atdd.md#case-atdd-002-loading-empty-failure) / End-to-end | Independent rows: pause latest query at dispatch; successful zero-row catalog; inject Catalog UNAVAILABLE; recover and retry same criteria. | Then Show loading, a successful empty state, or a failure with retry respectively; an outage never appears as an empty catalog. Observation: Observe loading before releasing the query barrier, empty only after a successful zero-row reply, and safe retry after actual UNAVAILABLE; criteria survive retry. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-003](../../../05-atdd.md#case-atdd-003-missing-artwork) / End-to-end | Rows: assetId absent, Media UNAVAILABLE, image GET 404, truncated response. Title M-A remains navigable with unchanged metadata. | Then A placeholder preserves readable title information and navigation; missing artwork does not make the title disappear. Observation: Capture absent/404/truncated/unavailable image response and actual placeholder/title/navigation; metadata stays readable despite Media failure. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-004](../../../05-atdd.md#case-atdd-004-shared-and-absent-credits) / End-to-end | F-LIFECYCLE truth table: active/archived title × active/archived person; also active title with no known credits. | Then Show applicable credits only where both title and person are active, without cloning the person; unknown credits remain explicit. No archived person/title information or retained credit relationship appears publicly. Observation: Inspect returned credits for every active/archived endpoint pair and no-credit row; same person UUID appears once with the selected roles rather than duplicate people. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-005](../../../05-atdd.md#case-atdd-005-unavailable-title) / End-to-end | List M-A, then archive or delete it in a second authorized session before following the saved UUID link. M-B has the same title text. | Then Report that it is unavailable and provide catalog navigation; do not expose archived information or substitute a same-named title. Observation: Follow the saved M-A UUID after archive/delete and inspect safe unavailable feedback; M-B is never substituted because its title text matches. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-006](../../../05-atdd.md#case-atdd-006-matching) / End-to-end | Search "lIgHt" against display title, "cAhAyA" against original title, and "aLeX" in people mode. P-A active/P-B archived; private account A also named Alex. | Then Return matching visible titles for title search and matching active cast/creator people for name search. Archived people are not returned, and their retained credits do not provide a public title-search match; independent matching title text still may. Account data never appear. Observation: Compare exact IDs for display/original-title and public-person casefolded searches; private account A and archived P-B never appear in people results. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-007](../../../05-atdd.md#case-atdd-007-no-match--clear) / End-to-end | Movie/Drama/2024 with text "ZZ-no-match"; clear only text. Reapply text and clear only type, then only genre, independently. | Then Show no matches first; clearing removes only the text restriction and preserves other filters. Observation: Inspect URL/control/request criteria while clearing one filter at a time; remaining filters stay present and the returned fixture IDs reflect that exact combination. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-008](../../../05-atdd.md#case-atdd-008-navigation-and-order) / End-to-end | F-ORDER: year 2024 fulldateOct09, monthOct, year-only, year 2023, all-unknown; tie UUIDs ascending. Fetch first=2 to last page and repeat unchanged. | Then Titles default to newest release context first, unknown dates last; equal input/data have consistent ordering without navigation duplication. Show correction for input outside supported limits. Search length follows section 5.5; navigation and other query limits are techdoc work. Observation: Record full sort tuples and UUIDs across first=2 pages and unchanged repeat; focus/back navigation and next-page state follow the real pageInfo. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-009](../../../05-atdd.md#case-atdd-009-rapid-edits--failure) / End-to-end | Dispatch query "Light" then "Map"; release Map reply first, then Light; in another row fail Map after delayed Light succeeds. Navigate away before late reply. | Then Only current criteria control the displayed result; retain those criteria and show their loading/failure state with retry. Observation: Release Map before Light, then obsolete success/failure after the current outcome; inspect request generation, criteria and unchanged current rendered result/error. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-010](../../../05-atdd.md#case-atdd-010-permission--interruption) / End-to-end | Visitor requests managedTitle/archives/audit; separately navigate from a paused search to people before releasing the old catalog response. | Then No restricted data are shown; old results cannot replace the reader's current view. Observation: Record direct private-route/query denial and retained public state; release an obsolete catalog reply after people navigation and verify it does not replace the new screen. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-011](../../../05-atdd.md#case-atdd-011-combined-filters) / End-to-end | F-FILTER: M-A MOVIE Drama+Mystery2024Oct09; M-B MOVIE Documentary2024year-only; TV-C TV Drama2024Octmonth; U-D MOVIE Drama unknown. Select two genres and all other categories. | Then Results match at least one selected genre and every other active filter category; resetting one category preserves the others. Observation: Capture selected type/genre/year/month/text predicates and exact result UUIDs; genre OR and category AND are separately falsified by the named fixture rows. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-012](../../../05-atdd.md#case-atdd-012-partial-dates) / End-to-end | Use F-FILTER date variants2024, 2024-10, 2024-10-09; select year 2024 then month 10. Exact IDs are {M-A, M-B, TV-C}, then {M-A, TV-C}. | Then All match the year; only the latter two match October. No missing month/day is fabricated. Observation: Compare year 2024 IDs {M-A, M-B, TV-C} and October IDs {M-A, TV-C}; inspect displayed precision without invented day values. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-013](../../../05-atdd.md#case-atdd-013-unknown-year) / End-to-end | U-D has unknown release year and createdAt2024-10-09. Select2024 excludes U-D; remove date filter includes it. | Then Exclude it while a year is selected; allow it without a date filter. Never filter on the date the record was added. Observation: Inspect stored unknown release versus createdAt and returned IDs with/without year filter; filtering must not use creation time or a fabricated release date. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-014](../../../05-atdd.md#case-atdd-014-month-validation) / End-to-end | Rows: month 10 without year; month 0/13; clear2024 while10 selected. Correct feedback and no hidden month restriction. | Then Month requires a year; invalid months receive correction; clearing year clears month so no hidden date restriction remains. Observation: Observe month-alone/0/13 field errors and clearing-year behavior in controls, URL and direct request; no hidden month restriction or partial valid search. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-015](../../../05-atdd.md#case-atdd-015-empty-combination) / End-to-end | Filter MOVIE+Documentary+2025+"Light" produces zero. Retain exact URL/control criteria. | Then Show no matches and retain editable filters; do not silently broaden them. Observation: Inspect successful zero-result feedback and retained MOVIE/Documentary/2025/Light criteria; no dependency-failure wording or reset. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-022](../../../05-atdd.md#case-atdd-022-listsearch-people) / End-to-end | Active P-A and same-name P-C; archived P-B. Rows pending/empty/no-match/failure/success on public people and private archive. | Then Public results show active catalog people and permitted roles; the Maintainer-only archive identifies archived people and retained credits. Show distinct loading/empty/no-match/failure states; same names do not collapse identities. Observation: Inspect public people response UUIDs/credits and pending/empty/error retry states; private archive results use separate authorized predicates. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-030](../../../05-atdd.md#case-atdd-030-unknown-optional-data) / End-to-end | All optional fields absent; compare NULL/empty collection roundtrip and visible Unknown/Artworkunavailable; do not assign0runtime or synthetic date. | Then Unknown is explicit; it does not become zero runtime, invented credits, a fabricated date, or guessed artwork. Observation: Inspect typed null/empty collections and visible Unknown/Artwork unavailable labels; stored/read values do not acquire zero runtime or synthetic date/artwork. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-031](../../../05-atdd.md#case-atdd-031-identityprovenance) / End-to-end | Two "The Last Light" titles and two "Alex" people have distinct IDs; two source namespaces reuse externalID42. Snapshot/retrieval/reuse field evidence. | Then Distinct works/people retain identities; source-scoped IDs and source record/link, snapshot/retrieval date, and reuse notes remain available. Text alone never merges them. Observation: Compare independent title/person IDs and source namespace/external identity despite matching spelling; inspect retrieval/reuse provenance without account-directory data. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-032](../../../05-atdd.md#case-atdd-032-tvdate-labels) / End-to-end | TV-C continuing, firstair2024-10, unknown end/episode runtime; secondrow ended with knownend2025year-only. No whole-series runtime or episode actions. | Then Label series status and first-air/end information honestly; label TV duration as episode runtime and retain year/year-month/full-date precision. Observation: Inspect continuing/ended TV labels, first-air/end precision and optional episode-runtime meaning; no whole-series runtime or episode management controls. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-133](../../../05-atdd.md#case-atdd-133-search-length-boundary-and-correction) / End-to-end | Queriesrepeatx0/200/201withDrama2024; empty clear onlytext, 200sendsunchanged, 201fullinputretainednoissuedtruncatedquery/old matching result. | Then Clearing text preserves other filters; 200 passes the length check. For 201, retain the full text and filters, show correction, and do not issue a truncated query or present old results as matching the invalid current query. No-match, loading, and failure remain distinct. Observation: Inspect query0/200/201 code-point boundary, URL criteria and correction through browser/direct Catalog; invalid query is not an empty successful result. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-139](../../../05-atdd.md#case-atdd-139-restore-complete-criteria-without-accepting-old-results) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §3.3 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then only the current request generation and full criteria populate results And URL genre sets are sorted and deduplicated, month requires year, and filter changes reset cursors And history restores complete criteria/page identity while invalid parameters show correction Observation: Use browser history and barriers for Light→Map replies in both orders. Inspect current URL/control criteria, query generation, cursor and rendered IDs; release the obsolete reply and assert it cannot change the latest result/error state. |
| [ATDD-256](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) / End-to-end | F-UI; each state row uses F-CATALOG/F-ACTORS and its source-defined stage. Replace local fixture IDs with deterministic actual IDs; no simulated save/upload/client result counts as execution evidence. | Then the visible heading, identity context, feedback and action match the row oracle And each outcome actually applicable to this flow remains distinct from its pending, failed or unavailable alternatives And theme, width and keyboard changes preserve state and show no restricted data or unsupported feature Observation: Exercise `session`, `policy`, `genres`, `titles` in `/`, capturing the current response/operation and each row’s visible heading, identity, controls, retained values and safe next action. Assert the named FilterBar / TitleGrid through actual visible layout and accessible names/status rather than hidden fixture text. Run each row independently in both themes at declared widths; verify keyboard/focus and permitted navigation without leaking protected data. Source fragments identify design references, not implementation selectors. \| State reference \| Actor / independent trigger \| Visible semantic oracle \| \| --- \| --- \| --- \| \| catalog \| Visitor/Viewer/Maintainer; complete `titles` with active F-CATALOG data \| Show the fixed discovery headline/promise, search, type/genre/year/month controls and active title cards with identity links and truthful result count. Keep same-name titles separate; browse details or change criteria. \| \| catalog-loading \| Reader; hold the current `titles` response pending \| Show Finding titles for the current criteria, withhold obsolete results and announce loading; allow criteria changes without presenting stale matches. \| \| catalog-empty \| Reader; return a successful `titles` response from an empty active catalog \| Show The catalog is empty and a browse-again action; retain criteria and distinguish an empty installation from failure or filtered no-match. \| \| catalog-failure \| Reader; fail the current `titles` read \| Show The catalog could not load, retained query/filters and retry of those criteria; show neither invented empty data nor older results as current. \| \| catalog-outdated \| Reader; change criteria before the prior `titles` response arrives \| Withhold the late result; show refresh/loading feedback for the current generation. Only its response can fill the grid; retry uses current criteria. \| \| catalog-invalid \| Reader; enter query length above effective maximum or invalid filter parameters \| Retain complete invalid input and other filters; identify correction and issue no truncated query. Old results cannot appear to match it; move focus to the affected control through the error link. \| \| catalog-no-match \| Reader; return successful empty `titles` for valid restrictive filters while other active titles exist \| Show No matching titles with editable retained filters. Clearing text removes only text; changing one filter preserves the rest. |
| [ATDD-257](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback) / End-to-end | F-UI; each state row uses F-CATALOG/F-ACTORS and its source-defined stage. Replace local fixture IDs with deterministic actual IDs; no simulated save/upload/client result counts as execution evidence. | Then the visible heading, identity context, feedback and action match the row oracle And each outcome actually applicable to this flow remains distinct from its pending, failed or unavailable alternatives And theme, width and keyboard changes preserve state and show no restricted data or unsupported feature Observation: Exercise `title`, `titleCredits` in `/titles/[id]`, capturing the current response/operation and each row’s visible heading, identity, controls, retained values and safe next action. Assert the named TitleMetadata / Artwork / CreditList through actual visible layout and accessible names/status rather than hidden fixture text. Run each row independently in both themes at declared widths; verify keyboard/focus and permitted navigation without leaking protected data. Source fragments identify design references, not implementation selectors. \| State reference \| Actor / independent trigger \| Visible semantic oracle \| \| --- \| --- \| --- \| \| movie \| Reader; open `/titles/[id]` with active M-A and its `titleCredits` \| Show movie label, synopsis, genres, honest release precision, known runtime, original title, provenance and visible cast/creator credits; preserve synopsis language and offer catalog/person navigation. \| \| tv \| Reader; open active TV-C via `title` and `titleCredits` \| Show TV series, first-air precision, series status, honest end context and episode-runtime label with explicit unknowns. No episode tracking or TV management controls appear. \| \| unavailable \| Reader; archive/delete the listed title before its detail read \| Show This title is unavailable and Return to catalog. Expose no archived values and never substitute a same-name title. \| \| identities \| Reader; read two active titles with equal display names and distinct IDs \| Show separate records, release context and source-scoped identities with links to the selected IDs. Name equality cannot merge records or redirect to another work. |
| [ATDD-303](../../../05-atdd.md#case-atdd-303-keep-only-the-latest-search-generation-visible) / Component | ControlledDOM/fetch: Light then Map, 250 ms debounce, Map resolves first; obsolete Light success/failure; history filter change and cursor reset. | Then only current-generation full criteria and Map result/error become visible And changed filters reset cursor and history restores complete criteria Observation: Inspect DOM/controller request criteria/focus with controlled responses; actual Catalog results and browser/network cancellation remain complementary end-to-end proof. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-256-T1-R01 · L10635](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | catalog; Visitor/Viewer/Maintainer; complete `titles` with active F-CATALOG data; Show the fixed discovery headline/promise, search, type/genre/year/month controls and active title cards with identity links and truthful result count. Keep same-name titles separate; browse details or change criteria. |
| [ATDD-256-T1-R02 · L10636](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | catalog-loading; Reader; hold the current `titles` response pending; Show Finding titles for the current criteria, withhold obsolete results and announce loading; allow criteria changes without presenting stale matches. |
| [ATDD-256-T1-R03 · L10637](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | catalog-empty; Reader; return a successful `titles` response from an empty active catalog; Show The catalog is empty and a browse-again action; retain criteria and distinguish an empty installation from failure or filtered no-match. |
| [ATDD-256-T1-R04 · L10638](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | catalog-failure; Reader; fail the current `titles` read; Show The catalog could not load, retained query/filters and retry of those criteria; show neither invented empty data nor older results as current. |
| [ATDD-256-T1-R05 · L10639](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | catalog-outdated; Reader; change criteria before the prior `titles` response arrives; Withhold the late result; show refresh/loading feedback for the current generation. Only its response can fill the grid; retry uses current criteria. |
| [ATDD-256-T1-R06 · L10640](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | catalog-invalid; Reader; enter query length above effective maximum or invalid filter parameters; Retain complete invalid input and other filters; identify correction and issue no truncated query. Old results cannot appear to match it; move focus to the affected control through the error link. |
| [ATDD-256-T1-R07 · L10641](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | catalog-no-match; Reader; return successful empty `titles` for valid restrictive filters while other active titles exist; Show No matching titles with editable retained filters. Clearing text removes only text; changing one filter preserves the rest. |
| [ATDD-257-T1-R01 · L10683](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback) | movie; Reader; open `/titles/[id]` with active M-A and its `titleCredits`; Show movie label, synopsis, genres, honest release precision, known runtime, original title, provenance and visible cast/creator credits; preserve synopsis language and offer catalog/person navigation. |
| [ATDD-257-T1-R02 · L10684](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback) | tv; Reader; open active TV-C via `title` and `titleCredits`; Show TV series, first-air precision, series status, honest end context and episode-runtime label with explicit unknowns. No episode tracking or TV management controls appear. |
| [ATDD-257-T1-R03 · L10685](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback) | unavailable; Reader; archive/delete the listed title before its detail read; Show This title is unavailable and Return to catalog. Expose no archived values and never substitute a same-name title. |
| [ATDD-257-T1-R04 · L10686](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback) | identities; Reader; read two active titles with equal display names and distinct IDs; Show separate records, release context and source-scoped identities with links to the selected IDs. Name equality cannot merge records or redirect to another work. |

Partial implementation contributions: [ATDD-029](../../../05-atdd.md#case-atdd-029-seed-metadata). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T013` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T013` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
| [discovery / catalog](../../../design-system/mockup/discovery.html#catalog) · [ATDD-256-T1-R01 · L10635](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | Visitor/Viewer/Maintainer; complete `titles` with active F-CATALOG data; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show the fixed discovery headline/promise, search, type/genre/year/month controls and active title cards with identity links and truthful result count. Keep same-name titles separate; browse details or change criteria. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [discovery / catalog-loading](../../../design-system/mockup/discovery.html#catalog-loading) · [ATDD-256-T1-R02 · L10636](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | Reader; hold the current `titles` response pending; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Finding titles for the current criteria, withhold obsolete results and announce loading; allow criteria changes without presenting stale matches. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [discovery / catalog-empty](../../../design-system/mockup/discovery.html#catalog-empty) · [ATDD-256-T1-R03 · L10637](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | Reader; return a successful `titles` response from an empty active catalog; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show The catalog is empty and a browse-again action; retain criteria and distinguish an empty installation from failure or filtered no-match. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [discovery / catalog-failure](../../../design-system/mockup/discovery.html#catalog-failure) · [ATDD-256-T1-R04 · L10638](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | Reader; fail the current `titles` read; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show The catalog could not load, retained query/filters and retry of those criteria; show neither invented empty data nor older results as current. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [discovery / catalog-outdated](../../../design-system/mockup/discovery.html#catalog-outdated) · [ATDD-256-T1-R05 · L10639](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | Reader; change criteria before the prior `titles` response arrives; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Withhold the late result; show refresh/loading feedback for the current generation. Only its response can fill the grid; retry uses current criteria. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [discovery / catalog-invalid](../../../design-system/mockup/discovery.html#catalog-invalid) · [ATDD-256-T1-R06 · L10640](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | Reader; enter query length above effective maximum or invalid filter parameters; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Retain complete invalid input and other filters; identify correction and issue no truncated query. Old results cannot appear to match it; move focus to the affected control through the error link. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [discovery / catalog-no-match](../../../design-system/mockup/discovery.html#catalog-no-match) · [ATDD-256-T1-R07 · L10641](../../../05-atdd.md#case-atdd-256-inspect-discovery-state-transitions-and-feedback) | Reader; return successful empty `titles` for valid restrictive filters while other active titles exist; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show No matching titles with editable retained filters. Clearing text removes only text; changing one filter preserves the rest. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [details / movie](../../../design-system/mockup/details.html#movie) · [ATDD-257-T1-R01 · L10683](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback) | Reader; open `/titles/[id]` with active M-A and its `titleCredits`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show movie label, synopsis, genres, honest release precision, known runtime, original title, provenance and visible cast/creator credits; preserve synopsis language and offer catalog/person navigation. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [details / tv](../../../design-system/mockup/details.html#tv) · [ATDD-257-T1-R02 · L10684](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback) | Reader; open active TV-C via `title` and `titleCredits`; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show TV series, first-air precision, series status, honest end context and episode-runtime label with explicit unknowns. No episode tracking or TV management controls appear. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [details / unavailable](../../../design-system/mockup/details.html#unavailable) · [ATDD-257-T1-R03 · L10685](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback) | Reader; archive/delete the listed title before its detail read; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show This title is unavailable and Return to catalog. Expose no archived values and never substitute a same-name title. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [details / identities](../../../design-system/mockup/details.html#identities) · [ATDD-257-T1-R04 · L10686](../../../05-atdd.md#case-atdd-257-inspect-details-state-transitions-and-feedback) | Reader; read two active titles with equal display names and distinct IDs; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show separate records, release context and source-scoped identities with links to the selected IDs. Name equality cannot merge records or redirect to another work. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

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
