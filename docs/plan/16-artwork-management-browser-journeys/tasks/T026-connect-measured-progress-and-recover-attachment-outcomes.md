---
kind: task
id: T026
group: G16
depends_on:
- T004
- T010
- T017
- T022
- T023
- T024
readiness: Ready
status: Implementation complete
risk: high
test_strategy: test-first
visual_review: required
---

# T026 — Connect measured progress and recover attachment outcomes

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Progress: observed byte events drive transfer only; processing remains visible until actual attachment and current movie image agree. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-064 | verifies |
| ATDD-038 | both |
| ATDD-039 | both |
| ATDD-040 | both |
| ATDD-041 | both |
| ATDD-042 | both |
| ATDD-043 | both |
| ATDD-044 | both |
| ATDD-045 | both |
| ATDD-046 | both |
| ATDD-123 | both |
| ATDD-124 | both |
| ATDD-125 | both |
| ATDD-126 | both |
| ATDD-159 | verifies |
| ATDD-160 | verifies |
| ATDD-161 | verifies |
| ATDD-162 | verifies |
| ATDD-163 | verifies |
| ATDD-169 | verifies |
| ATDD-170 | both |
| ATDD-263 | both |
| ATDD-263-T1-R01 | both |
| ATDD-263-T1-R02 | both |
| ATDD-263-T1-R03 | both |
| ATDD-263-T1-R04 | both |
| ATDD-263-T1-R05 | both |
| ATDD-263-T1-R06 | both |
| ATDD-263-T1-R07 | both |
| ATDD-263-T1-R08 | both |
| ATDD-263-T1-R09 | both |
| ATDD-263-T1-R10 | both |
| ATDD-315 | both |
| ATDD-305 | both |
| J-04 | both |
| FR-002 | both |
| FR-009 | both |
| FR-010 | both |
| FR-017 | both |
| NFR-001 | both |
| NFR-002 | both |
| NFR-005 | both |
| NFR-007 | both |
| NFR-008 | both |
| NFR-013 | both |
| NFR-016 | both |
| A03 | both |
| A04 | both |
| A05 | both |
| A08 | both |
| A14 | both |
| D03 | both |
| D10 | both |
| D11 | both |
| D19 | both |
| AC-038 | both |
| AC-038-C01 | both |
| AC-038-C02 | both |
| AC-039 | both |
| AC-039-C01 | both |
| AC-039-C02 | both |
| AC-039-C03 | both |
| AC-040 | both |
| AC-040-C01 | both |
| AC-041 | both |
| AC-041-C01 | both |
| AC-041-C02 | both |
| AC-042 | both |
| AC-042-C01 | both |
| AC-043 | both |
| AC-043-C01 | both |
| AC-043-C02 | both |
| AC-044 | both |
| AC-044-C01 | both |
| AC-044-C02 | both |
| AC-045 | both |
| AC-045-C01 | both |
| AC-045-C02 | both |
| AC-046 | both |
| AC-046-C01 | both |
| AC-068 | both |
| AC-068-C01 | both |
| AC-147 | both |
| AC-147-C01 | both |
| AC-147-C02 | both |
| AC-147-C03 | both |
| AC-149 | both |
| AC-149-C01 | both |
| AC-149-C02 | both |
| AC-149-C03 | both |
| AC-150 | both |
| AC-150-C01 | both |
| AC-150-C02 | both |
| AC-150-C03 | both |
| AC-151 | both |
| AC-151-C01 | both |
| AC-151-C02 | both |
| AC-151-C03 | both |
| AC-152 | both |
| AC-152-C01 | both |
| AC-152-C02 | both |
| AC-152-C03 | both |
| AC-157 | both |
| AC-157-C01 | both |
| AC-157-C02 | both |
| AC-065 | both |
| AC-066 | both |
| AC-087 | both |
| AC-103 | both |
| G-02 | both |
| G-04 | both |
| G-05 | both |
| OD-007 | both |
| OD-019 | both |
| US-001 | both |
| US-007 | both |
| US-011 | both |
| US-021 | both |
| PRD-PERM-03 | both |
| PRD-POLICY-06 | both |
| PRD-POLICY-13 | both |
| PRD-POLICY-27 | both |
| PRD-POLICY-28 | both |
| PRD-POLICY-29 | both |
| PRD-POLICY-30 | both |
| PRD-POLICY-31 | both |
| PRD-POLICY-32 | both |
| PRD-FEEDBACK-02 | both |
| PRD-CONCEPT-01 | both |
| PRD-CONCEPT-03 | both |
| PRD-HANDOFF-01 | both |
| PRD-HANDOFF-02 | both |
| PRD-HANDOFF-08 | both |
| PRD-HANDOFF-13 | both |
| UI-artwork-artwork | both |
| UI-artwork-artwork-transfer | both |
| UI-artwork-artwork-processing | both |
| UI-artwork-artwork-attached | both |
| UI-artwork-artwork-cancelled | both |
| UI-artwork-artwork-invalid | both |
| UI-artwork-artwork-failed | both |
| UI-artwork-artwork-target | both |
| UI-artwork-artwork-uncertain | both |
| UI-artwork-artwork-policy | both |
| DS-POLICY-03 | implements |
| DS-POLICY-05 | implements |
| DS-POLICY-09 | implements |
| DS-POLICY-10 | implements |
| DS-POLICY-14 | implements |
| DS-POLICY-15 | implements |
| DS-POLICY-18 | implements |
| TD-C-011 | both |
| TD-BROWSER-CACHE | both |
| V-ART | both |
| TD-ROW-L3568 | both |
| TD-ROW-L3600 | both |
| TD-ROW-L3603 | both |
| TD-ROW-L3627 | both |
| TD-ROW-L3629 | both |
| TD-ROW-L3630 | both |
| TD-ROW-L5185 | both |
| TD-ROW-L5514 | both |
| TD-ROW-L5527 | both |
| TD-BLOCK-L4832 | both |
| TD-BLOCK-L5044 | both |
| BRAIN-A03 | both |
| BRAIN-A04 | both |
| BRAIN-A05 | both |
| BRAIN-A08 | both |
| BRAIN-A14 | both |
| BRAIN-D03 | both |
| BRAIN-D10 | both |
| BRAIN-D11 | both |
| BRAIN-D19 | both |

## Prerequisites

