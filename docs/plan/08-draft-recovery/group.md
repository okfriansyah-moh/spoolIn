---
kind: group
id: G08
tasks:
- T014
- T015
- T016
integration_owner: T016
---

# 08-draft-recovery

[Overall plan](../../06-PLAN.md)

## Outcome and scope

Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation. Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request. Real recovery: after committed save loses response, resume reveals the original outcome and current record without creating another movie/person/credit. The tasks bind backend, frontend and verification where that produces an observable outcome. Only the named local results are owned here; source documents stay intact.

## Shared context

Read the overall source authority/shared rules first and each task's selected source sections. The technical contracts own exact fields, transactions and safe failures; the PRD owns behavior; the adopted design owns presentation. Stable record/operation/draft identities remain distinct.

- [3.4 Forms, drafts and conflict recovery](../../04-spoolin-techdoc.md#34-forms-drafts-and-conflict-recovery).
- [5.6.5 Code example: save and reconcile a draft](../../04-spoolin-techdoc.md#565-code-example-save-and-reconcile-a-draft).
- [5.11 JSON projections and draft schemas](../../04-spoolin-techdoc.md#511-json-projections-and-draft-schemas).
- [6.2.4 Core tables](../../04-spoolin-techdoc.md#624-core-tables).
- [7.3.1 Durable draft receipts](../../04-spoolin-techdoc.md#731-durable-draft-receipts).
- [3.4.1 Browser storage and restored-page state](../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state).
- [8.5.4 Session and CSRF responsibility map](../../04-spoolin-techdoc.md#854-session-and-csrf-responsibility-map).

Use [the adopted design](../../03-design-system.md), [shared components](../../design-system/components.md) and the exact task state tables. Component fixtures cannot close integrated service or host outcomes.

## Task list

| Task | Result | Detail |
|---|---|---|
| T014 | Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation. | [T014 — Persist encrypted drafts with durable save and discard receipts](tasks/T014-persist-encrypted-drafts-with-durable-save-and-discard-receipts.md) |
| T015 | Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request. | [T015 — Build account-isolated browser draft controllers](tasks/T015-build-account-isolated-browser-draft-controllers.md) |
| T016 | Real recovery: after committed save loses response, resume reveals the original outcome and current record without creating another movie/person/credit. | [T016 — Prove recovery through real movie, person and credit editors](tasks/T016-prove-recovery-through-real-movie-person-and-credit-editors.md) |

## Dependencies

External inputs: [T004 — Build the shared themed and accessible shell](../02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md), [T005 — Protect account identities and credential storage](../03-account-access/tasks/T005-protect-account-identities-and-credential-storage.md), [T006 — Implement registration and authoritative sessions](../03-account-access/tasks/T006-implement-registration-and-authoritative-sessions.md), [T007 — Connect account and session browser journeys](../03-account-access/tasks/T007-connect-account-and-session-browser-journeys.md), [T008 — Enforce scoped service identity and shared admission](../06-guarded-access/tasks/T008-enforce-scoped-service-identity-and-shared-admission.md), [T009 — Implement effective policy and precise input semantics](../04-effective-input-policy/tasks/T009-implement-effective-policy-and-precise-input-semantics.md), [T010 — Implement owner receipts and atomic mutation evidence](../05-reliable-work/tasks/T010-implement-owner-receipts-and-atomic-mutation-evidence.md), [T018 — Connect movie editors and confirmed outcomes](../09-movie-management/tasks/T018-connect-movie-editors-and-confirmed-outcomes.md), [T020 — Connect people management and retained edits](../10-people-and-credits/tasks/T020-connect-people-management-and-retained-edits.md), [T022 — Connect credit editing and verify the people outcome](../10-people-and-credits/tasks/T022-connect-credit-editing-and-verify-the-people-outcome.md), [T024 — Connect private archives and irreversible confirmations](../12-archive-management/tasks/T024-connect-private-archives-and-irreversible-confirmations.md), [T027 — Connect measured progress and recover attachment outcomes](../11-artwork-management/tasks/T027-connect-measured-progress-and-recover-attachment-outcomes.md). Consume their concrete outputs as stated in member prerequisites.

- T014 starts after T005, T006, T008, T009, T010; its task definition owns the specific files/contracts and acceptance boundary.
- T015 starts after T004, T007, T014; its task definition owns the specific files/contracts and acceptance boundary.
- T016 starts after T015, T018, T020, T022, T024, T027; its task definition owns the specific files/contracts and acceptance boundary.

Dependency edges control execution, not display order. Root lockfiles/generators remain T001/T002-owned; serialize migration-version allocation and shared configuration. Parallel workers use disjoint change surfaces, independent test namespaces/ports/DBs and compatible generated outputs. Ancestor/subfolder writes are conflicts even when filenames differ. A source update invalidates affected instructions/evidence.

## Integrated acceptance

[T016 — Prove recovery through real movie, person and credit editors](tasks/T016-prove-recovery-through-real-movie-person-and-credit-editors.md) owns the combined feature verdict. Require every member task Done with current local evidence plus the following real-boundary outcomes. Do not make this downstream proof a prerequisite for its upstream contributors to finish their local checks.

- T014: Save replay: losing the response after commit and retrying the same Core UUID returns one safe original result with no new nonce/TTL/audit/mutation.
- T014: Discard replay: deleted draft content is never reread or rebuilt; later state remains absent even when the old request repeats.
- T014: Isolation: B cannot read/replay A's recovery state, quota/policy changes cannot break identical retained replay, and distinct stale edits require review.
- T015: Ordering: known original operation is queried before current values, so uncertain commit cannot cause a duplicate new request.
- T015: Isolation: delayed A response after switch to B never renders A content; returning A may resume only after current permission/target/operation checks.
- T015: Feedback: autosave failure preserves permitted in-memory input with an unbacked warning; clear/discard does not claim success before confirmed outcome.
- T016: Real recovery: after committed save loses response, resume reveals the original outcome and current record without creating another movie/person/credit.
- T016: Ownership: B cannot inherit A's recovery data through storage/history/delayed replies; A's expired/revoked session cannot expose it before revalidation.
- T016: Mutation recovery: stale or removed targets require review and retain only permitted work; no force overwrite, resurrection or secret persistence.

Exact case/row owners are in member Traceability/Tests and the root register. At integration, follow the complete journey across actual participating owners and reconcile the corresponding current case/row evidence; an isolated mock or source link does not establish that journey. Include declared denied/invalid/stale/failed/uncertain/recovered variants and absence of prohibited effects. Retain matched expected/actual state captures with separate interaction/accessibility findings at the task-declared themes/viewports/hosts. For non-UI paths observe exact response, persisted state, file bytes, diagnostic/operational outcomes and restart behavior as relevant. If a required runtime/host/provider is unavailable, keep feature completion open with its concrete prerequisite.

Cross-feature release acceptance belongs to T042–T046; their later evidence does not prevent completed local contributors from being marked Done. A full release still requires every included obligation, both host proofs and G-01–G-06.
