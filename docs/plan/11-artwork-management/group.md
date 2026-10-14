---
kind: group
id: G12
tasks:
- T025
- T026
- T027
integration_owner: T027
---

# 11-artwork-management

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing. Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion. Progress: observed byte events drive transfer only; processing remains visible until actual attachment and current movie image agree. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [4.4 SpoolIn Media service](../../04-spoolin-techdoc.md#44-spoolin-media-service).
- [5.2.5 Media messages](../../04-spoolin-techdoc.md#525-media-messages).
- [6.2.6 Media tables](../../04-spoolin-techdoc.md#626-media-tables).
- [7.6 Artwork transfer and reference-safe attachment](../../04-spoolin-techdoc.md#76-artwork-transfer-and-reference-safe-attachment).
- [8.3.2 Output and executable-input controls](../../04-spoolin-techdoc.md#832-output-and-executable-input-controls).
- [5.5 Browser HTTP and operational endpoints](../../04-spoolin-techdoc.md#55-browser-http-and-operational-endpoints).
- [6.2.5 Catalog tables](../../04-spoolin-techdoc.md#625-catalog-tables).
- [7.7 Durable work, retries and notifications](../../04-spoolin-techdoc.md#77-durable-work-retries-and-notifications).
- [3.5 Artwork and operation feedback](../../04-spoolin-techdoc.md#35-artwork-and-operation-feedback).
- [3.1.1 UI reference and implementation map](../../04-spoolin-techdoc.md#311-ui-reference-and-implementation-map).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T025 | Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing. | [T025 — Accept and process bounded artwork streams](tasks/T025-accept-and-process-bounded-artwork-streams.md) |
| T026 | Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion. | [T026 — Coordinate protective holds, attachment and cleanup](tasks/T026-coordinate-protective-holds-attachment-and-cleanup.md) |
| T027 | Progress: observed byte events drive transfer only; processing remains visible until actual attachment and current movie image agree. | [T027 — Connect measured progress and recover attachment outcomes](tasks/T027-connect-measured-progress-and-recover-attachment-outcomes.md) |

## Dependencies

External inputs: [T003 — Establish private service and database ownership](../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md), [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T007 — Connect account and session browser journeys](../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md), [T011 — Run fenced recovery and bounded retries](../05-reliable-work/tasks/T011-run-fenced-recovery-and-bounded-retries.md), [T017 — Implement authoritative movie creation and edits](../09-movie-management/tasks/T017-implement-authoritative-movie-creation-and-edits.md), [T018 — Connect movie editors and confirmed outcomes](../09-movie-management/tasks/T018-connect-movie-editors-and-confirmed-outcomes.md), [T023 — Implement current-impact archive, restore and deletion](../12-archive-management/tasks/T023-implement-current-impact-archive-restore-and-deletion.md). Consume their concrete outputs as stated in member prerequisites.

- T025 starts after T003, T008, T009, T010, T011; its task definition owns the specific files/contracts and acceptance boundary.
- T026 starts after T008, T010, T011, T017, T025; its task definition owns the specific files/contracts and acceptance boundary.
- T027 starts after T004, T007, T018, T025, T026, T023; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T027 — Connect measured progress and recover attachment outcomes](tasks/T027-connect-measured-progress-and-recover-attachment-outcomes.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T025: Content: exact maximum valid image may proceed; maximum+1, one-byte invalid, animation, unsafe dimensions and forged MIME attach nothing.
- T025: Execution: hostile path/option metadata executes no extra command, fetch or write outside quarantine/output boundaries.
- T025: Durability: crash before/after accepted processing leaves truthful resumable state, one work binding and no false READY or lost protected output.
- T026: Replacement: failure keeps OLD, committed NEW survives finalization outage, and another title's active/reserved reference prevents deletion.
- T026: Cancel: either abort wins with no new attachment or attach wins truthfully; a stale/delayed release cannot undo the winner.
- T026: Retrieval: public/Viewer cannot fetch private/archived/unattached assets by guessed ID or stale URL; Media outage leaves unrelated metadata usable.
- T027: Progress: observed byte events drive transfer only; processing remains visible until actual attachment and current movie image agree.
- T027: Recovery: invalid/rejected/new failed replacement leaves previous image and metadata; unknown result preserves uncertainty and checks original UUID.
- T027: Safety: cancelled or expired-user upload cannot gain unauthorized attachment and unavailable original image still has readable fallback.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