- [T004 — Build the shared themed and accessible shell](../../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- [T010 — Connect account and session browser journeys](../../08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Access: Visitor/Viewer/Maintainer see the permitted navigation and direct protected routes enforce current access; Viewer cannot register or choose a management role.
- [T017 — Connect movie editors and confirmed outcomes](../../12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Editor: valid create/update displays committed values under the same ID; optional omissions stay unknown and retained invalid fields remain fully correctable.
- [T022 — Accept and process bounded artwork streams](../../14-artwork-management-processing-and-attachment/tasks/T022-accept-and-process-bounded-artwork-streams.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing.
- [T023 — Coordinate protective holds, attachment and cleanup](../../14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.
- [T024 — Implement current-impact archive, restore and deletion](../../15-archive-management/tasks/T024-implement-current-impact-archive-restore-and-deletion.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Lifecycle: active record allows archive only, archived record allows restore or separately confirmed delete, and cancel changes nothing.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | frontend/src/lib/features/artwork/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/artwork/ | T026 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.5 Artwork and operation feedback](../../../04-spoolin-techdoc.md#35-artwork-and-operation-feedback): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.1.1 UI reference and implementation map](../../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.6 Artwork transfer and reference-safe attachment](../../../04-spoolin-techdoc.md#76-artwork-transfer-and-reference-safe-attachment): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [J-04 · L90](../../../02-PRD.md#32-application-journeys) | Maintainer uploads/replaces artwork \| Active movie exists; authorized Maintainer selects a file \| See transfer, processing, and confirmed attachment; preserve existing artwork on replacement failure |
| [FR-002 · L236](../../../02-PRD.md#61-functional-requirements) | Search movie titles and cast/creator names with the approved matching and visibility rules \| A11/A14 Required; product policy Committed (section 13.1) \| AC-006 through AC-010, AC-022, AC-134, AC-147, AC-159 |
| [FR-009 · L243](../../../02-PRD.md#61-functional-requirements) | Upload movie artwork with validation and distinct transfer/processing/saved feedback \| A08/A14 Required \| AC-038 through AC-042, AC-149 through AC-151 |
| [FR-010 · L244](../../../02-PRD.md#61-functional-requirements) | Recover failed/uncertain artwork changes while protecting prior and still-used artwork \| D20/D21 Committed; product policy Committed (section 13.1); D11 mechanics Open \| AC-043 through AC-046, AC-152, AC-155, AC-157 |
| [FR-017 · L251](../../../02-PRD.md#61-functional-requirements) | Submit permitted inputs within visible, configurable bounds; correct invalid values without partial saves or lost drafts \| Committed input policy (section 5.5); non-name numerical defaults Recommended \| AC-139 through AC-146, AC-148 through AC-150, AC-153 through AC-159 |
| [NFR-001 · L259](../../../02-PRD.md#62-quality-and-delivery-requirements) | Required project stack and real frontend/backend protocols \| A01 through A05, D03 Required \| AC-064 through AC-066; section 10 |
| [NFR-002 · L260](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preferred App/Catalog/Media ownership with unrelated catalog work usable during artwork outage \| D02/D10 Working design \| AC-067, AC-068; section 10 |
| [NFR-005 · L263](../../../02-PRD.md#62-quality-and-delivery-requirements) | Setup rejects unusable configuration and protects secrets \| D13/D21; configuration and secret protection Committed \| AC-076 through AC-078, AC-153 through AC-155, AC-157 |
| [NFR-007 · L265](../../../02-PRD.md#62-quality-and-delivery-requirements) | Responsive, keyboard-usable journeys with accurate feedback and retained recoverable work \| A13/A14 Required; keyboard use Committed \| AC-083 through AC-085, AC-139, AC-147, AC-159 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-013 · L271](../../../02-PRD.md#62-quality-and-delivery-requirements) | Interfaces implement the product's validation, optional data, access, and failure behavior \| A04/A05/A14 Required support; detailed contracts Open \| AC-100 through AC-103, AC-142/AC-143, AC-148 |
| [NFR-016 · L274](../../../02-PRD.md#62-quality-and-delivery-requirements) | Treat entered text and artwork as untrusted content; enforce the effective policy without unsafe execution, bypass, or silent data loss \| Committed input policy (section 5.5); mechanisms assigned to techdoc \| AC-142/AC-143, AC-147/AC-148, AC-151 through AC-158 |
| [A03 · L282](../../../02-PRD.md#63-project-requirements-coverage) | At least two meaningful Spring Boot services \| NFR-001 / AC-065 |
| [A04 · L283](../../../02-PRD.md#63-project-requirements-coverage) | Real service-to-service gRPC \| NFR-001/NFR-008 / AC-065, AC-087 |
| [A05 · L284](../../../02-PRD.md#63-project-requirements-coverage) | Frontend application communication through GraphQL \| NFR-001/NFR-013 / AC-066, AC-100 |
| [A08 · L287](../../../02-PRD.md#63-project-requirements-coverage) | Upload movie artwork \| FR-009/FR-010 / AC-038 through AC-046 |
| [A14 · L293](../../../02-PRD.md#63-project-requirements-coverage) | Sufficient operation feedback \| NFR-007; FR-001/FR-002/FR-004/FR-009 / AC-002, AC-009, AC-018, AC-038, AC-085 |
| [D03 · L304](../../../02-PRD.md#64-project-decision-register) | GraphQL frontend and gRPC backend boundaries \| NFR-001/NFR-013 / AC-065, AC-066, AC-100 |
| [D10 · L311](../../../02-PRD.md#64-project-decision-register) | Catalog owns titles/people/credits, Media owns assets \| NFR-002 / AC-067, AC-068 |
| [D11 · L312](../../../02-PRD.md#64-project-decision-register) | Artwork transport and lifecycle protocol \| Product outcomes settled; mechanics Open in techdoc. FR-009/FR-010 / AC-038 through AC-046 |
| [D19 · L320](../../../02-PRD.md#64-project-decision-register) | Unit/integration, real gRPC and selected database checks \| NFR-008 / AC-086 through AC-088 |
| [AC-038 · L419](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Progress \| A Maintainer selects a supported file \| Transfer and processing run \| Show measured transfer progress, separate processing, then saved only after the movie displays the confirmed attachment. Transfer at 100% is not a saved result. |
| [AC-038-C01 · L419](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Show measured transfer progress, separate processing, then saved only after the movie displays the confirmed attachment. |
| [AC-038-C02 · L419](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Transfer at 100% is not a saved result. |
| [AC-039 · L420](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Invalid file \| Content is empty, malformed, animated, disguised, unsupported, unsafe, or outside displayed limits \| Upload is attempted \| Explain the applicable format/size/content/processing-limit issue and attach nothing; preserve previous artwork. Enforce section 5.5 against actual content, not filename or declared type alone. The techdoc defines validation mechanisms and processing limits. |
| [AC-039-C01 · L420](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Explain the applicable format/size/content/processing-limit issue and attach nothing; preserve previous artwork. |
| [AC-039-C02 · L420](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Enforce section 5.5 against actual content, not filename or declared type alone. |
| [AC-039-C03 · L420](../../../02-PRD.md#us-007-upload-and-recover-artwork) | The techdoc defines validation mechanisms and processing limits. |
| [AC-040 · L421](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Permission/target changes \| Permission expires or the movie is archived/deleted during upload \| Attachment is attempted \| Do not attach to an unauthorized/unavailable target; explain the outcome and preserve the previous valid artwork. |
| [AC-040-C01 · L421](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Do not attach to an unauthorized/unavailable target; explain the outcome and preserve the previous valid artwork. |
| [AC-041 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Interrupted upload \| Transfer is cancelled/interrupted or storage fails \| The operation ends \| Show cancellation/failure without claiming success, preserve the previous artwork, and allow a fresh attempt once the outcome is known. Resume-from-byte support is not required. |
| [AC-041-C01 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Show cancellation/failure without claiming success, preserve the previous artwork, and allow a fresh attempt once the outcome is known. |
| [AC-041-C02 · L422](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Resume-from-byte support is not required. |
| [AC-042 · L423](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Repeated upload \| A completed/pending upload is repeated \| Requests are processed \| Do not create unintended duplicate attachments; show the known result or pending outcome instead of treating each repeat as a new success. |
| [AC-042-C01 · L423](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Do not create unintended duplicate attachments; show the known result or pending outcome instead of treating each repeat as a new success. |
| [AC-043 · L424](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Upload succeeded, attachment failed \| File transfer/processing completed but the movie was not updated \| The Maintainer sees the result \| Clearly report that artwork was not attached; keep prior artwork, and offer a safe retry after confirming the failed outcome. No unattached image becomes public. |
| [AC-043-C01 · L424](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Clearly report that artwork was not attached; keep prior artwork, and offer a safe retry after confirming the failed outcome. |
| [AC-043-C02 · L424](../../../02-PRD.md#us-007-upload-and-recover-artwork) | No unattached image becomes public. |
| [AC-044 · L425](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Replacement \| A movie already has valid artwork \| Replacement succeeds or fails \| Success displays the new confirmed image; failure preserves the previous image. Later cleanup cannot remove another title's valid artwork. |
| [AC-044-C01 · L425](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Success displays the new confirmed image; failure preserves the previous image. |
| [AC-044-C02 · L425](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Later cleanup cannot remove another title's valid artwork. |
| [AC-045 · L426](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Uncertain attachment \| Confirmation is lost after submission \| The Maintainer checks status \| Show uncertainty while resolving the result; display success or a recoverable failure only when known. Do not ask for a blind repeat or delete possibly attached artwork. |
| [AC-045-C01 · L426](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Show uncertainty while resolving the result; display success or a recoverable failure only when known. |
| [AC-045-C02 · L426](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Do not ask for a blind repeat or delete possibly attached artwork. |
| [AC-046 · L427](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Interrupted cleanup \| Cleanup/recovery overlaps another valid use or a restart \| Work resumes \| Existing attachments stay usable; recovery does not remove referenced artwork or report missing output as completed. |
| [AC-046-C01 · L427](../../../02-PRD.md#us-007-upload-and-recover-artwork) | Existing attachments stay usable; recovery does not remove referenced artwork or report missing output as completed. |
| [AC-068 · L465](../../../02-PRD.md#us-011-continue-metadata-work-during-an-artwork-outage) | Artwork outage \| Artwork handling is unavailable but catalog work is available \| A Maintainer edits movie metadata \| Unrelated metadata work can succeed; image-dependent work explains its failure without claiming attachment success. |
| [AC-068-C01 · L465](../../../02-PRD.md#us-011-continue-metadata-work-during-an-artwork-outage) | Unrelated metadata work can succeed; image-dependent work explains its failure without claiming attachment success. |
| [AC-147 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Plain-text safety \| Entered catalog text, names, search text, or changed values contain markup, apostrophes, or instruction-like content within applicable limits \| They appear in forms, catalog/detail pages, search results, previews, or audit history \| Treat the content as text and execute no embedded code or instructions. Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. Private registration data retain their access boundaries. |
| [AC-147-C01 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Treat the content as text and execute no embedded code or instructions. |
| [AC-147-C02 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Legitimate punctuation remains intact; content cannot alter another record, grant access, or reveal protected information. |
| [AC-147-C03 · L537](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Private registration data retain their access boundaries. |
| [AC-149 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Artwork byte boundaries \| Recommended file bounds of 1 to 5,242,880 bytes are configured \| A Maintainer submits an empty file, a one-byte invalid file, a valid supported image exactly at the maximum, or a file one byte above it \| Reject empty/undecodable content and the oversized file with the specific reason; the exact maximum passes the size check subject to all content/processing checks. Attach no rejected image and preserve previous artwork. MiB is displayed with its exact byte interpretation. |
| [AC-149-C01 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject empty/undecodable content and the oversized file with the specific reason; the exact maximum passes the size check subject to all content/processing checks. |
| [AC-149-C02 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Attach no rejected image and preserve previous artwork. |
| [AC-149-C03 · L539](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | MiB is displayed with its exact byte interpretation. |
| [AC-150 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Static supported formats \| JPEG, PNG, and WebP are enabled and an active movie may lack artwork \| A Maintainer uploads valid static examples or attempts GIF, SVG, animated PNG/WebP, or another unsupported format \| Supported static files proceed through upload/processing to confirmed attachment if other checks pass. Reject unsupported or animated content with correction feedback and no attachment; do not silently convert animation into an accepted static upload. Missing artwork remains a placeholder on failure. |
| [AC-150-C01 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Supported static files proceed through upload/processing to confirmed attachment if other checks pass. |
| [AC-150-C02 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject unsupported or animated content with correction feedback and no attachment; do not silently convert animation into an accepted static upload. |
| [AC-150-C03 · L540](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Missing artwork remains a placeholder on failure. |
| [AC-151 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Disguised, malformed, or unsafe image \| A file has a permitted extension or declared type but invalid actual content, or a small compressed image exceeds the adopted processing bounds \| Artwork is validated or processed \| Reject it without attachment or unbounded processing; preserve prior artwork and report a safe content/processing-limit failure. Filename, declared type, or signature alone cannot bypass validation. Numerical processing bounds are established under OD-007/OD-019 before implementation. |
| [AC-151-C01 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Reject it without attachment or unbounded processing; preserve prior artwork and report a safe content/processing-limit failure. |
| [AC-151-C02 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Filename, declared type, or signature alone cannot bypass validation. |
| [AC-151-C03 · L541](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Numerical processing bounds are established under OD-007/OD-019 before implementation. |
| [AC-152 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Replacement failure and correction \| A movie has valid artwork and a replacement is rejected or fails processing/attachment \| The Maintainer inspects the result and selects a corrected image after the failed outcome is known \| Existing artwork and unrelated movie information remain available; show the failure and effective rules, then allow a safe fresh attempt. Confirm a replacement only after attachment. Uncertain outcomes still require reconciliation under AC-045 before repeat submission. |
| [AC-152-C01 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Existing artwork and unrelated movie information remain available; show the failure and effective rules, then allow a safe fresh attempt. |
| [AC-152-C02 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Confirm a replacement only after attachment. |
| [AC-152-C03 · L542](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Uncertain outcomes still require reconciliation under AC-045 before repeat submission. |
| [AC-157 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Policy changes during a draft/upload \| Input or artwork met earlier limits, and the policy changes before submission/attachment \| The operation is evaluated against the effective policy \| If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-157-C01 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | If changed/new input no longer qualifies, explain the current rule, preserve permitted draft content and prior artwork, and offer correction or a safe retry. |
| [AC-157-C02 · L547](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | Do not silently shorten input, partially save, bypass stale-edit review or AI approval, or label an uncertain committed outcome as a confirmed failure. |
| [AC-065 · L856](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise meaningful real gRPC features across at least two running Spring Boot applications \| NFR-001; technical design |
| [AC-066 · L857](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify frontend GraphQL and documented upload/retrieval/auth exceptions against A05; reconcile the MCP boundary \| NFR-001/NFR-013; technical design |
| [AC-087 · L870](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Exercise GraphQL contracts, real gRPC, selected database, security/audit, native SQL where used, seed, artwork, recovery, and notification integration \| NFR-008; technical design |
| [AC-103 · L879](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify effective input/file policies from section 5.5; establish remaining request/query/page and decoded-image resource bounds with understandable outcomes \| NFR-013/NFR-016; technical design |
| [G-02 · L923](../../../02-PRD.md#122-release-gates-remain-planned) | Mandated stack/protocols and chosen integration approach work \| NFR-001/NFR-002/NFR-013; AC-064 through AC-067, AC-100 through AC-103 \| Planned in techdoc |
| [G-04 · L925](../../../02-PRD.md#122-release-gates-remain-planned) | Upload, replacement, interrupted work, safe retries, exhaustion, and configured notification work \| FR-009/FR-010/FR-014/FR-015/FR-017; NFR-004/NFR-016; AC-149 through AC-152, AC-155/AC-157 \| Planned |
| [G-05 · L926](../../../02-PRD.md#122-release-gates-remain-planned) | Role enforcement, registration, private movie/people archives, audit UI, private-data protection/rotation, and monitoring/alerts work \| FR-011 through FR-013, FR-017; NFR-005/NFR-006/NFR-012/NFR-015/NFR-016; AC-133/AC-134, AC-139/AC-141/AC-142, AC-145 through AC-148, AC-153 through AC-155 \| Planned; functionality-first does not waive it |
| [OD-007 · L1045](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Upload UX, supported static formats, configurable size policy, and rejection/recovery outcomes fixed in section 5.5; adoption of Recommended byte bounds, transport/A05 interpretation, content checks, decoded-dimension/resource bounds, processing, storage/retrieval remain for techdoc \| FR-009/FR-010/FR-017, NFR-001/NFR-013/NFR-016 \| Technical design; before artwork implementation |
| [OD-019 · L1055](../../../02-PRD.md#132-engineering-and-other-next-phase-register) | Product discovery/feedback and section 5.5 input/artwork policy settled; GraphQL/gRPC and MCP contracts, human-approval enforcement, consistent counting/validation, safe display/data handling, compatibility, remaining request/query/page and decoding resource bounds, ordering ties/navigation, responsive support matrix, and accessibility verification remain for techdoc \| NFR-007/NFR-013/NFR-016, FR-016/FR-017 \| Technical design; before dependent implementation |
| [US-001 · L333](../../../02-PRD.md#us-001-browse-titles-and-people) | ### US-001: Browse titles and people As a visitor or Viewer, I want to inspect titles, artwork, and credits so I can identify the work and its people. Requirements: FR-001, NFR-007. Reading needs no account and changes no catalog information. |
| [US-007 · L413](../../../02-PRD.md#us-007-upload-and-recover-artwork) | ### US-007: Upload and recover artwork As a Maintainer, I want to know which image a movie actually uses and recover from failed uploads. Requirements: FR-009, FR-010. Artwork management applies to active movies; current permissions are required throughout the operation. |
| [US-011 · L459](../../../02-PRD.md#us-011-continue-metadata-work-during-an-artwork-outage) | ### US-011: Continue metadata work during an artwork outage As a Maintainer, I want to edit movie metadata when artwork handling is unavailable so I can continue work that does not need an image change. Requirement: NFR-002. Related project technology constraints NFR-001 and AC-064 through AC-067 remain in section 10. |
| [US-021 · L523](../../../02-PRD.md#us-021-submit-accepted-input-and-correct-validation-failures) | ### US-021: Submit accepted input and correct validation failures As a Visitor registering or searching, or a Maintainer entering catalog information or artwork, I want visible limits and specific correction feedback so I can complete a permitted action without losing valid work. Requirements: FR-017, NFR-016, NFR-005, NFR-007; related capabilities FR-002/FR-004/FR-005/FR-006/FR-009/FR-010/FR-011/FR-016. Section 5.5 defines the policies and their statuses. Selected name defaults are Committed; other numerical defaults remain Recommended. Scenarios using recommended numbers assume those values have been configured. Configuration verification concerns restricted deployment facilities, not another application role or settings screen. |
| [PRD-PERM-03 · L70](../../../02-PRD.md#31-permission-matrix) | Create/update movies and manage their credits/artwork \| No \| No \| Yes |
| [PRD-POLICY-06 · L126](../../../02-PRD.md#52-removal-and-editing-rules) | Archiving hides a movie from public/Viewer listings, search, filters, direct detail access, and public credit views. It preserves the movie's remaining metadata, artwork, and credits in the Maintainer-only archive. A Maintainer may restore it or permanently delete it with a separate confirmation that identifies the movie and irreversibility. There is no direct permanent-delete action for active movies and no restore after permanent deletion. Archive editing is limited to restore/permanent delete; restore first to edit movie content. |
| [PRD-POLICY-13 · L140](../../../02-PRD.md#52-removal-and-editing-rules) | A failed artwork replacement keeps the current valid image. A confirmed replacement changes the visible image; any later cleanup must not undo it. Transfer completion alone is not attachment success. Unattached or uncertain artwork remains outside public display until resolved. |
| [PRD-POLICY-27 · L204](../../../02-PRD.md#artwork-policy) | Artwork upload and replacement are Maintainer capabilities for active movies. Existing progress, permissions, attachment, and recovery rules remain binding. |
| [PRD-POLICY-28 · L213](../../../02-PRD.md#artwork-policy) | The maximum is an app recommendation informed by Supabase's standard-upload guidance, which recommends that method for files up to 6 MB. The recommendation does not select a storage provider or upload transport, and it is not a universal poster standard. A one-byte minimum merely rejects empty content; every accepted file must also be a valid, decodable supported image. |
| [PRD-POLICY-29 · L215](../../../02-PRD.md#artwork-policy) | File-size bounds are configurable in bytes and inclusive. Display the effective size limits and enabled formats before file selection. Validate actual content and decodability; an extension, filename, declared content type, or signature alone cannot establish validity. An unsupported, animated, malformed, disguised, or unsafe file attaches nothing. Explain the applicable format, size, content, or processing-limit issue and let the Maintainer select a corrected file. A failed or rejected replacement preserves current artwork and movie information. Distinguish uploading, processing, saved, failed, and uncertain outcomes; transfer completion does not bypass processing or confirm attachment. |
| [PRD-POLICY-30 · L217](../../../02-PRD.md#artwork-policy) | Require bounded image processing even when compressed file size is within its limit. Numerical limits on decoded dimensions and resource consumption, content-handling safeguards, and image-processing mechanisms remain OD-007/OD-019 technical-design matters before artwork implementation. OWASP file-upload guidance supports content checks and processing safeguards; a byte limit alone does not establish upload safety. |
| [PRD-POLICY-31 · L221](../../../02-PRD.md#configuration-and-existing-content) | Configuration must supply finite whole-number bounds in the appropriate units, with nonnegative text bounds, positive file-size bounds, and minimum no greater than maximum. A required field cannot have a zero effective minimum. Enabled artwork formats must be a nonempty subset of the supported static formats. An unset setting follows its deliberately documented default; an explicitly empty, invalid, or unsupported setting is not treated as unset. Negative values, reversed bounds, and unsupported formats prevent affected functionality from becoming ready, with actionable redacted diagnostics and no silent unlimited or permissive fallback. OD-020 owns configuration keys, sources, precedence, application/reload timing, and distribution of the effective policy. |
| [PRD-POLICY-32 · L223](../../../02-PRD.md#configuration-and-existing-content) | Changes to limits do not automatically truncate stored content, remove artwork, or invalidate existing password credentials. Existing records and artwork remain readable under their permissions. An unchanged stored field may be retained while other fields are edited; newly entered or changed values must meet current limits. Explicitly clearing an optional field remains allowed. Existing credentials must not be rejected solely because a new-password minimum changed. This compatibility rule does not exempt malicious new submissions or permit unsupported new artwork. |
| [PRD-FEEDBACK-02 · L796](../../../02-PRD.md#810-shared-feedback-rules) | Empty / unknown \| Distinguish empty catalog, no search matches, no audit events, unknown optional metadata, and missing artwork |
| [PRD-CONCEPT-01 · L809](../../../02-PRD.md#9-conceptual-information-and-privacy) | Movie / TV series \| Stable identity, separate title type, display title, original title where different, descriptive metadata, optional artwork, and public credits. Names alone do not identify a work. |
| [PRD-CONCEPT-03 · L811](../../../02-PRD.md#9-conceptual-information-and-privacy) | Optional context \| Original language, production countries, movie runtime, TV status/end context where applicable and known, source-scoped external identifiers, and attributed artwork where available. TV duration means episode runtime. |
| [PRD-HANDOFF-01 · L834](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | A01 through A05; D03 \| Required \| Svelte/TypeScript, Kotlin/Spring Boot, at least two meaningful services, frontend application GraphQL, and real backend gRPC. Document any artwork/auth transport interpretation. |
| [PRD-HANDOFF-02 · L835](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D02/D10 \| Working design \| Preferred App/Catalog/Media ownership, domain authorization, separate data ownership, and unrelated catalog work independent of artwork availability. It is not an external three-service requirement. |
| [PRD-HANDOFF-08 · L841](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | D13 \| Committed \| Docker Compose and `make start`, `make stop`, `make restart`, `make status`, `make logs`, `make test-unit`, `make test-integration`, with documented command behavior, isolated tests, readiness checks, failing exits, and persistent data/artwork. |
| [PRD-HANDOFF-13 · L846](../../../02-PRD.md#101-constraints-to-carry-into-the-techdoc) | Input policy; FR-017/NFR-016 \| Committed behavior and name defaults; other numerical defaults Recommended \| Implement section 5.5 with authoritative validation across entry paths, Unicode-code-point counting, plain-text display, safe data handling, inclusive configurable limits, and compatibility for retained data. Establish adopted email/URL/password rules, remaining field/collection bounds, upload content checks and bounded decoding, configuration distribution/application timing, and meaningful boundary/security verification. Configuration/library/transport choices remain open; do not introduce extra UI or turn recommended values into external obligations. |
| [DS-POLICY-03 · L41](../../../03-design-system.md#identity-and-voice) | The product feels cinematic through artwork, restrained contrast, typography, and composition. Catalog discovery gives posters prominence; forms use calm reading surfaces and concise labels. Use sentence case, direct verbs, concrete record names, and honest unknowns. Say “Save result not confirmed” when uncertain, “Changes were not saved” only after confirmed failure, and “Changes saved” only after confirmation. Do not hide consequential feedback in disappearing toasts. |
| [DS-POLICY-05 · L66](../../../03-design-system.md#one-token-authority-two-themes) | Do not use accent as a generic success signal, warning as a confirmed failure, or selection as a completed save. Never place low-contrast supporting text on the accent fill. Do not show a red button as the sole explanation of deletion. Do not put text directly on arbitrary user artwork without an opaque or verified protective surface. Do not lighten critical text with opacity. Status uses words and, when useful, symbols in addition to color. |
| [DS-POLICY-09 · L76](../../../03-design-system.md#typography-layout-and-shape) | Controls use shared 8px corners, artwork 12px, identity/status pills fully rounded. Cards derive structure from content and alignment; avoid outlining every metadata fragment. Overlay elevation uses one restrained shadow; forms and catalog cards do not need decorative shadows. Controls target at least 44px touch height. Table cells wrap and preserve headers; phone layouts prioritize identifiable records and explicit actions over compactness. |
| [DS-POLICY-10 · L80](../../../03-design-system.md#artwork-icons-and-motion) | Posters use a 2:3 slot with cover cropping; the hero is a wide landscape frame. Do not stretch images. Display title/type/context beside artwork, so a missing image never erases navigation or identity. Decorative repeated posters use empty alternative text; unique narrative/context imagery has descriptive text. Placeholder text says “Artwork unavailable,” distinct from a catalog failure. |
| [DS-POLICY-14 · L90](../../../03-design-system.md#visible-interaction-contracts) | Forms pair labels, effective guidance, and associated field errors. Required ordinary text is trimmed for emptiness/counting; preserve legitimate internal spacing and synopsis line breaks. Count Unicode code points: `é` is one, `e` plus combining accent is two. Person defaults are 1–100; registration defaults 2–100. Other numerical examples remain Recommended: titles 1–255, synopsis 1–5,000, roles 1–100, each character name 1–200, search 0–200, email maximum 254, URL maximum 2,048, new passwords 15–128, artwork 1 byte–5 MiB. Exact HTTP/HTTPS URL syntax/credential policy and processing limits remain technical design. A source URL does not grant arbitrary fetch permission. |
| [DS-POLICY-15 · L92](../../../03-design-system.md#visible-interaction-contracts) | Optional omission preserves saved data; an explicit empty value clears it. Policy tightening must not truncate existing data, remove accepted artwork, or invalidate existing credentials. Validate newly changed values against the current policy, preserve unchanged fields, and explain newly invalid drafts. No configuration editing UI is added. Invalid policy blocks the affected operation safely. |
| [DS-POLICY-18 · L98](../../../03-design-system.md#visible-interaction-contracts) | Stale saves retain draft and current version for review; no force overwrite. Uncertainty is reconciled against the original operation before another write. Metadata work remains usable during artwork outage. Upload transfer, processing, attachment, failed replacement, cancellation, changed access/target, and uncertainty are separate states. Transfer 100% is not saved artwork. Existing art remains after replacement failure; later cleanup cannot remove another title’s valid attachment. |
| [TD-C-011 · L1005](../../../04-spoolin-techdoc.md#35-artwork-and-operation-feedback) | Artwork transfer, processing and attachment states are distinct. Poll original Catalog UUID 500ms doubling to 5s, stop foreground at five minutes; resume lookup. Cancellation has separate UUID and preserves race-winning committed attachment; release client files/object URLs. |
| [TD-BROWSER-CACHE · L994](../../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state) | TD-BROWSER-CACHE \| HTTP responses \| Every GraphQL response, including protocol/authentication/domain errors, uses `Cache-Control: no-store`; artwork retains its existing no-store policy; static HTML/CSS/JS assets retain their specified caching |
| [V-ART · L6390](../../../04-spoolin-techdoc.md#131-verification-families-and-database-specific-obligations) | V-ART \| Stream actual boundary files over GraphQL/base64→gRPC to isolated filesystem; measure transfer events, malformed/animated/bomb/oversize rejection, codec resource kill, policy/permission/target race, reserve/attach/finalize/cleanup crashes and stale worker; assert safe old/new selected bytes and no referenced deletion |
| [TD-ROW-L3568 · L3568](../../../04-spoolin-techdoc.md#613-storage-consolidation-register) | source_record / artwork_intent \| provenance_record / artwork_attachment_request \| Provenance tombstones and cross-service attachment outcomes |
| [TD-ROW-L3600 · L3600](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `artwork_attachment_request` \| Catalog `artwork/persistence` \| Attachment/cancellation outcome survives cross-service interruption |
| [TD-ROW-L3603 · L3603](../../../04-spoolin-techdoc.md#persistence-implementation-ownership) | `asset`, `asset_reference` \| Media `asset/persistence` and `hold/persistence`; `upload` maps embedded processing evidence \| One processing record per asset; many reference promises must remain independently protected |
| [TD-ROW-L3627 · L3627](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `artwork_attachment_request` \| Existing operation/attachment/cancellation projections \| Expose only declared phase/outcome fields; no arbitrary intent-row access |
| [TD-ROW-L3629 · L3629](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `asset` \| GraphQL Artwork; internal Asset/AssetReadHeader and stream \| Public identity/URL/dimensions; processing work/path/input hash remains private |
| [TD-ROW-L3630 · L3630](../../../04-spoolin-techdoc.md#persistence-to-wire-projection-map) | `asset_reference` \| Existing internal Hold/Attachment/ArtworkGrant tuple \| Allowlisted service calls only; reserved protocol method/type names remain unchanged |
| [TD-ROW-L5185 · L5185](../../../04-spoolin-techdoc.md#71-transaction-inventory) | Reserve/attach/release artwork \| Each local Media or Catalog transaction is atomic; ordered handshake below \| READ COMMITTED with asset locks and version predicates; compensating/reconciling work |
| [TD-ROW-L5514 · L5514](../../../04-spoolin-techdoc.md#82-threat-model-and-assurance-records) | R-12 lifecycle race \| Stale delete removes unexpected credits/artwork \| Fresh impact hash under lock, versions, protective holds \| Concurrent credit/title changes/delete/attach/cleanup; serialized local writes trade throughput; Catalog/Media |
| [TD-ROW-L5527 · L5527](../../../04-spoolin-techdoc.md#83-api-security-enforcement) | Artwork HTTP / Core and Media \| Validated UUIDs, current Catalog visibility/attachment grant, READ quota, bounded stream/type/bytes \| UUID-selected private paths; processed JPEG/PNG/WebP only, nosniff/no-store; cancel closes streams, existing hold protocol protects references \| Traversal/hidden/stale asset and MIME attacks yield safe denial/failure; markup cannot be served as executable artwork |
| [TD-BLOCK-L4832 · L4832](../../../04-spoolin-techdoc.md#artwork_attachment_request-ddl) | artwork_attachment_request DDL — complete sql contract, all members, constraints and field numbers |
| [TD-BLOCK-L5044 · L5044](../../../04-spoolin-techdoc.md#asset-ddl) | asset DDL — complete sql contract, all members, constraints and field numbers |
| [BRAIN-A03 · L48](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A03 \| At least two Spring Boot services \| Distinct running services with meaningful responsibilities |
| [BRAIN-A04 · L49](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A04 \| Service-to-service communication via gRPC \| Real RPC calls supporting application features; protobuf contracts and integration checks |
| [BRAIN-A05 · L50](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A05 \| Frontend-to-backend communication via GraphQL \| Queries/mutations implement application operations; file/auth transport details documented |
| [BRAIN-A08 · L53](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A08 \| Upload movie artwork \| Valid upload, progress, processing/success feedback, and failure handling |
| [BRAIN-A14 · L59](../../../01-brainstorming-result.md#3-project-requirements-and-acceptance-baseline) | A14 \| Sufficient feedback for upload, listing, updating, and searching \| Loading, success, failure, validation, and empty states integrated into features |
| [BRAIN-D03 · L72](../../../01-brainstorming-result.md#4-decision-register) | D03 \| Required \| GraphQL for frontend application operations; gRPC between backend services |
| [BRAIN-D10 · L79](../../../01-brainstorming-result.md#4-decision-register) | D10 \| Working design \| Keep movies, people, and credits within Catalog; keep asset lifecycle in Media |
| [BRAIN-D11 · L80](../../../01-brainstorming-result.md#4-decision-register) | D11 \| Open \| Select artwork transport, attachment/replacement protocol, and recovery/cleanup policy |
| [BRAIN-D19 · L88](../../../01-brainstorming-result.md#4-decision-register) | D19 \| Committed \| Unit and integration tests, including real gRPC communication and persistence against the selected database engine |

## Implementation

1. Implement preflight effective byte/format guidance with browser unit checks while preserving server content authority. Release raw File/object URLs on completion/session loss; never save them in recovery drafts.
2. Use real XHR upload progress for GraphQL transfer; show processing and confirmed attachment separately. Transfer100% never says saved.
3. Poll the original Catalog operation at500ms doubling to5s, stop foreground polling atfive minutes with a resume/check action, and suppress stale controller results.
4. Show invalid/rejected/cancelled/changed-permission/changed-target/failed-replacement/uncertain/recovered states. Fresh retry is offered only after known failure and revalidation; cancellation uses its own UUID.
5. Exercise actual bytes, processor and owner services, including response loss and policy tightening, then compare previous/selected image and audit/receipt/hold state.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T026-A1:** Progress: observed byte events drive transfer only; processing remains visible until actual attachment and current movie image agree.
- **T026-A2:** Recovery: invalid/rejected/new failed replacement leaves previous image and metadata; unknown result preserves uncertainty and checks original UUID.
- **T026-A3:** Safety: cancelled or expired-user upload cannot gain unauthorized attachment and unavailable original image still has readable fallback.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-038](../../../05-atdd.md#case-atdd-038-progress) / End-to-end | Upload static PNG NEW with observedXHR events; pause Media processing after transfer100%, then complete safe attachment and query current selected bytes. | Then Show measured transfer progress, separate processing, then saved only after the movie displays the confirmed attachment. Transfer at 100% is not a saved result. Observation: Capture actual XHR bytes/total, then processing and attachment state plus selected bytes/receipt; transfer100% alone never shows Saved. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-039](../../../05-atdd.md#case-atdd-039-invalid-file) / End-to-end | F-IMAGES empty, random byte, disguised, animated, malformed, oversize, bomb; all rejected preserve OLD/current movie version except authorized stage effects. | Then Explain the applicable format/size/content/processing-limit issue and attach nothing; preserve previous artwork. Enforce section 5.5 against actual content, not filename or declared type alone. The techdoc defines validation mechanisms and processing limits. Observation: Measure independently verified input/decode/resource faults, safe field/status feedback and unchanged OLD selected reference/bytes; file size alone cannot prove supported content. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-040](../../../05-atdd.md#case-atdd-040-permissiontarget-changes) / End-to-end | Beforeattachment revoke actor, archive target or delete target in independent rows; reserve candidate mayexist butselectedOLD not replaced. | Then Do not attach to an unauthorized/unavailable target; explain the outcome and preserve the previous valid artwork. Observation: Revoke/archive/delete at the pre-attachment recheck barrier; inspect candidate hold versus unchanged old selection and abort/release work with no unauthorized attachment. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-041](../../../05-atdd.md#case-atdd-041-interrupted-upload) / End-to-end | Cancelpreparation/transfer; sever gRPC midstream; fail disk write. Reconcile intent before enabling new UUID. | Then Show cancellation/failure without claiming success, preserve the previous artwork, and allow a fresh attempt once the outcome is known. Resume-from-byte support is not required. Observation: Observe transfer cancellation/stream interruption/disk-write failure, original intent outcome and cleaned temporary resources; reconcile before permitting another UUID. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-042](../../../05-atdd.md#case-atdd-042-repeated-upload) / End-to-end | Repeat same original upload UUID/bytes concurrently duringPENDING and afterSUCCEEDED; compare selected hold and auditonce. | Then Do not create unintended duplicate attachments; show the known result or pending outcome instead of treating each repeat as a new success. Observation: Replay/concurrently submit the same upload identity and bytes; inspect one accepted child job/selected hold/attachment audit and stable original outcome. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-043](../../../05-atdd.md#case-atdd-043-upload-succeeded-attachment-failed) / End-to-end | Upload/processing complete then forceCatalogstaleversion/policy error before attachment; safeFAILED after reconciliation; OLD remains public and candidate private. | Then Clearly report that artwork was not attached; keep prior artwork, and offer a safe retry after confirming the failed outcome. No unattached image becomes public. Observation: Force stale/policy failure after successful byte processing but before attachment; inspect failed original receipt, preserved OLD and private releasable candidate. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-044](../../../05-atdd.md#case-atdd-044-replacement) / End-to-end | Success selectsNEW; processing/attachment failure retainsOLD; M-B stillreferencesOLD during later cleanup. | Then Success displays the new confirmed image; failure preserves the previous image. Later cleanup cannot remove another title's valid artwork. Observation: Compare actual selected OLD/NEW bytes across success/failure; cleanup preserves OLD while another title’s protective hold still exists. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-045](../../../05-atdd.md#case-atdd-045-uncertain-attachment) / End-to-end | Dropattachmentcommitresponse, pause GetOperation or make it unavailable; UI remains uncertain until original receipt recovered; neverdeletepossiblyheldNEW. | Then Show uncertainty while resolving the result; display success or a recoverable failure only when known. Do not ask for a blind repeat or delete possibly attached artwork. Observation: Lose the postcommit attachment reply and pause receipt lookup; inspect Uncertain with original UUID until authoritative outcome, never deleting possibly held NEW. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-046](../../../05-atdd.md#case-atdd-046-interrupted-cleanup) / End-to-end | Cleanupbeforeunlink/after unlink process crash; overlapping reserve/new title hold; resume after restart; protective assets unchanged. | Then Existing attachments stay usable; recovery does not remove referenced artwork or report missing output as completed. Observation: Kill real cleanup before/after unlink while racing a reserve; inspect protective holds, byte presence and idempotent restart/tombstone result. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-064](../../../05-atdd.md#case-atdd-064-artwork-outage) / System | StopMedia after ready; read title without image, public search, updateM-A metadata; image-dependent action fails truthfully. | Then Unrelated metadata work can succeed; image-dependent work explains its failure without claiming attachment success. Observation: Stop Media and inspect useful public metadata/search/person/edit behavior with placeholder; image-dependent operations fail truthfully rather than empty catalog. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-123](../../../05-atdd.md#case-atdd-123-artwork-byte-boundaries) / End-to-end | File0 bytes; one invalid byte; independentlyvalidJPEGat5242880 and 5242881 bytes with bounded metadata padding; byte check subject to decode; OLD preserve don reject. | Then Reject empty/undecodable content and the oversized file with the specific reason; the exact maximum passes the size check subject to all content/processing checks. Attach no rejected image and preserve previous artwork. MiB is displayed with its exact byte interpretation. Observation: Measure decoded byte0/limit−1/limit/limit+1 independent of encoded envelope and actual content validity; inspect safe rejection preserving old artwork. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-124](../../../05-atdd.md#case-atdd-124-static-supported-formats) / End-to-end | StaticJPEG/PNG/WebP; GIF/SVG/APNG/animatedWebP/unknown codec. Noanimationtostaticacceptance; placeholder when missing old image. | Then Supported static files proceed through upload/processing to confirmed attachment if other checks pass. Reject unsupported or animated content with correction feedback and no attachment; do not silently convert animation into an accepted static upload. Missing artwork remains a placeholder on failure. Observation: Fully decode verified static JPEG/PNG/WebP and enabled-format combinations; inspect safe processed output/dimensions/checksum rather than trusting extension/MIME. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-125](../../../05-atdd.md#case-atdd-125-disguised-malformed-or-unsafe-image) / End-to-end | Extension.jpg declares image jpeg with invalid actual body; valid magic truncated; compressed bomb; isolated libvips limits and no attachment. | Then Reject it without attachment or unbounded processing; preserve prior artwork and report a safe content/processing-limit failure. Filename, declared type, or signature alone cannot bypass validation. Numerical processing bounds are established under OD-007/OD-019 before implementation. Observation: Run actual disguised/animated/malformed/bomb/path input against bounded nonroot decoder/storage and inspect no unsafe output, containment breach or old-image loss. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-126](../../../05-atdd.md#case-atdd-126-replacement-failure-and-correction) / End-to-end | NEW processing fails/attachment fails then correctedVALID after known failure; OLD metadata preserved; uncertain first intent reconciled before fresh. | Then Existing artwork and unrelated movie information remain available; show the failure and effective rules, then allow a safe fresh attempt. Confirm a replacement only after attachment. Uncertain outcomes still require reconciliation under AC-045 before repeat submission. Observation: Reject invalid replacement, correct it under fresh permitted intent/version and inspect preserved OLD until confirmed NEW selection; no failed upload reports Saved. Execute every Test data row independently at this case’s stated real boundary and retain its sanitized response/state evidence; controlled faults do not substitute for real owner enforcement. |
| [ATDD-159](../../../05-atdd.md#case-atdd-159-validate-the-complete-artwork-stream-boundary) / Integration | F-IMAGES and F-ART; generate deterministic files with actual declared byte sizes, independently measured checksum/dimensions/frames and source limit − 1 / limit / limit + 1 variants. Reset OLD before each row. | Then only one valid header followed by bounded64 KiB chunks/128 KiB messages matching PREPARING actor/asset/intent is accepted And exact byte count and checksum are verified before a durable processing job commits And failure/cancellation cleans bounded temporary spools and attaches nothing Observation: Send real client-streaming bytes with one header and each malformed chunk/order/checksum/truncation condition. Measure spool/received bytes and inspect Media child receipt/job plus Catalog intent; rejected streams cannot create READY output or change selected artwork. |
| [ATDD-160](../../../05-atdd.md#case-atdd-160-bound-full-decoding-independently-of-compressed-size) / Integration | F-IMAGES and F-ART; generate deterministic files with actual declared byte sizes, independently measured checksum/dimensions/frames and source limit − 1 / limit / limit + 1 variants. Reset OLD before each row. | Then accepted output applies orientation, strips metadata and stores its measured checksum/type/dimensions And input≤5242880 bytes, dimensions≤8192, pixels≤40M, decoded≤160Mbytes, RSS≤256 MiB, CPU≤5 s, wall≤10 s, temp≤32 MiB, output≤8 MiB are independently enforced And animation, malformed/disguised/polyglot or resource-killed input attaches nothing and preserves OLD Observation: Use independently verified actual static, animated, malformed and bomb files in the restricted decoder subprocess. Record decode/frame verdict, peak RSS, CPU/wall/temp/output bounds and termination result. Reread selected old bytes after rejection; fake MIME or mocked decode success is insufficient. |
| [ATDD-161](../../../05-atdd.md#case-atdd-161-keep-file-content-away-from-execution-and-paths) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.6/8.2 R-06 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then storage uses generated UUID-only owned paths and fixed processor arguments And no user filename becomes a shell command, arbitrary path or network fetch And nonroot/no-network/read-only-root/restricted-mount processing preserves legitimate supported images within its bounds Observation: Send malicious filenames/path/MIME values while tracing fixed subprocess argv and generated UUID paths. Inspect filesystem containment, nonroot identity, mounts/read-only root and blocked egress; public serving must use processed output rather than supplied names/URLs. |
| [ATDD-162](../../../05-atdd.md#case-atdd-162-recover-crashes-between-intent-and-byte-acceptance) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.6 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then the first intent remains recoverable until its bounded deadline and then aborts without fabricated attachment And a successfully accepted Media job retains Catalog reconciliation work through restart And original upload/asset/child identities remain stable Observation: Kill Core/Media at each named intent/acceptance barrier, then inspect Catalog PREPARING/PENDING and actual Media child receipt. Restart recovery and observe either bounded abort or continuing attachment obligation; accepted bytes must not be silently orphaned. |
| [ATDD-163](../../../05-atdd.md#case-atdd-163-protect-a-candidate-before-selecting-it) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §7.6 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then no Catalog reference commits before a matching protective hold exists And final active MOVIE/version/policy/actor checks decide attachment And rejected attachment retains OLD and durable release work without exposing NEW publicly Observation: Observe Media RESERVED hold commit before enabling Catalog reference transaction. Change active movie/version/policy/actor at the recheck barrier and verify abort/release work plus unchanged OLD; success commits the protected NEW tuple and required audit/work. |
| [ATDD-169](../../../05-atdd.md#case-atdd-169-serve-only-currently-authorized-processed-artwork) / Integration | F-IMAGES and F-ART; generate deterministic files with actual declared byte sizes, independently measured checksum/dimensions/frames and source limit − 1 / limit / limit + 1 variants. Reset OLD before each row. | Then Core grants and Media rechecks the current attachment tuple before the first byte And one header and ordered64 KiB chunks match checksum/type/dimensions/exact length within8 MiB And nosniff/no-store and correct type/length hold; Range416 and incomplete/disconnected streams do not become successful images Observation: Inspect real Core HTTP headers and Media server-stream header/chunk sequence, checksum, length and current attachment grant. Revoke/archive/replace before first byte; tampered/stale/unreferenced grants fail closed. Truncation and disconnect must not count as a complete image. |
| [ATDD-170](../../../05-atdd.md#case-atdd-170-show-measured-stages-and-resume-the-original-outcome) / Integration | Use F-CATALOG/F-ACTORS/F-CLOCK, the exact §3.5 source values and the inputs named in When. Each fault/boundary row starts from a fresh snapshot. | Then only XHR transferred-body events supply network progress and100% switches to processing rather than saved And polling starts500 ms, doubles to5 s, stops foreground after5 min and resumes original UUID And uncertain polls preserve safe review; browser files/object URLs are released without deleting durable selected artwork Observation: Record actual XHR transferred body bytes separately from local read/encoding. Pause processing/attachment and inspect stages, polling timestamps/UUID and foreground cap; cancellation and page return reconcile the original operation and release browser handles. |
| [ATDD-263](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) / End-to-end | F-UI; each state row uses F-CATALOG/F-ACTORS and its source-defined stage. Replace local fixture IDs with deterministic actual IDs; no simulated save/upload/client result counts as execution evidence. | Then the visible heading, identity context, feedback and action match the row oracle And each outcome actually applicable to this flow remains distinct from its pending, failed or unavailable alternatives And theme, width and keyboard changes preserve state and show no restricted data or unsupported feature Observation: Exercise `uploadArtwork`, `cancelArtwork`, `operation`, `managedTitle` in Panel in the managed movie editor, capturing the current response/operation and each row’s visible heading, identity, controls, retained values and safe next action. Assert the named FilePicker / TransferProgress / ArtworkStatus through actual visible layout and accessible names/status rather than hidden fixture text. Run each row independently in both themes at declared widths; verify keyboard/focus and permitted navigation without leaking protected data. Source fragments identify design references, not implementation selectors. \| State reference \| Actor / independent trigger \| Visible semantic oracle \| \| --- \| --- \| --- \| \| artwork \| Maintainer; open movie artwork panel with current policy and OLD \| Show confirmed OLD separately from candidate, file picker and current static format/byte rules. Candidate is not public; filenames/MIME do not prove acceptance. \| \| artwork-transfer \| Maintainer; deliver measured XHR request-body progress while upload is pending \| Show measured transferred percentage and Cancel, preserving OLD. No timer-based percentage or attachment-success label appears. \| \| artwork-processing \| Maintainer; complete transfer while processing/attachment remains pending \| Show transfer complete and processing status, keep candidate separate and OLD confirmed. Transfer 100% is never saved/publication feedback. \| \| artwork-attached \| Maintainer; recover confirmed attachment receipt and managed title \| Show Artwork attached and actual selected NEW. Review/navigation uses confirmed state; another title's shared OLD remains usable through cleanup. \| \| artwork-cancelled \| Maintainer; win `cancelArtwork` before attachment and reconcile original intent \| Show Upload cancelled with unchanged OLD; release browser handles and offer fresh selection only after conclusive cancellation. A lost cancel/attach race instead shows the original success. \| \| artwork-invalid \| Maintainer; reject an invalid F-IMAGES file before attachment \| Show specific safe format/size/content/processing correction, retain OLD and permit corrected selection. No silent conversion of animation or candidate attachment appears. \| \| artwork-failed \| Maintainer; processing succeeds but attachment returns confirmed failure \| Show Artwork was not attached, keep OLD and offer corrected fresh attempt; transfer completion and candidate preview cannot masquerade as public attachment. \| \| artwork-target \| Maintainer; revoke permission/archive/delete target before attachment \| Show movie/access-changed feedback, preserve previous valid artwork and require current identity/target checks. Conceal protected values on expiry; no unauthorized attach/retry shortcut appears. \| \| artwork-uncertain \| Maintainer; lose attachment/cancellation reply \| Show Attachment is not confirmed and check original upload UUID. Candidate is not declared public, and repeat/deletion is not offered while outcome remains unknown. \| \| artwork-policy \| Maintainer; tighten allowed formats while a candidate awaits attach \| Show changed effective rules with retained permitted review context and OLD; disallowed changed/new input requires correction. Check original outcome before another attempt if uncertain. |
| [ATDD-305](../../../05-atdd.md#case-atdd-305-separate-real-transfer-events-from-processing-display) / Component | ControlledXHR transfer0/50/100%; encoding event100%; processingpending, attachmentuncertain and original receipt success; polling clock500 ms→5 s/cap5 min. | Then only XHR transferred HTTP-body bytes drive network percentage And transfer100% is processing, not saved; uncertainty retains original UUID And polling obeys selected bounds and objectURL/file handles are released Observation: Inspect visible stage/progress/request UUID and controlled schedule; actual streamed bytes/codec/hold durability remain integration. |
| [ATDD-315](../../../05-atdd.md#case-atdd-315-check-browser-file-policy-before-transfer-without-trusting-mime) / Unit | File sizes0/1/5242880/5242881 bytes; enabled JPEG/PNG/WebP subsets; disabledformat, misleadingextension/MIME and absentcurrent policy. | Then zero/oversize or disabled-format selections fail corrective client feedback before transfer And eligible size/type input still requires authoritative full server content validation And missing policy cannot silently enable unbounded upload Observation: Assert explicit selection eligibility and field error with fixed size/type values; no actual image decoding/streaming or persistence is claimed by this unit rule. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-263-T1-R01 · L10971](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork; Maintainer; open movie artwork panel with current policy and OLD; Show confirmed OLD separately from candidate, file picker and current static format/byte rules. Candidate is not public; filenames/MIME do not prove acceptance. |
| [ATDD-263-T1-R02 · L10972](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork-transfer; Maintainer; deliver measured XHR request-body progress while upload is pending; Show measured transferred percentage and Cancel, preserving OLD. No timer-based percentage or attachment-success label appears. |
| [ATDD-263-T1-R03 · L10973](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork-processing; Maintainer; complete transfer while processing/attachment remains pending; Show transfer complete and processing status, keep candidate separate and OLD confirmed. Transfer 100% is never saved/publication feedback. |
| [ATDD-263-T1-R04 · L10974](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork-attached; Maintainer; recover confirmed attachment receipt and managed title; Show Artwork attached and actual selected NEW. Review/navigation uses confirmed state; another title's shared OLD remains usable through cleanup. |
| [ATDD-263-T1-R05 · L10975](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork-cancelled; Maintainer; win `cancelArtwork` before attachment and reconcile original intent; Show Upload cancelled with unchanged OLD; release browser handles and offer fresh selection only after conclusive cancellation. A lost cancel/attach race instead shows the original success. |
| [ATDD-263-T1-R06 · L10976](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork-invalid; Maintainer; reject an invalid F-IMAGES file before attachment; Show specific safe format/size/content/processing correction, retain OLD and permit corrected selection. No silent conversion of animation or candidate attachment appears. |
| [ATDD-263-T1-R07 · L10977](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork-failed; Maintainer; processing succeeds but attachment returns confirmed failure; Show Artwork was not attached, keep OLD and offer corrected fresh attempt; transfer completion and candidate preview cannot masquerade as public attachment. |
| [ATDD-263-T1-R08 · L10978](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork-target; Maintainer; revoke permission/archive/delete target before attachment; Show movie/access-changed feedback, preserve previous valid artwork and require current identity/target checks. Conceal protected values on expiry; no unauthorized attach/retry shortcut appears. |
| [ATDD-263-T1-R09 · L10979](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork-uncertain; Maintainer; lose attachment/cancellation reply; Show Attachment is not confirmed and check original upload UUID. Candidate is not declared public, and repeat/deletion is not offered while outcome remains unknown. |
| [ATDD-263-T1-R10 · L10980](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | artwork-policy; Maintainer; tighten allowed formats while a candidate awaits attach; Show changed effective rules with retained permitted review context and OLD; disallowed changed/new input requires correction. Check original outcome before another attempt if uncertain. |

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T026` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T026` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
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
| [artwork / artwork](../../../design-system/mockup/artwork.html#artwork) · [ATDD-263-T1-R01 · L10971](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; open movie artwork panel with current policy and OLD; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show confirmed OLD separately from candidate, file picker and current static format/byte rules. Candidate is not public; filenames/MIME do not prove acceptance. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [artwork / artwork-transfer](../../../design-system/mockup/artwork.html#artwork-transfer) · [ATDD-263-T1-R02 · L10972](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; deliver measured XHR request-body progress while upload is pending; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show measured transferred percentage and Cancel, preserving OLD. No timer-based percentage or attachment-success label appears. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [artwork / artwork-processing](../../../design-system/mockup/artwork.html#artwork-processing) · [ATDD-263-T1-R03 · L10973](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; complete transfer while processing/attachment remains pending; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show transfer complete and processing status, keep candidate separate and OLD confirmed. Transfer 100% is never saved/publication feedback. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [artwork / artwork-attached](../../../design-system/mockup/artwork.html#artwork-attached) · [ATDD-263-T1-R04 · L10974](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; recover confirmed attachment receipt and managed title; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Artwork attached and actual selected NEW. Review/navigation uses confirmed state; another title's shared OLD remains usable through cleanup. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [artwork / artwork-cancelled](../../../design-system/mockup/artwork.html#artwork-cancelled) · [ATDD-263-T1-R05 · L10975](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; win `cancelArtwork` before attachment and reconcile original intent; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Upload cancelled with unchanged OLD; release browser handles and offer fresh selection only after conclusive cancellation. A lost cancel/attach race instead shows the original success. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [artwork / artwork-invalid](../../../design-system/mockup/artwork.html#artwork-invalid) · [ATDD-263-T1-R06 · L10976](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; reject an invalid F-IMAGES file before attachment; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show specific safe format/size/content/processing correction, retain OLD and permit corrected selection. No silent conversion of animation or candidate attachment appears. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [artwork / artwork-failed](../../../design-system/mockup/artwork.html#artwork-failed) · [ATDD-263-T1-R07 · L10977](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; processing succeeds but attachment returns confirmed failure; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Artwork was not attached, keep OLD and offer corrected fresh attempt; transfer completion and candidate preview cannot masquerade as public attachment. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [artwork / artwork-target](../../../design-system/mockup/artwork.html#artwork-target) · [ATDD-263-T1-R08 · L10978](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; revoke permission/archive/delete target before attachment; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show movie/access-changed feedback, preserve previous valid artwork and require current identity/target checks. Conceal protected values on expiry; no unauthorized attach/retry shortcut appears. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [artwork / artwork-uncertain](../../../design-system/mockup/artwork.html#artwork-uncertain) · [ATDD-263-T1-R09 · L10979](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; lose attachment/cancellation reply; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show Attachment is not confirmed and check original upload UUID. Candidate is not declared public, and repeat/deletion is not offered while outcome remains unknown. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |
| [artwork / artwork-policy](../../../design-system/mockup/artwork.html#artwork-policy) · [ATDD-263-T1-R10 · L10980](../../../05-atdd.md#case-atdd-263-inspect-artwork-state-transitions-and-feedback) | Maintainer; tighten allowed formats while a candidate awaits attach; F-CATALOG/F-ACTORS and fixed F-CLOCK as applicable; both themes and declared widths | Show changed effective rules with retained permitted review context and OLD; disallowed changed/new input requires correction. Check original outcome before another attempt if uncertain. Compare the required visible result at this exact state. Label controlled responses; full real journey proof remains with its case owner/T044. |

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
